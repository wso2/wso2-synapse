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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Class for TaskSchedulerFactory
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(TaskSchedulerFactory.class)
public class TaskSchedulerFactoryTest {
    @Test
    public void testGetTaskScheduler() throws Exception {
        final String name = "new";
        Map<String, TaskScheduler> map = new HashMap<String, TaskScheduler>();
        PowerMockito.whenNew(HashMap.class).withNoArguments().thenReturn((HashMap) map);
        TaskScheduler taskSchedulerNew = TaskSchedulerFactory.getTaskScheduler(name);
        Assert.assertNotNull("Task Object null", taskSchedulerNew);
        Assert.assertNotNull("Task Scheduler not found in the map", map.get(name));
        Assert.assertEquals("Objects mismatched in the map and return object", taskSchedulerNew, map.get(name));
        TaskScheduler taskSchedulerExisting = TaskSchedulerFactory.getTaskScheduler(name);
        Assert.assertEquals("Can not get existing Task", taskSchedulerNew, taskSchedulerExisting);
    }

    @Test(expected = SynapseTaskException.class)
    public void testGetTaskSchedulerForEmptyName() throws Exception {
        TaskSchedulerFactory.getTaskScheduler("");
    }

    @Test(expected = SynapseTaskException.class)
    public void testGetTaskSchedulerNameNull() throws Exception {
        TaskSchedulerFactory.getTaskScheduler(null);
    }
}
