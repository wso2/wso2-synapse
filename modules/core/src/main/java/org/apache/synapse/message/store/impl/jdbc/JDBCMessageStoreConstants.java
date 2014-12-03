/**
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.message.store.impl.jdbc;

public class JDBCMessageStoreConstants {

    /**
     * Connection URL to database
     */
    public static final String JDBC_CONNECTION_URL = "store.jdbc.connection.url";

    /**
     * Driver to use
     */
    public static final String JDBC_CONNECTION_DRIVER = "store.jdbc.driver";

    /**
     * User Name that is used to create the connection with the broker
     */
    public static final String JDBC_USERNAME = "store.jdbc.username";

    /**
     * Password that is used to create the connection with the broker
     */
    public static final String JDBC_PASSWORD = "store.jdbc.password";

    /**
     * DataSource name exists
     */
    public static final String JDBC_DSNAME = "store.jdbc.dsName";

    /**
     * IcClass of the
     */
    public static final String JDBC_ICCLASS = "store.jdbc.icClass";


    // Optional parameters
    /**
     * Name of the database table
     */
    public static final String JDBC_TABLE = "store.jdbc.table";
    /**
     * Default name of the database table
     */
    public static final String JDBC_DEFAULT_TABLE_NAME = "synapse_jdbc_store";
}
