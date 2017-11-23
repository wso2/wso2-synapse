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

package org.apache.synapse.commons.datasource.serializer;

import junit.framework.TestCase;
import org.apache.synapse.commons.datasource.DataSourceInformation;
import org.wso2.securevault.secret.SecretInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Test class for DataSourceInformationSerializer and DataSourceInformationListSerializer
 */
public class DataSourceInformationSerializerTest extends TestCase {

    /**
     * Test serializing DataSourceInformation list
     */
    public void testSerializeDataSourceInformation() {

        List<DataSourceInformation> dataSourceInformationList = new ArrayList<>();

        DataSourceInformation dataSourceInformation1 = new DataSourceInformation();
        dataSourceInformation1.setDriver("org.h2.Driver");
        dataSourceInformation1.setUrl("jdbc:h2:repository/database/test_db1");
        dataSourceInformation1.setAlias("dataSource1");
        SecretInformation secretInformation = new SecretInformation();
        secretInformation.setUser("user1");
        secretInformation.setAliasSecret("user1password");
        dataSourceInformation1.setSecretInformation(secretInformation);
        dataSourceInformationList.add(dataSourceInformation1);

        DataSourceInformation dataSourceInformation2 = new DataSourceInformation();
        dataSourceInformation2.setDriver("org.h2.Driver");
        dataSourceInformation2.setUrl("jdbc:h2:repository/database/test_db2");
        dataSourceInformation2.setAlias("dataSource2");
        dataSourceInformationList.add(dataSourceInformation2);

        DataSourceInformation dataSourceInformation3 = new DataSourceInformation();
        dataSourceInformation3.setDriver("org.h2.Driver");
        dataSourceInformation3.setUrl("jdbc:h2:repository/database/test_db3");
        dataSourceInformation3.setAlias("dataSource3");
        dataSourceInformationList.add(dataSourceInformation3);

        Properties properties = DataSourceInformationListSerializer.serialize(dataSourceInformationList);
        String dataSources = properties.getProperty("synapse.datasources");

        assertTrue("'dataSource1' cannot be found in datasource list ", dataSources.contains("dataSource1"));
        assertTrue("'dataSource2' cannot be found in datasource list ", dataSources.contains("dataSource2"));
        assertTrue("'dataSource3' cannot be found in datasource list ", dataSources.contains("dataSource3"));
    }
}
