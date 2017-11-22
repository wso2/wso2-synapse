/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.commons.datasource;

import org.apache.synapse.commons.SynapseCommonsException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.wso2.securevault.secret.SecretInformation;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Unit tests for DataSourceRepositoryManager class.
 */
public class DataSourceRepositoryManagerTest {
    private static DataSourceRepositoryManager repositoryManager;
    private static InMemoryDataSourceRepository inMemoryDataSourceRepository = new InMemoryDataSourceRepository();
    private static JNDIBasedDataSourceRepository jndiBasedDataSourceRepository = new JNDIBasedDataSourceRepository();
    private static DataSourceInformation informationInMemory = new DataSourceInformation();
    private static DataSourceInformation informationJndi = new DataSourceInformation();
    private static DataSourceInformation informationInMemSample = new DataSourceInformation();
    private static DataSourceInformation informationJndiSample = new DataSourceInformation();
    private static List<DataSourceInformation> informationList = new ArrayList<>();
    private static Properties properties = new Properties();
    private static SecretInformation secretInformation = new SecretInformation();
    private static final String DATASOURCE_NAME = "testDatasourceInMemory";
    private static final String DATASOURCE_NAME_ALTER = "testDatasourceInMemoryAlterHash";
    private static final String DATASOURCE_NAME_JNDI = "testDatasourceJndi";
    private static final String ERROR_DATASOURCE_NAME = "notExisting";
    private static final String DRIVER = "testDriver";
    private static final String URL = "testUrl";
    private static final String USER = "testUser";
    private static final String PASSWORD = "testPassword";
    private static Field dataSources;
    private static Field cachedNameListJndi;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Initializing repositories and registering datasource.
     *
     * @throws NoSuchFieldException
     */
    @BeforeClass
    public static void initialize() throws NoSuchFieldException {
        informationList.add(informationInMemory);
        informationList.add(informationJndi);
        informationList.add(informationInMemSample);
        informationList.add(informationJndiSample);
        secretInformation.setUser(USER);
        secretInformation.setAliasSecret(PASSWORD);

        for (DataSourceInformation temp : informationList) {
            temp.setDriver(DRIVER);
            temp.setUrl(URL);
            temp.setProperties(properties);
            temp.setSecretInformation(secretInformation);
        }

        informationInMemory.setDatasourceName(DATASOURCE_NAME);
        informationInMemSample.setDatasourceName(DATASOURCE_NAME_ALTER);
        informationJndiSample.setDatasourceName(DATASOURCE_NAME_JNDI);
        informationJndi.setDatasourceName(DATASOURCE_NAME_JNDI);
        informationJndi.setType(DataSourceInformation.BASIC_DATA_SOURCE);
        informationJndiSample.setRepositoryType(DataSourceConstants.PROP_REGISTRY_JNDI);
        informationInMemSample.setRepositoryType(DataSourceConstants.PROP_REGISTRY_MEMORY);

        dataSources = InMemoryDataSourceRepository.class.getDeclaredField("dataSources");
        dataSources.setAccessible(true);
        cachedNameListJndi = JNDIBasedDataSourceRepository.class.getDeclaredField("cachedNameList");
        cachedNameListJndi.setAccessible(true);

        Properties jndiEnv = new Properties();
        jndiEnv.setProperty(DataSourceConstants.PROP_SYNAPSE_PREFIX_DS, "SampleDataSource");
        jndiBasedDataSourceRepository.init(jndiEnv);
        inMemoryDataSourceRepository.register(informationInMemory);
        jndiBasedDataSourceRepository.register(informationJndi);
        repositoryManager =
                new DataSourceRepositoryManager(inMemoryDataSourceRepository, jndiBasedDataSourceRepository);
    }

    /**
     * Test getDataSource method with empty string.
     */
    @Test
    public void testGetDataSourceEmpty() {
        thrown.expectMessage("DataSource name cannot be found.");
        thrown.expect(SynapseCommonsException.class);
        repositoryManager.getDataSource("");
    }

    /**
     * Test getDataSource method with not existing datasource.
     */
    @Test
    public void testGetDataSourceNonExisting() {
        Assert.assertNull("not existing datasource should return null",
                repositoryManager.getDataSource(ERROR_DATASOURCE_NAME));
    }

    /**
     * Test getDataSource method with inMemory datasource.
     */
    @Test
    public void testGetDataSourceInMemory() {
        Assert.assertNotNull("datasource should exist in inMemoryRepository",
                repositoryManager.getDataSource(DATASOURCE_NAME));
    }

    /**
     * Test getDataSource method with Jndi datasource.
     */
    @Test
    public void testGetDataSourceJndi() {
        Assert.assertNotNull("datasource should exist in jndiRepository",
                repositoryManager.getDataSource(DATASOURCE_NAME_JNDI));
    }

    /**
     * Test addDataSourceInformation with null input.
     */
    @Test
    public void testAddDataSourceInformationNull() {
        thrown.expect(SynapseCommonsException.class);
        thrown.expectMessage("Provided DataSource Information instance is null");
        repositoryManager.addDataSourceInformation(null);
    }

    /**
     * Test add / remove dataSourceInformation.
     *
     * @throws IllegalAccessException
     */
    @Test
    public void testAddRemoveDataSourceInformation() throws IllegalAccessException {
        int jndiSize = ((List<String>) cachedNameListJndi.get(jndiBasedDataSourceRepository)).size();
        int inMemSize = ((Map<String, DataSource>) dataSources.get(inMemoryDataSourceRepository)).size();
        repositoryManager.addDataSourceInformation(informationJndiSample);
        Assert.assertEquals("cache should have new entries",
                ((List<String>) cachedNameListJndi.get(jndiBasedDataSourceRepository)).size(), jndiSize + 1);
        repositoryManager.addDataSourceInformation(informationInMemSample);
        Assert.assertEquals("map should have new entry",
                ((Map<String, DataSource>) dataSources.get(inMemoryDataSourceRepository)).size(), inMemSize + 1);
        repositoryManager.removeDataSourceInformation(informationJndiSample);
        Assert.assertEquals("datasource should be removed from the cache",
                ((List<String>) cachedNameListJndi.get(jndiBasedDataSourceRepository)).size(), jndiSize);
        repositoryManager.removeDataSourceInformation(informationInMemSample);
        Assert.assertEquals("datasource should be removed from the collection",
                ((Map<String, DataSource>) dataSources.get(inMemoryDataSourceRepository)).size(), inMemSize);
    }

    /**
     * Testing clear method.
     *
     * @throws IllegalAccessException
     */
    @AfterClass
    public static void testClear() throws IllegalAccessException {
        repositoryManager.clear();
        Assert.assertEquals("cache must be cleared",
                ((List<String>) cachedNameListJndi.get(jndiBasedDataSourceRepository)).size(), 0);
        Assert.assertEquals("collection must be cleared",
                ((Map<String, DataSource>) dataSources.get(inMemoryDataSourceRepository)).size(), 0);
    }
}
