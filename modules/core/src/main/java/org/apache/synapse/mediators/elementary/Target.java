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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.util.EnumSet;
import java.util.Set;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.SOAPHeaderImpl;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.apache.synapse.util.xpath.SynapseXPathConstants;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inset an Axiom element to the current message. The target to insert the OMElement can be
 * 1. A property
 * 2. SOAP Body child element
 * 3. SOAP envelope
 * 4. A XPath expression to get the correct node
 * <p/>
 * In case the target is an SOAP Envelope, the current SOAP envelope will be replaced by the
 * OMNode. So the OMNode must me a SOAPEnvelope.
 * <p/>
 * In case of Body the first child of the Body will be replaced by the new Node or a sibling
 * will be added to it depending on the replace property.
 * <p/>
 * In case of Expression a SOAP Element will be chosen based on the XPath. If replace is true
 * that element will be replaced, otherwise a sibling will be added to that element.
 * <p/>
 * Property case is simple. The OMNode will be stored in the given property
 */

public class Target {

    private SynapsePath xpath = null;

    private String property = null;

    private int targetType = EnrichMediator.CUSTOM;

    public static final String ACTION_REPLACE = "replace";

    public static final String ACTION_ADD_CHILD = "child";

    public static final String ACTION_ADD_SIBLING = "sibling";

    private String action = ACTION_REPLACE;

    public static final String XPATH_PROPERTY_PATTERN = "'[^']*'";

    private static final Log log = LogFactory.getLog(Target.class);

    private final JsonParser jsonParser = new JsonParser();

    public void insert(MessageContext synContext,
                       ArrayList<OMNode> sourceNodeList, SynapseLog synLog) throws JaxenException {

        if (targetType == EnrichMediator.CUSTOM) {
            assert xpath != null : "Xpath cannot be null for CUSTOM";

            if (sourceNodeList.isEmpty()) {
                synLog.error("Cannot Enrich message from an empty source.");
                return;
            }

            Object targetObj = xpath.selectSingleNode(synContext);
            //if the type custom is used to enrich a property, It'll be handled in a different method
            if (xpath.getExpression().startsWith(SynapseXPathConstants.GET_PROPERTY_FUNCTION)) {
                this.handleProperty((SynapseXPath) xpath, synContext, sourceNodeList, synLog);
            } else {
                if (targetObj instanceof SOAPHeaderImpl) {
                    OMElement targetElem = (OMElement) targetObj;
                    ArrayList<OMNode> headerSourceNodeList = new ArrayList<>();
                    for (OMNode o : sourceNodeList) {
                        OMElement ins = ((OMElement) o).cloneOMElement();
                        SOAPFactory fac = (SOAPFactory) synContext.getEnvelope().getOMFactory();
                        try {
                            headerSourceNodeList.add(ElementHelper.toSOAPHeaderBlock(ins, fac));
                        } catch (Exception e) {
                            log.error("Error occurred while transforming the OMElement to SOAPHeaderBlock ", e);
                            throw new JaxenException(e);
                        }
                    }
                    insertElement(headerSourceNodeList, targetElem, synLog);

                } else if (targetObj instanceof OMElement) {
                    OMElement targetElem = (OMElement) targetObj;
                    insertElement(sourceNodeList, targetElem, synLog);
                } else if (targetObj instanceof OMText) {
                    OMText targetText = (OMText) targetObj;
                    if (sourceNodeList.get(0) instanceof OMText) {
                        if (targetText.getParent() != null) {
                            Object parent = targetText.getParent();
                            if (parent instanceof OMElement) {
                                ((OMElement) parent).setText(((OMText) sourceNodeList.get(0)).getText());
                            }
                        }
                    } else if (sourceNodeList.get(0) instanceof OMElement) {
                        Object targetParent = targetText.getParent();
                        if (targetParent instanceof OMElement) {
                            targetText.detach();
                            synchronized (sourceNodeList.get(0)) {
                                ((OMElement) targetParent).addChild(sourceNodeList.get(0));
                            }
                        }
                    }
                } else if (targetObj instanceof OMAttribute) {
                    OMAttribute attribute = (OMAttribute) targetObj;
                    attribute.setAttributeValue(((OMText) sourceNodeList.get(0)).getText());
                } else {
                    synLog.error("Invalid Target object to be enrich.");
                    throw new SynapseException("Invalid Target object to be enrich.");
                }
            }
        } else if (targetType == EnrichMediator.BODY) {
            SOAPEnvelope env = synContext.getEnvelope();
            SOAPBody body = env.getBody();

            OMElement e = body.getFirstElement();

            if (e != null) {
                insertElement(sourceNodeList, e, synLog);
            } else {
                // if the body is empty just add as a child
                for (OMNode elem : sourceNodeList) {
                    if (elem instanceof OMElement) {
                        synchronized (elem){
                            body.addChild(elem);
                        }
                    } else {
                        synLog.error("Invalid Object type to be inserted into message body");
                    }
                }
            }
        } else if (targetType == EnrichMediator.ENVELOPE) {
            OMNode node = sourceNodeList.get(0);
            if (node instanceof SOAPEnvelope) {
                try {
                    synContext.setEnvelope((SOAPEnvelope) node);
                } catch (AxisFault axisFault) {
                    synLog.error("Failed to set the SOAP Envelope");
                    throw new SynapseException("Failed to set the SOAP Envelope");
                }
            } else {
                synLog.error("SOAPEnvelope is expected");
                throw new SynapseException("A SOAPEnvelope is expected");
            }
        } else if (targetType == EnrichMediator.PROPERTY) {
            assert property != null : "Property cannot be null for PROPERTY type";
			if (action != null && property != null) {
				Object propertyObj =synContext.getProperty(property);
				OMElement documentElement = null;
				try {
                    if (isOMElement(propertyObj)) {
                        documentElement = (OMElement) propertyObj;
                    } else {
                        documentElement = AXIOMUtil.stringToOM((String) propertyObj);
                    }
                } catch (Exception e1) {
	                //just ignoring the phaser error
                }

                if (documentElement != null && action.equals(ACTION_ADD_CHILD)) {
                    //logic should valid only when adding child elements, and other cases
                    //such as sibling and replacement using the else condition
                    insertElement(sourceNodeList, documentElement, synLog);
                    if (isOMElement(propertyObj)) {
                        synContext.setProperty(property, documentElement);
                    } else {
                        synContext.setProperty(property, documentElement.getText());
                    }
                } else {
                    synContext.setProperty(property, sourceNodeList);
                }

			}else{
			synContext.setProperty(property, sourceNodeList);  
			}
        }
    }

    /**
     * Checks whether object is instanceof OMElement
     *
     * @param propObject Object which needs to be evaluated
     * @return true if object is is instanceof OMElement else false
     */
    private boolean isOMElement(Object propObject) {
        return propObject instanceof OMElement;
    }

    private void insertElement(ArrayList<OMNode> sourceNodeList, OMElement e, SynapseLog synLog) {
        if (action.equals(ACTION_REPLACE)) {
            boolean isInserted = false;
            for (OMNode elem : sourceNodeList) {
                if (elem instanceof OMElement) {
                    e.insertSiblingBefore(elem);
                    isInserted = true;
                } else if (elem instanceof OMText) {
                    e.setText(((OMText) elem).getText());
                } else {
                    synLog.error("Invalid Source object to be inserted.");
                }
            }
            if (isInserted) {
                e.detach();
            }
        } else if (action.equals(ACTION_ADD_CHILD)) {
            for (OMNode elem : sourceNodeList) {
                if (elem instanceof OMElement) {
                    synchronized (elem){
                        e.addChild(elem);
                    }
                }
            }
        } else if (action.equals(ACTION_ADD_SIBLING)) {
            for (OMNode elem : sourceNodeList) {
                if (elem instanceof OMElement) {
                    e.insertSiblingAfter(elem);
                }
            }
        }
    }

    /**
     * Handles enrichment of properties when defined as a custom type
     *
     * @param xpath          expression to get property
     * @param synContext     messageContext used in the mediation
     * @param sourceNodeList node list which used to change the target
     * @param synLog         the Synapse log to use
     */
    private void handleProperty(SynapseXPath xpath, MessageContext synContext, ArrayList<OMNode> sourceNodeList, SynapseLog synLog) {

        String scope = XMLConfigConstants.SCOPE_DEFAULT;
        Pattern p = Pattern.compile(XPATH_PROPERTY_PATTERN);
        Matcher m = p.matcher(xpath.getExpression());
        List<String> propList = new ArrayList();
        while (m.find()) {
            propList.add(StringUtils.substringBetween(m.group(), "\'", "\'"));
        }

        if (propList.size() > 1) {
            property = propList.get(1);
            scope = propList.get(0);
        } else {
            property = propList.get(0);
        }

        OMElement documentElement = null;
        Object propertyObj = null;
        Axis2MessageContext axis2smc = (Axis2MessageContext) synContext;

        if (action != null && property != null) {
            if (XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
                propertyObj = synContext.getProperty(property);
            } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)) {
                propertyObj = axis2smc.getAxis2MessageContext().getProperty(property);
            } else if (XMLConfigConstants.SCOPE_OPERATION.equals(scope)) {
                propertyObj = axis2smc.getAxis2MessageContext().getOperationContext().getProperty(property);
            }

            if (propertyObj != null && propertyObj instanceof OMElement && action.equals(ACTION_ADD_CHILD)) {
                documentElement = (OMElement) propertyObj;
                documentElement = documentElement.cloneOMElement();
                //logic should valid only when adding child elements, and other cases
                //such as sibling and replacement using the else condition
                insertElement(sourceNodeList, documentElement, synLog);
                this.setProperty(scope, synContext, documentElement);
            } else {
                this.setProperty(scope, synContext, sourceNodeList);
            }
        } else {
            this.setProperty(scope, synContext, sourceNodeList);
        }
    }

    /**
     * Sets the property value in appropriate message context
     *
     * @param scope           which property needs to set
     * @param messageContext  messageContext used in the mediation
     * @param documentElement target element which needs to set as property
     */
    public void setProperty(String scope, MessageContext messageContext, Object documentElement) {
        if (XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
            messageContext.setProperty(property, documentElement);
        } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)) {
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty(property, documentElement);
        } else if (XMLConfigConstants.SCOPE_OPERATION.equals(scope)) {
            ((Axis2MessageContext) messageContext).getAxis2MessageContext().getOperationContext().setProperty(property, documentElement);
        }
    }

    /**
     * This method will insert a provided json element to a specified target.
     *
     * @param synCtx Current Message Context.
     * @param sourceJsonElement Evaluated Json Element by the Source.
     * @param synLog Default Logger for the package.
     */
    public void insertJson(MessageContext synCtx, Object sourceJsonElement, SynapseLog synLog) {

        String jsonPath = null;
        SynapseJsonPath sourceJsonPath = null;
        if (xpath != null) {
            sourceJsonPath = (SynapseJsonPath) this.xpath;
            jsonPath = sourceJsonPath.getJsonPath().getPath();
        }

        switch (targetType) {
            case EnrichMediator.CUSTOM: {
                assert jsonPath != null : "JSONPath should be non null in case of CUSTOM";
                setEnrichResultToBody(synCtx, sourceJsonPath, sourceJsonElement);
                break;
            }
            case EnrichMediator.BODY: {
                if (action.equalsIgnoreCase(ACTION_REPLACE)) {
                    org.apache.axis2.context.MessageContext context = ((Axis2MessageContext) synCtx).
                            getAxis2MessageContext();
                    try {
                        String jsonString = sourceJsonElement.toString();
                        JsonElement element = jsonParser.parse(jsonString);
                        if (element instanceof JsonObject || element instanceof JsonArray) {
                            JsonUtil.getNewJsonPayload(context, jsonString, true, true);
                        } else {
                            synLog.error("Unsupported JSON payload : " + jsonString
                                    + ".Only JSON arrays and objects can be enriched to the body");
                        }

                    } catch (AxisFault axisFault) {
                        synLog.error("Error occurred while adding a new JSON payload");
                    }
                } else {
                    synLog.error("Unsupported action : " + action + ".Only replace is supported for target body.");
                }
                break;
            }
            default: {
                synLog.error("Case mismatch for type: " + targetType);
            }
        }
    }


    /**
     * Set the enriched JSON result to body.
     * @param synapseContext Current message context.
     * @param synapseJsonPath   SynapseJsonPath instance of the target.
     * @param sourceNode Result from source which needs to be enriched to target.
     */
    private void setEnrichResultToBody(MessageContext synapseContext, SynapseJsonPath synapseJsonPath, Object
            sourceNode) {
        String expression = synapseJsonPath.getJsonPath().getPath();

        // Though SynapseJsonPath support "$.", the JSONPath implementation does not support it
        if (expression.endsWith(".")) {
            expression = expression.substring(0, expression.length() - 1);
        }

        org.apache.axis2.context.MessageContext context = ((Axis2MessageContext) synapseContext)
                .getAxis2MessageContext();

        assert JsonUtil.hasAJsonPayload(context) : "Message Context does not contain a JSON payload";

        String jsonString = JsonUtil.jsonPayloadToString(context);
        String newJsonString = "";

        if (action.equalsIgnoreCase(ACTION_REPLACE)) {
            // replaces an existing value in json
            newJsonString = JsonPath.parse(jsonString).set(expression, sourceNode).jsonString();
        } else if (action.equalsIgnoreCase(ACTION_ADD_CHILD)) {
            newJsonString = getNewJSONString(sourceNode, expression, jsonString, expression, false);
        } else if (action.equalsIgnoreCase(ACTION_ADD_SIBLING)) {
            log.error("Action sibling is not supported. Please use child action instead");
        } else {
            // invalid action
            log.error("Invalid action set: " + action);
        }
        try {
            if (!newJsonString.trim().isEmpty()) {
                JsonUtil.getNewJsonPayload(context, newJsonString, true, true);
            }
        } catch (AxisFault axisFault) {
            log.error("Error occurred while setting new JSON payload", axisFault);
        }
    }

    /**
     * This method will add the sourceNode to location pointed by expression in the jsonString.
     *
     * @param sourceNode JsonElement which needs to be inserted.
     * @param expression Json-path which points the location to be inserted.
     * @param jsonString Target payload as a string.
     * @param parentPath Parent path of expression.
     * @param isSibling to be added as sibling or child.
     * @return formatted string.
     */
    private String getNewJSONString(Object sourceNode, String expression, String jsonString, String parentPath,
                                    boolean isSibling) {
        String newJsonString;
        DocumentContext documentContext = JsonPath.parse(jsonString);
        JsonElement receivingElement = documentContext.read(parentPath);
        JsonElement sourceElement = EIPUtils.tryParseJsonString(jsonParser, sourceNode.toString());
        if (receivingElement.isJsonArray()) {
            receivingElement.getAsJsonArray().add(sourceElement);
        } else {
            log.error("Cannot append since the target element / parent of  target element is not a JSON array: " +
                    receivingElement.toString());
        }
        if (isSibling) {
            documentContext.set(parentPath, receivingElement);
        } else {
            documentContext.set(expression, receivingElement);
        }
        newJsonString = documentContext.json().toString();
        return newJsonString;
    }

    public SynapsePath getXpath() {
        return xpath;
    }

    public String getProperty() {
        return property;
    }

    public int getTargetType() {
        return targetType;
    }

    public void setXpath(SynapsePath xpath) {
        this.xpath = xpath;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}


