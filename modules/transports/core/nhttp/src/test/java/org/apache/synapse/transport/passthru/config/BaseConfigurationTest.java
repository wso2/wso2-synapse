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
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.ControlledByteBuffer;
import org.apache.synapse.transport.passthru.util.PassThroughTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for BaseConfiguration.
 */
public class BaseConfigurationTest {

    private static final int DEFAULT_WORKER_POOL_SIZE_CORE = 40;
    private static final int DEFAULT_IO_BUFFER_SIZE = 8 * 1024;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8285;
    private PassThroughConfiguration conf = PassThroughTestUtils.getPassThroughConfiguration();
    private static TransportInDescription transportInDescription = new TransportInDescription("http");
    private ConfigurationContext cfgCtx = null;
    private Scheme scheme = null;
    private BaseConfiguration baseConfiguration = null;
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
        baseConfiguration = new SourceConfiguration(cfgCtx, transportInDescription,
                scheme, PassThroughTestUtils.getWorkerPool(conf), metrics);
        baseConfiguration.build();
    }

    @Test
    public void testBuild() throws Exception {
        Assert.assertNotNull("Building base configuration isn't successful.", baseConfiguration);
        Assert.assertNotNull("Worker pool hasn't been initialized.", baseConfiguration.getWorkerPool());
        Assert.assertNotSame("Worker thread count hasn't been taken from passthru-http.properties file",
                conf.getWorkerPoolCoreSize(), DEFAULT_WORKER_POOL_SIZE_CORE);

    }

    @Test
    public void testGetWorkerPool() throws Exception {
        Assert.assertNotNull("Worker pool hasn't been initialized.", baseConfiguration.getWorkerPool());
    }

    @Test
    public void testGetIOBufferSize() throws Exception {
        Assert.assertNotNull("IO Buffer hasn't been initialized.", baseConfiguration.getIOBufferSize());
        Assert.assertNotSame("IO buffer size hasn't been taken from passthru-http.properties file",
                conf.getIOBufferSize(), DEFAULT_IO_BUFFER_SIZE);
    }

    @Test
    public void testGetConfigurationContext() throws Exception {
        Assert.assertNotNull("Configuration context hasn't been initialized.",
                baseConfiguration.getConfigurationContext());
    }

    @Test
    public void testBuildHttpParams() throws Exception {
        HttpParams httpParams = baseConfiguration.buildHttpParams();
        Assert.assertNotNull("HTTP Parameters hasn't been initialized.", httpParams);
        String originServer = (String) httpParams.getParameter(HttpProtocolParams.ORIGIN_SERVER);
        Assert.assertEquals("Origin Server isn't correct.", "WSO2-PassThrough-HTTP", originServer);
    }

    @Test
    public void testBuildIOReactorConfig() throws Exception {
        IOReactorConfig config = baseConfiguration.buildIOReactorConfig();
        int expectedIOThreadCount = Runtime.getRuntime().availableProcessors();
        Assert.assertNotNull("I/O Reactor hasn't been initialized.", config);
        Assert.assertEquals("I/O reactor thread count isn't correct.",
                expectedIOThreadCount, config.getIoThreadCount());
    }

    @Test
    public void testGetBufferFactory() throws Exception {
        Assert.assertNotNull("BufferFactor is null.", baseConfiguration.getBufferFactory());
        Assert.assertTrue("Buffer isn't an instance of ControlledByteBuffer.",
                baseConfiguration.getBufferFactory().getBuffer() instanceof ControlledByteBuffer);
    }

    @Test
    public void testGetMetrics() throws Exception {
        Assert.assertNotNull("Metrics hasn't been initialized.", baseConfiguration.getMetrics());
    }

    @Test
    public void testBuildwithDefaultWorkerPool() throws Exception {
        baseConfiguration = new SourceConfiguration(cfgCtx, transportInDescription, scheme, null, metrics);
        baseConfiguration.build();
        Assert.assertNotNull("Building base configuration isn't successful.", baseConfiguration);
        Assert.assertNotNull("Worker pool hasn't been initialized.", baseConfiguration.getWorkerPool());
    }

    @Test
    public void testDefaultWorkerPool() throws Exception {
        WorkerPool workerPool = baseConfiguration.getWorkerPool(0, 0, 0, 0, null, null);
        Assert.assertNotNull("Worker pool hasn't been initialized.", workerPool);
    }

}