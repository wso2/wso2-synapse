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

import org.apache.axiom.om.*;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * The source of the XML node to be stored. The source can be a
 * 1. Property
 * 2. XPath Expression
 * 3. SOAP Envelope
 * 4. SOAP Body
 * <p/>
 * If clone is true a clone will be create and stored from the original content. Otherwise a
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
    //private SynapseXPath xpath = null;
	private SynapsePath xpath = null;
	
    private String property = null;

    private int sourceType = EnrichMediator.CUSTOM;

    private boolean clone = true;

    private OMNode inlineOMNode = null;

    private String inlineKey = null;
    

	public ArrayList<OMNode> evaluate(MessageContext synCtx, SynapseLog synLog) throws JaxenException {

        ArrayList<OMNode> sourceNodeList = new ArrayList<OMNode>();

        if (sourceType == EnrichMediator.CUSTOM) {
            assert xpath != null : "XPath should be not null in case of CUSTOM";

            //following line can be important later to get appropriate json node set...
            List<?> selectedNodeList = xpath.selectNodes(synCtx);
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
     * This method will evaluate a specified source json element
     * @param synCtx - Current Message Context
     * @param synLog - Default Logger for the package
     * @return
     * A HashMap with the following keys:<br/>
     * [1] "errorsExistInSrcTag" - holds either true or false<br/>
     * [2] "evaluatedSrcJsonElement" - holds the evaluated Json Element as an Object
     * @throws JaxenException */
    
    public HashMap<String, Object> evaluateJson(MessageContext synCtx, SynapseLog synLog) throws JaxenException {
    		
		/**
		 * Why a HashMap? If we return simply the evaluated json element
		 * (since we allow null as a valid option in a json), when null is
		 * returned that can be due to a valid reason as well as due to an error
		 * in specified configuration. So to distinguish such occasions, a HashMap with
		 * an execution status has been used instead. */
    	
    	HashMap<String, Object> executionStatus = new HashMap<String, Object>();
    	executionStatus.put("errorsExistInSrcTag", false);
    	executionStatus.put("evaluatedSrcJsonElement", null);
    	
    	/* executionStatus with key 'evaluatedSrcJsonElement' 
    	 * will be finally updated with this to be returned */ 
    	Object sourceJsonElement = null;
    	
    	if (sourceType == EnrichMediator.CUSTOM) {
    		
            if (xpath != null) {
            	SynapseJsonPath sourceJsonPath = (SynapseJsonPath)this.xpath;
            	Object o = sourceJsonPath.evaluate(synCtx);
            	if(o != null) {
            		if (o instanceof List) {
            			if(((List<?>)o).size() == 0) {
                			sourceJsonElement = null;
                		}else if(((List<?>)o).size() == 1) {
                			sourceJsonElement = ((List<?>)o).get(0);
                		} else {
                			sourceJsonElement = o;
                		}
            		}
            	} else {
            		synLog.error("Error executing Source-type 'custom' : Errors exist in obtaining " +
            				"the specified source json element");
                	executionStatus.put("errorsExistInSrcTag", true);
            	}
            } else {
          	
            	synLog.error("Error executing Source-type 'custom' : " +
            			"JSON-Path should not be null when type is CUSTOM");
            	executionStatus.put("errorsExistInSrcTag", true);
            }
            
        } else if (sourceType == EnrichMediator.BODY) {
        	
        	SynapseJsonPath sourceJsonPath = new SynapseJsonPath("$");
        	Object o = sourceJsonPath.evaluate(synCtx);
        	if(o != null) {
        		if (o instanceof List) {
        			if(((List<?>)o).size() == 0) {
            			sourceJsonElement = null;
            		}else if(((List<?>)o).size() == 1) {
            			sourceJsonElement = ((List<?>)o).get(0);
            		} else {
            			sourceJsonElement = o;
            		}
        		}
        	} else {
        		synLog.error("Error executing Source-type 'body' : Errors exist in obtaining " +
        				"the specified source json element");
            	executionStatus.put("errorsExistInSrcTag", true);
        	}
        	
        } else if (sourceType == EnrichMediator.PROPERTY) {
        	
        	if(this.property != null && !this.property.isEmpty()) {
        		/** A property can have OM/String/Number/Boolean type values */
        		Object o = synCtx.getProperty(this.property);
            	if(o != null){
            		if(o instanceof OMElement){
            			/**
            			 * If target type is custom, this will be attached as a string
            			 * If target type is body-replace, this will be considered as an invalid source content
            			 * If target type is property, this will be attached as a string
            			 */
            			sourceJsonElement = ((OMElement)o).toString().trim();
            		}else{
            			/**
            			 * If 'else' condition is true, then the property can contain either:
            			 * [1] A String
            			 * [2] A JsonObject as a string
            			 * [3] A JsonArray as a string
            			 * [4] A Number
            			 * [5] A Boolean
            			 */
            			 if(o instanceof String) {
            				 String s = ((String)o).trim();
            				 /** check if string may contain a json-array or json-object */      				  
            				 if((s.startsWith("{") && s.endsWith("}"))
            						 || (s.startsWith("[") && s.endsWith("]"))) {
            					 /** if yes, try to convert */
            					 sourceJsonElement = EIPUtils.getRootJSONObject(s);
            					 if(sourceJsonElement == null) {
            						 sourceJsonElement = s;
            					 }
            				 } else if (s.startsWith("\"") && s.endsWith("\"")) {
                 				  sourceJsonElement = s.substring(1, s.length()-1);
                 			 } else {
                 				/**
            					  * use-cases of empty and null strings 
            					  * [1] Pointing to an empty string results in a "\"\"" string
            					  * [2] pointing to a null, results in a "null" string
            					  * [3] Pointing to a "null" string, results in a "\"null\"" string
            					  * */
                 				 
                 				  if(("\"null\"").equals(s)) {
                 					 sourceJsonElement = "null";
                 				  } else if (("null").equals(s)) {
                 					 sourceJsonElement = null;
                 				  } else {
                 					 sourceJsonElement = s;
                 				  }            				  
            				  }            				 
            			 } else if (o instanceof Number || o instanceof Boolean) {
            				 sourceJsonElement = o;
            			 } else {
            				 synLog.error("Error executing Source-type 'property' : Invalid source property " +
            				 		"with an unexpected value");
            				 executionStatus.put("errorsExistInSrcTag", true);
            			 }
            		}
            	} else {
            		
            		synLog.error("Error executing Source-type 'property' : Source definition may be poiting " +
            				"to a non-existing property");
            		executionStatus.put("errorsExistInSrcTag", true);
            	}
            } else {
            	
            	synLog.error("Error executing Source-type 'property' : Property name should not be null " +
            			"or empty when type is PROPERTY");
            	executionStatus.put("errorsExistInSrcTag", true);           	
            }

        } else if (sourceType == EnrichMediator.INLINE) {
        	
        	if(this.inlineOMNode != null){
        		if (this.inlineOMNode instanceof OMElement) {
        			/**
        			 * If target type is custom, this will be attached as a string
        			 * If target type is body-replace, this will be considered as an invalid source content
        			 * If target type is property, this will be attached as a string
        			 */
        			sourceJsonElement = ((OMElement)this.inlineOMNode).toString().trim();
        		} else {
        			/**
        			 * If 'else' condition is true, then the in-line text can contain either:
        			 * [1] A String
        			 * [2] A Number as a String
        			 * [3] A Boolean as a String
        			 * [4] A JsonObject as a string
        			 * [5] A JsonArray as a string
        			 */
        			String inlineText = ((OMText)this.inlineOMNode).getText().trim();
        			
        			if((inlineText.startsWith("{") && inlineText.endsWith("}")) 
            					|| (inlineText.startsWith("[") && inlineText.endsWith("]"))) {	
        				/* check if in-line text may contain a json-array or json-object */
            			/* if yes, try to convert */
            			sourceJsonElement = EIPUtils.getRootJSONObject(inlineText);
            			if(sourceJsonElement == null) {
            				sourceJsonElement = inlineText;
            			}
            		} else if(inlineText.startsWith("\"") && inlineText.endsWith("\"")) {
            			/* if inlineText contains beginning-and-ending-double-quotes, 
            			 * it will be considered as a string */
        				sourceJsonElement = inlineText = inlineText.substring(1, inlineText.length()-1);
        			} else {
                		if(Source.isNumeric(inlineText)) {
                			sourceJsonElement = new Double(inlineText);
                		} else if ("true".equals(inlineText.toLowerCase()) 
                					|| "false".equals(inlineText.toLowerCase())) {
                			sourceJsonElement = new Boolean(inlineText);
                		} else if ("null".equals(inlineText.toLowerCase()) || "".equals(inlineText)) {
                			sourceJsonElement = null;
                		} else {
                			sourceJsonElement = inlineText;
                		}
            		}
        		}
        	} else if (this.inlineKey != null && !this.inlineKey.isEmpty()) {
        		Object inlineKeyObj = synCtx.getEntry(this.inlineKey);
        		if(inlineKeyObj != null) {
        			if (inlineKeyObj instanceof OMElement) {
            			/**
            			 * If target type is custom, this will be attached as a string
            			 * If target type is body-replace, this will be considered as an invalid source content
            			 * If target type is property, this will be attached as a string
            			 */
            			sourceJsonElement = ((OMElement)inlineKeyObj).toString().trim();
            		} else {
            			/**
            			 * If 'else' condition is true, then the in-line text can contain either:
            			 * [1] A String
            			 * [2] A Number as a String
            			 * [3] A Boolean as a String
            			 * [4] A JsonObject as a string
            			 * [5] A JsonArray as a string
            			 */
            			String inlineText = ((String)inlineKeyObj).trim();
            				
            			/* check if in-line text may contain a json-array or json-object */           			
                		if((inlineText.startsWith("{") && inlineText.endsWith("}")) 
                					|| (inlineText.startsWith("[") && inlineText.endsWith("]"))) {	
                			/* if yes, try to convert */
                			sourceJsonElement = EIPUtils.getRootJSONObject(inlineText);
                			if(sourceJsonElement == null) {
                				sourceJsonElement = inlineText;
                			}
                		} else if (inlineText.startsWith("\"") && inlineText.endsWith("\"")) {
                			/* if inlineText contains beginning-and-ending-double-quotes, 
                			 * it will be considered as a string */
            				sourceJsonElement = inlineText = inlineText.substring(1, inlineText.length()-1);
            			} else {
                    		if(Source.isNumeric(inlineText)) {
                    			sourceJsonElement = new Double(inlineText);
                    		} else if ("true".equals(inlineText.toLowerCase()) 
                    				|| "false".equals(inlineText.toLowerCase())){
                    			sourceJsonElement = new Boolean(inlineText);
                    		} else if ("null".equals(inlineText.toLowerCase()) || "".equals(inlineText)) {
                    			sourceJsonElement = null;
                    		} else {
                    			sourceJsonElement = inlineText;
                    		}
                		}
            		}
        		}else{
        			sourceJsonElement = null;
        		}
        	} else {
                synLog.error("Error executing Source-type 'inline' : Inline Source Content Definition is not valid");
                executionStatus.put("errorsExistInSrcTag", true);
            }
        }  	
    	
    	executionStatus.put("evaluatedSrcJsonElement", sourceJsonElement);
        return executionStatus;
    }
    
	/**
	 * This method will check if a given string is a number 
	 * representation or not
	 * @param str - Input string
	 * @return a boolean
	 */
    private static boolean isNumeric(String str) {  
    	try{  
    		Double.parseDouble(str);
    	}catch(NumberFormatException e){  
    	    return false;  
    	}  
    	return true;  
    }

    /* 
     * original:
     * public SynapseXPath getXpath() {return xpath;} 
     */
    public SynapsePath getXpath() {
        return xpath;
    }

    /* 
     * original:
     * public void setXpath(SynapseXPath xpath) {this.xpath = xpath;}
     */
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
