/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.resolvers;

import org.apache.synapse.commons.util.FilePropertyLoader;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;


public class FilePropertyResolverTest {

    private final String KEY = "testKey";
    private static final String FILE_SYNC_INTERVAL = "file.properties.sync.interval";

    @Before
    public void resetInstance() throws NoSuchFieldException, IllegalAccessException {
        Field instance = FilePropertyLoader.class.getDeclaredField("fileLoaderInstance");
        instance.setAccessible(true);
        instance.set(null, null);
        System.clearProperty(FILE_SYNC_INTERVAL);
        System.setProperty("properties.file.path", System.getProperty("user.dir")
                                                   + "/src/test/resources/file.properties");
    }

    @After
    public void resetPropertiesFile() throws IOException {
        writeContentToFile("testKey=testValue");
    }

    /**
     * Test file property resolve method
     */
    @Test
    public void testBasicResolve() {
        assertInitialResolvedValue();
    }

    /**
     * Test file property resolve method with the sync interval defined.
     * <p>
     * The key should be resolved to the updated values once tested after the defined interval.
     */
    @Test
    public void testResolveWithSyncIntervalDefined() throws IOException {
        System.setProperty(FILE_SYNC_INTERVAL, "1");
        //assert initial value
        assertInitialResolvedValue();

        //modify content
        writeContentToFile("testKey=testValue2");

        //Wait for more than the sync interval and resolve
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //ignore
        }
        FilePropertyLoader propertyLoader = FilePropertyLoader.getInstance();
        Awaitility.await().pollInterval(500, TimeUnit.MILLISECONDS).atMost(8, TimeUnit.SECONDS).
                until(() -> "testValue2".equals(propertyLoader.getValue(KEY)));
    }

    private void writeContentToFile(String s) throws IOException {
        Path path = Paths.get("src", "test", "resources", "file.properties");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(s);
        }
    }

    private void assertInitialResolvedValue() {
        FilePropertyLoader propertyLoader = FilePropertyLoader.getInstance();
        String filePropertyValue = propertyLoader.getValue(KEY);
        Assert.assertEquals("Couldn't resolve the file property variable", "testValue", filePropertyValue);
    }

}
