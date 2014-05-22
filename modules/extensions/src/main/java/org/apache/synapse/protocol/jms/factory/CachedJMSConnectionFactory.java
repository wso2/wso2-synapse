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

package org.apache.synapse.protocol.jms.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.protocol.jms.JMSConstants;

import javax.jms.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class CachedJMSConnectionFactory extends JMSConnectionFactory {
    private static final Log logger = LogFactory.getLog(CachedJMSConnectionFactory.class);

    private int cacheLevel = 0;

    private Connection cachedConnection = null;
    private Session cachedSession = null;
    
    public CachedJMSConnectionFactory(Properties properties) {
        super(properties);
        String cacheLevel = properties.getProperty(JMSConstants.PARAM_CACHE_LEVEL);
        if(null != cacheLevel && !"".equals(cacheLevel)) {
            this.cacheLevel = Integer.parseInt(cacheLevel);
        } else {
            this.cacheLevel = JMSConstants.CACHE_NONE;
        }
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        return super.getConnectionFactory();
    }

    public Connection getConnection(String userName, String password) { 
    	Connection connection = null;
        if (cachedConnection == null) {
        	connection = createConnection(userName, password);
        }else{
        	connection = cachedConnection;
        }
        if(connection == null){
        	return null;
        }
        try {
        	connection.start();
        } catch (JMSException e) {
            logger.error("JMS Exception while starting connection for factory '" + this.connectionFactoryString + "' " + e.getMessage());
            resetCache();
        }        
        return connection;
    }

    @Override
    public Connection createConnection(String userName, String password){
        Connection connection = null;
        if(userName == null || password == null){
        	connection = super.createConnection();
        }else{
        	connection = super.createConnection(userName, password);
        }
        if(this.cacheLevel >= JMSConstants.CACHE_CONNECTION){
        	cachedConnection = connection;
        }
        return connection;
    }

    @Override
    public Session getSession(Connection connection) {
        if (cachedSession == null) {
            return createSession(connection);
        }else{
        	return cachedSession;
        }      
    }

    @Override
    protected Session createSession(Connection connection) {
        Session session = super.createSession(connection);
        if(this.cacheLevel >= JMSConstants.CACHE_SESSION){
        	cachedSession = session;
        }
        return session;
    }

    public boolean closeConnection() {
        try {
        	if(cachedConnection != null){
        		cachedConnection.close();
        	}
            return true;
        } catch (JMSException e) {
            logger.error("JMS Exception while closing the connection.");
        }
        return false;
    }

    private void resetCache(){
    	if(cachedConnection != null){
    		try{
    			cachedConnection.close();
    		}catch(JMSException e){}
    		cachedConnection = null;
    	}
    	if(cachedSession != null){
    		try{
    			cachedSession.close();
    		}catch(JMSException e){}
    		cachedSession = null;
    	}    	
    }
    
    public JMSConstants.JMSDestinationType getDestinationType() {
        return this.destinationType;
    }
}
