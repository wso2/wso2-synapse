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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.OutTransportInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.BeginString;
import quickfix.field.CheckSum;
import quickfix.field.ClOrdID;
import quickfix.field.MsgSeqNum;
import quickfix.field.SenderCompID;
import quickfix.field.SenderLocationID;
import quickfix.field.SenderSubID;
import quickfix.field.Symbol;
import quickfix.field.TargetCompID;
import quickfix.field.TargetLocationID;
import quickfix.field.TargetSubID;
import quickfix.field.TradeOriginationDate;
import quickfix.fix41.NewOrderSingle;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(FIXTransportSender.class)
public class FIXTransportSenderTest extends TestCase {
    @Test
    public void testFIXTransportSenderInit() throws Exception {
        AxisService axisService = new AxisService("testFIXService");
        axisService.addParameter(new Parameter(FIXConstants.FIX_ACCEPTOR_CONFIG_URL_PARAM, "/sample/path/Mock.cfg"));
        axisService.addParameter(new Parameter(FIXConstants.FIX_INITIATOR_CONFIG_URL_PARAM, "/sample/path/Mock2.cfg"));

        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        TransportOutDescription trpOutDesc = new TransportOutDescription("fix");

        FIXTransportSender fixTransportSender = new FIXTransportSender();
        fixTransportSender.init(cfgCtx, trpOutDesc);
    }

    @Test()
    public void testFIXTransportSenderSendMessage() throws Exception {
        String BEGIN_STRING = "FIX.4.1";
        String SENDER_ID = "BANZAI";
        String TARGET_ID = "SYNAPSE";
        int SEQ_NUM = 5;
        String SYMBOL = "APACHE";
        String CLORD_ID = "12345";
        String CHECKSUM = "67890";
        String TX_DATE = new Date().toString();
        String SESSION_ID = "FIX.4.1:BANZAI->SYNAPSE";

        Message message = new NewOrderSingle();
        message.getHeader().setField(new BeginString(BEGIN_STRING));
        message.getHeader().setField(new SenderCompID(SENDER_ID));
        message.getHeader().setField(new TargetCompID(TARGET_ID));
        message.getHeader().setField(new MsgSeqNum(SEQ_NUM));

        message.setField(new Symbol(SYMBOL));
        message.setField(new ClOrdID(CLORD_ID));
        message.setField(new TradeOriginationDate(TX_DATE));

        message.getTrailer().setField(new CheckSum(CHECKSUM));

        MessageContext msgCtx = new MessageContext();

        msgCtx.setProperty(FIXConstants.FIX_SERVICE_NAME, "sampleService");
        Map trpHeaders = new HashMap();
        trpHeaders.put(FIXConstants.FIX_MESSAGE_APPLICATION, "sampleApplication");
        msgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, trpHeaders);

        FIXUtils.getInstance().setSOAPEnvelope(message, SEQ_NUM, SESSION_ID, msgCtx);

        OutTransportInfo info = new FIXOutTransportInfo("fix://dummyEPR");
        FIXTransportSender spy = PowerMockito.spy(new FIXTransportSender());
        PowerMockito.doReturn(true)
                .when(spy, "sendUsingEPR", anyString(), anyString(), any(), anyString(), anyInt(), any());
        spy.sendMessage(msgCtx, "fix://dummyEPR", info);
        PowerMockito.verifyPrivate(spy, times(1))
                .invoke("sendUsingEPR", anyString(), anyString(), any(), anyString(), anyInt(), any());

    }

    @Test
    public void testIsTargetValid() throws Exception {
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put(FIXConstants.BEGIN_STRING, "FIX.4.1");
        fieldValues.put(FIXConstants.DELIVER_TO_COMP_ID, "SYNAPSE");
        fieldValues.put(FIXConstants.DELIVER_TO_SUB_ID, "sy");
        fieldValues.put(FIXConstants.DELIVER_TO_LOCATION_ID, "randomLoc");

        SessionID id = new SessionID(new BeginString("FIX.4.1"), new SenderCompID("BANZAI"), new SenderSubID("ba"),
                new SenderLocationID("senderLoc"), new TargetCompID("SYNAPSE"), new TargetSubID("sy"),
                new TargetLocationID("randomLoc"), "FIX.4.1:SYNAPSE->BANZAI");

        FIXTransportSender sender = new FIXTransportSender();
        Class senderClass = sender.getClass();
        Method isTargetvalid = senderClass
                .getDeclaredMethod("isTargetValid", Map.class, SessionID.class, boolean.class);
        isTargetvalid.setAccessible(true);

        Object result = isTargetvalid.invoke(senderClass.newInstance(), fieldValues, id, true);
        Assert.assertEquals("Invalid target!", "true", result.toString());
    }
}