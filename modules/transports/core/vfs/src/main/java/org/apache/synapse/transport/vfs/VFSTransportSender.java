/*
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.transport.vfs;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.format.BinaryFormatter;
import org.apache.axis2.format.PlainTextFormatter;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.base.*;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.synapse.commons.vfs.VFSConstants;
import org.apache.synapse.commons.vfs.VFSUtils;
import org.apache.synapse.commons.vfs.VFSOutTransportInfo ;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * axis2.xml - transport definition
 *  <transportSender name="file" class="org.apache.synapse.transport.vfs.VFSTransportSender">
 *      <parameter name="transport.vfs.Locking">enable|disable</parameter> ?
 *  </transportSender>
 */
public class VFSTransportSender extends AbstractTransportSender implements ManagementSupport {

    public static final String TRANSPORT_NAME = "vfs";

    /** The VFS file system manager */
    private FileSystemManager fsManager = null;

    /**
     * By default file locking in VFS transport is turned on at a global level
     *
     * NOTE: DO NOT USE THIS FLAG, USE PollTableEntry#isFileLockingEnabled() TO CHECK WHETHR
     * FILE LOCKING IS ENABLED
     */
    private boolean globalFileLockingFlag = true;

    /**
     * Map to hold lock object for each host per service when operating in synchronous write mode
     */
    private static final ConcurrentHashMap<String,WriteLockObject> lockingObjects = new ConcurrentHashMap<>();

    /**
     * The public constructor
     */
    public VFSTransportSender() {
        log = LogFactory.getLog(VFSTransportSender.class);
    }

    /**
     * Initialize the VFS file system manager and be ready to send messages
     * @param cfgCtx the axis2 configuration context
     * @param transportOut the transport-out description
     * @throws AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut)
            throws AxisFault {

        super.init(cfgCtx, transportOut);
        try {
            StandardFileSystemManager fsm = new StandardFileSystemManager();
            fsm.setConfiguration(getClass().getClassLoader().getResource("providers.xml"));
            fsm.init();
            fsManager = fsm;
            Parameter lckFlagParam = transportOut.getParameter(VFSConstants.TRANSPORT_FILE_LOCKING);
            if (lckFlagParam != null) {
                String strLockingFlag = lckFlagParam.getValue().toString();
                // by-default enabled, if explicitly specified as "disable" make it disable
                if (VFSConstants.TRANSPORT_FILE_LOCKING_DISABLED.equals(strLockingFlag)) {
                    globalFileLockingFlag = false;
                }
            }
        } catch (FileSystemException e) {
            handleException("Error initializing the file transport : " + e.getMessage(), e);
        }
    }

    /**
     * Send the given message over the VFS transport
     *
     * @param msgCtx the axis2 message context
     * @throws AxisFault on error
     */
    public void sendMessage(MessageContext msgCtx, String targetAddress,
                            OutTransportInfo outTransportInfo) throws AxisFault {

        if (waitForSynchronousResponse(msgCtx)) {
            throw new AxisFault("The VFS transport doesn't support synchronous responses. " +
                    "Please use the appropriate (out only) message exchange pattern.");
        }

        VFSOutTransportInfo vfsOutInfo = null;

        if (targetAddress != null) {
            vfsOutInfo = new VFSOutTransportInfo(targetAddress, globalFileLockingFlag);
        } else if (outTransportInfo != null && outTransportInfo instanceof VFSOutTransportInfo) {
            vfsOutInfo = (VFSOutTransportInfo) outTransportInfo;
        }

        WriteLockObject lockObject = null;
        String baseUri = null;
        if (vfsOutInfo != null) {
            if (vfsOutInfo.getSendFileSynchronously()) {
                baseUri = getBaseUri(targetAddress);
                if (baseUri != null) {
                    if (!lockingObjects.containsKey(baseUri)) {
                        lockingObjects.putIfAbsent(baseUri, new WriteLockObject());
                        log.debug("New locking object created for Synchronous write|MapSize:" + lockingObjects.size());
                    }
                    lockObject = lockingObjects.get(baseUri);
                    lockObject.incrementUsers();
                }
            }
        }

        try {
            if (lockObject == null) {
                writeFile(msgCtx, vfsOutInfo);
            } else {
                synchronized (lockObject) {
                    writeFile(msgCtx, vfsOutInfo);
                }
            }
        }catch (AxisFault axisFault){
            throw axisFault;
        }catch (Exception e){
            handleException("Exception occurred while sending output to the destination" + e.getMessage(), e);
        }finally {
            if(lockObject != null && (lockObject.decrementAndGetUsers() == 0)){
                lockingObjects.remove(baseUri);
                log.debug("locking object removed for after Synchronous write|MapSize:" + lockingObjects.size());
            }
        }
    }

    private void writeFile(MessageContext msgCtx, VFSOutTransportInfo vfsOutInfo) throws AxisFault {

        FileSystemOptions fso = null;
        try {
            fso = VFSUtils.attachFileSystemOptions(vfsOutInfo.getOutFileSystemOptionsMap(), fsManager);
        } catch (Exception e) {
            log.error("Error while attaching VFS file system properties. " + e.getMessage());
        }

        if (vfsOutInfo != null) {
            FileObject replyFile = null;
            try {
                
                boolean wasError = true;
                int retryCount = 0;
                int maxRetryCount = vfsOutInfo.getMaxRetryCount();
                long reconnectionTimeout = vfsOutInfo.getReconnectTimeout();
                boolean append = vfsOutInfo.isAppend();
                
                while (wasError) {
                    
                    try {
                        retryCount++;
                        replyFile = fsManager.resolveFile(vfsOutInfo.getOutFileURI(), fso);
                    
                        if (replyFile == null) {
                            log.error("replyFile is null");
                            throw new FileSystemException("replyFile is null");
                        }
                        wasError = false;
                                        
                    } catch (FileSystemException e) {
                        log.error("cannot resolve replyFile", e);
                        if(maxRetryCount <= retryCount) {
                            handleException("cannot resolve replyFile repeatedly: "
                                    + e.getMessage(), e);
                        }
                    }
                
                    if (wasError) {
                        try {
                            Thread.sleep(reconnectionTimeout);
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
                
                //If the reply folder does not exists create the folder structure
                if (vfsOutInfo.isForceCreateFolder()) {
                    String strPath = vfsOutInfo.getOutFileURI();
                    int iIndex = strPath.indexOf("?");
                    if(iIndex > -1){
                        strPath = strPath.substring(0, iIndex);
                    }
                    //Need to add a slash otherwise vfs consider this as a file
                    if(!strPath.endsWith("/") || !strPath.endsWith("\\")){
                        strPath += "/";
                    }
                    FileObject replyFolder = fsManager.resolveFile(strPath, fso);
                    if(!replyFolder.exists()){
                        replyFile.createFolder();
                    }
                }
                
                if (replyFile.exists()) {
                    if (replyFile.getType() == FileType.FOLDER) {
                        // we need to write a file containing the message to this folder
                        FileObject responseFile = fsManager.resolveFile(replyFile,
                                VFSUtils.getFileName(msgCtx, vfsOutInfo));

                        // if file locking is not disabled acquire the lock
                        // before uploading the file
                        if (vfsOutInfo.isFileLockingEnabled()) {
                            acquireLockForSending(responseFile, vfsOutInfo, fso);
                            if (!responseFile.exists()) {
                                responseFile.createFile();
                            }
                            populateResponseFile(responseFile, msgCtx,append, true, fso);
                            VFSUtils.releaseLock(fsManager, responseFile, fso);
                        } else {
                            if (!responseFile.exists()) {
                                responseFile.createFile();
                            }
                            populateResponseFile(responseFile, msgCtx,append, false, fso);
                        }

                    } else if (replyFile.getType() == FileType.FILE) {

                        // if file locking is not disabled acquire the lock
                        // before uploading the file
                        if (vfsOutInfo.isFileLockingEnabled()) {
                            acquireLockForSending(replyFile, vfsOutInfo, fso);
                            populateResponseFile(replyFile, msgCtx, append, true, fso);
                            VFSUtils.releaseLock(fsManager, replyFile, fso);
                        } else {
                            populateResponseFile(replyFile, msgCtx, append, false, fso);
                        }

                    } else {
                        handleException("Unsupported reply file type : " + replyFile.getType() +
                                " for file : " + VFSUtils.maskURLPassword(vfsOutInfo.getOutFileURI()));
                    }
                } else {
                    // if file locking is not disabled acquire the lock before uploading the file
                    if (vfsOutInfo.isFileLockingEnabled()) {
                        acquireLockForSending(replyFile, vfsOutInfo, fso);
                        replyFile.createFile();
                        populateResponseFile(replyFile, msgCtx, append, true, fso);
                        VFSUtils.releaseLock(fsManager, replyFile, fso);
                    } else {
                        replyFile.createFile();
                        populateResponseFile(replyFile, msgCtx, append, false, fso);
                    }
                }
            } catch (FileSystemException e) {
                handleException("Error resolving reply file : " +
                		VFSUtils.maskURLPassword(vfsOutInfo.getOutFileURI()), e);
            } finally {
                if (replyFile != null) {
                    try {/*
                        if (fsManager != null && replyFile.getParent() != null && replyFile.getParent().getFileSystem() != null) {
                            fsManager.closeFileSystem(replyFile.getParent().getFileSystem());
                        }*/
                        replyFile.close();
                    } catch (FileSystemException ignore) {}
                }
            }
        } else {
            handleException("Unable to determine out transport information to send message");
        }
    }

    private MessageFormatter getMessageFormatter(MessageContext msgContext){

        OMElement firstChild = msgContext.getEnvelope().getBody().getFirstElement();
        if (firstChild != null) {
            if (BaseConstants.DEFAULT_BINARY_WRAPPER.equals(firstChild.getQName())) {
                return new BinaryFormatter();
            } else if (BaseConstants.DEFAULT_TEXT_WRAPPER.equals(firstChild.getQName())) {
                return new PlainTextFormatter();
            }
        }
        try {
           return MessageProcessorSelector.getMessageFormatter(msgContext);
        } catch (AxisFault axisFault) {
            throw new BaseTransportException("Unable to get the message formatter to use");
        }

    }

    private void populateResponseFile(FileObject responseFile, MessageContext msgContext,
                                      boolean append, boolean lockingEnabled, FileSystemOptions fso) throws AxisFault {

        MessageFormatter messageFormatter = getMessageFormatter(msgContext);
        OMOutputFormat format = BaseUtils.getOMOutputFormat(msgContext);
        
        try {
            CountingOutputStream os = new CountingOutputStream(
                    responseFile.getContent().getOutputStream(append));
            try {
                messageFormatter.writeTo(msgContext, format, os, true);
            } finally {
                os.close();
            }
            
            // update metrics
            metrics.incrementMessagesSent(msgContext);
            metrics.incrementBytesSent(msgContext, os.getByteCount());
            
        } catch (FileSystemException e) {
            if (lockingEnabled) {
                VFSUtils.releaseLock(fsManager, responseFile, fso);
            }
            metrics.incrementFaultsSending();
            handleException("IO Error while creating response file : " + VFSUtils.maskURLPassword(responseFile.getName().getURI()), e);
        } catch (IOException e) {
            if (lockingEnabled) {
                VFSUtils.releaseLock(fsManager, responseFile, fso);
            }
            metrics.incrementFaultsSending();
            handleException("IO Error while creating response file : " + VFSUtils.maskURLPassword(responseFile.getName().getURI()), e);
        }
    }

    private void acquireLockForSending(FileObject responseFile, VFSOutTransportInfo vfsOutInfo, FileSystemOptions fso)
            throws AxisFault {
        
        int tryNum = 0;
        // wait till we get the lock
        while (!VFSUtils.acquireLock(fsManager, responseFile, fso)) {
            if (vfsOutInfo.getMaxRetryCount() == tryNum++) {
                handleException("Couldn't send the message to file : "
                        + VFSUtils.maskURLPassword(responseFile.getName().getURI()) + ", unable to acquire the " +
                        "lock even after " + tryNum + " retries");
            } else {

                log.warn("Couldn't get the lock for the file : "
                        + VFSUtils.maskURLPassword(responseFile.getName().getURI()) + ", retry : " + tryNum
                        + " scheduled after : " + vfsOutInfo.getReconnectTimeout());
                try {
                    Thread.sleep(vfsOutInfo.getReconnectTimeout());
                } catch (InterruptedException ignore) {}
            }
        }
    }

    /**
     * This method extracts base uri of vfs string.
     * @param targetAddress target address of the vfs connection
     * @return base uri for the vfs connection
     */
    private String getBaseUri(String targetAddress) {

        //Remove vfs part from the uri
        if(targetAddress.contains("vfs:"))
        {
            targetAddress = targetAddress.substring(targetAddress.indexOf("vfs:") + 4);
        }

        int index = targetAddress.indexOf("://");
        if (index > -1) {
            int endIndex = targetAddress.indexOf('/', index + 3);
            if (endIndex > -1) {
                return targetAddress.substring(0, endIndex);
            }
        }
        return null;
    }
}
