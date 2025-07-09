/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.message.store.impl.rabbitmq;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.MessageProducer;
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.Constants;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The message store and message processor implementation for the RabbitMQ
 */
public class RabbitMQStore extends AbstractMessageStore {

    private static final Log log = LogFactory.getLog(RabbitMQStore.class.getName());
    // default broker properties
    public static final String USERNAME = "store.rabbitmq.username";
    public static final String PASSWORD = "store.rabbitmq.password";
    public static final String HOST_NAME = "store.rabbitmq.host.name";
    public static final String HOST_PORT = "store.rabbitmq.host.port";
    public static final String VIRTUAL_HOST = "store.rabbitmq.virtual.host";
    public static final String QUEUE_NAME = "store.rabbitmq.queue.name";
    public static final String ROUTING_KEY = "store.rabbitmq.route.key";
    public static final String EXCHANGE_NAME = "store.rabbitmq.exchange.name";
    public static final String RETRY_INTERVAL = "rabbitmq.connection.retry.interval";
    public static final String RETRY_COUNT = "rabbitmq.connection.retry.count";
    public static final String PUBLISHER_CONFIRMS = "store.producer.guaranteed.delivery.enable";
    public static final int DEFAULT_RETRY_INTERVAL = 30000;
    public static final int DEFAULT_RETRY_COUNT = 3;

    // ssl related properties
    public static final String SSL_ENABLED = "rabbitmq.connection.ssl.enabled";
    public static final String SSL_KEYSTORE_LOCATION = "rabbitmq.connection.ssl.keystore.location";
    public static final String SSL_KEYSTORE_TYPE = "rabbitmq.connection.ssl.keystore.type";
    public static final String SSL_KEYSTORE_PASSWORD = "rabbitmq.connection.ssl.keystore.password";
    public static final String SSL_TRUSTSTORE_LOCATION = "rabbitmq.connection.ssl.truststore.location";
    public static final String SSL_TRUSTSTORE_TYPE = "rabbitmq.connection.ssl.truststore.type";
    public static final String SSL_TRUSTSTORE_PASSWORD = "rabbitmq.connection.ssl.truststore.password";
    public static final String SSL_VERSION = "rabbitmq.connection.ssl.version";

    public static final String AMQ_PREFIX = "amq.";

    /** regex for any vault expression */
    private static final String secureVaultRegex = "\\{(.*?):vault-lookup\\('(.*?)'\\)\\}";

    private String queueName;
    private String routingKey;
    private String exchangeName;
    private ConnectionFactory connectionFactory;
    private Connection producerConnection;
    private Address[] addresses;
    private int retryInterval;
    private int retryCount;
    private boolean publisherConfirmsEnabled;
    private Channel channel;

    @Override
    public void init(SynapseEnvironment se) {
        if (se == null) {
            log.error("Cannot initialize store [" + getName() + "]...");
        }
        super.init(se);
        initConnectionFactory();
        publisherConfirmsEnabled = BooleanUtils.toBooleanDefaultIfNull(
                BooleanUtils.toBoolean((String) parameters.get(PUBLISHER_CONFIRMS)), false);
        producerConnection = createConnection();
        if (producerConnection != null) {
            try (Channel channel = producerConnection.createChannel()) {
                queueName = (String) parameters.get(QUEUE_NAME);
                routingKey = (String) parameters.get(ROUTING_KEY);
                exchangeName = (String) parameters.get(EXCHANGE_NAME);
                if (StringUtils.isEmpty(queueName)) {
                    queueName = getName();
                }
                if (StringUtils.isEmpty(routingKey)) {
                    routingKey = queueName;
                }
                declareQueue(channel, queueName);
                declareExchange(channel, exchangeName, queueName, routingKey);
                log.info(nameString() + ". Initialized... ");
            } catch (TimeoutException | IOException e) {
                log.warn(nameString() + " - Producer channel initialization failed. Will retry connecting  during "
                        + "message publishing. Cause: " + e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug(nameString() + " - Initialization failed...:", e);
                }
            }
        } else {
            log.warn(nameString() + " - Producer connection not available at startup. " +
                    "Will retry connecting during message publishing.");

        }
        channel = createChannel(producerConnection);
    }

    /**
     * Initiate rabbitmq connection factory from the connection parameters
     */
    private void initConnectionFactory() {
        String hostnames = StringUtils.defaultIfEmpty(
                (String) parameters.get(HOST_NAME), ConnectionFactory.DEFAULT_HOST);
        String ports = StringUtils.defaultIfEmpty(
                (String) parameters.get(HOST_PORT), String.valueOf(ConnectionFactory.DEFAULT_AMQP_PORT));
        String username = StringUtils.defaultIfEmpty(resolveVaultExpressions((String) parameters.get(USERNAME)),
                ConnectionFactory.DEFAULT_USER);
        String password = StringUtils.defaultIfEmpty(resolveVaultExpressions((String) parameters.get(PASSWORD)),
                ConnectionFactory.DEFAULT_PASS);
        String virtualHost = StringUtils.defaultIfEmpty(
                (String) parameters.get(VIRTUAL_HOST), ConnectionFactory.DEFAULT_VHOST);
        boolean sslEnabled = BooleanUtils.toBooleanDefaultIfNull(
                BooleanUtils.toBoolean((String) parameters.get(SSL_ENABLED)), false);
        this.retryInterval = NumberUtils.toInt((String) parameters.get(RETRY_INTERVAL), DEFAULT_RETRY_INTERVAL);
        this.retryCount = NumberUtils.toInt((String) parameters.get(RETRY_COUNT), DEFAULT_RETRY_COUNT);

        String[] hostnameArray = hostnames.split(",");
        String[] portArray = ports.split(",");
        if (hostnameArray.length == portArray.length) {
            addresses = new Address[hostnameArray.length];
            for (int i = 0; i < hostnameArray.length; i++) {
                try {
                    addresses[i] = new Address(hostnameArray[i].trim(), Integer.parseInt(portArray[i].trim()));
                } catch (NumberFormatException e) {
                    throw new SynapseException("Number format error in port number", e);
                }
            }
        } else {
            throw new SynapseException("The number of hostnames must be equal to the number of ports");
        }

        connectionFactory = new ConnectionFactory();
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setTopologyRecoveryEnabled(true);
        setSSL(sslEnabled);
    }

    /**
     * Set secure socket layer configuration if enabled
     *
     * @param sslEnabled ssl enabled
     */
    private void setSSL(boolean sslEnabled) {
        try {
            if (sslEnabled) {
                String keyStoreLocation = (String) parameters.get(SSL_KEYSTORE_LOCATION);
                String keyStoreType = (String) parameters.get(SSL_KEYSTORE_TYPE);
                String keyStorePassword = (String) parameters.get(SSL_KEYSTORE_PASSWORD);
                String trustStoreLocation = (String) parameters.get(SSL_TRUSTSTORE_LOCATION);
                String trustStoreType = (String) parameters.get(SSL_TRUSTSTORE_TYPE);
                String trustStorePassword = (String) parameters.get(SSL_TRUSTSTORE_PASSWORD);
                String sslVersion = (String) parameters.get(SSL_VERSION);

                if (StringUtils.isEmpty(keyStoreLocation) || StringUtils.isEmpty(keyStoreType) ||
                        StringUtils.isEmpty(keyStorePassword) || StringUtils.isEmpty(trustStoreLocation) ||
                        StringUtils.isEmpty(trustStoreType) || StringUtils.isEmpty(trustStorePassword)) {
                    log.info("Trustore and keystore information is not provided");
                    if (StringUtils.isNotEmpty(sslVersion)) {
                        connectionFactory.useSslProtocol(sslVersion);
                    } else {
                        log.info("Proceeding with default SSL configuration");
                        connectionFactory.useSslProtocol();
                    }
                } else {
                    char[] keyPassphrase = keyStorePassword.toCharArray();
                    KeyStore ks = KeyStore.getInstance(keyStoreType);
                    ks.load(new FileInputStream(keyStoreLocation), keyPassphrase);

                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(ks, keyPassphrase);

                    char[] trustPassphrase = trustStorePassword.toCharArray();
                    KeyStore tks = KeyStore.getInstance(trustStoreType);
                    tks.load(new FileInputStream(trustStoreLocation), trustPassphrase);

                    TrustManagerFactory tmf = TrustManagerFactory
                            .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    tmf.init(tks);

                    SSLContext c = SSLContext.getInstance(sslVersion);
                    c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                    connectionFactory.useSslProtocol(c);
                }
            }
        } catch (Exception e) {
            log.warn("Format error in SSL enabled value. Proceeding without enabling SSL", e);
        }
    }

    /**
     * Create a RabbitMQ connection
     *
     * @return a {@link Connection} object
     */
    public Connection createConnection() {
        Connection connection = null;
        try {
            connection = connectionFactory.newConnection(addresses);
            log.info(nameString() + " Successfully connected to RabbitMQ Broker");
        } catch (TimeoutException e) {
            log.error("Error occurred while creating a connection.", e);
        } catch (IOException e) {
            log.error(nameString() + " Error creating connection to RabbitMQ Broker. " +
                    "Reattempting to connect.", e);
            connection = retry(connection);
            if (connection == null) {
                throw new SynapseException(nameString() + " Could not connect to RabbitMQ Broker. " +
                        "Error while creating connection", e);
            }
        }
        return connection;
    }

    /**
     * Create a RabbitMQ channel from given connection
     *
     * @param connection a {@link Connection} object
     * @return a {@link Channel} object
     */
    public Channel createChannel(Connection connection) {
        Channel channel = null;
        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            log.error("Error occurred while creating a channel.", e);
        }
        return channel;
    }

    /**
     * Retry when could not connect to the broker
     *
     * @param connection the failure connection {@link Connection} object
     * @return the {@link Connection} object after retry completion
     */
    private Connection retry(Connection connection) {
        int retryC = 0;
        while ((connection == null) && ((retryCount == -1) || (retryC < retryCount))) {
            retryC++;
            log.info(nameString() + " Attempting to create connection to RabbitMQ Broker" +
                    " in " + retryInterval + " ms");
            try {
                Thread.sleep(retryInterval);
                connection = connectionFactory.newConnection(addresses);
                log.info(nameString() + " Successfully connected to RabbitMQ Broker");
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException e1) {
                log.error("Error occurred while creating a connection", e1);
            } catch (IOException e1) {
                log.error(nameString() + " Error while trying to reconnect to RabbitMQ Broker", e1);
            }
        }
        return connection;
    }

    /**
     * Helper method to declare queue when direct channel is given
     *
     * @param channel   a rabbitmq channel
     * @param queueName a name of the queue to declare
     * @throws IOException
     */
    private void declareQueue(Channel channel, String queueName) throws IOException {
        channel.queueDeclare(queueName, true, false, false, new HashMap<String, Object>());
    }

    /**
     * Helper method to declare exchange when direct channel is given
     *
     * @param channel      {@link Channel} object
     * @param exchangeName the exchange exchangeName
     */
    private void declareExchange(Channel channel, String exchangeName, String queueName, String routingKey)
            throws IOException {
        if (StringUtils.isNotEmpty(exchangeName)) {
            // declare the exchange
            if (!exchangeName.startsWith(AMQ_PREFIX)) {
                channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, true,
                        false, new HashMap<String, Object>());
            }
            // bind the queue and exchange with routing key
            if (StringUtils.isNotEmpty(queueName) && StringUtils.isNotEmpty(routingKey)) {
                channel.queueBind(queueName, exchangeName, routingKey);
            } else if (StringUtils.isNotEmpty(queueName) && StringUtils.isEmpty(routingKey)) {
                if (log.isDebugEnabled()) {
                    log.debug("No routing key specified. The queue name is using as the routing key.");
                }
                channel.queueBind(queueName, exchangeName, routingKey);
            }
        }
    }

    @Override
    public void destroy() {
        if (producerConnection != null) {
            producerConnection.abort();
        }
        producerConnection = null;
        super.destroy();
    }

    /**
     * Create a new {@link RabbitMQProducer} object
     *
     * @return a message producer
     */
    @Override
    public MessageProducer getProducer() {
        RabbitMQProducer producer = new RabbitMQProducer(this);
        boolean producerConnectionInvalidated = false;
        producer.setId(nextProducerId());
        producer.setExchangeName(exchangeName);
        producer.setRoutingKey(routingKey);
        if (producerConnection == null) {
            producerConnectionInvalidated = true;
            producerConnection = createConnection();
        } else if (!producerConnection.isOpen()) {
            producerConnectionInvalidated = true;
            producerConnection.abort();
            producerConnection = createConnection();
        }
        producer.setConnection(producerConnection);
        producer.setPublisherConfirmsEnabled(publisherConfirmsEnabled);
        if (log.isDebugEnabled()) {
            log.debug(nameString() + " created message producer " + producer.getId());
        }
        if (channel == null || producerConnectionInvalidated) {
            channel = createChannel(producerConnection);
        }
        producer.setChannel(channel);
        return producer;
    }

    /**
     * Resolve secure-vault property values
     *
     * @param propertyValue value to be resolved
     * @return a resolved value
     */
    private String resolveVaultExpressions(String propertyValue) {
        Pattern vaultLookupPattern = Pattern.compile(secureVaultRegex);
        Matcher lookupMatcher = vaultLookupPattern.matcher(propertyValue);
        if (lookupMatcher.matches()) {
            Value expression = null;
            //getting the expression with out curly brackets
            String expressionStr = lookupMatcher.group(0).substring(1, lookupMatcher.group(0).length() - 1);
            try {
                expression = new Value(new SynapseXPath(expressionStr));
            } catch (JaxenException e) {
                log.error("Error while building the expression : " + expressionStr);
            }
            if (expression != null) {
                String resolvedValue = expression.evaluateValue(synapseEnvironment.createMessageContext());
                if (resolvedValue == null || resolvedValue.isEmpty()) {
                    log.warn("Found Empty value for expression : " + expression.getExpression());
                } else {
                    return resolvedValue;
                }
            }
        }
        return propertyValue;
    }

    /**
     * Create a new {@link RabbitMQConsumer} object
     *
     * @return a message consumer
     */
    @Override
    public MessageConsumer getConsumer() {
        RabbitMQConsumer consumer = new RabbitMQConsumer(this);
        consumer.setId(nextConsumerId());
        consumer.setQueueName(queueName);
        Connection connection = createConnection();
        consumer.setConnection(connection);
        consumer.setChannel(createChannel(connection));
        if (log.isDebugEnabled()) {
            log.debug(nameString() + " created message consumer " + consumer.getId());
        }
        return consumer;
    }

    public org.apache.axis2.context.MessageContext newAxis2Mc() {
        return ((Axis2SynapseEnvironment) synapseEnvironment).getAxis2ConfigurationContext().createMessageContext();
    }

    public org.apache.synapse.MessageContext newSynapseMc(org.apache.axis2.context.MessageContext msgCtx) {
        SynapseConfiguration configuration = synapseEnvironment.getSynapseConfiguration();
        return new Axis2MessageContext(msgCtx, configuration, synapseEnvironment);
    }

    @Override
    public MessageContext remove() throws NoSuchElementException {
        return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public int getType() {
        return Constants.RABBIT_MS;
    }

    @Override
    public MessageContext remove(String messageID) {
        return null;
    }

    @Override
    public MessageContext get(int index) {
        return null;
    }

    @Override
    public List<MessageContext> getAll() {
        return null;
    }

    @Override
    public MessageContext get(String messageId) {
        return null;
    }

    private String nameString() {
        return "Store [" + getName() + "]";
    }
}
