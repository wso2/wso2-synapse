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

import com.rabbitmq.client.AMQP;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.message.MessageProducer;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.*;

public class RabbitmqProducer  implements MessageProducer {

	private static final Log logger = LogFactory.getLog(RabbitmqProducer.class.getName());
	/** Connection to the Rabbitmq broker. Passed reference from Store **/
	private Connection connection;
	/** Reference of the store **/
	private RabbitmqStore store;
	/** Message storing queue name **/
	private String queueName;

	private boolean isInitialized = false;

	private boolean isConnectionError = false;
	/** ID of the MessageProducer **/
	private String idString;

	public RabbitmqProducer(RabbitmqStore store) {
		if (store == null) {
			logger.error("Cannot initialize.");
			return;
		}
		this.store = store;
		isInitialized = true;
	}

	public void setQueueName(String queueName){
		this.queueName = queueName;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public void setId(int id) {
		idString = "[" + store.getName() + "-P-" + id + "]";
	}

	public boolean storeMessage(MessageContext synCtx) {
		if (synCtx == null) {
			return false;
		}
		if (connection == null) {
			if (logger.isDebugEnabled()) {
				logger.error(getId() + " cannot proceed. RabbitMQ Connection is null.");
			}
			logger.warn(getId() + ". Ignored MessageID : " + synCtx.getMessageID());
			return false;
		}
		AMQPStorableMessage message = MessageConverter.toStorableMessage(synCtx);
		boolean error = false;
		Throwable throwable = null;
		Channel channel=null;
		try {
			//Serializing message
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(os);
			objOut.writeObject(message);
			byte[] byteForm = os.toByteArray();
			objOut.close();
			os.close();
			//building AMQP message
			AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
			builder.messageId(synCtx.getMessageID());
			channel = connection.createChannel();
			channel.basicPublish("",queueName,builder.build(),byteForm);
		} catch (IOException e) {
			throwable = e;
			error = true;
			isConnectionError = true;
		} catch (Throwable t) {
			throwable = t;
			error = true;
		}
		finally {
			if(channel != null && channel.isOpen())
				try {
					channel.close();
				} catch (IOException e) {
					logger.error("Error when closing connection"+synCtx.getMessageID()+". " + e);
				}
		}
		if (error) {
			String errorMsg = getId() + ". Ignored MessageID : " + synCtx.getMessageID()
			                  + ". Could not store message to store ["
			                  + store.getName() + "]. Error:" + throwable.getLocalizedMessage();
			logger.error(errorMsg, throwable);
			store.closeWriteConnection();
			connection = null;
			if (logger.isDebugEnabled()) {
				logger.debug(getId() + ". Ignored MessageID : " + synCtx.getMessageID());
			}
			return false;
		} else {
			cleanup();
		}
		if (logger.isDebugEnabled()) {
			logger.debug(getId() + ". Stored MessageID : " + synCtx.getMessageID());
		}
		store.enqueued();
		return true;
	}
	//Not useful yet, this should be use to close the session ( channel )
	public boolean cleanup() {
		return store.cleanup(null, false);
	}

	public boolean isInitialized() {
		return isInitialized;
	}

	public String getId() {
		return idString;
	}

}
