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

package org.apache.synapse.unittest;

import org.apache.log4j.Logger;
import org.json.JSONObject;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;


import static org.apache.synapse.unittest.Constants.WHITESPACE_REGEX;

/**
 * Class responsible for remove the unwanted whitespaces in any type of inputs.
 */
public class Trimmer {

    Trimmer() {
    }

    private static Logger logger = Logger.getLogger(Trimmer.class.getName());

    /**
     * Remove irrelevant whitespaces from the input string.
     *
     * @param inputString string which needs to remove whitespaces
     * @return trim string not include irrelevant whitespaces
     */
    public static String trimStrings(String inputString) {

        String trimedString = inputString.trim();


        if (trimedString.startsWith("<")) {

//            Document doc = convertStringToXMLDocument(trimedString);
//
//            if (doc != null) {
//                trimedString = nodeToString(doc.getDocumentElement());
//            }

            try {
                BufferedReader reader = new BufferedReader(new StringReader(trimedString));
                StringBuilder result = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line.trim());
                }
                trimedString = result.toString();
            } catch (Exception e) {
                logger.error(e);
                trimedString = inputString;
            }

            trimedString = trimedString.replaceAll(WHITESPACE_REGEX, "");

        } else if (trimedString.startsWith("{")) {

            JSONObject inputJson = new JSONObject(trimedString);
            trimedString = inputJson.toString();

            BufferedReader reader = new BufferedReader(new StringReader(trimedString));
            StringBuilder result = new StringBuilder();
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line.trim());
                }
                trimedString = result.toString();
            } catch (IOException e) {
                logger.error(e);
                trimedString = inputString;
            }

            trimedString = trimedString.replaceAll(WHITESPACE_REGEX, "");

        } else {
            trimedString = trimedString.replaceAll(" +", " ");
        }

        return trimedString;
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
            logger.error(e);
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
        } catch (Exception te) {
            logger.error("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

}
