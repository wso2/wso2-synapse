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

import java.util.Date;
import java.util.Properties;

public class JMSPollingConsumer implements MessageConsumer,PollingConsumer {

    private static final Log logger = LogFactory.getLog(JMSPollingConsumer.class.getName());

    private CachedJMSConnectionFactory jmsConnectionFactory;
    private InjectHandler injectHandler;
    private long scanInterval;
    private Long lastRanTime;
    private String strUserName;
    private String strPassword;
    
    public JMSPollingConsumer(CachedJMSConnectionFactory jmsConnectionFactory, Properties jmsProperties, long scanInterval) {
        this.jmsConnectionFactory = jmsConnectionFactory;
        strUserName = jmsProperties.getProperty(JMSConstants.PARAM_JMS_USERNAME);
        strPassword = jmsProperties.getProperty(JMSConstants.PARAM_JMS_PASSWORD);        
        this.scanInterval = scanInterval;
        this.lastRanTime = null;
    }    
    
	public void registerHandler(InjectHandler injectHandler){
		this.injectHandler = injectHandler;
	}    
    public void execute() {        
        try {
            if (logger.isDebugEnabled()) {
            	logger.debug("Start : JMS Inbound EP : ");
            }
            //Check if the cycles are running in correct interval and start scan
            long currentTime = (new Date()).getTime();
            if(lastRanTime == null || ((lastRanTime + (scanInterval)) <= currentTime)){
            	lastRanTime = currentTime;
            	poll();
            }else if (logger.isDebugEnabled()) {
            	logger.debug("Skip cycle since cuncurrent rate is higher than the scan interval : JMS Inbound EP ");
            }
            if (logger.isDebugEnabled()) {
            	logger.debug("End : JMS Inbound EP : ");
            }        	
        } catch (Exception e) {
            System.err.println("error in executing: It will no longer be run!");
            logger.error("Error while reading file. " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }     
    public Message poll() {
        if(logger.isDebugEnabled()) {
            logger.debug("run() - polling messages");
        }
        Connection connection = null;
        Session session = null;
        Destination destination = null;
        MessageConsumer messageConsumer = null;
        try {
            connection = jmsConnectionFactory.getConnection(strUserName, strPassword);
            if(connection == null){
            	return null;
            }        	
            session = jmsConnectionFactory.getSession(connection);
            destination = jmsConnectionFactory.getDestination(connection);
            messageConsumer = jmsConnectionFactory.getMessageConsumer(session, destination);        	
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
	            	boolean commitOrAck = true;
	            	commitOrAck = injectHandler.invoke(msg);
	                // if client acknowledgement is selected, and processing requested ACK
	                if (commitOrAck && jmsConnectionFactory.getSessionAckMode() == Session.CLIENT_ACKNOWLEDGE) {
	                    try {
	                    	msg.acknowledge();
	                        if (logger.isDebugEnabled()) {
	                        	logger.debug("Message : " + msg.getJMSMessageID() + " acknowledged");
	                        }
	                    } catch (JMSException e) {
	                    	logger.error("Error acknowledging message : " + msg.getJMSMessageID(), e);
	                    }
	                }	 
	                // if session was transacted, commit it or rollback
	                if(jmsConnectionFactory.isTransactedSession()){
		                try {
		                    if (session.getTransacted()) {
		                        if (commitOrAck) {
		                            session.commit();
		                            if (logger.isDebugEnabled()) {
		                            	logger.debug("Session for message : " + msg.getJMSMessageID() + " committed");
		                            }
		                        } else {
		                            session.rollback();
		                            if (logger.isDebugEnabled()) {
		                            	logger.debug("Session for message : " + msg.getJMSMessageID() + " rolled back");
		                            }
		                        }
		                    }
		                } catch (JMSException e) {
		                	logger.error("Error " + (commitOrAck ? "committing" : "rolling back") +
		                        " local session txn for message : " + msg.getJMSMessageID(), e);
		                }
	                }	             
	            }else{
	            	return msg;            	
	            }
	            msg = messageConsumer.receive(1);
            }

        } catch (JMSException e) {
            logger.error("Error while receiving JMS message. " + e.getMessage(), e);
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Error while receiving JMS message. " + e.getMessage(), e);
        }finally{
        	if(messageConsumer != null){
        		jmsConnectionFactory.closeConsumer(messageConsumer);
        	}
        	if(session != null){
        		jmsConnectionFactory.closeSession(session);
        	}
        	if(connection != null){
        		jmsConnectionFactory.closeConnection(connection);
        	}        	
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
