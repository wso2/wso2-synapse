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

package org.apache.synapse.message.store.impl.jms;

import junit.framework.Assert;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.activemq.broker.BrokerService;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for Jms Store implementation
 */
public class JmsMessageStoreTest{

    private static BrokerService broker;

    @BeforeClass
    public static void startBroker() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.addConnector("tcp://127.0.0.1:61616");
        broker.start();
        if (!broker.isStarted()) {
            throw new Exception("Couldn't start the broker!");
        }
    }

    /**
     * Testing whether jms producer is storing the message to the store
     * @throws Exception
     */
    @Test
    public void testProducer() throws Exception{

        JmsStore jmsStore = new JmsStore();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        parameters.put("java.naming.provider.url", "tcp://127.0.0.1:61616");
        jmsStore.setParameters(parameters);
        jmsStore.setName("TestJmsStore");
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        synapseConfiguration.addMessageStore("JMSStore", jmsStore);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(synapseConfiguration);
        Axis2MessageContext axis2MessageContext = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(), synapseConfiguration, synapseEnvironment);
        MessageContext messageContext = axis2MessageContext;
        SOAP11Factory factory = new SOAP11Factory();
        SOAPEnvelope envelope = factory.createSOAPEnvelope();
        messageContext.setEnvelope(envelope);

        jmsStore.init(synapseEnvironment);

        JmsProducer jmsProducer = (JmsProducer) jmsStore.getProducer();
        boolean response = jmsProducer.storeMessage(messageContext);

        Assert.assertTrue("Message not stored!", response);
    }

    @AfterClass
    public static void stopBroker() throws Exception{
        if(broker.isStarted()){
            broker.stop();
        }
    }
}

