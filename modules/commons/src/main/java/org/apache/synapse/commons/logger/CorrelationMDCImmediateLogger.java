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
import org.apache.logging.log4j.ThreadContext;
import org.apache.synapse.commons.CorrelationConstants;

/**
 * Responsible for providing wrapper implementation for CorrelationMDCAwareLogger.
 * The logging wrapper methods will remove the correlation id from the MDC after logging from CorrelationMDCAwareLogger.
 */
public class CorrelationMDCImmediateLogger extends CorrelationMDCAwareLogger {

    public CorrelationMDCImmediateLogger(String correlationId, Log log) {

        super(correlationId, log);
    }

    private void removeCorrelationIdFromMDC() {

        ThreadContext.remove(CorrelationConstants.CORRELATION_MDC_PROPERTY);
    }

    @Override
    public void trace(Object o) {

        super.trace(o);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void trace(Object o, Throwable throwable) {

        super.trace(o, throwable);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void debug(Object o) {

        super.debug(o);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void debug(Object o, Throwable throwable) {

        super.debug(o, throwable);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void info(Object o) {

        super.info(o);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void info(Object o, Throwable throwable) {

        super.info(o, throwable);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void warn(Object o) {

        super.warn(o);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void warn(Object o, Throwable throwable) {

        super.warn(o, throwable);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void error(Object o) {

        super.error(o);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void error(Object o, Throwable throwable) {

        super.error(o, throwable);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void fatal(Object o) {

        super.fatal(o);
        removeCorrelationIdFromMDC();
    }

    @Override
    public void fatal(Object o, Throwable throwable) {

        super.fatal(o, throwable);
        removeCorrelationIdFromMDC();
    }
}
