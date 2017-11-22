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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
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

    private SynapseXPath xpath = null;

    private String property = null;

    private int targetType = EnrichMediator.CUSTOM;

    public static final String ACTION_REPLACE = "replace";

    public static final String ACTION_ADD_CHILD = "child";

    public static final String ACTION_ADD_SIBLING = "sibling";

    private String action = ACTION_REPLACE;

    public static final String XPATH_PROPERTY_PATTERN = "'[^']*'";

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
                this.handleProperty(xpath, synContext, sourceNodeList, synLog);
            } else {
                if (targetObj instanceof OMElement) {
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

    public SynapseXPath getXpath() {
        return xpath;
    }

    public String getProperty() {
        return property;
    }

    public int getTargetType() {
        return targetType;
    }

    public void setXpath(SynapseXPath xpath) {
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


