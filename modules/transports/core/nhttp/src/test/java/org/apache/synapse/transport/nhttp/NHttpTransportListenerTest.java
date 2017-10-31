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
package org.apache.synapse.transport.nhttp;

import junit.framework.Assert;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.dispatchers.RequestURIBasedDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.synapse.transport.nhttp.util.RESTUtil;
import org.apache.synapse.transport.utils.ServiceUtils;
import org.apache.synapse.transport.utils.TCPUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for nhttp Transport Listener.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AxisEngine.class, RESTUtil.class })
@PowerMockIgnore("javax.management.*")
public class NHttpTransportListenerTest {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8486;
    private static HttpCoreNIOListener nHttpListener = new HttpCoreNIOListener();
    private static TransportInDescription transportInDescription = new TransportInDescription("http");

    @BeforeClass()
    public static void startListener() throws Exception {
        Assert.assertFalse("Port already occupied. Can not execute test", TCPUtils.isPortOpen(PORT, HOST));
        Parameter portParam = new Parameter("port", PORT + "");
        portParam.setParameterElement(
                AXIOMUtil.stringToOM("<parameter name=\"port\" locked=\"false\">" + PORT + "</parameter>"));
        Parameter hostParam = new Parameter("hostname", HOST);
        transportInDescription.addParameter(portParam);
        transportInDescription.addParameter(hostParam);
        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        cfgCtx.setServicePath("services");
        cfgCtx.setContextRoot("/");
        nHttpListener.init(cfgCtx, transportInDescription);
        nHttpListener.start();
        Assert.assertTrue("Listener port not open", TCPUtils.isPortOpen(PORT, HOST, 2000));
    }

    /**
     * Test the service endpoint url.
     *
     * @throws Exception
     */
    @Test
    public void testGetEPRForService() throws Exception {
        Assert.assertEquals("Service URL mismatched", ServiceUtils.getServiceEndpoint("myservice", HOST, PORT),
                nHttpListener.getEPRForService("myservice", HOST).getAddress());
        Assert.assertEquals("Service URL mismatched", ServiceUtils.getServiceEndpoint("myservice.endpoint", HOST, PORT),
                nHttpListener.getEPRForService("myservice.endpoint", HOST).getAddress());
        Assert.assertEquals("Service URL mismatched", ServiceUtils.getServiceEndpoint("t/myservice", HOST, PORT),
                nHttpListener.getEPRForService("t/myservice", HOST).getAddress());
    }

    /**
     * Test the service endpoints.
     *
     * @throws Exception
     */
    @Test
    public void testGetEPRsForService() throws Exception {
        EndpointReference[] endpointReference = nHttpListener.getEPRsForService("myservice", HOST);
        Assert.assertNotNull(endpointReference);
        Assert.assertEquals("Endpoint reference mismatched", ServiceUtils.getServiceEndpoint("myservice", HOST, PORT),
                endpointReference[0].getAddress());

        endpointReference = nHttpListener.getEPRsForService("t/myservice", HOST);
        Assert.assertNotNull("Endpoint references not found", endpointReference);
        Assert.assertEquals("Endpoint reference mismatched", ServiceUtils.getServiceEndpoint("t/myservice", HOST, PORT),
                endpointReference[0].getAddress());

        endpointReference = nHttpListener.getEPRsForService("myservice.endpoint", HOST);
        Assert.assertNotNull("Endpoint references not found", endpointReference);
        Assert.assertEquals("Endpoint reference mismatched",
                ServiceUtils.getServiceEndpoint("myservice.endpoint", HOST, PORT), endpointReference[0].getAddress());
    }

    /**
     * Test the session Context.
     *
     * @throws Exception
     */
    @Test
    public void testGetSessionContext() throws Exception {
        MessageContext messageContext = Mockito.mock(MessageContext.class);
        SessionContext sessionContext = nHttpListener.getSessionContext(messageContext);
        Assert.assertNull("SessionContext mut be null", sessionContext);
    }

    /**
     * Test the Source Handler respond to client.
     * Send a message to http listener and get the same request message as a response using PassThroughHttpSender
     */
    @Test
    public void testRequestAndResponse() throws Exception {
        RequestURIBasedDispatcher requestURIBasedDispatcher = Mockito.mock(RequestURIBasedDispatcher.class);
        Mockito.when(requestURIBasedDispatcher.findService(any(MessageContext.class)))
                .thenReturn(new AxisService("myservice"));
        PowerMockito.whenNew(RequestURIBasedDispatcher.class).withNoArguments().thenReturn(requestURIBasedDispatcher);
        PowerMockito.mockStatic(AxisEngine.class);
        PowerMockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Exception {
                MessageContext axis2MessageContext = invocation.getArgument(0);
                ServiceContext svcCtx = new ServiceContext();
                OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
                axis2MessageContext.setServiceContext(svcCtx);
                axis2MessageContext.setOperationContext(opCtx);
                axis2MessageContext.getOperationContext()
                        .setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "SKIP");
                axis2MessageContext.setTo(null);
                axis2MessageContext.setProperty("synapse.isresponse", true);
                HttpCoreNIOSender sender = new HttpCoreNIOSender();
                ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
                sender.init(cfgCtx, new TransportOutDescription("http"));
                sender.invoke(axis2MessageContext);
                return null;
            }
        }).when(AxisEngine.class, "receive", any(MessageContext.class));

        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(ServiceUtils.getServiceEndpoint("myservice", HOST, PORT));
        method.setRequestHeader("Content-Type", "application/xml");
        StringRequestEntity stringRequestEntity = new StringRequestEntity("<msg>hello</msg>", "application/xml",
                "UTF-8");
        method.setRequestEntity(stringRequestEntity);
        int responseCode = client.executeMethod(method);
        Assert.assertEquals("Response code mismatched", 200, responseCode);
        String response = method.getResponseBodyAsString();
        Assert.assertEquals("Response", "<msg>hello</msg>", response);
    }

    @AfterClass()
    public static void stopListener() throws Exception {
        nHttpListener.stop();
        Assert.assertFalse("Listener port not closed", TCPUtils.isPortOpen(PORT, HOST));
    }
}
