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
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.HashMap;

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

    public boolean mediate(MessageContext synCtx){
    	
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Enrich mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }
        
        if(this.isNativeJsonSupportEnabled(synCtx)) {
        	HashMap<String, Object> sourceEvaluationStatus = null;
            try {
            	/** returning the message block to be used for enriching */
            	sourceEvaluationStatus = source.evaluateJson(synCtx, synLog);
            } catch (JaxenException e) {
            	handleException("JaxenException : ", e, synCtx);
            }
            if (sourceEvaluationStatus.get("errorsExistInSrcTag").equals(true)) {
                handleException("Errors do exist in enrich source tag definition, unable to proceed : ", synCtx);
            } else {
            	/** synCtx: Current Message Context,
            	evaluatedSrcJsonElement: Json Element to be used for enriching */
                try {
                	target.insertJson(synCtx, sourceEvaluationStatus.get("evaluatedSrcJsonElement"), synLog);               
                } catch (JaxenException e) {
                	handleException("JaxenException : ", e, synCtx);
                }
            }
        } else {
        	ArrayList<OMNode> sourceNodeList;
            try {
            	/** returning the message block to be used for enriching */
                sourceNodeList = source.evaluate(synCtx, synLog);
                if (sourceNodeList == null) {
                    handleException("Failed to get the source for Enriching : ", synCtx);
                } else {
                	/** synCtx: Current Message Context,
                	sourceNodeList: Message part to be enriched from the in.message */
                    target.insert(synCtx, sourceNodeList, synLog);
                }
            } catch (JaxenException e) {
                handleException("Failed to get the source for Enriching", e, synCtx);
            }
        }

        synLog.traceOrDebug("End : Enrich mediator");
        return true;
    }
    
    /**
     * This method verifies whether the existing message payload should be treated with native json support or not
     * @param synCtx Message Context
     * @return True or false depending on whether native json support is enabled or not
     */
    private boolean isNativeJsonSupportEnabled(MessageContext synCtx) {
    	
    	/** Check whether the current message context has a json payload or not... */
        boolean currentMsgPayloadIsJson = 
        		JsonUtil.hasAJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext());
              
        boolean sourceHasCustom = (this.source.getSourceType() == EnrichMediator.CUSTOM);
        boolean targetHasCustom = (this.target.getTargetType() == EnrichMediator.CUSTOM);
        boolean enrichHasCustom = (sourceHasCustom || targetHasCustom);
        
        boolean sourceHasACustomJsonPath = false;
        boolean targetHasACustomJsonPath = false;
        
        if(sourceHasCustom){
        	boolean sourcePathIsJson = "JSON_PATH".equals(this.source.getXpath().getPathType());
        	sourceHasACustomJsonPath = sourcePathIsJson;
        }
        
        if(targetHasCustom){
        	boolean targetPathIsJson = "JSON_PATH".equals(this.target.getXpath().getPathType());
        	targetHasACustomJsonPath = targetPathIsJson;
        }
        
        /** conditions where native-json-processing is supported... */
        
        boolean cndt1IsTrue = false, cndt2IsTrue = false, cndt3IsTrue = false, cndt4IsTrue = false;
        cndt1IsTrue = (currentMsgPayloadIsJson && !enrichHasCustom);
        cndt2IsTrue = (currentMsgPayloadIsJson && sourceHasACustomJsonPath && !targetHasCustom);
        cndt3IsTrue = (currentMsgPayloadIsJson && !sourceHasCustom && targetHasACustomJsonPath);
        cndt4IsTrue = (currentMsgPayloadIsJson && sourceHasACustomJsonPath && targetHasACustomJsonPath);
        
    	return (cndt1IsTrue || cndt2IsTrue || cndt3IsTrue || cndt4IsTrue);
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
}