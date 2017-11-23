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
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;
import org.apache.commons.vfs2.provider.local.GenericFileNameParser;

public class MockFileNameParser extends GenericFileNameParser {

    public FileName parseUri(VfsComponentContext context, FileName base, String filename) throws FileSystemException {
        StringBuilder name = new StringBuilder();
        String scheme = UriParser.extractScheme(filename.split("\\?")[0], name);
        if (scheme == null) {
            scheme = "test";
        }

        UriParser.canonicalizePath(name, 0, name.length(), this);
        UriParser.fixSeparators(name);
        String rootFile = this.extractRootPrefix(filename, name);
        FileType fileType = UriParser.normalisePath(name);
        String path = name.toString();
        return new MockFileName(scheme, "", path, fileType);
    }
}
