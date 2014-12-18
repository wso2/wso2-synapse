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

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.transport.base.AbstractPollTableEntry;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.synapse.commons.vfs.VFSConstants;
import org.apache.synapse.commons.vfs.VFSUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * Holds information about an entry in the VFS transport poll table used by the
 * VFS Transport Listener
 */
public class PollTableEntry extends AbstractPollTableEntry {

    // operation after scan
    public static final int DELETE = 0;
    public static final int MOVE   = 1;

    /** File or Directory to scan */
    private String fileURI;
    /** The URI to send replies to. May be null. */
    private String replyFileURI;
    /** file name pattern for a directory or compressed file entry */
    private String fileNamePattern;
    /** Content-Type to use for the message */
    private String contentType;

    /** action to take after a successful poll */
    private int actionAfterProcess = DELETE;
    /** action to take after a poll with errors */
    private int actionAfterErrors = DELETE;
    /** action to take after a failed poll */
    private int actionAfterFailure = DELETE;

    /** where to move the file after processing */
    private String moveAfterProcess;
    /** where to move the file after encountering some errors */
    private String moveAfterErrors;
    /** where to move the file after total failure */
    private String moveAfterFailure;
    /** moved file will have this formatted timestamp prefix */    
    private DateFormat moveTimestampFormat;

    private boolean streaming;

    private int maxRetryCount;
    private long reconnectTimeout;
    private boolean fileLocking;

    private String moveAfterMoveFailure;

    private int nextRetryDurationForFailedMove;

    private String failedRecordFileName;

    private String failedRecordFileDestination;

    private String failedRecordTimestampFormat;

    private Integer fileProcessingInterval;
    
    private Integer fileProcessingCount;

    private Map<String, String> vfsSchemeProperties;
    private boolean autoLockRelease;

    private Long autoLockReleaseInterval;

    private Boolean autoLockReleaseSameNode;   
    
    private boolean distributedLock;
    
    private Long distributedLockTimeout;
    
    private static final Log log = LogFactory.getLog(PollTableEntry.class);
    
    public PollTableEntry(boolean fileLocking) {
        this.fileLocking = fileLocking;
    }

    @Override
    public EndpointReference[] getEndpointReferences(AxisService service, String ip) {
        return new EndpointReference[] { new EndpointReference("vfs:" + fileURI) };
    }

    public String getFileURI() {
        return fileURI;
    }

    public String getReplyFileURI() {
        return replyFileURI;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public String getContentType() {
        return contentType;
    }

    public int getActionAfterProcess() {
        return actionAfterProcess;
    }

    public int getActionAfterErrors() {
        return actionAfterErrors;
    }

    public int getActionAfterFailure() {
        return actionAfterFailure;
    }

    public String getMoveAfterProcess() {
        return moveAfterProcess;
    }

    public String getMoveAfterMoveFailure() {
        return moveAfterMoveFailure;
    }

    public int getNextRetryDuration() {
        return nextRetryDurationForFailedMove;
    }

    public String getFailedRecordFileName() {
        return failedRecordFileName;
    }

    public String getFailedRecordFileDestination() {
        return failedRecordFileDestination;
    }

    public String getFailedRecordTimestampFormat() {
        return failedRecordTimestampFormat;
    }

    public Integer getFileProcessingInterval() {
		return fileProcessingInterval;
	}

	public Integer getFileProcessingCount() {
		return fileProcessingCount;
	}

	private void setMoveAfterProcess(String moveAfterProcess) {
        if (moveAfterProcess == null) {
            this.moveAfterProcess = null;
        } else if (moveAfterProcess.startsWith(VFSConstants.VFS_PREFIX)) {
            // to recover a good directory location if user entered with the vfs: prefix
            // because transport uris are given like that
            this.moveAfterProcess = moveAfterProcess.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.moveAfterProcess = moveAfterProcess;
        }
    }

    public String getMoveAfterErrors() {
        return moveAfterErrors;
    }

    private void setMoveAfterErrors(String moveAfterErrors) {
        if (moveAfterErrors == null) {
            this.moveAfterErrors = null;
        } else if (moveAfterErrors.startsWith(VFSConstants.VFS_PREFIX)) {
            this.moveAfterErrors = moveAfterErrors.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.moveAfterErrors = moveAfterErrors;
        }  
    }

    public String getMoveAfterFailure() {
        return moveAfterFailure;
    }

    private void setMoveAfterFailure(String moveAfterFailure) {
        if (moveAfterFailure == null) {
            this.moveAfterFailure = null;
        } else if (moveAfterFailure.startsWith(VFSConstants.VFS_PREFIX)) {
            this.moveAfterFailure = moveAfterFailure.substring(VFSConstants.VFS_PREFIX.length());
        } else {
            this.moveAfterFailure = moveAfterFailure;
        }
    }

    public boolean isStreaming() {
        return streaming;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public boolean isFileLockingEnabled() {
        return fileLocking;
    }

    public long getReconnectTimeout() {
        return reconnectTimeout;
    }

    public DateFormat getMoveTimestampFormat() {
        return moveTimestampFormat;
    }

    /**
     * @return the autoLockRelease
     */
    public Boolean getAutoLockRelease() {
        return autoLockRelease;
    }

    /**
     * @param autoLockRelease the autoLockRelease to set
     */
    public void setAutoLockRelease(Boolean autoLockRelease) {
        this.autoLockRelease = autoLockRelease;
    }

    /**
     * @return the autoLockReleaseInterval
     */
    public Long getAutoLockReleaseInterval() {
        return autoLockReleaseInterval;
    }

    /**
     * @param autoLockReleaseInterval the autoLockReleaseInterval to set
     */
    public void setAutoLockReleaseInterval(Long autoLockReleaseInterval) {
        this.autoLockReleaseInterval = autoLockReleaseInterval;
    }

    /**
     * @return the autoLockReleaseSameNode
     */
    public Boolean getAutoLockReleaseSameNode() {
        return autoLockReleaseSameNode;
    }

    /**
     * @param autoLockReleaseSameNode the autoLockReleaseSameNode to set
     */
    public void setAutoLockReleaseSameNode(Boolean autoLockReleaseSameNode) {
        this.autoLockReleaseSameNode = autoLockReleaseSameNode;
    }

    
    
    /**
     * @return the distributedLock
     */
    public boolean isDistributedLock() {
        return distributedLock;
    }

    /**
     * @return the distributedLockTimeout
     */
    public Long getDistributedLockTimeout() {
        return distributedLockTimeout;
    }

    public Map<String, String> getVfsSchemeProperties() {
        return vfsSchemeProperties;
    }

    public void setVfsSchemeProperties(Map<String, String> vfsSchemeProperties) {
        this.vfsSchemeProperties = vfsSchemeProperties;
    }

    @Override
    public boolean loadConfiguration(ParameterInclude params) throws AxisFault {
        
        fileURI = ParamUtils.getOptionalParam(params, VFSConstants.TRANSPORT_FILE_FILE_URI);
        if (fileURI == null) {
        	log.warn("transport.vfs.FileURI parameter is missing in the proxy service configuration");
            return false;
        } else {
            
            if (fileURI.startsWith(VFSConstants.VFS_PREFIX)) {
                fileURI = fileURI.substring(VFSConstants.VFS_PREFIX.length());
            }
            
            replyFileURI = ParamUtils.getOptionalParam(params, VFSConstants.REPLY_FILE_URI);
            fileNamePattern = ParamUtils.getOptionalParam(params,
                    VFSConstants.TRANSPORT_FILE_FILE_NAME_PATTERN);

            contentType = ParamUtils.getRequiredParam(params,
                    VFSConstants.TRANSPORT_FILE_CONTENT_TYPE);
            String option = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_PROCESS);
            actionAfterProcess = VFSTransportListener.MOVE.equals(option) ?
                    PollTableEntry.MOVE : PollTableEntry.DELETE;

            option = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_ERRORS);
            actionAfterErrors = VFSTransportListener.MOVE.equals(option) ?
                    PollTableEntry.MOVE : PollTableEntry.DELETE;

            option = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_ACTION_AFTER_FAILURE);
            actionAfterFailure = VFSTransportListener.MOVE.equals(option) ?
                    PollTableEntry.MOVE : PollTableEntry.DELETE;

            String moveDirectoryAfterProcess = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_PROCESS);
            setMoveAfterProcess(moveDirectoryAfterProcess);

            String moveDirectoryAfterErrors = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_ERRORS);
            setMoveAfterErrors(moveDirectoryAfterErrors);

            String moveDirectoryAfterFailure = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_MOVE_AFTER_FAILURE);
            setMoveAfterFailure(moveDirectoryAfterFailure);

            String moveFileTimestampFormat = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_MOVE_TIMESTAMP_FORMAT);
            if(moveFileTimestampFormat != null) {
                moveTimestampFormat = new SimpleDateFormat(moveFileTimestampFormat);
            }

            setVfsSchemeProperties(VFSUtils.parseSchemeFileOptions(fileURI, params));

            String strStreaming = ParamUtils.getOptionalParam(params, VFSConstants.STREAMING);
            if (strStreaming != null) {
                streaming = Boolean.parseBoolean(strStreaming);
            }
            
            String strMaxRetryCount = ParamUtils.getOptionalParam(
                    params, VFSConstants.MAX_RETRY_COUNT);
            maxRetryCount = strMaxRetryCount != null ? Integer.parseInt(strMaxRetryCount) :
                    VFSConstants.DEFAULT_MAX_RETRY_COUNT;

            String strReconnectTimeout = ParamUtils.getOptionalParam(
                    params, VFSConstants.RECONNECT_TIMEOUT);
            reconnectTimeout = strReconnectTimeout != null ?
                    Integer.parseInt(strReconnectTimeout) * 1000 :
                    VFSConstants.DEFAULT_RECONNECT_TIMEOUT;

            String strFileLocking = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FILE_LOCKING);
            if (VFSConstants.TRANSPORT_FILE_LOCKING_ENABLED.equals(strFileLocking)) {
                fileLocking = true;
            } else if (VFSConstants.TRANSPORT_FILE_LOCKING_DISABLED.equals(strFileLocking)) {
                fileLocking = false;
            }

            moveAfterMoveFailure = ParamUtils.getOptionalParam(params,
                    VFSConstants.TRANSPORT_FILE_MOVE_AFTER_FAILED_MOVE);

            String nextRetryDuration = ParamUtils.getOptionalParam(
                    params, VFSConstants.TRANSPORT_FAILED_RECORD_NEXT_RETRY_DURATION);
            nextRetryDurationForFailedMove = nextRetryDuration != null ? Integer.parseInt(nextRetryDuration) :
                    VFSConstants.DEFAULT_NEXT_RETRY_DURATION;

            failedRecordFileName = ParamUtils.getOptionalParam(params,
                    VFSConstants.TRANSPORT_FAILED_RECORDS_FILE_NAME);
            if (failedRecordFileName == null) {
                failedRecordFileName = VFSConstants.DEFAULT_FAILED_RECORDS_FILE_NAME;
            }

            failedRecordFileDestination = ParamUtils.getOptionalParam(params,
                    VFSConstants.TRANSPORT_FAILED_RECORDS_FILE_DESTINATION);

            if (failedRecordFileDestination == null) {
                failedRecordFileDestination = VFSConstants.DEFAULT_FAILED_RECORDS_FILE_DESTINATION;
            }

            failedRecordTimestampFormat = ParamUtils.getOptionalParam(params,
                    VFSConstants.TRANSPORT_FAILED_RECORD_TIMESTAMP_FORMAT);
            if (failedRecordTimestampFormat == null) {
                failedRecordTimestampFormat =
                        VFSConstants.DEFAULT_TRANSPORT_FAILED_RECORD_TIMESTAMP_FORMAT;
            }

            String strFileProcessingInterval = ParamUtils.getOptionalParam(params, VFSConstants.TRANSPORT_FILE_INTERVAL);
            fileProcessingInterval = null;
            if (strFileProcessingInterval != null) {
            	try{
            		fileProcessingInterval = Integer.parseInt(strFileProcessingInterval);
            	}catch(NumberFormatException nfe){
            		log.warn("VFS File Processing Interval not set correctly. Current value is : " + strFileProcessingInterval , nfe);
            	}
            }  
            
            String strFileProcessingCount = ParamUtils.getOptionalParam(params, VFSConstants.TRANSPORT_FILE_COUNT);
            fileProcessingCount = null;
            if (strFileProcessingCount != null) {
            	try{
            		fileProcessingCount = Integer.parseInt(strFileProcessingCount);
            	}catch(NumberFormatException nfe){
            		log.warn("VFS File Processing Count not set correctly. Current value is : " + strFileProcessingCount , nfe);
            	}
            }                        
            
            String strAutoLock = ParamUtils.getOptionalParam(params,
                    VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE);
            autoLockRelease = false;
            autoLockReleaseSameNode = true;
            autoLockReleaseInterval = null;
            if (strAutoLock != null) {
                try {
                    autoLockRelease = Boolean.parseBoolean(strAutoLock);
                } catch (Exception e) {
                    autoLockRelease = false;
                    log.warn("VFS Auto lock removal not set properly. Current value is : "
                            + strAutoLock, e);
                }
                if (autoLockRelease) {
                    String strAutoLockInterval = ParamUtils.getOptionalParam(params,
                            VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE_INTERVAL);
                    if (strAutoLockInterval != null) {
                        try {
                            autoLockReleaseInterval = Long.parseLong(strAutoLockInterval);
                        } catch (Exception e) {
                            autoLockReleaseInterval = null;
                            log.warn(
                                    "VFS Auto lock removal property not set properly. Current value is : "
                                            + strAutoLockInterval, e);
                        }
                    }
                    String strAutoLockReleaseSameNode = ParamUtils.getOptionalParam(params,
                            VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE_SAME_NODE);
                    if (strAutoLockReleaseSameNode != null) {
                        try {
                            autoLockReleaseSameNode = Boolean
                                    .parseBoolean(strAutoLockReleaseSameNode);
                        } catch (Exception e) {
                            autoLockReleaseSameNode = true;
                            log.warn(
                                    "VFS Auto lock removal property not set properly. Current value is : "
                                            + autoLockReleaseSameNode, e);
                        }
                    }
                }

            }            
            
            distributedLock = false;
            distributedLockTimeout = null;
            String strDistributedLock = ParamUtils.getOptionalParam(params, VFSConstants.TRANSPORT_DISTRIBUTED_LOCK);
            if(strDistributedLock != null){
                try {
                    distributedLock = Boolean.parseBoolean(strDistributedLock);
                } catch (Exception e) {
                    autoLockRelease = false;
                    log.warn("VFS Distributed lock not set properly. Current value is : " + strDistributedLock, e);
                }            
                
                if(distributedLock){                
                    String strDistributedLockTimeout = ParamUtils.getOptionalParam(params, VFSConstants.TRANSPORT_DISTRIBUTED_LOCK_TIMEOUT);
                    if (strDistributedLockTimeout != null) {
                        try {
                            distributedLockTimeout = Long.parseLong(strDistributedLockTimeout);
                        } catch (Exception e) {
                            distributedLockTimeout = null;
                            log.warn(
                                    "VFS Distributed lock timeout property not set properly. Current value is : "
                                            + strDistributedLockTimeout, e);
                        }
                    }                
                }
                
            }            
            return super.loadConfiguration(params);
        }
    }
}
