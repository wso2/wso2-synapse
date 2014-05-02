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
    private static final Log logger = LogFactory.getLog(CachedJMSConnectionFactory.class.getName());

    private Map<Integer, Connection> cachedConnections = new ConcurrentHashMap<Integer, Connection>();
    private Map<Integer, Session> cachedSessions = new ConcurrentHashMap<Integer, Session>();

    private volatile int connectionCount = 0;
    private volatile int sessionCount = 0;

    private int cacheLevel = 0;
    private int maxConnectionCount = 10;
    private int maxSessionCount = 10;

    public CachedJMSConnectionFactory(Properties properties) {
        super(properties);

        String maxSharedConnectionCount = properties.getProperty(JMSConstants.MAX_JMS_CONNECTIONS);
        if (null != maxSharedConnectionCount && !"".equals(maxSharedConnectionCount)) {
            this.maxConnectionCount = Integer.parseInt(maxSharedConnectionCount);
        } else {
            this.maxConnectionCount = 10;
        }

        String cacheLevel = properties.getProperty(JMSConstants.PARAM_CACHE_LEVEL);
        if(null != cacheLevel && !"".equals(cacheLevel)) {
            this.cacheLevel = Integer.parseInt(cacheLevel);
        } else {
            this.cacheLevel = JMSConstants.CACHE_NONE;
        }

        String maxSharedSessionCount = properties.getProperty(JMSConstants.MAX_JMS_SESSIONS);
        if (null != maxSharedSessionCount && !"".equals(maxSharedSessionCount)) {
            this.maxSessionCount = Integer.parseInt(maxSharedSessionCount);
        } else {
            this.maxSessionCount = 10;
        }
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        return super.getConnectionFactory();
    }

    public Connection getConnection() {
        Connection connection = cachedConnections.get(connectionCount);
        if (connection == null) {
            return createConnection();
        }

        return connection;
    }

    @Override
    public Connection createConnection() {
        Connection connection = super.createConnection();
        cachedConnections.put(connectionCount, connection);
        connectionCount++;
        if(connectionCount >= maxConnectionCount) {
            connectionCount = 0;
        }

        return connection;
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        Connection connection = super.createConnection(userName, password);
        cachedConnections.put(connectionCount, connection);
        connectionCount++;
        if(connectionCount >= maxConnectionCount) {
            connectionCount = 0;
        }

        return connection;
    }

    @Override
    public QueueConnection createQueueConnection() throws JMSException {
        QueueConnection connection = super.createQueueConnection();
        cachedConnections.put(connectionCount, connection);
        connectionCount++;
        if(connectionCount >= maxConnectionCount) {
            connectionCount = 0;
        }

        return connection;
    }

    @Override
    public QueueConnection createQueueConnection(String userName, String password) throws JMSException {
        QueueConnection connection = super.createQueueConnection(userName, password);
        cachedConnections.put(connectionCount, connection);
        connectionCount++;
        if(connectionCount >= maxConnectionCount) {
            connectionCount = 0;
        }

        return connection;
    }

    @Override
    public TopicConnection createTopicConnection() throws JMSException {
        TopicConnection connection = super.createTopicConnection();
        cachedConnections.put(connectionCount, connection);
        connectionCount++;
        if(connectionCount >= maxConnectionCount) {
            connectionCount = 0;
        }

        return connection;
    }

    @Override
    public TopicConnection createTopicConnection(String userName, String password) throws JMSException {
        TopicConnection connection = super.createTopicConnection(userName, password);
        cachedConnections.put(connectionCount, connection);
        connectionCount++;
        if(connectionCount >= maxConnectionCount) {
            connectionCount = 0;
        }

        return connection;    }

    @Override
    public Session getSession(Connection connection) {
        Session cSession = cachedSessions.get(connectionCount);
        if (cSession == null) {
            return createSession(connection);
        }

        return cSession;
    }

    @Override
    protected Session createSession(Connection connection) {
        Session cSession = super.createSession(connection);
        cachedSessions.put(sessionCount, cSession);
        sessionCount++;
        if(sessionCount >= maxSessionCount) {
            sessionCount = 0;
        }

        return cSession;
    }

    public void start() {
        try {
            for(Map.Entry<Integer, Connection> c: cachedConnections.entrySet()) {
                c.getValue().start();
            }
        } catch (JMSException e) {
            logger.error("JMS Exception while starting connection for factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }
    }

    public void stop() {
        try {
            for(Map.Entry<Integer, Connection> c: cachedConnections.entrySet()) {
                c.getValue().stop();
            }        } catch (JMSException e) {
            logger.error("JMS Exception while stopping connection for factory '" + this.connectionFactoryString + "' " + e.getMessage());
        }
    }

    public boolean closeConnection() {
        try {
            for(Map.Entry<Integer, Connection> c: cachedConnections.entrySet()) {
                c.getValue().close();
            }
            return true;
        } catch (JMSException e) {
            logger.error("JMS Exception while closing the connection.");
        }

        return false;
    }

    public JMSConstants.JMSDestinationType getDestinationType() {
        return this.destinationType;
    }
}
