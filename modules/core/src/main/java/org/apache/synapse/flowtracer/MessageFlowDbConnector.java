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
import org.apache.synapse.flowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.flowtracer.data.MessageFlowTraceEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MessageFlowDbConnector {

    private JDBCConfiguration jdbcConfiguration;

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
        String stmt = new String("CREATE TABLE IF NOT EXISTS MESSAGE_FLOW_INFO\n" +
                "(\n" +
                "ID int NOT NULL AUTO_INCREMENT,"+
                "MessageId varchar(255)," +
                "Component varchar(255)," +
                "ComponentId varchar(255)," +
                "Response BOOLEAN,"+
                "Payload varchar(1000000),"+
                "Properties varchar(1000000),"+
                "Start BOOLEAN,"+
                "TimeStamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"+
                "PRIMARY KEY (ID)"+
                ")");
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt);
            ps.execute();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

    private void initTable2() {
        Connection con = null;
        PreparedStatement ps = null;
        String stmt = new String("CREATE TABLE IF NOT EXISTS MESSAGE_FLOWS\n" +
                "(\n" +
                "ID int NOT NULL AUTO_INCREMENT,"+
                "MessageId varchar(255)," +
                "FlowTrace varchar(10000)," +
                "EntryType varchar(1000)," +
                "TimeStamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"+
                "PRIMARY KEY (ID)"+
                ")");
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt);
            ps.execute();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

//    public void persistMessageFlowTraceEntry(MessageFlowTraceEntry entry) {
//        Connection con = null;
//        PreparedStatement ps = null;
//
//        String stmt = new String("INSERT INTO "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" (MessageId, FlowTrace, EntryType, TimeStamp)\n" +
//                "VALUES (\'"+entry.getMessageId()+"\', \'"+entry.getMessageFlow()+"\', \'"+entry.getEntryType()+ "\', \'"+entry.getTimeStamp()+ "\')");
//        try {
//            con = jdbcConfiguration.getConnection();
//            ps = con.prepareStatement(stmt);
//            int count = ps.executeUpdate();
//            con.commit();
//
//        } catch (SQLException e) {
//            logger.error("Error executing statement : " + stmt +
//                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
//        } finally {
//            close(con, ps, null);
//        }
//    }

    //    public void persistMessageFlowComponentEntry(MessageFlowComponentEntry entry) {
//        Connection con = null;
//        PreparedStatement ps = null;
//
//        String stmt = new String("INSERT INTO "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOW_INFO+" (MessageId, Component, ComponentId, Response, Start, Payload, Properties, Timestamp)\n" +
//                "VALUES (\'"+entry.getMessageId()+"\', \'"+entry.getComponentName()+"\', "+" \'"+entry.getComponentId()+"\', "+entry.isResponse()+","+entry.isStart()+", \'"+entry.getPayload().toString().replace("\'", "\'\'")+"\',\'"+entry.getPropertySet().toString().replace("\'", "\'\'")+"\',\'"+entry.getTimestamp()+"\')");
//        try {
//            con = jdbcConfiguration.getConnection();
//            ps = con.prepareStatement(stmt);
//            int count = ps.executeUpdate();
//            con.commit();
//
//        } catch (SQLException e) {
//            logger.error("Error executing statement : " + stmt +
//                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
//        } finally {
//            close(con, ps, null);
//        }
//    }

    public void persistMessageFlowTraceEntry(MessageFlowTraceEntry entry) {
        Connection con = null;
        PreparedStatement ps = null;

        String stmt = SQLQueries.INSERT_MESSAGE_FLOWS_ENTRY;
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt);

            ps.setString(1,entry.getMessageId());
            ps.setString(2,entry.getMessageFlow());
            ps.setString(3,entry.getEntryType());
            ps.setString(4,entry.getTimeStamp());

            ps.executeUpdate();
            con.commit();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

    public void persistMessageFlowComponentEntry(MessageFlowComponentEntry entry) {
        Connection con = null;
        PreparedStatement ps = null;

        String stmt = SQLQueries.INSERT_MESSAGE_FLOW_INFO_ENTRY;
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt);

            ps.setString(1,entry.getMessageId());
            ps.setString(2,entry.getComponentName());
            ps.setString(3,entry.getComponentId());
            ps.setBoolean(4,entry.isResponse());
            ps.setBoolean(5,entry.isStart());
            ps.setString(6,entry.getPayload());
            ps.setString(7,entry.getPropertySet());
            ps.setString(8,entry.getTimestamp());

            ps.executeUpdate();
            con.commit();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, null);
        }
    }

    public String[] getMessageFlowTrace(String messageId){
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        String stmt = SQLQueries.GET_MESSAGE_FLOW_TRACE;

        Set<String> messageFlows = new HashSet<>();

        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt);

            ps.setString(1,messageId);

            rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    messageFlows.add(rs.getString("FlowTrace"));
                } catch (Exception e) {
                    logger.error("Error executing statement : " + stmt +
                            " against DataSource : " + jdbcConfiguration.getDSName(), e);
                    break;
                }
            }

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt+
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return messageFlows.toArray(new String[messageFlows.size()]);
    }

    public Map<String,MessageFlowTraceEntry> getMessageFlows() {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        String stmt = SQLQueries.GET_ALL_MESSAGE_FLOWS;

        Map<String,MessageFlowTraceEntry> messageFlows = new HashMap<>();

        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt);
            rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    messageFlows.put(rs.getString("MessageId"), new MessageFlowTraceEntry(rs.getString("MessageId"),rs.getString("EntryType"),rs.getString("TimeStamp")));
                } catch (Exception e) {
                    logger.error("Error executing statement : " + stmt +
                            " against DataSource : " + jdbcConfiguration.getDSName(), e);
                    break;
                }
            }

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return messageFlows;
    }

    public MessageFlowComponentEntry[] getComponentInfo(String messageId) {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        String stmt = SQLQueries.GET_COMPONENT_INFO;

        List<MessageFlowComponentEntry> componentInfo = new ArrayList<>();

        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt);

            ps.setString(1,messageId);

            rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    componentInfo.add(new MessageFlowComponentEntry(rs.getString("MessageId"), rs.getString("ComponentId"), rs.getString("Component"),rs.getBoolean("Response"),rs.getBoolean("Start"),rs.getString("Timestamp"),rs.getString("Properties"),rs.getString("Payload")));
                } catch (Exception e) {
                    logger.error("Error executing statement : " + stmt +
                            " against DataSource : " + jdbcConfiguration.getDSName(), e);
                    break;
                }
            }

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return componentInfo.toArray(new MessageFlowComponentEntry[componentInfo.size()]);
    }

    public void clearTables(){
        clearTable(MessageFlowTracerConstants.TABLE_MESSAGE_FLOW_INFO);
        clearTable(MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS);
    }

    public void clearTable(String tableName){
        Connection con = null;
        PreparedStatement ps = null;
        String stmt = new String(SQLQueries.DELETE_ALL+ tableName);
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt);
            int count = ps.executeUpdate();
            con.commit();

        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt +
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
