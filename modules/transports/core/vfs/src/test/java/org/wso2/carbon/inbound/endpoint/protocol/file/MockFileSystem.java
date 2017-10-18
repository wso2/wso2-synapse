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

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;

import java.util.Collection;

public class MockFileSystem extends AbstractFileSystem implements FileSystem {

    private final String rootFile;

    public MockFileSystem(FileName rootName, String rootFile, FileSystemOptions opts) {
        super(rootName, (FileObject) null, opts);
        this.rootFile = rootFile;
    }

    protected FileObject createFile(AbstractFileName name) throws FileSystemException {
        MockFile file =  new MockFile(name, this);
        MockFileHolder.getInstance().addFile(name.getURI().split("\\?")[0], file);
        return file;
    }

    protected void addCapabilities(Collection<Capability> caps) {
        caps.addAll(DefaultLocalFileProvider.capabilities);
    }

}
