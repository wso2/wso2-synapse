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

    @Test()
    public void createTaskDescriptionTest() throws FileNotFoundException, XMLStreamException {
        String path = this.getClass().getClassLoader().getResource("task/task.xml").getFile();
        OMElement taskOme = loadOMElement(path);
        TaskDescription taskDescription = TaskDescriptionFactory.createTaskDescription(taskOme, SYNAPSE_OMNAMESPACE);
        Assert.assertEquals("Name mismatched", "task", taskDescription.getName());
        Assert.assertNull("Task Group not null", taskDescription.getTaskGroup());
        Assert.assertEquals("Task Class Name mismatched", TASK_CLASS_NAME, taskDescription.getTaskImplClassName());
        Assert.assertEquals("Properties not empty", 0, taskDescription.getProperties().size());
    }

    private OMElement loadOMElement(String path) throws FileNotFoundException, XMLStreamException {
        OMElement definitions = new StAXOMBuilder(new FileInputStream(path)).getDocumentElement();
        definitions.build();
        return definitions;
    }
}
