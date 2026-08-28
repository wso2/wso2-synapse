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
package org.apache.synapse.transport.http.access;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Constants used in the Access Class
 */
public class AccessConstants {

    private static final Log log = LogFactory.getLog(AccessConstants.class);

    /**
     * Pattern used to log - Default is COMBINED_PATTERN given below.
     */
    public static final String COMBINED_PATTERN =
            "%h %l %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-Agent}i\"";

    public static final String LOG_PATTERN =
            "%{X-Forwarded-For}i %h %l %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-Agent}i\"";

    /**
     * Common log pattern.
     */
    public static final String COMMON_PATTERN =
             "%h %l %u %t \"%r\" %s %b";

    /**
     * The set of month abbreviations for log messages.
     */
    public static final String MONTHS[] =
            {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
             "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    public static final String HTTP_REQUEST_TIME_MS = "http.request.time.ms";
    public static final String HTTP_RESPONSE_TIME_MS = "http.response.time.ms";

    /**
     * The directory in which log files are created.
     */
    public static String DIRECTORY = "repository/logs";

    /**
     * Gives the format of the date to be appended to the name of the access log file.
     */
    public static String FILE_FORMAT = "yyyy-MM-dd";

    /**
     * The prefix that is added to log file filenames.
     */
    public static String PREFIX = "http_access_";

    /**
     * The suffix that is added to log file filenames.
     */
    public static String SUFFIX = ".log";

    public static final String CONFIG_PATTERN = "access_log_pattern";

    public static final String CONFIG_DIRECTORY = "access_log_directory";

    public static final String CONFIG_PREFIX = "access_log_prefix";

    public static final String CONFIG_SUFFIX = "access_log_suffix";

    public static final String CONFIG_FILE_DATE_FORMAT = "access_log_file_date_format";

    public static final String CONFIG_ENABLE_LOGGING = "access_log_enable";

    private static final String CONFIG_V2_LOGGING = "access_log_v2";

    private static final String CONFIG_V2_QUEUE_SIZE = "access_log_v2_queue_size";

    private static final int DEFAULT_V2_QUEUE_SIZE = 2000;

    private static final boolean V2_LOGGING_ENABLED =
            AccessConfiguration.getInstance().getBooleanProperty(CONFIG_V2_LOGGING, Boolean.FALSE);

    public static String getLogPattern() {
        return AccessConfiguration.getInstance().getStringProperty(CONFIG_PATTERN, LOG_PATTERN);
    }

    public static String getSuffix() {
        return AccessConfiguration.getInstance().getStringProperty(CONFIG_SUFFIX, SUFFIX);
    }

    public static String getPrefix() {
        return AccessConfiguration.getInstance().getStringProperty(CONFIG_PREFIX, PREFIX);
    }

    public static String getFileDateFormat() {
        return AccessConfiguration.getInstance().getStringProperty(CONFIG_FILE_DATE_FORMAT, FILE_FORMAT);
    }

    public static String getDirectory() {
        return AccessConfiguration.getInstance().getStringProperty(CONFIG_DIRECTORY, DIRECTORY);
    }

    public static boolean isV2LoggingEnabled() {
        return V2_LOGGING_ENABLED;
    }

    public static int getV2QueueSize() {
        int size = AccessConfiguration.getInstance().getIntProperty(CONFIG_V2_QUEUE_SIZE, DEFAULT_V2_QUEUE_SIZE);
        if (size <= 0) {
            log.warn("Invalid value for " + CONFIG_V2_QUEUE_SIZE + ": " + size +
                    ". Using default value: " + DEFAULT_V2_QUEUE_SIZE);
            return DEFAULT_V2_QUEUE_SIZE;
        }
        return size;
    }

}