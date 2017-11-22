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

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for TaskDescription
 */
public class TaskDescriptionTest {
    private TaskDescription taskDescription = new TaskDescription();

    @Test()
    public void addPropertyTest() {
        final String propName = "soapAction";
        final String propValue = "urn:getQuote";
        taskDescription.addProperty(propName, propValue);
        Assert.assertEquals("Property value mismatched", propValue, taskDescription.getProperty(propName));
        taskDescription.addProperty(null, null);
    }

    @Test()
    public void setPropertiesTest() {
        Map<String, String> properties = new HashMap<>();
        properties.put("soapAction", "urn:getQuote");
        properties.put("injectTo", "Proxy");
        properties.put("format", "soap11");
        taskDescription.setProperties(properties);
        Assert.assertEquals("Property mismatched", properties, taskDescription.getProperties());
        Assert.assertEquals("soap11", taskDescription.getProperty("format"));
        taskDescription.setProperties(null);
    }

    @Test()
    public void getTaskImplClassNameTest() {
        final String classNameBySetter = "org.wso2.task.MyTaskImpl1";
        final String classNameByProperty = "org.wso2.task.MyTaskImpl2";
        taskDescription.setTaskImplClassName(classNameBySetter);
        //check the task class when the property TaskDescription.CLASSNAME in not exist
        Assert.assertEquals("Task class not same as set", classNameBySetter, taskDescription.getTaskImplClassName());

        taskDescription.addProperty(TaskDescription.CLASSNAME, classNameByProperty);
        Assert.assertEquals("Task class not same as property ClassName", classNameByProperty,
                taskDescription.getTaskImplClassName());
    }
}
