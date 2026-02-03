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
import org.apache.axis2.context.MessageContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.field.BeginString;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;
import quickfix.fix41.NewOrderSingle;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class FIXOutgoingMessageHandlerTest extends TestCase {
    @Mock(name = "sessionFactory")
    FIXSessionFactory sessionFactory;

    @Mock
    FIXIncomingMessageHandler app;

    @InjectMocks
    FIXOutgoingMessageHandler spy;

    @Test
    public void testSendMessage() throws Exception {
        int SEQ_NUM = 1;
        String SESSION_ID = "FIX.4.1:BANZAI->SYNAPSE";

        Message message = new NewOrderSingle();
        MessageContext msgCtx = new MessageContext();
        Mockito.when(sessionFactory.getApplication(anyString())).thenReturn(app);
        
        try (MockedStatic<Session> mockedSession = Mockito.mockStatic(Session.class)) {
            mockedSession.when(() -> Session.sendToTarget(any(Message.class), any(SessionID.class))).thenReturn(true);
            SessionID id = new SessionID(new BeginString("FIX.4.1"), new SenderCompID("SYNAPSE"),
                    new TargetCompID("BANZAI"), "FIX.4.1:SYNAPSE->BANZAI");

            spy.sendMessage(message, id, SESSION_ID, SEQ_NUM, msgCtx, "fix://sample");
        }
    }

}