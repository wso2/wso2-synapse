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

import org.apache.commons.logging.Log;
import org.apache.log4j.MDC;
import org.apache.synapse.commons.CorrelationConstants;

/**
 * Responsible for providing wrapper implementation for apache.commons.logging.Log interface. The logging wrapper
 * methods will put the correlation id to the MDC prior to the logging.
 */
public class CorrelationMDCAwareLogger implements Log {

    private final Object correlationId;
    private final Log log;

    public CorrelationMDCAwareLogger(Object correlationId, Log log) {

        this.log = log;
        this.correlationId = correlationId;
    }

    @Override
    public void trace(Object o) {

        addCorrelationIdToMDC();
        log.trace(o);
    }

    @Override
    public void trace(Object o, Throwable throwable) {

        addCorrelationIdToMDC();
        log.trace(o, throwable);
    }

    @Override
    public void debug(Object o) {

        addCorrelationIdToMDC();
        log.debug(o);
    }

    @Override
    public void debug(Object o, Throwable throwable) {

        addCorrelationIdToMDC();
        log.debug(o, throwable);
    }

    @Override
    public void info(Object o) {

        addCorrelationIdToMDC();
        log.info(o);
    }

    @Override
    public void info(Object o, Throwable throwable) {

        addCorrelationIdToMDC();
        log.info(o, throwable);
    }

    @Override
    public void warn(Object o) {

        addCorrelationIdToMDC();
        log.warn(o);
    }

    @Override
    public void warn(Object o, Throwable throwable) {

        addCorrelationIdToMDC();
        log.warn(o, throwable);
    }

    @Override
    public void error(Object o) {

        addCorrelationIdToMDC();
        log.error(o);
    }

    @Override
    public void error(Object o, Throwable throwable) {

        addCorrelationIdToMDC();
        log.error(o, throwable);
    }

    @Override
    public void fatal(Object o) {

        addCorrelationIdToMDC();
        log.fatal(o);
    }

    @Override
    public void fatal(Object o, Throwable throwable) {

        addCorrelationIdToMDC();
        log.fatal(o, throwable);
    }

    @Override
    public boolean isDebugEnabled() {

        return log.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {

        return log.isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled() {

        return log.isFatalEnabled();
    }

    @Override
    public boolean isInfoEnabled() {

        return log.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {

        return log.isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled() {

        return log.isWarnEnabled();
    }

    private void addCorrelationIdToMDC() {

        MDC.put(CorrelationConstants.CORRELATION_MDC_PROPERTY, correlationId);
    }

}
