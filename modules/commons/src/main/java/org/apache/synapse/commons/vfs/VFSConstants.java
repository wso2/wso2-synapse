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

package org.apache.synapse.commons.vfs;

public final class VFSConstants {
    
    // vfs transport prefix (e.g. used in an out EPR etc)
    public static final String VFS_PREFIX = "vfs:";

    public static final String TRANSPORT_FILE_ACTION_AFTER_PROCESS
            = "transport.vfs.ActionAfterProcess";
    public static final String TRANSPORT_FILE_ACTION_AFTER_ERRORS
            = "transport.vfs.ActionAfterErrors";
    public static final String TRANSPORT_FILE_ACTION_AFTER_FAILURE
            = "transport.vfs.ActionAfterFailure";

    public static final String TRANSPORT_FILE_MOVE_AFTER_PROCESS
            = "transport.vfs.MoveAfterProcess";
    public static final String TRANSPORT_FILE_MOVE_AFTER_ERRORS
            = "transport.vfs.MoveAfterErrors";
    public static final String TRANSPORT_FILE_MOVE_AFTER_FAILURE
            = "transport.vfs.MoveAfterFailure";

    public static final String TRANSPORT_FILE_FILE_URI = "transport.vfs.FileURI";
    public static final String TRANSPORT_FILE_FILE_NAME_PATTERN = "transport.vfs.FileNamePattern";
    public static final String TRANSPORT_FILE_CONTENT_TYPE = "transport.vfs.ContentType";
    public static final String TRANSPORT_FILE_LOCKING = "transport.vfs.Locking";
    public static final String TRANSPORT_FILE_LOCKING_ENABLED = "enable";    
    public static final String TRANSPORT_FILE_LOCKING_DISABLED = "disable";

    /**
     * This parameter is used decide whether the resolving hostname IP of URIs are done at deployment or dynamically.
     * At usage default id 'false' which lead hostname resolution at deployment
     */
    public static final String TRANSPORT_FILE_RESOLVEHOST_DYNAMICALLY = "transport.vfs.ResolveHostsDynamically";

    //Size limit of the files to be processed, defaults to -1 which means unlimited file size
    public static final String TRANSPORT_FILE_SIZE_LIMIT = "transport.vfs.FileSizeLimit";
    public static final double DEFAULT_TRANSPORT_FILE_SIZE_LIMIT = -1;

    public static final String REPLY_FILE_URI = "transport.vfs.ReplyFileURI";
    public static final String REPLY_FILE_NAME = "transport.vfs.ReplyFileName";

    public static final String TRANSPORT_FILE_MOVE_TIMESTAMP_FORMAT
            = "transport.vfs.MoveTimestampFormat";

    public static final String DEFAULT_RESPONSE_FILE = "response.xml";
    
    public static final String STREAMING = "transport.vfs.Streaming";
    
    public static final String MAX_RETRY_COUNT = "transport.vfs.MaxRetryCount";
    public static final String FORCE_CREATE_FOLDER = "transport.vfs.CreateFolder";
    public static final String RECONNECT_TIMEOUT = "transport.vfs.ReconnectTimeout";
    public static final String APPEND = "transport.vfs.Append";
    public static final String SUBFOLDER_TIMESTAMP = "transport.vfs.SubFolderTimestampFormat";

    /**
     * parameter to set synchronous uploading of the files for a file protocol on host basis
     */
    public static final String TRANSPORT_FILE_SEND_FILE_LOCKING = "transport.vfs.SendFileSynchronously";

    /**
     * File Sorting Parameter and values
     * */
    public static final String FILE_SORT_PARAM = "transport.vfs.FileSortAttribute";
    public static final String FILE_SORT_VALUE_NAME = "Name";
    public static final String FILE_SORT_VALUE_SIZE = "Size";
    public static final String FILE_SORT_VALUE_LASTMODIFIEDTIMESTAMP = "Lastmodifiedtimestamp";
    public static final String FILE_SORT_ORDER = "transport.vfs.FileSortAscending";
    
    
    /**
    * If a rename/move operation failed, we'll keep track of a list in a text file and this will
     * provide the ability to configure that property file name
     */
    public static final String TRANSPORT_FAILED_RECORDS_FILE_NAME =
            "transport.vfs.FailedRecordsFileName";

    /**
     * The default name of the failed file
     */
    public static final String DEFAULT_FAILED_RECORDS_FILE_NAME =
            "vfs-move-failed-records.properties";

    /**
     * The location to place the property file that contains the failed files
     */
    public static final String TRANSPORT_FAILED_RECORDS_FILE_DESTINATION =
            "transport.vfs.FailedRecordsFileDestination";

    /**
     * The default location to place the record file
     */
    public static final String DEFAULT_FAILED_RECORDS_FILE_DESTINATION = "repository/conf/";

    /**
     * Once a record/file is marked as failed we'll try again to move the file after this amount
     * of milliseconds
     */
    public static final String TRANSPORT_FAILED_RECORD_NEXT_RETRY_DURATION =
            "transport.vfs.FailedRecordNextRetryDuration";

    public static final int DEFAULT_NEXT_RETRY_DURATION = 3000; // 3 seconds

    /**
     * This specify the folder to which a move failed file should be moved to ( in next successful)
     * attempt
     */
    public static final String TRANSPORT_FILE_MOVE_AFTER_FAILED_MOVE =
            "transport.vfs.MoveAfterFailedMove";

    public static final String TRANSPORT_FAILED_RECORD_TIMESTAMP_FORMAT =
            "transport.vfs.MoveFailedRecordTimestampFormat";

    public static final String DEFAULT_TRANSPORT_FAILED_RECORD_TIMESTAMP_FORMAT =
            "dd/MM/yyyy/ HH:mm:ss";

    /**
     * This specify the interval between files processed (in milliseconds)
     * */
    public static final String TRANSPORT_FILE_INTERVAL = "transport.vfs.FileProcessInterval";
    /**
     * This specify the file count that will be processed in a cycle/batch 
     * */    
    public static final String TRANSPORT_FILE_COUNT = "transport.vfs.FileProcessCount";
    
    /**
     * Lock release true/false
     * */
    public static final String TRANSPORT_AUTO_LOCK_RELEASE = "transport.vfs.AutoLockRelease";

    /**
     * Lock release interval
     * */
    public static final String TRANSPORT_AUTO_LOCK_RELEASE_INTERVAL = "transport.vfs.AutoLockReleaseInterval";

    /**
     * Lock release same node
     * */
    public static final String TRANSPORT_AUTO_LOCK_RELEASE_SAME_NODE = "transport.vfs.LockReleaseSameNode";  

    /**
     * Distributed Lock enable/disable
     * */
    public static final String TRANSPORT_DISTRIBUTED_LOCK = "transport.vfs.DistributedLock";

    /**
     * Fully build the file
     * */
    public static final String TRANSPORT_BUILD = "transport.vfs.Build";
    
    /**
     * Distributed Lock timeout
     * */
    public static final String TRANSPORT_DISTRIBUTED_LOCK_TIMEOUT = "transport.vfs.DistributedTimeout";     
    
    public static final String FAILED_RECORD_DELIMITER = " ";

    public static final int DEFAULT_MAX_RETRY_COUNT = 3;
    public static final long DEFAULT_RECONNECT_TIMEOUT = 30000;
    
    // transport header property names used by the VFS transport
    public static final String FILE_PATH = "FILE_PATH";
    public static final String FILE_URI = "FILE_URI";
    public static final String FILE_NAME = "FILE_NAME";
    public static final String FILE_LENGTH = "FILE_LENGTH";
    public static final String LAST_MODIFIED = "LAST_MODIFIED";

    // identifies the scheme in use, i.e. file, ftp, sftp, http etc..
    public static final String SCHEME = "VFS_SCHEME";

    // sftp parameter prefix
    public static final String SFTP_PREFIX = "transport.vfs.SFTP";
    // sftp scheme
    public static final String SCHEME_SFTP = "sftp";
    public static final String SCHEME_FTP = "ftp";
    public static final String SCHEME_FTPS = "ftps";
    // sftp scheme file option list
    public static enum SFTP_FILE_OPTION {Identities, UserDirIsRoot, IdentityPassPhrase};

    public static final String FILE_TYPE_PREFIX = "transport.vfs.fileType";
    public static final String FILE_TYPE = "filetype";
    public static final String BINARY_TYPE = "BINARY";
    public static final String LOCAL_TYPE = "LOCAL";
    public static final String ASCII_TYPE = "ASCII";
    public static final String EBCDIC_TYPE = "EBCDIC";

    public static final String CLUSTER_AWARE = "transport.vfs.ClusterAware";

    // The waiting time of the file before picking it up (in milliseconds)
    public static final String WAIT_TIME_BEFORE_READ = "transport.vfs.waitTimeBeforeRead";
}
