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

import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.threads.NativeWorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.transport.http.conn.ClientConnFactory;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.HostConnections;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test class for TargetHandler
 */
public class TargetHandlerTest extends TestCase {
    private static Log logger = LogFactory.getLog(TargetHandlerTest.class.getName());

    /**
     * Testing whether request-ready connection is processed
     *
     * @throws Exception
     */
    @Test
    public void testRequestReady() throws Exception {
        try (MockedStatic<TargetContext> targetContextMock = mockStatic(TargetContext.class)) {
            DeliveryAgent deliveryAgent = mock(DeliveryAgent.class);
            ClientConnFactory connFactory = mock(ClientConnFactory.class);
            TargetConfiguration configuration = mock(TargetConfiguration.class);
            TargetHandler targetHandler = new TargetHandler(deliveryAgent, connFactory, configuration);
            NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
            HttpContext context = mock(HttpContext.class);
            when(conn.getContext()).thenReturn(context);

            targetContextMock.when(() -> TargetContext.getState(any(NHttpClientConnection.class)))
                    .thenReturn(ProtocolState.REQUEST_READY);

            targetHandler.requestReady(conn);
        }
    }

    /**
     * Testing whether output-ready connection is processed
     *
     * @throws Exception
     */
    @Test
    public void testOutputReady() throws Exception {
        try (MockedStatic<TargetContext> targetContextMock = mockStatic(TargetContext.class)) {
            DeliveryAgent deliveryAgent = mock(DeliveryAgent.class);
            HostConnections pool = new HostConnections(new RouteRequestMapping(new HttpRoute(new HttpHost("localhost")), ""), 1024);
            HttpContext context = mock(HttpContext.class);
            context.setAttribute("CONNECTION_POOL", pool);
            ClientConnFactory connFactory = mock(ClientConnFactory.class);
            ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
            WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
            PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");
            TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                    metrics, null);
            TargetContext targetContext = new TargetContext(targetConfiguration);
            MessageContext messageContext = new MessageContext();
            targetContext.setRequestMsgCtx(messageContext);
            TargetHandler targetHandler = new TargetHandler(deliveryAgent, connFactory, targetConfiguration);
            TargetRequest request = mock(TargetRequest.class);
            NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
            ContentEncoder encoder = mock(ContentEncoder.class);

            targetContextMock.when(() -> TargetContext.get(conn)).thenReturn(targetContext);
            targetContextMock.when(() -> TargetContext.getState(conn)).thenReturn(ProtocolState.REQUEST_HEAD);
            targetContextMock.when(() -> TargetContext.getRequest(conn)).thenReturn(request);

            when(request.hasEntityBody()).thenReturn(true);
            when(conn.getContext()).thenReturn(context);
            when(request.write(conn, encoder)).thenReturn(12);
            when(encoder.isCompleted()).thenReturn(true);

            targetHandler.outputReady(conn, encoder);
        }
    }

    /**
     * Testing whether input-ready connection is processed
     *
     * @throws Exception
     */
    @Test
    public void testInputReady() throws Exception {
        try (MockedStatic<TargetContext> targetContextMock = mockStatic(TargetContext.class)) {
            DeliveryAgent deliveryAgent = mock(DeliveryAgent.class);
            ClientConnFactory connFactory = mock(ClientConnFactory.class);
            HostConnections pool = new HostConnections(new RouteRequestMapping(new HttpRoute(new HttpHost("localhost")), ""), 1024);
            HttpContext context = mock(HttpContext.class);
            context.setAttribute("CONNECTION_POOL", pool);
            ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
            WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
            PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");
            TargetConfiguration targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool,
                    metrics, null);
            TargetContext targetContext = new TargetContext(targetConfiguration);
            MessageContext messageContext = new MessageContext();
            targetContext.setRequestMsgCtx(messageContext);
            TargetHandler targetHandler = new TargetHandler(deliveryAgent, connFactory, targetConfiguration);
            TargetResponse response = mock(TargetResponse.class);
            NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
            ContentDecoder decoder = mock(ContentDecoder.class);

            targetContextMock.when(() -> TargetContext.get(conn)).thenReturn(targetContext);
            targetContextMock.when(() -> TargetContext.getState(conn)).thenReturn(ProtocolState.RESPONSE_HEAD);
            targetContextMock.when(() -> TargetContext.getResponse(conn)).thenReturn(response);

            when(conn.getContext()).thenReturn(context);
            when(decoder.isCompleted()).thenReturn(true);

            targetHandler.inputReady(conn, decoder);
        }
    }
}
