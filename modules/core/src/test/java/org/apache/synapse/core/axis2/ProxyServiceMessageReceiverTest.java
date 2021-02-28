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

package org.apache.synapse.core.axis2;


import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.SequenceMediatorFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.util.Properties;

/**
 * Unit tests to test methods in class ProxyServiceMessageReceiver
 */
public class ProxyServiceMessageReceiverTest {

    /**
     * Test message receipt by ProxyServiceMessageReceiver.
     *
     * @throws Exception on a message receipt issue
     */
    @Test
    public void testReceive() throws Exception {

        //create ProxyServiceMessageReceiver instance
        ProxyServiceMessageReceiver proxyServiceMessageReceiver = new ProxyServiceMessageReceiver();
        ProxyService proxyService = new ProxyService("TestProxy");
        //create an inSequence and set
        OMElement sequenceAsOM = AXIOMUtil.stringToOM("<inSequence xmlns=\"http://ws.apache.org/ns/synapse\">\n"
                + "         <property name=\"TEST\" scope=\"axis2\" type=\"STRING\" value=\"WSO2\"/>\n"
                + "      </inSequence>");
        proxyService.setTargetInLineInSequence(new SequenceMediatorFactory().
                createAnonymousSequence(sequenceAsOM, new Properties()));

        proxyServiceMessageReceiver.setProxy(proxyService);

        MessageContext messageContext = createMessageContext();
        Axis2SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();

        //set required values
        MessageContextCreatorForAxis2.setSynConfig(new SynapseConfiguration());
        MessageContextCreatorForAxis2.setSynEnv(synapseEnvironment);
        messageContext.setEnvironment(synapseEnvironment);

        //invoke
        proxyServiceMessageReceiver.receive(((Axis2MessageContext) messageContext).getAxis2MessageContext());
        String propertySet = (String) ((Axis2MessageContext) messageContext).getAxis2MessageContext().getProperty("TEST");
        Assert.assertEquals("property is not set after delegating to proxy", "WSO2", propertySet);

    }

    /**
     * Create a mock SynapseEnvironment object.
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
     * Create a empty message context.
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
