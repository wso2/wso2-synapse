/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.flowtracer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MessageFlowDbConnector {

    private JDBCConfiguration jdbcConfiguration;

    /**
     * Logger for the class
     */
    private static final Log logger = LogFactory.getLog(MessageFlowDbConnector.class.getName());

    private static MessageFlowDbConnector instance;

    public static MessageFlowDbConnector getInstance() {
        if(instance==null)
            instance = new MessageFlowDbConnector();

        return instance;
    }

    public MessageFlowDbConnector() {
        jdbcConfiguration = new JDBCConfiguration();
        jdbcConfiguration.buildDataSource();

        initTable1();
        initTable2();
    }

    private void initTable1() {
        Connection con = null;
        PreparedStatement ps = null;
        Statement stmt = new Statement("CREATE TABLE IF NOT EXISTS MESSAGE_FLOW_INFO\n" +
                "(\n" +
                "ID int NOT NULL AUTO_INCREMENT,"+
                "MessageId varchar(255)," +
                "Component varchar(255)," +
                "ComponentId varchar(255)," +
                "Response BOOLEAN,"+
                "Payload varchar(10000),"+
                "Properties varchar(5000),"+
                "Start BOOLEAN,"+
                "TimeStamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"+
                "PRIMARY KEY (ID)"+
                ")");
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            ps.execute();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

    private void initTable2() {
        Connection con = null;
        PreparedStatement ps = null;
        Statement stmt = new Statement("CREATE TABLE IF NOT EXISTS MESSAGE_FLOWS\n" +
                "(\n" +
                "ID int NOT NULL AUTO_INCREMENT,"+
                "MessageId varchar(255)," +
                "FlowTrace varchar(1000)," +
                "TimeStamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"+
                "PRIMARY KEY (ID)"+
                ")");
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            ps.execute();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

    public void writeToDb(MessageContext synCtx) {
        Connection con = null;
        PreparedStatement ps = null;

        Statement stmt = new Statement("INSERT INTO "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" (MessageId, FlowTrace)\n" +
                "VALUES (\'"+synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID)+"\', \'"+synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW)+"\')");
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            int count = ps.executeUpdate();
            con.commit();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

    public void writeToDb(MessageFlowEntry entry) {
        Connection con = null;
        PreparedStatement ps = null;

        Statement stmt = new Statement("INSERT INTO "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOW_INFO+" (MessageId, Component, ComponentId, Response, Start, Payload, Properties, Timestamp)\n" +
                "VALUES (\'"+entry.getMessageId()+"\', \'"+entry.getComponentName()+"\', "+" \'"+entry.getComponentId()+"\', "+entry.isResponse()+","+entry.isStart()+", \'"+entry.getPayload().toString().replace("\'", "\'\'")+"\',\'"+entry.getPropertySet()+"\',\'"+entry.getTimestamp()+"\')");
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            int count = ps.executeUpdate();
            con.commit();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

    public String[] getMessageFlowTrace(String messageId){
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        Statement stmt = new Statement("SELECT * FROM "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" WHERE messageId = \'"+messageId+"\'");

        Set<String> messageFlows = new HashSet<>();

        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    messageFlows.add(rs.getString("FlowTrace"));
                } catch (Exception e) {
                    logger.error("Error executing statement : " + stmt.getRawStatement() +
                            " against DataSource : " + jdbcConfiguration.getDSName(), e);
                    break;
                }
            }

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return messageFlows.toArray(new String[messageFlows.size()]);
    }

    public String[] getMessageFlows() {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        Statement stmt = new Statement("SELECT distinct ID,MessageId FROM " + MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" ORDER BY ID");

        Set<String> messageFlows = new HashSet<>();

        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    messageFlows.add(rs.getString("MessageId"));
                } catch (Exception e) {
                    logger.error("Error executing statement : " + stmt.getRawStatement() +
                            " against DataSource : " + jdbcConfiguration.getDSName(), e);
                    break;
                }
            }

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return messageFlows.toArray(new String[messageFlows.size()]);
    }

    public String[] getMessageFlowInfo(String messageId){
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        Statement stmt = new Statement("SELECT * FROM " + MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" WHERE MessageId = \'"+messageId+"\' order by ID");

        ArrayList<String> messageFlows = new ArrayList<>();

        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    messageFlows.add(rs.getString("FlowTrace"));
                } catch (Exception e) {
                    logger.error("Error executing statement : " + stmt.getRawStatement() +
                            " against DataSource : " + jdbcConfiguration.getDSName(), e);
                    break;
                }
            }

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return messageFlows.toArray(new String[messageFlows.size()]);
    }

    public void clearTable(){
        clearTable(MessageFlowTracerConstants.TABLE_MESSAGE_FLOW_INFO);
        clearTable(MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS);
    }

    public void clearTable(String tableName){
        Connection con = null;
        PreparedStatement ps = null;
        Statement stmt = new Statement("DELETE FROM "+ tableName);
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            int count = ps.executeUpdate();
            con.commit();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

    /**
     * Close all ResultSet related things
     *
     * @param con - Connection to close
     * @param ps  - PreparedStatement to close
     * @param rs  - ResultSet to close
     */
    private void close(Connection con, PreparedStatement ps, ResultSet rs) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                logger.error("Error while closing prepared statement", e);
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("Error while closing result set", e);
            }
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                logger.error("Error while closing connection", e);
            }
        }
    }
}
