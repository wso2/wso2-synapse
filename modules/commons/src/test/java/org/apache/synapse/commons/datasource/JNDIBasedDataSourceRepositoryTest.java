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

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

/**
 * Unit tests for JNDIBasedDataSourceRepository class.
 */
public class JNDIBasedDataSourceRepositoryTest {
    private static JNDIBasedDataSourceRepository jndiBasedDataSourceRepository = new JNDIBasedDataSourceRepository();
    private static Field jndiProperties;
    private static Field cachedNameList;
    private static DataSourceInformation information = new DataSourceInformation();
    private static Properties properties = new Properties();
    private static SecretInformation secretInformation = new SecretInformation();
    private static final String DATASOURCE_NAME = "testDatasource";
    private static final String ERROR_DATASOURCE_NAME = "notExisting";
    private static final String TYPE = "testType";
    private static final String DRIVER = "testDriver";
    private static final String URL = "testUrl";
    private static final String USER = "testUser";
    private static final String PASSWORD = "testPassword";
    private final String CLASS_NAME = "testClassName";
    private final String FACTORY = "testFactory";
    private final String NAME = "testName";
    private static String url = "";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Initializing JNDIBasedDataSourceRepository.
     *
     * @throws NoSuchFieldException
     * @throws UnknownHostException
     */
    @BeforeClass()
    public static void initialization() throws NoSuchFieldException, UnknownHostException {
        jndiProperties = JNDIBasedDataSourceRepository.class.getDeclaredField("jndiProperties");
        jndiProperties.setAccessible(true);
        cachedNameList = JNDIBasedDataSourceRepository.class.getDeclaredField("cachedNameList");
        cachedNameList.setAccessible(true);
        String providerHost = "localhost";
        InetAddress addr = InetAddress.getLocalHost();
        if (addr != null) {
            String hostname = addr.getHostName();
            if (hostname == null) {
                String ipAddr = addr.getHostAddress();
                if (ipAddr != null) {
                    providerHost = ipAddr;
                }
            } else {
                providerHost = hostname;
            }
        }
        Properties jndiEnv = new Properties();
        jndiEnv.setProperty(DataSourceConstants.PROP_SYNAPSE_PREFIX_DS, "SampleDataSource");
        url = "rmi://" + providerHost + ":" + String.valueOf(DataSourceConstants
                .DEFAULT_PROVIDER_PORT);
        jndiBasedDataSourceRepository.init(jndiEnv);
        information.setDatasourceName(DATASOURCE_NAME);
        information.setProperties(properties);
        information.setType(TYPE);
        information.setDriver(DRIVER);
        information.setUrl(URL);
        secretInformation.setUser(USER);
        secretInformation.setAliasSecret(PASSWORD);
        information.setSecretInformation(secretInformation);
    }

    /**
     * Testing init method.
     *
     * @throws UnknownHostException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    @Test
    public void testInit() throws UnknownHostException, IllegalAccessException, NoSuchFieldException {
        final String key1 = "java.naming.provider.url";
        final String key2 = "java.naming.factory.initial";
        final String value2 = "com.sun.jndi.rmi.registry.RegistryContextFactory";
        Assert.assertEquals("Asserting property 1 inserted by method", ((Properties) jndiProperties.get
                (jndiBasedDataSourceRepository)).getProperty(key1), url);
        Assert.assertEquals("Asserting property 2 inserted by method", ((Properties) jndiProperties.get
                (jndiBasedDataSourceRepository)).getProperty(key2), value2);
    }

    /**
     * Testing register and un-register methods.
     *
     * @throws IllegalAccessException
     */
    @Test
    public void testRegisterUnRegister() throws IllegalAccessException {
        thrown.expectMessage("Unsupported data source type : " + TYPE);
        thrown.expect(SynapseCommonsException.class);
        jndiBasedDataSourceRepository.register(information);

        information.setType(DataSourceInformation.PER_USER_POOL_DATA_SOURCE);
        information.addParameter(DataSourceConstants.PROP_CPDS_ADAPTER +
                DataSourceConstants.DOT_STRING + DataSourceConstants.PROP_CPDS_CLASS_NAME, CLASS_NAME);
        information.addParameter(DataSourceConstants.PROP_CPDS_ADAPTER +
                DataSourceConstants.DOT_STRING + DataSourceConstants.PROP_CPDS_FACTORY, FACTORY);
        information.addParameter(DataSourceConstants.PROP_CPDS_ADAPTER +
                DataSourceConstants.DOT_STRING + DataSourceConstants.PROP_CPDS_NAME, NAME);
        jndiBasedDataSourceRepository.register(information);
        Assert.assertEquals("method should insert datasource to cache",
                ((List<String>) cachedNameList.get(jndiBasedDataSourceRepository)).get(0), DATASOURCE_NAME);

        information.setType(DataSourceInformation.BASIC_DATA_SOURCE);
        jndiBasedDataSourceRepository.register(information);
        Assert.assertEquals("cache size should be incremented to 2",
                ((List<String>) cachedNameList.get(jndiBasedDataSourceRepository)).size(), 2);

        jndiBasedDataSourceRepository.unRegister(DATASOURCE_NAME);
        Assert.assertEquals("cache size should be decremented after un-registering",
                ((List<String>) cachedNameList.get(jndiBasedDataSourceRepository)).size(), 1);
    }

    /**
     * Testing lookup method with erroneous input.
     */
    @Test
    public void testLookup() {
        Assert.assertNull("lookup non-existing datasource should return null",
                jndiBasedDataSourceRepository.lookUp(ERROR_DATASOURCE_NAME));
    }

    /**
     * Test clear method.
     *
     * @throws IllegalAccessException
     */
    @AfterClass
    public static void testClear() throws IllegalAccessException {
        jndiBasedDataSourceRepository.clear();
        Assert.assertEquals("cache should be empty after clearing",
                ((List<String>) cachedNameList.get(jndiBasedDataSourceRepository)).size(), 0);
    }
}
