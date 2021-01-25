/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.threads.NativeWorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.ByteBuffer;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for TargetResponse
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({ TargetContext.class, TargetResponse.class })
public class TargetResponseTest extends TestCase {
    private static Log logger = LogFactory.getLog(TargetResponseTest.class.getName());

    /**
     * Testing the starting of target response when response body is not expected
     *
     * @throws Exception
     */
    @Test
    public void testFalse() throws Exception {
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
        PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");

        TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                metrics, null);
        HttpResponse response = PowerMockito.mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        NHttpClientConnection conn = PowerMockito.mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
        TargetConnections connections = PowerMockito.mock(TargetConnections.class);
        targetConfiguration.setConnections(connections);

        PowerMockito.mockStatic(TargetContext.class);

        TargetResponse targetResponse = new TargetResponse(targetConfiguration, response, conn, false, false);

        try {
            targetResponse.start(conn);
        } catch (Exception e) {
            logger.error(e);
            Assert.fail("Unable to start the target response!");
        }
    }

    /**
     * Testing the starting of target response when response body is expected
     *
     * @throws Exception
     */
    @Test
    public void testTrue() throws Exception {
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
        PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");

        TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                metrics, null);
        targetConfiguration.build();
        HttpResponse response = PowerMockito.mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        NHttpClientConnection conn = PowerMockito.mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);

        PowerMockito.mockStatic(TargetContext.class);

        TargetContext cntxt = new TargetContext(targetConfiguration);
        PowerMockito.when(TargetContext.get(any(NHttpClientConnection.class))).thenReturn(cntxt);

        TargetResponse targetResponse = new TargetResponse(targetConfiguration, response, conn, true, false);

        try {
            targetResponse.start(conn);
        } catch (Exception e) {
            logger.error(e);
            Assert.fail("Unable to start the target response!");
        }
    }

    /**
     * Testing reading from a pipe
     * @throws Exception
     */
    @Test
    public void testRead() throws Exception {
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
        PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");

        TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                metrics, null);
        targetConfiguration.build();
        HttpResponse response = PowerMockito.mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        NHttpClientConnection conn = PowerMockito.mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
        ContentDecoder decoder = PowerMockito.mock(ContentDecoder.class);
        TargetConnections connections = PowerMockito.mock(TargetConnections.class);
        targetConfiguration.setConnections(connections);

        PowerMockito.mockStatic(TargetContext.class);

        TargetContext cntxt = new TargetContext(targetConfiguration);
        PowerMockito.when(TargetContext.get(any(NHttpClientConnection.class))).thenReturn(cntxt);
        PowerMockito.when(decoder.read(any(ByteBuffer.class))).thenReturn(12);
        PowerMockito.when(decoder.isCompleted()).thenReturn(true);

        TargetResponse targetResponse = new TargetResponse(targetConfiguration, response, conn, true, false);
        targetResponse.start(conn);
        int result = targetResponse.read(conn, decoder);

        Assert.assertEquals(12, result);
    }

    /**
     * Testing the starting of target response when response body is not expected and Keep alive equal to true
     *
     * @throws Exception
     */
    @Mock
    DefaultConnectionReuseStrategy connStrategy;
    @Test
    public void testCompletingStateTransition() throws Exception {
        PowerMockito.whenNew(DefaultConnectionReuseStrategy.class).withNoArguments().thenReturn(connStrategy);
        PowerMockito.when(connStrategy.keepAlive(any(HttpResponse.class), any(HttpContext.class))).thenReturn(true);
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
        PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");

        TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                metrics, null);
        HttpResponse response = PowerMockito.mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
        NHttpClientConnection conn = PowerMockito.mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
        TargetConnections connections = PowerMockito.mock(TargetConnections.class);
        targetConfiguration.setConnections(connections);

        PowerMockito.mockStatic(TargetContext.class);

        TargetResponse targetResponse = new TargetResponse(targetConfiguration, response, conn, false, false);

        try {
            targetResponse.start(conn);
            PowerMockito.verifyStatic( Mockito.times(1)); // Verify that the following mock method was called exactly 1 time
            TargetContext.updateState(conn, ProtocolState.RESPONSE_DONE);


        } catch (Exception e) {
            logger.error(e);
            Assert.fail("Unable to start the target response!");
        }
    }
}