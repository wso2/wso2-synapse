/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.messageflowtracer.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.messageflowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.messageflowtracer.data.MessageFlowComponentId;
import org.apache.synapse.messageflowtracer.util.MessageFlowTracerConstants;

/**
 * Tracing Data Collector class which has a blocking queue to store collected data temporarily until been analyzed
 */
public class MessageFlowTracingDataCollector {

    private static final Log log = LogFactory.getLog(MessageFlowTracingDataCollector.class.getName());
    private static boolean isMessageFlowTracingEnabled;

    public static void init() {
        isMessageFlowTracingEnabled = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue
                (MessageFlowTracerConstants.MESSAGE_FLOW_TRACE_ENABLED, MessageFlowTracerConstants
                        .DEFAULT_TRACE_ENABLED));
    }

    /**
     * Collect component information from mediation and store it as a MessageFlowComponentEntry instance in the queue
     *
     * @param synCtx        Synapse Message Context
     * @param componentId   Unique ID for the Component/Mediator
     * @param componentName Name od the component/Mediator
     */
    private static void addComponentInfoEntry(MessageContext synCtx, String componentId, String componentName) {
        if (synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID) != null) {
            synCtx.getEnvironment().getMessageDataCollector().enQueue(
                    new MessageFlowComponentEntry(synCtx, componentId, componentName)
            );
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Message Flow ID is null, Component Entry Data will not be updated for " + synCtx.getMessageID
                        ());
            }
        }
    }

    /**
     * Update mediation tracing information
     *
     * @param msgCtx       Synapse message context
     * @param mediatorName Component/Mediator Name
     * @return String ID for the mediator/component generated
     */
    public static String setTraceFlowEvent(MessageContext msgCtx, String mediatorName) {
        String updatedComponentId = null;
        if (MessageFlowTracingDataCollector.isMessageFlowTracingEnabled(msgCtx)) {
            MessageFlowComponentId messageFlowComponentId =
                    (MessageFlowComponentId) (msgCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_INCREMENT_ID));
            updatedComponentId = messageFlowComponentId.getUpdatedId();
            addComponentInfoEntry(msgCtx, updatedComponentId, mediatorName);

            msgCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_PARENT, updatedComponentId);
        }
        return updatedComponentId;
    }

    /**
     * Set properties of the entry point for the mediation flow such as API, Proxy, Sequence etc.
     *
     * @param synCtx    Synapse Message Context
     * @param entryType Type of the entry (API/Proxy etc)
     * @param messageID Unique ID for the message
     */
    public static void setEntryPoint(MessageContext synCtx, String entryType, String messageID) {
        if (synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID) == null) {
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID, messageID.replace(":", "_"));
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE, entryType);
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_INCREMENT_ID,
                               new MessageFlowComponentId(MessageFlowTracerConstants.INITIAL_FLOW_INCREMENT_ID));
            synCtx.setMessageFlowTracingState(SynapseConstants.TRACING_ON);
        }
    }

    /**
     * States whether message flow tracing is enabled
     *
     * @return True if enabled, False otherwise
     */
    public static boolean isMessageFlowTracingEnabled() {
        return isMessageFlowTracingEnabled;
    }

    /**
     * States whether message flow tracing is enabled in given message context
     *
     * @param msgCtx Message Context to validate
     * @return True if enabled, False otherwise
     */
    public static boolean isMessageFlowTracingEnabled(MessageContext msgCtx) {
        return (msgCtx.getMessageFlowTracingState() == SynapseConstants.TRACING_ON);
    }
}
