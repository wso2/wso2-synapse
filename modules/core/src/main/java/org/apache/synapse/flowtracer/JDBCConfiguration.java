/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.datasource.DataSourceFinder;
import org.apache.synapse.commons.datasource.DataSourceInformation;
import org.apache.synapse.commons.datasource.DataSourceRepositoryHolder;
import org.apache.synapse.commons.datasource.RepositoryBasedDataSourceFinder;
import org.wso2.securevault.secret.SecretManager;

import javax.naming.Context;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Class <code>JDBCUtil</code> provides the Utility functions to create JDBC resources
 */
public class JDBCConfiguration {

    /**
     * Logger for the class
     */
    private static final Log log = LogFactory.getLog(JDBCConfiguration.class);

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

    public void buildDataSource() {
        try {
            readLookupConfig();
            dataSource = lookupDataSource();
        } catch (Exception e) {
            log.error("Error looking up DataSource connection information: ", e);
        }
    }

    private void readLookupConfig() {
        String dataSourceName = MessageFlowTracerConstants.JDBC_DSNAME;
        this.setDataSourceName(dataSourceName);
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
        if (dataSource != null) {
            log.info("Successfully looked up datasource " + dataSourceName);
        }
        return dataSource;
    }

    /**
     * Get the password from SecretManager . here only use SecretManager
     *
     * @param aliasPassword alias password
     * @return if the SecretManager is initiated , then , get the corresponding secret
     * , else return alias itself
     */
    private String getActualPassword(String aliasPassword) {
        SecretManager secretManager = SecretManager.getInstance();
        if (secretManager.isInitialized()) {
            return secretManager.getSecret(aliasPassword);
        }
        return aliasPassword;
    }

    /**
     * Get a Connection from current datasource
     *
     * @return - Connection
     * @throws java.sql.SQLException - Failure in creating datasource connection
     */
    public Connection getConnection() throws SQLException {
        Connection con = getDataSource().getConnection();
        if (con == null) {
            String msg = "Connection from DataSource " + getDSName() + " is null.";
            throw new SynapseException(msg);
        }
        return con;
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
     * Handle Exceptions during process
     *
     * @param o - Exception needs to handle
     */
    private void handleException(Object o) {
        log.error(o);
    }
}
