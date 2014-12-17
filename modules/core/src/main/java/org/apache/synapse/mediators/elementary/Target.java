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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JSONProviderUtil;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.jaxen.JaxenException;

/**
 * Insert an Axiom element to the current message. The target to insert the OMElement can be
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

    //private SynapseXPath xpath = null;
	private SynapsePath xpath = null;

    private String property = null;

    private int targetType = EnrichMediator.CUSTOM;

    public static final String ACTION_REPLACE = "replace";

    public static final String ACTION_ADD_CHILD = "child";

    public static final String ACTION_ADD_SIBLING = "sibling";

    private String action = ACTION_REPLACE;

    public void insert(MessageContext synContext,
                       ArrayList<OMNode> sourceNodeList, SynapseLog synLog) throws JaxenException {

        if (targetType == EnrichMediator.CUSTOM) {
            assert xpath != null : "Xpath cannot be null for CUSTOM";

            if (sourceNodeList.isEmpty()) {
                synLog.error("Cannot enrich message from an empty source.");
                return;
            }

            Object targetObj = xpath.selectSingleNode(synContext);

            if (targetObj instanceof OMElement) {
                OMElement targetElem = (OMElement) targetObj;
                insertElement(sourceNodeList, targetElem, synLog);
            } else if (targetObj instanceof OMText) {
                OMText targetText = (OMText) targetObj;
                if (sourceNodeList.get(0) instanceof OMText) {
                    if (targetText.getParent() != null) {
                        Object parent = targetText.getParent();
                        if (parent instanceof OMElement) {
                            ((OMElement)parent).setText(((OMText) sourceNodeList.get(0)).getText());
                        }
                    }
                } else if (sourceNodeList.get(0) instanceof OMElement) {
                    Object targetParent = targetText.getParent();
                    if (targetParent instanceof OMElement) {
                        targetText.detach();
                        synchronized (sourceNodeList.get(0)) {
                            ((OMElement)targetParent).addChild(sourceNodeList.get(0));
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
        } else if (targetType == EnrichMediator.BODY) {
            SOAPEnvelope env = synContext.getEnvelope();
            SOAPBody body = env.getBody();

            //getting the first element of the in.message body...
            OMElement e = body.getFirstElement();

            if (e != null) {
            	//sourceNodeList: Message part to be enriched from the in.message, 
            	//e: FirstElement of in.message
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
	                 documentElement = AXIOMUtil.stringToOM((String)propertyObj);
                } catch (Exception e1) {
	                //just ignoring the phaser error 
                }
				if(documentElement != null && action.equals(ACTION_ADD_CHILD)){
					//logic should valid only when adding child elements, and other cases
					//such as sibling and replacement using the else condition
					insertElement(sourceNodeList, documentElement, synLog);
					synContext.setProperty(property, documentElement.getText());  
				}else{
					synContext.setProperty(property, sourceNodeList);  
				}
				
			}else{
			synContext.setProperty(property, sourceNodeList);  
			}
        }
    }
    
    private void insertElement(ArrayList<OMNode> sourceNodeList, OMElement e, SynapseLog synLog) {
        if (action.equals(ACTION_REPLACE)) {
            boolean isInserted = false;
            for (OMNode elem : sourceNodeList) {
                if (elem instanceof OMElement) {
                    e.insertSiblingAfter(elem);
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
     * This method will insert a provided json element to a specified target
     * @param synCtx - Current Message Context
     * @param sourceJsonElement - Evaluated Json Element by the Source
     * @param synLog - Default Logger for the package
     * @throws JaxenException
     */
    public void insertJson(MessageContext synCtx, Object sourceJsonElement, SynapseLog synLog) throws JaxenException {
    	
    	if (targetType == EnrichMediator.CUSTOM) {
    		
    		if(this.xpath != null) {
    		       		
        		SynapseJsonPath targetJsonPath = (SynapseJsonPath)xpath;      		
        		boolean targetPathIsDefinite = targetJsonPath.isPathDefinite();
        		
        		if(targetPathIsDefinite) {
        			/* See if path is valid to be considered 
        			 * If not valid, following line will result in an exception and stop proceeding further 
        			 * This has been added since find() method used by replace(), append() & appendToParent(), 
        			 * does not simply support throwing exceptions for invalid paths */
        			targetJsonPath.exitIfAnErrorExistsInFindingPath(synCtx);
        			/* only if target-path-is-definite and target-path-is-valid, 
        			 * a new element will be considered to be attached */
        			Object currentJsonPayload = null, newJsonPayload = null;
            		currentJsonPayload = EIPUtils.getRootJSONObject((Axis2MessageContext)synCtx);            		
            		
            		if (action.equals(ACTION_REPLACE)) {
            			
            			Object newJsonElement = targetJsonPath.replace(currentJsonPayload, sourceJsonElement);

        				if(newJsonElement instanceof Map || newJsonElement instanceof List) {
        					newJsonPayload = newJsonElement;
                			/* creating the new message payload with enriched json element */
                        	JsonUtil.newJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext(),
                        	                    JSONProviderUtil.objectToString(newJsonPayload), true, true);
                		} else if (newJsonElement != null) {
                			synLog.error("Error executing Target-type 'custom' with action 'replace' : " +
                					"Invalid json element < " + newJsonElement.getClass().getSimpleName() + 
                					" >, to be inserted as the new message payload");
                		} else {
                			synLog.error("Error executing Target-type 'custom' with action 'replace' : " +
                					"Invalid json element < " + newJsonElement + " >, to be inserted " +
                					"as the new message payload");
                		}
            			
                    } else if (action.equals(ACTION_ADD_CHILD)) {
                    
                    	Object targetJsonElement = targetJsonPath.find(currentJsonPayload);
                    	
                    	if(targetJsonElement instanceof List) {
                    		newJsonPayload = targetJsonPath.append(currentJsonPayload, sourceJsonElement);
                    		/* creating the new message payload with enriched json element */
                            JsonUtil.newJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext(),
                            		                JSONProviderUtil.objectToString(newJsonPayload), true, true);
                    	} else if(targetJsonElement instanceof Map) {
                    		
                    		Object key = synCtx.getProperty("ENRICH_TARGET_CHILD_KEY");    		
                    		newJsonPayload = targetJsonPath.appendToObject
                    				(currentJsonPayload, ((Map<?,?>)targetJsonElement), key, sourceJsonElement, false);
                    		/* creating the new message payload with enriched json element */
                            JsonUtil.newJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext(),
                            		                JSONProviderUtil.objectToString(newJsonPayload), true, true);
                  
                    	} else {
                    		synLog.error("Error executing Target-type 'custom' with action 'child' : " +
                					"Targeted json element currently does not hold either a JSON Array " +
                					"or JSON Object to which a new child can be attached");
                    	}                  	
                    	
                    } else if (action.equals(ACTION_ADD_SIBLING)) {

                    	newJsonPayload = targetJsonPath.appendToParent(currentJsonPayload, sourceJsonElement, true);
                    	/* creating the new message payload with enriched json element */
                        JsonUtil.newJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext(),
                                                    JSONProviderUtil.objectToString(newJsonPayload), true, true);
                    }
        		} else {
        			synLog.error("Error executing Target-type 'custom' : Json-path has to be definite");
        		}
        		
    		} else { 			
    			synLog.error("Error executing Target-type 'custom' : Json-path is null & somehow, not set");
    		}
    		
        } else if (targetType == EnrichMediator.BODY) {
        	
        	if (action.equals(ACTION_REPLACE)) {
        		
        		if(sourceJsonElement instanceof Map || sourceJsonElement instanceof List) {
        			Object newJsonPayload = sourceJsonElement;
        			/* creating the new message payload with enriched json element */
                	JsonUtil.newJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext(),
                	                                JSONProviderUtil.objectToString(newJsonPayload), true, true);
        		} else if (sourceJsonElement != null) {
        			synLog.error("Error executing Target-type 'body' with action 'replace' : " +
        					"Invalid source json element < " + sourceJsonElement.getClass().getSimpleName() + 
        					" >, to be inserted as the new message payload");
        		} else {
        			synLog.error("Error executing Target-type 'body' with action 'replace' : " +
        					"Invalid source json element < " + sourceJsonElement + " >, to be inserted " +
        					"as the new message payload");
        		}
            } else if (action.equals(ACTION_ADD_CHILD)) {
            	
            	Object currentJsonPayload = null, newJsonPayload = null;
        		currentJsonPayload = EIPUtils.getRootJSONObject((Axis2MessageContext)synCtx);
        		SynapseJsonPath targetJsonPath = new SynapseJsonPath("$");    
        		
        		/* check if currentJsonPayload is a JSON Array
        		 * if 'yes', attach sourceJsonElement as a child
        		 * if 'not', skip */
        		if(currentJsonPayload instanceof List){
        			      		
        			newJsonPayload = targetJsonPath.append(currentJsonPayload, sourceJsonElement);

        		} else {  /* when currentJsonPayload instanceof Map */
        			
        			Object key = synCtx.getProperty("ENRICH_TARGET_CHILD_KEY");   
            		newJsonPayload = targetJsonPath.appendToObject
            				(currentJsonPayload, ((Map<?,?>)currentJsonPayload), key, sourceJsonElement, false);
        		}
        		
        		/* creating the new message payload with enriched json element */
            	JsonUtil.newJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext(),
            	                                JSONProviderUtil.objectToString(newJsonPayload), true, true);
        			
            } else if (action.equals(ACTION_ADD_SIBLING)) {
            	
            	Object currentJsonPayload = null, newJsonPayload = null;
            	currentJsonPayload = EIPUtils.getRootJSONObject((Axis2MessageContext)synCtx);
            	
            	SynapseJsonPath targetJsonPath = new SynapseJsonPath("$");           	
            	newJsonPayload = targetJsonPath.appendToParent(currentJsonPayload, sourceJsonElement, true);

        		/* creating the new message payload with enriched json element */
            	JsonUtil.newJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext(),
            	                           JSONProviderUtil.objectToString(newJsonPayload), true, true);
            }
        	
        } else if (targetType == EnrichMediator.PROPERTY) {
        	if(property != null && !property.isEmpty()){
            	if (action.equals(ACTION_REPLACE)) {
            		
            		if(sourceJsonElement instanceof Map 
            				|| sourceJsonElement instanceof List 
            					|| sourceJsonElement instanceof String){
            			synCtx.setProperty(property, JSONProviderUtil.objectToString(sourceJsonElement));
            		} else if (sourceJsonElement != null){
            			synCtx.setProperty(property, sourceJsonElement);
            		} else {
            			synCtx.setProperty(property, "null");
            		}
                } else if (action.equals(ACTION_ADD_CHILD)) {
                	/* A property can have OM/String/Number/Boolean type values */
                	Object o = synCtx.getProperty(property);
                	if(o != null){
                		if(o instanceof String){
                			String s = ((String)o);
                			/* check if string may contain a json-array */
                			if(s.startsWith("[") && s.endsWith("]")) {
                				/* if yes, try to convert */
           					 	Object jsonArray = EIPUtils.getRootJSONObject(s);
           					 	if(jsonArray != null){
           					 		/* if not null, 'jsonArray' must now contain the corresponding json array 
           					 		 * Only then, sourceJsonElement can be inserted as a child */
               					 	SynapseJsonPath targetJsonPath = new SynapseJsonPath("$");            		
                            		Object newJsonElement = targetJsonPath.append(jsonArray, sourceJsonElement);
                            		synCtx.setProperty(property, JSONProviderUtil.objectToString(newJsonElement));
           					 	} else {
               					 	synLog.error("Error executing Target-type 'property' with action 'child' : " +
                            			"Original value of property " + property + " does not hold a valid object " +
                            			"representation (should be a JSON Array or JSON Object String) " +
                            			"to take source as a child");
           					 	}
                			} else if(s.startsWith("{") && s.endsWith("}")) {
                				/* if yes, try to convert */
           					 	Object jsonObject = EIPUtils.getRootJSONObject(s);
           					 	if(jsonObject != null){
        					 		/* if not null, 'jsonObject' must now contain the corresponding json object
        					 		 * Only then, sourceJsonElement can be inserted as a child */
            					 	SynapseJsonPath targetJsonPath = new SynapseJsonPath("$");            		
            					 	Object key = synCtx.getProperty("ENRICH_TARGET_CHILD_KEY");   
            					 	Object newJsonElement = targetJsonPath.appendToObject
            	            				(jsonObject, ((Map<?,?>)jsonObject), key, sourceJsonElement, false);
            					 	synCtx.setProperty(property, JSONProviderUtil.objectToString(newJsonElement));
        					 	} else {
            					 	synLog.error("Error executing Target-type 'property' with action 'child' : " +
                         			"Original value of property " + property + " does not hold a valid object " +
                         			"representation (should be a JSON Array or JSON Object String) " +
                         			"to take source as a child");
        					 	}
                			} else {
                				synLog.error("Error executing Target-type 'property' with action 'child' : " +
                            			"Original value of property " + property + " does not hold a valid object " +
                            			"representation (should be a JSON Array or JSON Object String) " +
                            			"to take source as a child");
                			}
                		} else {
                			/* 'else' becomes true, when 'o' is a number or boolean ... */
                			synLog.error("Error executing Target-type 'property' with action 'child' : " +
                        			"Original value of property " + property + " does not hold a valid object " +
                        			"representation (should be a JSON Array or JSON Object String) " +
                        			"to take source as a child");
                		}
                	} else {
                		synLog.error("Error executing Target-type 'property' with action 'child' : " +
                				"Specifed property does not exist");
                	}     	
                	
                } else if (action.equals(ACTION_ADD_SIBLING)) {
                	/* A property can have OM/String/Number/Boolean type values */
                	Object o = synCtx.getProperty(property);
                	if(o != null){ 
						/* Ultimate goal here is to create a JSON Like Array and
						 * store it in the property as a String */
                		if(o instanceof OMElement) {
                			o = JSONProviderUtil.objectToString(((OMElement)o).toString().trim());
                		}
                		ArrayList<Object> newArrayList = new ArrayList<Object>();
                		newArrayList.add(o);
                		if(sourceJsonElement instanceof Map 
                				|| sourceJsonElement instanceof List 
                					|| sourceJsonElement instanceof String){
                			sourceJsonElement = JSONProviderUtil.objectToString(sourceJsonElement);
                		}
                		newArrayList.add(sourceJsonElement);
                		newArrayList.trimToSize();
                		synCtx.setProperty(property, newArrayList.toString());
                		
                	} else {
                		synLog.error("Error executing Target-type 'property' with action 'sibling' : " +
                				"Specifed property does not exist");
                	}
                }
        	} else {
        		synLog.error("Error executing Target-type 'property' : " +
        				"property is null & somehow, not set or empty");
        	}
        }
    }
    
    /* 
     * original:
     * public SynapseXPath getXpath() {return xpath;} 
     */
    public SynapsePath getXpath() {
        return xpath;
    }

    public String getProperty() {
        return property;
    }

    public int getTargetType() {
        return targetType;
    }

    /* 
     * original:
     * public void setXpath(SynapseXPath xpath) {this.xpath = xpath;}
     */
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