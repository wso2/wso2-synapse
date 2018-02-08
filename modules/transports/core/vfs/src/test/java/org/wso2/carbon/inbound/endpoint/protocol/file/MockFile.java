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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Mock file implementation
 */
public class MockFile extends AbstractFileObject {

    private static Log log = LogFactory.getLog(MockFile.class);
    private byte[] buff = { 't', 'e', 's', 't' };
    private ByteArrayOutputStream fileContent = new ByteArrayOutputStream();

    //indicate that lock file has been created
    private boolean lockFileCreated = false;

    protected MockFile(AbstractFileName name, AbstractFileSystem fs) {
        super(name, fs);
    }

    protected FileType doGetType() throws Exception {
        if(getName().getPath().endsWith(".fail")) {
            return FileType.IMAGINARY;
        }

        if(getName().getPath().endsWith(".lock") && !lockFileCreated) {
            return FileType.IMAGINARY;
        }

        //If file name ends with file separator, it is mocked as folder
        if(getName().getPath().endsWith(File.separator)) {
            return FileType.FOLDER;
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
        return new ByteArrayInputStream(fileContent.toByteArray());
    }

    @Override
    public InputStream getInputStream() throws FileSystemException {
        return super.getInputStream();
    }

    @Override
    protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
        return fileContent;
    }

    protected void doDelete() throws Exception {
        //Done
    }

    @Override
    public void createFile() throws FileSystemException {
        log.info("Mocking create file:" + getName());

        //if creating mock lock file
        if(getName().getPath().endsWith(".lock") && !lockFileCreated) {
            lockFileCreated = true;
        }
    }

    @Override
    public FileContent getContent() throws FileSystemException {
        return super.getContent();
    }

    @Override
    protected void doRename(FileObject newfile) throws Exception {
        super.doRename(newfile);
        /*MockFile newMockFile = (MockFile) FileObjectUtils.getAbstractFileObject(newfile);
        MockFileHolder.getInstance().addFile(newfile.getName().getURI().split("\\?")[0], newMockFile);*/
    }

    @Override
    public boolean delete() throws FileSystemException {
        if (super.delete()) {
            //delete the file from mock file holder
            return MockFileHolder.getInstance().deleteMockFile(this.getName().getURI().split("\\?")[0]);
        }
        return false;
    }
}
