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

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;

public class MockFileName extends AbstractFileName {
    private final String rootFile;

    protected MockFileName(String scheme, String rootFile, String path, FileType type) {
        super(scheme, path, type);
        this.rootFile = rootFile;
    }

    public String getRootFile() {
        return this.rootFile;
    }

    public FileName createName(String path, FileType type) {
        return new MockFileName(this.getScheme(), this.rootFile, path, type);
    }

    protected void appendRootUri(StringBuilder buffer, boolean addPassword) {
        buffer.append(this.getScheme());
        buffer.append("://");
        buffer.append(this.rootFile);
    }

}
