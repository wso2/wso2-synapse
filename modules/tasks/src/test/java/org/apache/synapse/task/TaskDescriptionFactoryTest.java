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
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.commons.property.PropertyHolder;
import org.apache.synapse.commons.resolvers.ResolverException;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.xml.stream.XMLStreamException;

public class TaskDescriptionFactoryTest {

    private final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";
    private final OMNamespace SYNAPSE_OMNAMESPACE = OMAbstractFactory.getOMFactory()
            .createOMNamespace(SYNAPSE_NAMESPACE, "");
    private final String TASK_CLASS_NAME = "org.apache.synapse.task.impl.CustomTaskTestImpl";
    private static final String START_ON_LOAD_CONFIG_KEY = "taskStartOnLoadTestKey";

    @Test()
    public void createTaskDescriptionTest() throws FileNotFoundException, XMLStreamException {
        String path = this.getClass().getClassLoader().getResource("task/task.xml").getFile();
        OMElement taskOme = loadOMElement(path);
        TaskDescription taskDescription = TaskDescriptionFactory.createTaskDescription(taskOme, SYNAPSE_OMNAMESPACE);
        Assert.assertEquals("Name mismatched", "task", taskDescription.getName());
        Assert.assertEquals("Task Group mismatched", "TestGroup", taskDescription.getTaskGroup());
        Assert.assertEquals("Task Class Name mismatched", TASK_CLASS_NAME, taskDescription.getTaskImplClassName());
        Assert.assertEquals("Properties not empty", 0, taskDescription.getProperties().size());
        Assert.assertEquals("Pinned Server count mismatched", 2, taskDescription.getPinnedServers().size());
    }

    @Test(expected = SynapseTaskException.class)
    public void createTaskDescriptionWithOutNameTest() throws FileNotFoundException, XMLStreamException {
        String path = this.getClass().getClassLoader().getResource("task/taskNoName.xml").getFile();
        OMElement taskOme = loadOMElement(path);
        TaskDescriptionFactory.createTaskDescription(taskOme, SYNAPSE_OMNAMESPACE);
    }

    @Test(expected = SynapseTaskException.class)
    public void createTaskDescriptionNonExistingTaskClassTest() throws FileNotFoundException, XMLStreamException {
        String path = this.getClass().getClassLoader().getResource("task/taskNonExistingTaskClass.xml").getFile();
        OMElement taskOme = loadOMElement(path);
        TaskDescriptionFactory.createTaskDescription(taskOme, SYNAPSE_OMNAMESPACE);
    }

    @Test
    public void createTaskDescriptionStartOnLoadFromConfigTest() throws XMLStreamException {
        try {
            PropertyHolder.getInstance().setProperty(START_ON_LOAD_CONFIG_KEY, "false");
            TaskDescription taskDescription = TaskDescriptionFactory.createTaskDescription(
                    createTaskWithStartOnLoad("${configs." + START_ON_LOAD_CONFIG_KEY + "}"), SYNAPSE_OMNAMESPACE);
            Assert.assertFalse("startOnLoad should resolve to false", taskDescription.isStartOnLoad());

            PropertyHolder.getInstance().setProperty(START_ON_LOAD_CONFIG_KEY, "true");
            taskDescription = TaskDescriptionFactory.createTaskDescription(
                    createTaskWithStartOnLoad("${configs." + START_ON_LOAD_CONFIG_KEY + "}"), SYNAPSE_OMNAMESPACE);
            Assert.assertTrue("startOnLoad should resolve to true", taskDescription.isStartOnLoad());
        } finally {
            PropertyHolder.getInstance().getProperties().remove(START_ON_LOAD_CONFIG_KEY);
        }
    }

    @Test(expected = ResolverException.class)
    public void createTaskDescriptionStartOnLoadUndefinedConfigTest() throws XMLStreamException {
        TaskDescriptionFactory.createTaskDescription(
                createTaskWithStartOnLoad("${configs." + START_ON_LOAD_CONFIG_KEY + "}"), SYNAPSE_OMNAMESPACE);
    }

    @Test
    public void createTaskDescriptionStartOnLoadLiteralTest() throws XMLStreamException {
        Assert.assertFalse(TaskDescriptionFactory.createTaskDescription(
                createTaskWithStartOnLoad("false"), SYNAPSE_OMNAMESPACE).isStartOnLoad());
        Assert.assertTrue(TaskDescriptionFactory.createTaskDescription(
                createTaskWithStartOnLoad("true"), SYNAPSE_OMNAMESPACE).isStartOnLoad());
    }

    private OMElement createTaskWithStartOnLoad(String startOnLoad) throws XMLStreamException {
        return AXIOMUtil.stringToOM("<task xmlns=\"" + SYNAPSE_NAMESPACE + "\" class=\"" + TASK_CLASS_NAME +
                "\" name=\"startOnLoadTask\" group=\"TestGroup\" startOnLoad=\"" + startOnLoad + "\">" +
                "<trigger interval=\"5\"/></task>");
    }

    private OMElement loadOMElement(String path) throws FileNotFoundException, XMLStreamException {
        OMElement definitions = new StAXOMBuilder(new FileInputStream(path)).getDocumentElement();
        definitions.build();
        return definitions;
    }
}
