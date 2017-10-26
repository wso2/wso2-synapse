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
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.synapse.transport.utils.TCPUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class for PassThroughHttpListener.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AxisEngine.class)
@PowerMockIgnore("javax.management.*")
public class SourceHandlerTest {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8286;
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
    public void testRequestAck() throws Exception {
        PowerMockito.mockStatic(AxisEngine.class);
        PowerMockito.doNothing().doThrow(new RuntimeException()).when(AxisEngine.class);

        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(getServiceEndpoint("myservice"));
        method.setRequestHeader("Content-Type", "application/xml");
        StringRequestEntity stringRequestEntity = new StringRequestEntity("<msg>hello</msg>", "application/xml",
                "UTF-8");
        method.setRequestEntity(stringRequestEntity);
        int responseCode = client.executeMethod(method);
        method.getResponseBodyAsString();
        Assert.assertEquals("Response code mismatched", 202, responseCode);
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
