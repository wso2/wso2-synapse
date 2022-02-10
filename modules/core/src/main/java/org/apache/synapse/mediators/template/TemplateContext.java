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
package org.apache.synapse.mediators.template;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMText;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.transport.util.MessageHandlerProvider;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.util.xpath.SynapseJsonPath;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class will store runtime context of a synapse function template. For each invoked Template
 * a context will be populated with function parameters.
 */
public class TemplateContext {
    /**
     * refers to the function-template name this context is binded to
     */
    private String fName;
    /**
     * refers to the parameters of the function
     */
    private Collection<TemplateParam> parameters;
    /**
     * contains a map for parameterNames to evaluated values
     */
    private Map mappedValues;

    public TemplateContext(String name, Collection<TemplateParam> parameters) {
        this.fName = name;
        this.parameters = parameters;
        mappedValues = new HashMap();
    }

    /**
     * evaluate raw parameters passed from an invoke medaiator and store them in this context
     *
     * @param synCtxt Synapse MessageContext
     */
    public void setupParams(MessageContext synCtxt) {
        Iterator<TemplateParam> paramNames = parameters.iterator();
        while (paramNames.hasNext()) {
            TemplateParam parameter = paramNames.next();
            String parameterName = parameter.getName();
            String mapping = EIPUtils.getTemplatePropertyMapping(fName, parameterName);
            Object propertyValue = synCtxt.getProperty(mapping);
            Object paramValue = null;
            if (propertyValue == null) {
                if (parameter.isMandatory()) {
                    String errorDetail = "Neither a value nor default "
                            + "value is provided for mandatory parameter: "
                            + parameterName + " in seq-template " + fName;
                    synCtxt.setProperty(SynapseConstants.ERROR_CODE, 500101);
                    synCtxt.setProperty(SynapseConstants.ERROR_MESSAGE, errorDetail);
                    throw new SynapseException(errorDetail);
                } else {
                    Object defaultValue = parameter.getDefaultValue();
                    if (defaultValue != null) {
                        paramValue = defaultValue;
                    }
                }
            } else {
                try {
                    paramValue = getEvaluatedParamValue(synCtxt, parameterName, (Value) propertyValue);
                } catch (IOException | XMLStreamException e) {
                    throw new SynapseException("Error while evaluating parameters"
                            + " passed to template " + fName, e);
                }
            }
            if (paramValue != null) {
                mappedValues.put(parameterName, paramValue);
            }
            //remove temp property from the context
            removeProperty(synCtxt, mapping);
        }
    }

    /**
     * This method will go through the provided expression and try to evaluate if an xpath expression
     * or would return plain value. special case is present for a expression type plain value .
     * ie:- plain values in an expression format  ie:- {expr} .
     * @return evaluated value/expression
     */
    private Object getEvaluatedParamValue(MessageContext synCtx, String parameter, Value expression)
            throws IOException, XMLStreamException {

        if (expression != null) {
            if (expression.getExpression() != null) {
                if(expression.hasExprTypeKey()){
                	if(expression.hasPropertyEvaluateExpr()){
                		//TODO:evalute the string expression get the value
                		//String evaluatedPath ="{//m0:getQuote/m0:request}";
                		return expression.evalutePropertyExpression(synCtx);
                	}
                    return expression.getExpression();
                } else {
                    org.apache.axis2.context.MessageContext axis2MessageContext
                            = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                    /*
                     * Need to build message conditionally if not already built
                     * in order to evaluate XPath and JsonPath expressions
                     */
                    if(expression.getExpression().contentAware
                            && (!Boolean.TRUE.equals(axis2MessageContext.
                            getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED)))) {

                        org.apache.axis2.context.MessageContext axis2MsgCtx =
                                ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                        MessageHandlerProvider.getMessageHandler(axis2MsgCtx).buildMessage(axis2MsgCtx);
                    }

                    if (expression.getExpression() instanceof SynapseJsonPath) {
                        return expression.evaluateValue(synCtx);
                    } else {
                        return resolveExpressionValue(synCtx, expression);
                    }
                }
            } else if (expression.getKeyValue() != null) {
                return expression.evaluateValue(synCtx);
            }
        }
        return null;
    }

    private Object resolveExpressionValue(MessageContext synCtx, Value expression) {

        Object result = expression.resolveObject(synCtx);

        // Extract string values from axiom objects which has only texts
        if (result instanceof OMText) {
            return ((OMText) result).getText();
        } else if (result instanceof OMAttribute) {
            return ((OMAttribute) result).getAttributeValue();
        } else {
            return result;
        }
    }

    private void removeProperty(MessageContext synCtxt, String deletedMapping) {
        //Removing property from the  Synapse Context
        Set keys = synCtxt.getPropertyKeySet();
        if (keys != null) {
            keys.remove(deletedMapping);
        }
    }

    public Object getParameterValue(String paramName) {
        return mappedValues.get(paramName);
    }

    public Map getMappedValues() {
        return mappedValues;
    }

    public void setMappedValues(Map map) {
        this.mappedValues = map;
    }

    public String getName() {
        return fName;
    }

    public Collection getParameters() {
        return parameters;
    }
}
