/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.bsf;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.util.XMLUtils;
import org.apache.bsf.xml.DefaultXMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.script.ScriptException;
import javax.xml.stream.XMLStreamException;

/**
 * This class will provide the operations to convert between xml elements of scripts and OMElements.
 */
public class NashornJavaScriptXmlHelper extends DefaultXMLHelper {

    /**
     * This method will convert the message payload in to xml.
     *
     * @param scriptXML from java script
     * @return XML content as OMElement
     * @throws ScriptException when error
     */
    public OMElement toOMElement(Object scriptXML) throws ScriptException {
        OMElement omElement;
        if (scriptXML == null) {
            return null;
        } else if (scriptXML instanceof String) {
            try {
                String xmlString = scriptXML.toString();
                omElement = AXIOMUtil.stringToOM(xmlString);

            } catch (XMLStreamException | OMException e) {
                ScriptException scriptException = new ScriptException("Failed to create OMElement with provided " +
                        "payload");
                scriptException.initCause(e);
                throw scriptException;
            }
        } else if (scriptXML instanceof Document) {
            try {
                Element element = ((Document) scriptXML).getDocumentElement();
                omElement = XMLUtils.toOM(element);
            } catch (Exception e) {
                ScriptException scriptException = new ScriptException("Failed to create OMElement with provided " +
                        "payload");
                scriptException.initCause(e);
                throw scriptException;
            }
        } else if (scriptXML instanceof OMElement) {
            omElement = (OMElement) scriptXML;
        } else {
            throw new ScriptException("Unsupported type provide for XML. type: " + scriptXML.getClass());
        }
        return omElement;
    }

    /**
     * This method will convert the message payload in to xml string.
     *
     * @param omElement axiom element representation of xml document
     * @return xml string by adding the xml content
     * @throws ScriptException when error
     */
    public Object toScriptXML(OMElement omElement) throws ScriptException {
        String xmlString;
        if (omElement == null) {
            return null;
        }
        try {
            xmlString = omElement.toStringWithConsume();
        } catch (XMLStreamException e) {
            ScriptException scriptException = new ScriptException("Failed to convert OMElement to a string");
            scriptException.initCause(e);
            throw scriptException;
        }
        return xmlString;
    }
}

