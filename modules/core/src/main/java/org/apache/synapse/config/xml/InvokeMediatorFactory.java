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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.template.InvokeMediator;

import java.util.Iterator;
import java.util.Properties;
import javax.xml.namespace.QName;

/**
 * Builds Invoke mediator from a configuration as
 * <invoke target="">
 * <parameter name="p1" value="{expr}" />
 * <parameter name="p1" value="{{expr}}" />
 * <parameter name="p1" value="v2" />
 * ...
 * ..
 * </invoke>
 */
public class InvokeMediatorFactory extends AbstractMediatorFactory {

    private static final QName INVOKE_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "call-template");

    /**
     * Element  QName Definitions
     */
    public static final QName WITH_PARAM_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "with-param");

    public static final QName WITH_PARAM_DYNAMIC_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "parameter");

    
    InvokeMediator invoker;

    @Override
    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {
        invoker = new InvokeMediator();
        processAuditStatus(invoker, elem);
        OMAttribute targetTemplateAttr = elem.getAttribute(ATT_TARGET);
        if (targetTemplateAttr != null) {
            if (StringUtils.isNotEmpty(targetTemplateAttr.getAttributeValue())) {
                invoker.setTargetTemplate(targetTemplateAttr.getAttributeValue());
                buildParameters(elem);
            } else {
                String msg = "EIP Invoke mediator should have a non empty target name specified.";
                log.error(msg);
                throw new SynapseException(msg);
            }
        } else {
            String msg = "EIP Invoke mediator should have a target template specified.";
            log.error(msg);
            throw new SynapseException(msg);
        }

        addAllCommentChildrenToList(elem, invoker.getCommentsList());

        return invoker;
    }

    private void buildParameters(OMElement elem) {
        Iterator subElements = elem.getChildElements();
        while (subElements.hasNext()) {
            OMElement child = (OMElement) subElements.next();
            if (child.getQName().equals(WITH_PARAM_Q)) {
                OMAttribute paramNameAttr = child.getAttribute(ATT_NAME);
                Value paramValue = new ValueFactory().createValue("value", child);
                if (paramNameAttr != null) {
                    if (StringUtils.isNotEmpty(paramNameAttr.getAttributeValue())) {
                        //set parameter value
                        invoker.addExpressionForParamName(paramNameAttr.getAttributeValue(), paramValue);
                    } else {
                        String msg = "Call template mediator parameters should have non empty name.";
                        log.error(msg);
                        throw new SynapseException(msg);
                    }
                }
            }
        }

    }


    public QName getTagQName() {
        return INVOKE_Q;
    }
}
