/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.endpoints;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.clustering.Member;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;



/**
 * Unit tests for class RecipientListEndpoint
 */
@RunWith(PowerMockRunner.class)
public class RecipientListEndpointTest {

    /**
     * Test on init of RecipientListEndpoint
     */
    @Test
    public void testRecipientListEndpointInit() throws Exception {

        Axis2SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();
        RecipientListEndpoint recipientListEndpoint = new RecipientListEndpoint();
        recipientListEndpoint.init(synapseEnvironment);
    }

    /**
     * Test on Sending messages to different members using RecipientListEndpoint
     */
    @Test
    public void testSendMessageToMembers() throws Exception {
        //perform init
        Axis2SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();
        RecipientListEndpoint recipientListEndpoint = new RecipientListEndpoint();
        recipientListEndpoint.init(synapseEnvironment);
        Mockito.when(synapseEnvironment.createMessageContext()).thenReturn(createMessageContext());

        //set members
        Member member1 = new Member("localhost", 9000);
        Member member2 = new Member("localhost", 9001);
        ArrayList<Member> members = new ArrayList<>(2);
        members.add(member1);
        members.add(member2);
        recipientListEndpoint.setMembers(members);

        //test send message
        String samplePayload = "<test>value</test>";
        Axis2MessageContext messageContext = getMessageContext(samplePayload);
        //message will be sent to EP using this env (which is mocked and do nothing)
        messageContext.setEnvironment(synapseEnvironment);
        messageContext.setTo(new EndpointReference("http://localhost:9000/services/SimpleStockQuoteService"));
        recipientListEndpoint.sendMessage(messageContext);
    }

    /**
     * Test on Sending messages to a dynamic EP based on an expression
     *
     * @throws Exception on test failure
     */
    @Test
    public void testSendToDynamicMembers() throws Exception {
        //perform init
        Axis2SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();
        RecipientListEndpoint recipientListEndpoint = new RecipientListEndpoint(2);
        recipientListEndpoint.init(synapseEnvironment);
        Mockito.when(synapseEnvironment.createMessageContext()).thenReturn(createMessageContext());

        //add dynamic EPs
        Value dynamicEPs = new Value(new SynapseXPath("//endpoints"));
        recipientListEndpoint.setDynamicEnpointSet(dynamicEPs);

        //test send message
        String samplePayload = "<test><endpoints>http://localhost:9000/services/SimpleStockQuoteService," +
                "http://localhost:9001/services/SimpleStockQuoteService" +
                "</endpoints><body>wso2</body></test>";
        Axis2MessageContext messageContext = getMessageContext(samplePayload);
        //message will be sent to EP using this env (which is mocked and do nothing)
        messageContext.setEnvironment(synapseEnvironment);
        //messageContext.setTo(new EndpointReference("http://localhost:9000/services/SimpleStockQuoteService"));
        recipientListEndpoint.sendMessage(messageContext);

    }

    /**
     * Create a RecipientListEndpoint by config and test sending a message
     * @throws Exception on an issue sending out the message
     */
    @Test
    public void testSendToEndpointList() throws Exception {

        OMElement omBody = AXIOMUtil.stringToOM("<endpoint><recipientlist xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                "    <endpoint xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                "            <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                "    </endpoint>\n" +
                "    <endpoint xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                "            <address uri=\"http://localhost:9001/services/SimpleStockQuoteService\"/>\n" +
                "    </endpoint>\n" +
                "</recipientlist></endpoint>");

        RecipientListEndpoint recipientListEndpoint = (RecipientListEndpoint) EndpointFactory.
                getEndpointFromElement(omBody,true, null);

        Axis2SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();
        Mockito.when(synapseEnvironment.createMessageContext()).thenReturn(createMessageContext());
        //test send message
        String samplePayload = "<test><a>WSO2</a></test>";
        Axis2MessageContext messageContext = getMessageContext(samplePayload);
        //message will be sent to EP using this env (which is mocked and do nothing)
        messageContext.setEnvironment(synapseEnvironment);
        recipientListEndpoint.init(synapseEnvironment);
        recipientListEndpoint.sendMessage(messageContext);

    }

    /**
     * Create a sample synapse message context with a simple payload
     *
     * @param payload payload of the envelope of message context
     * @return Axis2MessageContext with payload and parameters
     * @throws Exception on creating the context
     */
    private Axis2MessageContext getMessageContext(String payload) throws Exception {
        Map<String, Entry> properties = new HashMap<>();
        Axis2MessageContext messageContext = TestUtils.getAxis2MessageContext(payload, properties);
        messageContext.getAxis2MessageContext().setTransportIn(new TransportInDescription("http"));
        return messageContext;
    }

    /**
     * Create a mock SynapseEnvironment object
     *
     * @return Axis2SynapseEnvironment instance
     * @throws AxisFault on creating/mocking object
     */
    private Axis2SynapseEnvironment getMockedSynapseEnvironment() throws AxisFault {
        Axis2SynapseEnvironment synapseEnvironment = PowerMockito.mock(Axis2SynapseEnvironment.class);
        ConfigurationContext axis2ConfigurationContext = new ConfigurationContext(new AxisConfiguration());
        Mockito.when(synapseEnvironment.getAxis2ConfigurationContext()).thenReturn(axis2ConfigurationContext);
        return synapseEnvironment;
    }

    /**
     * Create a empty message context
     *
     * @return A context with empty message
     * @throws AxisFault on an error creating a context
     */
    private MessageContext createMessageContext() throws AxisFault {

        Axis2SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(new SynapseConfiguration());
        org.apache.axis2.context.MessageContext axis2MC
                = new org.apache.axis2.context.MessageContext();
        axis2MC.setConfigurationContext(new ConfigurationContext(new AxisConfiguration()));

        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MC.setServiceContext(svcCtx);
        axis2MC.setOperationContext(opCtx);
        MessageContext mc = new Axis2MessageContext(axis2MC, new SynapseConfiguration(), synapseEnvironment);
        mc.setMessageID(UIDGenerator.generateURNString());
        mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
        mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        return mc;
    }

}
