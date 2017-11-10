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

import junit.framework.AssertionFailedError;
import org.apache.axiom.om.OMNode;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.template.TemplateMediator;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.registry.SimpleInMemoryRegistry;
import org.apache.synapse.startup.quartz.QuartzTaskManager;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the test class for MultiXMLConfigurationSerializer class.
 */
public class MultiXMLConfigurationSerializerTest {

    private static final String SYNAPSE_XML = "synapse.xml";
    private static final String KEY_DYNAMIC_SEQUENCE_1 = "dynamic_sequence_1";
    private static final String KEY_DYNAMIC_ENDPOINT_1 = "dynamic_endpoint_1";
    private static final String TEST_DIRECTORY_NAME = "serializeTestFolder";

    private static final String DYNAMIC_ENDPOINT_1 =
            "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\">\n <address uri=\"http://test.url\"/>\n"
                    + "</endpoint>";

    private static final String DYNAMIC_SEQUENCE_1 =
            "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"seq1\">\n"
                    + "<property name=\"foo\" value=\"bar\" />" + "</sequence>";

    /**
     * Test serialize method and assert synapse.xml is created.
     */
    @Test
    public void testSerialize() {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        SynapseConfiguration configuration = new SynapseConfiguration();
        serializer.serialize(configuration);
        Assert.assertTrue("Error in serializing Synapse configuration.",
                new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test serialize method with root directory already existing and assert synapse.xml is created.
     *
     * @throws IOException - IOException in file creation.
     */
    @Test
    public void testSerialize2() throws IOException {
        File file = new File(TEST_DIRECTORY_NAME);
        File xmlFile = new File(TEST_DIRECTORY_NAME + File.separator + "test.xml");
        if (!file.exists()) {
            file.mkdir();
            if (!xmlFile.exists()) {
                xmlFile.createNewFile();
            }
        }
        Long startTime = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        } while (!xmlFile.exists() && (System.currentTimeMillis() - startTime) < 5000);
        if (xmlFile.exists()) {
            MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
            SynapseConfiguration configuration = new SynapseConfiguration();
            serializer.serialize(configuration);
            Assert.assertTrue("Error in serializing Synapse configuration.",
                    new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        } else {
            Assert.fail("Failed to create XML file.");
        }
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test serialize method with registry set for SynapseConfiguration and assert synapse.xml is created.
     */
    @Test
    public void testSerialize3() {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        SynapseConfiguration configuration = new SynapseConfiguration();
        Map<String, OMNode> data = new HashMap<>();
        data.put(KEY_DYNAMIC_ENDPOINT_1, TestUtils.createOMElement(DYNAMIC_ENDPOINT_1));
        data.put(KEY_DYNAMIC_SEQUENCE_1, TestUtils.createOMElement(DYNAMIC_SEQUENCE_1));
        Registry registry = new SimpleInMemoryRegistry(data, 8000L);
        configuration.setRegistry(registry);
        serializer.serialize(configuration);
        Assert.assertTrue("Error in serializing Synapse configuration.",
                new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test serialize method with taskManager set for SynapseConfiguration and assert synapse.xml is created.
     */
    @Test
    public void testSerialize4() {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        SynapseConfiguration configuration = new SynapseConfiguration();
        configuration.setTaskManager(new QuartzTaskManager());
        serializer.serialize(configuration);
        Assert.assertTrue("Error in serializing Synapse configuration.",
                new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test serializeSynapseXML method and assert synapse.xml is created.
     */
    @Test
    public void testSerializeSynapseXML() throws Exception {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        SynapseConfiguration configuration = new SynapseConfiguration();
        serializer.serializeSynapseXML(configuration);
        Assert.assertTrue("Error in serializing Synapse configuration.",
                new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test serializeSynapseXML method with taskManager set for SynapseConfiguration and assert synapse.xml is created.
     */
    @Test
    public void testSerializeSynapseXML2() throws Exception {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        SynapseConfiguration configuration = new SynapseConfiguration();
        configuration.setTaskManager(new QuartzTaskManager());
        serializer.serializeSynapseXML(configuration);
        Assert.assertTrue("Error in serializing Synapse configuration.",
                new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test serializeSynapseXML method with proxyServices added for SynapseConfiguration and
     * assert synapse.xml is created.
     */
    @Test
    public void testSerializeSynapseXML3() throws Exception {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        SynapseConfiguration configuration = new SynapseConfiguration();
        configuration.setTaskManager(new QuartzTaskManager());
        ProxyService proxyService = new ProxyService("testProxyService");
        configuration.addProxyService(proxyService.getName(), proxyService);
        serializer.serializeSynapseXML(configuration);
        Assert.assertTrue("Error in serializing Synapse configuration.",
                new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test serializeSynapseXML method with SequenceMediator added for SynapseConfiguration and
     * assert synapse.xml is created.
     */
    @Test
    public void testSerializeSynapseXML4() throws Exception {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        SynapseConfiguration configuration = new SynapseConfiguration();
        org.apache.synapse.mediators.TestMediator t1 = new org.apache.synapse.mediators.TestMediator();
        org.apache.synapse.mediators.TestMediator t2 = new org.apache.synapse.mediators.TestMediator();
        org.apache.synapse.mediators.TestMediator t3 = new org.apache.synapse.mediators.TestMediator();
        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);
        configuration.addSequence("testSequence", seq);
        serializer.serializeSynapseXML(configuration);
        Assert.assertTrue("Error in serializing Synapse configuration.",
                new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test serializeSynapseXML method with TemplateMediator added for SynapseConfiguration and
     * assert synapse.xml is created.
     */
    @Test
    public void testSerializeSynapseXML5() throws Exception {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        SynapseConfiguration configuration = new SynapseConfiguration();
        org.apache.synapse.mediators.TestMediator t1 = new org.apache.synapse.mediators.TestMediator();
        org.apache.synapse.mediators.TestMediator t2 = new org.apache.synapse.mediators.TestMediator();
        org.apache.synapse.mediators.TestMediator t3 = new org.apache.synapse.mediators.TestMediator();
        TemplateMediator templateMediator = new TemplateMediator();
        templateMediator.addChild(t1);
        templateMediator.addChild(t2);
        templateMediator.addChild(t3);
        configuration.addSequence("testSequence", templateMediator);
        serializer.serializeSynapseXML(configuration);
        Assert.assertTrue("Error in serializing Synapse configuration.",
                new File(TEST_DIRECTORY_NAME + File.separator + SYNAPSE_XML).exists());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Test isWritable method by creating a writable a directory and passing to it and asserting it returns true.
     */
    @Test
    public void testIsWritable() throws Exception {
        MultiXMLConfigurationSerializer serializer = new MultiXMLConfigurationSerializer(TEST_DIRECTORY_NAME);
        File file = new File(TEST_DIRECTORY_NAME);
        if (!file.exists()) {
            file.mkdir();
            file.setWritable(true);
        }
        Assert.assertTrue(TEST_DIRECTORY_NAME + " is writable.", serializer.isWritable());
        removeTestFolder(TEST_DIRECTORY_NAME);
    }

    /**
     * Removing the folders created during the test.
     *
     * @param folderName - The name of the folder to be removed.
     */
    private void removeTestFolder(String folderName) {
        File index = new File(folderName);
        String[] entries = index.list();
        if (entries != null) {
            for (String s : entries) {
                File currentFile = new File(index.getPath(), s);
                currentFile.delete();
            }
        }
        index.delete();
    }

}
