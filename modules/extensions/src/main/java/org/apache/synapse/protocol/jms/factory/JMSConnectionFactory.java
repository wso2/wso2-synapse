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

package org.apache.synapse.protocol.jms.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.protocol.jms.JMSConstants;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.Properties;


/**
 * use of factory
 * server down and up
 * jms spec
 * transport.jms.MessageSelector
 * isDurable
 * 
 * */

public class JMSConnectionFactory implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory {
    private static final Log logger = LogFactory.getLog(JMSConnectionFactory.class.getName());

    protected Context ctx;
    protected ConnectionFactory connectionFactory;
    protected String connectionFactoryString;

    protected JMSConstants.JMSDestinationType destinationType;

    private Destination destination;
    protected String destinationName;

    protected boolean transactedSession = false;
    protected int sessionAckMode = 0;

    public JMSConnectionFactory(Properties properties) {
        try {
            ctx = new InitialContext(properties);
        } catch (NamingException e) {
            logger.error("NamingException while obtaining initial context.");
        }

        String connectionFactoryType = properties.getProperty(JMSConstants.CONNECTION_FACTORY_TYPE);
        if (connectionFactoryType.equals("topic")) {
            this.destinationType = JMSConstants.JMSDestinationType.TOPIC;
        } else {
            this.destinationType = JMSConstants.JMSDestinationType.QUEUE;
        }

        this.connectionFactoryString = properties.getProperty(JMSConstants.CONNECTION_FACTORY_JNDI_NAME);
        if(connectionFactoryString == null || "".equals(connectionFactoryString)) {
            connectionFactoryString = "QueueConnectionFactory";
        }

        this.destinationName = properties.getProperty(JMSConstants.DESTINATION_NAME);
        if(destinationName == null || "".equals(destinationName)) {
            destinationName = "QUEUE_" + System.currentTimeMillis();
        }

        String strTransactedSession = properties.getProperty(JMSConstants.SESSION_TRANSACTED);
        if(strTransactedSession == null || "".equals(strTransactedSession) || !strTransactedSession.equals("true")) {
            transactedSession = false;
        } else if(strTransactedSession.equals("true")) {
            transactedSession = true;
        }

        String strSessionAck = properties.getProperty(JMSConstants.SESSION_ACK);
        if(null == strSessionAck) {
            sessionAckMode = 1;
        } else if(strSessionAck.equals("AUTO_ACKNOWLEDGE")) {
            sessionAckMode = Session.AUTO_ACKNOWLEDGE;
        } else if(strSessionAck.equals("CLIENT_ACKNOWLEDGE")) {
            sessionAckMode = Session.CLIENT_ACKNOWLEDGE;
        } else if(strSessionAck.equals("DUPS_OK_ACKNOWLEDGE")) {
            sessionAckMode = Session.DUPS_OK_ACKNOWLEDGE;
        } else if(strSessionAck.equals("SESSION_TRANSACTED")) {
            sessionAckMode = Session.SESSION_TRANSACTED;
        } else {
            sessionAckMode = 1;
        }

        createConnectionFactory();
    }

    public ConnectionFactory getConnectionFactory() {
        if (this.connectionFactory != null) {
            return this.connectionFactory;
        }

        return createConnectionFactory();
    }

    private ConnectionFactory createConnectionFactory() {
        if (this.connectionFactory != null) {
            return this.connectionFactory;
        }

        try {
            if(this.destinationType.equals(JMSConstants.JMSDestinationType.QUEUE)) {
                this.connectionFactory = (QueueConnectionFactory) ctx.lookup(this.connectionFactoryString);
            } else if(this.destinationType.equals(JMSConstants.JMSDestinationType.TOPIC)) {
                this.connectionFactory = (TopicConnectionFactory) ctx.lookup(this.connectionFactoryString);
            }
        } catch (NamingException e) {
            logger.error("Naming exception while obtaining connection factory for '" + this.connectionFactoryString +"'");
        }

        return this.connectionFactory;
    }

    public Connection getConnection() {
        return createConnection();
    }

    public Connection createConnection() {
        try {
             if(this.destinationType.equals(JMSConstants.JMSDestinationType.QUEUE)) {
                 return ((QueueConnectionFactory) (this.connectionFactory)).createQueueConnection();
             } else if(this.destinationType.equals(JMSConstants.JMSDestinationType.TOPIC)) {
                 return ((TopicConnectionFactory) (this.connectionFactory)).createTopicConnection();
             }
        } catch (JMSException e) {
            logger.error("JMS Exception while creating connection through factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }

        return null;
    }

    public Connection createConnection(String userName, String password) {
        try {
            if(this.destinationType.equals(JMSConstants.JMSDestinationType.QUEUE)) {
                return ((QueueConnectionFactory) (this.connectionFactory)).createQueueConnection(userName, password);
            } else if(this.destinationType.equals(JMSConstants.JMSDestinationType.TOPIC)) {
                return ((TopicConnectionFactory) (this.connectionFactory)).createTopicConnection(userName, password);
            }
        } catch (JMSException e) {
            logger.error("JMS Exception while creating connection through factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }
        return null;
    }

    public QueueConnection createQueueConnection() throws JMSException {
        try {
            return ((QueueConnectionFactory) (this.connectionFactory)).createQueueConnection();
        } catch (JMSException e) {
            logger.error("JMS Exception while creating queue connection through factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }
        return null;
    }

    public QueueConnection createQueueConnection(String userName, String password) throws JMSException {
        try {
            return ((QueueConnectionFactory) (this.connectionFactory)).createQueueConnection(userName, password);
        } catch (JMSException e) {
            logger.error("JMS Exception while creating queue connection through factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }

        return null;
    }

    public TopicConnection createTopicConnection() throws JMSException {
        try {
            return ((TopicConnectionFactory) (this.connectionFactory)).createTopicConnection();
        } catch (JMSException e) {
            logger.error("JMS Exception while creating topic connection through factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }

        return null;
    }

    public TopicConnection createTopicConnection(String userName, String password) throws JMSException {
        try {
            return ((TopicConnectionFactory) (this.connectionFactory)).createTopicConnection(userName, password);
        } catch (JMSException e) {
            logger.error("JMS Exception while creating topic connection through factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }

        return null;
    }

    public Destination getDestination(Connection connection) {
        if(this.destination != null) {
            return this.destination;
        }

        return createDestination(connection);
    }

    private Destination createDestination(Connection connection) {
        if(this.destination != null) {
            return this.destination;
        }

        try {
            if(this.destinationType.equals(JMSConstants.JMSDestinationType.QUEUE)) {
                this.destination = (Queue) ctx.lookup(this.destinationName);
            } else if(this.destinationType.equals(JMSConstants.JMSDestinationType.TOPIC)) {
                this.destination = (Topic) ctx.lookup(this.destinationName);
            }
        } catch (NameNotFoundException e) {
            logger.warn("Could not find destination '" + this.destinationName + "' on connection factory for '" + this.connectionFactoryString + "'. " + e.getMessage());
            logger.info("Creating destination '" + this.destinationName + "' on connection factory for '" + this.connectionFactoryString + ".");

            Session createSession = createSession(connection);
            try {
                if(this.destinationType.equals(JMSConstants.JMSDestinationType.QUEUE)) {
                    this.destination = (Queue) createSession.createQueue(this.destinationName);
                } else if(this.destinationType.equals(JMSConstants.JMSDestinationType.TOPIC)) {
                    this.destination = (Topic) createSession.createTopic(this.destinationName);
                }
            } catch (JMSException e1) {
                logger.error("Could not find nor create '" + this.destinationName + "' on connection factory for '" + this.connectionFactoryString + "'. " + e.getMessage());
            }

        } catch (NamingException e) {
            logger.error("Naming exception while obtaining connection factory for '" + this.connectionFactoryString +"' " + e.getMessage());
        }

        return this.destination;
    }

    public Session getSession(Connection connection) {
        return createSession(connection);
    }

    protected Session createSession(Connection connection) {
        try {
            if(this.destinationType.equals(JMSConstants.JMSDestinationType.QUEUE)) {
                return (QueueSession) ((QueueConnection) (connection)).createQueueSession(transactedSession, sessionAckMode);
            } else if(this.destinationType.equals(JMSConstants.JMSDestinationType.TOPIC)) {
                return  (TopicSession) ((TopicConnection) (connection)).createTopicSession(transactedSession, sessionAckMode);
            }
        } catch (JMSException e) {
            logger.error("JMS Exception while obtaining session for factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }

        return null;
    }

    public void start(Connection connection) {
        try {
            connection.start();
        } catch (JMSException e) {
            logger.error("JMS Exception while starting connection for factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }
    }

    public void stop(Connection connection) {
        try {
            connection.stop();
        } catch (JMSException e) {
            logger.error("JMS Exception while stopping connection for factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }
    }

    public boolean closeConnection(Connection connection) {
        try {
            connection.close();
            return true;
        } catch (JMSException e) {
            logger.error("JMS Exception while closing the connection.");
        }

        return false;
    }

    public Context getContext() {
        return this.ctx;
    }

    public JMSConstants.JMSDestinationType getDestinationType() {
        return this.destinationType;
    }

    public String getConnectionFactoryString() {
        return connectionFactoryString;
    }
}
