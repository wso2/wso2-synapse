/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.protocol.jms;

import javax.jms.*;
import javax.naming.*;
import java.util.Properties;

public class JMSUtils {

    public static Destination lookupDestination(Context context, String destinationName,
                                                JMSConstants.JMSDestinationType destinationType) throws NamingException {

        if (destinationName == null) {
            return null;
        }

        try {
            return JMSUtils.lookup(context, Destination.class, destinationName);
        } catch (NameNotFoundException e) {
            try {
                Properties initialContextProperties = new Properties();
                if (context.getEnvironment() != null) {
                    if (context.getEnvironment().get(JMSConstants.JAVA_INITIAL_NAMING_FACTORY) != null) {
                        initialContextProperties.put(JMSConstants.JAVA_INITIAL_NAMING_FACTORY, context.getEnvironment().get(JMSConstants.JAVA_INITIAL_NAMING_FACTORY));
                    }
                    if (context.getEnvironment().get(JMSConstants.CONNECTION_FACTORY_JNDI_NAME) != null) {
                        initialContextProperties.put(JMSConstants.CONNECTION_FACTORY_JNDI_NAME, context.getEnvironment().get(JMSConstants.CONNECTION_FACTORY_JNDI_NAME));
                    }
                    if(context.getEnvironment().get(JMSConstants.JAVA_NAMING_PROVIDER_URL) != null){
                        initialContextProperties.put(JMSConstants.JAVA_NAMING_PROVIDER_URL, context.getEnvironment().get(JMSConstants.JAVA_NAMING_PROVIDER_URL));
                    }
                }

                if (JMSConstants.JMSDestinationType.TOPIC.equals(destinationType)) {
                    initialContextProperties.put(JMSConstants.TOPIC_PREFIX + destinationName, destinationName);
                } else {
                    initialContextProperties.put(JMSConstants.QUEUE_PREFIX + destinationName, destinationName);
                }

                InitialContext initialContext = new InitialContext(initialContextProperties);
                try {
                    return JMSUtils.lookup(initialContext, Destination.class, destinationName);
                } catch (NamingException e1) {
                    return JMSUtils.lookup(context, Destination.class,
                            (JMSConstants.JMSDestinationType.TOPIC.equals(destinationType) ?
                                    "dynamicTopics/" : "dynamicQueues/") + destinationName);
                }

            } catch (NamingException x) {
                throw x;
            }
        } catch (NamingException e) {
            throw e;
        }
    }


    public static Connection createConnection(ConnectionFactory conFac,
                                              String user, String pass, boolean jmsSpec11, Boolean isQueue,
                                              boolean isDurable, String clientID) throws JMSException {

        Connection connection = null;

        if (jmsSpec11 || isQueue == null) {
            if (user != null && pass != null) {
                connection = conFac.createConnection(user, pass);
            } else {
                connection = conFac.createConnection();
            }
            if(isDurable){
                connection.setClientID(clientID);
            }

        } else {
            QueueConnectionFactory qConFac = null;
            TopicConnectionFactory tConFac = null;
            if (isQueue) {
                qConFac = (QueueConnectionFactory) conFac;
            } else {
                tConFac = (TopicConnectionFactory) conFac;
            }

            if (user != null && pass != null) {
                if (qConFac != null) {
                    connection = qConFac.createQueueConnection(user, pass);
                } else if (tConFac != null) {
                    connection = tConFac.createTopicConnection(user, pass);
                }
            } else {
                if (qConFac != null) {
                    connection = qConFac.createQueueConnection();
                } else if (tConFac != null) {
                    connection = tConFac.createTopicConnection();
                }
            }
            if(isDurable){
                connection.setClientID(clientID);
            }
        }
        return connection;
    }


    public static MessageProducer createProducer(
            Session session, Destination destination, Boolean isQueue, boolean jmsSpec11) throws JMSException {

        if (jmsSpec11 || isQueue == null) {
            return session.createProducer(destination);
        } else {
            if (isQueue) {
                return ((QueueSession) session).createSender((Queue) destination);
            } else {
                return ((TopicSession) session).createPublisher((Topic) destination);
            }
        }
    }

    public static Session createSession(Connection connection, boolean transacted, int ackMode,
                                        boolean jmsSpec11, Boolean isQueue) throws JMSException {

        if (isQueue) {
            return ((QueueConnection) connection).createQueueSession(transacted, ackMode);
        }
        return null;
    }


    public static MessageConsumer createConsumer(
            Session session, Destination destination, Boolean isQueue,
            String subscriberName, String messageSelector, boolean pubSubNoLocal,
            boolean isDurable, boolean jmsSpec11) throws JMSException {

        if (isQueue) {
            return ((QueueSession) session).createReceiver((Queue) destination, messageSelector);
        }

        return null;
    }

    public static MessageConsumer createConsumer(Session session, Destination dest, String messageSelector)
            throws JMSException {

        if (dest instanceof Queue) {
            return ((QueueSession) session).createReceiver((Queue) dest, messageSelector);
        } else {
            return ((TopicSession) session).createSubscriber((Topic) dest, messageSelector, false);
        }
    }


    public static <T> T lookup(Context context, Class<T> clazz, String name)
            throws NamingException {

        Object object = context.lookup(name);
        try {
            return clazz.cast(object);
        } catch (ClassCastException ex) {
            // Instead of a ClassCastException, throw an exception with some
            // more information.
            if (object instanceof Reference) {
                Reference ref = (Reference)object;
                /*handleException("JNDI failed to de-reference Reference with name " +
                        name + "; is the factory " + ref.getFactoryClassName() +
                        " in your classpath?");*/
                return null;
            } else {
              /*  handleException("JNDI lookup of name " + name + " returned a " +
                        object.getClass().getName() + " while a " + clazz + " was expected");*/
                return null;
            }
        }
    }

    public static String inferJMSMessageType(Message msg) {
        if(inferTextMessage(msg)) {
            return TextMessage.class.getName();
        } else if(inferByteMessage(msg)) {
            return BytesMessage.class.getName();
        } else if(inferObjectMessage(msg)) {
            return ObjectMessage.class.getName();
        } else if(inferStreamMessage(msg)) {
            return StreamMessage.class.getName();
        } else {
            return null;
        }
    }

    private static boolean inferTextMessage(Message msg) {
        if (msg instanceof TextMessage) {
            return true;
        }
        return false;
    }

    private static boolean inferStreamMessage(Message msg) {
        if (msg instanceof StreamMessage) {
            return true;
        }
        return false;
    }

    private static boolean inferObjectMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            return true;
        }
        return false;
    }

    private static boolean inferByteMessage(Message msg) {
        if (msg instanceof BytesMessage) {
            return true;
        }
        return false;
    }
}
