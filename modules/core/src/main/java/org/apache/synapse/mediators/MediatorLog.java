/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.util.logging.LoggingUtils;

/**
 * Concrete implementation of the {@link SynapseLog} interface appropriate
 * for usage in a mediator. Instances of this class should not be created
 * directly but by using the factory method
 * {@link AbstractMediator#getLog(org.apache.synapse.MessageContext)}.
 * <p>
 * Note that this is work in progress.
 * Please refer to https://issues.apache.org/jira/browse/SYNAPSE-374 for
 * more information.
 */
public class MediatorLog implements SynapseLog {
    private static final Log traceLog = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);
    
    private final Log defaultLog;
    private final boolean traceOn;
    private final MessageContext synCtx;
    
    // The definition of this constructor might change...
    public MediatorLog(Log defaultLog, boolean traceOn, MessageContext synCtx) {
        this.defaultLog = defaultLog;
        this.traceOn = traceOn;
        this.synCtx = synCtx;
    }

    public boolean isTraceOrDebugEnabled() {
        return traceOn || isDebugEnabled();
    }

    public boolean isDebugEnabled() {
    	// Returns true if either default or service log has debug enabled
    	if (defaultLog.isDebugEnabled()) {
    		return true;
    	}
    	if (synCtx.getServiceLog() != null && synCtx.getServiceLog().isDebugEnabled()) {
    		return true;
    	}
        return false;
    }    

    public boolean isTraceEnabled() {
        if (defaultLog.isTraceEnabled()) {
            return true;
        }
        if (synCtx.getServiceLog() != null && synCtx.getServiceLog().isTraceEnabled()) {
            return true;
        }
        return false;
    }    
    
    /**
     * Log a message to the default log at level DEBUG and and to the trace log
     * at level INFO if trace is enabled for the mediator.
     */
    public void traceOrDebug(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        defaultLog.debug(formattedMsg);
        if (traceOn) {
            traceLog.info(formattedMsg);
        }
    }

    /**
     * Log a message at level WARN to the default log, if level DEBUG is enabled,
     * and to the trace log, if trace is enabled for the mediator.
     */
    public void traceOrDebugWarn(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        if (traceOn) {
            traceLog.warn(formattedMsg);
        }
        if (defaultLog.isDebugEnabled()) {
            defaultLog.warn(formattedMsg);
        }
    }
    
    public boolean isTraceTraceEnabled() {
        return traceOn && traceLog.isTraceEnabled();
    }

    /**
     * Log a message to the trace log at level TRACE if trace is enabled for the mediator.
     */
    public void traceTrace(Object msg) {
        if (traceOn) {
            traceLog.trace(LoggingUtils.getFormattedLog(synCtx, msg));
        }
    }

    /**
     * Log a message at level INFO to all available/enabled logs.
     */
    public void auditLog(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        defaultLog.info(formattedMsg);
        if (synCtx.getServiceLog() != null) {
            synCtx.getServiceLog().info(msg);
        }
        if (traceOn) {
            traceLog.info(formattedMsg);
        }
    }

    /**
     * Log a message at level DEBUG to all available/enabled logs.
     */
    public void auditDebug(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        defaultLog.debug(formattedMsg);
        if (synCtx.getServiceLog() != null && synCtx.getServiceLog().isDebugEnabled()) {
            synCtx.getServiceLog().debug(msg);
        }
        if (traceOn) {
            traceLog.debug(formattedMsg);
        }
    }

    /**
     * Log a message at level TRACE to all available/enabled logs.
     */
    public void auditTrace(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        if (defaultLog.isTraceEnabled()) {
            defaultLog.trace(formattedMsg);
        }
        if (synCtx.getServiceLog() != null && synCtx.getServiceLog().isTraceEnabled()) {
            synCtx.getServiceLog().trace(msg);
        }
        if (traceOn) {
            traceLog.trace(formattedMsg);
        }
    }

    /**
     * Log a message at level WARN to all available/enabled logs.
     */
    public void auditWarn(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        defaultLog.warn(formattedMsg);
        if (synCtx.getServiceLog() != null) {
            synCtx.getServiceLog().warn(msg);
        }
        if (traceOn) {
            traceLog.warn(formattedMsg);
        }
    }

    /**
     * Log a message at level ERROR to all available/enabled logs.
     */
    public void auditError(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        defaultLog.error(formattedMsg);
        if (synCtx.getServiceLog() != null) {
            synCtx.getServiceLog().error(msg);
        }
        if (traceOn) {
            traceLog.error(formattedMsg);
        }
    }

    /**
     * Log a message at level FATAL to all available/enabled logs.
     */
    public void auditFatal(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        defaultLog.fatal(formattedMsg);
        if (synCtx.getServiceLog() != null) {
            synCtx.getServiceLog().fatal(msg);
        }
        if (traceOn) {
            traceLog.fatal(formattedMsg);
        }
    }

    /**
     * Log a message at level ERROR to the default log and to the trace, if trace is enabled.
     */
    public void error(Object msg) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        defaultLog.error(formattedMsg);
        if (traceOn) {
            traceLog.error(formattedMsg);
        }
    }

    /**
     * Log a message at level ERROR to the default log, the service log and the trace, if trace
     * is enabled.
     */
    public void logSynapseException(String msg, Throwable cause) {

        String formattedMsg = LoggingUtils.getFormattedLog(synCtx, msg);
        defaultLog.error(formattedMsg, cause);
        if (synCtx.getServiceLog() != null) {
            synCtx.getServiceLog().error(msg, cause);
        }
        if (traceOn) {
            traceLog.error(formattedMsg, cause);
        }
    }
}
