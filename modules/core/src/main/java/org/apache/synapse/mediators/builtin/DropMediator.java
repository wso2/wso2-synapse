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

package org.apache.synapse.mediators.builtin;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.flowtracer.MessageFlowDataHolder;
import org.apache.synapse.flowtracer.MessageFlowDbConnector;
import org.apache.synapse.aspects.statistics.StatisticsReporter;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.UUID;

/**
 * Halts further processing/mediation of the current message. i.e. returns false
 */
public class DropMediator extends AbstractMediator {

    /**
     * Halts further mediation of the current message by returning false.
     *
     * @param synCtx the current message
     * @return false always
     */
    public boolean mediate(MessageContext synCtx) {

         SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Drop mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        String mediatorId = null;
//        if(MessageFlowDataHolder.isMessageFlowTraceEnable()) {
//            mediatorId = UUID.randomUUID().toString();
//            MessageFlowDataHolder.addComponentInfoEntry(synCtx, mediatorId, "Drop Mediator", true);
//            synCtx.addComponentToMessageFlow(mediatorId);
//            MessageFlowDataHolder.addFlowInfoEntry(synCtx);
//        }

        synCtx.setTo(null);

        // if this is a response , this the end of the outflow
        if (synCtx.isResponse()) {
            StatisticsReporter.reportForAllOnOutFlowEnd(synCtx);
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : Drop mediator");
        }

//        if(MessageFlowDataHolder.isMessageFlowTraceEnable()) {
//            MessageFlowDataHolder.addComponentInfoEntry(synCtx, mediatorId, "Drop Mediator", false);
//        }

        return false;
    }

    @Override
    public boolean isContentAware() {
        return false;
    }
}
