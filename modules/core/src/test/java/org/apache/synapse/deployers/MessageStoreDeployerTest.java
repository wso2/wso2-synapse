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

package org.apache.synapse.deployers;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for MessageStoreDeployer
 */
public class MessageStoreDeployerTest {
    /**
     * Testing the deployment of an message store
     *
     * @throws Exception
     */
    @Test
    public void testDeploy() throws Exception {
        String inputXML =
                "<messageStore name=\"JMSMS\" class=\"org.apache.synapse.message.store.impl.jms.JmsStore\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                        + "   <parameter name=\"java.naming.factory.initial\">org.wso2.andes.jndi.PropertiesFileInitialContextFactory</parameter>"
                        + "   <parameter name=\"java.naming.provider.url\">repository/conf/jndi.properties</parameter>"
                        + "   <parameter name=\"store.jms.destination\">ordersQueue</parameter>"
                        + "   <parameter name=\"store.jms.connection.factory\">QueueConnectionFactory</parameter>"
                        + "   <parameter name=\"store.jms.JMSSpecVersion\">1.1</parameter>" + "</messageStore>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        MessageStoreDeployer messageStoreDeployer = new MessageStoreDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        messageStoreDeployer.init(cfgCtx);
        String response = messageStoreDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);
        Assert.assertEquals("Message store not deployed!", "JMSMS", response);
    }

    /**
     * Test updating a message store
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        String inputXML =
                "<messageStore name=\"JMSMS\" class=\"org.apache.synapse.message.store.impl.jms.JmsStore\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                        + "   <parameter name=\"java.naming.factory.initial\">org.wso2.andes.jndi.PropertiesFileInitialContextFactory</parameter>"
                        + "   <parameter name=\"java.naming.provider.url\">repository/conf/jndi.properties</parameter>"
                        + "   <parameter name=\"store.jms.destination\">ordersQueue</parameter>"
                        + "   <parameter name=\"store.jms.connection.factory\">QueueConnectionFactory</parameter>"
                        + "   <parameter name=\"store.jms.JMSSpecVersion\">1.1</parameter>" + "</messageStore>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        MessageStoreDeployer messageStoreDeployer = new MessageStoreDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        messageStoreDeployer.init(cfgCtx);
        messageStoreDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        String inputUpdatedXML =
                "<messageStore name=\"JMSMSnew\" class=\"org.apache.synapse.message.store.impl.jms.JmsStore\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                        + "   <parameter name=\"java.naming.factory.initial\">org.wso2.andes.jndi.PropertiesFileInitialContextFactory</parameter>"
                        + "   <parameter name=\"java.naming.provider.url\">repository/conf/jndi.properties</parameter>"
                        + "   <parameter name=\"store.jms.destination\">ordersQueue</parameter>"
                        + "   <parameter name=\"store.jms.connection.factory\">QueueConnectionFactory</parameter>"
                        + "   <parameter name=\"store.jms.JMSSpecVersion\">1.1</parameter>" + "</messageStore>";

        OMElement inputUpdatedElement = AXIOMUtil.stringToOM(inputUpdatedXML);

        String response = messageStoreDeployer
                .updateSynapseArtifact(inputUpdatedElement, "sampleUpdatedFile", "JMSMS", null);
        Assert.assertEquals("Message Store not updated!", "JMSMSnew", response);
    }

    /**
     * Test undeploying a message store
     *
     * @throws Exception
     */
    @Test
    public void testUndeploy() throws Exception {
        String inputXML =
                "<messageStore name=\"JMSMS\" class=\"org.apache.synapse.message.store.impl.jms.JmsStore\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                        + "   <parameter name=\"java.naming.factory.initial\">org.wso2.andes.jndi.PropertiesFileInitialContextFactory</parameter>"
                        + "   <parameter name=\"java.naming.provider.url\">repository/conf/jndi.properties</parameter>"
                        + "   <parameter name=\"store.jms.destination\">ordersQueue</parameter>"
                        + "   <parameter name=\"store.jms.connection.factory\">QueueConnectionFactory</parameter>"
                        + "   <parameter name=\"store.jms.JMSSpecVersion\">1.1</parameter>" + "</messageStore>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        MessageStoreDeployer messageStoreDeployer = new MessageStoreDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        messageStoreDeployer.init(cfgCtx);
        messageStoreDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);
        Assert.assertNotNull("Message Store not deployed!", synapseConfiguration.getMessageStore("JMSMS"));

        messageStoreDeployer.undeploySynapseArtifact("JMSMS");
        Assert.assertNull("Message Store cannot be undeployed!", synapseConfiguration.getMessageStore("JMSMS"));
    }
}