/*
 * Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.commons.logger;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.commons.CorrelationConstants;

/**
 * This class provides the relevant context aware logging wrapper implementation according to the availability of
 * the correlation id in the provided context and based on the expected behavior of the MDC.
 */
public class ContextAwareLogger {

    private static Boolean correlationLoggingEnabled = false;

    static {
        String sysCorrelationStatus = System.getProperty(CorrelationConstants.CORRELATION_LOGS_SYS_PROPERTY);
        if (sysCorrelationStatus != null) {
            correlationLoggingEnabled = sysCorrelationStatus.equalsIgnoreCase("true");
        }
    }

    /**
     * Provides a wrapper implementation for the given logger according to the availability of the correlation id in
     * the axis2 and based on the expected behavior of the Correlation-ID MDC property.
     *
     * @param axis2MessageContext Axis2MessageContext
     * @param log                 Log
     * @param removeFromMDC       whether to remove the Correlation-ID property from MDC after logging
     * @return log wrapper
     */
    public static Log getLogger(MessageContext axis2MessageContext, Log log, boolean removeFromMDC) {

        return getLogger(axis2MessageContext.getProperty(CorrelationConstants.CORRELATION_ID), log, removeFromMDC);
    }

    /**
     * Provides a wrapper implementation for the given logger according to the availability of the correlation id in
     * the httpcontext and based on the expected behavior of the Correlation-ID MDC property.
     *
     * @param httpContext   HttpContext
     * @param log           Log
     * @param removeFromMDC whether to remove the Correlation-ID property from MDC after logging
     * @return log wrapper
     */
    public static Log getLogger(HttpContext httpContext, Log log, boolean removeFromMDC) {

        return getLogger(httpContext.getAttribute(CorrelationConstants.CORRELATION_ID), log, removeFromMDC);
    }

    private static Log getLogger(Object correlationId, Log log, boolean removeFromMDC) {

        if (correlationLoggingEnabled) {
            if (correlationId != null) {
                if (removeFromMDC) {
                    return new CorrelationMDCImmediateLogger(correlationId, log);
                }
                return new CorrelationMDCAwareLogger(correlationId, log);
            }
        }
        return log;
    }

    protected static void setCorrelationLoggingEnabled(Boolean correlationLoggingEnabled) {

        ContextAwareLogger.correlationLoggingEnabled = correlationLoggingEnabled;
    }
}
