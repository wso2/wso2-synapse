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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.inbound.PollingConsumer;
import org.apache.synapse.inbound.InjectHandler;
import org.apache.synapse.protocol.jms.factory.CachedJMSConnectionFactory;

import javax.jms.*;

import java.util.Properties;

public class JMSPollingConsumer implements Runnable, MessageConsumer,PollingConsumer {

    private static final Log logger = LogFactory.getLog(JMSPollingConsumer.class.getName());

    private CachedJMSConnectionFactory jmsConnectionFactory;
    private InjectHandler injectHandler;
    private Properties jmsProperties;
    private String strUserName;
    private String strPassword;
    
    public JMSPollingConsumer(CachedJMSConnectionFactory jmsConnectionFactory, Properties jmsProperties) {
        this.jmsConnectionFactory = jmsConnectionFactory;
        strUserName = jmsProperties.getProperty(JMSConstants.PARAM_JMS_USERNAME);
        strPassword = jmsProperties.getProperty(JMSConstants.PARAM_JMS_PASSWORD);        
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
        Connection connection = jmsConnectionFactory.getConnection(strUserName, strPassword);
        if(connection == null){
        	return null;
        }
        Session session = jmsConnectionFactory.getSession(connection);
        Destination destination = jmsConnectionFactory.getDestination(connection);
        MessageConsumer messageConsumer = jmsConnectionFactory.getMessageConsumer(session, destination);
        try {
            Message msg = messageConsumer.receive(1);
            if(null == msg) {
                if(logger.isDebugEnabled()) {
                    logger.debug("No message");
                }
                return null;
            }
            while(msg != null){
	            if(!JMSUtils.inferJMSMessageType(msg).equals(TextMessage.class.getName())) {
	                logger.error("JMS Inbound transport support JMS TextMessage type only.");
	                return null;
	            }           
	
	            if(injectHandler != null){
	            	injectHandler.invoke(msg);
	            }else{
	            	return msg;            	
	            }
	            msg = messageConsumer.receive(1);
            }
        } catch (JMSException e) {
            logger.error("Error while receiving JMS message. " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error while receiving JMS message. " + e.getMessage());      
        }
        return null;
    }



	public void close() throws JMSException {
		// TODO Auto-generated method stub
		
	}

	public MessageListener getMessageListener() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getMessageSelector() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	public Message receive() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	public Message receive(long arg0) throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	public Message receiveNoWait() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setMessageListener(MessageListener arg0) throws JMSException {
		// TODO Auto-generated method stub
		
	}

}
