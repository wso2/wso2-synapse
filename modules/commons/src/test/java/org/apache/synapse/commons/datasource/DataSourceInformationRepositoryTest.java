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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

/**
 * Unit tests for DataSourceInformationRepository class.
 */
public class DataSourceInformationRepositoryTest {
    private DataSourceInformationRepository repository = new DataSourceInformationRepository();
    private static InMemoryDataSourceRepository inMemoryDataSourceRepository = new InMemoryDataSourceRepository();
    private static JNDIBasedDataSourceRepository jndiBasedDataSourceRepository = new JNDIBasedDataSourceRepository();
    private static Field repositoryListener;
    private static Field dataSourceInformationMap;
    private static Field secretResolver;
    private static Properties properties = new Properties();
    private static DataSourceInformation information = new DataSourceInformation();
    private static final String ALIAS = "testAlias";
    private static final String ERR_DATASOURCE = "notExistingDataSourceName";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Initializing reflection variables.
     *
     * @throws NoSuchFieldException
     */
    @BeforeClass
    public static void initialize() throws NoSuchFieldException {
        information.setAlias(ALIAS);
        repositoryListener = DataSourceInformationRepository.class.getDeclaredField("listener");
        repositoryListener.setAccessible(true);
        dataSourceInformationMap = DataSourceInformationRepository.class.getDeclaredField("dataSourceInformationMap");
        dataSourceInformationMap.setAccessible(true);
        secretResolver = DataSourceInformationRepository.class.getDeclaredField("secretResolver");
        secretResolver.setAccessible(true);
    }

    /**
     * Test set, get and remove methods of repository listener.
     */
    @Test
    public void testSetGetRemoveRepositoryListener() throws IllegalAccessException {
        DataSourceInformationRepositoryListener listener = new DataSourceRepositoryManager
                (inMemoryDataSourceRepository, jndiBasedDataSourceRepository);
        repository.setRepositoryListener(listener);
        Assert.assertNotNull("new repository listener should be added", repositoryListener.get(repository));
        thrown.expect(SynapseCommonsException.class);
        thrown.expectMessage("There is already a DataSourceInformationRepositoryListener associated with " +
                "'DataSourceInformationRepository");
        repository.setRepositoryListener(listener);
        DataSourceInformationRepositoryListener tempListener = repository.getRepositoryListener();
        Assert.assertEquals("received listener should be same as the inserted one", listener, tempListener);
        repository.removeRepositoryListener();
        Assert.assertNull("listener should be removed", repositoryListener.get(repository));
    }

    /**
     * Test setRepositoryListener with null parameter.
     */
    @Test
    public void testSetRepositoryListenerNull() {
        thrown.expect(SynapseCommonsException.class);
        thrown.expectMessage("Provided DataSourceInformationRepositoryListener instance is null");
        repository.setRepositoryListener(null);
    }

    /**
     * Test addDataSourceInformation with null parameter.
     */
    @Test
    public void testAddDataSourceInformationNull() {
        thrown.expectMessage("DataSource information is null");
        thrown.expect(SynapseCommonsException.class);
        repository.addDataSourceInformation(null);
    }

    /**
     * Test getDataSourceInformation with null parameter.
     */
    @Test
    public void testGetDataSourceInformationNull() {
        thrown.expect(SynapseCommonsException.class);
        thrown.expectMessage("Name of the datasource information instance to be returned is null");
        repository.getDataSourceInformation(null);
    }

    /**
     * Test add,get and remove datasource information.
     *
     * @throws IllegalAccessException
     */
    @Test
    public void testAddDataSourceInformation() throws IllegalAccessException {
        repository.addDataSourceInformation(information);
        Assert.assertEquals("datasource information should be added to the map",
                ((Map<String, DataSourceInformation>) dataSourceInformationMap.get(repository)).size(), 1);
        DataSourceInformation tempInfo = repository.getDataSourceInformation(ALIAS);
        Assert.assertEquals("receives datasource info should be same as the saved", information, tempInfo);
        tempInfo = repository.removeDataSourceInformation(ALIAS);
        Assert.assertEquals("datasource info should be removed", information, tempInfo);
    }

    /**
     * Test removeDataSourceInformation with null parameters.
     */
    @Test
    public void testRemoveDataSourceInformationNull() {
        thrown.expectMessage("Name of the datasource information instance to be removed is null");
        thrown.expect(SynapseCommonsException.class);
        repository.removeDataSourceInformation(null);
    }

    /**
     * Test removeDataSourceInformation with not existing datasource.
     */
    @Test
    public void testRemoveDataSourceInformationNotExist() {
        thrown.expect(SynapseCommonsException.class);
        thrown.expectMessage("There is no datasource information instance for given name :" + ERR_DATASOURCE);
        repository.removeDataSourceInformation(ERR_DATASOURCE);
    }

    /**
     * Test configure method.
     *
     * @throws IllegalAccessException
     */
    @Test
    public void testConfigure() throws IllegalAccessException {
        repository.configure(properties);
        Assert.assertNotNull("secretResolver should be instantiated", secretResolver.get(repository));
    }

    /**
     * Test getAllDataSourceInformation method.
     *
     * @throws IllegalAccessException
     */
    @Test
    public void testGetAllDataSourceInformation() throws IllegalAccessException {
        Assert.assertNotNull("non empty iterator should be returned", repository.getAllDataSourceInformation());
    }
}
