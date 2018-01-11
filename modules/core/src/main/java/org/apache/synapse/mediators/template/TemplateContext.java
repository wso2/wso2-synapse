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
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.eip.EIPUtils;

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
     * refers to the parameter names of the function
     */
    private Collection<String> parameters;
    /**
     * contains a map for parameterNames to evaluated values
     */
    private Map mappedValues;

    public TemplateContext(String name, Collection<String> parameters) {
        this.fName = name;
        this.parameters = parameters;
        mappedValues = new HashMap();
    }

    /**
     * evaluate raw parameters passed from an invoke medaiator and store them in this context
     * @param synCtxt Synapse MessageContext
     */
    public void setupParams(MessageContext synCtxt) {
        Iterator<String> paramNames = parameters.iterator();
        while (paramNames.hasNext()) {
            String parameter = paramNames.next();
            String mapping = EIPUtils.getTemplatePropertyMapping(fName, parameter);
            Object propertyValue = synCtxt.getProperty(mapping);
            Object paramValue = getEvaluatedParamValue(synCtxt, parameter, (Value) propertyValue);
            if (paramValue != null) {
                mappedValues.put(parameter, paramValue);
                //remove temp property from the context
                removeProperty(synCtxt, mapping);
            }
        }
    }

    /**
     * This method will go through the provided expression and try to evaluate if an xpath expression
     * or would return plain value. special case is present for a expression type plain value .
     * ie:- plain values in an expression format  ie:- {expr} .
     * @return evaluated value/expression
     */
    private Object getEvaluatedParamValue(MessageContext synCtx, String parameter, Value expression) {
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
                    return resolveExpressionValue(synCtx, expression);
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
