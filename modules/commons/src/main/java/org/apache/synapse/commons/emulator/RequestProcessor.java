/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.emulator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 * Class responsible for removing the unwanted whitespaces in any type of inputs.
 */
public class RequestProcessor {

    RequestProcessor() {
    }

    private static Logger log = Logger.getLogger(RequestProcessor.class.getName());

    /**
     * Remove irrelevant whitespaces from the input string.
     * If string is a XML or JSON reordered the structure of the string to normalize
     *
     * @param inputString string which needs to remove whitespaces
     * @return trim string not include irrelevant whitespaces
     */
    public static String trimStrings(String inputString) {

        //trim the string
        String trimedString = inputString.trim();

        //remove CDATA tag from the string if exists
        if (trimedString.startsWith("<![CDATA[")) {
            trimedString = trimedString.substring(9);
            int index = trimedString.indexOf("]]>");
            if (index == -1) {
                throw new IllegalStateException("argument starts with <![CDATA[ but cannot find pairing ]]>");
            }
            trimedString = trimedString.substring(0, index);
        }

        trimedString = convertStringToRelatedDocumentType(trimedString);
        return trimedString.replaceAll("\\s", "");
    }

    /**
     * Check the input string is a XML type and generate a XML object and convert it to string.
     * Check the input string is a JSON type and generate a JSON object and convert it to string.
     *
     * @param domString inputString which about to convert
     * @return reordered trim string not include irrelevant whitespaces
     */
    private static String convertStringToRelatedDocumentType(String domString) {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        //API to obtain DOM Document instance
        DocumentBuilder builder;

        String processedString;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();

            //Parse the content to Document object
            Document xmlDOM = builder.parse(new InputSource(new StringReader(domString)));
            processedString = nodeToString(xmlDOM);
            processedString = processedString.replaceAll("xmlns=\"\"", "");
        } catch (Exception e) {
            processedString = convertAsJSONString(domString);
        }

        return processedString;
    }

    /**
     * Convert XML document to string.
     *
     * @param node document node which represent XML
     * @return converted document element as a string
     */
    private static String nodeToString(Node node) {
        StringWriter stringWriter = new StringWriter();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
        } catch (Exception e) {
            log.error("nodeToString Transformer Exception", e);
        }
        return stringWriter.toString();
    }

    /**
     * Create JSON object using input string.
     * If failed return as it is.
     *
     * @param inputString input string which is not an XML
     * @return reordered string
     */
    private static String convertAsJSONString(String inputString) {
        try {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapterFactory(new TreeMapTypeAdapterFactory())
                    .create();

            Map root = gson.fromJson(inputString, Map.class);
            return gson.toJson(root);
        } catch (Exception e) {
            return inputString;
        }
    }
}
