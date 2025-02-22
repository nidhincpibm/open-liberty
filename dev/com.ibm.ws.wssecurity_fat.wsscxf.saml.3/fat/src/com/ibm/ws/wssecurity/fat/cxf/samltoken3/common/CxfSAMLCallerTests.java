/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.samltoken3.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.annotation.SkipForRepeat;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;


/**
 * The testcases in this class were ported from tWAS' test SamlWebSSOTests.
 * If a tWAS test is not applicable, it will be noted in the comments below.
 * If a tWAS test fits better into another test class, it will be noted
 * which test project/class it now resides in.
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
 * to an application installed on a WebSphere SP. When the Web client
 * invokes the SP application, it is redirected to a TFIM IdP which issues
 * a login challenge to the Web client. The Web Client fills in the login
 * form and after a successful login, receives a SAML 2.0 token from the
 * TFIM IdP. The client invokes the SP application by sending the SAML
 * 2.0 token in the HTTP POST request.
 */

//issue 23060
@SkipForRepeat({ EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@LibertyServerWrapper
@RunWith(FATRunner.class)
public class CxfSAMLCallerTests extends SAMLCommonTest {

    protected static String userName = null ;
    protected static String userPass = null ;

    //private static final Class<?> thisClass = CxfSAMLCallerTests.class;
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;
    protected static CXFSAMLCommonUtils commonUtils = new CXFSAMLCommonUtils();
    
    
    /**
     * TestDescription:
     * 
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Service client passes a flag with a value of "cxf" to indicate that a
     * user/password should be included in the service context. This is NOT
     * needed or used as we've specified SAML in the policy. Unlike using
     * usernametoken which doesn't require it either, in this case, it won't
     * even be looked at.
     * Test should succeed in accessing the server side service.
     * 
     */
    
    //scenario 1 - done
    @Test
    public void testCxfCallerHttpPolicy() throws Exception {

        // Create the conversation object which will maintain state for us
        String webServiceName = "FatSamlC01Service";
        String webServicePort = "SamlCallerToken01";
        String policyName = "CallerHttpPolicy";
        String titleToCheck = "CXF SAML Caller Service Client";
        String partToCheck = "pass:true::FatSamlC01Service";
        String testMode = "positive";
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings("testCxfSamlCallerHttpPolicy", "cxf", servicePort, null, userName, userPass, webServiceName, webServicePort, "", "False", null, null);
        updatedTestSettings.getCXFSettings().setBodyPartToCheck(partToCheck);
        updatedTestSettings.getCXFSettings().setTitleToCheck(titleToCheck);
        updatedTestSettings.getCXFSettings().setTestMode(testMode);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));
    }
 
    //scenario 2
    @Test
    public void testCxfCallerHttpsPolicy() throws Exception {
         	
        if (testSAMLServer2 == null) {
            //1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2in1_asymProtection.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            //2 servers reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_caller_asymProtection.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            testSAMLServer.reconfigServer(buildSPServerName("server_1_asymProtection.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        }
    	
    	
        // Create the conversation object which will maintain state for us
        String webServiceName = "FatSamlC03Service";
        String webServicePort = "SamlCallerToken03";
        String policyName = "CallerHttpsPolicy";
        String titleToCheck = "CXF SAML Caller Service Client";
        String partToCheck = "pass:true::FatSamlC03Service";
        String testMode = "positive";
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings("testCxfCallerHttpsPolicy", "cxf", servicePort, serviceSecurePort, userName, userPass, webServiceName,
                webServicePort, "", "False", null, null);
        updatedTestSettings.getCXFSettings().setBodyPartToCheck(partToCheck);
        updatedTestSettings.getCXFSettings().setTitleToCheck(titleToCheck);
        updatedTestSettings.getCXFSettings().setTestMode(testMode);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));

    }
 
    //scenario 3 - done
    @Test
    public void testCxfCaller_WithRealmName() throws Exception {
          	
    	if (testSAMLServer2 == null) {
            // 1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2in1_realmName.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            // 2 server reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_caller_realmName.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            // shouldn't need to reconfig server 1 - we don't need to change anything there
            //			testSAMLServer.reconfigServer(buildSPServerName("server_1_realmName.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        }
    	
    	
        // Create the conversation object which will maintain state for us
        String webServiceName = "FatSamlC02Service";
        String webServicePort = "SamlCallerToken02";
        String policyName = "CallerHttpsPolicy";
        String titleToCheck = "CXF SAML Caller Service Client";
        String partToCheck = "pass:true::FatSamlC02Service";
        String testMode = "positive";
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
     
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings("testCxfCaller_WithRealmName", "cxf", servicePort, serviceSecurePort, userName, userPass, webServiceName, webServicePort, "", "False", null, null);
        updatedTestSettings.getCXFSettings().setBodyPartToCheck(partToCheck);
        updatedTestSettings.getCXFSettings().setTitleToCheck(titleToCheck);
        updatedTestSettings.getCXFSettings().setTestMode(testMode);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));

    }
 
    //scenario 5
    @Test
    public void testCxfCallerHttpsPolicy_IncludeTokenInSubjectIsFalse() throws Exception {
        	
    	if (testSAMLServer2 == null) {
            // 1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2in1_asymProtection_TokenInSubFalse.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            // 2 server reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_caller_asymProtection_TokenInSubFalse.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            testSAMLServer.reconfigServer(buildSPServerName("server_1_asymProtection.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        }
    	
    	
        // Create the conversation object which will maintain state for us
        String webServiceName = "FatSamlC04Service";
        String webServicePort = "SamlCallerToken04";
        String policyName = "CallerHttpsPolicy";
        String titleToCheck = "CXF SAML Caller Service Client";
        String partToCheck = "pass:false::FatSamlC04Service";
        String testMode = "positive";
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings("testCxfCallerHttpsPolicy_IncludeTokenInSubjectIsFalse", "cxf", servicePort, serviceSecurePort, userName, userPass, webServiceName, webServicePort, "", "False", null, null);
        updatedTestSettings.getCXFSettings().setBodyPartToCheck(partToCheck);
        updatedTestSettings.getCXFSettings().setTitleToCheck(titleToCheck);
        updatedTestSettings.getCXFSettings().setTestMode(testMode);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));
        
    }
    
}
