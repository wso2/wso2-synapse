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

import org.apache.synapse.message.store.impl.commons.MessageConverter;
import org.apache.synapse.message.store.impl.commons.StorableMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

public class RabbitMQConsumer implements MessageConsumer {
	private static final Log logger = LogFactory.getLog(RabbitMQConsumer.class.getName());

	private Connection connection;

	private Channel channel;

	private QueueingConsumer consumer;

	private RabbitMQStore store;

	private String queueName;

	private String idString;

	private boolean isInitialized;
	/**
	 * Did last receive() call cause an error?
	 */
	private boolean isReceiveError;
	/**
	 * Holds the last message read from the message store.
	 */
	private CachedMessage cachedMessage;

	public RabbitMQConsumer(RabbitMQStore store) {
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
				consumer = new QueueingConsumer(channel);
			}
		} else {
			if (!setChannel()) {
				logger.info(getId() + " unable to create the channel.");
				return null;
			}
			consumer = new QueueingConsumer(channel);
		}
		//receive messages
		try {
			channel.basicConsume(queueName, false, consumer);

			QueueingConsumer.Delivery delivery = null;
			try { //TODO: find if non-blocking consume is possible
				delivery = consumer.nextDelivery(1);
			} catch (InterruptedException e) {
				logger.error("Error while consuming message", e);
			}

			if (delivery != null) {
				//deserilizing message
				StorableMessage storableMessage = null;
				ByteArrayInputStream bis = new ByteArrayInputStream(delivery.getBody());
				ObjectInput in = new ObjectInputStream(bis);
				try {
					storableMessage = (StorableMessage) in.readObject();
				} catch (ClassNotFoundException e) {
					logger.error(getId() + "unable to read the stored message" + e);
					channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				}

				bis.close();
				in.close();
				org.apache.axis2.context.MessageContext axis2Mc = store.newAxis2Mc();
				MessageContext synapseMc = store.newSynapseMc(axis2Mc);
				synapseMc =
						MessageConverter.toMessageContext(storableMessage, axis2Mc, synapseMc);
				updateCache(delivery, synapseMc, null, false);
				if (logger.isDebugEnabled()) {
					logger.debug(getId() + " Received MessageId:" +
					             delivery.getProperties().getMessageId());
				}
				return synapseMc;
			}
		} catch (ShutdownSignalException sse) {
			logger.error(getId() + " connection error when receiving messages" + sse);
		} catch (IOException ioe) {
			logger.error(getId() + " connection error when receiving messages" + ioe);
		}
		return null;
	}

	public boolean ack() {
		boolean result = cachedMessage.ack();
		if (result) {
			store.dequeued();
		}
		return result;
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

	public RabbitMQConsumer setConnection(Connection connection) {
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
		RabbitMQConsumer consumer = (RabbitMQConsumer) store.getConsumer();

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

	private void updateCache(QueueingConsumer.Delivery message, MessageContext synCtx,
	                         String messageId,
	                         boolean receiveError) throws IOException {
		isReceiveError = receiveError;
		cachedMessage.setMessage(message);
		cachedMessage.setMc(synCtx);
		cachedMessage.setId(messageId);
	}

	/**
	 * This is used to store the last received message
	 * if the message is successfully sent to the endpoint, ack will sent to the store
	 * in order to delete message from the queue
	 *
	 * In RabbitMQ message ack should be using the same channel which was consumed the message
	 * There for the consumed channel will also stored without closing until the message is ackd
	 */
	private final class CachedMessage {
		private QueueingConsumer.Delivery message = null;
		private MessageContext mc = null;
		private String id = "";

		public CachedMessage setMessage(QueueingConsumer.Delivery message) {
			this.message = message;
			return this;
		}

		public boolean ack() {
			if (message != null && channel != null && channel.isOpen()) {
				try {
					channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
					return true;
				} catch (IOException e) {
					logger.error(getId() + " cannot ack last read message. Error:"
					             + e.getLocalizedMessage(), e);
					return false;
				}
			}
			return false;
		}

		public QueueingConsumer.Delivery getMessage() {
			return message;
		}

		public CachedMessage setMc(MessageContext mc) {
			this.mc = mc;
			return this;
		}

		public CachedMessage setId(String id) {
			this.id = id;
			return this;
		}

		public String getId() {
			return id;
		}
	}
}

