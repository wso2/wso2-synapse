/**
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.message.store.impl.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang.StringUtils;
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
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.Constants;
import org.apache.synapse.util.resolver.SecureVaultResolver;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RabbitMQStore extends AbstractMessageStore {
	/**
	 * RabbitMQ Broker username
	 */
	public static final String USERNAME = "store.rabbitmq.username";
	/**
	 * RabbitMQ Broker password
	 */
	public static final String PASSWORD = "store.rabbitmq.password";
	/**
	 * RabbitMQ Server Host name
	 */
	public static final String HOST_NAME = "store.rabbitmq.host.name";
	/**
	 * RabbitMQ Server Port
	 */
	public static final String HOST_PORT = "store.rabbitmq.host.port";
	/**
	 * RabbitMQ Server default port
	 */
	public static final int DEFAULT_PORT = 5672;
	/**
	 * RabbitMQ Server Virtual Host
	 */
	public static final String VIRTUAL_HOST = "store.rabbitmq.virtual.host";
	/**
	 * RabbitMQ queue name that this message store must store the messages to.
	 */
	public static final String QUEUE_NAME = "store.rabbitmq.queue.name";
	/**
	 * RabbitMQ route key which queue is binded
	 */
	public static final String ROUTE_KEY = "store.rabbitmq.route.key";
	/**
	 * RabbitMQ exchange.
	 */
	public static final String EXCHANGE_NAME = "store.rabbitmq.exchange.name";

    //SSL related properties
    public static final String SSL_ENABLED = "rabbitmq.connection.ssl.enabled";
    public static final String SSL_KEYSTORE_LOCATION = "rabbitmq.connection.ssl.keystore.location";
    public static final String SSL_KEYSTORE_TYPE = "rabbitmq.connection.ssl.keystore.type";
    public static final String SSL_KEYSTORE_PASSWORD = "rabbitmq.connection.ssl.keystore.password";
    public static final String SSL_TRUSTSTORE_LOCATION = "rabbitmq.connection.ssl.truststore.location";
    public static final String SSL_TRUSTSTORE_TYPE = "rabbitmq.connection.ssl.truststore.type";
    public static final String SSL_TRUSTSTORE_PASSWORD = "rabbitmq.connection.ssl.truststore.password";
    public static final String SSL_VERSION = "rabbitmq.connection.ssl.version";
	/**
	 * RabbitMQ connection properties
	 */
	private final Properties properties = new Properties();
	/**
	 * RabbitMQ username
	 */
	private String userName;
	/**
	 * RabbitMQ password
	 */
	private String password;
	/**
	 * RabbitMQ queue name
	 */
	private String queueName;
	/**
	 * RabbitMQ routing key
	 */
	private String routeKey;
	/**
	 * RabbitMQ exchange name
	 */
	private String exchangeName;
	/**
	 * RabbitMQ host name
	 */
	private String hostName;
	/**
	 * RabbitMQ host port
	 */
	private String hostPort;
	/**
	 * RabbitMQ virtual host
	 */
	private String virtualHost;
	private static final Log logger = LogFactory.getLog(RabbitMQStore.class.getName());
	/**
	 * Rabbitmq Connection factory
	 */
	private ConnectionFactory connectionFactory;
	/**
	 * RabbitMQ Connection used to send messages to the queue
	 */
	private Connection producerConnection;
	/**
	 * lock protecting the producer connection
	 */
	private final Object producerLock = new Object();
	/**
	 * records the last retried time between the broker and ESB
	 */
	private long retryTime = -1;

	public void init(SynapseEnvironment se) {
		if (se == null) {
			logger.error("Cannot initialize store.");
			return;
		}
		super.init(se);
		boolean initOk = initme();
		if (initOk) {
			logger.info(nameString() + ". Initialized... ");
		} else {
			logger.info(nameString() + ". Initialization failed...");
			return;
		}
	}

	private boolean initme() {
		Set<Map.Entry<String, Object>> mapSet = parameters.entrySet();
		for (Map.Entry<String, Object> e : mapSet) {
			if (e.getValue() instanceof String) {
				properties.put(e.getKey(), e.getValue());
			}
		}
		userName = (String) parameters.get(USERNAME);
		password = SecureVaultResolver.resolve(synapseEnvironment, (String) parameters.get(PASSWORD));
		hostName = (String) parameters.get(HOST_NAME);
		hostPort = (String) parameters.get(HOST_PORT);
		virtualHost = (String) parameters.get(VIRTUAL_HOST);

		//Possible timeouts that can be added in future if requested, should be added to the
		//setConnectionTimeout, ShutdownTimeout, RequestedHeartbeat
		connectionFactory = new ConnectionFactory();
		if (hostName != null && !hostName.equals("")) {
			connectionFactory.setHost(hostName);
		} else {
			throw new SynapseException(nameString() + " host name is not correctly defined");
		}
		int port = 0;
		try {
			port = Integer.parseInt(hostPort);
		} catch (NumberFormatException nfe) {
			logger.error("Port value for " + nameString() + " is not correctly defined" + nfe);
		}

		if (port > 0) {
			connectionFactory.setPort(port);
		} else {
			connectionFactory.setPort(DEFAULT_PORT);
			logger.info(nameString() + " port is set to default value (5672");
		}

		if (userName != null && !userName.equals("")) {
			connectionFactory.setUsername(userName);
		}

		if (password != null && !password.equals("")) {
			connectionFactory.setPassword(password);
		}

		if (virtualHost != null && !virtualHost.equals("")) {
			connectionFactory.setVirtualHost(virtualHost);
		}

        String sslEnabledS = parameters.get(SSL_ENABLED) != null ? parameters.get(SSL_ENABLED).toString() : "";
        if (!StringUtils.isEmpty(sslEnabledS)) {
            try {
                boolean sslEnabled = Boolean.parseBoolean(sslEnabledS);
                if (sslEnabled) {
                    String keyStoreLocation = parameters.get(SSL_KEYSTORE_LOCATION) != null ? parameters.get(SSL_KEYSTORE_LOCATION).toString() : "";
                    String keyStoreType = parameters.get(SSL_KEYSTORE_TYPE) != null ? parameters.get(SSL_KEYSTORE_TYPE).toString() : "";
                    String keyStorePassword = parameters.get(SSL_KEYSTORE_PASSWORD) != null ? parameters.get(SSL_KEYSTORE_PASSWORD).toString() : "";
                    String trustStoreLocation = parameters.get(SSL_TRUSTSTORE_LOCATION) != null ? parameters.get(SSL_TRUSTSTORE_LOCATION).toString() : "";
                    String trustStoreType = parameters.get(SSL_TRUSTSTORE_TYPE) != null ? parameters.get(SSL_TRUSTSTORE_TYPE).toString() : "";
                    String trustStorePassword = parameters.get(SSL_TRUSTSTORE_PASSWORD) != null ? parameters.get(SSL_TRUSTSTORE_PASSWORD).toString() : "";
                    String sslVersion = parameters.get(SSL_VERSION) != null ? parameters.get(SSL_VERSION).toString() : "";

                    if (StringUtils.isEmpty(keyStoreLocation) || StringUtils.isEmpty(keyStoreType) || StringUtils.isEmpty(keyStorePassword) ||
                        StringUtils.isEmpty(trustStoreLocation) || StringUtils.isEmpty(trustStoreType) || StringUtils.isEmpty(trustStorePassword)) {
                        logger.warn("Trustore and keystore information is not provided correctly. Proceeding with default SSL configuration");
                        connectionFactory.useSslProtocol();
                    } else {
                        char[] keyPassphrase = keyStorePassword.toCharArray();
                        KeyStore ks = KeyStore.getInstance(keyStoreType);
                        ks.load(new FileInputStream(keyStoreLocation), keyPassphrase);

                        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        kmf.init(ks, keyPassphrase);

                        char[] trustPassphrase = trustStorePassword.toCharArray();
                        KeyStore tks = KeyStore.getInstance(trustStoreType);
                        tks.load(new FileInputStream(trustStoreLocation), trustPassphrase);

                        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        tmf.init(tks);

                        SSLContext c = SSLContext.getInstance(sslVersion);
                        c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                        connectionFactory.useSslProtocol(c);
                    }
                }
            } catch (Exception e) {
                logger.warn("Format error in SSL enabled value. Proceeding without enabling SSL", e);
            }
        }
        
		//declaring queue
		String queueName = (String) parameters.get(QUEUE_NAME);
		if (queueName != null) {
			this.queueName = queueName;
		} else {
			String name = getName();
			String defaultQueue;
			if (name != null && !name.isEmpty()) {
				defaultQueue = name + "_Queue";
			} else {
				defaultQueue = "RabiitmqStore_" + System.currentTimeMillis() + "_Queue";
			}
			logger.warn(nameString() + ". Destination not provided. " +
			            "Setting default destination to [" + defaultQueue + "].");
			this.queueName = defaultQueue;
		}
		exchangeName = (String) properties.get(EXCHANGE_NAME);
		routeKey = (String) properties.get(ROUTE_KEY);
		if (routeKey == null) {
			logger.warn(nameString() + ". Routing key is not provided. " +
			            "Setting queue name " + this.queueName + " as routing key.");
			routeKey = this.queueName;
		}

		if (!newProducerConnection()) {
			logger.warn(nameString() + ". Starting with a faulty connection to the broker.");
			return false;
		}
		try {
			setQueue();
		} catch (IOException e) {
			logger.error(nameString() + " error in storage declaring queue " + queueName);
			return false;
		}
		return true;
	}

	/**
	 * Create a Queue to store messages, this will used queueName parameter
	 *
	 * @throws IOException : if its enable to create the queue or exchange
	 */
	private void setQueue() throws IOException {
		Channel channel = null;
		try {
			channel = producerConnection.createChannel();
			try {
				channel.queueDeclarePassive(queueName);
			} catch (java.io.IOException e) {
				logger.info("Queue :" + queueName + " not found.Declaring queue.");
				if (!channel.isOpen()) {
					channel = producerConnection.createChannel();
				}
				//Hashmap with names and parameters can be used in the place of null argument
				//Eg: adding dead letter exchange to the queue
				//params: (queueName, durable, exclusive, autoDelete, paramMap)
				channel.queueDeclare(queueName, true, false, false, null);
			}
			//declaring exchange
			if (exchangeName != null) {
				try {
					channel.exchangeDeclarePassive(exchangeName);
				} catch (java.io.IOException e) {
					logger.info("Exchange :" + exchangeName + " not found. Declaring exchange.");
					if (!channel.isOpen()) {
						channel = producerConnection.createChannel();
					}
					//params : ( exchangeName, exchangeType, durable, autoDelete, paramMap )
					channel.exchangeDeclare(exchangeName, "direct", true, false, null);
				}
				channel.queueBind(queueName, exchangeName, routeKey);
			}
		} finally {
			try {
				channel.close();
			} catch (TimeoutException e) {
				logger.error(nameString() + " TimeoutException while closing connection.", e);
			}
		}
	}

	/**
	 * Create a new message producer connection, if there is an existing connection close it and
	 * create a new connection
	 *
	 * @return true if a new connection is created successfully
	 */
	public boolean newProducerConnection() {
		synchronized (producerLock) {
			if (producerConnection != null) {
				//if the exiting producer connection not closed successfully, this will return false
				if (!closeConnection(producerConnection)) {
					return false;
				}
			}
			try {
				producerConnection = connectionFactory.newConnection();
			} catch (TimeoutException e) {
				logger.error(nameString() + " cannot create connection to the broker,TimeoutException.", e);
				producerConnection = null;
			} catch (IOException e) {
				logger.error(nameString() + " cannot create connection to the broker." + e);
				producerConnection = null;
			}
		}
		return producerConnection != null;
	}

	public void destroy() {
		// do whatever...
		if (logger.isDebugEnabled()) {
			logger.debug("Destroying " + nameString() + "...");
		}
		closeProducerConnection();
		super.destroy();
	}

	/**
	 * Close any given connection
	 *
	 * @param connection
	 * @return true if the connection closed successfully false otherwise
	 */
	public boolean closeConnection(Connection connection) {
		try {
			if (connection.isOpen()) {
				connection.close();
			}
			if (logger.isDebugEnabled()) {
				logger.debug(nameString() + " closed connection to RabbitMQ broker.");
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * Closes the existing RabbitMQ message producer connection.
	 *
	 * @return true if the producer connection was closed without any error, <br/>
	 * false otherwise.
	 */
	public boolean closeProducerConnection() {
		synchronized (producerLock) {
			if (producerConnection != null) {
				if (!closeConnection(producerConnection)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @return new MessageProducer
	 */
	public MessageProducer getProducer() {
		RabbitMQProducer producer = new RabbitMQProducer(this);
		producer.setId(nextProducerId());
		if (exchangeName != null) {
			producer.setQueueName(routeKey);
			producer.setExchangeName(exchangeName);
		} else {
			producer.setQueueName(queueName);
			producer.setExchangeName(null);
			if (logger.isDebugEnabled()) {
				logger.debug(
						nameString() + " exchange is not defined, using default exchange and " +
						"queue name for routing messages");
			}
		}

		Throwable throwable = null;
		boolean error = false;
		try {
			synchronized (producerLock) {
				if (producerConnection == null) {
					boolean ok = newProducerConnection();
					//if its not possible to create a producer connection, return messagePeoducer
					//without a connection
					if (!ok) {
						return producer;
					}
				}
				if (!producerConnection.isOpen()) {
					producerConnection = connectionFactory.newConnection();
				}
			}
			producer.setConnection(producerConnection());
		} catch (Throwable t) {
			error = true;
			throwable = t;
		}
		if (error) {
			String errorMsg = "Could not create a Message Producer for "
			                  + nameString() + ". Error:" + throwable.getLocalizedMessage();
			logger.error(errorMsg, throwable);
			synchronized (producerLock) {
				cleanup(producerConnection, true);
				producerConnection = null;
			}
			return producer;
		}
		if (logger.isDebugEnabled()) {
			logger.debug(nameString() + " created message producer " + producer.getId());
		}
		return producer;
	}

	public MessageConsumer getConsumer() {
		RabbitMQConsumer consumer = new RabbitMQConsumer(this);
		consumer.setId(nextConsumerId());
		consumer.setQueueName(queueName);
		Connection connection = null;
		try {
			// To avoid piling up log files with the error message and throttle retries.
			if ((System.currentTimeMillis() - retryTime) >= 3000) {
				connection = connectionFactory.newConnection();
				retryTime = -1;
			}
		} catch (TimeoutException e) {
			retryTime = System.currentTimeMillis();
			if (logger.isDebugEnabled()) {
				logger.error("Could not create a Message Consumer for " + nameString()
				        + ". Could not create connection,TimeoutException.");
			}
			return consumer;
		} catch (IOException e) {
			retryTime = System.currentTimeMillis();
			if (logger.isDebugEnabled()) {
				logger.error("Could not create a Message Consumer for "
				             + nameString() + ". Could not create connection.");
			}
			return consumer;
		}
		if (connection == null) {
			return consumer;
		}
		consumer.setConnection(connection);
		if (logger.isDebugEnabled()) {
			logger.debug(nameString() + " created message consumer " + consumer.getId());
		}
		return consumer;
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

	public Connection producerConnection() {
		return producerConnection;
	}

	public MessageContext remove() throws NoSuchElementException {
		return null;
	}

	public void clear() {
	}

	public int getType() {
		return Constants.RABBIT_MS;
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
	}

	private String nameString() {
		return "Store [" + getName() + "]";
	}

	/**
	 * Cleans up the RabbitMQ Connection
	 *
	 * @param connection RabbitMQ Connection
	 * @param error      is this method called upon an error
	 * @return {@code true} if the cleanup is successful. {@code false} otherwise.
	 */
	public boolean cleanup(Connection connection, boolean error) {
		if (connection == null && error) {
			return true;
		}
		try {
			if (connection != null && error && connection.isOpen()) {
				connection.close();
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}
}
