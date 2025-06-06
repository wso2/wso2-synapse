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
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.template.InvokeMediator;
import org.apache.synapse.mediators.template.InvokeParam;

import java.util.Iterator;
import java.util.Map;

/**
 * Serialize a Invoke mediator to a configuration given below
 * <invoke target="">
 * <parameter name="p1" value="{expr}" />
 * <parameter name="p1" value="{{expr}}" />
 * <parameter name="p1" value="v2" />
 * ...
 * ..
 * </invoke>
 */
public class InvokeMediatorSerializer extends AbstractMediatorSerializer{
    public static final String INVOKE_N = "call-template";


    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {
        if (!(m instanceof InvokeMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }
        InvokeMediator mediator = (InvokeMediator) m;
        OMElement invokeElem = null;
        if(mediator.isDynamicMediator()){
        	String packageName = (mediator.getPackageName() != null && !mediator.getPackageName().isEmpty())?mediator.getPackageName():"";
        	invokeElem=fac.createOMElement(mediator.getTargetTemplate().substring(packageName.length()+1,mediator.getTargetTemplate().length()), synNS);
        	 if (mediator.getKey() != null) {
                 // Serialize Value using ValueSerializer
                 ValueSerializer keySerializer = new ValueSerializer();
                 keySerializer.serializeValue(mediator.getKey(), XMLConfigConstants.CONFIG_REF, invokeElem);
                 
             } 
        }else{
        	invokeElem=fac.createOMElement(INVOKE_N, synNS);
        }

        if (mediator.getTargetTemplate() != null) {
        	if(!mediator.isDynamicMediator()){
            invokeElem.addAttribute(fac.createOMAttribute(
                    "target", nullNS, mediator.getTargetTemplate()));
        	}

            serializeParams(invokeElem, mediator);
            saveTracingState(invokeElem, mediator);
        }

        if (mediator.getErrorHandler() != null) {
            invokeElem.addAttribute(fac.createOMAttribute("onError",
                    nullNS, mediator.getErrorHandler()));
        }

        serializeComments(invokeElem, mediator.getCommentsList());

        return invokeElem;
    }

    private void serializeParams(OMElement invokeElem, InvokeMediator mediator) {

        Map<String, InvokeParam> paramsMap = mediator.getpName2ParamMap();
        Iterator<String> paramIterator = paramsMap.keySet().iterator();
        while (paramIterator.hasNext()) {
            String paramName = paramIterator.next();
            if (paramName != null) {
                if (mediator.isDynamicMediator()) {
                    OMElement paramEl = fac.createOMElement(paramName, synNS);
                    InvokeParam value = paramsMap.get(paramName);
                    serializeInvokeParam(value, paramEl);
                    invokeElem.addChild(paramEl);
                } else {
                    String prefix = mediator.isDynamicMediator()
                            ? InvokeMediatorFactory.WITH_PARAM_DYNAMIC_Q.getLocalPart()
                            : InvokeMediatorFactory.WITH_PARAM_Q.getLocalPart();
                    OMElement paramEl = fac.createOMElement(prefix, synNS);
                    paramEl.addAttribute(fac.createOMAttribute("name", nullNS, paramName));
                    // serialize value attribute
                    Value value = paramsMap.get(paramName).getInlineValue();
                    new ValueSerializer().serializeValue(value, "value", paramEl);
                    invokeElem.addChild(paramEl);
                }
            }
        }

    }

    private void serializeInvokeParam(InvokeParam value, OMElement paramEl) {

        if (value.getInlineValue() != null) {
            new ValueSerializer().serializeTextValue(value.getInlineValue(), "value", paramEl);
        }
        if (value.getAttributeName2ExpressionMap() != null) {
            Iterator<String> attrIterator = value.getAttributeName2ExpressionMap().keySet().iterator();
            while (attrIterator.hasNext()) {
                String attrName = attrIterator.next();
                Value attrValue = value.getAttributeName2ExpressionMap().get(attrName);
                new ValueSerializer().serializeValue(attrValue, attrName, paramEl);
            }
        }
        if (value.getChildParams() != null) {
            Iterator<InvokeParam> childIterator = value.getChildParams().iterator();
            while (childIterator.hasNext()) {
                InvokeParam childParam = childIterator.next();
                OMElement childEl = fac.createOMElement(childParam.getParamName(), synNS);
                serializeInvokeParam(childParam, childEl);
                paramEl.addChild(childEl);
            }
        }
    }

    public String getMediatorClassName() {
        return InvokeMediator.class.getName();
    }
    
 
}
