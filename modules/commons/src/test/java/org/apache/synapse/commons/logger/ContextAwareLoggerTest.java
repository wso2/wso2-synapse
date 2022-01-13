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

import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.ThreadContext;
import org.apache.synapse.commons.CorrelationConstants;

public class ContextAwareLoggerTest extends TestCase {

    private static final Log log = LogFactory.getLog(ContextAwareLoggerTest.class);
    private static final String correlationId = "testCorrelationId";

    public void testGetLoggerWhileCorrelationLogsDisabled() {

        //For axis2MessageContext
        MessageContext messageContext = new MessageContext();
        //when flag 'remove from mdc after logging' is disabled
        ContextAwareLogger.setCorrelationLoggingEnabled(true);
        Log contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, false);
        validateLoggerWhileCorrelationLogsDisabled(contextAwareLogger);

        //when flag 'remove from mdc after logging' is enabled
        contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, true);
        validateLoggerWhileCorrelationLogsDisabled(contextAwareLogger);

        //For HttpContext
        HttpContext httpContext = new BasicHttpContext();
        //when flag 'remove from mdc after logging' is disabled
        contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, false);
        validateLoggerWhileCorrelationLogsDisabled(contextAwareLogger);

        //when flag 'remove from mdc after logging' is enabled
        contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, true);
        validateLoggerWhileCorrelationLogsDisabled(contextAwareLogger);

        ContextAwareLogger.setCorrelationLoggingEnabled(false);
        contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, false);
        validateLoggerWhileCorrelationLogsDisabled(contextAwareLogger);
    }

    private void validateLoggerWhileCorrelationLogsDisabled(Log contextAwareLogger) {

        assertTrue("Same log object instance not returned", contextAwareLogger instanceof Log);
        assertFalse("Invalid logging wrapper returned",
                contextAwareLogger instanceof CorrelationMDCAwareLogger);
        assertFalse("Invalid logging wrapper returned",
                contextAwareLogger instanceof CorrelationMDCImmediateLogger);
    }

    private HttpContext getCorrelationIdAwareHTTPContext() {

        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(CorrelationConstants.CORRELATION_ID, correlationId);
        return httpContext;
    }

    private MessageContext getCorrelationIdAwareAxis2Context() {

        MessageContext messageContext = new MessageContext();
        messageContext.setProperty(CorrelationConstants.CORRELATION_ID, correlationId);
        return messageContext;
    }

    public void testGetMDCAwareLogger() {

        // Not removing from mdc after logging
        // Axis2 Message Context
        MessageContext messageContext = getCorrelationIdAwareAxis2Context();
        ContextAwareLogger.setCorrelationLoggingEnabled(true);
        Log contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, false);
        validateMDCAwareLogger(contextAwareLogger);

        // HTTPContext
        HttpContext httpContext = getCorrelationIdAwareHTTPContext();
        contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, false);
        validateMDCAwareLogger(contextAwareLogger);
    }

    public void testGetMDCImmediateLogger() {

        // Removing from mdc after logging
        // Axis2 MessageContext
        MessageContext messageContext = getCorrelationIdAwareAxis2Context();
        ContextAwareLogger.setCorrelationLoggingEnabled(true);
        Log contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, true);
        validateMDCImmediateLogger(contextAwareLogger);

        // HTTP Context
        HttpContext httpContext = getCorrelationIdAwareHTTPContext();
        contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, true);
        validateMDCImmediateLogger(contextAwareLogger);
    }

    private void validateMDCAwareLogger(Log contextAwareLogger) {

        assertFalse("Invalid logging wrapper returned",
                contextAwareLogger instanceof CorrelationMDCImmediateLogger);
        assertTrue("Not an instance of Log type", contextAwareLogger instanceof Log);
        assertTrue("Not an instance of CorrelationMDCAwareLogger type ",
                contextAwareLogger instanceof CorrelationMDCAwareLogger);
    }

    private void validateMDCImmediateLogger(Log contextAwareLogger) {

        assertTrue("Same log object instance not returned for axis2 msg context",
                contextAwareLogger instanceof Log);
        assertTrue("Invalid logging wrapper returned for axis2 msg context",
                contextAwareLogger instanceof CorrelationMDCAwareLogger);
        assertTrue("Invalid logging wrapper returned for axis2 msg context",
                contextAwareLogger instanceof CorrelationMDCImmediateLogger);
    }

    public void testLogInfoForMDCImmediateLogger() {

        String logMessage = "Testing INFO log For CorrelationMDCImmediateLogger";
        HttpContext httpContext = getCorrelationIdAwareHTTPContext();
        Log contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, true);
        contextAwareLogger.info(logMessage);
        validateMDCPropertyForMDCImmediateLogger();

        MessageContext messageContext = getCorrelationIdAwareAxis2Context();
        contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, true);
        contextAwareLogger.info(logMessage);
        validateMDCPropertyForMDCImmediateLogger();
    }

    public void testLogInfoForMDCAwareLogger() {

        String logMessage = "Testing INFO log For CorrelationMDCAwareLogger";
        HttpContext httpContext = getCorrelationIdAwareHTTPContext();
        Log contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, false);
        contextAwareLogger.info(logMessage);
        validateMDCPropertyForMDCAwareLogger();

        MessageContext messageContext = getCorrelationIdAwareAxis2Context();
        contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, false);
        contextAwareLogger.info(logMessage);
        validateMDCPropertyForMDCAwareLogger();
    }

    public void testLogErrorForMDCImmediateLogger() {

        String logMessage = "Testing ERROR log For CorrelationMDCImmediateLogger";
        HttpContext httpContext = getCorrelationIdAwareHTTPContext();
        Log contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, true);
        contextAwareLogger.info(logMessage);
        validateMDCPropertyForMDCImmediateLogger();

        MessageContext messageContext = getCorrelationIdAwareAxis2Context();
        contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, true);
        contextAwareLogger.error(logMessage);
        validateMDCPropertyForMDCImmediateLogger();
    }

    public void testLogErrorForMDCAwareLogger() {

        String logMessage = "Testing ERROR log For CorrelationMDCAwareLogger";
        HttpContext httpContext = getCorrelationIdAwareHTTPContext();
        Log contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, false);
        contextAwareLogger.info(logMessage);
        validateMDCPropertyForMDCAwareLogger();

        ThreadContext.remove(CorrelationConstants.CORRELATION_MDC_PROPERTY);
        MessageContext messageContext = getCorrelationIdAwareAxis2Context();
        contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, false);
        contextAwareLogger.error(logMessage);
        validateMDCPropertyForMDCAwareLogger();
    }

    public void testLogWarnForMDCImmediateLogger() {

        String logMessage = "Testing WARN log For CorrelationMDCImmediateLogger";
        HttpContext httpContext = getCorrelationIdAwareHTTPContext();
        Log contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, true);
        contextAwareLogger.info(logMessage);
        validateMDCPropertyForMDCImmediateLogger();

        MessageContext messageContext = getCorrelationIdAwareAxis2Context();
        contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, true);
        contextAwareLogger.warn(logMessage);
        validateMDCPropertyForMDCImmediateLogger();
    }

    public void testLogWarnForMDCAwareLogger() {

        String logMessage = "Testing WARN log For CorrelationMDCAwareLogger";
        HttpContext httpContext = getCorrelationIdAwareHTTPContext();
        ContextAwareLogger.setCorrelationLoggingEnabled(true);
        Log contextAwareLogger = ContextAwareLogger.getLogger(httpContext, log, false);
        contextAwareLogger.info(logMessage);
        validateMDCPropertyForMDCAwareLogger();

        ThreadContext.remove(CorrelationConstants.CORRELATION_MDC_PROPERTY);
        MessageContext messageContext = getCorrelationIdAwareAxis2Context();
        contextAwareLogger = ContextAwareLogger.getLogger(messageContext, log, false);
        contextAwareLogger.warn(logMessage);
        validateMDCPropertyForMDCAwareLogger();
    }

    private void validateMDCPropertyForMDCImmediateLogger() {

        Object correlationIdFromMDC = ThreadContext.get(CorrelationConstants.CORRELATION_MDC_PROPERTY);
        assertNull(correlationIdFromMDC);
    }

    private void validateMDCPropertyForMDCAwareLogger() {

        Object correlationIdFromMDC = ThreadContext.get(CorrelationConstants.CORRELATION_MDC_PROPERTY);
        assertNotNull(correlationIdFromMDC);
        assertEquals((String) correlationIdFromMDC, correlationId);
        ThreadContext.remove(CorrelationConstants.CORRELATION_MDC_PROPERTY);
    }
}
