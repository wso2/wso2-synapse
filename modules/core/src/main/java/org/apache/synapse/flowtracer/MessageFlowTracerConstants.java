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

public class MessageFlowTracerConstants {

    /**
     * Connection URL to database
     */
    public static final String JDBC_CONNECTION_URL = "store.jdbc.connection.url";

    /**
     * Driver to use
     */
    public static final String JDBC_CONNECTION_DRIVER = "store.jdbc.driver";

    /**
     * DataSource name
     */
    public static final String JDBC_DSNAME = "jdbc/WSO2CarbonDB";

    /**
     * Name of the database table
     */
    public static final String TABLE_MESSAGE_FLOW_INFO = "MESSAGE_FLOW_INFO";

    public static final String TABLE_MESSAGE_FLOWS = "MESSAGE_FLOWS";

    public static final String MESSAGE_FLOW_ID = "MESSAGE_FLOW_ID";

    public static final String MESSAGE_FLOW = "MESSAGE_FLOW";
}
