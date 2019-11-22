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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.io.InputStream;

public class SynapseJsonPath extends SynapsePath {

    private static final Log log = LogFactory.getLog(SynapseJsonPath.class);

    private String enableStreamingJsonPath = SynapsePropertiesLoader.loadSynapseProperties().
    getProperty(SynapseConstants.STREAMING_JSONPATH_PROCESSING);

    public JsonPath getJsonPath() {
        return jsonPath;
    }

    // Given a json-path this method will return the parent json-path.
    public String getParentPath() {
        String[] array = expression.split("\\.");
        if (array.length > 1) {
            // handle json-path expressions ends with array notation Ex:- $.student.marks[0]
            if (array[array.length - 1].endsWith("]")) {
                array[array.length - 1] = array[array.length - 1].replaceAll("\\[(.*?)\\]", "");
                return StringUtils.join(array, ".");
            } else {
                String[] parent = Arrays.copyOf(array, array.length - 1);
                return StringUtils.join(parent, ".");
            }
        }
        return null;
    }

    public void setJsonPath(JsonPath jsonPath) {
        this.jsonPath = jsonPath;
    }

    private JsonPath jsonPath;

    private boolean isWholeBody = false;

    public SynapseJsonPath(String jsonPathExpression)  throws JaxenException {
        super(jsonPathExpression, SynapsePath.JSON_PATH, log);

        // Set default configuration for Jayway JsonPath
        EIPUtils.setJsonPathConfiguration();

        this.contentAware = true;
        this.expression = jsonPathExpression;
        // Though SynapseJsonPath support "$.", the JSONPath implementation does not support it
        if (expression.endsWith(".")) {
            expression = expression.substring(0, expression.length() - 1);
        }
        jsonPath = JsonPath.compile(expression);
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
        read = formatJsonPathResponse(jsonPath.read(jsonString));
        return (null == read ? "null" : read.toString());
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
                        JsonUtil.getNewJsonPayload(amc, stream, true, true);
                    }
                } else {
                    // Message Already built.
                    stream = JsonUtil.toJsonStream(amc.getEnvelope().getBody().getFirstElement());
                }
                if(stream != null) {
                    return stringValueOf(stream);
                }else{
                    log.warn("Json Payload is empty.");
                    return "";
                }
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
            read = formatJsonPathResponse(jsonPath.read(jsonStream));
            if (log.isDebugEnabled()) {
                log.debug("#stringValueOf. Evaluated JSON path <" + jsonPath.getPath() + "> : <" + (read == null ? null : read.toString()) + ">");
            }
            return (null == read ? "null" : read.toString());
        } catch (IOException e) {
            handleException("Error evaluating JSON Path <" + jsonPath.getPath() + ">", e);
        } catch (Exception e) { // catch invalid json paths that do not match with the existing JSON payload.
            if (log.isDebugEnabled()) {
                log.debug("#stringValueOf. Error evaluating JSON Path <" + jsonPath.getPath()
                        + ">. Returning empty result. Error>>> " + e.getLocalizedMessage());
            }
            return "";
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
    public Object evaluate(Object object) {
        List result = null;
        if (object != null) {
            if (object instanceof MessageContext) {
                MessageContext synCtx = (MessageContext) object;
                result = listValueOf(synCtx);
            } else if (object instanceof String) {
                result = listValueOf(IOUtils.toInputStream(object.toString()));
            }
        }
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
                        JsonUtil.getNewJsonPayload(amc, stream, true, true);
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
     */
    private List listValueOf(final InputStream jsonStream) {
        if (jsonStream == null) {
            return null;
        }
        List result = new ArrayList();
        try {
            Object object = jsonPath.read(jsonStream);
            object = formatJsonPathResponse(jsonPath.read(jsonStream));
            if (object != null) {
                if (object instanceof List && !jsonPath.isDefinite()) {
                    result = (List) object;
                } else if (object instanceof JsonArray) {
                    for (JsonElement element:
                            (JsonArray) object) {
                        result.add(element);
                    }
                } else {
                    result.add(object);
                }
            }
        } catch (IOException e) {
            // catch invalid json paths that do not match with the existing JSON payload.
            // not throwing the exception as done in Xpath
            log.error("AggregateMediator Failed to evaluate correlate expression: " + jsonPath.getPath());
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("#listValueOf. Evaluated JSON path <" + jsonPath.getPath() + "> : <null>.");
        }
        return result;
    }

    /**
     * JayWay json-path response have additional elements like "members"(for objects) and "elements"(for arrays)
     * This method will correct such strings by removing additional elements.
     *
     * @param input input jsonElement.
     * @return corrected jsonObject.
     */
    private Object formatJsonPathResponse(Object input) {
        // Return numeric result of ison-eval() as it is Ex: .length() function
        if (input instanceof Number) {
            return input;
        }
        JsonElement jsonElement = (JsonElement) input;
        if (jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsString();
        } else if(jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("members")) {
                return jsonObject.get("members");
            } else if (jsonObject.has("elements")) {
                return jsonObject.get("elements");
            }
            return jsonObject.toString();
        }
        return jsonElement.isJsonArray() ? jsonElement : null;
    }

    /**
     * Replaces first matching item with a given child object.
     * Updated root object will be return back to the caller
     *
     * @param rootObject
     *            Root JSON Object or Array
     * @param newChild
     *            New jsonObject to replace
     * @return Updated Root Object
     */
    public Object replace(Object rootObject, Object newChild) {
        if (isWholeBody) {
            rootObject = newChild;

        } else {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(rootObject.toString());

            Object attachPathObject = null;

            //this try catch block evaluates whether the attachPath is valid and available in the root Object
            try {
                attachPathObject = formatJsonPathResponse(JsonPath.parse(jsonElement.toString()).read(getJsonPath()));

            } catch (PathNotFoundException e) {
                handleException("Unable to get the attach path specified by the expression " + expression, e);
            }

            if (attachPathObject != null) {
                rootObject =
                        JsonPath.parse(jsonElement.toString()).set(expression, newChild).jsonString();

            }

        }
        return rootObject;
    }

    /**
     * This method will return the boolean value of the jsonpath.
     *
     * @param synCtx message context
     * @return boolean value
     */
    public boolean booleanValueOf(MessageContext synCtx) {
        return Boolean.parseBoolean(this.stringValueOf(synCtx));
    }
}
