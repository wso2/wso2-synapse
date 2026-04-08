/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.mediators.bsf.utils;

import java.io.IOException;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class contains secure utility methods to handle XML payloads for the script mediator.
 */
public class ScriptUtils {

    public static Document parseXml(String text) throws IOException, SAXException {
        InputSource sax = new InputSource(new java.io.StringReader(text));
        DOMParser parser = new DOMParser();
        // 1. Disable external DTDs completely
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        // 2. Disable external entities (General)
        parser.setFeature("http://xml.org/sax/features/external-general-entities", false);
        // 3. Disable external entities (Parameter)
        parser.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // 4. Ignore external DTDs if they are present
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        parser.parse(sax);
        Document doc = parser.getDocument();
        doc.getDocumentElement().normalize();
        return doc;
    }
}
