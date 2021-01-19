/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */


package org.apache.synapse.mediators.transform.pfutils;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.mediators.transform.Argument;
import org.apache.synapse.mediators.transform.ArgumentDetails;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import static org.apache.synapse.mediators.transform.PayloadFactoryMediator.QUOTE_STRING_IN_PAYLOAD_FACTORY_JSON;

/**
 * Abstract TemplateProcessor. This is the class using by the 
 */
public abstract class TemplateProcessor {

    protected static final String JSON_TYPE = "json";
    protected static final String XML_TYPE = "xml";
    protected static final String TEXT_TYPE = "text";
    protected static final String STRING_TYPE = "str";
    protected static final String ESCAPE_DOUBLE_QUOTE_WITH_FIVE_BACK_SLASHES = "\\\\\"";
    protected static final String ESCAPE_DOUBLE_QUOTE_WITH_NINE_BACK_SLASHES = "\\\\\\\\\"";
    protected static final String ESCAPE_BACK_SLASH_WITH_SIXTEEN_BACK_SLASHES = "\\\\\\\\\\\\\\\\";
    protected static final String ESCAPE_DOLLAR_WITH_SIX_BACK_SLASHES = "\\\\\\$";
    protected static final String ESCAPE_DOLLAR_WITH_TEN_BACK_SLASHES = "\\\\\\\\\\$";
    protected static final String ESCAPE_BACKSPACE_WITH_EIGHT_BACK_SLASHES = "\\\\\\\\b";
    protected static final String ESCAPE_FORMFEED_WITH_EIGHT_BACK_SLASHES = "\\\\\\\\f";
    protected static final String ESCAPE_NEWLINE_WITH_EIGHT_BACK_SLASHES = "\\\\\\\\n";
    protected static final String ESCAPE_CRETURN_WITH_EIGHT_BACK_SLASHES = "\\\\\\\\r";
    protected static final String ESCAPE_TAB_WITH_EIGHT_BACK_SLASHES = "\\\\\\\\t";
    private static final Pattern validJsonNumber = Pattern.compile("^-?(0|([1-9]\\d*))(\\.\\d+)?([eE][+-]?\\d+)?$");
    private static final Log log = LogFactory.getLog(TemplateProcessor.class);
    protected final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    private final List<Argument> pathArgumentList = new ArrayList<>();
    private boolean escapeXmlChars = false;
    private String format;
    private String mediaType = XML_TYPE;

    /**
     * Process the given template and return the output as String
     *
     * @param template        Template string
     * @param mediaType       Output media type
     * @param synCtx          MessageContext
     * @return The processed output
     */
    public abstract String processTemplate(String template, String mediaType, MessageContext synCtx);

    /**
     * Execute pre-processing steps if needed
     */
    public abstract void init();

    /**
     * Goes through SynapsePath argument list, evaluating each by calling stringValueOf and returns a HashMap String, String
     * array where each item will contain a hash map with key "evaluated expression" and value "SynapsePath type".
     *
     * @param synCtx MessageContext
     * @return
     */
    protected HashMap<String, ArgumentDetails>[] getArgValues(String mediaType, MessageContext synCtx) {

        HashMap<String, ArgumentDetails>[] argValues = new HashMap[pathArgumentList.size()];
        HashMap<String, ArgumentDetails> valueMap;
        String value = "";
        for (int i = 0; i < pathArgumentList.size(); ++i) {       /*ToDo use foreach*/
            Argument arg = pathArgumentList.get(i);
            ArgumentDetails details = new ArgumentDetails();
            if (arg.getValue() != null) {
                value = arg.getValue();
                details.setXml(isXML(value));
                if (!details.isXml()) {
                    value = escapeXMLEnvelope(synCtx, value);
                }
                value = Matcher.quoteReplacement(value);
            } else if (arg.getExpression() != null) {
                value = arg.getExpression().stringValueOf(synCtx);
                details.setLiteral(arg.isLiteral());
                if (value != null) {
                    // XML escape the result of an expression that produces a literal, if the target format
                    // of the payload is XML.
                    details.setXml(isXML(value));
                    if (!details.isXml() && XML_TYPE.equals(mediaType) && !isJson(value.trim(), arg.getExpression())) {
                        value = escapeXMLEnvelope(synCtx, value);
                    }
                    value = Matcher.quoteReplacement(value);
                } else {
                    value = "";
                }
            } else {
                handleException("Unexpected arg type detected");
            }
            //value = value.replace(String.valueOf((char) 160), " ").trim();
            valueMap = new HashMap<>();
            if (null != arg.getExpression()) {
                details.setPathType(arg.getExpression().getPathType());
                valueMap.put(value, details);
            } else {
                details.setPathType(SynapsePath.X_PATH);
                valueMap.put(value, details);
            }
            argValues[i] = valueMap;
        }
        return argValues;
    }

    /**
     * Preprocess and converty types of the given arg value.
     *
     * @param mediaType        Output media type
     * @param synCtx           Message context
     * @param replacementEntry Argument
     * @return Preprocessed value
     */
    protected String prepareReplacementValue(String mediaType, MessageContext synCtx,
                                             Map.Entry<String, ArgumentDetails> replacementEntry) {

        String replacementValue = null;

        if (mediaType.equals(JSON_TYPE) && inferReplacementType(replacementEntry).equals(XML_TYPE)) {
            // XML to JSON conversion here
            replacementValue = convertXmlArgumentToJson(replacementEntry, replacementValue);
        } else if (mediaType.equals(XML_TYPE) && inferReplacementType(replacementEntry).equals(JSON_TYPE)) {
            // JSON to XML conversion here
            replacementValue = convertJsonArgumentToXml(replacementEntry, replacementValue);
        } else {
            // No conversion required, as path evaluates to regular String.
            replacementValue = replacementEntry.getKey();
            String trimmedReplacementValue = replacementValue.trim();
            //If media type is xml and replacement value is json convert it to xml format prior to replacement
            if (mediaType.equals(XML_TYPE) && inferReplacementType(replacementEntry).equals(STRING_TYPE)
                    && isJson(trimmedReplacementValue)) {
                replacementValue = convertJsonStringToXml(replacementValue);
            } else if (mediaType.equals(JSON_TYPE) &&
                    inferReplacementType(replacementEntry).equals(JSON_TYPE) &&
                    isEscapeXmlChars()) {
                //checks whether the escapeXmlChars attribute is true when media-type and evaluator is json and
                //escapes xml chars. otherwise json messages with non escaped xml characters will fail to build
                //in content aware mediators.
                replacementValue = escapeXMLSpecialChars(replacementValue);
            } else if (mediaType.equals(JSON_TYPE) &&
                    inferReplacementType(replacementEntry).equals(STRING_TYPE)) {
                replacementValue = escapeSpecialChars(replacementValue);
                if (!trimmedReplacementValue.startsWith("{") && !trimmedReplacementValue.startsWith("[")) {
                    // Check for following property which will force the string to include quotes
                    Object force_string_quote = synCtx.getProperty(QUOTE_STRING_IN_PAYLOAD_FACTORY_JSON);
                    // skip double quotes if replacement is boolean or null or valid json number
                    if (force_string_quote != null && ((String) force_string_quote).equalsIgnoreCase("true")
                            && !trimmedReplacementValue.equals("true") && !trimmedReplacementValue.equals("false")
                            && !trimmedReplacementValue.equals("null")
                            && !validJsonNumber.matcher(trimmedReplacementValue).matches()) {
                        replacementValue = "\"" + replacementValue + "\"";
                    }
                }
            } else if (
                    (mediaType.equals(JSON_TYPE) && inferReplacementType(replacementEntry).equals(JSON_TYPE)) &&
                            (!trimmedReplacementValue.startsWith("{") &&
                                    !trimmedReplacementValue.startsWith("["))) {
                // This is to handle only the string value
                replacementValue =
                        replacementValue.replaceAll("\"", ESCAPE_DOUBLE_QUOTE_WITH_NINE_BACK_SLASHES);
            }
        }
        return replacementValue;
    }

    /**
     * Convert Json string value to XML
     *
     * @param replacementValue Replacement value string
     * @return Converted value
     */
    private String convertJsonStringToXml(String replacementValue) {

        try {
            replacementValue = escapeSpecialCharactersOfXml(replacementValue);
            OMElement omXML = JsonUtil.toXml(IOUtils.toInputStream(replacementValue), false);
            if (JsonUtil.isAJsonPayloadElement(omXML)) { // remove <jsonObject/> from result.
                Iterator children = omXML.getChildElements();
                String childrenStr = "";
                while (children.hasNext()) {
                    childrenStr += (children.next()).toString().trim();
                }
                replacementValue = childrenStr;
            } else {
                replacementValue = omXML.toString();
            }
        } catch (AxisFault e) {
            handleException("Error converting JSON to XML, please check your JSON Path expressions"
                    + " return valid JSON: ");
        }
        return replacementValue;
    }

    /**
     * Convert JSON argument to XML
     *
     * @param replacementEntry Argument
     * @param replacementValue Replacement Value
     * @return
     */
    private String convertJsonArgumentToXml(Map.Entry<String, ArgumentDetails> replacementEntry,
                                            String replacementValue) {

        try {
            replacementValue = replacementEntry.getKey();
            replacementValue = escapeSpecialCharactersOfXml(replacementValue);
            OMElement omXML = JsonUtil.toXml(IOUtils.toInputStream(replacementValue), false);
            if (JsonUtil.isAJsonPayloadElement(omXML)) { // remove <jsonObject/> from result.
                Iterator children = omXML.getChildElements();
                String childrenStr = "";
                while (children.hasNext()) {
                    childrenStr += (children.next()).toString().trim();
                }
                replacementValue = childrenStr;
            } else { ///~
                replacementValue = omXML.toString();
            }
            //replacementValue = omXML.toString();
        } catch (AxisFault e) {
            handleException(
                    "Error converting JSON to XML, please check your JSON Path expressions return valid JSON: ");
        }
        return replacementValue;
    }

    /**
     * Convert XML argument to JSON
     *
     * @param replacementEntry Entity
     * @param replacementValue Replacement value
     * @return Converted value
     */
    private String convertXmlArgumentToJson(Map.Entry<String, ArgumentDetails> replacementEntry,
                                            String replacementValue) {

        try {
            replacementValue = "<jsonObject>" + replacementEntry.getKey() + "</jsonObject>";
            OMElement omXML = convertStringToOM(replacementValue);
            replacementValue = JsonUtil.toJsonString(omXML).toString();
            replacementValue = escapeSpecialCharactersOfJson(replacementValue);
        } catch (XMLStreamException e) {
            handleException(
                    "Error parsing XML for JSON conversion, please check your xPath expressions return valid XML: ");
        } catch (AxisFault e) {
            handleException("Error converting XML to JSON");
        } catch (OMException e) {
            //if the logic comes to this means, it was tried as a XML, which means it has
            // "<" as starting element and ">" as end element, so basically if the logic comes here, that means
            //value is a string value, that means No conversion required, as path evaluates to regular String.
            replacementValue = replacementEntry.getKey();

            // This is to replace " with \" and \\ with \\\\
            //replacing other json special characters i.e \b, \f, \n \r, \t
            replacementValue = escapeSpecialChars(replacementValue);

        }
        return replacementValue;
    }

    /**
     * Helper function that takes a Map of String, ArgumentDetails where key contains the value of an evaluated
     * SynapsePath expression and value contains the type of SynapsePath + deepcheck status in use.
     * <p>
     * It returns the type of conversion required (XML | JSON | String) based on the actual returned value and the path
     * type.
     *
     * @param entry
     * @return
     */
    protected String inferReplacementType(Map.Entry<String, ArgumentDetails> entry) {

        if (entry.getValue().isLiteral()) {
            return STRING_TYPE;
        } else if (entry.getValue().getPathType().equals(SynapsePath.X_PATH)
                && entry.getValue().isXml()) {
            return XML_TYPE;
        } else if (entry.getValue().getPathType().equals(SynapsePath.X_PATH)
                && !entry.getValue().isXml()) {
            return STRING_TYPE;
        } else if (entry.getValue().getPathType().equals(SynapsePath.JSON_PATH)
                && isJson(entry.getKey())) {
            return JSON_TYPE;
        } else if (entry.getValue().getPathType().equals(SynapsePath.JSON_PATH)
                && !isJson((entry.getKey()))) {
            return STRING_TYPE;
        } else {
            return STRING_TYPE;
        }
    }

    /**
     * Helper function that returns true if value passed is of JSON type.
     *
     * @param value
     * @return
     */
    protected boolean isJson(String value) {

        return !(value == null || value.trim().isEmpty()) &&
                (value.trim().charAt(0) == '{' || value.trim().charAt(0) == '[');
    }

    /**
     * Helper function that returns true if value passed is of JSON type and expression is JSON.
     */
    private boolean isJson(String value, SynapsePath expression) {

        return !(value == null || value.trim().isEmpty()) && (value.trim().charAt(0) == '{'
                || value.trim().charAt(0) == '[') && expression.getPathType().equals(SynapsePath.JSON_PATH);
    }

    /**
     * Helper function that returns true if value passed is of XML Type.
     *
     * @param value
     * @return
     */
    protected boolean isXML(String value) {

        try {
            value = value.trim();
            if (!value.endsWith(">") || value.length() < 4) {
                return false;
            }
            // validate xml
            convertStringToOM(value);
            return true;
        } catch (XMLStreamException | OMException ignore) {
            // means not a xml
            return false;
        }
    }

    /**
     * Converts String to OMElement
     *
     * @param value String value to convert
     * @return parsed OMElement
     */
    protected OMElement convertStringToOM(String value) throws XMLStreamException, OMException {

        javax.xml.stream.XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(new StringReader(value));
        StAXBuilder builder = new StAXOMBuilder(xmlReader);
        return builder.getDocumentElement();
    }

    /**
     * Helper method to replace required char values with escape characters.
     *
     * @param replaceString
     * @return replacedString
     */
    protected String escapeSpecialChars(String replaceString) {

        return replaceString.replaceAll(Matcher.quoteReplacement("\\\\"), ESCAPE_BACK_SLASH_WITH_SIXTEEN_BACK_SLASHES)
                .replaceAll("\"", ESCAPE_DOUBLE_QUOTE_WITH_NINE_BACK_SLASHES)
                .replaceAll("\b", ESCAPE_BACKSPACE_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\f", ESCAPE_FORMFEED_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\n", ESCAPE_NEWLINE_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\r", ESCAPE_CRETURN_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\t", ESCAPE_TAB_WITH_EIGHT_BACK_SLASHES);
    }

    /**
     * Replace special characters of a JSON string.
     *
     * @param jsonString JSON string.
     * @return
     */
    protected String escapeSpecialCharactersOfJson(String jsonString) {
        // This is to replace \" with \\" and \\$ with \$. Because for Matcher, $ sign is
        // a special character and for JSON " is a special character.
        //replacing other json special characters i.e \b, \f, \n \r, \t
        return jsonString.replaceAll(ESCAPE_DOUBLE_QUOTE_WITH_FIVE_BACK_SLASHES,
                ESCAPE_DOUBLE_QUOTE_WITH_NINE_BACK_SLASHES)
                .replaceAll(ESCAPE_DOLLAR_WITH_TEN_BACK_SLASHES, ESCAPE_DOLLAR_WITH_SIX_BACK_SLASHES)
                .replaceAll("\\\\b", ESCAPE_BACKSPACE_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\\\\f", ESCAPE_FORMFEED_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\\\\n", ESCAPE_NEWLINE_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\\\\r", ESCAPE_CRETURN_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\\\\t", ESCAPE_TAB_WITH_EIGHT_BACK_SLASHES);
    }

    /**
     * Replace special characters of a XML string.
     *
     * @param xmlString XML string.
     * @return
     */
    protected String escapeSpecialCharactersOfXml(String xmlString) {

        return xmlString.replaceAll(ESCAPE_DOUBLE_QUOTE_WITH_FIVE_BACK_SLASHES,
                ESCAPE_DOUBLE_QUOTE_WITH_NINE_BACK_SLASHES)
                .replaceAll("\\$", ESCAPE_DOLLAR_WITH_SIX_BACK_SLASHES)
                .replaceAll("\\\\b", ESCAPE_BACKSPACE_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\\\\f", ESCAPE_FORMFEED_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\\\\n", ESCAPE_NEWLINE_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\\\\r", ESCAPE_CRETURN_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\\\\t", ESCAPE_TAB_WITH_EIGHT_BACK_SLASHES);
    }

    /**
     * Helper method to replace required char values with escape characters for XML.
     *
     * @param replaceString
     * @return replacedString
     */
    protected String escapeXMLSpecialChars(String replaceString) {

        return replaceString.replaceAll("\b", ESCAPE_BACKSPACE_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\f", ESCAPE_FORMFEED_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\n", ESCAPE_NEWLINE_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\r", ESCAPE_CRETURN_WITH_EIGHT_BACK_SLASHES)
                .replaceAll("\t", ESCAPE_TAB_WITH_EIGHT_BACK_SLASHES);
    }

    /**
     * Escapes XML special characters
     *
     * @param msgCtx Message Context
     * @param value  XML String which needs to be escaped
     * @return XML special char escaped string
     */
    protected String escapeXMLEnvelope(MessageContext msgCtx, String value) {

        String xmlVersion = "1.0"; //Default is set to 1.0

        try {
            xmlVersion = checkXMLVersion(msgCtx);
        } catch (IOException e) {
            log.error("Error reading message envelope", e);
        } catch (SAXException e) {
            log.error("Error parsing message envelope", e);
        } catch (ParserConfigurationException e) {
            log.error("Error building message envelope document", e);
        }

        if ("1.1".equals(xmlVersion)) {
            return org.apache.commons.text.StringEscapeUtils.escapeXml11(value);
        } else {
            return org.apache.commons.text.StringEscapeUtils.escapeXml10(value);
        }

    }

    /**
     * Checks and returns XML version of the envelope
     *
     * @param msgCtx Message Context
     * @return xmlVersion in XML Declaration
     * @throws ParserConfigurationException failure in building message envelope document
     * @throws IOException                  Error reading message envelope
     * @throws SAXException                 Error parsing message envelope
     */
    private String checkXMLVersion(MessageContext msgCtx)
            throws IOException, SAXException, ParserConfigurationException {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(msgCtx.getEnvelope().toString()));
        Document document = documentBuilder.parse(inputSource);
        return document.getXmlVersion();
    }

    protected boolean isEscapeXmlChars() {

        return escapeXmlChars;
    }

    public void setEscapeXmlChars(boolean escapeXmlChars) {

        this.escapeXmlChars = escapeXmlChars;
    }

    public void addPathArgument(Argument arg) {

        pathArgumentList.add(arg);
    }

    public List<Argument> getPathArgumentList() {

        return pathArgumentList;
    }

    public String getFormat() {

        return format;
    }

    public void setFormat(String format) {

        this.format = format;
    }

    public String getMediaType() {

        return mediaType;
    }

    public void setMediaType(String mediaType) {

        this.mediaType = mediaType;
    }

    protected void handleException(String msg) {

        throw new TemplateProcessorException(msg);
    }

}
