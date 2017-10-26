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

import java.util.Properties;

/**
 * Test class for DataSourceInformationFactory
 */
public class DataSourceInformationFactoryTest extends TestCase {

    /**
     * Test creating DataSourceInformation from given properties
     */
    public void testCreateDataSourceInformation() {
        Properties properties = new Properties();
        properties.put("synapse.datasources.dataSource1.driverClassName", "org.h2.Driver");
        properties.put("synapse.datasources.dataSource1.url", "jdbc:h2:repository/database/test_db");
        properties.put("synapse.datasources.dataSource1.dsName", "dataSource1");
        DataSourceInformation dataSourceInformation =
                DataSourceInformationFactory.createDataSourceInformation("dataSource1", properties);
    }

    /**
     * Test creating DataSourceInformation data source name = null
     */
    public void testCreateDataSourceInformationNameNull() {
        Properties properties = new Properties();
        DataSourceInformation dataSourceInformation =
                DataSourceInformationFactory.createDataSourceInformation(null, properties);
        assertNull("Expected null value", dataSourceInformation);
    }

    /**
     * Test creating DataSourceInformation empty data source name
     */
    public void testCreateDataSourceInformationWithEmptyName() {
        Properties properties = new Properties();
        DataSourceInformation dataSourceInformation =
                DataSourceInformationFactory.createDataSourceInformation("", properties);
        assertNull("Expected null value", dataSourceInformation);
    }

    /**
     * Test creating DatasourceInformation without defining db url
     */
    public void testCreateDataSourceInformationUrlNull() {
        Properties properties = new Properties();
        properties.put("synapse.datasources.dataSource1.driverClassName", "org.h2.Driver");
        properties.put("synapse.datasources.dataSource1.dsName", "dataSource1");
        try {
            DataSourceInformationFactory.createDataSourceInformation("dataSource1", properties);
        } catch (SynapseCommonsException e) {
            assertEquals("Invalid exception message",
                    "synapse.datasources.dataSource1.url cannot be found.", e.getMessage());
        }
    }
}
