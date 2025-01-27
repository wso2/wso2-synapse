/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.util.synapse.expression.ast;

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.property.PropertyHolder;
import org.apache.synapse.commons.resolvers.ResolverException;
import org.apache.synapse.util.synapse.expression.constants.ExpressionConstants;
import org.apache.synapse.util.synapse.expression.context.EvaluationContext;
import org.apache.synapse.util.synapse.expression.exception.EvaluationException;

import java.math.BigDecimal;

/**
 * Represents a node in the abstract syntax tree that provides access to headers and properties.
 */
public class HeadersAndPropertiesAccessNode implements ExpressionNode {
    private final String scope;

    public enum Type {
        HEADER,
        PROPERTY,
        CONFIG,
        FUNCTION_PARAM
    }

    private final Type type;

    // property key or header name
    private final ExpressionNode key;

    public HeadersAndPropertiesAccessNode(ExpressionNode node, Type type) {
        this.key = node;
        this.type = type;
        scope = null;
    }

    public HeadersAndPropertiesAccessNode(ExpressionNode node, String scope) {
        this.key = node;
        this.scope = scope;
        this.type = Type.PROPERTY;
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context, boolean isObjectValue) throws EvaluationException {
        if (key != null) {
            String name = key.evaluate(context, isObjectValue).asString();
            Object value;
            if (Type.HEADER.equals(type)) {
                value = context.getHeader(name);
            } else if (Type.CONFIG.equals(type)) {
                try {
                    value = PropertyHolder.getInstance().getPropertyValue(name);
                } catch (ResolverException e) {
                    throw new EvaluationException("The value of the key:[" + name + "] is null");
                }
            } else if (Type.FUNCTION_PARAM.equals(type)) {
                value = context.getFunctionParam(name);
            } else {
                if (ExpressionConstants.PATH_PARAM.equals(scope)) {
                    value = context.getProperty("uri.var." + name, SynapseConstants.SYNAPSE);
                } else if (ExpressionConstants.QUERY_PARAM.equals(scope)) {
                    value = context.getProperty("query.param." + name, SynapseConstants.SYNAPSE);
                } else {
                    value = context.getProperty(name, scope);
                }
            }
            if (value != null) {
                return tryParseNumber(value.toString());
            } else {
                throw new EvaluationException("Could not fetch the value of the key: " + name);
            }
        }
        throw new EvaluationException("Key cannot be null when accessing headers or properties");
    }


    private ExpressionResult tryParseNumber(String value) {
        try {
            return new ExpressionResult(Integer.parseInt(value));
        } catch (NumberFormatException e1) {
            try {
                return new ExpressionResult(Long.parseLong(value));
            } catch (NumberFormatException e2) {
                try {
                    return new ExpressionResult(Double.parseDouble(value));
                } catch (NumberFormatException e3) {
                    return new ExpressionResult(value);
                }
            }
        }
    }

}
