/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.injection.xml.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * Compatibility EJBLocalHome interface for Session beans with
 * methods to verify Environment Injection.
 **/
public interface EnvInjectionEJBLocalHome extends EJBLocalHome {
    /**
     * @return EnvInjectionEJBLocal The SessionBean EJB object.
     * @exception javax.ejb.CreateException SessionBean EJB object was not created.
     */
    public EnvInjectionEJBLocal create() throws CreateException;
}
