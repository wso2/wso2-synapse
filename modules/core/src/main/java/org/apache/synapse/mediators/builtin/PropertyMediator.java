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

package org.apache.synapse.mediators.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.axis2.context.OperationContext;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.util.JavaUtils;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.util.MediatorPropertyUtils;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The property mediator would save(or remove) a named property as a local property of
 * the Synapse Message Context or as a property of the Axis2 Message Context or
 * as a Transport Header.
 * Properties set this way could be extracted through the XPath extension function
 * "synapse:get-property(scope,prop-name)"
 */

public class PropertyMediator extends AbstractMediator {

    /** The Name of the property  */
    private String name = null;
    /** The DynamicNameValue of the property if it is dynamic  */
    private Value dynamicNameValue = null;
    /** The Value to be set  */
    private Object value = null;
    /** The data type of the value */
    private String type = null;
    /** The XML value to be set */
    private OMElement valueElement = null;
    /** The XPath expr. to get value  */
    private SynapsePath expression = null;
    /** The scope for which decide properties where to go*/
    private String scope = null;
    /** The Action - set or remove */
    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;
    /** Set the property (ACTION_SET) or remove it (ACTION_REMOVE). Defaults to ACTION_SET */
    private int action = ACTION_SET;

    /** Regualar expresion pattern to be evaluated over the property value.
     * Resulting match string will be applied to the property */
    private Pattern pattern;

    /** A pattern can be matched to several parts of the value. Which one to choose.*/
    private int group = 0;

    /** Define the Content type of the resource **/
    public static final String CONTENT_TYPE = "text/plain";

    private static final String EMPTY_CONTENT = "";

    /** To keep the Property Value for tracing **/
    private String propertyValue = null;

    /**
     * Sets a property into the current (local) Synapse Context or into the Axis Message Context
     * or into Transports Header and removes above properties from the corresponding locations.
     *
     * @param synCtx the message context
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Property mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        String name = this.name;
        //checks the name attribute value is a dynamic or not
        if (dynamicNameValue != null) {
            name  = dynamicNameValue.evaluateValue(synCtx);
            if (StringUtils.isEmpty(name)) {
                log.warn("Evaluated value for " + this.name + " is empty");
            }
        }

        if (action == ACTION_SET) {

            Object resultValue = getResultValue(synCtx);

            // if the result value is a String we can apply a reguar expression to
            // choose part of it
            if (resultValue instanceof String && pattern != null) {
                resultValue = getMatchedValue((String) resultValue, synLog);
            }

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Setting property : " + name + " at scope : " +
                    (scope == null ? "default" : scope) + " to : " + resultValue + " (i.e. " +
                    (value != null ? "constant : " + value :
                          "result of expression : " + expression) + ")");
            }

            if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
                //Setting property into the  Synapse Context
				if (resultValue != null && resultValue instanceof OMElement) {
					((OMElement) resultValue).build();
				}
				
                synCtx.setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_TRACE.equals(scope)) {
                //Setting property value into the propertyValue variable for tracing purposes
                if (resultValue != null ) {
                    if (resultValue instanceof OMElement) {
                        ((OMElement) resultValue).build();
                    }
                    //Converted to string since only used to display as a span tag
                    propertyValue = resultValue.toString();
                }
            } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                //Setting property into the  Axis2 Message Context
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.setProperty(name, resultValue);
                MediatorPropertyUtils.handleSpecialProperties(name, resultValue, axis2MessageCtx);

            } else if (XMLConfigConstants.SCOPE_CLIENT.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                //Setting property into the  Axis2 Message Context client options
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.getOptions().setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                //Setting Transport Headers
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                Object headers = axis2MessageCtx.getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                /*
                 * if null is passed as header value at AbstractHTTPSender in Axis2 when header
                 * value is read causes a null-pointer issue
                 */
                if (resultValue == null) {
                    resultValue = "";
                }

                if (headers != null && headers instanceof Map) {
                    Map headersMap = (Map) headers;
                    headersMap.put(name, resultValue);
                }
                if (headers == null) {
                    Map<String, Object> headersMap = new TreeMap<>(new Comparator<String>() {
                        public int compare(String o1, String o2) {
                            return o1.compareToIgnoreCase(o2);
                        }
                    });
                    headersMap.put(name, resultValue);
                    axis2MessageCtx.setProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                            headersMap);
                }
            }else if(XMLConfigConstants.SCOPE_OPERATION.equals(scope)
                    && synCtx instanceof Axis2MessageContext){
            	//Setting Transport Headers
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2smc.getAxis2MessageContext().getOperationContext().setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_REGISTRY.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {

                String[] args = name.split("@");
                String path = "";
                String propertyName = "";
                Registry registry = synCtx.getConfiguration().getRegistry();

                // If the name argument consistent with a @ separated property name then an empty resource is added
                // with the property mentioned and the value as its value
                if (args.length == 1){
                    path = args[0];
                    registry.newNonEmptyResource(path, false, CONTENT_TYPE, resultValue.toString(), propertyName);
                } else if (args.length == 2) {
                    path = args[0];
                    propertyName = args[1];
                    registry.newNonEmptyResource(path, false, CONTENT_TYPE, resultValue.toString(), propertyName);
                    registry.updateResource(path, EMPTY_CONTENT);
                }
            } else if (XMLConfigConstants.SCOPE_SYSTEM.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                if (resultValue != null ) {
                    System.setProperty(name, resultValue.toString());
                }
            } else if (XMLConfigConstants.SCOPE_ANALYTICS.equals(scope)
                && synCtx instanceof Axis2MessageContext) {
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                if (resultValue != null ) {
                    axis2smc.setAnalyticsMetadata(name, resultValue);
                }
            }

        } else {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Removing property : " + name +
                    " (scope:" + (scope == null ? "default" : scope) + ")");
            }

            if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
                //Removing property from the  Synapse Context
                Set pros = synCtx.getPropertyKeySet();
                if (pros != null) {
                    pros.remove(name);
                }

            } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)
                && synCtx instanceof Axis2MessageContext) {
                
                //Removing property from the Axis2 Message Context
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.removeProperty(name);

            } else if (XMLConfigConstants.SCOPE_CLIENT.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {

                //Removing property from the Axis2-client Message Context
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                //Property value is set to null since axis2MessageCtx.getOptions()
                //does not have an option to remove properties
                axis2MessageCtx.getOptions().setProperty(name, null);

            } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                // Removing transport headers
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                Object headers = axis2MessageCtx.getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                if (headers != null && headers instanceof Map) {
                    Map headersMap = (Map) headers;
                    headersMap.remove(name);
                } else {
                    synLog.traceOrDebug("No transport headers found for the message");
                }
            } else if (XMLConfigConstants.SCOPE_OPERATION.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                // Removing operation scope headers
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                OperationContext axis2oc = axis2MessageCtx.getOperationContext();
                axis2oc.removeProperty(name);
            } else if (XMLConfigConstants.SCOPE_ANALYTICS.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                axis2smc.removeAnalyticsMetadata(name);
            }
        }
        synLog.traceOrDebug("End : Property mediator");

        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(String value) {
        setValue(value, null);
    }

    /**
     * Set the value to be set by this property mediator and the data type
     * to be used when setting the value. Accepted type names are defined in
     * XMLConfigConstants.DATA_TYPES enumeration. Passing null as the type
     * implies that 'STRING' type should be used.
     *
     * @param value the value to be set as a string
     * @param type the type name
     */
    public void setValue(String value, String type) {
        this.type = type;
        // Convert the value into specified type
        this.value = convertValue(value, type);
    }

    public String getType() {
        return type;
    }

    public OMElement getValueElement() {
        return valueElement;
    }

    public void setValueElement(OMElement valueElement) {
        this.valueElement = valueElement;
    }

    public SynapsePath getExpression() {
        return expression;
    }

    public void setExpression(SynapsePath expression) {
        setExpression(expression, null);
    }

    public void reportCloseStatistics(MessageContext messageContext, Integer currentIndex) {
        CloseEventCollector
                .closeEntryEvent(messageContext, getMediatorName(), ComponentType.MEDIATOR, currentIndex,
                        isContentAltering(), propertyValue);
    }

    public void setExpression(SynapsePath expression, String type) {
        this.expression = expression;
        // Save the type information for now
        // We need to convert the result of the expression into this type during mediation
        // A null type would imply 'STRING' type
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    private Object getResultValue(MessageContext synCtx) {
        if (value != null) {
            return value;
        } else if (valueElement != null) {
            //Need to clone to prevent same reference sharing accross all requests
            return valueElement.cloneOMElement();
        } else {
            if(expression != null) {
                return convertValue(expression.stringValueOf(synCtx), type);
            }
        }

        return null;
    }

    private Object convertValue(String value, String type) {
        if (type == null) {
            // If no type is set we simply return the string value
            return value;
        }

        try {
            XMLConfigConstants.DATA_TYPES dataType = XMLConfigConstants.DATA_TYPES.valueOf(type);
            switch (dataType) {
                case BOOLEAN    : return JavaUtils.isTrueExplicitly(value);
                case DOUBLE     : return Double.parseDouble(value);
                case FLOAT      : return Float.parseFloat(value);
                case INTEGER    : return Integer.parseInt(value);
                case LONG       : return Long.parseLong(value);
                case OM         : return buildOMElement(value);
                case SHORT      : return Short.parseShort(value);
                case JSON       : return buildJSONElement(value);
                default         : return value;
            }
        } catch (IllegalArgumentException e) {
            String msg = "Unknown type : " + type + " for the property mediator or the " +
                    "property value cannot be converted into the specified type.";
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    /**
     * Apply the Regular expression on to the String value and choose the matching group.
     * If a matching not found same string passed in to this method will be returned.
     * If a pattern is not specified same String passed to this method will be returned.
     *
     * @param value String value against the regular expression is matched
     * @param synLog log
     * @return the matched string group
     */
    private String getMatchedValue(String value, SynapseLog synLog) {

        String matchedValue = value;
        // if we cannot find a match set the value to empty string
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            
            if (matcher.groupCount() >= group) {
                matchedValue = matcher.group(group);
            } else {
                if (synLog.isTraceOrDebugEnabled()) {
                    String msg = "Failed to get a match for regx : " +
                            pattern.toString() + " with the property value :" +
                            value + " for group :" + group;
                    synLog.traceOrDebug(msg);
                }
                //Returns an empty string as the number of capturing groups in this matcher's pattern is not satisfied.
                return "";
            }
            
        } else {
            if (synLog.isTraceOrDebugEnabled()) {
                String msg = "Unable to find a match for regx : " +
                        pattern.toString() + " with the property value :" + value;
                synLog.traceOrDebug(msg);
            }
            return ""; //if not matched ideally should return empty string
        }
        
        return matchedValue;
    }

    @Override
    public boolean isContentAware() {
        boolean contentAware= false;
    	if (expression != null) {
            contentAware = expression.isContentAware();
        }

        if (dynamicNameValue != null && dynamicNameValue.getExpression() != null) {
            contentAware = contentAware || dynamicNameValue.getExpression().isContentAware();
        }
        
    	if (XMLConfigConstants.SCOPE_AXIS2.equals(scope) ) {
            // the logic will be determine the contentaware true
            if (org.apache.axis2.Constants.Configuration.MESSAGE_TYPE.equals(name)
                    || PassThroughConstants.DISABLE_CHUNKING.equals(name)
                    || PassThroughConstants.FORCE_HTTP_1_0.equals(name)) {
                contentAware = true;
            }
        } else if(XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)){
        	 //the logic will be determine the contentaware true
            if(HTTP.CONTENT_ENCODING.equals(name)){
            	contentAware =true;
            }
        }
        return contentAware;
    }

    private OMElement buildOMElement(String xml) {
        // intentionally building the resulting OMElement. See ESBJAVA-3478.
        if (xml == null) {
            return null;
        }
        OMElement result = SynapseConfigUtils.stringToOM(xml);
        result.buildWithAttachments();
        return result;
    }

    private JsonElement buildJSONElement(String jsonPayload) {
        JsonParser jsonParser = new JsonParser();
        try {
            return jsonParser.parse(jsonPayload);
        } catch (JsonSyntaxException ex) {
            // Enclosing using quotes due to the following issue
            // https://github.com/google/gson/issues/1286
            String enclosed = "\"" + jsonPayload + "\"";
            try {
                return jsonParser.parse(enclosed);
            } catch (JsonSyntaxException e) {
                // log the original exception and discard the new exception
                log.error("Malformed JSON payload : " + jsonPayload, ex);
                return null;
            }
        }
    }

    @Override public String getMediatorName() {
        return super.getMediatorName() + ":" + name;
    }

    /**
     * Setter for the Value of the Name attribute when it has a dynamic value.
     *
     * @param nameValue Value of the dynamic name value
     */
    public void setDynamicNameValue(Value nameValue) {
        this.dynamicNameValue = nameValue;
    }
}
