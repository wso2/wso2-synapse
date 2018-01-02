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
package org.apache.synapse.transport.passthru.config;

import junit.framework.Assert;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.passthru.HttpGetRequestProcessor;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.PassThroughTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for SourceConfiguration.
 */
public class SourceConfigurationTest {

    private static final int DEFAULT_WORKER_POOL_SIZE_CORE = 40;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8285;
    private PassThroughConfiguration passThroughConfiguration = PassThroughTestUtils.getPassThroughConfiguration();
    private static TransportInDescription transportInDescription = new TransportInDescription("http");
    private static final String HTTP_GET_PROCESSOR =
            "org.apache.synapse.transport.passthru.api.PassThroughNHttpGetProcessor";
    private static final String INVALID_HTTP_GET_PROCESSOR = "org.apache.synapse.transport.InvalidClass";
    private static final String INCORRECT_HTTP_GET_PROCESSOR =
            "org.apache.synapse.transport.nhttp.DefaultHttpGetProcessor";
    private static final String WSDLPREFIX = "http://apachehost:" + PORT + "/somepath";
    private ConfigurationContext cfgCtx = null;
    private Scheme scheme = null;
    private SourceConfiguration sourceConfiguration = null;
    private PassThroughTransportMetricsCollector metrics = null;

    @Before
    public void setUp() throws Exception {
        Parameter portParam = new Parameter("port", PORT);
        portParam.setParameterElement(
                AXIOMUtil.stringToOM("<parameter name=\"port\" locked=\"false\">" + PORT + "</parameter>"));
        Parameter hostParam = new Parameter("hostname", HOST);
        transportInDescription.addParameter(portParam);
        transportInDescription.addParameter(hostParam);
        cfgCtx = new ConfigurationContext(new AxisConfiguration());
        cfgCtx.setServicePath("services");
        cfgCtx.setContextRoot("/");
        scheme = new Scheme(transportInDescription.getName(), PORT, false);
        metrics = new PassThroughTransportMetricsCollector(true, scheme.getName());
        sourceConfiguration = new SourceConfiguration(cfgCtx, transportInDescription,
                scheme, PassThroughTestUtils.getWorkerPool(passThroughConfiguration), metrics);
        sourceConfiguration.build();
    }

    @After
    public void tearDown() throws Exception {
        Parameter wsdlPrefix = transportInDescription.getParameter("WSDLEPRPrefix");
        Parameter httpGetProcessor = transportInDescription.getParameter("httpGetProcessor");
        if (wsdlPrefix != null) {
            transportInDescription.removeParameter(wsdlPrefix);
        }
        if (httpGetProcessor != null) {
            transportInDescription.removeParameter(httpGetProcessor);
        }
    }

    @Test
    public void testBuild() throws Exception {
        Assert.assertNotNull("Building base configuration isn't successful.", sourceConfiguration);
        Assert.assertNotNull("Worker pool hasn't been initialized.", sourceConfiguration.getWorkerPool());
        Assert.assertNotSame("Worker thread count hasn't been taken from passthru-http.properties file",
                passThroughConfiguration.getWorkerPoolCoreSize(), DEFAULT_WORKER_POOL_SIZE_CORE);
    }

    @Test
    public void testGetHttpParams() throws Exception {
        HttpParams httpParams = sourceConfiguration.getHttpParams();
        Assert.assertNotNull("HTTP Parameters are null.", httpParams);
        String originServer = (String) httpParams.getParameter(HttpProtocolParams.ORIGIN_SERVER);
        Assert.assertEquals("Origin Server isn't correct.", "WSO2-PassThrough-HTTP", originServer);

    }

    @Test
    public void testGetIOReactorConfig() throws Exception {
        IOReactorConfig config = sourceConfiguration.getIOReactorConfig();
        int expectedIOThreadCount = Runtime.getRuntime().availableProcessors();
        Assert.assertNotNull("I/O Reactor hasn't been initialized.", config);
        Assert.assertEquals("I/O reactor thread count isn't correct.",
                expectedIOThreadCount, config.getIoThreadCount());
    }

    @Test
    public void testGetHttpProcessor() throws Exception {
        Assert.assertNotNull("HttpProcessor hasn't been initialized.", sourceConfiguration.getHttpProcessor());
    }

    @Test
    public void testGetResponseFactory() throws Exception {
        Assert.assertNotNull("ResponseFactory hasn't been initialized.", sourceConfiguration.getResponseFactory());
    }

    @Test
    public void testGetHostname() throws Exception {
        String hostName = sourceConfiguration.getHostname();
        Assert.assertNotNull("Host hasn't been initialized.", hostName);
        Assert.assertEquals("Host name doesn't match with : " + HOST, HOST, hostName);
    }

    @Test
    public void testGetPort() throws Exception {
        int port = sourceConfiguration.getPort();
        Assert.assertNotNull("Host hasn't been initialized.", port);
        Assert.assertEquals("Port doesn't match with : " + PORT, PORT, port);
    }

    @Test
    public void testGetSourceConnections() throws Exception {
        Assert.assertNotNull("Source Connections hasn't been initialized.", sourceConfiguration.getSourceConnections());
    }

    @Test
    public void testGetInDescription() throws Exception {
        Assert.assertNotNull("ResponseFactory hasn't been initialized.", sourceConfiguration.getInDescription());
    }

    @Test
    public void testGetScheme() throws Exception {
        Scheme scheme = sourceConfiguration.getScheme();
        String expectedSchemeName = transportInDescription.getName();
        Assert.assertNotNull("ResponseFactory hasn't been initialized.", scheme);
        Assert.assertEquals("Scheme doesn't match with : " + expectedSchemeName, expectedSchemeName, scheme.getName());
    }

    @Test
    public void testGetServiceEPRPrefix() throws Exception {
        String serviceEPPrefix = sourceConfiguration.getServiceEPRPrefix();
        Assert.assertNotNull("Service EPR prefix hasn't been initialized.", serviceEPPrefix);
        Assert.assertEquals("Service Endpoint prefix isn't correct.",
                scheme.getName() + "://" + HOST + "/services/", serviceEPPrefix);
    }

    @Test
    public void testGetServiceEPWithWSDLPrefix() throws Exception {
        Parameter wsdlPrefix = new Parameter("WSDLEPRPrefix", WSDLPREFIX);
        wsdlPrefix.setParameterElement(AXIOMUtil.stringToOM(
                "<parameter name=\"WSDLEPRPrefix\" locked=\"false\">" + WSDLPREFIX + "</parameter>"));
        transportInDescription.addParameter(wsdlPrefix);
        sourceConfiguration.build();
        String serviceEPPrefix = sourceConfiguration.getServiceEPRPrefix();
        Assert.assertNotNull("Service EPR prefix hasn't been initialized.", serviceEPPrefix);
        Assert.assertEquals("Service Endpoint with WDSLPrefix isn't correct.", WSDLPREFIX +
                "/services/", serviceEPPrefix);
    }

    @Test
    public void testGetCustomEPRPrefix() throws Exception {
        Assert.assertNotNull("Custom EPR prefix hasn't been initialized.", sourceConfiguration.getCustomEPRPrefix());
    }

    @Test
    public void testGetHttpGetRequestProcessor() throws Exception {
        Parameter httpGetProcessor = new Parameter("httpGetProcessor", HTTP_GET_PROCESSOR);
        httpGetProcessor.setParameterElement(AXIOMUtil.stringToOM("" +
                "<parameter name=\"httpGetProcessor\" locked=\"false\">" + HTTP_GET_PROCESSOR + "</parameter>"));
        transportInDescription.addParameter(httpGetProcessor);
        sourceConfiguration.build();
        HttpGetRequestProcessor httpGetRequestProcessor = sourceConfiguration.getHttpGetRequestProcessor();
        Assert.assertNotNull("httpGetProcessor hasn't been initialized.", httpGetRequestProcessor);
        Assert.assertEquals("Service Endpoint with WDSLPrefix isn't correct.",
                httpGetRequestProcessor.getClass().getName(), HTTP_GET_PROCESSOR);
    }

    @Test
    public void testFaultGetHttpGetRequestProcessor() {
        Parameter httpGetProcessor = new Parameter("httpGetProcessor", INCORRECT_HTTP_GET_PROCESSOR);
        try {
            httpGetProcessor.setParameterElement(AXIOMUtil.stringToOM(
                    "<parameter name=\"httpGetProcessor\" locked=\"false\">" +
                            INCORRECT_HTTP_GET_PROCESSOR + "</parameter>"));
            transportInDescription.addParameter(httpGetProcessor);
            sourceConfiguration.build();
        } catch (Exception ex) {
            Assert.assertNotNull("Error message is null", ex.getMessage());
            Assert.assertTrue("", ex.getMessage().contains("Error creating WSDL processor"));
        }
    }

    @Test
    public void testInvalidGetHttpGetRequestProcessor() {
        Parameter httpGetProcessor = new Parameter("httpGetProcessor", INVALID_HTTP_GET_PROCESSOR);
        try {
            httpGetProcessor.setParameterElement(AXIOMUtil.stringToOM(
                    "<parameter name=\"httpGetProcessor\" locked=\"false\">" +
                            INVALID_HTTP_GET_PROCESSOR + "</parameter>"));
            transportInDescription.addParameter(httpGetProcessor);
            sourceConfiguration.build();
        } catch (Exception ex) {
            Assert.assertNotNull("Error message is null", ex.getMessage());
            Assert.assertTrue("", ex.getMessage().contains("Error creating WSDL processor"));
        }
    }

    @Test
    public void testGetBooleanValue() throws Exception {
        boolean enableAdvancedForS2SView = sourceConfiguration.getBooleanValue(
                PassThroughConstants.SYNAPSE_PASSTHROUGH_S2SLATENCY_ADVANCE_VIEW, false);
        Assert.assertFalse(PassThroughConstants.SYNAPSE_PASSTHROUGH_S2SLATENCY_ADVANCE_VIEW + " is enabled",
                enableAdvancedForS2SView);
    }

    @Test
    public void testSourceConfiguration() throws Exception {
        WorkerPool workerPool = PassThroughTestUtils.getWorkerPool(passThroughConfiguration);
        SourceConfiguration sourceConfiguration = new SourceConfiguration(workerPool, metrics);
        Assert.assertNotNull("SourceConfiguration is null", sourceConfiguration);
    }

}