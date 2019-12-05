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
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.resolvers.ResolverFactory;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.message.store.Constants;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.message.store.impl.jms.JmsStore;
import org.apache.synapse.message.store.impl.memory.InMemoryStore;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Create an instance of the given Message Store, and sets properties on it.
 * <p/>
 * &lt;messageStore name="string" class="classname" [sequence = "string" ]&gt;
 * &lt;parameter name="string"&gt"string" &lt;parameter&gt;
 * &lt;parameter name="string"&gt"string" &lt;parameter&gt;
 * &lt;parameter name="string"&gt"string" &lt;parameter&gt;
 * .
 * .
 * &lt;/messageStore&gt;
 */
public class MessageStoreFactory {

    private static final Log log = LogFactory.getLog(MessageStoreFactory.class);

    public static final QName CLASS_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "class");
    public static final QName NAME_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "name");
    public static final QName EXPRESSION_Q = new QName(XMLConfigConstants.NULL_NAMESPACE,"expression");
    public static final QName SEQUENCE_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "sequence");
    public static final QName PARAMETER_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
            "parameter");
    private static final QName DESCRIPTION_Q
            = new QName(SynapseConstants.SYNAPSE_NAMESPACE, "description");
    public static final QName KEY_Q = new QName("", "key");


    public static MessageStore createMessageStore(OMElement elem, Properties properties) {
        return createMessageStore(elem, properties, null);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static MessageStore createMessageStore(OMElement elem, Properties properties,
            SynapseConfiguration synapseConfiguration) {

        OMAttribute clss = elem.getAttribute(CLASS_Q);
        MessageStore messageStore;
        if (clss != null) {
            String clssName = clss.getAttributeValue();

            // Make synapse configuration backward compatible
            if (Constants.DEPRECATED_INMEMORY_CLASS.equals(clssName)) {
                clssName = InMemoryStore.class.getName();
            } else if (Constants.DEPRECATED_JMS_CLASS.equals(clssName)) {
                clssName = JmsStore.class.getName();
            }

            try {
                Class cls = Class.forName(clssName);
                messageStore = (MessageStore) cls.newInstance();
            } catch (Exception e) {
                handleException("Error while instantiating the message store", e);
                return null;
            }
        } else {
            messageStore = new InMemoryStore();
        }


        OMAttribute nameAtt = elem.getAttribute(NAME_Q);

        if (nameAtt != null) {
            messageStore.setName(nameAtt.getAttributeValue());
        } else {
            handleException("Message Store name not specified");
        }


        OMElement descriptionElem = elem.getFirstChildWithName(DESCRIPTION_Q);
        if (descriptionElem != null) {
            messageStore.setDescription(descriptionElem.getText());
        }

        messageStore.setParameters(getParameters(elem, messageStore, synapseConfiguration));



        log.info("Successfully added Message Store configuration of : [" + nameAtt.getAttributeValue() + "].");
        return messageStore;
    }

    private static Map<String, Object> getParameters(OMElement elem, MessageStore messageStore,
            SynapseConfiguration synapseConfiguration) {
        Map<String, Object> parameters = null;
        try {
            Iterator params = elem.getChildrenWithName(PARAMETER_Q);
            parameters = new HashMap<>();
            while (params.hasNext()) {
                Object o = params.next();
                if (o instanceof OMElement) {
                    OMElement prop = (OMElement) o;
                    OMAttribute paramName = prop.getAttribute(NAME_Q);
                    OMAttribute expression = prop.getAttribute(EXPRESSION_Q);
                    OMAttribute paramKey = prop.getAttribute(KEY_Q);
                    String paramValue = prop.getText();
                    if(expression != null){
                        SynapsePath xPathExpression = SynapsePathFactory.getSynapsePath(prop, EXPRESSION_Q);
                        registerParameter(parameters, paramName, xPathExpression);
                    } else if (paramKey != null) {
                        addParameterKey(messageStore, paramName.getAttributeValue(), paramKey.getAttributeValue());
                        if (synapseConfiguration != null) {
                            Object obj = synapseConfiguration.getEntry(paramKey.getAttributeValue());
                            if (obj == null) {
                                synapseConfiguration.getEntryDefinition(paramKey.getAttributeValue());
                                obj = synapseConfiguration.getEntry(paramKey.getAttributeValue());
                            }
                            if (obj != null && obj instanceof OMTextImpl) {
                                OMText objText = (OMText) obj;
                                registerParameter(parameters, paramName, objText.getText());
                            } else {
                                String msg = "Registry entry defined with key: " + paramKey.getAttributeValue()
                                        + " not found.";
                                handleException(msg);
                            }
                        } else {
                            handleException("Synapse configuration is null.");
                        }
                    } else {
                        String resolvedParameter = paramValue;
                        if (resolvedParameter != null) {
                            resolvedParameter = ResolverFactory.getInstance().getResolver(paramValue).resolve();
                        }
                        registerParameter(parameters, paramName, resolvedParameter);
                    }
                }
            }
        } catch (JaxenException e) {
            throw new SynapseException("Error occurred while extracting the parameters",e);
        }
        return parameters;
    }

    /**
     * Register the extracted parameter in the list.
     *
     * @param parameters the list of parameters which should be registered.
     * @param paramName  the name of the parameter.
     * @param paramValue the value of the parameter.
     * @param <T>        the type of the parameter.
     */
    private static <T> void registerParameter(Map<String, Object> parameters,
                                              OMAttribute paramName,
                                              T paramValue) {
        if (paramName != null && paramValue != null) {
            parameters.put(paramName.getAttributeValue(), paramValue);
        } else {
            handleException("Invalid MessageStore parameter - Parameter must have a name ");
        }
    }

    private static void addParameterKey(MessageStore ms, String key, String value) {
        ms.addParameterKey(key, value);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
