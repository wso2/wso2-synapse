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
import org.apache.synapse.commons.datasource.DataSourceInformationRepository;

import java.util.Properties;

/**
 * Test class for DataSourceInformationRepositoryFactory
 */
public class DataSourceInformationRepositoryFactoryTest extends TestCase {

    /**
     * Test creating DataSourceInformationRepositoryFactory from properties
     */
    public void testCreateDataSourceInformationRepository() {

        Properties properties = new Properties();
        properties.put("synapse.datasources", "dataSource1,dataSource2,dataSource3");

        properties.put("synapse.datasources.dataSource1.driverClassName", "org.h2.Driver");
        properties.put("synapse.datasources.dataSource1.url", "jdbc:h2:repository/database/test_db1");
        properties.put("synapse.datasources.dataSource1.dsName", "dataSource1");

        properties.put("synapse.datasources.dataSource2.driverClassName", "org.h2.Driver");
        properties.put("synapse.datasources.dataSource2.url", "jdbc:h2:repository/database/test_db2");
        properties.put("synapse.datasources.dataSource2.dsName", "dataSource2");

        properties.put("synapse.datasources.dataSource3.driverClassName", "org.h2.Driver");
        properties.put("synapse.datasources.dataSource3.url", "jdbc:h2:repository/database/test_db3");
        properties.put("synapse.datasources.dataSource3.dsName", "dataSource3");

        DataSourceInformationRepository dataSourceInformationRepository =
                DataSourceInformationRepositoryFactory.createDataSourceInformationRepository(properties);
        assertNotNull("DataSourceInformationRepository is not created", dataSourceInformationRepository);
    }
}
