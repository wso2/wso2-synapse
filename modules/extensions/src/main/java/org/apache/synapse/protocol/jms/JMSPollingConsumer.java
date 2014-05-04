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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.builder.SOAPBuilder;
import org.apache.axis2.format.DataSourceMessageBuilder;
import org.apache.axis2.format.ManagedDataSource;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.inbound.InboundEndpointUtils;
import org.apache.synapse.inbound.PollingConsumer;
import org.apache.synapse.inbound.InjectHandler;
import org.apache.synapse.protocol.jms.factory.CachedJMSConnectionFactory;

import javax.jms.*;
import java.io.InputStream;
import java.util.Properties;

public class JMSPollingConsumer implements Runnable, MessageConsumer,PollingConsumer {

    private static final Log logger = LogFactory.getLog(JMSPollingConsumer.class.getName());

    private CachedJMSConnectionFactory jmsConnectionFactory;
    private Connection connection;
    private Session session;
    private Destination destination;
    private MessageConsumer messageConsumer;
    private InjectHandler injectHandler;
    private Properties jmsProperties;

    public JMSPollingConsumer(CachedJMSConnectionFactory jmsConnectionFactory, Properties jmsProperties) {
        this.jmsConnectionFactory = jmsConnectionFactory;
        String strUserName = jmsProperties.getProperty(JMSConstants.PARAM_JMS_USERNAME);
        String strPassword = jmsProperties.getProperty(JMSConstants.PARAM_JMS_PASSWORD);
        this.connection = jmsConnectionFactory.getConnection(strUserName, strPassword);
        this.jmsConnectionFactory.start(connection);
        this.session = jmsConnectionFactory.getSession(connection);
        this.destination = jmsConnectionFactory.getDestination(connection);
        this.messageConsumer = createMessageConsumer();
        this.jmsProperties = jmsProperties;
    }
    
    public void run() {
    	poll();
    }

    public void execute() {
    	poll();
    }    
    
	public void registerHandler(InjectHandler injectHandler){
		this.injectHandler = injectHandler;
	}    
    
    public Message poll() {
        if(logger.isDebugEnabled()) {
            logger.debug("run() - polling messages");
        }

        try {
            Message msg = messageConsumer.receive(1);
            if(null == msg) {
                if(logger.isDebugEnabled()) {
                    logger.debug("No message");
                }
                return null;
            }

            if(!JMSUtils.inferJMSMessageType(msg).equals(TextMessage.class.getName())) {
                logger.error("JMS Inbound transport support JMS TextMessage type only.");
                return null;
            }           

            if(injectHandler != null){
            	injectHandler.invoke(msg);
            }else{
            	return msg;            	
            }
        } catch (JMSException e) {
            logger.error("Error while receiving JMS message. " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error while receiving JMS message. " + e.getMessage());
        }
        return null;
    }

    public MessageConsumer getMessageConsumer(String messageSelector) {
        if(messageConsumer != null) {
            return messageConsumer;
        }

        return createMessageConsumer(messageSelector);
    }

    public MessageConsumer getMessageConsumer() {
        if(messageConsumer != null) {
            return messageConsumer;
        }

        return createMessageConsumer();
    }

    private MessageConsumer createMessageConsumer() {
        try {
            messageConsumer = this.session.createConsumer(destination);
            return messageConsumer;
        } catch (JMSException e) {
            logger.error("JMS Exception while creating consumer. " + e.getMessage());
        }

        return null;
    }

    private MessageConsumer createMessageConsumer(String messageSelector) {
        try {
            messageConsumer = this.session.createConsumer(destination, messageSelector);
            return messageConsumer;
        } catch (JMSException e) {
            logger.error("JMS Exception while creating consumer. " + e.getMessage());
        }

        return null;
    }


    public String getMessageSelector() throws JMSException {
        return messageConsumer.getMessageSelector();
    }

    public MessageListener getMessageListener() throws JMSException {
        return messageConsumer.getMessageListener();
    }

    public void setMessageListener(MessageListener messageListener) throws JMSException {
        messageConsumer.setMessageListener(messageListener);
    }

    public Message receive() throws JMSException {
        return messageConsumer.receive();
    }

    public Message receive(long l) throws JMSException {
        return messageConsumer.receive(l);
    }

    public Message receiveNoWait() throws JMSException {
        return messageConsumer.receiveNoWait();
    }

    public void close() throws JMSException {
        messageConsumer.close();
    }
}
