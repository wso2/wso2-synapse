/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.apache.synapse.samples.framework;

import org.apache.axiom.om.OMElement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

import java.io.File;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Responsible for starting up and shutting down
 * a derby server instance in order to run a sample test.
 */
public class DerbyServerController extends AbstractBackEndServerController {

    private static final Log log = LogFactory.getLog(DerbyServerController.class);

    private NetworkServerControl server;
    private int port;

    public DerbyServerController(OMElement element) {
        super(element);
        port = Integer.parseInt(SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_DERBY_PORT,
                SampleConfigConstants.DEFAULT_BE_SERVER_CONF_DERBY_PORT));
    }

    public boolean startProcess() {
        try {
            //server
            Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
            server = new NetworkServerControl
                    (InetAddress.getByName("localhost"), port);
            server.start(null);
            while (true) {
                try {
                    server.ping();
                    break;
                } catch (Exception ignored) {
                    log.info("Waiting for Derby server to start...");
                    Thread.sleep(2000);
                }
            }
            log.info("Derby is successfully started.");
            initData();
            return true;
        } catch (Exception e) {
            log.error("There was an error starting Derby server: " + serverName, e);
            return false;
        }
    }

    private void initData() throws Exception {
        log.info("Creating the sample table and inserting values");

        //client
        String dbName = "synapsedb";
        String createTableQuery = "CREATE table company(name varchar(10), id varchar(10), price double)";
        String connectionURL = "jdbc:derby://localhost:1527/" + dbName + ";create=true";

        java.util.Properties props = new java.util.Properties();
        props.put("user", "synapse");
        props.put("password", "synapse");
        props.put("create", "true");

        Connection conn = null;
        try {
            // on JDK 1.6 or higher, EmbeddedDriver get loaded automatically.
            Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
            conn = DriverManager.getConnection(connectionURL, props);
            Statement s = conn.createStatement();
            s.execute(createTableQuery);
            s.execute("INSERT into company values ('IBM','c1',0.0)");
            s.execute(" INSERT into company values ('SUN','c2',0.0)");
            s.execute(" INSERT into company values ('MSFT','c3',0.0)");
            conn.commit();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean stopProcess() {
        log.info("Shutting down Derby server...");
        try {
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            } catch (SQLException se) {
                if (se.getErrorCode() == 50000 && "XJ015".equals(se.getSQLState())) {
                    // we got the expected exception
                    log.info("Derby shut down normally");
                }
            }
            server.shutdown();
            FileUtils.deleteDirectory(new File("./synapsedb"));
            return true;

        } catch (Exception e) {
            log.warn("Error while trying to delete database directory", e);
            return false;
        }
    }
}
