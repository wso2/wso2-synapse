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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class MockFile extends AbstractFileObject implements FileObject {

    private byte[] buff = { 't', 'e', 's', 't' };

    protected MockFile(AbstractFileName name, AbstractFileSystem fs) {
        super(name, fs);
    }

    protected FileType doGetType() throws Exception {
        if(getName().getPath().endsWith(".fail")) {
            return FileType.IMAGINARY;
        }
        if(getName().getPath().endsWith(".lock")) {
            return FileType.IMAGINARY;
        }
        return getName().getType();
    }

    protected String[] doListChildren() throws Exception {
        if(getType() == FileType.FOLDER) {
            String[] children = new String[1];
            for (int i = 0; i < children.length; i++) {
                children[i] = "t-" + i + "-" + System.currentTimeMillis() + ".txt";
            }

            return children;
        }
        return new String[0];
    }

    protected FileObject[] doListChildrenResolved() throws Exception {
        String[] children = doListChildren();
        FileObject[] result = new FileObject[children.length];
        for (int i = 0; i < children.length; i++) {
            String name = getName().getPath();
            if(!name.endsWith("/")) {
                name += "/";
            }
            FileObject fo = getFileSystem().resolveFile(name + children[i]);
            result[i] = fo;
        }
        return result;
    }

    protected long doGetContentSize() throws Exception {
        return buff.length;
    }

    protected InputStream doGetInputStream() throws Exception {
        return new ByteArrayInputStream(buff);
    }

    protected void doDelete() throws Exception {
        //Done
    }
}
