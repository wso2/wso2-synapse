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
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseException;
import org.apache.synapse.startup.quartz.QuartzTaskManager;
import org.apache.synapse.task.TaskManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.stream.XMLStreamException;

/**
 * This is the test class for TaskManagerFactory class.
 */
public class TaskManagerFactoryTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * test createTaskManager
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test
    public void testCreateTaskManager() throws XMLStreamException {
        String inputXML = "<taskManager provider=\"org.apache.synapse.startup.quartz.QuartzTaskManager\"/>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        TaskManager taskManager = TaskManagerFactory.createTaskManager(element, null);
        Assert.assertTrue("TaskManager is not created.", taskManager instanceof QuartzTaskManager);
    }

    /**
     * test createTaskManager with parameter specified for OMElement
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test
    public void testCreateTaskManager2() throws XMLStreamException {
        String inputXML = "<taskManager provider=\"org.apache.synapse.startup.quartz.QuartzTaskManager\">"
                + "<parameter xmlns=\"http://ws.apache.org/ns/synapse\"  name =\"test\"> test </parameter>"
                + "</taskManager>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        TaskManager taskManager = TaskManagerFactory.createTaskManager(element, null);
        Assert.assertTrue("TaskManager is not created.", taskManager instanceof QuartzTaskManager);
    }

    /**
     * test createTaskManager with wrong providerName
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test
    public void testCreateTaskManager3() throws XMLStreamException {
        expectedEx.expect(SynapseException.class);
        expectedEx.expectMessage("Cannot locate task provider class : " +
                "org.apache.synapse.startup.quartz.QuartzTaskManagerDummy");
        String inputXML = "<taskManager provider=\"org.apache.synapse.startup.quartz.QuartzTaskManagerDummy\">"
                + "<parameter xmlns=\"http://ws.apache.org/ns/synapse\"  name =\"test\"> test </parameter>"
                + "</taskManager>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        TaskManagerFactory.createTaskManager(element, null);
    }

    /**
     * test createTaskManager with provider not specified.
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test
    public void testCreateTaskManager4() throws XMLStreamException {
        expectedEx.expect(SynapseException.class);
        expectedEx.expectMessage("The task 'provider' attribute is required for a taskManager definition");
        String inputXML = "<taskManager>"
                + "<parameter xmlns=\"http://ws.apache.org/ns/synapse\"  name =\"test\"> test </parameter>"
                + "</taskManager>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        TaskManagerFactory.createTaskManager(element, null);
    }
}
