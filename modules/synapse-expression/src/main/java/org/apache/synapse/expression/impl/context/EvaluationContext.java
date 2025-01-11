/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.expression.impl.context;

import com.jayway.jsonpath.JsonPath;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.expression.impl.constants.ExpressionConstants;
import org.apache.synapse.expression.impl.exception.EvaluationException;
import org.apache.synapse.mediators.template.TemplateContext;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Stack;

import static org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS;

/**
 * Represents the evaluation context of the Synapse Expressions.
 * Which provides payload and headers access.
 */
public class EvaluationContext {

    private MessageContext synCtx;

    private Map<String, String> namespaceMap;

    // re-use the payload string to avoid multiple evaluations ex: payload.num1 + payload.num2 requires two evaluations
    private String payload;

    private boolean isJSON = false;

    public EvaluationContext() {
    }

    // Variable methods
    public Object getVariable(String name) {
        if (synCtx != null) {
            return synCtx.getVariable(name);
        }
        return null;
    }

    // Payload methods
    public Object getJSONResult(String expression) throws IOException, JaxenException {
        if (payload == null) {
            if (JsonUtil.hasAJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext())) {
                payload = IOUtils.toString(Objects.requireNonNull(JsonUtil.getJsonPayload(((Axis2MessageContext) synCtx)
                        .getAxis2MessageContext())));
                isJSON = true;
            } else {
                // handle non-json payloads
                SynapseJsonPath jsonPath = new SynapseJsonPath("$.");
                payload = jsonPath.stringValueOf(synCtx);
            }
            if (StringUtils.isEmpty(payload)) {
                throw new EvaluationException("Payload is empty");
            }
        }
        return JsonPath.parse(payload).read(expression);
    }

    public Object getHeader(String name) {
        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        Object headerMap = axis2MessageContext.getProperty(TRANSPORT_HEADERS);
        if (headerMap instanceof Map) {
            return ((Map<String, Object>) headerMap).get(name);
        }
        return null;
    }


    public Object getProperty(String proName, String scope) {
        if (synCtx != null) {
            if (SynapseConstants.SYNAPSE.equals(scope)) {
                return synCtx.getProperty(proName);
            } else if (ExpressionConstants.AXIS2.equals(scope)) {
                return ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(proName);
            }
        }
        return null;
    }

    public Object getFunctionParam(String name) {
        Stack<TemplateContext> functionStack = (Stack) synCtx.getProperty(SynapseConstants.SYNAPSE__FUNCTION__STACK);
        if (!functionStack.empty()) {
            TemplateContext topCtxt = functionStack.peek();
            if (topCtxt != null) {
                Object result = topCtxt.getParameterValue(name);
                if (result instanceof SynapseXPath) {
                    SynapseXPath expression = (SynapseXPath) topCtxt.getParameterValue(name);
                    try {
                        return expression.evaluate(synCtx);
                    } catch (JaxenException e) {
                        return null;
                    }
                } else {
                    return result;
                }
            }
        }
        return null;
    }

    public Object getRegistryResource(String key) throws UnsupportedEncodingException {
        if (synCtx != null && key != null && !key.isEmpty()) {
            Registry registry = synCtx.getConfiguration().getRegistry();
            if (registry != null) {
                OMNode resource = registry.lookup(key);
                if (resource != null) {
                    if (resource instanceof OMText) {
                        OMTextImpl omText = (OMTextImpl) resource;
                        byte[] bytes = Base64.getDecoder().decode(omText.getText());
                        return new String(bytes, StandardCharsets.UTF_8);
                    } else if (resource instanceof OMElement) {
                        return resource.toString();
                    }
                }
            }
        }
        return null;
    }

    public String getRegistryResourceProperty(String key, String propName) {
        if (synCtx != null && key != null && !key.isEmpty() && propName != null && !propName.isEmpty()) {
            Registry registry = synCtx.getConfiguration().getRegistry();
            if (registry != null) {
                Properties properties = registry.getResourceProperties(key);
                if (properties != null) {
                    return properties.getProperty(propName);
                }
            }
        }
        return null;
    }

    /**
     * Evaluates a XPath expression. <property name="Width" expression="#[xpath('//a:parent/b:child/a:value/text()')]"
     * xmlns:a="http://namespaceA.com" xmlns:b="http://namespaceB.com"/>
     *
     * @param expression XPath expression
     * @return evaluated result
     * @throws JaxenException if an error occurs while evaluating the expression
     */
    public Object evaluateXpathExpression(String expression, boolean isObjectValue) throws JaxenException {
        SynapseXPath xpath = new SynapseXPath(expression);
        for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
            xpath.addNamespace(entry.getKey(), entry.getValue());
        }
        if (isObjectValue) {
            return xpath.evaluate(synCtx);
        } else {
            return xpath.stringValueOf(synCtx);
        }
    }

    public String fetchSecretValue(String alias) throws JaxenException {
        SynapseXPath xpath = new SynapseXPath(ExpressionConstants.VAULT_LOOKUP + alias + "')");
        return xpath.stringValueOf(synCtx);
    }

    public void setSynCtx(MessageContext synCtx) {
        this.synCtx = synCtx;
    }

    public void setNamespaceMap(Map<String, String> namespaceMap) {
        this.namespaceMap = namespaceMap;
    }

    public boolean isJSON() {
        return isJSON;
    }
}
