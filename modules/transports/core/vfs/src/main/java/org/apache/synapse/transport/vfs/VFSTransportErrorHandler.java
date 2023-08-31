/*
 * Copyright (c) 2023, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.transport.vfs;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;

/**
 * This class is used to handle errors in VFS transport
 * <p> We have not modified the original implementation, but we have included the error handling scenarios
 * from {@link VFSTransportListener}, {@link VFSTransportSender} and {@link PollTableEntry} in this class.</p>
 */
public class VFSTransportErrorHandler {

    /**
     * This method is used to log exceptions with exception
     * @param log Log
     * @param type {@link LogType}
     * @param message String message to be logged
     * @param e Exception
     */
    public static void logException(Log log, LogType type, String message, Exception e) {
        switch (type) {
            case INFO:
                log.info(message, e);
                break;
            case DEBUG:
                log.debug(message, e);
                break;
            case WARN:
                log.warn(message, e);
                break;
            case ERROR:
                log.error(message, e);
                break;
            case FATAL:
                log.fatal(message, e);
                break;
        }
    }

    /**
     * This method is used to log exceptions with exception
     * @param log Log
     * @param type {@link LogType}
     * @param message String message to be logged
     * @param configName String name of the configuration
     * @param e Exception
     */
    public static void logException(Log log, LogType type, String message, String configName, Exception e) {
        message = constructLogMessage(message, configName);
        logException(log, type, message, e);
    }

    /**
     * This method is used to log exceptions without exception
     * @param log Log
     * @param type {@link LogType}
     * @param message String message to be logged
     */
    public static void logException(Log log, LogType type, String message) {
        switch (type) {
            case INFO:
                log.info(message);
                break;
            case DEBUG:
                log.debug(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
            case FATAL:
                log.fatal(message);
                break;
        }
    }

    /**
     * This method is used to log exceptions without exception
     * @param log Log
     * @param type {@link LogType}
     * @param message String message to be logged
     * @param configName String name of the configuration
     */
    public static void logException(Log log, LogType type, String message, String configName) {
        message = constructLogMessage(message, configName);
        logException(log, type, message);
    }

    /**
     * This method is used to handle exceptions. Log error message and throws an AxisFault with the exception
     * @param log Log
     * @param message String message to be logged
     * @param e Exception
     * @throws AxisFault
     */
    public static void handleException(Log log, String message, Exception e) throws AxisFault {
        logException(log, LogType.ERROR, message, e);
        throw new AxisFault(message, e);
    }

    /**
     * This method is used to handle exceptions. Log error message and throws an AxisFault with the exception
     * @param log Log
     * @param message String message to be logged
     * @param configName String name of the configuration
     * @param e Exception
     * @throws AxisFault
     */
    public static void handleException(Log log, String message, String configName, Exception e) throws AxisFault {
        logException(log, LogType.ERROR, message, configName, e);
        throw new AxisFault(message, e);
    }

    /**
     * This method is used to handle exceptions. Log error message and throws an AxisFault
     * @param log Log
     * @param message String message to be logged
     * @throws AxisFault
     */
    public static void handleException(Log log, String message) throws AxisFault {
        logException(log, LogType.ERROR, message);
        throw new AxisFault(message);
    }

    /**
     * This method is used to handle exceptions. Log error message and throws an AxisFault
     * @param log Log
     * @param message String message to be logged
     * @param configName String name of the configuration
     * @throws AxisFault
     */
    public static void handleException(Log log, String message, String configName) throws AxisFault {
        logException(log, LogType.ERROR, message, configName);
        throw new AxisFault(message);
    }

    /**
     * This method is used to handle print the stack trace
     * @param e InterruptedException
     */
    public static void printStackTrace(Exception e) {
        e.printStackTrace();
    }

    /**
     * This method is used to throw a Runtime exception
     * @param e Exception
     */
    public static void throwException(RuntimeException e) {
        throw e;
    }

    /**
     * This enum is used to define the log type
     */
    public enum LogType {
        INFO,
        DEBUG,
        WARN,
        ERROR,
        FATAL
    }

    /**
     * This method is used to construct the log message
     * @param message String message to be logged
     * @param configName String name of the configuration
     * @return String constructed log message
     */
    public static String constructLogMessage(String message, String configName) {
        if (null == configName || configName.trim().isEmpty()) {
            return message;
        }
        return "[Service: ".concat(configName).concat("] - ").concat(message);
    }
}
