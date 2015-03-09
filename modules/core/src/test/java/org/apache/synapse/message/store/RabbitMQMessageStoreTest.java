/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.message.store;

import junit.framework.TestCase;
import org.apache.commons.lang.SerializationUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.message.store.impl.rabbitmq.MessageConverter;
import org.apache.synapse.message.store.impl.rabbitmq.RabbitmqStore;
import org.apache.synapse.message.store.impl.rabbitmq.AMQPStorableMessage;

public class RabbitMQMessageStoreTest extends TestCase {
    
//    public void testBasics() throws Exception {
//        System.out.println("Testing Basic RabbitMQStore operations...");
//        MessageStore store = new InMemoryStore();
//        populateStore(store, 10);
//
//        // test size()
//        assertEquals(10, store.size());
//
//
//        // test get(index)
//        for (int i = 0; i < 10; i++) {
//            assertEquals("ID" + i, store.get(i).getMessageID());
//        }
//
//
//        // test get(messageId)
//        for (int i = 0; i < 10; i++) {
//            assertEquals("ID" + i, store.get("ID" + i).getMessageID());
//        }
//
//
//        // test getAll()
//        List<MessageContext> list = store.getAll();
//        assertEquals(10, list.size());
//
//
//        for (int i = 0; i < 10; i++) {
//            assertEquals("ID" + i, list.get(i).getMessageID());
//        }
//
//        // test receive()
//        MessageConsumer consumer = store.getConsumer();
//        for (int i = 0; i < 10; i++) {
//            assertEquals("ID" + i, consumer.receive().getMessageID());
//            consumer.ack();
//        }
//
//
//        populateStore(store, 10);
//
//        // test remove()
//        for (int i = 0; i < 10; i++) {
//            assertEquals("ID" + i, store.remove().getMessageID());
//        }
//
//        try {
//            store.remove();
//            fail();
//        } catch (NoSuchElementException expected) {}
//
//        populateStore(store, 10);
//
//        // test clear()
//        assertEquals(10, store.size());
//        store.clear();
//        assertEquals(0, store.size());
//
//    }
//
//    public void testOrderedDelivery1() throws Exception {
//        System.out.println("Testing InMemoryStore Ordered Delivery...");
//        MessageStore store = new InMemoryStore();
//        for (int i = 0; i < 100; i++) {
//            store.getProducer().storeMessage(createMessageContext("ID" + i));
//        }
//        MessageConsumer consumer = store.getConsumer();
//        for (int i = 0; i < 100; i++) {
//            assertEquals("ID" + i, consumer.receive().getMessageID());
//            consumer.ack();
//        }
//    }
    
    public void testOrderedDelivery2() throws  Exception {
        System.out.println("Testing RabbitMQStore Guaranteed Delivery...");
        RabbitmqStore store = new RabbitmqStore();
        MessageContext synContext = createMessageContext("FOO");
       store.getProducer().storeMessage(synContext);
//        MessageConsumer consumer = store.getConsumer();
        AMQPStorableMessage message = MessageConverter.toStorableMessage(synContext);
        byte[] msgAry = SerializationUtils.serialize(message);
        AMQPStorableMessage reply = (AMQPStorableMessage)SerializationUtils.deserialize(msgAry);
        System.out.println("Stop");
//        assertEquals(message.getSynapseMessage().get,reply.getSynapseMessage().getProperties().get());
//        org.apache.axis2.context.MessageContext axis2Mc = store.newAxis2Mc();
//        MessageContext synapseMc = store.newSynapseMc(axis2Mc);
//        synapseMc = MessageConverter.toMessageContext(reply, axis2Mc, synapseMc);
//        assertEquals("FOO", synapseMc.getMessageID());
    }

//    public void testSerialization() throws  Exception {
//        System.out.println("Testing RabbitMQStore testing serialization...");
//        String QUEUE_NAME = "ESBStoreTestQueue";
//        MessageContext synContext = createMessageContext("FOO");
//        StorableMessage message = MessageConverter.toStorableMessage(synContext);
//        ConnectionFactory factory = new ConnectionFactory();
//        Connection connection = factory.newConnection();
//        Channel channel = connection.createChannel();
//        channel.queueDeclare(QUEUE_NAME, false, false, true, null);
//        //publish
//        byte[] msgAry = SerializationUtils.serialize(message);
//        channel.basicPublish("", QUEUE_NAME , new AMQP.BasicProperties.Builder().contentType("text/plain").build(), msgAry);
//
//        //consume
//        QueueingConsumer consumer = new QueueingConsumer(channel);
//        channel.basicConsume(QUEUE_NAME, true, consumer);
//        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
//
//        StorableMessage reply = (StorableMessage)SerializationUtils.deserialize(delivery.getBody());
//
//        channel.close();
//        connection.close();
//
//        System.out.println("Serialization successful");
//    }
    
    private MessageContext createMessageContext(String identifier) throws Exception {
        MessageContext msg = TestUtils.createLightweightSynapseMessageContext("<test/>");
        msg.setMessageID(identifier);
        return msg;
    }
    

}
