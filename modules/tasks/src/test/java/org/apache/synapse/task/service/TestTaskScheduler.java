/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.task.service;

import junit.framework.Assert;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.startup.quartz.QuartzTaskManager;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskDescriptionFactory;
import org.apache.synapse.task.TaskScheduler;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

@PrepareForTest(QuartzTaskManager.class)
public class TestTaskScheduler {

    private final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";
    private final OMNamespace SYNAPSE_OMNAMESPACE =
            OMAbstractFactory.getOMFactory().createOMNamespace(SYNAPSE_NAMESPACE, "");

    @Test()
    public void testInitialization() {

        Properties properties = new Properties();
        QuartzTaskManager quartzTaskManager = PowerMockito.mock(QuartzTaskManager.class);
        PowerMockito.when(quartzTaskManager.isInitialized()).thenReturn(true);
        TaskScheduler taskScheduler = new TaskScheduler("CheckPrice");
        taskScheduler.init(properties, quartzTaskManager);
        Assert.assertTrue("Task Scheduler not initialized.", taskScheduler.isInitialized());
    }

    @Test()
    public void testGetRunningTaskCount() {

        QuartzTaskManager quartzTaskManager = PowerMockito.mock(QuartzTaskManager.class);
        PowerMockito.when(quartzTaskManager.getRunningTaskCount()).thenReturn(1);
        TaskScheduler taskScheduler = new TaskScheduler("CheckPrice");
        taskScheduler.init(new Properties(), quartzTaskManager);
        Assert.assertEquals("Running task count is not the expected value.", 1, taskScheduler.getRunningTaskCount());
    }

    @Test()
    public void testScheduleTask() throws Exception {

        String path = this.getClass().getClassLoader().getResource("task/task_TestScheduler.xml").getFile();
        OMElement taskOme = loadOMElement(path);
        TaskDescription taskDescription = TaskDescriptionFactory.createTaskDescription(taskOme, SYNAPSE_OMNAMESPACE);
        QuartzTaskManager quartzTaskManager = PowerMockito.mock(QuartzTaskManager.class);
        PowerMockito.when(quartzTaskManager.schedule(taskDescription)).thenReturn(true);
        TaskScheduler taskScheduler = new TaskScheduler("task");
        Assert.assertFalse("Task cannot be scheduled before initialization.", taskScheduler.scheduleTask(taskDescription));
        taskScheduler.init(new Properties(), quartzTaskManager);
        Assert.assertTrue("Task at file task_TestScheduler.xml not " +
                "scheduled even after initializing the scheduler.", taskScheduler.scheduleTask(taskDescription));

    }

    private OMElement loadOMElement(String path) throws FileNotFoundException, XMLStreamException {
        OMElement definitions = new StAXOMBuilder(new FileInputStream(path)).getDocumentElement();
        definitions.build();
        return definitions;
    }

}
