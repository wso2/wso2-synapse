/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.mediators.db;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.DBReportMediatorFactory;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediatorTestCase;
import org.apache.synapse.mediators.TestUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DBReportMediatorJsonTest extends AbstractMediatorTestCase {

    private static DBReportMediator report;

    public void testReportMediatorJsonPath() throws Exception {
        MessageContext synCtx = TestUtils.getTestContextJson(
                "{\"from\":\"me\",\"count\":5,\"to\":\"you\",\"category\":\"GOLD\"}", null);
        assertTrue(report.mediate(synCtx));
        Connection con = report.getDataSource().getConnection();
        ResultSet rs = con.createStatement().executeQuery(
                "select fromepr, cnt, toepr, category from audit");
        if (rs.next()) {
            assertEquals("me", rs.getString("fromepr"));
            assertEquals(5, rs.getInt("cnt"));
            assertEquals("you", rs.getString("toepr"));
            assertEquals("GOLD", rs.getString("category"));
        } else {
            fail("DB report json use case failed");
        }
        assertEquals("Validate updated raw count", synCtx.getProperty("DBREPORT_MODIFIED_RAW_COUNT"), 1);
    }

    public static Test suite() {
        return new TestSetup(new TestSuite(DBReportMediatorJsonTest.class)) {

            @Override
            protected void setUp() throws Exception {

                String baseDir = System.getProperty("basedir");
                if (baseDir == null) {
                    baseDir = ".";
                }

                report = (DBReportMediator)
                        new DBReportMediatorFactory().createMediator(createOMElement(
                                "<dbreport xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                                        "  <connection>\n" +
                                        "    <pool>\n" +
                                        "      <driver>org.apache.derby.jdbc.EmbeddedDriver</driver>\n" +
                                        "      <url>jdbc:derby:" + baseDir + "/target/derbyDB;create=true</url>\n" +
                                        "      <user>user</user>\n" +
                                        "      <password>pass</password>\n" +
                                        "      <property name=\"initialsize\" value=\"2\"/>\n" +
                                        "      <property name=\"isolation\" value=\"Connection.TRANSACTION_SERIALIZABLE\"/>\n" +
                                        "    </pool>\n" +
                                        "  </connection>\n" +
                                        "  <statement>\n" +
                                        "    <sql>insert into audit values(?, ?, ?, ?)</sql>\n" +
                                        "    <parameter expression=\"json-eval($.from)\" type=\"VARCHAR\"/>\n" +
                                        "    <parameter expression=\"json-eval($.count)\" type=\"INTEGER\"/>\n" +
                                        "    <parameter expression=\"json-eval($.to)\" type=\"VARCHAR\"/>\n" +
                                        "    <parameter value=\"GOLD\" type=\"VARCHAR\"/>\n" +
                                        "  </statement>\n" +
                                        "</dbreport>"
                                                                                    ), new Properties());

                report.init(new Axis2SynapseEnvironment(new SynapseConfiguration()));

                java.sql.Statement s = report.getDataSource().getConnection().createStatement();
                try {
                    s.execute("drop table audit");
                } catch (SQLException ignore) {}
                s.execute("create table audit(fromepr varchar(10), cnt int, toepr varchar(10), category varchar(10))");
                s.close();
            }

            @Override
            protected void tearDown() throws Exception {

            }
        };
    }
}
