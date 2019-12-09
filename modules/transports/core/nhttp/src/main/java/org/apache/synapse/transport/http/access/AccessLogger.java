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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.MiscellaneousUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that logs the Http Accesses to the access log files. Code segment borrowed from
 * Apache Tomcat's org.apache.catalina.valves.AccessLogValve with thanks.
 */
public class AccessLogger {

    private static final String ACCESS_LOG_PROPERTIES = "access-log.properties";

    //property name of nhttp log directory
    public static final String NHTTP_LOG_DIRECTORY = "nhttp.log.directory";

    public final static String ACCESS_LOG_ID = "org.apache.synapse.transport.nhttp.access";

    private final static String DATE_EXTRACT_REGEX = "\\[([^]]+)\\]";

    private final static String DATE_FORMAT_STRING = "dd/MMM/yyyy:HH:mm:ss Z";

    private static final String IS_LOG_ROTATABLE = "nhttp.is.log.rotatable";

    private static Log log = LogFactory.getLog(ACCESS_LOG_ID);

    public AccessLogger(final Log log) {
        super();
        this.initOpen();
        AccessLogger.log = log;
        buffered = true;
        checkExists = false;
    }

    /**
     * A date formatter to format a Date into a date in the given file format
     */
    protected SimpleDateFormat fileDateFormatter =
            new SimpleDateFormat(AccessConstants.getFileDateFormat());

    /**
     * The PrintWriter to which we are currently logging, if any.
     */
    protected PrintWriter writer;

    /**
     * The as-of date for the currently open log file, or a zero-length
     * string if there is no open log file.
     */
    private volatile String dateStamp = "";

    /**
     * Instant when the log daily rotation was last checked.
     */
    private volatile long rotationLastChecked = 0L;

    /**
     * Buffered logging.
     */
    private boolean buffered = true;

    /**
     * Do we check for log file existence? Helpful if an external
     * agent renames the log file so we can automatically recreate it.
     */
    private boolean checkExists = false;

    /**
     * The current log file we are writing to. Helpful when checkExists
     * is true.
     */
    protected File currentLogFile = null;

    /**
     * Can the log file be rotated.
     */
    private boolean isRotatable = getBooleanValue(IS_LOG_ROTATABLE, true);

    /**
     * Log the specified message to the log file, switching files if the date
     * has changed since the previous log call.
     *
     * @param message Message to be logged
     */
    public void log(String message) {
        if (isRotatable) {
            // Only do a logfile switch check once a second, max.
            long systime = System.currentTimeMillis();
            if ((systime - rotationLastChecked) > 1000) {
                synchronized (this) {
                    if ((systime - rotationLastChecked) > 1000) {
                        rotationLastChecked = systime;

                        String tsDate;
                        // Check for a change of date
                        tsDate = fileDateFormatter.format(new Date(systime));

                        // If the date has changed, switch log files
                        if (!dateStamp.equals(tsDate)) {
                            close();
                            dateStamp = tsDate;
                            open();
                        }
                    }
                }
            }
        }

        /* In case something external rotated the file instead */
        if (checkExists) {
            synchronized (this) {
                if (currentLogFile != null && !currentLogFile.exists()) {
                    try {
                        close();
                    } catch (Throwable e) {
                        handleThrowable(e);
                        log.info("Access Log file Close failed");
                    }

                    /* Make sure date is correct */
                    dateStamp = fileDateFormatter.format(
                            new Date(System.currentTimeMillis()));

                    open();
                }
            }
        }

        // Log this message
        synchronized (this) {
            if (writer != null) {
                writer.println(message);

                if (!buffered) {
                    writer.flush();
                }
            }
        }
    }

    /**
     * Get properties that tune access-log.properties. Preference to system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    private boolean getBooleanValue(String name, boolean def) {

        Properties props = MiscellaneousUtil.loadProperties(ACCESS_LOG_PROPERTIES);

        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (Boolean.parseBoolean(val)) {
            if (log.isDebugEnabled()) {
                log.debug("Using tuning parameter : " + name);
            }
            return true;
        } else if (val != null && !Boolean.parseBoolean(val)) {
            if (log.isDebugEnabled()) {
                log.debug("Using tuning parameter : " + name);
            }
            return false;
        }
        return def;
    }

    protected synchronized void initOpen() {
        /* Make sure date is correct */
        dateStamp = fileDateFormatter.format(
                new Date(System.currentTimeMillis()));
        this.open();
    }

    /**
     * Open the new log file for the date specified by <code>dateStamp</code>.
     */
    protected synchronized void open() {
        // Create the directory if necessary
        File dir;
        Properties synapseProps = MiscellaneousUtil.loadProperties(ACCESS_LOG_PROPERTIES);
        String nhttpLogDir =  synapseProps.getProperty(NHTTP_LOG_DIRECTORY);
        if (nhttpLogDir != null) {
            dir = new File(nhttpLogDir);
        } else {
            dir = new File(AccessConstants.getDirectory());
        }
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                log.error("Access Log Open Directory Failed");
            }
        }

        // Open the current log file
        try {
            Date today = new Date(System.currentTimeMillis());
            String pathname = dir.getAbsolutePath() + File.separator + AccessConstants.getPrefix()
                    + AccessConstants.getSuffix();
            String oldPathname;
            // If no rotate - no need for dateStamp in fileName
            if (isRotatable) {
                File existing = new File(pathname);
                // renaming existing file to a file with date
                if (existing.exists()) {
                    try {
                        // if file exists, read first line and take the timestamp
                        BufferedReader input = new BufferedReader(new FileReader(existing));
                        Pattern pattern = Pattern.compile(DATE_EXTRACT_REGEX);
                        String line = input.readLine();
                        if (StringUtils.isNotEmpty(line)) {
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                String date = matcher.group(1);
                                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING,Locale.ENGLISH);
                                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                                Date lastDateFromLog = dateFormat.parse(date);
                                // if same day - write to the existing file
                                if (!DateUtils.isSameDay(today, lastDateFromLog)) {
                                    // rename the file to log_[date].log, delete the existing file
                                    String oldDateStamp = fileDateFormatter.format(lastDateFromLog);
                                    oldPathname = dir.getAbsolutePath() + File.separator + AccessConstants.getPrefix()
                                            + oldDateStamp + AccessConstants.getSuffix();
                                    File oldFile = new File(oldPathname);
                                    existing.renameTo(oldFile);
                                    existing.delete();
                                }
                            } else {
                                // no date found from the regex
                                createFileInError(dir, existing);
                            }
                        }
                    } catch (ParseException e) {
                        log.error("Error occurred when parsing the date from existing log file", e);
                        createFileInError(dir, existing);
                    }
                }
            }
            writer = new PrintWriter(new BufferedWriter(new FileWriter(
                    pathname, true), 128000), true);
            currentLogFile = new File(pathname);
        } catch (IOException e) {
            log.warn("Unable to open the print writer", e);
            writer = null;
            currentLogFile = null;
        }
    }

    /**
     * If error happens, create a file with yesterday timestamp.
     *
     * @param dir      log file directory
     * @param existing existing log fil
     */
    private void createFileInError(File dir, File existing) {
        Date today = new Date(System.currentTimeMillis());
        Calendar calender = Calendar.getInstance();
        calender.setTime(today);
        calender.add(Calendar.DATE, -1);
        Date dayBefore = calender.getTime();
        String pathName = dir.getAbsolutePath() + File.separator + AccessConstants.getPrefix()
                + fileDateFormatter.format(dayBefore) + AccessConstants.getSuffix();
        File oldFile = new File(pathName);
        // file with yesterday timestamp exists
        if (oldFile.exists()) {
            pathName = dir.getAbsolutePath() + File.separator + AccessConstants.getPrefix()
                    + fileDateFormatter.format(dayBefore) + "_" + fileDateFormatter.format(today)
                    + AccessConstants.getSuffix();
            oldFile = new File(pathName);
        }
        existing.renameTo(oldFile);
        existing.delete();
    }

    /**
     * Close the currently open log file (if any)
     */
    synchronized void close() {
        if (writer == null) {
            return;
        }
        writer.flush();
        writer.close();
        writer = null;
        dateStamp = "";
        currentLogFile = null;
    }

    /**
     * Checks whether the supplied Throwable is one that needs to be
     * re-thrown and swallows all others.
     *
     * @param t the Throwable to check
     */
    public static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }
}
