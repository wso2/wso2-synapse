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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.AXIOMUtils;
import org.apache.synapse.util.jaxp.SchemaResourceResolver;
import org.apache.synapse.util.resolver.ResourceMap;
import org.apache.synapse.util.resolver.UserDefinedXmlSchemaURIResolver;
import org.apache.synapse.util.xpath.SourceXPathSupport;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.synapse.SynapseConstants.SYNAPSE_VALIDATE_MEDIATOR_REDEPLOYMENT_CACHE_CLEAR;

/**
 * Validate a message or an element against a schema
 * <p/>
 * This internally uses the Xerces2-j parser, which cautions a lot about thread-safety and
 * memory leaks. Hence this initial implementation will create a single parser instance
 * for each unique mediator instance, and re-use it to validate multiple messages - even
 * concurrently - by synchronizing access
 */
public class ValidateMediator extends AbstractListMediator implements FlowContinuableMediator {

    /**
     * A list of property keys, referring to the schemas to be used for the validation
     * key can be static or dynamic(xpath) key
     */
    private List<Value> schemaKeys = new ArrayList<Value>();

    /**
     * A list of property keys, referring to the external schema resources to be used for the validation
     */
    private ResourceMap resourceMap;

    /**
     * An XPath expression to be evaluated against the message to find the element to be validated.
     * If this is not specified, the validation will occur against the first child element of the
     * SOAP body
     */
    private final SourceXPathSupport source = new SourceXPathSupport();

    /**
     * A Map containing features to be passed to the actual validator (Xerces)
     */
    private final List<MediatorProperty> explicityFeatures = new ArrayList<MediatorProperty>();

    /**
     * Lock used to ensure thread-safe creation and use of the above Validator
     */
    private final Object validatorLock = new Object();

    /**
     * The SchemaFactory used to create new schema instances.
     */
    private final SchemaFactory factory = SchemaFactory.newInstance(
            XMLConstants.W3C_XML_SCHEMA_NS_URI);
    /**
     * The JSONSchemaFactory used to create JSONs Schema instance
     */
    private  final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();

    /**
     * to hold the json string of the schema
     */
    private JsonNode jsonSchemaNode;
    /**
     * to hold the Path Expression to be evaluated against the message to find the element to be validated.
     */
    private SynapsePath sourcePath;
    
    /**
     * Concurrent hash map for cached schemas.
     */
    private Map<String, Schema> cachedSchemaMap = new ConcurrentHashMap<String, Schema>();
    
    /**
     * Concurrent hash map for cached json schemas.
     */
    private Map<String, JsonSchema> cachedJsonSchemaMap = new ConcurrentHashMap<String, JsonSchema>();

    /**
     * Whether schema need to cache or not. Default cache every schema.
     */
    private boolean cacheSchema = true;

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public boolean mediate(MessageContext synCtx) {

    	// This is the actual schema instance used to create a new schema
    	Schema cachedSchema = null;
    	JsonSchema cachedJsonSchema = null;
    	
        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        synLog.traceOrDebug("Start : Validate mediator");
        if (synLog.isTraceTraceEnabled()) {
            synLog.traceTrace("Message : " + synCtx.getEnvelope());
        }

        org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        if (JsonUtil.hasAJsonPayload(a2mc)) {
            ProcessingReport report;

            // This JsonSchema used if user decide not to cache the schema. In such a situation jsonSchema will not used.
            JsonSchema uncachedJsonSchema = null;
            JsonNode uncachedJsonSchemaNode = null;
            // flag to check if we need to initialize/re-initialize the schema
            StringBuilder combinedPropertyKey = new StringBuilder();
            StringBuilder cachedJsonSchemaKey = new StringBuilder();

            // if any of the schemas are not loaded, or have expired, load or re-load them
            boolean reCreate = !cacheSchema || isReCreate(synCtx, combinedPropertyKey);
            
            /*
             * Fixing ESBJAVA-4958, Implementation has done assuming that the
             * artifacts are added and removed via a .car file. When a schema is
             * getting removed since the .car file is redeploying, the deleted
             * items will be removed from the map.
             */

            /*
             * Check for the cached schema in the map and if it's available get
             * the cached schema else re initialize the schema
             */
            if (cachedJsonSchemaMap.containsKey(combinedPropertyKey.toString())) {
                cachedJsonSchema = cachedJsonSchemaMap.get(combinedPropertyKey.toString());
            } else {
                reCreate = true;
            }

            // do not re-initialize schema unless required
            synchronized (validatorLock) {
                if (reCreate || cachedJsonSchema == null) {
                    Object jsonSchemaObj = null;
                    for (Value schemaKey : schemaKeys) {
                        // Derive actual key from message context
                        String propName = schemaKey.evaluateValue(synCtx);
                        jsonSchemaObj = synCtx.getEntry(propName);
                        cachedJsonSchemaKey.append(propName);
                    }

                    if (jsonSchemaObj == null) {
                        handleException("Can not find JSON Schema " + cachedJsonSchemaKey.toString(), synCtx);
                    }

                    try {
                        if (jsonSchemaObj instanceof String) {
                            if (cacheSchema) {
                                jsonSchemaNode = JsonLoader.fromString((String) jsonSchemaObj);
                            } else {
                                uncachedJsonSchemaNode = JsonLoader.fromString((String) jsonSchemaObj);
                            }
                        } else if (jsonSchemaObj instanceof OMTextImpl) {
                            //if Schema provides from registry
                            InputStreamReader reader = null;
                            try {
                                reader = new InputStreamReader(((OMTextImpl) jsonSchemaObj).getInputStream());
                                if (cacheSchema) {
                                    jsonSchemaNode = JsonLoader.fromReader(reader);
                                } else {
                                    uncachedJsonSchemaNode = JsonLoader.fromReader(reader);
                                }
                            } finally {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        log.warn("Error while closing registry resource stream. " + e);
                                    }
                                }
                            }
                        } else {
                            handleException("Can not find valid JSON Schema content", synCtx);
                        }
                        if (cacheSchema) {
                            cachedJsonSchema = jsonSchemaFactory.getJsonSchema(jsonSchemaNode);
                            /*
                             * Initially adds the cached schema to the map if it's
                             * not available
                             */
                            if (!cachedJsonSchemaMap.containsKey(cachedJsonSchemaKey.toString())) {
                                cachedJsonSchemaMap.put(cachedJsonSchemaKey.toString(), cachedJsonSchema);
                            /*
                             * Removes the existing cached schema and adds the
                             * new cached schema This is used when editing a
                             * registry resource or when the cache expires
                             */
                            } else if (cachedJsonSchemaMap.containsKey(cachedJsonSchemaKey.toString())) {
                                cachedJsonSchemaMap.remove(cachedJsonSchemaKey.toString());
                                cachedJsonSchemaMap.put(cachedJsonSchemaKey.toString(), cachedJsonSchema);
                            }
                        } else {
                            uncachedJsonSchema = jsonSchemaFactory.getJsonSchema(uncachedJsonSchemaNode);
                        }
                    } catch (ProcessingException | IOException e) {
                        handleException("Error while validating the JSON Schema", e, synCtx);
                    }
                }
            }

            try {
                if (cachedJsonSchema == null && uncachedJsonSchema == null) {
                    handleException("Failed to create JSON Schema Validator", synCtx);
                }
                String jsonPayload = null;
                if (sourcePath != null) {
                    //evaluating
                    if (sourcePath instanceof SynapseJsonPath) {
                        jsonPayload = sourcePath.stringValueOf(synCtx);
                    } else {
                        handleException("Could not find the JSONPath evaluator for Source", synCtx);
                    }
                } else {
                    jsonPayload = JsonUtil.jsonPayloadToString(a2mc);
                }
                if(jsonPayload == null || jsonPayload.length() == 0) {
                    //making empty json string
                    jsonPayload = "{}";
                }
                if (cacheSchema) {
                    report = cachedJsonSchema.validate(JsonLoader.fromString(jsonPayload));
                } else {
                    report = uncachedJsonSchema.validate(JsonLoader.fromString(jsonPayload));
                }
                if (report.isSuccess()) {
                    return true;
                } else {
                    if (synLog.isTraceOrDebugEnabled()) {
                        String msg = "Validation of JSON failed against the given schema(s) " + cachedJsonSchemaKey.toString()
                                     + " with error : " + report + " Executing 'on-fail' sequence";
                        synLog.traceOrDebug(msg);

                        // write a warning to the service log
                        synCtx.getServiceLog().warn(msg);

                        if (synLog.isTraceTraceEnabled()) {
                            synLog.traceTrace("Failed message envelope : " + synCtx.getEnvelope());
                        }
                    }

                    // set error message and detail (stack trace) into the message context
                    Iterator<ProcessingMessage> itrErrorMessages = report.iterator();
                    //there is only one element in the report
                    if (itrErrorMessages.hasNext()) {
                        ProcessingMessage processingMessage = itrErrorMessages.next();
                        String errorMessage = processingMessage.getMessage();
                        synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
                        synCtx.setProperty(SynapseConstants.ERROR_DETAIL, "Error while validating Json message "
                                + errorMessage);
                    }
                    // invokes the "on-fail" sequence of mediator
                    return invokeOnFailSequence(synCtx);
                }
            } catch (ProcessingException | IOException e) {
                String msg = "";
                if (sourcePath != null) {
                    msg = " for JSONPath " + sourcePath.getExpression();
                }
                handleException("Error while validating the JSON Schema" + msg, e, synCtx);
            }
        } else {

            Source validateSrc;
            try {
                // Input source for the validation
                validateSrc = getValidationSource(synCtx, synLog);

            } catch (SynapseException e) {
                /* Catches the exception here to forward to 'on-fail' sequence.
                   The 'on-fail' sequence will get invoked when the given xpath source is not available
                   in the message.
                 */

                String errorMessage = "Error occurred while accessing source element: " + source;

                if (synLog.isTraceOrDebugEnabled()) {
                    String msg = "Error occurred while accessing source element : " + source +
                            "with error : '" + e.getMessage() + "'. Executing 'on-fail' sequence";
                    synLog.traceOrDebug(msg);

                    // write a warning to the service log
                    synCtx.getServiceLog().warn(msg);

                    if (synLog.isTraceTraceEnabled()) {
                        synLog.traceTrace("Failed message envelope : " + synCtx.getEnvelope());
                    }
                }

                synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
                synCtx.setProperty(SynapseConstants.ERROR_DETAIL, e.getMessage());

                // invokes the "on-fail" sequence of mediator
                return invokeOnFailSequence(synCtx);
            }

            StringBuilder combinedPropertyKey = new StringBuilder();
            // flag to check if we need to initialize/re-initialize the schema
            // if any of the schemas are not loaded, or have expired, load or re-load them
            boolean reCreate = !cacheSchema || isReCreate(synCtx, combinedPropertyKey);
            
            /*
             * Fixing ESBJAVA-4958, Implementation has done assuming that the
             * artifacts are added and removed via a .car file. When a schema is
             * getting removed since the .car file is redeploying, the deleted
             * items will be removed from the map.
             */

            /*
             * Check for the cached schema in the map and if it's available get
             * the cached schema else re initialize the schema
             */
            if (cachedSchemaMap.containsKey(combinedPropertyKey.toString())) {
                cachedSchema = cachedSchemaMap.get(combinedPropertyKey.toString());
            } else {
                reCreate = true;
            }

            // This is the reference to the DefaultHandler instance
            ValidateMediatorErrorHandler errorHandler = new ValidateMediatorErrorHandler();

            // This instance used to handle schema not cached scenarios.
            Schema uncachedSchema = null;

            // do not re-initialize schema unless required
            synchronized (validatorLock) {
                if (reCreate || cachedSchema == null) {

                    factory.setErrorHandler(errorHandler);
                    StreamSource[] sources = new StreamSource[schemaKeys.size()];
                    StringBuilder cachedSchemaKey = new StringBuilder();

                    int i = 0;
                    for (Value schemaKey : schemaKeys) {
                        // Derive actual key from message context
                        String propName = schemaKey.evaluateValue(synCtx);
                        Object schemaObject = synCtx.getEntry(propName);
                        if (schemaObject == null) {
                            throw new SynapseException("No Schema is available with the key  : " + propName);
                        }
                        sources[i++] = SynapseConfigUtils.getStreamSource(schemaObject);
                        // Generating a cached schema key
                        cachedSchemaKey.append(propName);
                    }
                    // load the UserDefined SchemaURIResolver implementations
                    try {
                        SynapseConfiguration synCfg = synCtx.getConfiguration();
                        if (synCfg.getProperty(SynapseConstants.SYNAPSE_SCHEMA_RESOLVER) != null) {
                            setUserDefinedSchemaResourceResolver(synCtx);
                        } else {
                            factory.setResourceResolver(
                                    new SchemaResourceResolver(synCtx.getConfiguration(), resourceMap));
                        }
                        if (cacheSchema) {
                            cachedSchema = factory.newSchema(sources);
                            /*
                             * Initially adds the cached schema to the map if it's
                             * not available
                             */
                            if (!cachedSchemaMap.containsKey(cachedSchemaKey.toString())) {
                                cachedSchemaMap.put(cachedSchemaKey.toString(), cachedSchema);
                            /*
                             * Removes the existing cached schema and adds the
                             * new cached schema This is used when editing a
                             * registry resource or when the cache expires
                             */
                            } else if (cachedSchemaMap.containsKey(cachedSchemaKey.toString())) {

                                cachedSchemaMap.remove(cachedSchemaKey.toString());
                                cachedSchemaMap.put(cachedSchemaKey.toString(), cachedSchema);
                            }
                        } else {
                            uncachedSchema = factory.newSchema(sources);
                        }
                    } catch (SAXException e) {
                        handleException("Error creating a new schema objects for " +
                                        "schemas : " + schemaKeys.toString(), e, synCtx);
                    } catch (RuntimeException e) {
                        handleException("Error creating a new schema objects for " +
                                        "schemas : " + schemaKeys.toString(), e, synCtx);
                    }

                    if (errorHandler.isValidationError()) {
                        //reset the errorhandler state
                        errorHandler.setValidationError(false);
                        cachedSchema = null;
                        // Removes the erroneous cached schema from the map
                        if (cachedSchemaMap.containsKey(cachedSchemaKey.toString())) {
                            cachedSchemaMap.remove(cachedSchemaKey.toString());
                        }
                        handleException("Error creating a new schema objects for schemas : "
                                        + schemaKeys.toString(), errorHandler.getSaxParseException(), synCtx);
                    }
                }
            }

            // no need to synchronize, schema instances are thread-safe
            try {
                Validator validator;
                if (cacheSchema) {
                    validator = cachedSchema.newValidator();
                } else {
                    validator = uncachedSchema.newValidator();
                }
                validator.setErrorHandler(errorHandler);

                // perform actual validation
                validator.validate(validateSrc);

                if (errorHandler.isValidationError()) {

                    if (synLog.isTraceOrDebugEnabled()) {
                        String msg = "Validation of element returned by XPath : " + source +
                                     " failed against the given schema(s) " + schemaKeys +
                                     "with error : " + errorHandler.getSaxParseException().getMessage() +
                                     " Executing 'on-fail' sequence";
                        synLog.traceOrDebug(msg);

                        // write a warning to the service log
                        synCtx.getServiceLog().warn(msg);

                        if (synLog.isTraceTraceEnabled()) {
                            synLog.traceTrace("Failed message envelope : " + synCtx.getEnvelope());
                        }
                    }

                    // set error message and detail (stack trace) into the message context
                    synCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                                       errorHandler.getAllExceptions());
                    synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION,
                                       errorHandler.getSaxParseException());
                    synCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                                       FaultHandler.getStackTrace(errorHandler.getSaxParseException()));

                    // invokes the "on-fail" sequence of the mediator
                    return invokeOnFailSequence(synCtx);
                }
            } catch (SAXException e) {
                handleException("Error validating " + source + " element", e, synCtx);
            } catch (IOException e) {
                handleException("Error validating " + source + " element", e, synCtx);
            }
        }
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Validation of element returned by the XPath expression : "
                + source + " succeeded against the given schemas and the current message");
            synLog.traceOrDebug("End : Validate mediator");
        }

        return true;
    }

    private boolean isReCreate(MessageContext synCtx, StringBuilder combinedPropertyKey) {
        boolean reCreate = false;
        for (Value schemaKey : schemaKeys) {
            // Derive actual key from message context
            String propKey = schemaKey.evaluateValue(synCtx);
            // Generating a property key
            combinedPropertyKey.append(propKey);

            Entry dp = synCtx.getConfiguration().getEntryDefinition(propKey);
            if (dp != null && dp.isDynamic()) {
                if (!dp.isCached() || dp.isExpired()) {
                    reCreate = true;       // request re-initialization of Validator
                }
            }
        }
        return reCreate;
    }

    public boolean mediate(MessageContext synCtx,
                           ContinuationState continuationState) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Validate mediator : Mediating from ContinuationState");
        }

        boolean result;
        if (!continuationState.hasChild()) {
            result = super.mediate(synCtx, continuationState.getPosition() + 1);
        } else {
            FlowContinuableMediator mediator =
                    (FlowContinuableMediator) getChild(continuationState.getPosition());

            result = mediator.mediate(synCtx, continuationState.getChildContState());

            if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                ((Mediator) mediator).reportCloseStatistics(synCtx, null);
            }
        }
        return result;
    }

    @Override
    /**
     * Initialize child mediators recursively
     *
     * @param synapseEnvironment synapse environment
     */
    public void init(SynapseEnvironment synapseEnvironment) {
        super.init(synapseEnvironment);
    }

    /**
     * UserDefined schema resource resolver

     * @param synCtx message context
     */
    private void setUserDefinedSchemaResourceResolver(MessageContext synCtx) {
        SynapseConfiguration synCfg = synCtx.getConfiguration();
        String schemaResolverName = synCfg.getProperty(SynapseConstants.SYNAPSE_SCHEMA_RESOLVER);
        Class schemaClazz;
        Object schemaClazzObject;
        try {
            schemaClazz = Class.forName(schemaResolverName);
        } catch (ClassNotFoundException e) {
            String msg =
                    "System could not find the class defined for the specific properties" +
                            "\n SchemaResolverImplementation:" + schemaResolverName;
            handleException(msg, e, synCtx);
            return;
        }

        try {
            schemaClazzObject = schemaClazz.newInstance();

            UserDefinedXmlSchemaURIResolver userDefSchemaResResolver =
                    (UserDefinedXmlSchemaURIResolver) schemaClazzObject;
            userDefSchemaResResolver.init(resourceMap, synCfg, schemaKeys);
            factory.setResourceResolver(userDefSchemaResResolver);
        } catch (Exception e) {
            String msg = "Could not create an instance from the class";
            handleException(msg, e, synCtx);
        }
    }

    /**
     * This method invokes the on-fail sequence of the mediator.
     * @param synCtx the current message for mediation
     * @return true if further mediation should continue
     */
    private boolean invokeOnFailSequence(MessageContext synCtx) {
        ContinuationStackManager.addReliantContinuationState(synCtx, 0, getMediatorPosition());
        boolean result = super.mediate(synCtx);
        if (result) {
            ContinuationStackManager.removeReliantContinuationState(synCtx);
        }
        return result;
    }
    
    /**
     * Get the validation Source for the message context
     *
     * @param synCtx the current message to validate
     * @param synLog  SynapseLog instance
     * @return the validation Source for the current message
     */
    private Source getValidationSource(MessageContext synCtx, SynapseLog synLog) throws SynapseException {
        OMNode validateSource = source.selectOMNode(synCtx, synLog);
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Validation source : " + validateSource.toString());
        }
        return AXIOMUtils.asSource(validateSource);
    }

    /**
     * This class handles validation errors to be used for the error reporting
     */
    private static class ValidateMediatorErrorHandler extends DefaultHandler {

        private boolean validationError = false;
        private SAXParseException saxParseException = null;
        private List<SAXParseException> saxParseExceptionList = new ArrayList<SAXParseException>();

        public void error(SAXParseException exception) throws SAXException {
            validationError = true;
            saxParseException = exception; 
            saxParseExceptionList.add(exception);
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            validationError = true;
            saxParseException = exception;
            saxParseExceptionList.add(exception);
        }

        public void warning(SAXParseException exception) throws SAXException {
        }

        public boolean isValidationError() {
            return validationError;
        }

        public SAXParseException getSaxParseException() {
            return saxParseException;
        }

        public List<SAXParseException> getSaxParseExceptionList() {
            return saxParseExceptionList;
        }

        public String getAllExceptions() {
            StringBuilder errors = new StringBuilder();
            for (SAXParseException e : saxParseExceptionList) {
                errors.append(e.getMessage());
                errors.append("\n");
            }

            return errors.toString();
        }

        /**
         * To set explicitly validation error condition
         * @param validationError  is occur validation error?
         */
        public void setValidationError(boolean validationError) {
            this.validationError = validationError;
        }
    }

    // setters and getters

    /**
     * Get a mediator feature. The common use case is a feature for the
     * underlying Xerces validator
     *
     * @param key property key / feature name
     * @return property string value (usually true|false)
     */
    public Object getFeature(String key) {
        for (MediatorProperty prop : explicityFeatures) {
            if (key.equals(prop.getName())) {
                return prop.getValue();
            }
        }
        return null;
    }

    /**
     * add a feature which need to set for the Schema Factory
     *
     * @param  featureName The name of the feature
     * @param isFeatureEnable should this feature enable?(true|false)
     * @see #getFeature(String)
     * @throws SAXException on an unknown feature
     */
   public void addFeature(String featureName, boolean isFeatureEnable) throws SAXException {
        MediatorProperty mp = new MediatorProperty();
        mp.setName(featureName);
        if (isFeatureEnable) {
            mp.setValue("true");
        } else {
            mp.setValue("false");
        }
        explicityFeatures.add(mp);
        factory.setFeature(featureName, isFeatureEnable);
    }

    /**
     * Set a list of local property names which refer to a list of schemas to be
     * used for validation
     *
     * @param schemaKeys list of local property names
     */
    public void setSchemaKeys(List<Value> schemaKeys) {
        this.schemaKeys = schemaKeys;
    }

    /**
     * Set the given XPath as the source XPath
     * @param source an XPath to be set as the source
     */
    public void setSource(SynapsePath source) {
        this.sourcePath = source;
        if (source instanceof SynapseXPath) {
            this.source.setXPath((SynapseXPath) source);
        }
    }

    /**
     * Set the External Schema ResourceMap that will required for schema validation
     * @param resourceMap  the ResourceMap which contains external schema resources
     */
    public void setResourceMap(ResourceMap resourceMap) {
        this.resourceMap = resourceMap;
    }

    /**
     * Get the source XPath which yields the source element for validation
     * @return the XPath which yields the source element for validation
     */
    public SynapsePath getSource() {
        return this.sourcePath;
    }

    /**
     * The keys for the schema resources used for validation
     * @return schema registry keys
     */
    public List<Value> getSchemaKeys() {
        return schemaKeys;
    }

    /**
     * Features for the actual Xerces validator
     * @return explicityFeatures to be passed to the Xerces validator
     */
    public List<MediatorProperty> getFeatures() {
        return explicityFeatures;
    }

    /**
     *ResourceMap for the external schema resources to be used for the validation
     * @return the ResourceMap with external schema resources
     */
    public ResourceMap getResourceMap() {
        return resourceMap;
    }

    /**
     * Set whether schema need to cache or not.
     *
     * @param cacheSchema cache the schema or not.
     */
    public void setCacheSchema(boolean cacheSchema) {
        this.cacheSchema = cacheSchema;
    }

    /**
     * Check whether to cahce the schemas.
     *
     * @return whether to cache or not.
     */
    public boolean isCacheSchema() {
        return cacheSchema;
    }

    @Override
    public boolean isContentAware() {
        return true;
    }

    @Override
    public void setComponentStatisticsId(ArtifactHolder holder) {
        if (getAspectConfiguration() == null) {
            configure(new AspectConfiguration(getMediatorName()));
        }
        String mediatorId =
                StatisticIdentityGenerator.getIdForFlowContinuableMediator(getMediatorName(), ComponentType.MEDIATOR, holder);
        getAspectConfiguration().setUniqueId(mediatorId);
        StatisticIdentityGenerator.reportingFlowContinuableEndEvent(mediatorId, ComponentType.MEDIATOR, holder);
    }

}
