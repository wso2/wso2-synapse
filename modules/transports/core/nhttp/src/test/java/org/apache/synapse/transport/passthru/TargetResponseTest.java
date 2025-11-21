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
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.HostConnections;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.ByteBuffer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test class for TargetResponse
 */
public class TargetResponseTest extends TestCase {
    private static Log logger = LogFactory.getLog(TargetResponseTest.class.getName());

    /**
     * Testing the starting of target response when response body is not expected
     *
     * @throws Exception
     */
    @Test
    public void testFalse() throws Exception {
        try (MockedStatic<TargetContext> targetContextMock = mockStatic(TargetContext.class)) {
            ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
            WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
            PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");

            TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                    metrics, null);
            HttpResponse response = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
            NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
            TargetConnections connections = mock(TargetConnections.class);
            targetConfiguration.setConnections(connections);

            HttpRequest httpRequest = new BasicHttpRequest("GET", "test.com");
            when(conn.getContext().getAttribute("http.request")).thenReturn(httpRequest);

            TargetResponse targetResponse = new TargetResponse(targetConfiguration, response, conn, false, false);

            try {
                targetResponse.start(conn);
            } catch (Exception e) {
                logger.error(e);
                Assert.fail("Unable to start the target response!");
            }
        }
    }

    /**
     * Testing the starting of target response when response body is expected
     *
     * @throws Exception
     */
    @Test
    public void testTrue() throws Exception {
        try (MockedStatic<TargetContext> targetContextMock = mockStatic(TargetContext.class)) {
            ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
            WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
            PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");

            TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                    metrics, null);
            targetConfiguration.build();
            HttpResponse response = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
            NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);

            TargetContext cntxt = new TargetContext(targetConfiguration);
            targetContextMock.when(() -> TargetContext.get(any(NHttpClientConnection.class))).thenReturn(cntxt);

            TargetResponse targetResponse = new TargetResponse(targetConfiguration, response, conn, true, false);

            try {
                targetResponse.start(conn);
            } catch (Exception e) {
                logger.error(e);
                Assert.fail("Unable to start the target response!");
            }
        }
    }

    /**
     * Testing reading from a pipe
     * @throws Exception
     */
    @Test
    public void testRead() throws Exception {
        try (MockedStatic<TargetContext> targetContextMock = mockStatic(TargetContext.class)) {
            ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
            WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
            PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");

            HostConnections pool = new HostConnections(new RouteRequestMapping(new HttpRoute(new HttpHost("localhost")), ""), 1024);
            HttpContext context = mock(HttpContext.class);
            context.setAttribute("CONNECTION_POOL", pool);

            TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                    metrics, null);
            targetConfiguration.build();
            HttpResponse response = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
            NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
            ContentDecoder decoder = mock(ContentDecoder.class);
            TargetConnections connections = mock(TargetConnections.class);
            targetConfiguration.setConnections(connections);

            HttpRequest httpRequest = new BasicHttpRequest("GET", "test.com");
            when(conn.getContext().getAttribute("http.request")).thenReturn(httpRequest);

            TargetContext cntxt = new TargetContext(targetConfiguration);
            targetContextMock.when(() -> TargetContext.get(any(NHttpClientConnection.class))).thenReturn(cntxt);
            when(decoder.read(any(ByteBuffer.class))).thenReturn(12, -1);
            when(decoder.isCompleted()).thenReturn(true);
            when(conn.getContext()).thenReturn(context);

            TargetResponse targetResponse = new TargetResponse(targetConfiguration, response, conn, true, false);
            targetResponse.start(conn);
            int result = targetResponse.read(conn, decoder);

            Assert.assertEquals(12, result);
        }
    }

    /**
     * Testing the starting of target response when response body is not expected and Keep alive equal to true
     *
     * @throws Exception
     */
    @Test
    public void testCompletingStateTransition() throws Exception {
        try (MockedStatic<TargetContext> targetContextMock = mockStatic(TargetContext.class);
             MockedConstruction<DefaultConnectionReuseStrategy> connStrategyMock = mockConstruction(
                     DefaultConnectionReuseStrategy.class,
                     (mock, context) -> {
                         when(mock.keepAlive(any(HttpResponse.class), any(HttpContext.class))).thenReturn(true);
                     })) {

            ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
            WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
            PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");

            TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                    metrics, null);
            HttpResponse response = mock(HttpResponse.class, Mockito.RETURNS_DEEP_STUBS);
            NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
            TargetConnections connections = mock(TargetConnections.class);
            targetConfiguration.setConnections(connections);

            TargetResponse targetResponse = new TargetResponse(targetConfiguration, response, conn, false, false);

            try {
                targetResponse.start(conn);
                // Verify that the following method was called exactly 1 time
                targetContextMock.verify(() -> TargetContext.updateState(conn, ProtocolState.RESPONSE_DONE),
                        Mockito.times(1));

            } catch (Exception e) {
                logger.error(e);
                Assert.fail("Unable to start the target response!");
            }
        }
    }
}
