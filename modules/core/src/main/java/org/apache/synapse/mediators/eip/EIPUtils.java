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

package org.apache.synapse.mediators.eip;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for the EIP mediators
 */
public class EIPUtils {

    private static final Log log = LogFactory.getLog(EIPUtils.class);

    /**
     * Return the set of elements specified by the XPath over the given envelope
     *
     * @param envelope SOAPEnvelope from which the elements will be extracted
     * @param expression SynapseXPath expression describing the elements to be extracted
     * @return List OMElements in the envelope matching the expression
     * @throws JaxenException if the XPath expression evaluation fails
     */
    public static List getMatchingElements(SOAPEnvelope envelope, SynapseXPath expression)
        throws JaxenException {
        return getMatchingElements(envelope,null,expression);
    }

    /**
     * Return the set of elements specified by the XPath over the given envelope
     *
     * @param envelope SOAPEnvelope from which the elements will be extracted
     * @param expression SynapseXPath expression describing the elements to be extracted
     * @return List OMElements in the envelope matching the expression
     * @throws JaxenException if the XPath expression evaluation fails
     */
    public static List getMatchingElements(SOAPEnvelope envelope,MessageContext synCtxt, SynapseXPath expression)
        throws JaxenException {

        Object o = expression.evaluate(envelope, synCtxt);
        if (o instanceof OMNode) {
            List list = new ArrayList();
            list.add(o);
            return list;
        } else if (o instanceof List) {
            return (List) o;
        } else {
            return new ArrayList();
        }
    }

    /**
     * Return the set of detached elements specified by the XPath over the given envelope
     *
     * @param envelope SOAPEnvelope from which the elements will be extracted
     * @param expression SynapseXPath expression describing the elements to be extracted
     * @return List detached OMElements in the envelope matching the expression
     * @throws JaxenException if the XPath expression evaluation fails
     */
    public static List<OMNode> getDetachedMatchingElements(SOAPEnvelope envelope, MessageContext synCtxt,
                                                           SynapseXPath expression)
        throws JaxenException {

        List<OMNode> elementList = new ArrayList<OMNode>();
        Object o = expression.evaluate(envelope, synCtxt);
        if (o instanceof OMNode) {
            elementList.add(((OMNode) o).detach());
        } else if (o instanceof List) {
            for (Object elem : (List) o) {
                if (elem instanceof OMNode) {
                    elementList.add(((OMNode) elem).detach());
                }
            }
        }
        return elementList;
    }

    /**
     * Merge two SOAP envelopes using the given XPath expression that specifies the
     * element that enriches the first envelope from the second
     *
     * @param envelope   SOAPEnvelope to be enriched with the content
     * @param enricher   SOAPEnvelope from which the enriching element will be extracted
     * @param expression SynapseXPath describing the enriching element
     * @throws JaxenException on failing of processing the xpath
     */
    public static void enrichEnvelope(SOAPEnvelope envelope, SOAPEnvelope enricher,
                                      MessageContext synCtxt,
                                      SynapseXPath expression) throws JaxenException {

        OMElement enrichingElement;
        enricher.toString();
        List elementList = getMatchingElements(envelope, synCtxt, expression);
        List list = getMatchingElements(enricher, synCtxt, expression);
        if ((checkNotEmpty(elementList) && checkNotEmpty(list))
            || (!checkNotEmpty(elementList) && checkNotEmpty(list))) {
            if (checkNotEmpty(elementList)) {
                // attach at parent of the first result from the XPath, or to the SOAPBody
                Object o = elementList.get(0);

                if (o instanceof OMElement &&
                    ((OMElement) o).getParent() != null &&
                    ((OMElement) o).getParent() instanceof OMElement) {
                    enrichingElement = (OMElement) ((OMElement) o).getParent();
                    OMElement body = envelope.getBody();
                    if (!isBody(body, enrichingElement)) {
                        OMElement nonBodyElem = enrichingElement;
                        enrichingElement = envelope.getBody();
                        addChildren(elementList, enrichingElement);
                        while (!isBody(body, (OMElement) nonBodyElem.getParent())) {
                            nonBodyElem = (OMElement) nonBodyElem.getParent();
                        }
                        nonBodyElem.detach();
                    }
                }
            }
            enrichingElement = envelope.getBody();
            //This has to introduce because if on complete expression written to extract SOAPEnvelop ex: "."
            // then StAXOMBuilder end up in while loop that never exit. This cause to OOM the ESB. Hence the fix is to
            // validate child element is a SOAPEnvelop
            for (Object child : list) {
                if (child instanceof SOAPEnvelope) {
                    throw new SynapseException("Could not add SOAPEnvelope as child element.");
                }
            }
            addChildren(list, enrichingElement);
        } else {
            throw new SynapseException("Could not find matching elements to aggregate.");
        }
    }

    private static boolean isBody(OMElement body, OMElement enrichingElement) {
        try {
            return (body.getLocalName().equals(enrichingElement.getLocalName()) &&
                    body.getNamespace().getNamespaceURI().equals(enrichingElement.getNamespace().getNamespaceURI()));
        } catch (NullPointerException e) {
            return false;
        }
    }

    private static boolean checkNotEmpty(List list) {
        return list != null && !list.isEmpty();
    }

    private static void addChildren(List list, OMElement element) {
        Iterator itr = list.iterator();
        Object o;
        while (itr.hasNext()) {
            o = itr.next();
            if (o != null && o instanceof OMElement) {
                element.addChild((OMElement) o);
            }
            itr.remove();
        }
    }

    /**
     * Util functions related to EIP Templates
     */
    public static String getTemplatePropertyMapping(String templateName, String parameter) {
        return templateName + ":" + parameter;
    }

    public static void createSynapseEIPTemplateProperty(MessageContext synCtxt, String templateName,
                                                        String  paramName, Object value) {
        String targetSynapsePropName = getTemplatePropertyMapping(templateName,paramName);
        synCtxt.setProperty(targetSynapsePropName,value);
    }

    /**
     * Enclose children of the soap body with a specific element
     *
     * @param envelope SOAPEnvelope which is to be enclosed
     * @param encloseElement enclosing element
     * @return modified SOAPEnvelope
     */
    public static SOAPEnvelope encloseWithElement (SOAPEnvelope envelope, OMElement encloseElement) {
        Iterator itr = envelope.getBody().getChildElements();
        Object o;
        while (itr.hasNext()) {
            o = itr.next();
            if (o != null && o instanceof OMElement) {
                encloseElement.addChild((OMElement) o);
            }
        }
        envelope.getBody().addChild(encloseElement);
        return envelope;
    }

    /**
     * Evaluate JSON path and retrieve the result as JsonElement. If multiple matches found, combine matching results
     * in a comma separated list and parse as a JSON array.
     *
     * @param messageContext messageContext which contains the JSON payload.
     * @return JsonArray or a JsonPrimitive depending on the JsonPath response.
     */
    public static JsonElement getJSONElement(MessageContext messageContext, SynapseJsonPath jsonPath) {
        JsonParser parser = new JsonParser();

        Object objectList = jsonPath.evaluate(messageContext);
        if (objectList instanceof List) {
            List list = (List) objectList;
            if (!list.isEmpty()) {
                if (list.size() > 1) {
                    String result = "[" + StringUtils.join(list, ',') + "]";
                    JsonElement element = parser.parse(result);
                    return element.getAsJsonArray();
                }
                JsonElement result;
                String resultString = list.get(0).toString().trim();
                result = tryParseJsonString(parser, resultString);
                return result;
            }
        }
        return null;
    }

    /**
     * Given a json string and a parser this method will return the parsed string.
     *
     * @param parser JSON parser instance.
     * @param inputJson input JSON string.
     * @return parsed JsonElement.
     */
    public static JsonElement tryParseJsonString(JsonParser parser, String inputJson) {
        JsonElement result;
        try {
            result = parser.parse(inputJson);
        } catch (JsonSyntaxException e) {
            // Enclosing using quotes due to the following issue
            // https://github.com/google/gson/issues/1286
            inputJson = "\"" + inputJson + "\"";
            result = parser.parse(inputJson);
        }
        return result;
    }
}
