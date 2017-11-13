/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.commons.datasource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Unit tests for DataSourceRepositoryHolder class.
 */
public class DataSourceRepositoryHolderTest {

    private static DataSourceRepositoryHolder repositoryHolder;
    private static Field dataSourceInformationRepository;
    private static Field dataSourceRepositoryManager;
    private static DataSourceInformationRepository informationRepository = new DataSourceInformationRepository();

    /**
     * Initializing DataSourceRepositoryHolder and reflection variables.
     * @throws NoSuchFieldException
     */
    @BeforeClass
    public static void initialize() throws NoSuchFieldException {
        repositoryHolder = DataSourceRepositoryHolder.getInstance();
        dataSourceInformationRepository =
                DataSourceRepositoryHolder.class.getDeclaredField("dataSourceInformationRepository");
        dataSourceInformationRepository.setAccessible(true);
        dataSourceRepositoryManager =
                DataSourceRepositoryHolder.class.getDeclaredField("dataSourceRepositoryManager");
        dataSourceRepositoryManager.setAccessible(true);
        Properties properties = new Properties();
        repositoryHolder.init(informationRepository, properties);
    }

    /**
     * Test getInstance method.
     */
    @Test
    public void testGetInstance() {
        Assert.assertNotNull("must return an instance", DataSourceRepositoryHolder.getInstance());
    }

    /**
     * Test init method (called inside initialize method).
     */
    @Test
    public void testInit() {
        Assert.assertNotNull("new listener should be initialized by init method",
                informationRepository.getRepositoryListener());
    }

    /**
     * Test isInitialized method.
     */
    @Test
    public void testIsInitialized() {
        Assert.assertTrue("must return true since already initialized", repositoryHolder.isInitialized());
    }

    /**
     * Test getDataSourceInformationRepository method.
     * @throws IllegalAccessException
     */
    @Test
    public void testGetDataSourceInformationRepository() throws IllegalAccessException {
        Assert.assertEquals("must be equal to reflection variable",
                dataSourceInformationRepository.get(repositoryHolder),
                repositoryHolder.getDataSourceInformationRepository());
    }

    /**
     * Test getDataSourceRepositoryManager method.
     * @throws IllegalAccessException
     */
    @Test
    public void testGetDataSourceRepositoryManager() throws IllegalAccessException {
        Assert.assertEquals("must be equal to reflection variable",
                dataSourceRepositoryManager.get(repositoryHolder),
                repositoryHolder.getDataSourceRepositoryManager());
    }
}
