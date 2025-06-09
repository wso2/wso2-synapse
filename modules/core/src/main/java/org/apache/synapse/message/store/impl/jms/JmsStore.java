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
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.MessageProducer;
import org.apache.synapse.message.StoreForwardException;
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.Constants;
import org.apache.synapse.util.resolver.SecureVaultResolver;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JmsStore extends AbstractMessageStore {
    protected static final Log log = LogFactory.getLog(JmsStore.class);

    /** JMS Broker username */
    public static final String USERNAME = "store.jms.username";
    /** JMS Broker password */
    public static final String PASSWORD = "store.jms.password";
    /** Whether to cache the connection or not */
    public static final String CACHE = "store.jms.cache.connection";
    /** JMS destination (ie. Queue) name that this message store must store the messages to. */
    public static final String DESTINATION = "store.jms.destination";
    /** JMS Specification version */
    public static final String JMS_VERSION = "store.jms.JMSSpecVersion";
    /** */
    public static final String CONSUMER_TIMEOUT = "store.jms.ConsumerReceiveTimeOut";
    /** */
    public static final String CONN_FACTORY = "store.jms.connection.factory";
    /** */
    public static final String NAMING_FACTORY_INITIAL = "java.naming.factory.initial";
    /** */
    public static final String CONNECTION_STRING = "connectionfactory.QueueConnectionFactory";
    /** */
    public static final String PROVIDER_URL = "java.naming.provider.url";
    /** JNDI Queue Prefix */
    public static final String QUEUE_PREFIX = "queue.";
    /** Guaranteed delivery status*/
    public static final String GUARANTEED_DELIVERY_ENABLE = "store.producer.guaranteed.delivery.enable";

    /** JMS connection properties */
    private final Properties connectionProperties = new Properties();
    /** JMS username */
    private String userName;
    /** JMS password */
    private String password;
    /** JMS queue name */
    private String destination;
    /** type of the JMS destination. we support queue */
    private String destinationType = "queue";
    /** */
    private static final Log logger = LogFactory.getLog(JmsStore.class.getName());
    /** */
    private int cacheLevel = 0;
    /** */
    public static final String JMS_SPEC_11 = "1.1";
    /** */
    public static final String JMS_SPEC_31 = "3.1";
    /** Is JMS Version 1.1? */
    private boolean isVersion11 = true;
    private boolean isVersion31 = false;
    /** Look up context */
    private Context context;
    /** JMS cachedConnection factory */
    private javax.jms.ConnectionFactory javaxConnectionFactory;
    private jakarta.jms.ConnectionFactory jakartaConnectionFactory;
    /** JMS destination */
    private javax.jms.Destination javaxQueue;
    /** JMS destination */
    private jakarta.jms.Destination jakartaQueue;
    /** */
    private final Object queueLock = new Object();
    /** JMS Connection used to send messages to the queue */
    private javax.jms.Connection producerJavaxConnection;
    /** JMS Connection used to send messages to the queue */
    private jakarta.jms.Connection producerJakartaConnection;
    /** lock protecting the producer connection */
    private final Object producerLock = new Object();
    /** records the last retried time between the broker and ESB */
    private long retryTime = -1;
    /** Guaranteed delivery enable or disable flag */
    private boolean isGuaranteedDeliveryEnable = false;
    /** Preserve session for caching */
    private MessageProducer cachedProducer;

    private MessageProducer producer = null;

    private SynapseEnvironment synapseEnvironment;

    /** regex for secure vault expression */
    private static final String SECURE_VAULT_REGEX = "\\{(wso2:vault-lookup\\('(.*?)'\\))\\}";

    private Pattern queueLookupPattern = Pattern.compile(SECURE_VAULT_REGEX);

    public MessageProducer getProducer() {
        if (cacheLevel == 1 && cachedProducer != null) {
            return cachedProducer;
        }

        if (this.producer == null) {
            this.producer = new JmsProducer(this, isVersion31);
            this.producer.setId(nextProducerId());
        } else {
            return producer;
        }
        if (isVersion31) {
            getJakartaProducer();
        } else {
            getJavaxProducer();
        }

        return this.producer;
    }

    private void getJavaxProducer() {
        javax.jms.Session session = null;
        javax.jms.MessageProducer messageProducer;
        try {
            synchronized (producerLock) {
                if (producerJavaxConnection == null) {
                    newWriteJavaxConnection();
                }
            }
            if (((JmsProducer) this.producer).getJavaxSession() == null) {
                try {
                    session = newJavaxSession(producerJavaxConnection(), Session.AUTO_ACKNOWLEDGE, true);
                } catch (javax.jms.JMSException e) {
                    newWriteJavaxConnection();
                    session = newJavaxSession(producerJavaxConnection(), Session.AUTO_ACKNOWLEDGE, true);
                }
                messageProducer = newJavaxProducer(session);
                ((JmsProducer) this.producer).setJavaxConnection(producerJavaxConnection()).setJavaxSession(session).
                        setJavaxProducer(messageProducer);
                if (logger.isDebugEnabled()) {
                    logger.debug(nameString() + " created message producer " + this.producer.getId());
                }
                if (cacheLevel == 1) {
                    cachedProducer = producer;
                }

            }
        } catch (Throwable t) {
            String errorMsg = "Could not create a Message Producer for " + nameString();
            logger.error(errorMsg, t);
            synchronized (producerLock) {
                try {
                    cleanupJavax(producerJavaxConnection, session);
                } catch (javax.jms.JMSException e) {
                    throw new SynapseException("Error while cleaning up connection for message store "
                            + nameString(), e);
                }
                producerJavaxConnection = null;
            }
        }
    }

    private void getJakartaProducer() {
        jakarta.jms.Session session = null;
        jakarta.jms.MessageProducer messageProducer;
        try {
            synchronized (producerLock) {
                if (producerJakartaConnection == null) {
                    newWriteJakartaConnection();
                }
            }
            if (((JmsProducer) this.producer).getJakartaSession() == null) {
                try {
                    session = newJakartaSession(producerJakartaConnection(), Session.AUTO_ACKNOWLEDGE, true);
                } catch (jakarta.jms.JMSException e) {
                    newWriteJakartaConnection();
                    session = newJakartaSession(producerJakartaConnection(), Session.AUTO_ACKNOWLEDGE, true);
                }
                messageProducer = newJakartaProducer(session);
                ((JmsProducer) this.producer).setJakartaConnection(producerJakartaConnection()).setJakartaSession(session).
                        setJakartaProducer(messageProducer);
                if (logger.isDebugEnabled()) {
                    logger.debug(nameString() + " created message producer " + this.producer.getId());
                }
                if (cacheLevel == 1) {
                    cachedProducer = producer;
                }

            }
        } catch (Throwable t) {
            String errorMsg = "Could not create a Message Producer for " + nameString();
            logger.error(errorMsg, t);
            synchronized (producerLock) {
                try {
                    cleanupJakarta(producerJakartaConnection, session);
                } catch (jakarta.jms.JMSException e) {
                    throw new SynapseException("Error while cleaning up connection for message store "
                            + nameString(), e);
                }
                producerJakartaConnection = null;
            }
        }
    }

    public MessageConsumer getConsumer() throws SynapseException {
        JmsConsumer consumer = new JmsConsumer(this, isVersion31);
        consumer.setId(nextConsumerId());
        if (isVersion31) {
            consumer = getJakartaConsumer(consumer);
        } else {
            consumer = getJavaxConsumer(consumer);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " created message consumer " + consumer.getId());
        }
        return consumer;
    }

    private JmsConsumer getJavaxConsumer(JmsConsumer consumer) {
        try {
            Connection connection = newJavaxConnection();
            Session session = newJavaxSession(connection, Session.CLIENT_ACKNOWLEDGE, false);
            javax.jms.MessageConsumer jmsConsumer = newJavaxConsumer(session);
            consumer.setJavaxConnection(connection)
                    .setJavaxSession(session)
                    .setJavaxConsumer(jmsConsumer);
        } catch (JMSException | StoreForwardException e) {
            throw new SynapseException("Could not create a Message Consumer for " + nameString(), e);
        }
        return consumer;
    }

    private JmsConsumer getJakartaConsumer(JmsConsumer consumer) {
        try {
            jakarta.jms.Connection connection = newJakartaConnection();
            jakarta.jms.Session session = newJakartaSession(connection, Session.CLIENT_ACKNOWLEDGE, false);
            jakarta.jms.MessageConsumer jmsConsumer = newJakartaConsumer(session);
            consumer.setJakartaConnection(connection)
                    .setJakartaSession(session)
                    .setJakartaConsumer(jmsConsumer);
        } catch (jakarta.jms.JMSException | StoreForwardException e) {
            throw new SynapseException("Could not create a Message Consumer for " + nameString(), e);
        }
        return consumer;
    }

    public int getType() {
        return Constants.JMS_MS;
    }

    /** JMS Message store does not support following operations. */
    public MessageContext remove() throws NoSuchElementException {
        return null;
    }

    public void clear() {
    }

    public MessageContext remove(String messageID) {
        return null;
    }

    public MessageContext get(int index) {
        return null;
    }

    public List<MessageContext> getAll() {
        return null;
    }

    public MessageContext get(String messageId) {
        return null;
    } /** End of unsupported operations. */

    public void init(SynapseEnvironment se) {
        synapseEnvironment = se;
        if (se == null) {
            logger.error("Cannot initialize store.");
            return;
        }
        try {
            initme();
        } catch (StoreForwardException | javax.jms.JMSException | jakarta.jms.JMSException e) {
            logger.error(nameString() + ". Initialization failed...", e);
        }
        super.init(se);
        logger.info(nameString() + ". Initialized... ");
    }

    public void destroy() throws SynapseException {
        if (logger.isDebugEnabled()) {
            logger.debug("Destroying " + nameString() + "...");
        }
        if (isVersion31) {
            try {
                closeJakartaWriteConnection();
            } catch (jakarta.jms.JMSException e) {
                throw new SynapseException("Error while closing JMS connection at " + nameString(), e);
            }
        } else {
            try {
                closeJavaxWriteConnection();
            } catch (javax.jms.JMSException e) {
                throw new SynapseException("Error while closing JMS connection at " + nameString(), e);
            }
        }
        super.destroy();
    }

    /**
     * Creates a new JMS Connection.
     *
     * @return A connection to the JMS Queue used as the store of this message store.
     * @throws JMSException on a JMS issue
     * @throws StoreForwardException on a non JMS issue
     */
    public Connection newJavaxConnection() throws JMSException, StoreForwardException {
        Connection connection;
        if (javaxConnectionFactory == null) {
            throw new StoreForwardException("Cannot create a connection to JMS provider as connectionFactory == null");
        }
        if (isVersion11) {
            if (userName != null && password != null) {
                connection = javaxConnectionFactory.createConnection(userName, password);
            } else {
                connection = javaxConnectionFactory.createConnection();
            }
        } else {
            QueueConnectionFactory connectionFactory;
            connectionFactory = (QueueConnectionFactory) this.javaxConnectionFactory;
            if (userName != null && password != null) {
                connection = connectionFactory.createQueueConnection(userName, password);
            } else {
                connection = connectionFactory.createQueueConnection();
            }
        }
        connection.start();
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + ". Created JMS Connection.");
        }
        return connection;
    }

    /**
     * Creates a new Jakarta JMS Connection.
     *
     * @return A connection to the JMS Queue used as the store of this message store.
     * @throws JMSException on a JMS issue
     * @throws StoreForwardException on a non JMS issue
     */
    public jakarta.jms.Connection newJakartaConnection() throws jakarta.jms.JMSException, StoreForwardException {
        jakarta.jms.Connection connection;
        if (jakartaConnectionFactory == null) {
            throw new StoreForwardException("Cannot create a connection to JMS provider as connectionFactory == null");
        }
        if (userName != null && password != null) {
            connection = jakartaConnectionFactory.createConnection(userName, password);
        } else {
            connection = jakartaConnectionFactory.createConnection();
        }

        connection.start();
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + ". Created JMS Connection.");
        }
        return connection;
    }

    /**
     * Creates a new JMS Session.
     *
     * @param connection The JMS Connection that must be used when creating the session.
     * @param mode Acknowledgement mode that must be used for this session.
     * @param isProducerSession Type of the session going to create
     * @return A JMS Session.
     * @throws JMSException on a JMS related issue
     * @throws StoreForwardException on a non JMS related issue
     */
    public Session newJavaxSession(Connection connection, int mode, boolean isProducerSession)
            throws JMSException, StoreForwardException {

        Session session;

        if(connection == null) {
            throw new StoreForwardException("Cannot Create JMS session on null connection ");
        }

        if (isVersion11) {
            if (isGuaranteedDeliveryEnable && isProducerSession) {
                session = connection.createSession(true, Session.SESSION_TRANSACTED);
            } else {
                session = connection.createSession(false, mode);
            }
        } else {
            if (isGuaranteedDeliveryEnable && isProducerSession) {
                session = ((QueueConnection) connection).createQueueSession(true, Session.SESSION_TRANSACTED);
            } else {
                session = ((QueueConnection) connection).createQueueSession(false, mode);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + ". Created JMS Session.");
        }
        return session;
    }

    public jakarta.jms.Session newJakartaSession(jakarta.jms.Connection connection, int mode, boolean isProducerSession)
            throws jakarta.jms.JMSException, StoreForwardException {

        jakarta.jms.Session session;

        if(connection == null) {
            throw new StoreForwardException("Cannot Create JMS session on null connection ");
        }

        if (isGuaranteedDeliveryEnable && isProducerSession) {
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } else {
            session = connection.createSession(false, mode);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + ". Created JMS Session.");
        }
        return session;
    }

    /**
     * Creates a new JMS Message Producer.
     *
     * @param session  A JMS Session.
     * @return A JMS Message Producer.
     * @throws JMSException
     */
    public javax.jms.MessageProducer newJavaxProducer(javax.jms.Session session) throws JMSException, StoreForwardException {
        if (session == null) {
            throw new StoreForwardException("Cannot create a JMS consumer on null session");
        }

        //create destination before creating the consumer if it is not yet created
        createJavaxDestIfAbsent(session);

        javax.jms.MessageProducer producer;

        if (isVersion11) {
            producer = session.createProducer(javaxQueue);
        } else {
            producer = ((QueueSession) session).createSender((javax.jms.Queue) javaxQueue);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " created JMS Message Producer to destination ["
                         + javaxQueue.toString() + "].");
        }
        return producer;
    }

    /**
     * Creates a new JMS Message Producer.
     *
     * @param session  A JMS Session.
     * @return A JMS Message Producer.
     * @throws JMSException
     */
    public jakarta.jms.MessageProducer newJakartaProducer(jakarta.jms.Session session) throws jakarta.jms.JMSException, StoreForwardException {
        if (session == null) {
            throw new StoreForwardException("Cannot create a JMS consumer on null session");
        }
        //create destination before creating the consumer if it is not yet created
        createJakartaDestIfAbsent(session);
        jakarta.jms.MessageProducer producer;
        producer = session.createProducer(jakartaQueue);

        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " created JMS Message Producer to destination ["
                    + javaxQueue.toString() + "].");
        }
        return producer;
    }

    /**
     * Returns a new JMS Message Consumer.
     * @param session JMS Session to create consumer form
     * @return JMS Message Consumer
     * @throws JMSException on JMS issue when creating consumer with JMS provider
     */
    public javax.jms.MessageConsumer newJavaxConsumer(Session session) throws JMSException, StoreForwardException {
        if (session == null) {
            throw new StoreForwardException("Cannot create a JMS consumer on null session");
        }

        //create destination before creating the consumer if it is not yet created
        createJavaxDestIfAbsent(session);

        javax.jms.MessageConsumer consumer;
        if(isVersion11) {
            consumer = session.createConsumer(javaxQueue);
        } else {
            consumer = ((QueueSession) session).createReceiver((Queue) javaxQueue);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " created JMS Message Consumer to destination ["
                         + javaxQueue.toString() + "].");
        }
        return consumer;
    }

    /**
     * Returns a new JMS Message Consumer.
     * @param session JMS Session to create consumer form
     * @return JMS Message Consumer
     * @throws JMSException on JMS issue when creating consumer with JMS provider
     */
    public jakarta.jms.MessageConsumer newJakartaConsumer(jakarta.jms.Session session) throws jakarta.jms.JMSException,
            StoreForwardException {
        if (session == null) {
            throw new StoreForwardException("Cannot create a JMS consumer on null session");
        }

        //create destination before creating the consumer if it is not yet created
        createJakartaDestIfAbsent(session);

        jakarta.jms.MessageConsumer consumer = session.createConsumer(jakartaQueue);
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " created JMS Message Consumer to destination ["
                    + javaxQueue.toString() + "].");
        }
        return consumer;
    }

    /**
     * Creates a new JMS Message producer connection.
     *
     * @return true if new producer connection was successfully created, <br/>
     * false otherwise.
     * @throws StoreForwardException on a non JMS related issue
     * @throws JMSException          on a JMS issue
     */
    public void newWriteJavaxConnection() throws StoreForwardException, JMSException {
        synchronized (producerLock) {
            if (producerJavaxConnection != null) {
                closeJavaxConnection(producerJavaxConnection);
            }
            producerJavaxConnection = newJavaxConnection();
        }
    }

    public void newWriteJakartaConnection() throws StoreForwardException, jakarta.jms.JMSException {
        synchronized (producerLock) {
            if (producerJakartaConnection != null) {
                closeJakartaConnection(producerJakartaConnection);
            }
            producerJakartaConnection = newJakartaConnection();
        }
    }

    /**
     * Closes the existing JMS message producer connection.
     *
     * @return true if the producer connection was closed without any error, <br/>
     * false otherwise.
     * @throws JMSException on a JMS level issue
     */
    public void closeJavaxWriteConnection() throws JMSException {
        synchronized (producerLock) {
            if (producerJavaxConnection != null) {
                closeJavaxConnection(producerJavaxConnection);
            }
        }
    }

    /**
     * Closes the existing JMS message producer connection.
     *
     * @return true if the producer connection was closed without any error, <br/>
     * false otherwise.
     * @throws JMSException on a JMS level issue
     */
    public void closeJakartaWriteConnection() throws jakarta.jms.JMSException {
        synchronized (producerLock) {
            if (producerJakartaConnection != null) {
                closeJakartaConnection(producerJakartaConnection);
            }
        }
    }

    /**
     * Returns the existing JMS message producer connection.
     *
     * @return The current JMS Connection used to create message producers.
     */
    public javax.jms.Connection producerJavaxConnection() {
        return producerJavaxConnection;
    }

    /**
     * Returns the existing JMS message producer connection.
     *
     * @return The current JMS Connection used to create message producers.
     */
    public jakarta.jms.Connection producerJakartaConnection() {
        return producerJakartaConnection;
    }

    /**
     * Closes the given JMS Connection.
     *
     * @param connection The JMS Connection to be closed.
     * @return true if the connection was successfully closed. false otherwise.
     * @throws JMSException on a JMS level issue
     */
    public void closeJavaxConnection(Connection connection) throws JMSException {
        connection.close();
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " closed connection to JMS broker.");
        }
    }

    /**
     * Closes the given Jakarta JMS Connection.
     *
     * @param connection The JMS Connection to be closed.
     * @return true if the connection was successfully closed. false otherwise.
     * @throws JMSException on a JMS level issue
     */
    public void closeJakartaConnection(jakarta.jms.Connection connection) throws jakarta.jms.JMSException {
        connection.close();
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " closed connection to JMS broker.");
        }
    }

    /**
     * Resets the JMS session for next message
     *
     * @param connection  JMS Connection
     * @param session     JMS Session associated with the given connection
     */
    public void reset(Connection connection, Session session) throws JMSException {
        cleanupJavax(connection, session);
    }

    /**
     * Cleans up the JMS Connection and Session associated with a JMS client.
     *
     * @param connection  JMS Connection
     * @param session JMS Session associated with the given connection
     */
    public void cleanupJavax(Connection connection, Session session) throws JMSException {
        cachedProducer = null;
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Cleans up the JMS Connection and Session associated with a JMS client.
     *
     * @param connection  JMS Connection
     * @param session JMS Session associated with the given connection
     */
    public void cleanupJakarta(jakarta.jms.Connection connection, jakarta.jms.Session session) throws jakarta.jms.JMSException {
        cachedProducer = null;
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    public org.apache.axis2.context.MessageContext newAxis2Mc() {
        return ((Axis2SynapseEnvironment) synapseEnvironment)
                .getAxis2ConfigurationContext().createMessageContext();
    }

    public org.apache.synapse.MessageContext newSynapseMc(
            org.apache.axis2.context.MessageContext msgCtx) {
        SynapseConfiguration configuration = synapseEnvironment.getSynapseConfiguration();
        return new Axis2MessageContext(msgCtx, configuration, synapseEnvironment);
    }

    public void setParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            throw new SynapseException("Cannot initialize JMS Store [" + getName() +
                                       "]. Required parameters are not available.");
        }
        super.setParameters(parameters);
    }

    public void setCachedProducer(MessageProducer cachedProducer) {
        this.cachedProducer = cachedProducer;
    }

    private boolean initme() throws StoreForwardException, javax.jms.JMSException, jakarta.jms.JMSException {
        Set<Map.Entry<String, Object>> mapSet = parameters.entrySet();
        for (Map.Entry<String, Object> e : mapSet) {
            Object value = e.getValue();
            if (value instanceof String) {
                if (CONNECTION_STRING.equals(e.getKey())) {
                    value = resolveConnectionStringValues(value);
                }
                connectionProperties.put(e.getKey(), value);
            }
        }
        userName = (String) parameters.get(USERNAME);
        password = SecureVaultResolver.resolve(synapseEnvironment, (String) parameters.get(PASSWORD));

        String conCaching = (String) parameters.get(CACHE);
        if ("true".equals(conCaching)) {
            if (logger.isDebugEnabled()) {
                logger.debug(nameString() + " enabling connection Caching");
            }
            cacheLevel = 1;
        }
        String destination = (String) parameters.get(DESTINATION);
        if (destination != null) {
            this.destination = destination;
        } else {
            String name = getName();
            String defaultDest;
            if (name != null && !name.isEmpty()) {
                defaultDest = name + "_Queue";
            } else {
                defaultDest =  "JmsStore_" + System.currentTimeMillis() + "_Queue";
            }
            logger.warn(nameString() + ". Destination not provided. " +
                        "Setting default destination to [" + defaultDest + "].");
            this.destination = defaultDest;
        }
        destinationType = "queue";
        String version = (String) parameters.get(JMS_VERSION);
        if (version != null) {
            if (JMS_SPEC_31.equals(version)) {
                isVersion31 = true;
                isVersion11 = false;
            } else if (!JMS_SPEC_11.equals(version)) {
                isVersion11 = false;
            }
        }

        if (parameters != null && !parameters.isEmpty() && parameters.get(GUARANTEED_DELIVERY_ENABLE) != null) {
            isGuaranteedDeliveryEnable = Boolean.valueOf(parameters.get(GUARANTEED_DELIVERY_ENABLE).toString());
        }

        String consumerReceiveTimeOut = (String) parameters.get(CONSUMER_TIMEOUT);
        int consumerReceiveTimeOutI = 6000;
        if (consumerReceiveTimeOut != null) {
            try {
                consumerReceiveTimeOutI = Integer.parseInt(consumerReceiveTimeOut);
            } catch (NumberFormatException e) {
                //logger.error(nameString() + ". Error parsing consumer receive time out value. " +
                //             "Set to 60s.");
            }
        } //else {
            //logger.warn(nameString() + ". Consumer Receiving time out not passed in. " +
            //            "Set to 60s.");
        //}
        if (isVersion31) {
            processJakartaConnection();
        } else {
            processJavaxConnection();
        }
        return true;
    }

    private void processJavaxConnection() throws StoreForwardException, javax.jms.JMSException {
        String connectionFac = null;
        try {
            context = new InitialContext(connectionProperties);
            connectionFac = (String) parameters.get(CONN_FACTORY);
            if (connectionFac == null) {
                connectionFac = "QueueConnectionFactory";
            }
            javaxConnectionFactory = lookup(context, javax.jms.ConnectionFactory.class, connectionFac);
            if (javaxConnectionFactory == null) {
                throw new StoreForwardException(nameString() + " could not initialize JMS Connection Factory. "
                        + "Connection factory not found : " + connectionFac);
            }
            createJavaxDestIfAbsent(null);
            if (javaxQueue == null) {
                logger.warn(nameString() + ". JMS Destination [" + destination + "] does not exist.");
            }
        } catch (NamingException e) {
            logger.error(nameString() + ". Could not initialize JMS Message Store. Error:"
                    + e.getLocalizedMessage() + ". Initial Context Factory:[" + parameters.get(NAMING_FACTORY_INITIAL)
                    + "]; Provider URL:[" + parameters.get(PROVIDER_URL) + "]; Connection Factory:[" + connectionFac
                    + "].", e);
        } catch (Throwable t) {
            logger.error(nameString() + ". Could not initialize JMS Message Store. Error:"
                    + t.getMessage() + ". Initial Context Factory:[" + parameters.get(NAMING_FACTORY_INITIAL)
                    + "]; Provider URL:[" + parameters.get(PROVIDER_URL) + "]; Connection Factory:[" + connectionFac
                    + "].",t);
        }
        newWriteJavaxConnection();
    }

    private void processJakartaConnection() throws StoreForwardException, jakarta.jms.JMSException {
        String connectionFac = null;
        try {
            context = new InitialContext(connectionProperties);
            connectionFac = (String) parameters.get(CONN_FACTORY);
            if (connectionFac == null) {
                connectionFac = "QueueConnectionFactory";
            }
            jakartaConnectionFactory = lookup(context, jakarta.jms.ConnectionFactory.class, connectionFac);
            if (jakartaConnectionFactory == null) {
                throw new StoreForwardException(nameString() + " could not initialize JMS Connection Factory. "
                        + "Connection factory not found : " + connectionFac);
            }
            createJakartaDestIfAbsent(null);
            if (jakartaQueue == null) {
                logger.warn(nameString() + ". JMS Destination [" + destination + "] does not exist.");
            }
        } catch (NamingException e) {
            logger.error(nameString() + ". Could not initialize JMS Message Store. Error:"
                    + e.getLocalizedMessage() + ". Initial Context Factory:[" + parameters.get(NAMING_FACTORY_INITIAL)
                    + "]; Provider URL:[" + parameters.get(PROVIDER_URL) + "]; Connection Factory:[" + connectionFac
                    + "].", e);
        } catch (Throwable t) {
            logger.error(nameString() + ". Could not initialize JMS Message Store. Error:"
                    + t.getMessage() + ". Initial Context Factory:[" + parameters.get(NAMING_FACTORY_INITIAL)
                    + "]; Provider URL:[" + parameters.get(PROVIDER_URL) + "]; Connection Factory:[" + connectionFac
                    + "].",t);
        }
        newWriteJakartaConnection();
    }

    /**
     * Get Destination denoted by destination variable by looking up context or
     * creating it using the session.
     *
     * @param session Session to create destination from
     * @return Destination object
     * @throws JMSException on a JMS exception when creating Destination using session
     * @throws StoreForwardException on other issue
     */
    private Destination getJavaxDestination(Session session) throws JMSException, StoreForwardException {
        Destination dest = javaxQueue;
        if (dest != null) {
            return dest;
        }
        //try creating a destination by looking up context
        InitialContext newContext;
        String destinationLookupFailureReason = "";
        try {
            dest = lookup(context, javax.jms.Destination.class, destination);
        } catch (NamingException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(nameString() + ". Could not lookup destination [" + destination
                        + "]. Message: " + e.getLocalizedMessage());
            }
            //try to re-init the context
            newContext = newContext();
            try {
                dest = lookup(newContext, Destination.class, destination);
            } catch (Throwable t) {
                destinationLookupFailureReason = nameString() + ". Destination [" + destination
                        + "] not defined in JNDI context. Message:" + t.getLocalizedMessage();
            }
        }
        //try creating destination by session as lookup failed (dest == null)
        if (dest == null) {

            if (session == null) {
                throw new StoreForwardException(nameString() + "cannot create Destination" + destination
                        +". JMS Session " + "cannot be null");
            }

            try {
                dest = session.createQueue(destination);
                if (logger.isDebugEnabled()) {
                    logger.debug(nameString() + " created destination ["
                            + destination + "] from session object.");
                }
            } catch (JMSException e) {
                String error = nameString() + " could not create destination ["
                        + destination + "]. from session or by JNDI context lookup. ";
                error.concat("create by session error: " + e);
                if (!destinationLookupFailureReason.isEmpty()) {
                    error.concat(" create by lookup error: " + destinationLookupFailureReason);
                }
                throw new StoreForwardException(error, e);
            }
        }
        synchronized (queueLock) {
            javaxQueue = dest;
        }
        return dest;
    }

    private jakarta.jms.Destination getJakartaDestination(jakarta.jms.Session session) throws StoreForwardException {
        jakarta.jms.Destination dest = jakartaQueue;
        if (dest != null) {
            return dest;
        }
        //try creating a destination by looking up context
        InitialContext newContext;
        String destinationLookupFailureReason = "";
        try {
            dest = lookup(context, jakarta.jms.Destination.class, destination);
        } catch (NamingException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(nameString() + ". Could not lookup destination [" + destination
                        + "]. Message: " + e.getLocalizedMessage());
            }
            //try to re-init the context
            newContext = newContext();
            try {
                dest = lookup(newContext, jakarta.jms.Destination.class, destination);
            } catch (Throwable t) {
                destinationLookupFailureReason = nameString() + ". Destination [" + destination
                        + "] not defined in JNDI context. Message:" + t.getLocalizedMessage();
            }
        }
        //try creating destination by session as lookup failed (dest == null)
        if (dest == null) {

            if (session == null) {
                throw new StoreForwardException(nameString() + "cannot create Destination" + destination
                        +". JMS Session " + "cannot be null");
            }

            try {
                dest = session.createQueue(destination);
                if (logger.isDebugEnabled()) {
                    logger.debug(nameString() + " created destination ["
                            + destination + "] from session object.");
                }
            } catch (jakarta.jms.JMSException e) {
                String error = nameString() + " could not create destination ["
                        + destination + "]. from session or by JNDI context lookup. ";
                error.concat("create by session error: " + e);
                if (!destinationLookupFailureReason.isEmpty()) {
                    error.concat(" create by lookup error: " + destinationLookupFailureReason);
                }
                throw new StoreForwardException(error, e);
            }
        }
        synchronized (queueLock) {
            jakartaQueue = dest;
        }
        return dest;
    }

    private InitialContext newContext() {
        Properties properties = new Properties();
        InitialContext newContext;
        Map env;
        try {
            env = context.getEnvironment();
            Object o = env.get(NAMING_FACTORY_INITIAL);
            if (o != null) {
                properties.put(NAMING_FACTORY_INITIAL, o);
            }
            o = env.get(CONNECTION_STRING);
            if (o != null) {
                properties.put(CONNECTION_STRING, o);
            }
            o = env.get(PROVIDER_URL);
            if (o != null) {
                properties.put(PROVIDER_URL, o);
            }
            properties.put(QUEUE_PREFIX + destination, destination);
            newContext = new InitialContext(properties);
        } catch (NamingException e) {
            logger.info(nameString() + " could not create a new Context. Message:"
                        + e.getLocalizedMessage());
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " Created a new Context.");
        }
        return newContext;
    }

    /**
     * Lookup the Object in the given context and cast it to the class specified
     *
     * @param context Context to lookup
     * @param clazz   Class to cast
     * @param name    Name of object to lookup
     * @param <T>     any object
     * @return Any object
     * @throws NamingException in case of object is absent in context
     */
    private <T> T lookup(Context context, Class<T> clazz, String name)
            throws NamingException {
        if (context == null) {
            logger.error(nameString() + ". Cannot perform JNDI lookup. Invalid context.");
            return null;
        }
        if (name == null || "".equals(name)) {
            logger.error(nameString() + ". Cannot perform JNDI lookup. Invalid name.");
            return null;
        }
        Object object = context.lookup(name);
        try {
            return clazz.cast(object);
        } catch (ClassCastException e) {
            logger.error(nameString() + ". Cannot perform JNDI lookup for the name ["
                    + name + "].", e);
            return null;
        }
    }

    private boolean destinationNonNull() {
        synchronized (queueLock) {
            return javaxQueue != null;
        }
    }

    private boolean createJavaxDestIfAbsent(Session session) throws JMSException, StoreForwardException {
        synchronized (queueLock) {
            return getJavaxDestination(session) != null;
        }
    }

    private boolean createJakartaDestIfAbsent(jakarta.jms.Session session) throws StoreForwardException {
        synchronized (queueLock) {
            return getJakartaDestination(session) != null;
        }
    }


    private String nameString() {
        return "Store [" + getName() + "]";
    }

    public void setProducer(MessageProducer producer) {
        this.producer = producer;
    }

    private String resolveConnectionStringValues(Object param) {
        String paramString = param.toString();
        Matcher lookupMatcher = queueLookupPattern.matcher(paramString);

        while (lookupMatcher.find()) {
            for (int i = 0; i < lookupMatcher.groupCount(); i++) {
                paramString = paramString.replace(lookupMatcher.group(i), SecureVaultResolver.
                        resolve(synapseEnvironment, lookupMatcher.group(i)));
            }
        }
        return paramString;
    }
}
