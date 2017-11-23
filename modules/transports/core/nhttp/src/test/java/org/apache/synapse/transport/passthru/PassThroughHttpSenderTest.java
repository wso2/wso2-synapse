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
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.transport.base.threads.NativeWorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.commons.logging.Log;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.util.BufferFactory;
import org.apache.synapse.transport.passthru.util.SourceResponseFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.caching.digest.DigestGenerator;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for PassThroughHttpSender
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({ PassThroughHttpSender.class, SourceContext.class, SourceResponseFactory.class})
public class PassThroughHttpSenderTest extends TestCase {

    @Mock(name = "deliveryAgent")
    private DeliveryAgent deliveryAgent;

    @Mock(name = "targetConfiguration")
    private TargetConfiguration targetConfiguration;

    @Mock(name = "digestGenerator")
    private DigestGenerator digestGenerator;

    @Mock(name = "log")
    Log log;

    @Mock
    BufferFactory factory;

    @InjectMocks
    PassThroughHttpSender sender;

    /**
     * This method tests the initialization of PassThroughHttpSender
     * @throws Exception
     */
    @Test
    public void testInit() throws Exception {
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
        configurationContext.setProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL, workerPool);
        TransportOutDescription transportOutDescription = new TransportOutDescription("passthru");

        PassThroughHttpSender passThroughHttpSender = new PassThroughHttpSender();

        passThroughHttpSender.init(configurationContext, transportOutDescription);
    }

    /**
     * This method tests the invoke of PassThroughHttpSender when
     * the endpoint reference is given in the message context
     * @throws Exception
     */
    @Test
    public void testInvoke() throws Exception {
        MockitoAnnotations.initMocks(this);
        MessageContext messageContext = new MessageContext();
        messageContext.setProperty(Constants.Configuration.TRANSPORT_URL, "http://sample.url");
        messageContext.setProperty(NhttpConstants.FORCE_HTTP_CONTENT_LENGTH, true);
        messageContext.setProperty(PassThroughConstants.COPY_CONTENT_LENGTH_FROM_INCOMING, true);
        messageContext.setProperty(PassThroughConstants.ORGINAL_CONTEN_LENGTH, "1000");
        messageContext.setOperationContext(new OperationContext());

        sender = PowerMockito.spy(sender);

        PowerMockito.when(targetConfiguration.getBufferFactory()).thenReturn(factory);
        PowerMockito.doNothing().when(sender, "sendRequestContent", any(MessageContext.class));

        Handler.InvocationResponse response = sender.invoke(messageContext);

        Assert.assertNotNull("PassThrough Http Sender not invoked!", response);
    }

    /**
     * This method tests the submitting of response when the source request is null
     * @throws Exception
     */
    @Test
    public void testSubmitResponse() throws Exception {
        MockitoAnnotations.initMocks(this);
        MessageContext messageContext = new MessageContext();
        messageContext.setProperty(Constants.Configuration.TRANSPORT_URL, "http://sample.url");
        messageContext.setProperty(NhttpConstants.FORCE_HTTP_CONTENT_LENGTH, true);
        messageContext.setProperty(PassThroughConstants.COPY_CONTENT_LENGTH_FROM_INCOMING, true);
        messageContext.setProperty(PassThroughConstants.ORGINAL_CONTEN_LENGTH, "1000");
        messageContext.setOperationContext(new OperationContext());

        IOSession session = PowerMockito.mock(IOSession.class);
        NHttpServerConnection conn = new DefaultNHttpServerConnection(session, 12);

        messageContext.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION, conn);
        messageContext.setProperty(PassThroughConstants.HTTP_ETAG_ENABLED, true);

        Map headers = new HashMap();
        headers.put("randomKey", "randomValue");

        messageContext.setProperty(MessageContext.TRANSPORT_HEADERS, headers);

        sender = PowerMockito.spy(sender);

        PowerMockito.when(digestGenerator.getDigest(any(MessageContext.class))).thenReturn("testString");
        sender.submitResponse(messageContext);
    }
}