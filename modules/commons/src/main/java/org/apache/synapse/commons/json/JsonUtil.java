/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json;

import java.util.HashMap;
import java.util.Map;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.synapse.commons.SynapseCommonsException;
import org.apache.synapse.commons.staxon.core.json.JsonXMLConfig;
import org.apache.synapse.commons.staxon.core.json.JsonXMLConfigBuilder;
import org.apache.synapse.commons.staxon.core.json.JsonXMLInputFactory;
import org.apache.synapse.commons.staxon.core.json.JsonXMLOutputFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.OMSourcedElementImpl;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.MiscellaneousUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Properties;

public final class JsonUtil {
    private static Log logger = LogFactory.getLog(JsonUtil.class.getName());

    private static final String ORG_APACHE_SYNAPSE_COMMONS_JSON_IS_JSON_OBJECT = "org.apache.synapse.commons.json.JsonInputStream.IsJsonObject";

    private static final QName JSON_OBJECT = new QName("jsonObject");

    private static final QName JSON_ARRAY = new QName("jsonArray");

    private static final QName JSON_VALUE = new QName("jsonValue");
    /**
     * If this property is set to <tt>true</tt> the input stream of the JSON payload will be reset
     * after writing to the output stream within the #writeAsJson method.
     */
    public static final String PRESERVE_JSON_STREAM = "preserve.json.stream";

    /// JSON/XML INPUT OUTPUT Formatting Configuration
    // TODO: Build thie configuration from a "json.properties" file. Add a debug log to dump() the config to the log.
    // TODO: Add another param to empty xml element to null or empty json string mapping <a/> -> "a":null or "a":""
    // TODO: Build this configuration into a separate class.
    // TODO: Property to remove root element from XML output
    // TODO: Axis2 property/synapse static property add XML Namespace to the root element

    private static boolean isJsonToXmlPiEnabled = false;

    /**
     * Factory used to create JSON Writers
     */
    private static final JsonXMLOutputFactory jsonOutputFactory;

    /**
     * Factory used to create JSON Readers
     */
    private static final JsonXMLInputFactory jsonInputFactory;

    /**
     * Factory used to create JSON Readers
     */
    private static final JsonXMLInputFactory xmlInputFactoryNoPIs;

    /**
     * Factory used to create XML Readers
     */
    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        Properties properties = MiscellaneousUtil.loadProperties("synapse.properties");
        isJsonToXmlPiEnabled = Boolean.parseBoolean(
                properties.getProperty(Constants.SYNAPSE_JSON_TO_XML_PROCESS_INSTRUCTION_ENABLE, "false").trim());
        jsonOutputFactory = generateJSONOutputFactory(properties);
        jsonInputFactory = generateJSONInputFactory(properties);
        xmlInputFactoryNoPIs = generateJsonXMLInputFactory(properties);
    }

    /**
     * Generate factory that is used to create JSON Readers.
     * It uses Configuration that is used to produce XML that has no processing instructions in it.
     *
     * @param props Properties that are loaded
     * @return JsonXMLOutputFactory that is used create JSON Readers
     */
    public static JsonXMLInputFactory generateJsonXMLInputFactory(Properties props) {
        boolean xmloutMultiplePI;
        boolean xmloutAutoArray;
        boolean xmlNilReadWriteEnabled;
        if (props == null) {
            xmloutMultiplePI = false;
            xmloutAutoArray = true;
            xmlNilReadWriteEnabled = false;
        } else {
            xmloutMultiplePI = Boolean.parseBoolean(props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_XML_OUT_MULTIPLE_PI, "false"));
            xmloutAutoArray = Boolean.parseBoolean(props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_XML_OUT_AUTO_ARRAY, "true"));
            xmlNilReadWriteEnabled = Boolean
                    .parseBoolean(props.getProperty(Constants.SYNAPSE_COMMONS_ENABLE_XML_NIL_READ_WRITE, "false"));
        }

        //Configuration used to produce XML that has no processing instructions in it.
        JsonXMLConfig xmlOutputConfigNoPIs = new JsonXMLConfigBuilder()
                .multiplePI(xmloutMultiplePI)
                .autoArray(xmloutAutoArray)
                .autoPrimitive(true)
                .namespaceDeclarations(false)
                .namespaceSeparator('\u0D89')
                .readWriteXmlNil(xmlNilReadWriteEnabled)
                .build();

        return new JsonXMLInputFactory(xmlOutputConfigNoPIs);
    }

    /**
     * Generate factory that is used to create JSON Readers
     * It uses Configuration that is used to produce XML that has processing instructions in it.
     *
     * @param props Properties that are loaded
     * @return JsonXMLOutputFactory that is used create JSON Readers
     */
    public static JsonXMLInputFactory generateJSONInputFactory(Properties props) {
        String jsonoutcustomRegex;
        boolean xmlNilReadWriteEnabled;
        if (props == null) {
            jsonoutcustomRegex = null;
            xmlNilReadWriteEnabled = false;
        } else {
            jsonoutcustomRegex = props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_DISABLE_AUTO_PRIMITIVE_REGEX, null);
            xmlNilReadWriteEnabled = Boolean
                    .parseBoolean(props.getProperty(Constants.SYNAPSE_COMMONS_ENABLE_XML_NIL_READ_WRITE, "false"));
        }

        //Configuration used to produce XML that has processing instructions in it.
        JsonXMLConfig xmlOutputConfig = new JsonXMLConfigBuilder()
                .multiplePI(true)
                .autoArray(true)
                .autoPrimitive(true)
                .namespaceDeclarations(false)
                .namespaceSeparator('\u0D89')
                .customRegex(jsonoutcustomRegex)
                .readWriteXmlNil(xmlNilReadWriteEnabled)
                .build();

        return new JsonXMLInputFactory(xmlOutputConfig);
    }

    /**
     * Generate factory that is used to create JSON Writers
     * It uses configuration that is used to format the JSON output produced by the JSON writer.
     *
     * @param props Properties that are loaded
     * @return JsonXMLOutputFactory that is used to create JSON Writers
     */
    public static JsonXMLOutputFactory generateJSONOutputFactory(Properties props) {
        boolean jsonoutMultiplePI;
        boolean jsonoutAutoArray;
        boolean jsonOutAutoPrimitive;
        boolean jsonOutEnableNsDeclarations;
        char jsonOutNamespaceSepChar;
        String jsonoutCustomReplaceRegex;
        String jsonoutCustomReplaceSequence;
        String jsonoutcustomRegex;
        boolean xmlNilReadWriteEnabled;
        boolean xmlWriteNullForEmptyElements;
        boolean preserverNamespacesForJson;
        boolean processNCNames;

        if (props == null) {
            preserverNamespacesForJson = false;
            jsonoutMultiplePI = true;
            jsonoutAutoArray = true;
            jsonOutAutoPrimitive = true;
            jsonOutEnableNsDeclarations = false;
            jsonOutNamespaceSepChar = '_';
            jsonoutCustomReplaceRegex = null;
            jsonoutCustomReplaceSequence = "";
            jsonoutcustomRegex = null;
            xmlNilReadWriteEnabled = false;
            xmlWriteNullForEmptyElements = true;
            processNCNames = false;
        } else {
            preserverNamespacesForJson = Boolean.parseBoolean(props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_PRESERVE_NAMESPACE, "false").trim().toLowerCase());
            jsonoutMultiplePI = Boolean.parseBoolean(props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_JSON_OUT_MULTIPLE_PI, "true"));
            jsonoutAutoArray = Boolean.parseBoolean(props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_JSON_OUT_AUTO_ARRAY, "true"));
            jsonOutAutoPrimitive = Boolean.parseBoolean(props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_AUTO_PRIMITIVE, "true").trim().toLowerCase());
            jsonOutEnableNsDeclarations = Boolean.parseBoolean(props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_ENABLE_NS_DECLARATIONS,
                            "false").trim().toLowerCase());
            jsonOutNamespaceSepChar = props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_NAMESPACE_SEP_CHAR, "_").trim().charAt(0);
            jsonoutCustomReplaceRegex = props
                    .getProperty(Constants.SYNAPSE_COMMONS_JSON_DISABLE_AUTO_PRIMITIVE_CUSTOM_REPLACE_REGEX, null);
            jsonoutCustomReplaceSequence = props
                    .getProperty(Constants.SYNAPSE_COMMONS_JSON_DISABLE_AUTO_PRIMITIVE_CUSTOM_REPLACE_SEQUENCE, "");
            jsonoutcustomRegex = props.getProperty
                    (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_DISABLE_AUTO_PRIMITIVE_REGEX, null);
            xmlNilReadWriteEnabled = Boolean
                    .parseBoolean(props.getProperty(Constants.SYNAPSE_COMMONS_ENABLE_XML_NIL_READ_WRITE, "false"));
            xmlWriteNullForEmptyElements = Boolean.parseBoolean(
                    props.getProperty(Constants.SYNAPSE_COMMONS_ENABLE_XML_NULL_FOR_EMPTY_ELEMENT, "true"));
            processNCNames = Boolean.parseBoolean(props.getProperty(
                    Constants.SYNAPSE_COMMONS_JSON_BUILD_VALID_NC_NAMES, "false").trim().toLowerCase());
        }

        //This configuration is used to format the JSON output produced by the JSON writer.
        JsonXMLConfig jsonOutputConfig = new JsonXMLConfigBuilder()
                .multiplePI(jsonoutMultiplePI)
                .autoArray(jsonoutAutoArray)
                .autoPrimitive(jsonOutAutoPrimitive)
                .namespaceDeclarations(jsonOutEnableNsDeclarations)
                .namespaceSeparator(jsonOutNamespaceSepChar)
                .customReplaceRegex(jsonoutCustomReplaceRegex)
                .customReplaceSequence(jsonoutCustomReplaceSequence)
                .customRegex(jsonoutcustomRegex)
                .readWriteXmlNil(xmlNilReadWriteEnabled)
                .writeNullForEmptyElement(xmlWriteNullForEmptyElements)
                .preserverNamespacesForJson(preserverNamespacesForJson)
                .processNCNames(processNCNames)
                .build();

        return new JsonXMLOutputFactory(jsonOutputConfig);
    }

    /**
     * Generate factory that is used to create JSON Writers
     * This is will override the global configration
     *
     * @param props Properties that are loaded
     * @return JsonXMLOutputFactory that is used to create JSON Writers
     */
    public static JsonXMLOutputFactory generateJSONOutputFactoryWithOveride(Properties props) {

        boolean jsonoutMultiplePI;
        boolean jsonoutAutoArray;
        boolean jsonOutAutoPrimitive;
        boolean jsonOutEnableNsDeclarations;
        char jsonOutNamespaceSepChar;
        String jsonoutCustomReplaceRegex;
        String jsonoutCustomReplaceSequence;
        String jsonoutcustomRegex;
        boolean xmlNilReadWriteEnabled;
        boolean xmlWriteNullForEmptyElements;
        boolean preserverNamespacesForJson;
        boolean processNCNames;

        preserverNamespacesForJson = Boolean.parseBoolean(props.getProperty
                (Constants.SYNAPSE_COMMONS_JSON_PRESERVE_NAMESPACE,
                        Boolean.toString(jsonOutputFactory.getConfig().isPreserverNamespacesForJson())));
        jsonoutMultiplePI = Boolean.parseBoolean(props.getProperty
                (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_JSON_OUT_MULTIPLE_PI,
                        Boolean.toString(jsonOutputFactory.getConfig().isMultiplePI())));
        jsonoutAutoArray = Boolean.parseBoolean(props.getProperty
                (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_JSON_OUT_AUTO_ARRAY,
                        Boolean.toString(jsonOutputFactory.getConfig().isAutoArray())));
        jsonOutAutoPrimitive = Boolean.parseBoolean(props.getProperty
                (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_AUTO_PRIMITIVE,
                        Boolean.toString(jsonOutputFactory.getConfig().isAutoPrimitive())).trim().toLowerCase());
        jsonOutEnableNsDeclarations = Boolean.parseBoolean(props.getProperty
                (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_ENABLE_NS_DECLARATIONS,
                        Boolean.toString(jsonOutputFactory.getConfig()
                                .isNamespaceDeclarations())).trim().toLowerCase());
        jsonOutNamespaceSepChar = props.getProperty
                (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_NAMESPACE_SEP_CHAR,
                        Character.toString(jsonOutputFactory.getConfig().getNamespaceSeparator())).trim().charAt(0);
        jsonoutCustomReplaceRegex = props
                .getProperty(Constants.SYNAPSE_COMMONS_JSON_DISABLE_AUTO_PRIMITIVE_CUSTOM_REPLACE_REGEX,
                        jsonOutputFactory.getConfig().getCustomReplaceRegex());
        jsonoutCustomReplaceSequence = props
                .getProperty(Constants.SYNAPSE_COMMONS_JSON_DISABLE_AUTO_PRIMITIVE_CUSTOM_REPLACE_SEQUENCE,
                        jsonOutputFactory.getConfig().getCustomReplaceSequence());
        jsonoutcustomRegex = props.getProperty
                (Constants.SYNAPSE_COMMONS_JSON_OUTPUT_DISABLE_AUTO_PRIMITIVE_REGEX,
                        jsonOutputFactory.getConfig().getCustomRegex());
        xmlNilReadWriteEnabled = Boolean
                .parseBoolean(props.getProperty(Constants.SYNAPSE_COMMONS_ENABLE_XML_NIL_READ_WRITE,
                        Boolean.toString(jsonOutputFactory.getConfig().isReadWriteXmlNil())));
        xmlWriteNullForEmptyElements = Boolean.parseBoolean(
                props.getProperty(Constants.SYNAPSE_COMMONS_ENABLE_XML_NULL_FOR_EMPTY_ELEMENT,
                        Boolean.toString(jsonOutputFactory.getConfig().isWriteNullForEmptyElements())));
        processNCNames = Boolean.parseBoolean(props.getProperty(
                Constants.SYNAPSE_COMMONS_JSON_BUILD_VALID_NC_NAMES,
                Boolean.toString(jsonOutputFactory.getConfig().isProcessNCNames())).trim().toLowerCase());

        //This configuration is used to format the JSON output produced by the JSON writer.
        JsonXMLConfig jsonOutputConfig = new JsonXMLConfigBuilder()
                .multiplePI(jsonoutMultiplePI)
                .autoArray(jsonoutAutoArray)
                .autoPrimitive(jsonOutAutoPrimitive)
                .namespaceDeclarations(jsonOutEnableNsDeclarations)
                .namespaceSeparator(jsonOutNamespaceSepChar)
                .customReplaceRegex(jsonoutCustomReplaceRegex)
                .customReplaceSequence(jsonoutCustomReplaceSequence)
                .customRegex(jsonoutcustomRegex)
                .readWriteXmlNil(xmlNilReadWriteEnabled)
                .writeNullForEmptyElement(xmlWriteNullForEmptyElements)
                .preserverNamespacesForJson(preserverNamespacesForJson)
                .processNCNames(processNCNames)
                .build();

        return new JsonXMLOutputFactory(jsonOutputConfig);
    }

    /**
     * Converts the XML payload of a message context into its JSON representation and writes it to an output stream.<br/>
     * If no XML payload is found, the existing JSON payload will be copied to the output stream.<br/>
     * Note that this method removes all existing namespace declarations and namespace prefixes of the payload that is <br/>
     * present in the provided message context.
     *
     * @param messageContext Axis2 Message context that holds the JSON/XML payload.
     * @param out            Output stream to which the payload(JSON) must be written.
     * @throws org.apache.axis2.AxisFault
     */
    public static void writeAsJson(MessageContext messageContext, OutputStream out) throws AxisFault {
        if (messageContext == null || out == null) {
            return;
        }
        OMElement element = messageContext.getEnvelope().getBody().getFirstElement();
        Object o = jsonStream(messageContext, false);
        InputStream json = null;
        if (o != null) {
            json = (InputStream) o;
        }
        o = messageContext.getProperty(org.apache.synapse.commons.json.Constants.JSON_STRING);
        String jsonStr = null;
        if (o instanceof String) {
            jsonStr = (String) o;
        }
        if (json != null) { // there is a JSON stream
            try {
                if (element instanceof OMSourcedElementImpl) {
                    if (isAJsonPayloadElement(element)) {
                        writeJsonStream(json, messageContext, out);
                    } else { // Ignore the JSON stream
                        writeAsJson(element, out);
                    }
                } else if (element != null) { // element is not an OMSourcedElementImpl. But we ignore the JSON stream.
                    writeAsJson(element, out);
                } else { // element == null.
                    writeJsonStream(json, messageContext, out);
                }
            } catch (Exception e) {
                //Close the stream
                IOUtils.closeQuietly(json);
                throw new AxisFault("Could not write JSON stream.", e);
            }
        } else if (element != null) { // No JSON stream found. Convert the existing element to JSON.
            writeAsJson(element, out, populateRequiredProperties(messageContext), JsonUtil.jsonOutputFactory);
        } else if (jsonStr != null) { // No JSON stream or element found. See if there's a JSON_STRING set.
            try {
                out.write(jsonStr.getBytes());
            } catch (IOException e) {
                logger.error("#writeAsJson. Could not write JSON string. MessageID: "
                        + messageContext.getMessageID() + ". Error>> " + e.getLocalizedMessage());
                throw new AxisFault("Could not write JSON string.", e);
            }
        } else {
            logger.error("#writeAsJson. Payload could not be written as JSON. MessageID: " + messageContext.getMessageID());
            throw new AxisFault("Payload could not be written as JSON.");
        }
    }

    /**
     * Returning a HashMap with required properties which used for JSON conversion.
     *
     * @param msgCtx Message context
     * @return Required properties
     */
    private static Map populateRequiredProperties(MessageContext msgCtx) {
        Map<String, String> requiredProperties = new HashMap<>();
        requiredProperties.put(Constants.PRESERVE_SPACES, (String) msgCtx.getProperty(Constants.PRESERVE_SPACES));
        return requiredProperties;
    }

    /**
     * Converts a JSON input stream to its XML representation.
     *
     * @param jsonStream JSON input stream
     * @param pIs        Whether or not to add XML processing instructions to the output XML.<br/>
     *                   This property is useful when converting JSON payloads with array objects.
     * @return OMElement that is the XML representation of the input JSON data.
     */
    public static OMElement toXml(InputStream jsonStream, boolean pIs) throws AxisFault {
        if (jsonStream == null) {
            logger.error("#toXml. Could not convert JSON Stream to XML. JSON input stream is null.");
            return null;
        }
        try {
            XMLStreamReader streamReader = getReader(jsonStream, pIs);
            return new StAXOMBuilder(streamReader).getDocumentElement();
        } catch (XMLStreamException e) {//invalid JSON?
            logger.error("#toXml. Could not convert JSON Stream to XML. Cannot handle JSON input. Error>>> " + e.getLocalizedMessage());
            throw new AxisFault("Could not convert JSON Stream to XML. Cannot handle JSON input.", e);
        }
    }

    /**
     * Returns an XMLStreamReader for a JSON input stream
     *
     * @param jsonStream InputStream of JSON
     * @param pIs        Whether to add XML PIs to the XML output. This is used as an instruction to the returned XML Stream Reader.
     * @return An XMLStreamReader
     * @throws javax.xml.stream.XMLStreamException
     */
    public static XMLStreamReader getReader(InputStream jsonStream, boolean pIs) throws XMLStreamException {
        if (jsonStream == null) {
            logger.error("#getReader. Could not create XMLStreamReader from [null] input stream.");
            return null;
        }
        return pIs ? getReader(jsonStream)
                : new JsonReaderDelegate(xmlInputFactoryNoPIs.createXMLStreamReader(jsonStream,
                org.apache.synapse.commons.staxon.core.json.stream.impl.Constants.SCANNER.SCANNER_1),
                jsonOutputFactory.getConfig().isProcessNCNames());
    }

    /**
     * This method is useful when you need to get an XML reader directly for the input JSON stream <br/>
     * without adding any additional object wrapper elements such as 'jsonObject' and 'jsonArray'.
     *
     * @param jsonStream InputStream of JSON
     * @return An XMLStreamReader
     * @throws javax.xml.stream.XMLStreamException
     */
    public static XMLStreamReader getReader(InputStream jsonStream) throws XMLStreamException {
        if (jsonStream == null) {
            logger.error("#getReader. Could not create XMLStreamReader from [null] input stream.");
            return null;
        }
        return new JsonReaderDelegate(jsonInputFactory.createXMLStreamReader(jsonStream,
                org.apache.synapse.commons.staxon.core.json.stream.impl.Constants.SCANNER.SCANNER_1),
                jsonOutputFactory.getConfig().isProcessNCNames());
    }

    /**
     * Converts an XML element to its JSON representation and writes it to an output stream.<br/>
     * Note that this method removes all existing namespace declarations and namespace prefixes of the provided XML element<br/>
     *
     * @param element      XML element of which JSON representation is expected.
     * @param outputStream Output Stream to write the JSON representation.<br/>
     *                     At the end of a successful conversion, its flush method will be called.
     * @throws AxisFault
     */
    public static void writeAsJson(OMElement element, OutputStream outputStream) throws AxisFault {
        writeAsJson(element, outputStream, null, null);
    }

    /**
     * Converts an XML element to its JSON representation and writes it to an output stream.<br/>
     * Note that this method removes all existing namespace declarations and namespace prefixes of the provided XML
     * element<br/>
     *
     * @param element      XML element of which JSON representation is expected.
     * @param outputStream Output Stream to write the JSON representation.<br/>
     *                     At the end of a successful conversion, its flush method will be called.
     * @param properties   Message context properties
     * @throws AxisFault
     */
    public static void writeAsJson(OMElement element, OutputStream outputStream, Map properties,
                                   JsonXMLOutputFactory jsonOutputFactory) throws
            AxisFault {
        jsonOutputFactory = jsonOutputFactory == null ? JsonUtil.jsonOutputFactory : jsonOutputFactory;
        if (element == null) {
            throw new AxisFault("OMElement is null. Cannot convert to JSON.");
        }
        if (outputStream == null) {
            return;
        }
        transformElement(element, true, properties, jsonOutputFactory);
        convertOMElementToJson(element, outputStream, jsonOutputFactory);
    }

    /**
     * Convert OMElement to JSON.
     *
     * @param element      OMElement
     * @param outputStream Output stream
     * @param jsonOutputFactory Object to hold JsonXMLConfigurations
     * @throws AxisFault
     */
    private static void convertOMElementToJson(OMElement element, OutputStream outputStream,
                                               JsonXMLOutputFactory jsonOutputFactory) throws AxisFault {
        XMLEventReader xmlEventReader = null;
        XMLEventWriter jsonWriter = null;
        try {
            if (JSON_VALUE.getLocalPart().equals(element.getLocalName())) {
                outputStream.write(element.getText().getBytes());
                outputStream.flush();
                return;
            }
            org.apache.commons.io.output.ByteArrayOutputStream xmlStream =
                    new org.apache.commons.io.output.ByteArrayOutputStream();
            element.serialize(xmlStream);
            xmlStream.flush();
            xmlEventReader = xmlInputFactory.createXMLEventReader(
                    new XmlReaderDelegate(xmlInputFactory.createXMLStreamReader(
                            new ByteArrayInputStream(xmlStream.toByteArray())
                    ), jsonOutputFactory.getConfig().isProcessNCNames())
            );
            jsonWriter = jsonOutputFactory.createXMLEventWriter(outputStream);
            jsonWriter.add(xmlEventReader);
            outputStream.flush();
        } catch (XMLStreamException e) {
            logger.error("#writeAsJson. Could not convert OMElement to JSON. Invalid XML payload. Error>>> " +
                    e.getLocalizedMessage());
            throw new AxisFault("Could not convert OMElement to JSON. Invalid XML payload.", e);
        } catch (IOException e) {
            logger.error("#writeAsJson. Could not convert OMElement to JSON. Error>>> " + e.getLocalizedMessage());
            throw new AxisFault("Could not convert OMElement to JSON.", e);
        } finally {
            if (xmlEventReader != null) {
                try {
                    xmlEventReader.close();
                } catch (XMLStreamException ex) {
                    //ignore
                }
            }
            if (jsonWriter != null) {
                try {
                    jsonWriter.close();
                } catch (XMLStreamException ex) {
                    //ignore
                }
            }
        }
    }

    public static StringBuilder toJsonString(OMElement element) throws AxisFault {
        return toJsonString(element, null);
    }

    /**
     * Converts an XML element to its JSON representation and returns it as a String.
     *
     * @param element OMElement to be converted to JSON.
     * @return A String builder instance that contains the converted JSON string.
     */
    public static StringBuilder toJsonString(OMElement element, JsonXMLOutputFactory jsonOutputFactory)
            throws AxisFault {
        if (element == null) {
            return new StringBuilder("{}");
        }
        org.apache.commons.io.output.ByteArrayOutputStream byteStream =
                new org.apache.commons.io.output.ByteArrayOutputStream();
        writeAsJson(element.cloneOMElement(), byteStream, null, jsonOutputFactory);
        return new StringBuilder(new String(byteStream.toByteArray()));
    }

    /**
     * Removes XML namespace declarations, and namespace prefixes from an XML element.
     *
     * @param element       Source XML element
     * @param processAttrbs Whether to remove the namespaces from attributes as well
     */
    public static void transformElement(OMElement element, boolean processAttrbs) {
        transformElement(element, processAttrbs, null, null);
    }

    /**
     * Removes XML namespace declarations, and namespace prefixes from an XML element.
     *
     * @param element       Source XML element
     * @param processAttrbs Whether to remove the namespaces from attributes as well
     * @param properties    Message context properties
     * @param jsonOutputFactory JsonOutputFactory with all the configurations.
     *                          Pass null to use global configuration
     */
    public static void transformElement(OMElement element, boolean processAttrbs,
                                        Map properties, JsonXMLOutputFactory jsonOutputFactory) {
        boolean preserveNamespacesForJson =
                (jsonOutputFactory != null) && jsonOutputFactory.getConfig().isPreserverNamespacesForJson();
        if (element == null) {
            return;
        }
        if (properties == null) {
            removeIndentations(element);
        } else {
            removeIndentations(element, properties);
        }

        if (!preserveNamespacesForJson) {
            removeNamespaces(element, processAttrbs);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("#transformElement. Transformed OMElement. Result: " + element.toString());
        }
    }

    private static void removeIndentations(OMElement elem) {
        Iterator children = elem.getChildren();
        while (children.hasNext()) {
            OMNode child = (OMNode) children.next();
            if (child instanceof OMText) {
                if ((((OMText) child).getText().trim()).isEmpty()) {
                    children.remove();
                }
            } else if (child instanceof OMElement) {
                removeIndentations((OMElement) child);
            }
        }
    }

    /**
     * Remove indentations of a OMElement.
     * If PRESERVE_SPACES property in the requiredProperties is true, spaces inside XML elements will be preserved.
     *
     * @param elem       OMElement needed to remove indentations
     * @param properties Required properties
     */
    private static void removeIndentations(OMElement elem, Map properties) {
        Iterator children = elem.getChildren();
        boolean preserveSpaces = properties.get(Constants.PRESERVE_SPACES) != null
                && Boolean.parseBoolean((String) properties.get(Constants.PRESERVE_SPACES));

        while (children.hasNext()) {
            OMNode child = (OMNode) children.next();
            if (child instanceof OMText) {
                // preserve spaces of OMText if preserve spaces property is true
                if (preserveSpaces && child.getPreviousOMSibling() == null && child.getNextOMSibling() == null) {
                    continue;
                }
                if ((((OMText) child).getText().trim()).isEmpty()) {
                    children.remove();
                }
            } else if (child instanceof OMElement) {
                removeIndentations((OMElement) child, properties);
            }
        }
    }

    private static void removeNamespaces(OMElement element, boolean processAttrbs) {
        OMNamespace ns = element.getNamespace();
        Iterator i = element.getAllDeclaredNamespaces();
        while (i.hasNext()) {
            i.next();
            i.remove();
        }
        String prefix;
        if (ns != null) {
            prefix = "";//element.getNamespace().getPrefix();
            element.setNamespace(element.getOMFactory().createOMNamespace("", prefix));
        }
        Iterator children = element.getChildElements();
        while (children.hasNext()) {
            removeNamespaces((OMElement) children.next(), processAttrbs);
        }
        if (!processAttrbs) {
            return;
        }
        Iterator attrbs = element.getAllAttributes();
        while (attrbs.hasNext()) {
            OMAttribute attrb = (OMAttribute) attrbs.next();
            prefix = "";//attrb.getQName().getPrefix();
            attrb.setOMNamespace(attrb.getOMFactory().createOMNamespace("", prefix));
            //element.removeAttribute(attrb);
        }
    }

    /**
     * Builds and returns a new JSON payload for a message context with a stream of JSON content. <br/>
     * This is the recommended way to build a JSON payload into an Axis2 message context.<br/>
     * A JSON payload built into a message context with this method can only be removed by calling
     * {@link #removeJsonPayload(org.apache.axis2.context.MessageContext)} method. This added to avoid code breaks
     * for previous implementations. For new implementations use getNewJsonPayload method.
     *
     * @param messageContext     Axis2 Message context to which the new JSON payload must be saved (if instructed with <tt>addAsNewFirstChild</tt>).
     * @param inputStream        JSON content as an input stream.
     * @param removeChildren     Whether to remove existing child nodes of the existing payload of the message context
     * @param addAsNewFirstChild Whether to add the new JSON payload as the first child of this message context *after* removing the existing first child element.<br/>
     *                           Setting this argument to <tt>true</tt> will have no effect if the value of the argument <tt>removeChildren</tt> is already <tt>false</tt>.
     * @return Payload object that stores the input JSON content as a Sourced object (See {@link org.apache.axiom.om.OMSourcedElement}) that can build the XML tree for contained JSON payload on demand.
     */
    @Deprecated
    public static OMElement newJsonPayload(MessageContext messageContext, InputStream inputStream,
                                           boolean removeChildren, boolean addAsNewFirstChild) {
        try {
            return getNewJsonPayload(messageContext, inputStream, removeChildren, addAsNewFirstChild);
        } catch (AxisFault axisFault) {
            logger.error("Payload provided is not an JSON payload.");
            return null;
        }
    }

    /**
     * Builds and returns a new JSON payload for a message context with a stream of JSON content. <br/>
     * This is the recommended way to build a JSON payload into an Axis2 message context.<br/>
     * A JSON payload built into a message context with this method can only be removed by calling
     * {@link #removeJsonPayload(org.apache.axis2.context.MessageContext)} method.
     *
     * @param messageContext     Axis2 Message context to which the new JSON payload must be saved (if instructed with <tt>addAsNewFirstChild</tt>).
     * @param inputStream        JSON content as an input stream.
     * @param removeChildren     Whether to remove existing child nodes of the existing payload of the message context
     * @param addAsNewFirstChild Whether to add the new JSON payload as the first child of this message context *after* removing the existing first child element.<br/>
     *                           Setting this argument to <tt>true</tt> will have no effect if the value of the argument <tt>removeChildren</tt> is already <tt>false</tt>.
     * @return Payload object that stores the input JSON content as a Sourced object (See {@link org.apache.axiom.om.OMSourcedElement}) that can build the XML tree for contained JSON payload on demand.
     */
    public static OMElement getNewJsonPayload(MessageContext messageContext, InputStream inputStream,
                                              boolean removeChildren, boolean addAsNewFirstChild) throws AxisFault {
        if (messageContext == null) {
            logger.error("#getNewJsonPayload. Could not save JSON stream. Message context is null.");
            return null;
        }
        boolean isObject = false;
        boolean isArray = false;
        boolean isValue = false;
        if (inputStream != null) {
            InputStream json = setJsonStream(messageContext, inputStream);
            // read ahead few characters to see if the stream is valid...
            boolean isEmptyPayload = true;
            boolean valid = false;
            try {
                /*
                * Checks for all empty or all whitespace streams and if found  sets isEmptyPayload to false. The while
                * loop exits if one of the valid literals is found. If no valid literal is found the it will loop to
                * the end of the stream and the next if statement after the loop makes sure that valid will remain false
                * */
                int character = json.read();
                while (character != -1 && character != '{' && character != '[' && character != '"' &&
                        character != 't' && character != 'n' && character != 'f' && character != '-' &&
                        character != '0' && character != '1' && character != '2' && character != '3' &&
                        character != '4' && character != '5' && character != '6' && character != '7' &&
                        character != '8' && character != '9') {
                    if (character != 32) {
                        isEmptyPayload = false;
                    }
                    character = json.read();
                }
                if (character != -1) {
                    valid = true;
                }
                /*
                * A valid JSON can be an object, array or a value (http://json.org/). The following conditions check
                * the beginning char to see if the json stream could fall into one of this category
                * */
                if (character == '{') {
                    isObject = true;
                    isArray = false;
                    isValue = false;
                } else if (character == '[') {
                    isArray = true;
                    isObject = false;
                    isValue = false;
                } else if (character == '"' || character == 't' || character == 'n' || character == 'f' ||
                        character == '-' || character == '0' || character == '1' ||
                        character == '2' || character == '3' || character == '4' || character == '5' ||
                        character == '6' || character == '7' ||
                        character == '8' || character == '9') { //a value can be a quoted string (") true (t),
                    // false (f), null (n) or a number (- or any digit and cannot start with a +)
                    isArray = false;
                    isObject = false;
                    isValue = true;
                }
                json.reset();
            } catch (IOException e) {
                logger.error(
                        "#getNewJsonPayload. Could not determine availability of the JSON input stream. MessageID: " +
                        messageContext.getMessageID() + ". Error>>> " + e.getLocalizedMessage());
                return null;
            }
            if (!valid) {
                if (isEmptyPayload) {
                    //This is a empty payload so return null without doing further processing.
                    if (logger.isDebugEnabled()) {
                        logger.debug("#emptyJsonPayload found.MessageID: " + messageContext.getMessageID());
                    }
                    logger.debug("#emptyJsonPayload found.MessageID: " + messageContext.getMessageID());
                    return null;
                } else {
                    /*
                     * This method required to introduce due the GET request failure with query parameter and
                     * contenttype=application/json. Because of the logic implemented with '=' sign below,
                     * it expects a valid JSON string as the query parameter value (string after '=' sign) for GET
                     * requests with application/json content type. Therefore it fails for requests like
                     * https://localhost:8243/services/customer?format=xml and throws axis fault. With this fix,
                     * HTTP method is checked and avoid throwing axis2 fault for GET requests.
                     * https://wso2.org/jira/browse/ESBJAVA-4270
                     */
                    if (isValidPayloadRequired(messageContext)) {
                        throw new AxisFault(
                                "#getNewJsonPayload. Could not save JSON payload. Invalid input stream found. Payload"
                                        + " is not a JSON string.");
                    } else {
                        return null;
                    }
                }
            }
            QName jsonElement = null;
            if (isObject) {
                jsonElement = JSON_OBJECT;
                messageContext.setProperty(ORG_APACHE_SYNAPSE_COMMONS_JSON_IS_JSON_OBJECT, true);
            }
            if (isValue) {
                String jsonString = "";
                try {
                    jsonString = IOUtils.toString(json, "UTF-8");

                    messageContext.setProperty(ORG_APACHE_SYNAPSE_COMMONS_JSON_IS_JSON_OBJECT, false);
                    if (jsonString.startsWith("\"") && jsonString.endsWith("\"")|| jsonString.equals("true") || jsonString.equals("false") ||
                            jsonString.equals("null") || jsonString.matches("-?\\d+(\\.\\d+)?")) {
                        jsonElement = JSON_VALUE;
                    } else {
                        throw new AxisFault(
                                "#getNewJsonPayload. Could not save JSON payload. Invalid input stream found. Payload"
                                        + " is not a JSON string.");
                    }
                    OMElement element = OMAbstractFactory.getOMFactory().createOMElement(jsonElement, null);
                    element.addChild(OMAbstractFactory.getOMFactory().createOMText(jsonString));
                    if (removeChildren) {
                        removeChildrenFromPayloadBody(messageContext);
                        if (addAsNewFirstChild) {
                            addPayloadBody(messageContext, element);
                        }
                    }
                    return element;
                } catch (IOException e) {
                    throw new AxisFault(
                            "#Can not parse stream. MessageID: " + messageContext.getMessageID() + ". Error>>> "
                                    + e.getLocalizedMessage(), e);
                }
            }
            if (isArray) {
                jsonElement = JSON_ARRAY;
                messageContext.setProperty(ORG_APACHE_SYNAPSE_COMMONS_JSON_IS_JSON_OBJECT, false);
            }
            OMElement elem = new OMSourcedElementImpl(jsonElement, OMAbstractFactory.getOMFactory(), new JsonDataSource(
                    (InputStream) messageContext.getProperty(Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM)));
            if (!removeChildren) {
                if (logger.isTraceEnabled()) {
                    logger.trace(
                            "#getNewJsonPayload. Not removing child elements from exiting message. Returning result. MessageID: " +
                            messageContext.getMessageID());
                }
                return elem;
            } else {
                removeChildrenFromPayloadBody(messageContext);
                if (addAsNewFirstChild) {
                    addPayloadBody(messageContext, elem);
                }
            }
            return elem;
        }
        return null;
    }

    /**
     * Remove the children element from the existing message
     *
     * @param messageContext Axis2 Message context
     */
    private static void removeChildrenFromPayloadBody(MessageContext messageContext) {
        SOAPEnvelope envelope = messageContext.getEnvelope();
        if (envelope != null) {
            SOAPBody body = envelope.getBody();
            if (body != null) {
                try {
                    removeIndentations(body);
                } catch (Exception exp) {
                    // This means json payload is malformed.
                    body.getFirstElement().detach();
                    throw  new SynapseCommonsException("Existing json payload is malformed. MessageID : " +
                            messageContext.getMessageID(), exp);
                }
                Iterator children = body.getChildren();
                while (children.hasNext()) {
                    Object child = children.next();
                    if (child instanceof OMNode) {
                        children.remove();
                    }
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("#getNewJsonPayload. Removed child elements from exiting message. MessageID: " +
                            messageContext.getMessageID());
                }
            }
        }
    }

    /**
     * Add the new JSON sourced element as the first child in the payload body
     *
     * @param messageContext Axis2 Message context
     * @param elem          The new payload
     */
    private static void addPayloadBody(MessageContext messageContext, OMElement elem) {
        SOAPEnvelope envelope = messageContext.getEnvelope();
        if (envelope != null) {
            SOAPBody body = envelope.getBody();
            body.addChild(elem);
            if (logger.isTraceEnabled()) {
                logger.trace("#getNewJsonPayload. Added the new JSON sourced element as the first child. MessageID: "
                        + messageContext.getMessageID());
            }
        }
    }

    /**
     * Sets the input JSON stream as a property of the MessageContext
     * @param messageContext the axis2MessageContext
     * @param inputStream the JSON inputStream
     * @return a readonly InputStream
     */
    public static InputStream setJsonStream(MessageContext messageContext, InputStream inputStream) {
        InputStream json = toReadOnlyStream(inputStream);
        messageContext.setProperty(Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM, json);
        return json;
    }

    /**
     * Removes the JSON stream from the the MessageContext
     * This method is used to remove the outdated JSON stream
     * @param messageContext the axis2MessageContext
     */
    public static void removeJsonStream(MessageContext messageContext) {
        messageContext.removeProperty(Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM);
    }

    /**
     * Builds and returns a new JSON payload for a message context with a JSON string. This is deprecated and use
     * getNewJsonPayload for new implementations.
     *
     * @param messageContext     Axis2 Message context to which the new JSON payload must be saved (if instructed with <tt>addAsNewFirstChild</tt>).
     * @param jsonString         JSON content as a String.
     * @param removeChildren     Whether to remove existing child nodes of the existing payload of the message context
     * @param addAsNewFirstChild Whether to add the new JSON payload as the first child of this message context *after* removing the existing first child element.<br/>
     *                           Setting this argument to <tt>true</tt> will have no effect if the value of the argument <tt>removeChildren</tt> is already <tt>false</tt>.
     * @return Payload object that stores the input JSON content as a Sourced object (See {@link org.apache.axiom.om.OMSourcedElement}) that facilitates on demand building of the XML tree.
     * @see #getNewJsonPayload(org.apache.axis2.context.MessageContext, java.io.InputStream, boolean, boolean)
     */
    @Deprecated
    public static OMElement newJsonPayload(MessageContext messageContext, String jsonString, boolean removeChildren,
                                           boolean addAsNewFirstChild) {
        try {
            return getNewJsonPayload(messageContext, jsonString, removeChildren, addAsNewFirstChild);
        } catch (AxisFault axisFault) {
            logger.error("Payload provided is not an JSON payload.");
            return null;
        }
    }

    /**
     * Builds and returns a new JSON payload for a message context with a JSON string.<br/>
     *
     * @param messageContext     Axis2 Message context to which the new JSON payload must be saved (if instructed with <tt>addAsNewFirstChild</tt>).
     * @param jsonString         JSON content as a String.
     * @param removeChildren     Whether to remove existing child nodes of the existing payload of the message context
     * @param addAsNewFirstChild Whether to add the new JSON payload as the first child of this message context *after* removing the existing first child element.<br/>
     *                           Setting this argument to <tt>true</tt> will have no effect if the value of the argument <tt>removeChildren</tt> is already <tt>false</tt>.
     * @return Payload object that stores the input JSON content as a Sourced object (See {@link org.apache.axiom.om.OMSourcedElement}) that facilitates on demand building of the XML tree.
     * @see #getNewJsonPayload(org.apache.axis2.context.MessageContext, java.io.InputStream, boolean, boolean)
     */
    public static OMElement getNewJsonPayload(MessageContext messageContext, String jsonString, boolean removeChildren,
                                              boolean addAsNewFirstChild) throws AxisFault {
        if (jsonString == null || jsonString.isEmpty()) {
            jsonString = "{}";
        }
        return getNewJsonPayload(messageContext, new ByteArrayInputStream(jsonString.getBytes()), removeChildren,
                                 addAsNewFirstChild);
    }

    /**
     * Builds and returns a new JSON payload for a message context with a byte array containing JSON.This method is
     * now deprecated and replaced by getNewJsonPayload method.
     *
     * @param messageContext     Axis2 Message context to which the new JSON payload must be saved (if instructed with <tt>addAsNewFirstChild</tt>).
     * @param json               JSON content as a byte array.
     * @param offset             starting position of the JSON content in the provided array
     * @param length             how many bytes to read starting from the offset provided.
     * @param removeChildren     Whether to remove existing child nodes of the existing payload of the message context
     * @param addAsNewFirstChild Whether to add the new JSON payload as the first child of this message context *after* removing the existing first child element.<br/>
     *                           Setting this argument to <tt>true</tt> will have no effect if the value of the argument <tt>removeChildren</tt> is already <tt>false</tt>.
     * @return Payload object that stores the input JSON content as a Sourced object (See {@link org.apache.axiom.om.OMSourcedElement}) that facilitates on demand building of the XML tree.
     * @see #getNewJsonPayload(org.apache.axis2.context.MessageContext, java.io.InputStream, boolean, boolean)
     */
    @Deprecated
    public static OMElement newJsonPayload(MessageContext messageContext, byte[] json, int offset, int length,
                                              boolean removeChildren, boolean addAsNewFirstChild) {
        try {
            return  getNewJsonPayload(messageContext, json, offset, length,
            removeChildren, addAsNewFirstChild);
        } catch (AxisFault axisFault) {
            logger.error("Payload provided is not an JSON payload.");
            return null;
        }
    }

    /**
     * Builds and returns a new JSON payload for a message context with a byte array containing JSON.<br/>
     *
     * @param messageContext     Axis2 Message context to which the new JSON payload must be saved (if instructed with <tt>addAsNewFirstChild</tt>).
     * @param json               JSON content as a byte array.
     * @param offset             starting position of the JSON content in the provided array
     * @param length             how many bytes to read starting from the offset provided.
     * @param removeChildren     Whether to remove existing child nodes of the existing payload of the message context
     * @param addAsNewFirstChild Whether to add the new JSON payload as the first child of this message context *after* removing the existing first child element.<br/>
     *                           Setting this argument to <tt>true</tt> will have no effect if the value of the argument <tt>removeChildren</tt> is already <tt>false</tt>.
     * @return Payload object that stores the input JSON content as a Sourced object (See {@link org.apache.axiom.om.OMSourcedElement}) that facilitates on demand building of the XML tree.
     * @see #getNewJsonPayload(org.apache.axis2.context.MessageContext, java.io.InputStream, boolean, boolean)
     */
    public static OMElement getNewJsonPayload(MessageContext messageContext, byte[] json, int offset, int length,
                                              boolean removeChildren, boolean addAsNewFirstChild) throws AxisFault{
        InputStream is;
        if (json == null || json.length < 2) {
            json = new byte[]{'{', '}'};
            is = new ByteArrayInputStream(json);
        } else {
            is = new ByteArrayInputStream(json, offset, length);
        }
        return getNewJsonPayload(messageContext, is, removeChildren, addAsNewFirstChild);
    }

    /**
     * Removes the existing JSON payload of a message context if any.<br/>
     * This method can only remove a JSON payload that has been set with {@link #getNewJsonPayload(org.apache.axis2.context.MessageContext, java.io.InputStream, boolean, boolean)}
     * and its variants.
     *
     * @param messageContext Axis2 Message context from which the JSON stream must be removed.
     * @return <tt>true</tt> if the operation is successful.
     */
    public static boolean removeJsonPayload(MessageContext messageContext) {
        messageContext.removeProperty(Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM);
        messageContext.removeProperty(ORG_APACHE_SYNAPSE_COMMONS_JSON_IS_JSON_OBJECT);
        boolean removeChildren = true;
        if (!removeChildren) { // don't change this.
            if (logger.isTraceEnabled()) {
                logger.trace("#removeJsonPayload. Removed JSON stream. MessageID: " + messageContext.getMessageID());
            }
            return true;
        }
        SOAPEnvelope e = messageContext.getEnvelope();
        if (e != null) {
            SOAPBody b = e.getBody();
            if (b != null) {
                Iterator children = b.getChildren();
                while (children.hasNext()) {
                    Object o = children.next();
                    if (o instanceof OMNode) {
                        //((OMNode) o).detach();
                        children.remove();
                    }
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("#removeJsonPayload. Removed JSON stream and child elements of payload. MessageID: "
                            + messageContext.getMessageID());
                }
            }
        }
        return true;
    }

    /**
     * Returns the JSON stream associated with the payload of this message context.
     *
     * @param messageContext Axis2 Message context
     * @param reset          Whether to reset the input stream that contains this JSON payload so that next read will start from the beginning of this stream.
     * @return JSON input stream
     */
    private static InputStream jsonStream(MessageContext messageContext, boolean reset) {
        if (messageContext == null) {
            return null;
        }
        Object o = messageContext.getProperty(Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM);
        if (o instanceof InputStream) {
            InputStream is = (InputStream) o;
            if (reset) {
                if (is.markSupported()) {
                    try {
                        is.reset();
                    } catch (IOException e) {
                        logger.error("#jsonStream. Could not reuse JSON Stream. Error>>>\n", e);
                        return null;
                    }
                }
            }
            return is;
        }
        return null;
    }

    /**
     * Returns the READ-ONLY input stream of the JSON payload contained in the provided message context.
     *
     * @param messageContext Axis2 Message context
     * @return {@link java.io.InputStream} of JSON payload contained in the message context. Null otherwise.<br/>
     * It is possible to read from this stream right away. This InputStream cannot be <tt>close</tt>d, <tt>mark</tt>ed, or <tt>skip</tt>ped. <br/>
     * If <tt>close()</tt> is invoked on this input stream, it will be reset to the beginning.
     */
    public static InputStream getJsonPayload(MessageContext messageContext) {
        return hasAJsonPayload(messageContext) ? jsonStream(messageContext, true) : null;
    }

    /**
     * Returns a copy of the JSON stream contained in the provided Message Context.
     *
     * @param messageContext Axis2 Message context that contains a JSON payload.
     * @return {@link java.io.InputStream}
     */
    private static InputStream copyOfJsonPayload(MessageContext messageContext, boolean closable) {
        if (messageContext == null) {
            logger.error("#copyOfJsonPayload. Cannot copy JSON stream from message context. [null].");
            return null;
        }
        InputStream jsonStream = jsonStream(messageContext, true);
        if (jsonStream == null) {
            logger.error("#copyOfJsonPayload. Cannot copy JSON stream from message context. [null] stream.");
            return null;
        }
        org.apache.commons.io.output.ByteArrayOutputStream out = new org.apache.commons.io.output.ByteArrayOutputStream();
        try {
            IOUtils.copy(jsonStream, out);
            out.flush();
            return closable ? new ByteArrayInputStream(out.toByteArray())
                    : toReadOnlyStream(new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            logger.error("#copyOfJsonPayload. Could not copy the JSON stream from message context. Error>>> " + e.getLocalizedMessage());
        }
        return null;
    }

    private static void writeJsonStream(InputStream json, MessageContext messageContext, OutputStream out) throws AxisFault {
        try {
            if (json.markSupported()) {
                json.reset();
            }
            IOUtils.copy(json, out); // Write the JSON stream
            if (messageContext.getProperty(PRESERVE_JSON_STREAM) != null) {
                if (json.markSupported()) {
                    json.reset();
                }
                messageContext.removeProperty(PRESERVE_JSON_STREAM);
            }
        } catch (IOException e) {
            logger.error("#writeJsonStream. Could not write JSON stream. MessageID: "
                    + messageContext.getMessageID() + ". Error>> " + e.getLocalizedMessage());
            throw new AxisFault("Could not write JSON stream.", e);
        }
    }

    /**
     * Returns a reusable cached copy of the JSON stream contained in the provided Message Context.
     *
     * @param messageContext Axis2 Message context that contains a JSON payload.
     * @return {@link java.io.InputStream}
     */
    private static InputStream cachedCopyOfJsonPayload(MessageContext messageContext) {
        if (messageContext == null) {
            logger.error("#cachedCopyOfJsonPayload. Cannot copy JSON stream from message context. [null].");
            return null;
        }
        InputStream jsonStream = jsonStream(messageContext, true);
        if (jsonStream == null) {
            logger.error("#cachedCopyOfJsonPayload. Cannot copy JSON stream from message context. [null] stream.");
            return null;
        }
        String inputStreamCache = Long.toString(jsonStream.hashCode());
        Object o = messageContext.getProperty(inputStreamCache);
        if (o instanceof InputStream) {
            InputStream inputStream = (InputStream) o;
            try {
                inputStream.reset();
                if (logger.isDebugEnabled()) {
                    logger.debug("#cachedCopyOfJsonPayload. Cache HIT");
                }
                return inputStream;
            } catch (IOException e) {
                logger.warn("#cachedCopyOfJsonPayload. Could not reuse the cached input stream. Error>>> " + e.getLocalizedMessage());
            }
        }
        org.apache.commons.io.output.ByteArrayOutputStream out = new org.apache.commons.io.output.ByteArrayOutputStream();
        try {
            IOUtils.copy(jsonStream, out);
            out.flush();
            InputStream inputStream = toReadOnlyStream(new ByteArrayInputStream(out.toByteArray()));
            messageContext.setProperty(inputStreamCache, inputStream);
            if (logger.isDebugEnabled()) {
                logger.debug("#cachedCopyOfJsonPayload. Cache MISS");
            }
            return inputStream;
        } catch (IOException e) {
            logger.error("#cachedCopyOfJsonPayload. Could not copy the JSON stream from message context. Error>>> " + e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Returns a new instance of a reader that can read from the JSON payload contained in the provided message context.
     *
     * @param messageContext Axis2 Message context
     * @return {@link java.io.Reader} if a JSON payload is found in the message context. null otherwise.
     */
    public static Reader newJsonPayloadReader(MessageContext messageContext) {
        if (messageContext == null) {
            return null;
        }
        InputStream is = jsonStream(messageContext, true);
        if (is == null) {
            return null;
        }
        return new InputStreamReader(is);
    }

    /**
     * Returns the JSON payload contained in the provided message context as a byte array.
     *
     * @param messageContext Axis2 Message context
     * @return <tt>byte</tt> array containing the JSON payload. Empty array if no JSON payload found or invalid message context is passed in.
     */
    public static byte[] jsonPayloadToByteArray(MessageContext messageContext) {
        if (messageContext == null) {
            return new byte[0];
        }
        InputStream is = jsonStream(messageContext, true);
        if (is == null) {
            return new byte[0];
        }
        try {
            return IOUtils.toByteArray(is); // IOUtils.toByteArray() doesn't close the input stream.
        } catch (IOException e) {
            logger.warn("#jsonPayloadToByteArray. Could not convert JSON stream to byte array.");
            return new byte[0];
        }
    }

    /**
     * Returns the JSON payload contained in the provided message context as a String.
     *
     * @param messageContext Axis2 Message context
     * @return <tt>java.lang.String</tt> representation of the JSON payload. Returns "{}" if no JSON payload found or invalid message context is passed in.
     */
    public static String jsonPayloadToString(MessageContext messageContext) {
        if (messageContext == null) {
            return "{}";
        }
        InputStream is = jsonStream(messageContext, true);
        if (is == null) {
            return "{}";
        }
        try {
            return IOUtils.toString(is); // IOUtils.toByteArray() doesn't close the input stream.
        } catch (IOException e) {
            logger.warn("#jsonPayloadToString. Could not convert JSON stream to String.");
            return "{}";
        }
    }

    /**
     * Returns whether the provided XML element is an element that stores a sourced JSON payload.
     *
     * @param element XML element
     * @return <tt>true</tt> if the element is a sourced JSON object (ie. an <tt>OMSourcedElement</tt> instance containing a JSON stream).
     */
    public static boolean hasAJsonPayload(OMElement element) {
        return ((element instanceof OMSourcedElementImpl) || (element instanceof OMElementImpl)) && isAJsonPayloadElement(element);
    }

    /**
     * Returns true if the element passed in as the parameter is an element that contains a JSON stream.
     *
     * @param element XML element
     * @return <tt>true</tt> if the element has the local name of a sourced (ie. an <tt>OMSourcedElement</tt>) JSON object.
     */
    public static boolean isAJsonPayloadElement(OMElement element) {
        return element != null
                && (JSON_OBJECT.getLocalPart().equals(element.getLocalName())
                || JSON_ARRAY.getLocalPart().equals(element.getLocalName())
                || JSON_VALUE.getLocalPart().equals(element.getLocalName()));
    }

    /**
     * Returns true if the payload stored in the provided message context is used as a JSON streaming payload.
     *
     * @param messageContext Axis2 Message context
     * @return <tt>true</tt> if the message context contains a Streaming JSON payload.
     */
    public static boolean hasAJsonPayload(MessageContext messageContext) {
        if (messageContext == null || messageContext.getEnvelope() == null) {
            return false;
        }
        SOAPBody b = messageContext.getEnvelope().getBody();
        return b != null && jsonStream(messageContext, false) != null && hasAJsonPayload(b.getFirstElement());
    }

    /**
     * Clones the JSON stream payload contained in the source message context, if any, to the target message context.
     *
     * @param sourceMc Where to get the payload
     * @param targetMc Where to clone and copy the payload
     * @return <tt>true</tt> if the cloning was successful.
     */
    public static boolean cloneJsonPayload(MessageContext sourceMc, MessageContext targetMc) {
        if (!hasAJsonPayload(sourceMc)) {
            return false;
        }
        InputStream json = jsonStream(sourceMc, true);
        try {
            byte[] stream = IOUtils.toByteArray(json);
            getNewJsonPayload(targetMc, new ByteArrayInputStream(stream), true, true);
        } catch (IOException e) {
            logger.error("#cloneJsonPayload. Could not clone JSON stream. Error>>> " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    /**
     * Sets JSON media type 'application/json' as the message type to the current message context.
     *
     * @param messageContext Axis2 Message context
     */
    public static void setContentType(MessageContext messageContext) {
        if (messageContext == null) {
            return;
        }
        messageContext.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, "application/json");
    }

    /**
     * Returns a read only, re-readable input stream for an input stream. <br/>
     * The returned input stream cannot be closed, marked, or skipped, but it can be reset to the beginning of the stream.
     *
     * @param inputStream Input stream to be wrapped
     * @return {@link java.io.InputStream}
     */
    public static InputStream toReadOnlyStream(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        return new ReadOnlyBIS(inputStream);
    }

    /**
     * Returns an input stream that contains the JSON representation of an XML element.
     *
     * @param element XML element of which JSON representation is expected.
     * @return {@link java.io.InputStream}
     */
    public static InputStream toJsonStream(OMElement element) {
        if (element == null) {
            logger.error("#toJsonStream. Could not create input stream from XML element [null]");
            return null;
        }
        org.apache.commons.io.output.ByteArrayOutputStream bos = new org.apache.commons.io.output.ByteArrayOutputStream();
        try {
            JsonUtil.writeAsJson(element.cloneOMElement(), bos);
        } catch (AxisFault axisFault) {
            logger.error("#toJsonStream. Could not create input stream from XML element ["
                    + element.toString() + "]. Error>>> " + axisFault.getLocalizedMessage());
            return null;
        }
        return new ByteArrayInputStream(bos.toByteArray());
    }

    /**
     * Returns a reader that can read from the JSON payload contained in the provided message context as a JavaScript source.<br/>
     * The reader returns the '(' character at the beginning of the stream and marks the end with the ')' character.<br/>
     * The reader returned by this method can be directly used with the JavaScript {@link javax.script.ScriptEngine#eval(java.io.Reader)} method.
     *
     * @param messageContext Axis2 Message context
     * @return {@link java.io.InputStreamReader}
     */
    public static Reader newJavaScriptSourceReader(MessageContext messageContext) {
        InputStream jsonStream = jsonStream(messageContext, true);
        if (jsonStream == null) {
            logger.error("#newJavaScriptSourceReader. Could not create a JavaScript source. Error>>> No JSON stream found.");
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write('(');
            IOUtils.copy(jsonStream, out);
            out.write(')');
            out.flush();
        } catch (IOException e) {
            logger.error("#newJavaScriptSourceReader. Could not create a JavaScript source. Error>>> " + e.getLocalizedMessage());
            return null;
        }
        return new InputStreamReader(new ByteArrayInputStream(out.toByteArray()));
    }

    /**
     * Returns <tt>true</tt> if the message context contains a JSON payload that is a JSON Object. See {@link #hasAJsonArray(MessageContext)}<br/>
     * Example : {"a":1, "b":2}
     *
     * @param messageContext request message context
     * @return
     */
    public static boolean hasAJsonObject(MessageContext messageContext) {
        return hasAJsonPayload(messageContext) && _hasAJsonObject(messageContext);
    }

    /**
     * Returns <tt>true</tt> if the message context contains a JSON payload that is a JSON Array. See {@link #hasAJsonObject(MessageContext)}<br/>
     * Example: [{"a":1}, 2, null]
     *
     * @param messageContext request message context
     * @return
     */
    public static boolean hasAJsonArray(MessageContext messageContext) {
        return hasAJsonPayload(messageContext) && !_hasAJsonObject(messageContext);
    }

    private static boolean _hasAJsonObject(MessageContext messageContext) {
        Object isObject = messageContext.getProperty(ORG_APACHE_SYNAPSE_COMMONS_JSON_IS_JSON_OBJECT);
        return isObject != null && ((Boolean) isObject);
    }

    /**
     * An Un-closable, Read-Only, Reusable, BufferedInputStream
     */
    private static class ReadOnlyBIS extends BufferedInputStream {
        private static final String LOG_STREAM = "org.apache.synapse.commons.json.JsonReadOnlyStream";
        private static final Log logger = LogFactory.getLog(LOG_STREAM);

        public ReadOnlyBIS(InputStream inputStream) {
            super(inputStream);
            super.mark(Integer.MAX_VALUE);
            if (logger.isDebugEnabled()) {
                logger.debug("<init>");
            }
        }

        @Override
        public void close() throws IOException {
            super.reset();
            //super.mark(Integer.MAX_VALUE);
            if (logger.isDebugEnabled()) {
                logger.debug("#close");
            }
        }

        @Override
        public void mark(int readlimit) {
            if (logger.isDebugEnabled()) {
                logger.debug("#mark");
            }
        }

        @Override
        public boolean markSupported() {
            return true; //but we don't mark.
        }

        @Override
        public long skip(long n) {
            if (logger.isDebugEnabled()) {
                logger.debug("#skip");
            }
            return 0;
        }
    }

    /**
     * Check whether the request HTTP method is required valid payload
     *
     * @param msgCtx Message Context of incoming request
     * @return true if payload required, false otherwise
     */
    private static boolean isValidPayloadRequired(MessageContext msgCtx) {
        boolean isRequired = true;
        if (HTTPConstants.HEADER_GET.equals(msgCtx.getProperty(HTTPConstants.HTTP_METHOD)) || HTTPConstants
                .HEADER_DELETE.equals(msgCtx.getProperty(HTTPConstants.HTTP_METHOD))) {
            isRequired = false;
        }
        return isRequired;
    }

    /**
     * Returns the configured value of the parameter (synapse.json.to.xml.process.instruction.enabled).
     *
     * @return true to inform staxon library to add PIs to JSON -> XML conversion
     */
    public static boolean isPiEnabled() {
        return isJsonToXmlPiEnabled;
    }
}
