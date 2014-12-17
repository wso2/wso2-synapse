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
package org.apache.synapse.util.xpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.json.JSONProviderUtil;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.jaxen.JaxenException;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.internal.PathTokenizer;
import com.jayway.jsonpath.spi.impl.JacksonProvider;

public class SynapseJsonPath extends SynapsePath {

    private static final Log log = LogFactory.getLog(SynapseJsonPath.class);

    private String enableStreamingJsonPath = SynapsePropertiesLoader.loadSynapseProperties().
    getProperty(SynapseConstants.STREAMING_JSONPATH_PROCESSING);

    private JsonPath jsonPath;
    PathTokenizer pathTokenizer;
    private static Configuration configuration;
    
    static{
    	/*
    	 * JacksonProvider : set to use Jackson API
    	 * Option : Throw an exception, if path is invalid
    	 */
    	configuration = 
    			Configuration.builder().jsonProvider
    			(new JacksonProvider()).options(EnumSet.allOf(Option.class)).build();
    }

    private boolean isWholeBody = false;

    public SynapseJsonPath(String jsonPathExpression)  throws JaxenException {
        super(jsonPathExpression, SynapsePath.JSON_PATH, log);
        this.contentAware = true;
        this.expression = jsonPathExpression;
        jsonPath = JsonPath.compile(jsonPathExpression);
        pathTokenizer = new PathTokenizer(jsonPath.getPath());
        // Check if the JSON path expression evaluates to the whole payload. If so no point in evaluating the path.
        if ("$".equals(jsonPath.getPath().trim()) || "$.".equals(jsonPath.getPath().trim())) {
            isWholeBody = true;
        }
        this.setPathType(SynapsePath.JSON_PATH);
    }

    public String stringValueOf(final String jsonString) {
        if (jsonString == null) {
            return "";
        }
        if (isWholeBody) {
            return jsonString;
        }
        Object read;
        read = jsonPath.read(jsonString,configuration);
        return (null == read ? "null" : JSONProviderUtil.objectToString(read));
    }

    public String stringValueOf(MessageContext synCtx) {
        org.apache.axis2.context.MessageContext amc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        InputStream stream;
        if (!JsonUtil.hasAJsonPayload(amc) || "true".equals(enableStreamingJsonPath)) {
            try {
                if (null == amc.getEnvelope().getBody().getFirstElement()) {
                    // Get message from PT Pipe.
                    stream = getMessageInputStreamPT(amc);
                    if (stream == null) {
                        stream = JsonUtil.getJsonPayload(amc);
                    } else {
                        JsonUtil.newJsonPayload(amc, stream, true, true);
                    }
                } else {
                    // Message Already built.
                    stream = JsonUtil.toJsonStream(amc.getEnvelope().getBody().getFirstElement());
                }
                return stringValueOf(stream);
            } catch (IOException e) {
                handleException("Could not find JSON Stream in PassThrough Pipe during JSON path evaluation.", e);
            }
        } else {
            stream = JsonUtil.getJsonPayload(amc);
            return stringValueOf(stream);
        }
        return "";
    }

    public String stringValueOf(final InputStream jsonStream) {
        if (jsonStream == null) {
            return "";
        }
        if (isWholeBody) {
            try {
                return IOUtils.toString(jsonStream);
            } catch(IOException e) {
                log.error("#stringValueOf. Could not convert JSON input stream to String.");
                return "";
            }
        }
        Object read;
        try {
            read = jsonPath.read(jsonStream, configuration);
            if (log.isDebugEnabled()) {
                log.debug("#stringValueOf. Evaluated JSON path <" + jsonPath.getPath() + "> : <" + (read == null ? null : JSONProviderUtil.objectToString(read)) + ">");
            }
            return (null == read ? "null" : JSONProviderUtil.objectToString(read));
        } catch (IOException e) {
            handleException("Error evaluating JSON Path <" + jsonPath.getPath() + ">", e);
        } catch (Exception e) { // catch invalid json paths that do not match with the existing JSON payload.
            log.error("#stringValueOf. Error evaluating JSON Path <" + jsonPath.getPath() + ">. Returning empty result. Error>>> " + e.getLocalizedMessage());
            handleException("Error evaluating JSON Path <" + jsonPath.getPath() + ">", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("#stringValueOf. Evaluated JSON path <" + jsonPath.getPath() + "> : <null>.");
        }
        return "";
    }

    public String getJsonPathExpression() {
        return expression;
    }

    public void setJsonPathExpression(String jsonPathExpression) {
        this.expression = jsonPathExpression;
    }
    
    /**
     * Read the JSON Stream and returns a list of objects using the jsonPath.
     */
	@Override
	public Object evaluate(Object object) throws JaxenException {
		List result = null;
		if (object != null){
			if(object instanceof MessageContext) {
				MessageContext synCtx = (MessageContext) object;
    			result = listValueOf(synCtx);
    		}else if(object instanceof String){
    			result = listValueOf(IOUtils.toInputStream(object.toString()));
    		}
		}
		// return Collections.EMPTY_LIST;
		return result;
	}
    
    /* 
     * Read JSON stream and return and object
     */
	private List listValueOf(MessageContext synCtx) {
		org.apache.axis2.context.MessageContext amc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
		InputStream stream;
		if (!JsonUtil.hasAJsonPayload(amc) || "true".equals(enableStreamingJsonPath)) {
			try {
				if (null == amc.getEnvelope().getBody().getFirstElement()) {
					// Get message from PT Pipe.
					stream = getMessageInputStreamPT(amc);
					if (stream == null) {
						stream = JsonUtil.getJsonPayload(amc);
					} else {
						JsonUtil.newJsonPayload(amc, stream, true, true);
					}
				} else {
					// Message Already built.
					stream = JsonUtil.toJsonStream(amc.getEnvelope().getBody().getFirstElement());
				}
				return listValueOf(stream);
			} catch (IOException e) {
				handleException("Could not find JSON Stream in PassThrough Pipe during JSON path evaluation.", e);
			}
		} else {
			stream = JsonUtil.getJsonPayload(amc);
			return listValueOf(stream);
		}
		return null;

	}

	/**
	 * This method always return a List and it will contains a list as the 0th
	 * value, if the path is definite. if the path is not a definite list will
	 * contain multiple element. NULL will return if the path is invalid. Empty
	 * list will return if the path points to null.
	 * 
	 * @param jsonStream
	 * @return
	 */
	private List listValueOf(final InputStream jsonStream) {
        if (jsonStream == null) {
            return null;
        }
        List result = new ArrayList();
        Object object;
        try {
        	object = jsonPath.read(jsonStream, configuration);
            if (log.isDebugEnabled()) {
                log.debug("#listValueOf. Evaluated JSON path <" + jsonPath.getPath()
                          + "> : <" + (object == null ? null : JSONProviderUtil.objectToString(object)) + ">");
            }
            if(object != null){
            	if(object instanceof List && !jsonPath.isPathDefinite()){
            		result = (List)object;
            	} else {
            		result.add(object);
            	}           		
            }
        } catch (IOException e) {
            handleException("Error evaluating JSON Path <" + jsonPath.getPath() + ">", e);
        } catch (Exception e) { // catch invalid json paths that do not match with the existing JSON payload.
            log.error("#listValueOf. Error evaluating JSON Path <" + jsonPath.getPath() + 
                      		">. Returning empty result. Error >>> " + e.getLocalizedMessage());
            handleException("Error evaluating JSON Path <" + jsonPath.getPath() + ">", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("#listValueOf. Evaluated JSON path <" + jsonPath.getPath() + "> : <null>.");
        }
        return result;
    }
	
	/**
	 * Find the matching objects in the given rootObject instance and returns
	 * the same instance without creating clones. So when we modify the result
	 * objects, rootObject also get effected.
	 * 
	 * @param rootObject
	 *            Root JSON object
	 * @return matching objects within the rootObject
	 */
	public Object find(Object rootObject) {
		return jsonPath.find(rootObject);
	}
	
	/**
	 * This method will evaluate the JSON expression and return the first parent
	 * object matching object.
	 * 
	 * @param rootObject
	 *            Root JSON Object or Array to evaluate
	 * @return Parent object of the evaluation result
	 */
	private Object findParent(Object rootObject) {
		Object parent = null;
		if (!isWholeBody) {
			StringBuilder sb = new StringBuilder();
			List<String> fragments = pathTokenizer.getFragments();
			for (int i = 0; i < fragments.size() - 1 ; i++) {
				sb.append(fragments.get(i));
				if (i < fragments.size() - 2)
					sb.append(".");
			}
			if (!"".equals(sb.toString())) {
				JsonPath tempPath = JsonPath.compile(sb.toString());
				parent = tempPath.find(rootObject);
			}
		}
		return parent;
	}
	
	/**
	 * This method will insert given child Object (JSONObject or JSONArray) to
	 * the matching path of the root object. Updated root object will be return
	 * back to the caller.
	 * 
	 * @param rootObject
	 *            Root JSON Object or Array
	 * @param child
	 *            new JSON object to be insert
	 * @return Updated Root Object
	 */
	public Object appendToParent(Object rootObject, Object newChild) {
		return appendToParent(rootObject, newChild, false);
	}
	
	/**
	 * This method will insert given child Object (JSONObject or JSONArray) to
	 * the matching path of the root object. Updated root object will be return
	 * back to the caller.
	 * 
	 * @param rootObject
	 *            Root JSON Object or Array
	 * @param child
	 *            new JSON object to be insert
	 * @param isSibling
	 *            true if the new item add as a sibling
	 * @return Updated Root Object
	 */
	public Object appendToParent(Object rootObject, Object newChild, boolean isSibling) {
		Object parent = findParent(rootObject);
		if (parent == null) {
			if (isWholeBody) {
				if (isSibling || !(rootObject instanceof List)) {
					List array = new ArrayList<Object>();
					array.add(rootObject);
					rootObject = array;
				}
				parent = rootObject;
			}
		}
		rootObject = append(rootObject, parent, newChild, isSibling);
		return rootObject;
	}
	
	/**
	 * This method will insert given child Object (JSONObject or JSONArray) to
	 * the matching parent object of the jsonPath. Updated root object will be
	 * return back to the caller.
	 * 
	 * @param rootObject
	 *            Root JSON Object or Array
	 * @param child
	 *            New item to insert
	 * @return Updated Root Object
	 */
	public Object append(Object rootObject, Object child) {
		Object parent = jsonPath.find(rootObject);
		return append(rootObject, parent, child, false);
	}

	/**
	 * This method will insert given child Object (JSONObject or JSONArray) to
	 * given parent object. Updated root object will be return back to the
	 * caller.
	 * 
	 * @param rootObject
	 *            Root JSON Object or Array
	 * @param parent
	 *            Parent JSON Object or Array
	 * @param child
	 *            New item to insert
	 * @param isSibling
	 *            true if the new item add as a sibling
	 * @return Updated Root Object
	 */
	public Object append(Object rootObject, Object parent, Object child, boolean isSibling) {
		if (parent instanceof List) {
			((List) parent).add(child);
		} else if (parent instanceof Map) {
			Object currentChild = jsonPath.find(rootObject);
			if (parent != null) {
				String skey = getLastToken();
				Map obj = (Map) parent;
				if (obj.containsKey(skey)) {
					Object val = obj.get(skey);
					/*
					 * To verify whether the result of the expression is same as the value we extracted from parent. there can be several objects with same key.
					 */
					if (currentChild == val) {
						rootObject = appendToObject(rootObject, obj, skey, child, isSibling);
						return rootObject;
					}
				}
			}
		}
		return rootObject;
	}

	/**
	 * Append the given child to the parent object with given key. if the key is
	 * exist and if value of the key is an array then new child will added to
	 * the same array, otherwise will create an array and add existing and new
	 * child to it. if the isSibling is true, even the value is an array will
	 * create an new array.
	 * 
	 * @param rootObject
	 *            Root JSON Object or Array
	 * @param parent
	 *            Parent JSON Object or Array
	 * @param key
	 *            key of the new child
	 * @param child
	 *            New child object to append
	 * @param isSibling
	 *            true if the new item add as a sibling
	 * @return Updated root object
	 */
	public Object appendToObject(Object rootObject, Map parent, Object key, Object child, boolean isSibling) {
		if (key == null)
			key = "default_key";
		if (parent != null) {
			if (parent.containsKey(key)) {
				Object existingValue = parent.get(key);
				if (!isSibling && existingValue instanceof List) {
					List array = (List) existingValue;
					array.add(child);
				} else {
					List array = new ArrayList();
					array.add(existingValue);
					array.add(child);
					parent.put(key, array);
				}
			} else {
				parent.put(key, child);
			}
		}
		return rootObject;
	}

	/**
	 * This method will remove given child object from the given parent object.
	 * Updated rootObject will be return back to the caller
	 * 
	 * @param rootObject
	 *            Root JSON Object or Array
	 * @param parent
	 *            Parent JSON Object or Array
	 * @param child
	 *            item to remove
	 * @return Updated Root Object
	 */
	public Object remove(Object rootObject, Object parent, Object child) {
		if (parent instanceof List) {
			((List) parent).remove(child);
		} else if (parent instanceof Map) {
			String skey = getLastToken();
			Map parentMap = (Map) parent;
			if (parentMap.containsKey(skey)) {
				Object val = parentMap.get(skey);
				if (child == val) {
					parentMap.remove(skey);
					return rootObject;
				}
			}
		}
		return rootObject;
	}

	/**
	 * This method will be replace first matching item with given child object.
	 * Updated root object will be return back to the caller
	 * 
	 * @param rootObject
	 *            Root JSON Object or Array
	 * @param newChild
	 *            New item to replace
	 * @return Updated Root Object
	 */
	public Object replace(Object rootObject, Object newChild) {
		if(isWholeBody){
			rootObject = newChild;
		}else{
    		Object child = jsonPath.find(rootObject);
    		Object parent = findParent(rootObject);
    		if (parent instanceof List) {
    			((List) parent).remove(child);
    			((List) parent).add(newChild);
    		} else if (parent instanceof Map) {
    			String skey = getLastToken();
    			Map parentMap = (Map) parent;
    			if (parentMap.containsKey(skey)) {
    				Object val = parentMap.get(skey);
    				if (child == val) {
    					parentMap.put(skey, newChild);
    					return rootObject;
    				}
    			}
    		}
		}
		return rootObject;
	}

	/**
	 * Return the last token of the jsonPath instance. If the jsonPath is
	 * $.student.name, name will be the result. this is useful to identify the
	 * key of the JSON objects
	 * 
	 * @return last token of the jsonPath.
	 */
	private String getLastToken() {
		/*PathTokenizer tokenizer = new PathTokenizer(jsonPath.getPath());
		return tokenizer.getFragments().get(tokenizer.getFragments().size() - 1);*/
		return pathTokenizer.getFragments().get(pathTokenizer.getFragments().size() - 1);
	}
	
	/**
     * This method will return the corresponding JSON Element in a HashMap,
     * provided that a json stream of the payload is input.
     * Resulting HashMap will contain values for the following keys: <br/>   
     * [1] "errorsExistInReadingStream" - Boolean, <br/>
     * [2] "pathIsValid" - String (possible values: "yes", "no", "cannot-decide"), <br/>
     * [3] "evaluatedJsonElement" - Object
     * @param Json stream of the message payload
     * @return A HashMap as stated above
     */
    public HashMap<String, Object> getJsonElement(final InputStream jsonStream){
       
        HashMap<String, Object> executionStatus = new HashMap<String, Object>();
       
        /* Initializing execution status */
        executionStatus.put("errorsExistInReadingStream", false);
        executionStatus.put("pathIsValid", "cannot-decide");
        executionStatus.put("evaluatedJsonElement", null);
       
        boolean anIOExceptionOccured = false;
        boolean anExceptionOccured = false;
       
        try {
            Object o = jsonPath.read(jsonStream);
            executionStatus.put("evaluatedJsonElement", o);
            if (log.isDebugEnabled()) {
                log.debug("#getJsonElement. Evaluated JSON path <" + jsonPath.getPath() +
                          "> : <" + (o == null ? null : JSONProviderUtil.objectToString(o)) + ">");
            }
        } catch (IOException e) {
            handleException("#getJsonElement. Error evaluating JSON Path <" + jsonPath.getPath() + ">", e);
            executionStatus.put("errorsExistInReadingStream", true);
            anIOExceptionOccured = true;
        } catch (Exception e) {
            // catch invalid json paths that do not match with the existing JSON payload.
            log.error("#getJsonElement. Error evaluating JSON Path <" + jsonPath.getPath() +
                      ">. Returning empty result. Error>>> " + e.getLocalizedMessage());
            anExceptionOccured = true;
        }
       
        if(!anIOExceptionOccured && anExceptionOccured) {
            executionStatus.put("pathIsValid", "no");
        } else if (!anIOExceptionOccured && !anExceptionOccured) {
            executionStatus.put("pathIsValid", "yes");
        }
        return executionStatus;
    }


    /**
     * This method will return the corresponding JSON Element in a HashMap,
     * provided that a message context is input.
     * Resulting HashMap will contain values for the following keys: <br/>   
     * [1] "errorsExistInReadingStream" - Boolean, <br/>
     * [2] "pathIsValid" - String (possible values: "yes", "no", "cannot-decide"), <br/>
     * [3] "evaluatedJsonElement" - Object
     * @param Message Context
     * @return A HashMap as stated above
     */
	public HashMap<String, Object> getJsonElement(MessageContext synCtx) {

		InputStream jsonStream = null;
		org.apache.axis2.context.MessageContext amc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
		jsonStream = JsonUtil.getJsonPayload(amc);

		return this.getJsonElement(jsonStream);
	}


    /**
     * This method will stop executing the calling function in case of an exception
     * due to an error in finding the specified path
     * @param synCtx Message Context
     */
    public void exitIfAnErrorExistsInFindingPath(MessageContext synCtx){
        if(isWholeBody){
            return;
        } else {             
        	InputStream jsonStream = null;
    		org.apache.axis2.context.MessageContext amc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
    		jsonStream = JsonUtil.getJsonPayload(amc);
        	try {
	            this.jsonPath.read(jsonStream, configuration);
            } catch (IOException e) {
            	handleException("Error evaluating JSON Path <" + jsonPath.getPath() + ">", e);
            } catch (Exception e) {
            	handleException("Error evaluating JSON Path <" + jsonPath.getPath() + ">", e);
            }
        	return;
        }      
    }
    
    public boolean isPathDefinite(){
        return jsonPath.isPathDefinite();
    }
}