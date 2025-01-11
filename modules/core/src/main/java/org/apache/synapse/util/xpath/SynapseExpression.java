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
package org.apache.synapse.util.xpath;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.config.xml.SynapsePath;
import org.jaxen.JaxenException;

import java.lang.reflect.InvocationTargetException;

/**
 * Create a Synapse Expression by loading the implementation class.
 * Syntax ${ + expression +  } ex: expression="${vars.test + 5}"
 */
public class SynapseExpression extends SynapsePath {

    private final SynapsePath expression;

    private static final Log log = LogFactory.getLog(SynapseExpression.class);

    public SynapseExpression(String synapseExpression) throws JaxenException {
        super(synapseExpression, org.apache.synapse.config.xml.SynapsePath.SYNAPSE_EXPRESSIONS_PATH, log);
        String expressionClass = SynapsePropertiesLoader.loadSynapseProperties().
                getProperty(SynapseConstants.SYNAPSE_EXPRESSION_IMPL);
        if (!StringUtils.isEmpty(expressionClass)) {
            if (log.isDebugEnabled()) {
                log.debug("Trying to load the SynapseExpression implementation");
            }
            try {
                Class<?> clazz = Class.forName(expressionClass);
                this.expression = (SynapsePath) clazz.getConstructor(String.class).newInstance(synapseExpression);
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new JaxenException("Error while loading the SynapseExpression implementation class", e);
            }
        } else {
            throw new JaxenException("SynapseExpression implementation class is not defined in the properties");
        }
    }

    @Override
    public String stringValueOf(MessageContext synCtx) {
        return this.expression.stringValueOf(synCtx);
    }

    @Override
    public Object objectValueOf(MessageContext synCtx) {
        return this.expression.objectValueOf(synCtx);
    }

    @Override
    public boolean isContentAware() {
        return this.expression.isContentAware();
    }
}
