/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.rest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.rest.AbstractTelemetryContainerFilter;
import io.openliberty.microprofile.telemetry.internal.common.rest.RestRouteCache;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesGetter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor;
import io.opentelemetry.semconv.SemanticAttributes;

@Provider
public class TelemetryContainerFilter extends AbstractTelemetryContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";

    private static final String REST_RESOURCE_CLASS = "rest.resource.class";
    private static final String REST_RESOURCE_METHOD = "rest.resource.method";

    private static final String SPAN_CONTEXT = "otel.span.server.context";
    private static final String SPAN_PARENT_CONTEXT = "otel.span.server.parentContext";

    private static final HttpServerAttributesGetterImpl HTTP_SERVER_ATTRIBUTES_GETTER = new HttpServerAttributesGetterImpl();

    private static final RestRouteCache ROUTE_CACHE = new RestRouteCache();

    private Instrumenter<ContainerRequestContext, ContainerResponseContext> instrumenter;
    private volatile boolean lazyCreate = false;
    private final AtomicReference<Instrumenter<ContainerRequestContext, ContainerResponseContext>> lazyInstrumenter = new AtomicReference<>();

    @javax.ws.rs.core.Context
    private ResourceInfo resourceInfo;

    public TelemetryContainerFilter() {
        if (!CheckpointPhase.getPhase().restored()) {
            lazyCreate = true;
        } else {
            instrumenter = createInstrumenter();
        }
    }

    private Instrumenter<ContainerRequestContext, ContainerResponseContext> getInstrumenter() {
        if (instrumenter != null) {
            return instrumenter;
        }
        if (lazyCreate) {
            instrumenter = lazyInstrumenter.updateAndGet((i) -> {
                if (i == null) {
                    return createInstrumenter();
                } else {
                    return i;
                }
            });
            lazyCreate = false;
        }
        return instrumenter;
    }

    private Instrumenter<ContainerRequestContext, ContainerResponseContext> createInstrumenter() {
        final OpenTelemetryInfo openTelemetry = OpenTelemetryAccessor.getOpenTelemetryInfo();
        if (openTelemetry.getEnabled() && !AgentDetection.isAgentActive()) {
            InstrumenterBuilder<ContainerRequestContext, ContainerResponseContext> builder = Instrumenter.builder(
                                                                                                                  openTelemetry.getOpenTelemetry(),
                                                                                                                  INSTRUMENTATION_NAME,
                                                                                                                  HttpSpanNameExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER));

            Instrumenter<ContainerRequestContext, ContainerResponseContext> result = builder
                            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER))
                            .addAttributesExtractor(HttpServerAttributesExtractor.create(HTTP_SERVER_ATTRIBUTES_GETTER))
                            .buildServerInstrumenter(new ContainerRequestContextTextMapGetter());
            return result;

        } else {
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        if (!CheckpointPhase.getPhase().restored()) {
            return true;
        }
        return getInstrumenter() != null;
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        Instrumenter<ContainerRequestContext, ContainerResponseContext> currentInstrumenter = getInstrumenter();
        if (currentInstrumenter == null) {
            return;
        }

        Context parentContext = Context.current();
        if (currentInstrumenter.shouldStart(parentContext, request)) {
            request.setProperty(REST_RESOURCE_CLASS, resourceInfo.getResourceClass());
            request.setProperty(REST_RESOURCE_METHOD, resourceInfo.getResourceMethod());

            Context spanContext = currentInstrumenter.start(parentContext, request);
            Scope scope = spanContext.makeCurrent();
            request.setProperty(SPAN_CONTEXT, spanContext);
            request.setProperty(SPAN_PARENT_CONTEXT, parentContext);
            request.setProperty(SPAN_SCOPE, scope);
        } else {
            Span currentSpan = Span.current();
            if (currentSpan != null) {

                Class<?> resourceClass = resourceInfo.getResourceClass();
                Method resourceMethod = resourceInfo.getResourceMethod();

                String route = getRoute(request, resourceClass, resourceMethod);

                if (route != null) {
                    currentSpan.setAttribute(SemanticAttributes.HTTP_ROUTE, route);
                    currentSpan.updateName(request.getMethod() + " " + route);
                }
            }
        }
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        // Note: for async resource methods, this may not run on the same thread as the other filter method
        // Scope is ended in TelemetryServletRequestListener to ensure it does run on the original request thread
        Instrumenter<ContainerRequestContext, ContainerResponseContext> currentInstrumenter = getInstrumenter();
        if (currentInstrumenter == null) {
            return;
        }

        Context spanContext = (Context) request.getProperty(SPAN_CONTEXT);
        if (spanContext == null) {
            return;
        }

        try {
            currentInstrumenter.end(spanContext, request, response, null);
        } finally {
            request.removeProperty(REST_RESOURCE_CLASS);
            request.removeProperty(REST_RESOURCE_METHOD);
            request.removeProperty(SPAN_CONTEXT);
            request.removeProperty(SPAN_PARENT_CONTEXT);
        }
    }

    private static String getRoute(final ContainerRequestContext request, Class<?> resourceClass, Method resourceMethod) {

        String route = ROUTE_CACHE.getRoute(resourceClass, resourceMethod);

        if (route == null) {

            int checkResourceSize = request.getUriInfo().getMatchedResources().size();

            // Check the resource size using getMatchedResource()
            // A resource size > 1 indicates that there is a subresource
            // We can't currently compute the route correctly when subresources are used
            if (checkResourceSize == 1) {

                String contextRoot = request.getUriInfo().getBaseUri().getPath();
                UriBuilder template = UriBuilder.fromPath(contextRoot);

                if (resourceClass.isAnnotationPresent(Path.class)) {
                    template.path(resourceClass);
                }

                if (resourceMethod.isAnnotationPresent(Path.class)) {
                    template.path(resourceMethod);
                }

                route = template.toTemplate();
                ROUTE_CACHE.putRoute(resourceClass, resourceMethod, route);
            }
        }
        return route;

    }

    private static class ContainerRequestContextTextMapGetter implements TextMapGetter<ContainerRequestContext> {

        @Override
        public Iterable<String> keys(final ContainerRequestContext carrier) {
            return carrier.getHeaders().keySet();
        }

        @Override
        public String get(final ContainerRequestContext carrier, final String key) {
            if (carrier == null) {
                return null;
            }

            return carrier.getHeaders().getOrDefault(key, singletonList(null)).get(0);
        }
    }

    private static class HttpServerAttributesGetterImpl implements HttpServerAttributesGetter<ContainerRequestContext, ContainerResponseContext> {

        @Override
        public String getHttpRoute(final ContainerRequestContext request) {

            Class<?> resourceClass = (Class<?>) request.getProperty(REST_RESOURCE_CLASS);
            Method resourceMethod = (Method) request.getProperty(REST_RESOURCE_METHOD);

            return getRoute(request, resourceClass, resourceMethod);
        }

        //required
        @Override
        public String getHttpRequestMethod(final ContainerRequestContext request) {
            return request.getMethod();
        }

        @Override
        public String getUrlPath(final ContainerRequestContext request) {
            URI requestUri = request.getUriInfo().getRequestUri();
            String path = requestUri.getPath();
            return path;
        }

        @Override
        public String getUrlQuery(final ContainerRequestContext request) {
            URI requestUri = request.getUriInfo().getRequestUri();
            return requestUri.getQuery();
        }

        @Override
        public String getUrlScheme(final ContainerRequestContext request) {
            return request.getUriInfo().getRequestUri().getScheme();
        }

        //If one was sent
        @Override
        public Integer getHttpResponseStatusCode(final ContainerRequestContext request, final ContainerResponseContext response, Throwable error) {
            return response.getStatus();
        }

        @Override
        public List<String> getHttpRequestHeader(final ContainerRequestContext request, final String name) {
            return request.getHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public List<String> getHttpResponseHeader(final ContainerRequestContext request, final ContainerResponseContext response,
                                                  final String name) {
            return response.getStringHeaders().getOrDefault(name, emptyList());
        }
    }
}
