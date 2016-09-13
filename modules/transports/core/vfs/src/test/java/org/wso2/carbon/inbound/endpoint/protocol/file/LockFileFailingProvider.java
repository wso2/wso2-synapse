/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.inbound.endpoint.protocol.file;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class LockFileFailingProvider extends AbstractOriginatingFileProvider {

    public static final Collection<Capability> capabilities;

    public LockFileFailingProvider() {
        this.setFileNameParser(new MockFileNameParser());
    }

    protected FileSystem doCreateFileSystem(FileName name, FileSystemOptions fileSystemOptions)
            throws FileSystemException {
        MockFileName rootName = (MockFileName) name;
        return new MockFileSystem(rootName, rootName.getRootFile(), fileSystemOptions);
    }

    public Collection<Capability> getCapabilities() {
        return capabilities;
    }

    static {
        capabilities = Collections.unmodifiableCollection(Arrays.asList(
                new Capability[] { Capability.CREATE, Capability.DELETE, Capability.RENAME, Capability.GET_TYPE,
                        Capability.GET_LAST_MODIFIED, Capability.SET_LAST_MODIFIED_FILE,
                        Capability.SET_LAST_MODIFIED_FOLDER, Capability.LIST_CHILDREN, Capability.READ_CONTENT,
                        Capability.URI, Capability.WRITE_CONTENT, Capability.APPEND_CONTENT,
                        Capability.RANDOM_ACCESS_READ, Capability.RANDOM_ACCESS_WRITE }));
    }

}
