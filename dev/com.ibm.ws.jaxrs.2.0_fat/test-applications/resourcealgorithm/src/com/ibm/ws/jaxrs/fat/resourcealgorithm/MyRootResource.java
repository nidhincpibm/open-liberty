/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.resourcealgorithm;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/root")
public class MyRootResource {

    @GET
    public String get() {
        return "MyRootResource.get()";
    }

    /*
     * Subresource method.
     */
    @Path("actual_subresource")
    public MyObject postSub() {
        return new MyObject();
    }

    public static class MyObject {

        @POST
        public String hello() {
            return "MyObject.hello()";
        }
    }

    /*
     * Subresource method.
     */
    @GET
    @Path("subresource")
    public String getSub() {
        return "MyRootResource.getSub()";
    }
}
