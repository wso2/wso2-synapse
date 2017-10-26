/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.jmx;

import junit.framework.TestCase;
import java.util.Properties;

/**
 * Test class for JmxInformationFactory
 */
public class JmxInformationFactoryTest extends TestCase {

    /**
     * Test creating @{@link JmxInformation} from Properties
     */
    public void testCreateJmxInformation() {
        Properties properties = new Properties();
        properties.put("synapse.jmx.jndiPort", "1100");
        properties.put("synapse.jmx.rmiPort", "9999");
        properties.put("synapse.jmx.username", "user1");
        JmxInformation jmxInformation = JmxInformationFactory.createJmxInformation(properties, "localhost");

        assertEquals("Incorrect jndi port", jmxInformation.getJndiPort(), 1100);
        assertEquals("Incorrect rmi port", jmxInformation.getRmiPort(), 9999);
        assertEquals("Incorrect host", jmxInformation.getHostName(), "localhost");
        assertTrue("JmxInformation Authenticate should be true", jmxInformation.isAuthenticate());
    }

    /**
     * Test update jmx url
     */
    public void testUpdateJmxUrl() {
        Properties properties = new Properties();
        properties.put("synapse.jmx.jndiPort", "1100");
        properties.put("synapse.jmx.rmiPort", "9999");
        JmxInformation jmxInformation = JmxInformationFactory.createJmxInformation(properties, "localhost");

        assertNull("Null value expected for Jmx URL before updating", jmxInformation.getJmxUrl());
        jmxInformation.updateJMXUrl();
        assertEquals("Incorrect Jmx URL", "service:jmx:rmi://localhost:9999/jndi/rmi://localhost:1100/synapse", jmxInformation.getJmxUrl());
    }
}
