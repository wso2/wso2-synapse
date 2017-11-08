/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.task;

import junit.framework.Assert;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.task.utils.Utils;
import org.jaxen.JaxenException;
import org.junit.Test;

import java.io.FileNotFoundException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * Test class for TaskDescriptionSerializer.
 */
public class TaskDescriptionSerializerTest {
    private final String SYNAPSE_NAMESPACE = TaskTestConstant.SYNAPSE_NAMESPACE;
    private final OMNamespace SYNAPSE_OMNAMESPACE = TaskTestConstant.SYNAPSE_OMNAMESPACE;

    /**
     * Check the TaskDescription object serialization to xml
     */
    @Test()
    public void createTaskDescriptionTest() throws FileNotFoundException, XMLStreamException, JaxenException {
        String path = this.getClass().getClassLoader().getResource("task/task.xml").getFile();
        OMElement ome = Utils.loadOMElement(path);
        TaskDescription taskDescription = TaskDescriptionFactory.createTaskDescription(ome, SYNAPSE_OMNAMESPACE);

        OMElement taskOmeSerialised = TaskDescriptionSerializer
                .serializeTaskDescription(SYNAPSE_OMNAMESPACE, taskDescription);
        Assert.assertEquals("Task Name mismatched", "task", taskOmeSerialised.getAttributeValue(new QName("name")));
        Assert.assertEquals("Task Class mismatched", "org.apache.synapse.task.impl.CustomTaskTestImpl",
                taskOmeSerialised.getAttributeValue(new QName("class")));
        AXIOMXPath xpathExpression = new AXIOMXPath("/ns:property[@name='soapAction']");
        xpathExpression.addNamespace("ns", SYNAPSE_NAMESPACE);
        OMElement soapActionProperty = (OMElement) xpathExpression.selectSingleNode(taskOmeSerialised);
        Assert.assertNotNull("soapAction property not found", soapActionProperty);
        String soapAction = soapActionProperty.getAttributeValue(new QName("value"));
        Assert.assertEquals("SOAPAction property mismatched", "urn:getQuote", soapAction);
    }

    /**
     * Check the TaskDescription object serialization to xml when the trigger is a cron expression.
     */
    @Test()
    public void createTaskDescriptionTriggerCronTest()
            throws FileNotFoundException, XMLStreamException, JaxenException {
        String path = this.getClass().getClassLoader().getResource("task/taskTriggerCron.xml").getFile();
        OMElement ome = Utils.loadOMElement(path);
        TaskDescription taskDescription = TaskDescriptionFactory.createTaskDescription(ome, SYNAPSE_OMNAMESPACE);

        OMElement taskOmeSerialised = TaskDescriptionSerializer
                .serializeTaskDescription(SYNAPSE_OMNAMESPACE, taskDescription);
        Assert.assertEquals("Task Name mismatched", "taskCron", taskOmeSerialised.getAttributeValue(new QName("name")));
        Assert.assertEquals("Task Class mismatched", "org.apache.synapse.task.impl.CustomTaskTestImpl",
                taskOmeSerialised.getAttributeValue(new QName("class")));
        AXIOMXPath xpathExpression = new AXIOMXPath("/ns:trigger[@cron]");
        xpathExpression.addNamespace("ns", SYNAPSE_NAMESPACE);
        OMElement triggerOme = (OMElement) xpathExpression.selectSingleNode(taskOmeSerialised);
        Assert.assertNotNull("trigger element not found", triggerOme);
        String cron = triggerOme.getAttributeValue(new QName("cron"));
        Assert.assertEquals("Cron expression mismatched", "0 * * * * ?", cron);
    }

    /**
     * Check the TaskDescription object serialization to xml when the trigger is once
     */
    @Test()
    public void createTaskDescriptionTriggerOnceTest()
            throws FileNotFoundException, XMLStreamException, JaxenException {
        String path = this.getClass().getClassLoader().getResource("task/taskTriggerOnce.xml").getFile();
        OMElement ome = Utils.loadOMElement(path);
        TaskDescription taskDescription = TaskDescriptionFactory.createTaskDescription(ome, SYNAPSE_OMNAMESPACE);

        OMElement taskOmeSerialised = TaskDescriptionSerializer
                .serializeTaskDescription(SYNAPSE_OMNAMESPACE, taskDescription);
        Assert.assertEquals("Task Name mismatched", "taskCron", taskOmeSerialised.getAttributeValue(new QName("name")));
        Assert.assertEquals("Task Class mismatched", "org.apache.synapse.task.impl.CustomTaskTestImpl",
                taskOmeSerialised.getAttributeValue(new QName("class")));
        AXIOMXPath xpathExpression = new AXIOMXPath("/ns:trigger[@once]");
        xpathExpression.addNamespace("ns", SYNAPSE_NAMESPACE);
        OMElement triggerOme = (OMElement) xpathExpression.selectSingleNode(taskOmeSerialised);
        Assert.assertNotNull("trigger element not found", triggerOme);
        String once = triggerOme.getAttributeValue(new QName("once"));
        Assert.assertEquals("once attribute mismatched", "true", once);
    }

    /**
     * Check the Exception class when the TaskDescription is null
     */
    @Test(expected = SynapseTaskException.class)
    public void createTaskDescriptionNullTest() throws FileNotFoundException, XMLStreamException, JaxenException {
        TaskDescriptionSerializer.serializeTaskDescription(SYNAPSE_OMNAMESPACE, null);
    }
}
