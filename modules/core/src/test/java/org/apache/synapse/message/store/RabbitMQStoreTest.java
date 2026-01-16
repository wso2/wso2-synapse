/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.message.store;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;
import junit.framework.Assert;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.message.store.impl.rabbitmq.RabbitMQConsumer;
import org.apache.synapse.message.store.impl.rabbitmq.RabbitMQProducer;
import org.apache.synapse.message.store.impl.rabbitmq.RabbitMQStore;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;


/**
 * Unit tests for RabbitMQStore class
 */
public class RabbitMQStoreTest {

    private static RabbitMQStore rabbitMQStore;
    private static Field connectionFactory;
    private static Field addresses;
    private static ConfigurationContext configurationContext;
    private static final String USERNAME = "TestUserName";
    private static final String PASSWORD = "TestPassword";
    private static final String HOST = "TestHost";
    private static final String PORT = "1234";
    private static final String VIRTUAL_HOST = "TestVirtualHost";
    private static final String QUEUE = "TestQueue";

    @BeforeClass
    public static void init() throws NoSuchFieldException {
        rabbitMQStore = new RabbitMQStore();
        //Accessing private variable using reflection
        connectionFactory = RabbitMQStore.class.getDeclaredField("connectionFactory");
        connectionFactory.setAccessible(true);
        addresses = RabbitMQStore.class.getDeclaredField("addresses");
        addresses.setAccessible(true);
        //Setting parameters of the RabbitMQStore
        Map<String, Object> temp = new HashMap<>();
        temp.put(RabbitMQStore.USERNAME, USERNAME);
        temp.put(RabbitMQStore.PASSWORD, PASSWORD);
        temp.put(RabbitMQStore.HOST_NAME, HOST);
        temp.put(RabbitMQStore.HOST_PORT, PORT);
        temp.put(RabbitMQStore.VIRTUAL_HOST, VIRTUAL_HOST);
        temp.put(RabbitMQStore.QUEUE_NAME, QUEUE);
        temp.put(RabbitMQStore.RETRY_INTERVAL, "0");
        temp.put(RabbitMQStore.RETRY_COUNT, "0");
        rabbitMQStore.setParameters(temp);
        AxisConfiguration axisConfiguration = new AxisConfiguration();
        configurationContext = new ConfigurationContext(axisConfiguration);
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        Axis2SynapseEnvironment env = new Axis2SynapseEnvironment(configurationContext, synapseConfiguration);
        try {
            rabbitMQStore.init(env);
        } catch (Exception ignore) {
            // org.apache.synapse.SynapseException will throw wrapping the java.net.UnknownHostException because the
            // init method is trying to create an connection with the broker.
        }
    }

    /**
     * call init method with dummy values and validating connectionFactory object
     *
     * @throws IllegalAccessException
     */
    @Test
    public void testInit() throws IllegalAccessException {
        ConnectionFactory factory = (ConnectionFactory) connectionFactory.get(rabbitMQStore);
        Address[] addresses = (Address[]) RabbitMQStoreTest.addresses.get(rabbitMQStore);
        Assert.assertEquals("should return previously stored values", addresses[0].getPort(), Integer.parseInt(PORT));
        Assert.assertEquals("should return previously stored values", addresses[0].getHost(), HOST);
        Assert.assertEquals("should return previously stored values", factory.getPassword(), PASSWORD);
        Assert.assertEquals("should return previously stored values", factory.getUsername(), USERNAME);
        Assert.assertEquals("should return previously stored values", factory.getVirtualHost(), VIRTUAL_HOST);
    }

    /**
     * Creating a producer and validate initialized
     */
    @Test(expected = SynapseException.class)
    public void testGetProducer() {
        RabbitMQProducer messageProducer = (RabbitMQProducer) rabbitMQStore.getProducer();
        Assert.assertTrue("MessageProducer should be initialized", messageProducer.isInitialized());
    }

    /**
     * Creating a consumer and validating consumer id
     */
    @Test(expected = SynapseException.class)
    public void testGetConsumer() {
        RabbitMQConsumer messageConsumer = (RabbitMQConsumer) rabbitMQStore.getConsumer();
        Assert.assertEquals("Comparing MessageConsumer id", "[null-C-1]", messageConsumer.getId());
    }

    /**
     * Create axis2MessageContext
     */
    @Test
    public void testNewAxis2Mc() {
        MessageContext messageContext = rabbitMQStore.newAxis2Mc();
        Assert.assertEquals("MessageContext should be initiated with configurationContext",messageContext
                .getConfigurationContext(), configurationContext);
    }

    //Following test cases assert currently not implemented methods which are just returning null
    @Test
    public void testRemove() {
        Assert.assertNull(rabbitMQStore.remove());
    }

    @Test
    public void testGetType() {
        Assert.assertEquals(rabbitMQStore.getType(), Constants.RABBIT_MS);
    }

    @Test
    public void testRemoveMessage() {
        Assert.assertNull(rabbitMQStore.remove("Test"));
    }

    @Test
    public void testGet() {
        Assert.assertNull(rabbitMQStore.get(0));
    }

    @Test
    public void testGetAll() {
        Assert.assertNull(rabbitMQStore.getAll());
    }

    @Test
    public void testGetMessageId() {
        Assert.assertNull(rabbitMQStore.get("Test"));
    }

}
