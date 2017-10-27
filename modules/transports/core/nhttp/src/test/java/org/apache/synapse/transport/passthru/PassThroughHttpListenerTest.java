/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.transport.passthru;

import junit.framework.Assert;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.transport.utils.TCPUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test class for PassThroughHttpListener.
 */
public class PassThroughHttpListenerTest {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8285;
    private static PassThroughHttpListener passThroughHttpListener = new PassThroughHttpListener();
    private static TransportInDescription transportInDescription = new TransportInDescription("http");

    @BeforeClass()
    public static void startListener() throws Exception {
        Assert.assertFalse("Port already occupied. Can not execute test", TCPUtils.isPortOpen(PORT, HOST));
        Parameter portParam = new Parameter("port", PORT);
        portParam.setParameterElement(
                AXIOMUtil.stringToOM("<parameter name=\"port\" locked=\"false\">" + PORT + "</parameter>"));
        Parameter hostParam = new Parameter("hostname", HOST);
        transportInDescription.addParameter(portParam);
        transportInDescription.addParameter(hostParam);
        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        cfgCtx.setServicePath("services");
        cfgCtx.setContextRoot("/");
        passThroughHttpListener.init(cfgCtx, transportInDescription);
        passThroughHttpListener.start();
        Assert.assertTrue("Listener port not open", TCPUtils.isPortOpen(PORT, HOST));
    }

    @Test
    public void testGetTransportName() throws Exception {
        Assert.assertEquals("Transport name mismatched", "http", passThroughHttpListener.getTransportName());
    }

    @Test
    public void testGetEPRForService() throws Exception {
        Assert.assertEquals("Service URL mismatched", getServiceEndpoint("myservice"),
                passThroughHttpListener.getEPRForService("myservice", HOST).getAddress());
        Assert.assertEquals("Service URL mismatched", getServiceEndpoint("myservice.endpoint"),
                passThroughHttpListener.getEPRForService("myservice.endpoint", HOST).getAddress());
        Assert.assertEquals("Service URL mismatched", getServiceEndpoint("t/myservice"),
                passThroughHttpListener.getEPRForService("t/myservice", HOST).getAddress());
    }

    @Test
    public void testGetEPRsForService() throws Exception {
        EndpointReference[] endpointReference = passThroughHttpListener.getEPRsForService("myservice", HOST);
        Assert.assertNotNull(endpointReference);
        Assert.assertEquals("Endpoint reference mismatched", getServiceEndpoint("myservice"),
                endpointReference[0].getAddress());

        endpointReference = passThroughHttpListener.getEPRsForService("t/myservice", HOST);
        Assert.assertNotNull(endpointReference);
        Assert.assertEquals("Endpoint reference mismatched", getServiceEndpoint("t/myservice"),
                endpointReference[0].getAddress());

        endpointReference = passThroughHttpListener.getEPRsForService("myservice.endpoint", HOST);
        Assert.assertNotNull(endpointReference);
        Assert.assertEquals("Endpoint reference mismatched", getServiceEndpoint("myservice.endpoint"),
                endpointReference[0].getAddress());
    }

    @Test
    public void testGetSessionContext() throws Exception {
        MessageContext messageContext = Mockito.mock(MessageContext.class);
        SessionContext sessionContext = passThroughHttpListener.getSessionContext(messageContext);
        Assert.assertNull("SessionContext mut be null", sessionContext);
    }

    @Test
    public void testReloadSpecificEndPoints() throws Exception {
        passThroughHttpListener.reloadSpecificEndPoints(transportInDescription);
    }

    @Test
    public void testReload() throws Exception {
        passThroughHttpListener.reload(transportInDescription);
    }

    @AfterClass()
    public static void stopListener() throws Exception {
        passThroughHttpListener.stop();
        Assert.assertFalse("Listener port not closed", TCPUtils.isPortOpen(PORT, HOST));
    }

    private String getServiceEndpoint(String service) {
        return "http://" + HOST + ":" + PORT + "/services/" + service;
    }
}
