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
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.startup.quartz.QuartzTaskManager;
import org.apache.synapse.task.TaskManager;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.util.Iterator;
import java.util.Properties;

/**
 * Unit tests for TaskManagerSerializer class.
 */
public class TaskManagerSerializerTest {

    /**
     * Test serializetaskManager method by asserting resulting OMElement.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testCase() throws XMLStreamException {
        String input = "<template xmlns=\"http://ws.apache.org/ns/synapse\" name=\"HelloWordLogger\">\n" +
                "   <sequence>\n" +
                "      <log level=\"full\">\n" +
                "         <property xmlns:ns2=\"http://org.apache.synapse/xsd\" name=\"message\" " +
                "expression=\"$func:message\"></property>\n" +
                "      </log>\n" +
                "   </sequence>\n" +
                "</template>";
        OMElement element = AXIOMUtil.stringToOM(input);
        TaskManager taskManager = new QuartzTaskManager();
        Properties properties = new Properties();
        properties.setProperty("name", "testName");
        taskManager.setConfigurationProperties(properties);
        OMElement omElement = TaskManagerSerializer.serializetaskManager(element, taskManager);
        Assert.assertEquals("asserting localName inserted by the method", "taskManager", omElement.getLocalName());
        Iterator iter = omElement.getChildElements();
        while (iter.hasNext()) {
            OMElementImpl impl = (OMElementImpl) iter.next();
            Assert.assertEquals("asserting localName inserted by the method", "parameter", impl.getLocalName());
        }
    }
}
