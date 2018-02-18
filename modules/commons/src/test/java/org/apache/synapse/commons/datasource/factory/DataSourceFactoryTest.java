/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.datasource.factory;

import junit.framework.TestCase;
import org.apache.synapse.commons.SynapseCommonsException;
import org.apache.synapse.commons.datasource.DataSourceInformation;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Test class for DataSourceFactory
 */
public class DataSourceFactoryTest extends TestCase {

    /**
     * Test create DataSource from DataSourceInformation object
     */
    public void testCreateDatasource() {
        DataSourceInformation dataSourceInformation = createDataSourceInformation();
        DataSource dataSource = DataSourceFactory.createDataSource(dataSourceInformation);
        assertNotNull("Datasource not created", dataSource);
    }

    /**
     * Test create DataSource from DataSourceInformation object with null url
     */
    public void testCreateDatasourceUrlNull() {
        DataSourceInformation dataSourceInformation = createDataSourceInformation();
        dataSourceInformation.setUrl(null);
        try {
            DataSourceFactory.createDataSource(dataSourceInformation);
            fail("SynapseCommonsException expected");
        } catch (SynapseCommonsException e) {
            assertEquals("Invalid exception message", "Database connection URL cannot be found.", e.getMessage());
        }
    }

    /**
     * Helper method to create DataSourceInformation object
     */
    private DataSourceInformation createDataSourceInformation() {
        Properties properties = new Properties();
        properties.put("synapse.datasources.dataSource1.driverClassName", "org.h2.Driver");
        properties.put("synapse.datasources.dataSource1.url", "jdbc:h2:repository/database/test_db");
        properties.put("synapse.datasources.dataSource1.dsName", "dataSource1");
        properties.put("synapse.datasources.dataSource1.username", "user1");
        properties.put("synapse.datasources.dataSource1.password", "user1password");
        return DataSourceInformationFactory.createDataSourceInformation("dataSource1", properties);
    }
}
