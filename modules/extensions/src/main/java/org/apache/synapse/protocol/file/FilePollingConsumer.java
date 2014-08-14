/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.protocol.file;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileNotFolderException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.inbound.InjectHandler;
import org.apache.synapse.inbound.PollingConsumer;

import org.apache.synapse.commons.vfs.VFSConstants; 
import org.apache.synapse.commons.vfs.VFSUtils;


public class FilePollingConsumer implements PollingConsumer {

    private static final Log log = LogFactory.getLog(FilePollingConsumer.class);
    private Properties vfsProperties;
    private boolean fileLock = true;

    private FileSystemManager fsManager = null;
    private String name;
    private SynapseEnvironment synapseEnvironment;
    private long scanInterval;
    private Long lastRanTime;
    private int lastCycle;
    private InjectHandler injectHandler;
    

    public FilePollingConsumer(Properties vfsProperties, String name, SynapseEnvironment synapseEnvironment, long scanInterval) {
    	this.vfsProperties = vfsProperties;
    	this.name = name; 
    	this.synapseEnvironment = synapseEnvironment;
    	this.scanInterval = scanInterval;
    	this.lastRanTime = null;
    	String strFileLock = vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_LOCKING);
    	if(strFileLock != null && strFileLock.toLowerCase().equals(VFSConstants.TRANSPORT_FILE_LOCKING_DISABLED)){
    		fileLock = false;
    	}
    	try{
            StandardFileSystemManager fsm = new StandardFileSystemManager();
            fsm.setConfiguration(getClass().getClassLoader().getResource("providers.xml"));
            fsm.init();
            fsManager = fsm;
    	}catch(Exception e){
        	log.error(e);
        	throw new RuntimeException(e);
        }
    }    
    
	public void registerHandler(InjectHandler injectHandler){
		this.injectHandler = injectHandler;
	}
    
    public void execute() {        
        try {
            if (log.isDebugEnabled()) {
                log.debug("Start : File Inbound EP : " + name);
            }
            //Check if the cycles are running in correct interval and start scan
            long currentTime = (new Date()).getTime();
            if(lastRanTime == null || ((lastRanTime + (scanInterval)) <= currentTime)){
            	lastRanTime = currentTime;
            	poll();
            }else if (log.isDebugEnabled()) {
            	log.debug("Skip cycle since cuncurrent rate is higher than the scan interval : VFS Inbound EP : " + name);
            }
            if (log.isDebugEnabled()) {
            	log.debug("End : File Inbound EP : " + name);
            }        	
        } catch (Exception e) {
            System.err.println("error in executing: It will no longer be run!");
            log.error("Error while reading file. " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }   
    
    /**
     * 
     * Do the file processing operation for the given set of properties
     * Do the checks and pass the control to processFile method
     * 
     * */    
    public FileObject poll(){
    	
    	String fileURI = vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_FILE_URI);
    	
    	if(fileURI == null || fileURI.trim().equals("")){
    		log.error("Invalid file url. Check the inbound endpoint configuration. Endpoint Name : " + name + ", File URL : " + fileURI);
    		return null;  
    	}
    	
        if (log.isDebugEnabled()) {
            log.debug("Start : Scanning directory or file : " + VFSUtils.maskURLPassword(fileURI));
        }
             
        //Check if the file/folder exists before proceeding and retrying
        boolean wasError = true;
        int retryCount = 0;
        int maxRetryCount = 0;
        if(vfsProperties.getProperty(VFSConstants.MAX_RETRY_COUNT) != null){
            try{            	
            	maxRetryCount = Integer.valueOf(vfsProperties.getProperty(VFSConstants.MAX_RETRY_COUNT));
            }catch(NumberFormatException e){
            	log.warn("Invalid values for Max Retry Count");
            	maxRetryCount = 0;
            }
        }
        long reconnectionTimeout = 1;
        if(vfsProperties.getProperty(VFSConstants.RECONNECT_TIMEOUT) != null){
            try{            	
            	reconnectionTimeout = Long.valueOf(vfsProperties.getProperty(VFSConstants.RECONNECT_TIMEOUT));
            }catch(NumberFormatException e){
            	log.warn("Invalid values for Reconnection Timeout");
            	reconnectionTimeout = 1;
            }
        }                
        FileObject fileObject = null;        
        while (wasError) {
            try {
                retryCount++;
                fileObject = fsManager.resolveFile(fileURI);
                if (fileObject == null) {
                    log.error("fileObject is null");
                    throw new FileSystemException("fileObject is null");
                }
                wasError = false;
            } catch (FileSystemException e) {
                if (retryCount >= maxRetryCount) {
                    log.error("Repeatedly failed to resolve the file URI: " + VFSUtils.maskURLPassword(fileURI), e);
                    return null;
                } else {
                    log.warn("Failed to resolve the file URI: " + VFSUtils.maskURLPassword(fileURI) + ", in attempt " + retryCount + ", " + e.getMessage() + " Retrying in " + reconnectionTimeout + " milliseconds.");
                }
            }
            if (wasError) {
                try {
                    Thread.sleep(reconnectionTimeout);
                } catch (InterruptedException e2) {
                    log.error("Thread was interrupted while waiting to reconnect.", e2);
                }
            }
        }    
        //If file/folder found proceed to the processing stage
        try {
        	lastCycle = 0;
            if (fileObject.exists() && fileObject.isReadable()) {         
                FileObject[] children = null;
                try {
                    children = fileObject.getChildren();
                } catch (FileNotFolderException ignored) {
                	if(log.isDebugEnabled()){
                		log.debug("No Folder found. Only file found on : " + VFSUtils.maskURLPassword(fileURI));
                	}
                } catch (FileSystemException ex) {
                    log.error(ex.getMessage(), ex);
                }

                // if this is a file that would translate to a single message
                if (children == null || children.length == 0) {
                	//Fail record is a one that is processed but was not moved or deleted due to an error.
                    boolean isFailedRecord = VFSUtils.isFailRecord(fsManager, fileObject);

                    if (fileObject.getType() == FileType.FILE && !isFailedRecord) {
                    	
                        if (!fileLock || (fileLock &&  VFSUtils.acquireLock(fsManager, fileObject))) {
                            try {
                                processFile(fileObject);  
                                lastCycle = 1;
                            } catch (AxisFault e) {
                            	lastCycle = 2;
                                log.error("Error processing File URI : " + fileObject.getName(), e);                             
                            }

                            try {
                                moveOrDeleteAfterProcessing(fileObject);
                            } catch (AxisFault axisFault) {
                            	lastCycle = 3;
                                log.error("File object '" + fileObject.getURL().toString() + "' " + "cloud not be moved", axisFault);                          
                                VFSUtils.markFailRecord(fsManager, fileObject);
                            }
                            if (fileLock) {
                                VFSUtils.releaseLock(fsManager, fileObject);
                                if (log.isDebugEnabled()) {
                                    log.debug("Removed the lock file '" + fileObject.toString() + ".lock' of the file '" + fileObject.toString());
                                }
                            }
                            if(injectHandler != null){
                            	return fileObject;
                            }                             
                        } else {
                            log.error("Couldn't get the lock for processing the file : " + fileObject.getName());
                        }
                    } else if (isFailedRecord) {
                        try {
                        	lastCycle = 2;
                            moveOrDeleteAfterProcessing(fileObject);
                        } catch (AxisFault axisFault) {
                            log.error("File object '" + fileObject.getURL().toString() + "' " + "cloud not be moved after first attempt", axisFault);                                                      
                        }                        	
                        if (fileLock) {
                            VFSUtils.releaseLock(fsManager, fileObject);
                        }                                     
                        if (log.isDebugEnabled()) {
                            log.debug("File '" + fileObject.getURL() + "' has been marked as a failed" + " record, it will not process");
                        }
                    }else{
                    	if(log.isDebugEnabled()){
                    		log.debug("Cannot find the file or failed file record. File : " + VFSUtils.maskURLPassword(fileURI));
                    	}
                    }

                } else {
                	//Process Directory
                	lastCycle = 0;
                    int failCount = 0;
                    int successCount = 0;
                    int processCount = 0;
                    Integer iFileProcessingInterval = null;
                    Integer iFileProcessingCount = null;
                    
                    if(vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_INTERVAL) != null){
                    	try{
                    		iFileProcessingInterval = Integer.valueOf(vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_INTERVAL));
                    	}catch(NumberFormatException e){
                    		log.warn("Invalid param value for transport.vfs.FileProcessInterval : " + vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_INTERVAL) + ". Expected numeric value.");
                    	}  
                    }
                    if(vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_COUNT) != null){
                    	try{
                    		iFileProcessingCount = Integer.valueOf(vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_COUNT));
                    	}catch(NumberFormatException e){
                    		log.warn("Invalid param value for transport.vfs.FileProcessCount : " + vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_COUNT) + ". Expected numeric value.");
                    	}
                    }                    
                    
                    if (log.isDebugEnabled()) {
                        log.debug("File name pattern : " + vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_FILE_NAME_PATTERN));
                    }
                    for (FileObject child : children) {
						// skipping *.lock file
						if (child.getName().getBaseName().endsWith(".lock") || child.getName().getBaseName().endsWith(".fail")) {
							continue;
						}
                        boolean isFailedRecord = VFSUtils.isFailRecord(fsManager, child);                        

                        String strFilePattern = vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_FILE_NAME_PATTERN);
                        
                        if((strFilePattern == null || child.getName().getBaseName().matches(strFilePattern)) && !isFailedRecord){
                            //child's file name matches the file name pattern or process all files
                            //now we try to get the lock and process
                            if (log.isDebugEnabled()) {
                                log.debug("Matching file : " + child.getName().getBaseName());
                            }
                            
                            if((!fileLock || (fileLock && VFSUtils.acquireLock(fsManager, child)))){
                                //process the file                            	
                                try {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Processing file :" + child);
                                    }
                                    processCount++;
                                    processFile(child);
                                    successCount++;
                                    // tell moveOrDeleteAfterProcessing() file was success
                                    lastCycle = 1;
                                } catch (Exception e) {
                                    log.error("Error processing File URI : " + child.getName(), e);
                                    failCount++;
                                    // tell moveOrDeleteAfterProcessing() file failed
                                    lastCycle = 2;;
                                }
                                //skipping un-locking file if failed to do delete/move after process
                                boolean skipUnlock = false;
                                try {
                                    moveOrDeleteAfterProcessing(child);
                                } catch (AxisFault axisFault) {
                                    log.error("File object '" + child.getURL().toString() + "'cloud not be moved, will remain in \"locked\" state", axisFault);
                                    skipUnlock = true;
                                    failCount++;
                                    lastCycle = 3;
                                    VFSUtils.markFailRecord(fsManager, child);
                                }
                                // if there is a failure or not we'll try to release the lock
                                if (fileLock && !skipUnlock) {
                                    VFSUtils.releaseLock(fsManager, child);
                                }
                                if(injectHandler == null){
                                	return child;
                                }                                
                            } 
                        }else if(log.isDebugEnabled() && strFilePattern != null && !child.getName().getBaseName().matches(strFilePattern) && !isFailedRecord){
                           	//child's file name does not match the file name pattern                           
                           	log.debug("Non-Matching file : " + child.getName().getBaseName());                                                    
                        } else if(isFailedRecord){                       	
                            //it is a failed record
                            try {
                            	lastCycle = 2;
                                moveOrDeleteAfterProcessing(child);
                            } catch (AxisFault axisFault) {
                                log.error("File object '" + child.getURL().toString() + "'cloud not be moved, will remain in \"fail\" state", axisFault);                                
                            }                        	                        	                            
                            if (fileLock) {
                            	VFSUtils.releaseLock(fsManager, child);
                                VFSUtils.releaseLock(fsManager, fileObject);
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("File '" + fileObject.getURL() + "' has been marked as a failed record, it will not " + "process");
                            }
                        }
                        
                        if(iFileProcessingInterval != null && iFileProcessingInterval > 0){
                        	try{
                                if (log.isDebugEnabled()) {
                                    log.debug("Put the VFS processor to sleep for : " + iFileProcessingInterval);
                                }                        		
                        		Thread.sleep(iFileProcessingInterval);
                        	}catch(InterruptedException ie){
                        		log.error("Unable to set the interval between file processors." + ie);
                        	}
                        }else if(iFileProcessingCount != null && iFileProcessingCount <= processCount){
                        	break;
                        }
                    }

                    if (failCount == 0 && successCount > 0) {
                        lastCycle = 1;
                    } else if (successCount == 0 && failCount > 0) {
                    	lastCycle = 4;
                    } else {
                    	lastCycle = 5;
                    }
                }
            } else {
				log.warn("Unable to access or read file or directory : " + VFSUtils.maskURLPassword(fileURI)+ "." + " Reason: " + (fileObject.exists()? (fileObject.isReadable()? "Unknown reason":"The file can not be read!"): "The file does not exists!"));
				return null;
            }
        } catch (FileSystemException e) {
            log.error("Error checking for existence and readability : " + VFSUtils.maskURLPassword(fileURI), e);
            return null;
        } catch (Exception e) {
            log.error("Error while processing the file/folder in URL : " + VFSUtils.maskURLPassword(fileURI), e);
            return null;               
        }finally{
        	try{
        		fsManager.closeFileSystem(fileObject.getParent().getFileSystem());
        		fileObject.close();
        	}catch(Exception e){
        		log.error("Unable to close the file system. " + e.getMessage());
        		log.error(e);
        	}
        }
        if (log.isDebugEnabled()) {
            log.debug("End : Scanning directory or file : " + VFSUtils.maskURLPassword(fileURI));
        }              
        return null;
    }

    /**
     * 
     * Actual processing of the file/folder
     * */
    private FileObject processFile(FileObject file) throws AxisFault {

        try {
            FileContent content = file.getContent();
            String fileName = file.getName().getBaseName();
            String filePath = file.getName().getPath();
            String fileURI = file.getName().getURI();

            Map<String, Object> transportHeaders = new HashMap<String, Object>();
            transportHeaders.put(VFSConstants.FILE_PATH, filePath);
            transportHeaders.put(VFSConstants.FILE_NAME, fileName);
            transportHeaders.put(VFSConstants.FILE_URI, fileURI);

            try {
                transportHeaders.put(VFSConstants.FILE_LENGTH, content.getSize());
                transportHeaders.put(VFSConstants.LAST_MODIFIED, content.getLastModifiedTime());
            } catch (FileSystemException ignore) {}
                                                              
            if(injectHandler != null){
            	//injectHandler
            	injectHandler.invoke(file);
            }else{
            	return file;
            }
                       
        } catch (FileSystemException e) {
            log.error("Error reading file content or attributes : " + file, e);            
        }
        return null; 
    }
    /**
     * 
     * Do the post processing actions
     * */
    private void moveOrDeleteAfterProcessing(FileObject fileObject) throws AxisFault {

        String moveToDirectoryURI = null;
        try {
            switch (lastCycle) {
                case 1:
                    if ("MOVE".equals(vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_ACTION_AFTER_PROCESS))) {
                        moveToDirectoryURI = vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_MOVE_AFTER_PROCESS);
                    }
                    break;

                case 2:
                    if ("MOVE".equals(vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_ACTION_AFTER_FAILURE))) {
                        moveToDirectoryURI = vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_MOVE_AFTER_FAILURE);
                    }
                    break;
                
                default:
                    return;
            }

            if (moveToDirectoryURI != null) {
                FileObject moveToDirectory = fsManager.resolveFile(moveToDirectoryURI);
                String prefix;
                if(vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_MOVE_TIMESTAMP_FORMAT) != null) {                	
                    prefix = new SimpleDateFormat(vfsProperties.getProperty(VFSConstants.TRANSPORT_FILE_MOVE_TIMESTAMP_FORMAT)).format(new Date());
                } else {
                    prefix = "";
                }
                FileObject dest = moveToDirectory.resolveFile(prefix + fileObject.getName().getBaseName());
                if (log.isDebugEnabled()) {
                    log.debug("Moving to file :" + dest.getName().getURI());
                }
                try {
                    fileObject.moveTo(dest);
                } catch (FileSystemException e) {
                    log.error("Error moving file : " + fileObject + " to " + moveToDirectoryURI, e);
                }
            } else {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Deleting file :" + fileObject);
                    }
                    fileObject.close();
                    if (!fileObject.delete()) {
                        String msg = "Cannot delete file : " + fileObject;
                        log.error(msg);
                        throw new AxisFault(msg);
                    }
                } catch (FileSystemException e) {
                    log.error("Error deleting file : " + fileObject, e);
                }
            }
        	if(VFSUtils.isFailRecord(fsManager, fileObject)){
        		VFSUtils.releaseFail(fsManager, fileObject);            
        	}
        } catch (FileSystemException e) {
        	if(!VFSUtils.isFailRecord(fsManager, fileObject)){
        		VFSUtils.markFailRecord(fsManager, fileObject);
            	log.error("Error resolving directory to move after processing : " + moveToDirectoryURI, e);
        	}
        }
    }    

} 