/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.synapse.mediators.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.PathNotFoundException;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.util.collectors.CsvCollector;
import org.apache.synapse.mediators.util.collectors.JsonArrayCollector;
import org.apache.synapse.mediators.util.collectors.JsonObjectCollector;
import org.apache.synapse.mediators.util.exceptions.SimpleMessageContextException;
import org.apache.synapse.util.PayloadHelper;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.jaxen.JaxenException;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SimpleMessageContext {

    private final MessageContext messageContext;
    private final JsonParser jsonParser;
    private final Gson gson;

    private static final Log log = LogFactory.getLog(SimpleMessageContext.class);

    public SimpleMessageContext(MessageContext messageContext) {

        this.messageContext = messageContext;
        gson = new Gson();
        jsonParser = new JsonParser();
    }

    /**
     * Get Stream of IndexedElement of JsonElement from the payload of Json Array
     * If payload is not a valid Json Array, gives a Stream with empty array
     *
     * @return Stream of IndexedJsonElement
     */
    public Stream<IndexedElement<JsonElement>> getJsonArrayStreamWithIndex() {

        JsonArray jsonArray = getJsonArray();
        return getJsonArrayStreamWithIndex(jsonArray);
    }

    /**
     * Get Stream of IndexedElement of JsonElement from given JsonArray
     *
     * @param jsonArray JsonArray to convert to Stream
     * @return Stream of IndexedJsonElement
     */
    public Stream<IndexedElement<JsonElement>> getJsonArrayStreamWithIndex(JsonArray jsonArray) {

        return IntStream.range(0, jsonArray.size())
                .mapToObj(i -> new IndexedElement<>(i, jsonArray.get(i)));
    }

    /**
     * Get Stream of JsonElement if the matching part of payload for given json path is a valid Json Array.
     * If not, gives an Stream of empty array
     *
     * @param jsonPath Json path String to apply on payload
     * @return JsonElement Stream
     */
    public Stream<JsonElement> getJsonArrayStream(String jsonPath) {

        JsonElement jsonPathResult = getJsonElement(jsonPath);

        JsonArray jsonArrayToStream;
        if (jsonPathResult.isJsonArray()) {
            jsonArrayToStream = jsonPathResult.getAsJsonArray();
        } else {
            log.error("Error converting data : not a valid Json Array");
            jsonArrayToStream = new JsonArray();
        }

        return getJsonArrayStream(jsonArrayToStream);
    }

    /**
     * Get Stream of JsonElement from given JsonArray
     *
     * @param jsonArray JsonArray to convert to Stream
     * @return Stream of JsonElement
     */
    public Stream<JsonElement> getJsonArrayStream(JsonArray jsonArray) {

        return StreamSupport.stream(jsonArray.spliterator(), false);
    }

    /**
     * Get the payload as a Stream of JsonElements if the payload is a valid Json array
     * or gives Stream of empty json array
     *
     * @return Stream of JsonElement
     */
    public Stream<JsonElement> getJsonArrayStream() {

        JsonArray jsonArray = getJsonArray();
        return getJsonArrayStream(jsonArray);
    }

    /**
     * Get an Indexed Object stream for the Json payload after applying given json path.
     * If payload is not valid or the json path does not match,
     * gives a stream created with an empty Json object
     *
     * @param jsonPath json path string to apply
     * @return Indexed Object stream
     */
    public Stream<IndexedEntry> getIndexedJsonObjectStream(String jsonPath) {

        JsonObject jsonObjectToStream = getJsonObject(jsonPath);
        return getJsonObjectStreamWithIndex(jsonObjectToStream);
    }

    /**
     * Get an Object stream for the Json payload after applying given json path.
     * If payload is not valid or the json path does not match,
     * gives a stream created with an empty Json object
     *
     * @param jsonPath json path string to apply
     * @return Object stream
     */
    public Stream<Map.Entry<String, JsonElement>> getJsonObjectStream(String jsonPath) {

        JsonObject jsonObjectToStream = getJsonObject(jsonPath);
        return getJsonObjectStream(jsonObjectToStream);
    }

    /**
     * Get JsonObject from json path applied to payload
     * If result is invalid then return an empty JsonObject
     *
     * @param jsonPath json path string to apply
     * @return result JsonObject
     */
    public JsonObject getJsonObject(String jsonPath) {

        JsonElement jsonPathResult = getJsonElement(jsonPath);

        JsonObject jsonObject;
        if (jsonPathResult.isJsonArray()) {
            jsonObject =
                    jsonParser.parse(jsonPathResult.getAsJsonArray().get(0).toString()).getAsJsonObject();
        } else {
            jsonObject = jsonPathResult.getAsJsonObject();
        }
        return jsonObject;
    }

    /**
     * Get the given JsonObject as a Stream of IndexedEntry. IndexedEntry includes the position of the parameter as
     * index
     *
     * @param jsonObject JsonObject to convert as a Stream
     * @return Stream of IndexedEntry
     */
    public Stream<IndexedEntry> getJsonObjectStreamWithIndex(JsonObject jsonObject) {

        return IntStream.range(0, jsonObject.entrySet().size())
                .mapToObj(i -> new IndexedEntry(i, jsonObject.entrySet().iterator().next()));
    }

    /**
     * Get the given JsonObject as a stream of Object entries (key, value)
     *
     * @param jsonObject JsonObject to convert as a Stream
     * @return Stream of Map.Entry with object attribute name as the key
     * and attribute value as value
     */
    public Stream<Map.Entry<String, JsonElement>> getJsonObjectStream(JsonObject jsonObject) {

        return jsonObject.entrySet().stream();
    }

    /**
     * Get payload as a stream of Object entries (key, value)
     * If payload is not a valid JsonObject then, empty JsonObject would be given as a stream
     *
     * @return Stream of Map.Entry with object attribute name as the key
     * and attribute value as value
     */
    public Stream<Map.Entry<String, JsonElement>> getJsonObjectStream() {

        JsonObject jsonObject = getJsonObject();
        return getJsonObjectStream(jsonObject);
    }

    /**
     * Give the Json Payload as a JsonArray
     * If payload is not a valid Json Array, gives an empty JsonArray
     *
     * @return payload as JsonArray
     */
    public JsonArray getJsonArray() {

        JsonElement jsonElement = getJsonElement();
        if (jsonElement.isJsonArray()) {
            return jsonElement.getAsJsonArray();
        } else {
            log.error("Error converting data : not a valid Json Array");
            return new JsonArray();
        }
    }

    /**
     * Give the Json payload as a JsonObject
     * If payload is null or not the Json content type, then give an empty JsonObject
     *
     * @return payload as a JsonObject
     */
    public JsonObject getJsonObject() {

        JsonElement jsonElement = getJsonElement();
        if (jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        } else {
            log.error("Error converting data : not a valid Json Object");
            return new JsonObject();
        }
    }

    /**
     * Apply given jsonPath to the context JsonPayload and return result as a JsonElement
     * If json path is not valid, gives an empty json object
     *
     * @param jsonPath json path string to apply
     * @return matching json content as JsonElement
     */
    public JsonElement getJsonElement(String jsonPath) {

        try {
            SynapseJsonPath synapseJsonPath = new SynapseJsonPath(jsonPath);
            Object evaluationResult = synapseJsonPath.evaluate(messageContext);
            return gson.toJsonTree(evaluationResult);
        } catch (JaxenException | PathNotFoundException e) {
            log.error("Error converting data", e);
            return new JsonObject();
        }
    }

    /**
     * Give the Json payload as a JsonElement
     * If payload is null or not the Json content type, then give an empty Json Object as JsonElement
     *
     * @return payload body as JsonElement
     */
    public JsonElement getJsonElement() {

        String jsonPayloadString = getJsonString();
        return jsonParser.parse(jsonPayloadString);
    }

    /**
     * Give the String representation of the Json payload
     * If payload is null or not the Json content type, then gives an empty Json object
     *
     * @return payload string
     */
    public String getJsonString() {

        return JsonUtil.jsonPayloadToString(((Axis2MessageContext) messageContext).getAxis2MessageContext());
    }

    /**
     * Set given JsonElement as the Json payload of. If the current payload type is not Json, the payload type would
     * be set as Json
     *
     * @param jsonPayload JsonElement to set as the current payload
     */
    public void setJsonPayload(JsonElement jsonPayload) {

        String transformedJson = jsonPayload.toString();
        setJsonPayload(transformedJson);
    }

    /**
     * Set given String as the Json payload of. If the current payload type is not Json, the payload type would be
     * set to Json
     *
     * @param payload String to set as the current payload
     */
    public void setJsonPayload(String payload) {

        setPayloadType("application/json");

        try {
            JsonUtil.getNewJsonPayload(((Axis2MessageContext) messageContext).getAxis2MessageContext(),
                    payload, true, true);
        } catch (AxisFault axisFault) {
            throw new SimpleMessageContextException(axisFault);
        }
    }

    /**
     * Get Stream of OMElement by appliying given xPath to given OMElement as root,
     * If error occur while applying xPath, an empty Stream would return
     *
     * @param baseElement OMElement to consider as root element
     * @param xPath       xPath string to apply
     * @return Stream of OMElement
     */
    public Stream<OMElement> getXmlElementsStream(OMElement baseElement, String xPath) {

        try {
            AXIOMXPath axiomxPath = new AXIOMXPath(xPath);
            List<OMElement> elementList = axiomxPath.selectNodes(baseElement);
            return elementList.stream();
        } catch (JaxenException e) {
            return Stream.empty();
        }
    }

    /**
     * If the payload is type XML, then get the child elements of the root XML element as a Stream of OMElement
     *
     * @return Stream of OMElement of the child elements of the root xml element
     */
    public Stream<OMElement> getXmlChildElementsStream() {

        OMElement rootElement = messageContext.getEnvelope().getBody().getFirstElement();
        return getXmlChildElementsStream(rootElement);
    }

    public Stream<OMElement> getXmlChildElementsStream(OMElement xmlElement) {

        Iterable<OMElement> iterable = xmlElement::getChildElements;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * If the payload is a CSV, get the payload as a Stream of Array of String for each line in CSV file.
     * If payload is not a valid CSV then, returns an empty Stream
     *
     * @param linesToSkip Number of lines to skip from header of CSV
     * @return Stream of Array of String representing each line of the CSV payload
     */
    public Stream<String[]> getCsvArrayStream(int linesToSkip) {

        String payloadText = PayloadHelper.getTextPayload(messageContext);

        String csvText;
        if (payloadText != null) {
            csvText = payloadText;
        } else {
            log.error("Error converting data : not a valid CSV payload");
            csvText = "";
        }

        CSVReader csvReader = new CSVReaderBuilder(new StringReader(csvText)).withSkipLines(linesToSkip).build();
        return StreamSupport.stream(csvReader.spliterator(), false);
    }

    /**
     * Set the current payload to the given text
     *
     * @param text String to set as the current payload
     */
    public void setCsvPayload(String text) {

        setPayloadType("text/plain");

        if (messageContext.getEnvelope() == null) {
            try {
                messageContext.setEnvelope(OMAbstractFactory.getSOAP12Factory()
                        .createSOAPEnvelope());
            } catch (Exception e) {
                throw new SimpleMessageContextException(e);
            }
        }
        org.apache.synapse.util.PayloadHelper.setTextPayload(messageContext.getEnvelope(), text);
    }

    /**
     * Get the value of a custom (local) property set on the message instance
     *
     * @param key key to look up property
     * @return value for the given key
     */
    public Object getProperty(String key) {

        return messageContext.getProperty(key);
    }

    /**
     * Set a custom (local) property with the given name on the message instance
     *
     * @param key   key to be used
     * @param value value to be saved
     */
    public void setProperty(String key, Object value) {

        messageContext.setProperty(key, value);
    }

    /**
     * Set given key and value as a header
     *
     * @param key   header key
     * @param value header value
     */
    public void setHeader(String key, String value) {

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContext).getAxis2MessageContext();
        Object headerObj = axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        if (headerObj != null) {
            Map headers = (Map) headerObj;
            headers.remove(key);
            headers.put(key, value);
        }
    }

    /**
     * Provides an instance of JsonArrayCollector to collect and set payload as JsonArray
     *
     * @return instance of JsonArrayCollector
     */
    public JsonArrayCollector collectToJsonArray() {

        return new JsonArrayCollector(this);
    }

    /**
     * Provides an instance of JsonObjectCollector to collect and set payload as JsonObject
     *
     * @return instance of JsonObjectCollector
     */
    public JsonObjectCollector collectToJsonObject() {

        return new JsonObjectCollector(this);
    }

    /**
     * Provides an instance of CsvCollector to collect and set payload as CSV for String[] Stream
     *
     * @return instance of CsvCollector
     */
    public CsvCollector collectToCsv() {

        return new CsvCollector(this);
    }

    private void setPayloadType(String payloadType) {

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContext).getAxis2MessageContext();
        axis2MessageContext.setProperty(Constants.Configuration.MESSAGE_TYPE, payloadType);
        axis2MessageContext.setProperty(Constants.Configuration.CONTENT_TYPE, payloadType);
    }

}
