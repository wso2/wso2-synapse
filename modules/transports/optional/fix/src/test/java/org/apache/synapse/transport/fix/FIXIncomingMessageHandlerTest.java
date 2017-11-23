/*
 *     Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *     WSO2 Inc. licenses this file to you under the Apache License,
 *     Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.synapse.transport.fix;

import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.threads.NativeWorkerPool;
import org.junit.Assert;
import org.junit.Test;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.BeginString;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;
import quickfix.fix41.NewOrderSingle;

import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({ FIXIncomingMessageHandler.class })
public class FIXIncomingMessageHandlerTest extends TestCase {
    @Mock
    AxisService service;

    @Test
    public void testInitialization() throws Exception {
        AxisService axisService = new AxisService("testFIXService");
        axisService.addParameter(new Parameter(FIXConstants.FIX_ACCEPTOR_EVENT_HANDLER, "randomClass"));
        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        WorkerPool pool = new NativeWorkerPool(3, 4, 10, 10, "name", "id");

        FIXIncomingMessageHandler handler = new FIXIncomingMessageHandler(cfgCtx, pool, axisService, true);

        Assert.assertNotNull("FIXIncomingMessageHandler not initialized!", handler);

    }

    @Test
    public void testToAdmin() throws Exception {
        initMocks(this);
        SessionID id = new SessionID(new BeginString("FIX.4.1"), new SenderCompID("SYNAPSE"),
                new TargetCompID("BANZAI"), "FIX.4.1:SYNAPSE->BANZAI");

        Message message = new NewOrderSingle();
        message.getHeader().setField(new BeginString("FIX.4.1"));
        message.getHeader().setField(new SenderCompID("SYNAPSE"));
        message.getHeader().setField(new TargetCompID("BANZAI"));
        message.getHeader().setField(new MsgSeqNum(1));
        message.getHeader().setField(new MsgType("A"));

        PowerMockito.when(service.getParameter(FIXConstants.FIX_USERNAME))
                .thenReturn(new Parameter(FIXConstants.FIX_USERNAME, "wos2"));
        PowerMockito.when(service.getParameter(FIXConstants.FIX_PASSWORD))
                .thenReturn(new Parameter(FIXConstants.FIX_PASSWORD, "wos2"));

        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        WorkerPool pool = new NativeWorkerPool(3, 4, 10, 10, "name", "id");
        FIXIncomingMessageHandler handler = new FIXIncomingMessageHandler(cfgCtx, pool, service, true);

        handler.toAdmin(message, id);
    }

    @Test
    public void testFromAdmin() throws Exception {
        SessionID id = new SessionID(new BeginString("FIX.4.1"), new SenderCompID("SYNAPSE"),
                new TargetCompID("BANZAI"), "FIX.4.1:SYNAPSE->BANZAI");

        Message message = new NewOrderSingle();
        message.getHeader().setField(new BeginString("FIX.4.1"));
        message.getHeader().setField(new SenderCompID("SYNAPSE"));
        message.getHeader().setField(new TargetCompID("BANZAI"));
        message.getHeader().setField(new MsgSeqNum(1));
        message.getHeader().setField(new MsgType("A"));

        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        WorkerPool pool = new NativeWorkerPool(3, 4, 10, 10, "name", "id");
        FIXIncomingMessageHandler handler = new FIXIncomingMessageHandler(cfgCtx, pool, service, true);

        handler.fromAdmin(message, id);
    }

    @Test
    public void testToApp() throws Exception {
        SessionID id = new SessionID(new BeginString("FIX.4.1"), new SenderCompID("SYNAPSE"),
                new TargetCompID("BANZAI"), "FIX.4.1:SYNAPSE->BANZAI");

        Message message = new NewOrderSingle();
        message.getHeader().setField(new BeginString("FIX.4.1"));
        message.getHeader().setField(new SenderCompID("SYNAPSE"));
        message.getHeader().setField(new TargetCompID("BANZAI"));
        message.getHeader().setField(new MsgSeqNum(1));
        message.getHeader().setField(new MsgType("A"));

        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        WorkerPool pool = new NativeWorkerPool(3, 4, 10, 10, "name", "id");
        FIXIncomingMessageHandler handler = new FIXIncomingMessageHandler(cfgCtx, pool, service, true);

        handler.toApp(message, id);
    }
}
