/**
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
package org.apache.synapse.message.store.impl.jdbc.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.datasource.DataSourceFinder;
import org.apache.synapse.commons.datasource.DataSourceInformation;
import org.apache.synapse.commons.datasource.DataSourceRepositoryHolder;
import org.apache.synapse.commons.datasource.RepositoryBasedDataSourceFinder;
import org.apache.synapse.commons.datasource.factory.DataSourceFactory;
import org.apache.synapse.message.store.impl.jdbc.JDBCMessageStoreConstants;
import org.apache.synapse.message.store.impl.jdbc.message.StorableMessage;
import org.wso2.securevault.secret.SecretInformation;
import org.wso2.securevault.secret.SecretManager;

import javax.naming.Context;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Class <code>JDBCUtil</code> provides the Utility functions to create JDBC resources
 */
public class JDBCUtil {
    private static final Log log = LogFactory.getLog(JDBCUtil.class);

    /**
     * Information about datasource
     */
    private DataSourceInformation dataSourceInformation;

    /**
     * Name of the data source
     */
    private String dataSourceName;

    /**
     * Jndi properties
     */
    private Properties jndiProperties = new Properties();

    /**
     * Data source
     */
    private DataSource dataSource;

    /**
     * Name of the table
     */
    private String tableName;

    /**
     * Creating datasource at startup using configured parameters
     *
     * @param parameters - parameters given in configuration
     */
    public void buildDataSource(Map<String, Object> parameters) {
        try {
            // Get datasource information
            if ((parameters.get(JDBCMessageStoreConstants.JDBC_DSNAME)) != null) {
                readLookupConfig(parameters);
                dataSource = lookupDataSource();
            } else if ((parameters.get(JDBCMessageStoreConstants.JDBC_CONNECTION_DRIVER)) != null) {
                readCustomDataSourceConfig(parameters);
                dataSource = createCustomDataSource();
            } else {
                handleException("The DataSource connection information must be specified for " +
                                "using a custom DataSource connection pool or for a JNDI lookup");
            }

            // Get table information
            if (parameters.get(JDBCMessageStoreConstants.JDBC_TABLE) != null) {
                tableName = (String) parameters.get(JDBCMessageStoreConstants.JDBC_TABLE);

            } else {
                tableName = "synapse_jdbc_store";
            }
        } catch (Exception e) {
            log.error("Error looking up DataSource connection information: ", e);
        }
    }

    /**
     * Reading lookup information for existing datasource
     *
     * @param parameters -  parameters given in configuration
     */
    private void readLookupConfig(Map<String, Object> parameters) {
        String dataSourceName = (String) parameters.get(JDBCMessageStoreConstants.JDBC_DSNAME);
        this.setDataSourceName(dataSourceName);

        if (parameters.get(JDBCMessageStoreConstants.JDBC_ICCLASS) != null) {
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, parameters.get(JDBCMessageStoreConstants.JDBC_ICCLASS));
            props.put(Context.PROVIDER_URL, parameters.get(JDBCMessageStoreConstants.JDBC_CONNECTION_URL));
            props.put(Context.SECURITY_PRINCIPAL, parameters.get(JDBCMessageStoreConstants.JDBC_USERNAME));
            props.put(Context.SECURITY_CREDENTIALS, parameters.get(JDBCMessageStoreConstants.JDBC_PASSWORD));

            this.setJndiProperties(props);
        }
    }

    /**
     * Configure for custom datasource
     *
     * @param parameters - parameters given in configuration
     */
    private void readCustomDataSourceConfig(Map<String, Object> parameters) {

        DataSourceInformation dataSourceInformation = new DataSourceInformation();

        dataSourceInformation.setDriver((String) parameters.get(JDBCMessageStoreConstants.JDBC_CONNECTION_DRIVER));
        dataSourceInformation.setUrl((String) parameters.get(JDBCMessageStoreConstants.JDBC_CONNECTION_URL));

        SecretInformation secretInformation = new SecretInformation();
        secretInformation.setUser((String) parameters.get(JDBCMessageStoreConstants.JDBC_USERNAME));
        secretInformation.setAliasSecret((String) parameters.get(JDBCMessageStoreConstants.JDBC_PASSWORD));
        dataSourceInformation.setSecretInformation(secretInformation);

        this.setDataSourceInformation(dataSourceInformation);
    }

    /**
     * Lookup the DataSource on JNDI using the specified name and optional properties
     *
     * @return a DataSource looked up using the specified JNDI properties
     */
    private DataSource lookupDataSource() {
        DataSource dataSource = null;
        RepositoryBasedDataSourceFinder finder = DataSourceRepositoryHolder.getInstance()
                .getRepositoryBasedDataSourceFinder();

        if (finder.isInitialized()) {
            // First try a lookup based on the data source name only
            dataSource = finder.find(dataSourceName);
        }
        if (dataSource == null) {
            // Decrypt the password if needed
            String password = jndiProperties.getProperty(Context.SECURITY_CREDENTIALS);
            if (password != null && !"".equals(password)) {
                jndiProperties.put(Context.SECURITY_CREDENTIALS, getActualPassword(password));
            }
            // Lookup the data source using the specified jndi properties
            dataSource = DataSourceFinder.find(dataSourceName, jndiProperties);
            if (dataSource == null) {
                handleException("Cannot find a DataSource " + dataSourceName + " for given JNDI" +
                                " properties :" + jndiProperties);
            }
        }
        log.info("Successfully looked up datasource " + dataSourceName + ".");
        return dataSource;
    }

    /**
     * Create a custom DataSource using the specified data source information.
     *
     * @return a DataSource created using specified properties
     */
    protected DataSource createCustomDataSource() {
        DataSource dataSource = DataSourceFactory.createDataSource(dataSourceInformation);
        if (dataSource != null) {
            log.info("Successfully created data source for " + dataSourceInformation.getUrl());
        }
        return dataSource;
    }

    /**
     * Get the password from SecretManager . here only use SecretManager
     *
     * @param aliasPasword alias password
     * @return if the SecretManager is initiated , then , get the corresponding secret
     * , else return alias itself
     */
    private String getActualPassword(String aliasPasword) {
        SecretManager secretManager = SecretManager.getInstance();
        if (secretManager.isInitialized()) {
            return secretManager.getSecret(aliasPasword);
        }
        return aliasPasword;
    }

    /**
     * Get a PreparedStatement for a given statement
     *
     * @param stmnt - A statement with query and parameters
     * @return - Prepared statement
     * @throws java.sql.SQLException - Failure in creating datasource connection
     */
    public PreparedStatement getPreparedStatement(Statement stmnt) throws SQLException {
        Connection con = getDataSource().getConnection();
        if (con == null) {
            String msg = "Connection from DataSource " + getDSName() + " is null.";
            log.error(msg);
            throw new SynapseException(msg);
        }
        PreparedStatement ps;
        ps = con.prepareStatement(stmnt.getRawStatement());
        return ps;
    }

    /**
     * Return the name or (hopefully) unique connection URL specific to the DataSource being used
     * This is used for logging purposes only
     *
     * @return a unique name or URL to refer to the DataSource being used
     */
    public String getDSName() {
        if (dataSourceName != null) {
            return dataSourceName;
        } else if (dataSourceInformation != null) {
            String name = dataSourceInformation.getUrl();
            if (name == null) {
                name = dataSourceInformation.getDatasourceName();
            }
            return name;
        }
        return null;
    }

    /**
     * Set DataSourceInformation
     *
     * @param dataSourceInformation - information to set
     */
    public void setDataSourceInformation(DataSourceInformation dataSourceInformation) {
        this.dataSourceInformation = dataSourceInformation;
    }

    /**
     * Set JNDI Properties
     *
     * @param jndiProperties -   properties to set
     */
    public void setJndiProperties(Properties jndiProperties) {
        this.jndiProperties = jndiProperties;
    }

    /**
     * Get datasource
     *
     * @return - Datasource currently in use
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Get datasource name currently in use
     *
     * @param dataSourceName - Datasource name
     */
    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    /**
     * Table name  in use
     *
     * @return - Name of the table
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Handle Exceptions during process
     *
     * @param o - Exception needs to handle
     */
    private void handleException(Object o) {
        log.error(o);
    }
}
