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

import com.jayway.jsonpath.JsonPath;
import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.jaxen.JaxenException;
import java.util.ArrayList;
import java.util.List;


/**
 * The source of the XML node to be stored. The source can be a
 * 1. Property
 * 2. XPath Expression
 * 3. SOAP Envelope
 * 4. SOAP Body
 * <p/>
 * If clone is true a clone will be create and stored from the origincal content. Otherwise a
 * reference will be stored.
 * <p/>
 * In case of property a OMElement is stored in a property and it will be fetched.
 * <p/>
 * In case of a XPath expression, it will be evaluated to get the OMElement
 * <p/>
 * In case of SOAPEnvelope entire SOAP envelope will be stored
 * <p/>
 * In case of Body, the first child of body will be stored
 */

public class Source {
    private SynapsePath xpath = null;

    private String property = null;

    private int sourceType = EnrichMediator.CUSTOM;

    private boolean clone = true;

    private OMNode inlineOMNode = null;

    private String inlineKey = null;

    public ArrayList<OMNode> evaluate(MessageContext synCtx, SynapseLog synLog)
            throws JaxenException {

        ArrayList<OMNode> sourceNodeList = new ArrayList<OMNode>();

        if (sourceType == EnrichMediator.CUSTOM) {
            assert xpath != null : "XPath should be non null in case of CUSTOM";

            List selectedNodeList = xpath.selectNodes(synCtx);
            if (selectedNodeList != null && selectedNodeList.size() != 0) {
                for (Object o : selectedNodeList) {
                    if (o instanceof OMElement) {
                        if (clone) {
                            OMElement ins = ((OMElement) o).cloneOMElement();
                            if (o instanceof SOAPHeaderBlock) {
                                SOAPFactory fac = (SOAPFactory) ((OMElement) o).getOMFactory();
                                try {
                                    sourceNodeList.add(ElementHelper.toSOAPHeaderBlock(ins, fac));
                                } catch (Exception e) {
                                    synLog.error(e);
                                    throw new JaxenException(e);
                                }
                            } else {
                                sourceNodeList.add(ins);
                            }
                        } else {
                            sourceNodeList.add((OMElement) o);
                        }
                    } else if (o instanceof OMText) {
                        sourceNodeList.add((OMText) o);
                    } else if (o instanceof String) {
                         OMFactory fac = OMAbstractFactory.getOMFactory();
                         sourceNodeList.add(fac.createOMText(o.toString()));
                    }
                }
            } else {
                synLog.error("Specified node by xpath cannot be found.");
            }
        } else if (sourceType == EnrichMediator.BODY) {
            if (clone) {
				if (synCtx.getEnvelope().getBody().getFirstElement() != null) {
					sourceNodeList.add(synCtx.getEnvelope().getBody().getFirstElement()
					                         .cloneOMElement());
				}
            } else {
				if (synCtx.getEnvelope().getBody().getFirstElement() != null) {
					sourceNodeList.add(synCtx.getEnvelope().getBody().getFirstElement());
				}
            }
        } else if (sourceType == EnrichMediator.ENVELOPE) {
            if (clone) {
                sourceNodeList.add(MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope()));
            } else {
                sourceNodeList.add(synCtx.getEnvelope());
            }
        } else if (sourceType == EnrichMediator.PROPERTY) {
            assert property != null : "property shouldn't be null when type is PROPERTY";

            Object o = synCtx.getProperty(property);

            if (o instanceof OMElement) {
                if (clone) {
                    sourceNodeList.add(((OMElement) o).cloneOMElement());
                }
                else{
                    sourceNodeList.add((OMElement) o);
                }
            } else if (o instanceof String) {
                String sourceStr = (String) o;
                OMFactory fac = OMAbstractFactory.getOMFactory();
                sourceNodeList.add(fac.createOMText(sourceStr));
			} else if (o instanceof ArrayList) {
				ArrayList nodesList;
				if (clone) {
					nodesList = MessageHelper.cloneArrayList((ArrayList) o);
				} else {
					nodesList = (ArrayList) o;
				}
				for (Object node : nodesList) {
                    if (node instanceof OMElement) {
                        if (node instanceof SOAPEnvelope) {
                            SOAPEnvelope soapEnvelope = (SOAPEnvelope) node;
                            String soapNamespace = null;

                            if (soapEnvelope.getNamespace() != null) {
                                soapNamespace = soapEnvelope.getNamespace().getNamespaceURI();
                            }
                            if (soapEnvelope.getHeader() == null && soapNamespace != null) {
                                SOAPFactory soapFactory;
                                if (soapNamespace.equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                                    soapFactory = OMAbstractFactory.getSOAP12Factory();
                                } else {
                                    soapFactory = OMAbstractFactory.getSOAP11Factory();
                                }
                                soapFactory.createSOAPHeader(soapEnvelope);
                            }
                            sourceNodeList.add(soapEnvelope);
                        } else {
                            OMElement ele = (OMElement) node;
                            sourceNodeList.add(ele);
                        }
                    } else if (node instanceof OMText) {
                        sourceNodeList.add((OMText)node);
                    }
                }
            } else {
                synLog.error("Invalid source property type.");
            }
        } else if (sourceType == EnrichMediator.INLINE) {
            if (inlineOMNode instanceof OMElement) {
                OMElement inlineOMElement = (OMElement) inlineOMNode;
                if (inlineOMElement.getQName().getLocalPart().equals("Envelope")) {
                    SOAPEnvelope soapEnvelope = getSOAPEnvFromOM(inlineOMElement);
                    if (soapEnvelope != null) {
                        sourceNodeList.add(soapEnvelope);
                    } else {
                        synLog.error("Inline Source is not a valid SOAPEnvelope.");
                    }
                } else {
                    sourceNodeList.add(inlineOMElement.cloneOMElement());
                }
            } else if (inlineOMNode instanceof OMText) {
                sourceNodeList.add(inlineOMNode);
            } else if (inlineKey != null) {
                Object inlineObj = synCtx.getEntry(inlineKey);
                if (inlineObj instanceof OMElement) {
                    if (((OMElement) inlineObj).getQName().getLocalPart().equals("Envelope")) {
                        SOAPEnvelope soapEnvelope = getSOAPEnvFromOM((OMElement) inlineObj);
                        if (soapEnvelope != null) {
                            sourceNodeList.add(soapEnvelope);
                        } else {
                            synLog.error("Specified Resource as Source is not a valid SOAPEnvelope.");
                        }
                    } else {
                        sourceNodeList.add((OMElement) inlineObj);
                    }
                } else if (inlineObj instanceof OMText) {
                    sourceNodeList.add((OMText)inlineObj);
                } else if (inlineObj instanceof String) {
                    sourceNodeList.add(
                            OMAbstractFactory.getOMFactory().createOMText(inlineObj.toString()));
                } else {
                    synLog.error("Specified Resource as Source is not valid.");
                }
            } else {
                synLog.error("Inline Source Content is not valid.");
            }
        }
        return sourceNodeList;
    }

    private SOAPEnvelope getSOAPEnvFromOM(OMElement inlineElement) {
        SOAPFactory soapFactory;
        if (inlineElement.getQName().getNamespaceURI().equals(
                SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
            soapFactory = OMAbstractFactory.getSOAP12Factory();
        } else {
            soapFactory = OMAbstractFactory.getSOAP11Factory();
        }

        StAXSOAPModelBuilder builder = new StAXSOAPModelBuilder(inlineElement.getXMLStreamReader(),
                soapFactory, inlineElement.getQName().getNamespaceURI());
        return builder.getSOAPEnvelope();
    }

    /**
     * This method will evaluate a specified source json element.
     *
     * @param synCtx - Current Message Context
     * @param synLog - Default Logger for the package
     * @return A HashMap with the following keys: <br/>
     * [1] "errorsExistInSrcTag" - holds either true or false <br/>
     * [2] "evaluatedSrcJsonElement" - holds the evaluated Json Element as an Object
     * @throws JaxenException
     */
    public Object evaluateJson(MessageContext synCtx, SynapseLog synLog) throws JaxenException {

        Object object = "";
        String jsonPath = null;
        if (xpath != null) {
            SynapseJsonPath sourceJsonPath = (SynapseJsonPath) this.xpath;
            jsonPath = sourceJsonPath.getJsonPath().getPath();
        }

        org.apache.axis2.context.MessageContext context = ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        if(!JsonUtil.hasAJsonPayload(context)) {
            synLog.error("JSON payload not found in message context");
        }

        switch (sourceType) {
            case EnrichMediator.CUSTOM : {
                assert jsonPath != null : "JSONPath should be non null in case of CUSTOM";
                String jsonString = JsonUtil.jsonPayloadToString(context);
                object = JsonPath.parse(jsonString).read(jsonPath);
                if (!clone) {
                    // when cloning is false, remove the element in JSON path from payload
                    String modifiedJsonString = JsonPath.parse(jsonString).delete(jsonPath).jsonString();
                    try {
                        JsonUtil.getNewJsonPayload(context, modifiedJsonString, true, true);
                    } catch (Exception ex) {
                        synLog.error("Error while setting json payload, when cloning is false");
                    }
                }
                break;
            }
            case EnrichMediator.BODY: {
                object = JsonUtil.jsonPayloadToString(context);
                if (!clone){
                    JsonUtil.removeJsonPayload(context);}
                break;
            }
            case EnrichMediator.INLINE: {
                assert inlineOMNode != null || inlineKey != null : "inlineJSONNode or key shouldn't be null when type is INLINE";
                if (inlineOMNode != null && inlineOMNode instanceof OMText) {
                    object = JsonPath.parse(((OMTextImpl) inlineOMNode).getText()).json();
                } else if (inlineKey != null && !inlineKey.trim().equals("")) {
                    Object inlineObj = synCtx.getEntry(inlineKey);
                    if ((inlineObj instanceof String) && !(((String) inlineObj).trim().equals(""))) {
                        object = JsonPath.parse(((String) inlineObj)).json();
                    } else {
                        synLog.error("Source failed to get inline JSON" + "inlineKey=" + inlineKey);
                    }
                } else {
                    synLog.error("Source failed to get inline JSON" + "inlineJSONNode=" + inlineOMNode + ", inlineKey=" + inlineKey);
                }
                break;
            }
            case EnrichMediator.PROPERTY: {
                assert property != null : "property shouldn't be null when type is PROPERTY";
                Object propertyObject = synCtx.getProperty(property);
                if (propertyObject instanceof String) {
                    String sourceStr = (String) propertyObject;
                    object = sourceStr;
                } else {
                    synLog.error("Invalid source property type");
                }
                break;
            }
            default: {
                synLog.error("Case mismatch for type: " + sourceType);
            }
        }

        return object;
    }

    public SynapsePath getXpath() {
        return xpath;
    }

    public void setXpath(SynapsePath xpath) {
        this.xpath = xpath;
    }

    public int getSourceType() {
        return sourceType;
    }

    public void setSourceType(int sourceType) {
        this.sourceType = sourceType;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public boolean isClone() {
        return clone;
    }

    public void setClone(boolean clone) {
        this.clone = clone;
    }

    public void setInlineOMNode(OMNode inlineOMNode) {
        this.inlineOMNode = inlineOMNode;
    }

    public OMNode getInlineOMNode() {
        return inlineOMNode;
    }

    public String getInlineKey() {
        return inlineKey;
    }

    public void setInlineKey(String inlineKey) {
        this.inlineKey = inlineKey;
    }
}

