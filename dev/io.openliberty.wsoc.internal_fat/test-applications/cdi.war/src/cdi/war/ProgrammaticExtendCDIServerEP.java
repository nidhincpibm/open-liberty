/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package cdi.war;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class ProgrammaticExtendCDIServerEP extends Endpoint implements
                MessageHandler.Whole<String> {

    private Session session = null;

    public @Inject
    CounterDependentScoped depScopedCounter;

    public @Inject
    CounterApplicationScoped appScopedCounter;

    public @Inject
    CounterSessionScoped sesScopedCounter;

    @Override
    public void onOpen(final Session session, EndpointConfig ec) {
        this.session = session;
        session.addMessageHandler(this);
    }

    @Override
    public void onMessage(String msg) {

        String responseMessage = "Nothing yet";
        try {
            int depCount = depScopedCounter.getNext();
            depCount = depScopedCounter.getNext();

            int appCount = appScopedCounter.getNext();
            appCount = appScopedCounter.getNext();
            appCount = appScopedCounter.getNext();

            int sesCount = sesScopedCounter.getNext();
            sesCount = sesScopedCounter.getNext();
            sesCount = sesScopedCounter.getNext();
            sesCount = sesScopedCounter.getNext();

            // string to test for on first iteration: "Dependent Scoped Counter: 2 ApplicationScopedCounter: 3 SessionScopedCounter: 4"
            responseMessage = "Dependent Scoped Counter: " + depCount + " ApplicationScopedCounter: " + appCount + " SessionScopedCounter: " + sesCount;
            Logger.getLogger(ProgrammaticExtendCDIServerEP.class.getName()).log(Level.FINE, "zzz sesCount is: " + sesCount);
        } catch (Exception ex) {
            Logger.getLogger(ProgrammaticExtendCDIServerEP.class.getName()).log(Level.SEVERE, null, ex);
            responseMessage = ex.toString();
        }
        try {
            this.session.getBasicRemote().sendText(responseMessage);
        } catch (Exception ex) {
            Logger.getLogger(ProgrammaticExtendCDIServerEP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
