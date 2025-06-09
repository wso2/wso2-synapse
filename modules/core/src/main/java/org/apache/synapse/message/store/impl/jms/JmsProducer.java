/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.message.store.impl.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.message.MessageProducer;
import org.apache.synapse.message.store.impl.commons.MessageConverter;
import org.apache.synapse.message.store.impl.commons.StorableMessage;

import javax.jms.*;
import java.util.Map;
import java.util.Set;

public class JmsProducer implements MessageProducer {
    private static final String JMS_PROD_TIME_TO_LIVE = "JMS_PROD_TIME_TO_LIVE";
    private static final String JMS_PROD_DISABLE_MSG_TIMESTAMP = "JMS_PROD_DISABLE_MSG_TIMESTAMP";
    private static final String JMS_PROD_DELIVERY_MODE = "JMS_PROD_DELIVERY_MODE";
    private static final String JMS_PROD_DISABLE_MSG_ID = "JMS_PROD_DISABLE_MSG_ID";
    private static final String JMS_PROD_PRIORITY = "JMS_PROD_PRIORITY";
    private static final int DEFAULT_PRIORITY = 4;
    // prefix used to set JMS Message level properties. ex: JMS_MSG_P_brokerSpecificProperty
    private static final String JMS_MSG_P = "JMS_MSG_P_";
    private static final Log logger = LogFactory.getLog(JmsProducer.class.getName());

    private static final String OriginalMessageID = "OrigMessageID";

    private javax.jms.Connection javaxConnection;

    private jakarta.jms.Connection jakartaConnection;

    private javax.jms.Session javaxSession;

    private jakarta.jms.Session jakartaSession;

    private javax.jms.MessageProducer javaxProducer;

    private jakarta.jms.MessageProducer jakartaProducer;

    private JmsStore store;

    private String idString;

    private boolean isConnectionError = false;

    private boolean isInitialized = false;

    private boolean isVersion30 = false;

    public JmsProducer(JmsStore store, boolean isVersion30) {
        if (store == null) {
            logger.error("Cannot initialize.");
            return;
        }
        this.store = store;
        this.isVersion30 = isVersion30;
        isInitialized = true;
    }

    public boolean storeMessage(MessageContext synCtx) {
        if (synCtx == null) {
            return false;
        }
        if (!checkConnection()) {
            logger.warn(getId() + ". Ignored MessageID : " + synCtx.getMessageID());
            store.setCachedProducer(null);
            store.setProducer(null);
            return false;
        }
        if (isVersion30) {
           return storeJakartaMessage(synCtx);
        } else {
            return storeJavaxMessage(synCtx);
        }
    }

    private boolean storeJavaxMessage(MessageContext synCtx) {
        StorableMessage message = MessageConverter.toStorableMessage(synCtx);
        boolean error = false;
        Throwable throwable = null;
        try {
            ObjectMessage objectMessage = javaxSession.createObjectMessage(message);
            objectMessage.setStringProperty(OriginalMessageID, synCtx.getMessageID());
            setJavaxPriority(javaxProducer, objectMessage, message);
            setJavaxJmsProducerProperties(javaxProducer, synCtx);
            setJavaxJmsMessageProperties(objectMessage, synCtx);
            setJavaxTransportHeaders(objectMessage,synCtx);
            javaxProducer.send(objectMessage);

            if (javaxSession.getTransacted()) {
                javaxSession.commit();
            }

        } catch (JMSException e) {
            throwable = e;
            error = true;
            isConnectionError = true;
            try {
                if (javaxSession.getTransacted()) {
                    javaxSession.rollback();
                }
            } catch (JMSException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Fail to rollback message [" + synCtx.getMessageID() + "] from the message store " +
                            ":" + store.getName(), ex);
                }
            }
        } catch (Throwable t) {
            throwable = t;
            error = true;
            try {
                if (javaxSession.getTransacted()) {
                    javaxSession.rollback();
                }
            } catch (JMSException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Fail to rollback message [" + synCtx.getMessageID()+"] from the message store " +
                            ":" + store.getName(), e);
                }
            }
        }

        if (error) {
            String errorMsg = getId() + ". Ignored MessageID : " + synCtx.getMessageID()
                    + ". Could not store message to store ["
                    + store.getName() + "]. Error:" + throwable.getLocalizedMessage();
            logger.error(errorMsg, throwable);
            try {
                store.closeJavaxWriteConnection();
            } catch (JMSException e) {
                logger.error("Error while closing connection  to store " + store.getName(), e);
            }
            javaxConnection = null;
            if (logger.isDebugEnabled()) {
                logger.debug(getId() + ". Ignored MessageID : " + synCtx.getMessageID());
            }
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(getId() + ". Stored MessageID : " + synCtx.getMessageID());
        }
        store.enqueued();
        return true;
    }

    private boolean storeJakartaMessage(MessageContext synCtx) {
        StorableMessage message = MessageConverter.toStorableMessage(synCtx);
        boolean error = false;
        Throwable throwable = null;
        try {
            jakarta.jms.ObjectMessage objectMessage = jakartaSession.createObjectMessage(message);
            objectMessage.setStringProperty(OriginalMessageID, synCtx.getMessageID());
            setJakartaPriority(jakartaProducer, objectMessage, message);
            setJakartaJmsProducerProperties(jakartaProducer, synCtx);
            setJakartaJmsMessageProperties(objectMessage, synCtx);
            setJakartaTransportHeaders(objectMessage,synCtx);
            jakartaProducer.send(objectMessage);

            if (jakartaSession.getTransacted()) {
                jakartaSession.commit();
            }

        } catch (jakarta.jms.JMSException e) {
            throwable = e;
            error = true;
            isConnectionError = true;
            try {
                if (jakartaSession.getTransacted()) {
                    jakartaSession.rollback();
                }
            } catch (jakarta.jms.JMSException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Fail to rollback message [" + synCtx.getMessageID() + "] from the message store " +
                            ":" + store.getName(), ex);
                }
            }
        } catch (Throwable t) {
            throwable = t;
            error = true;
            try {
                if (jakartaSession.getTransacted()) {
                    jakartaSession.rollback();
                }
            } catch (jakarta.jms.JMSException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Fail to rollback message [" + synCtx.getMessageID()+"] from the message store " +
                            ":" + store.getName(), e);
                }
            }
        }

        if (error) {
            String errorMsg = getId() + ". Ignored MessageID : " + synCtx.getMessageID()
                    + ". Could not store message to store ["
                    + store.getName() + "]. Error:" + throwable.getLocalizedMessage();
            logger.error(errorMsg, throwable);
            try {
                store.closeJakartaWriteConnection();
            } catch (jakarta.jms.JMSException e) {
                logger.error("Error while closing connection  to store " + store.getName(), e);
            }
            jakartaConnection = null;
            if (logger.isDebugEnabled()) {
                logger.debug(getId() + ". Ignored MessageID : " + synCtx.getMessageID());
            }
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(getId() + ". Stored MessageID : " + synCtx.getMessageID());
        }
        store.enqueued();
        return true;
    }

    public boolean cleanup() {
        if (isVersion30) {
            try {
                store.cleanupJavax(null, javaxSession);
                return true;
            } catch (javax.jms.JMSException e) {
                throw new SynapseException("Error while cleaning up connection to store " + store.getName(), e);
            }
        } else {
            try {
                store.cleanupJakarta(null, jakartaSession);
                return true;
            } catch (jakarta.jms.JMSException e) {
                throw new SynapseException("Error while cleaning up connection to store " + store.getName(), e);
            }
        }
    }

    public JmsProducer setJavaxConnection(Connection connection) {
        this.javaxConnection = connection;
        return this;
    }

    public JmsProducer setJakartaConnection(jakarta.jms.Connection connection) {
        this.jakartaConnection = connection;
        return this;
    }

    public JmsProducer setJavaxSession(javax.jms.Session session) {
        this.javaxSession = session;
        return this;
    }

    public JmsProducer setJakartaSession(jakarta.jms.Session session) {
        this.jakartaSession = session;
        return this;
    }

    public JmsProducer setJavaxProducer(javax.jms.MessageProducer producer) {
        this.javaxProducer = producer;
        return this;
    }

    public JmsProducer setJakartaProducer(jakarta.jms.MessageProducer producer) {
        this.jakartaProducer = producer;
        return this;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setId(int id) {
        idString = "[" + store.getName() + "-P-" + id + "]";
    }

    public String getId() {
        return getIdAsString();
    }

    private String getIdAsString() {
        if (idString == null) {
            return "[unknown-producer]";
        }
        return idString;
    }

    private boolean checkConnection() {
        if (isVersion30) {
            return checkJakartaConnection();
        } else {
            return checkJavaxConnection();
        }
    }

    private boolean checkJavaxConnection() {
        if (javaxProducer == null) {
            if (logger.isDebugEnabled()) {
                logger.error(getId() + " cannot proceed. Message producer is null.");
            }
            return false;
        }
        if (javaxSession == null) {
            if (logger.isDebugEnabled()) {
                logger.error(getId() + " cannot proceed. JMS Session is null.");
            }
            return false;
        }
        if (javaxConnection == null) {
            if (logger.isDebugEnabled()) {
                logger.error(getId() + " cannot proceed. JMS Connection is null.");
            }
            return false;
        }
        return true;
    }

    private boolean checkJakartaConnection() {
        if (jakartaProducer == null) {
            if (logger.isDebugEnabled()) {
                logger.error(getId() + " cannot proceed. Message producer is null.");
            }
            return false;
        }
        if (jakartaSession == null) {
            if (logger.isDebugEnabled()) {
                logger.error(getId() + " cannot proceed. JMS Session is null.");
            }
            return false;
        }
        if (jakartaConnection == null) {
            if (logger.isDebugEnabled()) {
                logger.error(getId() + " cannot proceed. JMS Connection is null.");
            }
            return false;
        }
        return true;
    }

    private void setJavaxPriority(javax.jms.MessageProducer producer, javax.jms.ObjectMessage objectMessage,
                                  StorableMessage message) {
        if (message.getPriority(DEFAULT_PRIORITY) != Message.DEFAULT_PRIORITY) {
            try {
                producer.setPriority(message.getPriority(DEFAULT_PRIORITY));
            } catch (javax.jms.JMSException e) {
                logger.warn(getId() + " could not set priority ["
                            + message.getPriority(DEFAULT_PRIORITY) + "]");
            }
        }  else {
            try {
                producer.setPriority(Message.DEFAULT_PRIORITY);
            } catch (javax.jms.JMSException e) {}
        }
    }

    private void setJakartaPriority(jakarta.jms.MessageProducer producer, jakarta.jms.ObjectMessage objectMessage,
                             StorableMessage message) {
        if (message.getPriority(DEFAULT_PRIORITY) != Message.DEFAULT_PRIORITY) {
            try {
                producer.setPriority(message.getPriority(DEFAULT_PRIORITY));
            } catch (jakarta.jms.JMSException e) {
                logger.warn(getId() + " could not set priority ["
                        + message.getPriority(DEFAULT_PRIORITY) + "]");
            }
        }  else {
            try {
                producer.setPriority(Message.DEFAULT_PRIORITY);
            } catch (jakarta.jms.JMSException e) {}
        }
    }

    private void setJavaxJmsProducerProperties(javax.jms.MessageProducer producer, MessageContext synCtx) {
        Object prop = synCtx.getProperty(JMS_PROD_TIME_TO_LIVE);
        long ttl = Message.DEFAULT_TIME_TO_LIVE;
        if (prop instanceof String) {
            ttl = Long.parseLong((String) prop);
        } else if (prop instanceof Long || prop instanceof Integer) {
            ttl = Long.parseLong(prop.toString());
        }
        prop = synCtx.getProperty(JMS_PROD_DISABLE_MSG_TIMESTAMP);
        boolean disableMessageTimestamp = false;
        if (prop instanceof String) {
            disableMessageTimestamp = Boolean.parseBoolean((String) prop);
        } else if (prop instanceof Boolean) {
            disableMessageTimestamp = (Boolean) prop;
        }
        prop = synCtx.getProperty(JMS_PROD_DELIVERY_MODE);
        int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
        if (prop instanceof Integer) {
            deliveryMode = (Integer) prop;
        } else if (prop instanceof String) {
            deliveryMode = Integer.parseInt((String) prop);
        }
        prop = synCtx.getProperty(JMS_PROD_DISABLE_MSG_ID);
        boolean disableMessageId = false;
        if (prop instanceof String) {
            disableMessageId = Boolean.parseBoolean((String) prop);
        } else if (prop instanceof Boolean) {
            disableMessageId = (Boolean) prop;
        }
        prop = synCtx.getProperty(JMS_PROD_PRIORITY);
        int priority = Message.DEFAULT_PRIORITY;
        if (prop instanceof Integer) {
            priority = (Integer) prop;
        } else if (prop instanceof String) {
            priority = Integer.parseInt((String) prop);
        }
        try {
            producer.setTimeToLive(ttl);
            producer.setDisableMessageTimestamp(disableMessageTimestamp);
            producer.setDeliveryMode(deliveryMode);
            producer.setDisableMessageID(disableMessageId);
            producer.setPriority(priority);
        } catch (JMSException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not save Producer property: " + e.getLocalizedMessage());
            }
        }
    }

    private void setJakartaJmsProducerProperties(jakarta.jms.MessageProducer producer, MessageContext synCtx) {
        Object prop = synCtx.getProperty(JMS_PROD_TIME_TO_LIVE);
        long ttl = Message.DEFAULT_TIME_TO_LIVE;
        if (prop instanceof String) {
            ttl = Long.parseLong((String) prop);
        } else if (prop instanceof Long || prop instanceof Integer) {
            ttl = Long.parseLong(prop.toString());
        }
        prop = synCtx.getProperty(JMS_PROD_DISABLE_MSG_TIMESTAMP);
        boolean disableMessageTimestamp = false;
        if (prop instanceof String) {
            disableMessageTimestamp = Boolean.parseBoolean((String) prop);
        } else if (prop instanceof Boolean) {
            disableMessageTimestamp = (Boolean) prop;
        }
        prop = synCtx.getProperty(JMS_PROD_DELIVERY_MODE);
        int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
        if (prop instanceof Integer) {
            deliveryMode = (Integer) prop;
        } else if (prop instanceof String) {
            deliveryMode = Integer.parseInt((String) prop);
        }
        prop = synCtx.getProperty(JMS_PROD_DISABLE_MSG_ID);
        boolean disableMessageId = false;
        if (prop instanceof String) {
            disableMessageId = Boolean.parseBoolean((String) prop);
        } else if (prop instanceof Boolean) {
            disableMessageId = (Boolean) prop;
        }
        prop = synCtx.getProperty(JMS_PROD_PRIORITY);
        int priority = Message.DEFAULT_PRIORITY;
        if (prop instanceof Integer) {
            priority = (Integer) prop;
        } else if (prop instanceof String) {
            priority = Integer.parseInt((String) prop);
        }
        try {
            producer.setTimeToLive(ttl);
            producer.setDisableMessageTimestamp(disableMessageTimestamp);
            producer.setDeliveryMode(deliveryMode);
            producer.setDisableMessageID(disableMessageId);
            producer.setPriority(priority);
        } catch (jakarta.jms.JMSException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not save Producer property: " + e.getLocalizedMessage());
            }
        }
    }

    private void setJavaxJmsMessageProperties(javax.jms.Message message, MessageContext synCtx) {
        Set<String> properties = synCtx.getPropertyKeySet();
        for (String prop : properties) {
            if (prop.startsWith(JMS_MSG_P)) {
                Object value = synCtx.getProperty(prop);
                String key = prop.substring(JMS_MSG_P.length());
                try {
                    if (value instanceof String) {
                        message.setStringProperty(key, (String) value);
                    } else if (value instanceof Long) {
                        message.setLongProperty(key, (Long) value);
                    } else if (value instanceof Integer) {
                        message.setIntProperty(key, (Integer) value);
                    } else if (value instanceof Boolean) {
                        message.setBooleanProperty(key, (Boolean) value);
                    } else if (value instanceof Double) {
                        message.setDoubleProperty(key, (Double) value);
                    } else if (value instanceof Float) {
                        message.setFloatProperty(key, (Float) value);
                    } else if (value instanceof Short) {
                        message.setShortProperty(key, (Short) value);
                    }
                } catch (javax.jms.JMSException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not save Message property: " + e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    private void setJakartaJmsMessageProperties(jakarta.jms.Message message, MessageContext synCtx) {
        Set<String> properties = synCtx.getPropertyKeySet();
        for (String prop : properties) {
            if (prop.startsWith(JMS_MSG_P)) {
                Object value = synCtx.getProperty(prop);
                String key = prop.substring(JMS_MSG_P.length());
                try {
                    if (value instanceof String) {
                        message.setStringProperty(key, (String) value);
                    } else if (value instanceof Long) {
                        message.setLongProperty(key, (Long) value);
                    } else if (value instanceof Integer) {
                        message.setIntProperty(key, (Integer) value);
                    } else if (value instanceof Boolean) {
                        message.setBooleanProperty(key, (Boolean) value);
                    } else if (value instanceof Double) {
                        message.setDoubleProperty(key, (Double) value);
                    } else if (value instanceof Float) {
                        message.setFloatProperty(key, (Float) value);
                    } else if (value instanceof Short) {
                        message.setShortProperty(key, (Short) value);
                    }
                } catch (jakarta.jms.JMSException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not save Message property: " + e.getLocalizedMessage());
                    }
                }
            }
        }
    }
    private void setJavaxTransportHeaders(javax.jms.Message message, MessageContext synCtx){
        //Set transport headers to the message
        Map<?,?> headerMap = (Map<?,?>) ((Axis2MessageContext)synCtx).getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if(headerMap != null) {
            for (Object headerName : headerMap.keySet()) {
                String name = (String) headerName;
                Object value = headerMap.get(name);
                try {
                    if (value instanceof String) {
                        message.setStringProperty(name, (String) value);
                    } else if (value instanceof Boolean) {
                        message.setBooleanProperty(name, (Boolean) value);
                    } else if (value instanceof Integer) {
                        message.setIntProperty(name, (Integer) value);
                    } else if (value instanceof Long) {
                        message.setLongProperty(name, (Long) value);
                    } else if (value instanceof Double) {
                        message.setDoubleProperty(name, (Double) value);
                    } else if (value instanceof Float) {
                        message.setFloatProperty(name, (Float) value);
                    }
                } catch (javax.jms.JMSException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not save Message property: " + ex.getLocalizedMessage());
                    }
                }
            }
        }
    }

    private void setJakartaTransportHeaders(jakarta.jms.Message message, MessageContext synCtx){
        //Set transport headers to the message
        Map<?,?> headerMap = (Map<?,?>) ((Axis2MessageContext)synCtx).getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if(headerMap != null) {
            for (Object headerName : headerMap.keySet()) {
                String name = (String) headerName;
                Object value = headerMap.get(name);
                try {
                    if (value instanceof String) {
                        message.setStringProperty(name, (String) value);
                    } else if (value instanceof Boolean) {
                        message.setBooleanProperty(name, (Boolean) value);
                    } else if (value instanceof Integer) {
                        message.setIntProperty(name, (Integer) value);
                    } else if (value instanceof Long) {
                        message.setLongProperty(name, (Long) value);
                    } else if (value instanceof Double) {
                        message.setDoubleProperty(name, (Double) value);
                    } else if (value instanceof Float) {
                        message.setFloatProperty(name, (Float) value);
                    }
                } catch (jakarta.jms.JMSException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not save Message property: " + ex.getLocalizedMessage());
                    }
                }
            }
        }
    }

    public javax.jms.Session getJavaxSession(){
        return this.javaxSession;
    }

    public jakarta.jms.Session getJakartaSession(){
        return this.jakartaSession;
    }
}
