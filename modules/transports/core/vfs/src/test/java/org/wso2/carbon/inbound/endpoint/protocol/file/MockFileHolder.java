/*
 *     Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *     WSO2 Inc. licenses this file to you under the Apache License,
 *     Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.wso2.carbon.inbound.endpoint.protocol.file;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock files holder. This replicates a file system
 */
public class MockFileHolder {

    private static MockFileHolder mockFileHolder = new MockFileHolder();

    private ConcurrentHashMap<String, MockFile> mockFileMap = new ConcurrentHashMap<>();

    private MockFileHolder() {
    }

    /**
     * Get mockfile holder instance
     * @return
     */
    public static MockFileHolder getInstance() {
        return mockFileHolder;
    }

    /**
     * Add a mock file
     * @param uri file usri
     * @param mockFile mock file object
     */
    public void addFile(String uri, MockFile mockFile) {
        if (mockFile.getName().getType() == FileType.FOLDER && !uri.endsWith(File.separator)) {
            uri += File.separator;
        }
        mockFileMap.put(uri, mockFile);
    }

    /**
     * retrieve mock file for given uri
     * @param uri file uri
     * @return mock file object
     */
    public MockFile getFile(String uri) {
        return mockFileMap.get(uri);
    }

    /**
     * Remove mock file from the file holder
     * @param uri uri of the file
     * @return true if success, false otherwise
     */
    public boolean deleteMockFile(String uri) {
        if (mockFileMap.containsKey(uri)) {
            mockFileMap.remove(uri);
            return true;
        }
        return false;
    }

    /**
     * Clear all mock files from the holder
     */
    public void clear() {
        mockFileMap.clear();
    }

}
