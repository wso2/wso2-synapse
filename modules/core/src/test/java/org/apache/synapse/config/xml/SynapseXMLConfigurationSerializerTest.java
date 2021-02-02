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
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.template.TemplateMediator;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.message.store.impl.jms.JmsStore;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.registry.SimpleInMemoryRegistry;
import org.apache.synapse.api.API;
import org.apache.synapse.startup.quartz.QuartzTaskManager;
import org.apache.synapse.startup.quartz.StartUpController;
import org.apache.synapse.task.TaskDescription;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.doReturn;

/**
 * This is the test class for SynapseXMLConfigurationSerializer class.
 */
public class SynapseXMLConfigurationSerializerTest {

    private static final String KEY_DYNAMIC_SEQUENCE_1 = "dynamic_sequence_1";
    private static final String KEY_DYNAMIC_ENDPOINT_1 = "dynamic_endpoint_1";
    private static final String DYNAMIC_ENDPOINT_1 =
            "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                    "    <address uri=\"http://test.url\"/>\n" +
                    "</endpoint>";
    private static final String DYNAMIC_SEQUENCE_1 =
            "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"seq1\">\n" +
                    "    <property name=\"foo\" value=\"bar\" />" +
                    "</sequence>";

    /**
     * Test serializeConfigurationMethod and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        synapseConfiguration.setDescription("testConfiguration");
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
    }

    /**
     * Test serializeConfigurationMethod with registry set for SynapseConfiguration and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration2() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        Map<String, OMNode> data = new HashMap<>();
        data.put(KEY_DYNAMIC_ENDPOINT_1, TestUtils.createOMElement(DYNAMIC_ENDPOINT_1));
        data.put(KEY_DYNAMIC_SEQUENCE_1, TestUtils.createOMElement(DYNAMIC_SEQUENCE_1));
        Registry registry = new SimpleInMemoryRegistry(data, 8000L);
        synapseConfiguration.setRegistry(registry);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("Registry added is not serialized.", element.getChildren().next().
                toString().contains("registry"));
    }

    /**
     * Test serializeConfigurationMethod with taskManager set for SynapseConfiguration and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration3() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        synapseConfiguration.setTaskManager(new QuartzTaskManager());
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("TaskManager added is not serialized.", element.getChildren().next().
                toString().contains("taskManager"));
    }

    /**
     * Test serializeConfigurationMethod with proxyServices added for SynapseConfiguration and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration4() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        ProxyService proxyService = new ProxyService("testProxyService");
        synapseConfiguration.addProxyService(proxyService.getName(), proxyService);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("ProxyService added is not serialized.", element.getChildren().next().
                toString().contains("name=\"testProxyService\""));
    }

    /**
     * Test serializeConfigurationMethod with TemplateMediator added for SynapseConfiguration
     * and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration5() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        org.apache.synapse.mediators.TestMediator t1 = new org.apache.synapse.mediators.TestMediator();
        org.apache.synapse.mediators.TestMediator t2 = new org.apache.synapse.mediators.TestMediator();
        org.apache.synapse.mediators.TestMediator t3 = new org.apache.synapse.mediators.TestMediator();
        TemplateMediator templateMediator = new TemplateMediator();
        templateMediator.addChild(t1);
        templateMediator.addChild(t2);
        templateMediator.addChild(t3);
        synapseConfiguration.addSequence("testSequence", templateMediator);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("Template added is not serialized.", element.getChildren().next().
                toString().contains("template"));
    }

    /**
     * Test serializeConfigurationMethod with Endpoint added for SynapseConfiguration and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration6() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        definition.setAddress("testUrl");
        endpoint.setName("testEndpoint");
        endpoint.setDefinition(definition);
        synapseConfiguration.addEndpoint(endpoint.getName(), endpoint);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("Endpoint added is not serialized.", element.getChildren().next().
                toString().contains("name=\"testEndpoint\""));
    }

    /**
     * Test serializeConfigurationMethod with Entry added for SynapseConfiguration and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration7() throws MalformedURLException {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        Entry entry = PowerMockito.mock(Entry.class);
        entry.setType(Entry.URL_SRC);
        doReturn(new URL("https://test")).when(entry).getSrc();
        doReturn("testKey").when(entry).getKey();
        synapseConfiguration.addEntry("root_wsdl", entry);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("Entry added is not serialized.", element.getChildren().next().
                toString().contains("key=\"testKey\""));
    }

    /**
     * Test serializeConfigurationMethod with startUp added for SynapseConfiguration and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration8() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        StartUpController startup = new StartUpController();
        startup.setName("testStartup");
        TaskDescription taskDescription = new TaskDescription();
        taskDescription.setName("testTask");
        startup.setTaskDescription(taskDescription);
        synapseConfiguration.addStartup(startup);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("StartUp added is not serialized.", element.getChildren().next().
                toString().contains("name=\"testTask\""));
    }

    /**
     * Test serializeConfigurationMethod with messageStore added for SynapseConfiguration and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration9() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        MessageStore messageStore = new JmsStore();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        parameters.put("java.naming.provider.url", "tcp://127.0.0.1:61616");
        messageStore.setName("testMessageStore");
        messageStore.setParameters(parameters);
        synapseConfiguration.addMessageStore(messageStore.getName(), messageStore);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("MessageStore added is not serialized.", element.getChildren().next().
                toString().contains("name=\"testMessageStore\""));
    }

    /**
     * Test serializeConfigurationMethod with InboundEndpoint added for SynapseConfiguration
     * and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration10() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        InboundEndpoint inboundEndpoint = new InboundEndpoint();
        inboundEndpoint.setName("test_inbound_1");
        inboundEndpoint.setProtocol("http");
        synapseConfiguration.addInboundEndpoint(inboundEndpoint.getName(), inboundEndpoint);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("InboundEndpoint added is not serialized.", element.getChildren().next().
                toString().contains("name=\"test_inbound_1\""));
    }

    /**
     * Test serializeConfigurationMethod with API added for SynapseConfiguration and assert OMElement returned
     */
    @Test
    public void testSerializeConfiguration11() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        API api = new API("testAPI", "/");
        synapseConfiguration.addAPI(api.getName(), api);
        OMElement element = serializer.serializeConfiguration(synapseConfiguration);
        Assert.assertNotNull("OMElement is not returned", element);
        Assert.assertEquals("definitions", element.getLocalName());
        Assert.assertTrue("testAPI added is not serialized.", element.getChildren().next().
                toString().contains("name=\"testAPI\""));
    }

    /**
     * Test getQName method and assert it.
     */
    @Test
    public void testGetQNAme() {
        SynapseXMLConfigurationSerializer serializer = new SynapseXMLConfigurationSerializer();
        QName qName = serializer.getTagQName();
        Assert.assertEquals("Tag QNames are not equal", XMLConfigConstants.DEFINITIONS_ELT, qName);
    }

}
