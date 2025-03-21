/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.template;

import org.apache.synapse.mediators.Value;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class represents the nested parameters of the connector operation.
 * <p>
 * <pre>{@code
 * <connector.operation>
 *     <parameter1 name="p1" value="{expr}">
 *         <childParam1 name="p2" value="{expr}"/>
 *         <childParam2>value</childParam2>
 *     </parameter1>
 *     <parameter2>value</parameter2>
 * </connector.operation>
 * }</pre>
 * </p>
 */
public class InvokeParam {

    private String paramName;

    /**
     * stores the expression for the inline value:{@code <parameter2>value</parameter2>}.
     */
    private Value inlineValue;

    /**
     * stores the attribute name expression map. {@code <parameter1 name="p1" value="{expr}">}
     */
    private Map<String, Value> attributeName2ExpressionMap;

    /**
     * stores the nested child parameters. {@code <childParam name="p2" value="{expr}"/>}
     */
    private List<InvokeParam> childParams;

    public InvokeParam(Value inlineValue) {

        this.inlineValue = inlineValue;
    }

    public InvokeParam() {

        attributeName2ExpressionMap = new LinkedHashMap<>();
        childParams = new LinkedList<>();
    }

    public String getParamName() {

        return paramName;
    }

    public void setParamName(String paramName) {

        this.paramName = paramName;
    }

    public Value getInlineValue() {

        return inlineValue;
    }

    public void setInlineValue(Value inlineValue) {

        this.inlineValue = inlineValue;
    }

    public Map<String, Value> getAttributeName2ExpressionMap() {

        return attributeName2ExpressionMap;
    }

    public void setAttributeName2ExpressionMap(Map<String, Value> attributeName2ExpressionMap) {

        this.attributeName2ExpressionMap = attributeName2ExpressionMap;
    }

    public List<InvokeParam> getChildParams() {

        return childParams;
    }

    public void setChildParams(List<InvokeParam> childParams) {

        this.childParams = childParams;
    }

    public void addChildParam(InvokeParam childParam) {

        childParams.add(childParam);
    }

    public void addAttribute2Expression(String attributeName, Value expression) {

        attributeName2ExpressionMap.put(attributeName, expression);
    }
}
