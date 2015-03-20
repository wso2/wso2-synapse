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
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.message.MessageConsumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

public class RabbitmqConsumer implements MessageConsumer {
	private static final Log logger = LogFactory.getLog(RabbitmqConsumer.class.getName());

	private Connection connection;

	private Channel channel;

	private RabbitmqStore store;

	private String queueName;

	private String idString;

	private boolean isInitialized;
	/**
	 * Holds the last message read from the message store.
	 */
	private CachedMessage cachedMessage;
	/**
	 * Did last receive() call cause an error?
	 */
	private boolean isReceiveError;

	public RabbitmqConsumer(RabbitmqStore store) {
		if (store == null) {
			logger.error("Cannot initialize.");
			return;
		}
		this.store = store;
		cachedMessage = new CachedMessage();
		isReceiveError = false;
		isInitialized = true;
	}

	public MessageContext receive() {
		if (!checkConnection()) {
			if (!reconnect()) {
				if (logger.isDebugEnabled()) {
					logger.debug(getId()
					             + " cannot receive message from store. Can not reconnect.");
				}
				return null;
			} else {
				logger.info(getId() + " reconnected to store.");
				isReceiveError = false;
			}
		}
		//setting channel
		if (channel != null) {
			if (!channel.isOpen()) {
				if (!setChannel()) {
					logger.info(getId() + " unable to create the channel.");
					return null;
				}
			}
		} else {
			if (!setChannel()) {
				logger.info(getId() + " unable to create the channel.");
				return null;
			}
		}
		//receive messages
		try {
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueName, false, consumer);
			try {
				channel.txSelect();
			} catch (IOException e) {
				logger.error("Error while starting transaction", e);
			}

			boolean successful = false;
			QueueingConsumer.Delivery delivery = null;
			try {
				delivery = consumer.nextDelivery(1);
			} catch (InterruptedException e) {
				logger.error("Error while consuming message", e);
			}

			if (delivery != null) {
				try {
					//deserilizing message
					AMQPStorableMessage storableMessage = null;
					ByteArrayInputStream bis = new ByteArrayInputStream(delivery.getBody());
					ObjectInput in = new ObjectInputStream(bis);
					try {
						storableMessage = (AMQPStorableMessage) in.readObject();
					} catch (ClassNotFoundException e) {
						logger.error(getId() + "unable to read the stored message" + e);
					}
					bis.close();
					in.close();
					org.apache.axis2.context.MessageContext axis2Mc = store.newAxis2Mc();
					MessageContext synapseMc = store.newSynapseMc(axis2Mc);
					synapseMc =
							MessageConverter.toMessageContext(storableMessage, axis2Mc, synapseMc);
					successful = true;
					if (logger.isDebugEnabled()) {
						logger.debug(getId() + " Received MessageId:" +
						             delivery.getProperties().getMessageId());
					}
					return synapseMc;
				} finally {
					if (successful) {
						try {
							channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
							channel.txCommit();
						} catch (IOException e) {
							logger.error("Error while committing transaction", e);
						}
					} else {
						try {
							channel.txRollback();
						} catch (IOException e) {
							logger.error("Error while trying to roll back transaction", e);
						}
					}
				}
			}
			if (channel.isOpen())
				channel.close();
		} catch (ShutdownSignalException sse) {
			logger.error(getId() + " connection error when receiving messages" + sse);
		} catch (IOException ioe) {
			logger.error(getId() + " connection error when receiving messages" + ioe);
		}
		return null;
	}

	public boolean ack() {
		return false;
	}

	public boolean cleanup() {
		if (logger.isDebugEnabled()) {
			logger.debug(getId() + " cleaning up...");
		}
		boolean result = store.cleanup(connection, true);
		if (result) {
			connection = null;
			return true;
		}
		return false;
	}

	public RabbitmqConsumer setConnection(Connection connection) {
		this.connection = connection;
		return this;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public boolean setChannel() {
		if (connection != null && connection.isOpen()) {
			try {
				this.channel = connection.createChannel();
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}

	public void setId(int id) {
		idString = "[" + store.getName() + "-C-" + id + "]";
	}

	public String getId() {
		return idString;
	}

	public Connection getConnection() {
		return connection;
	}

	private boolean checkConnection() {
		if (connection == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(getId() + " cannot proceed. RabbitMQ Connection is null.");
			}
			return false;
		}
		if (!connection.isOpen()) {
			if (logger.isDebugEnabled()) {
				logger.debug(getId() + " cannot proceed. RabbitMQ Connection is closed.");
			}
			return false;
		}
		return true;
	}

	private boolean reconnect() {
		RabbitmqConsumer consumer = (RabbitmqConsumer) store.getConsumer();

		if (consumer.getConnection() == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(getId() + " could not reconnect to the broker.");
			}
			return false;
		}
		setConnection(consumer.getConnection());
		if (logger.isDebugEnabled()) {
			logger.debug(getId() + " ===> " + consumer.getId());
		}
		idString = consumer.getId();
		return true;
	}

	//Implement cahcedMessage class similar to JMS (requirement is not clear in JMS)
	private final class CachedMessage {
	}
}


