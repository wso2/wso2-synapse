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
package org.apache.synapse.mediators.elementary;

import org.apache.axiom.om.OMNode;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.jaxen.JaxenException;

import java.util.ArrayList;

/**
 * Syntax for EnrichMediator
 * <p/>
 * <enrich>
 * <source [clone=true | false] type=[custom|envelope|body|property] xpath="" property=""/>
 * <target [replace=true | false] type=[custom|envelope|body|property] xpath="" property=""/>
 * </enrich>
 * <p/>
 * This mediator will first get an OMElement from the source. Then put it to the current message
 * according to the target element.
 * <p/>
 * Both target and source can specify a type. These are the types supported
 * <p/>
 * custom : xpath expression should be provided to get the xml
 * envelope : the soap envelope
 * body : first child of the soap body
 * property : synapse property
 * <p/>
 * When specifying the source one can clone the xml by setting the clone to true. The default
 * value for clone is false.
 * <p/>
 * When specifying the target one can replace the existing xml. replace is only valid for custom
 * and body types. By default replace is true.
 */

public class EnrichMediator extends AbstractMediator {
    public static final int CUSTOM = 0;

    public static final int ENVELOPE = 1;

    public static final int BODY = 2;

    public static final int PROPERTY = 3;

    public static final int INLINE = 4;

    private Source source = null;

    private Target target = null;

    private boolean isNativeJsonSupportEnabled = false;

    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Enrich mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        boolean hasJSONPayload = JsonUtil.hasAJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext());
        if (isNativeJsonSupportEnabled && hasJSONPayload) {
            Object sourceNode;
            try {
                sourceNode = source.evaluateJson(synCtx, synLog);
                if (sourceNode == null) {
                    handleException("Failed to get the source for Enriching : ", synCtx);
                } else {
                    target.insertJson(synCtx, sourceNode, synLog);
                }
            } catch (JaxenException e) {
                handleException("Failed to get the source for Enriching", e, synCtx);
            }
        } else {
            ArrayList<OMNode> sourceNodeList;
            try {
                sourceNodeList = source.evaluate(synCtx, synLog);
                if (sourceNodeList == null) {
                    handleException("Failed to get the source for Enriching : ", synCtx);
                } else {
                    target.insert(synCtx, sourceNodeList, synLog);
                }
            } catch (JaxenException e) {
                handleException("Failed to get the source for Enriching", e, synCtx);
            }
        }

        //If enrich mediator modifies JSON payload update JSON stream in the axis2MessageContext
        org.apache.axis2.context.MessageContext axis2MsgCtx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        if (JsonUtil.hasAJsonPayload(axis2MsgCtx)) {
            JsonUtil.setJsonStream(axis2MsgCtx,
                                   JsonUtil.toJsonStream(axis2MsgCtx.getEnvelope().getBody().getFirstElement()));
        }
        synLog.traceOrDebug("End : Enrich mediator");
        return true;
    }

    public Source getSource() {
        return source;
    }

    public Target getTarget() {
        return target;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    @Override
    public boolean isContentAltering() {
        return true;
    }

    public void setNativeJsonSupportEnabled(boolean nativeJsonSupportEnabled) {
        isNativeJsonSupportEnabled = nativeJsonSupportEnabled;
    }
}
