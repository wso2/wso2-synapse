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
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.threads.NativeWorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.transport.http.conn.ClientConnFactory;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test class for TargetHandler
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(TargetContext.class)
public class TargetHandlerTest extends TestCase {
    private static Log logger = LogFactory.getLog(TargetHandlerTest.class.getName());

    /**
     * Testing whether request-ready connection is processed
     *
     * @throws Exception
     */
    @Test
    public void testRequestReady() throws Exception {
        DeliveryAgent deliveryAgent = mock(DeliveryAgent.class);
        ClientConnFactory connFactory = mock(ClientConnFactory.class);
        TargetConfiguration configuration = mock(TargetConfiguration.class);
        TargetHandler targetHandler = new TargetHandler(deliveryAgent, connFactory, configuration);
        NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
        HttpContext context = mock(HttpContext.class);
        when(conn.getContext()).thenReturn(context);
        mockStatic(TargetContext.class);
        when(TargetContext.getState(any(NHttpClientConnection.class))).thenReturn(ProtocolState.REQUEST_READY);
        targetHandler.requestReady(conn);
    }

    /**
     * Testing whether output-ready connection is processed
     *
     * @throws Exception
     */
    @Test
    public void testOutputReady() throws Exception {
        DeliveryAgent deliveryAgent = mock(DeliveryAgent.class);
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
        mockStatic(TargetContext.class);
        when(TargetContext.get(conn)).thenReturn(targetContext);
        when(TargetContext.getState(conn)).thenReturn(ProtocolState.REQUEST_HEAD);
        when(TargetContext.getRequest(conn)).thenReturn(request);
        when(request.hasEntityBody()).thenReturn(true);
        when(request.write(conn, encoder)).thenReturn(12);
        when(encoder.isCompleted()).thenReturn(true);
        targetHandler.outputReady(conn, encoder);

    }

    /**
     * Testing whether input-ready connection is processed
     *
     * @throws Exception
     */
    @Test
    public void testInputReady() throws Exception {
        DeliveryAgent deliveryAgent = mock(DeliveryAgent.class);
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
        TargetResponse response = mock(TargetResponse.class);
        NHttpClientConnection conn = mock(NHttpClientConnection.class, Mockito.RETURNS_DEEP_STUBS);
        ContentDecoder decoder = mock(ContentDecoder.class);
        mockStatic(TargetContext.class);
        when(TargetContext.get(conn)).thenReturn(targetContext);
        when(TargetContext.getState(conn)).thenReturn(ProtocolState.RESPONSE_HEAD);
        when(TargetContext.getResponse(conn)).thenReturn(response);
        when(decoder.isCompleted()).thenReturn(true);
        targetHandler.inputReady(conn, decoder);
        
    }
}