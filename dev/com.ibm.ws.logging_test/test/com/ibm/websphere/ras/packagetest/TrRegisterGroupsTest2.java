/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.websphere.ras.packagetest;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ras.SharedTr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceOptions;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

/**
 * Test TraceComponent registration methods using annotations to specify group
 * only ensure class-level annotation has precedence over package-level
 * annotation
 */
@TraceOptions(traceGroup = "singleclassgroup")
public class TrRegisterGroupsTest2 {
    static {
        LoggingTestUtils.ensureLogManager();
    }
    // static Class<?> myClass = TrRegisterGroupsTest.class;

    static final String myName = TrRegisterGroupsTest2.class.getName();

    static SharedOutputManager outputMgr;

    static final Object[] objs = new Object[] { "p1", "p2", "p3", "p4" };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        SharedTr.clearComponents();

        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() throws Exception {
        SharedTr.clearComponents();
        outputMgr.resetStreams();
    }

    @Test
    public void testRegisterClass() {
        Class<?> myClass = this.getClass();
        TraceOptions options = myClass.getAnnotation(TraceOptions.class);
        System.out.println("options are: " + options);

        final String m = "testRegisterClass";
        try {
            TraceComponent tc = Tr.register(myClass);
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[singleclassgroup],,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}