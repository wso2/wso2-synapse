/*
 Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Class responsible for removing the unwanted whitespaces in any type of inputs.
 */
public class RequestProcessor {

    RequestProcessor() {
    }

    private static Logger logger = Logger.getLogger(RequestProcessor.class.getName());

    /**
     * Remove irrelevant whitespaces from the input string.
     *
     * @param inputString string which needs to remove whitespaces
     * @return trim string not include irrelevant whitespaces
     */
    public static String trimStrings(String inputString) {

        //trim the string
        String trimedString = inputString.trim();

        //check whether string is an XML
        if (trimedString.startsWith("<")) {

            //remove CDATA tag from the string
            if (trimedString.startsWith("<![CDATA[")) {
                trimedString = trimedString.substring(9);
                int i = trimedString.indexOf("]]>");
                if (i == -1)
                    throw new IllegalStateException("argument starts with <![CDATA[ but cannot find pairing ]]>");
                trimedString = trimedString.substring(0, i);
            }

            //convert string to XML DOM
            Document xmlDOM = convertStringToXMLDocument(trimedString);
            trimedString = nodeToString(xmlDOM);
            trimedString = trimedString.replaceAll("xmlns=\"\"", "");

        } else if (trimedString.startsWith("{")) {
            JsonObject inputJSON = new JsonParser()
                    .parse(trimedString).getAsJsonObject();
            trimedString = inputJSON.toString();
        }

        return trimedString.replaceAll("\\s", "");
    }

    private static Document convertStringToXMLDocument(String xmlString) {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        //API to obtain DOM Document instance
        DocumentBuilder builder;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();

            //Parse the content to Document object
            return builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            logger.error("Error while parsing xmlString to DOM", e);
        }
        return null;
    }

    private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (Exception e) {
            logger.error("nodeToString Transformer Exception", e);
        }
        return sw.toString();
    }
}
