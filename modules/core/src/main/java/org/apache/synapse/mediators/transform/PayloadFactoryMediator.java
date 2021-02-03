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

package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.transform.pfutils.TemplateProcessor;
import org.apache.synapse.mediators.transform.pfutils.TemplateProcessorException;
import org.apache.synapse.util.AXIOMUtils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import static org.apache.synapse.mediators.transform.pfutils.Constants.JSON_TYPE;
import static org.apache.synapse.mediators.transform.pfutils.Constants.REGEX_TEMPLATE_TYPE;
import static org.apache.synapse.mediators.transform.pfutils.Constants.TEXT_TYPE;
import static org.apache.synapse.mediators.transform.pfutils.Constants.XML_TYPE;

public class PayloadFactoryMediator extends AbstractMediator {
    private Value formatKey = null;
    private boolean isFormatDynamic = false;
    private String formatRaw;
    private String mediaType = XML_TYPE;
    private static final String XML_CONTENT_TYPE = "application/xml";
    private boolean escapeXmlChars = false;
    private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String TEXT_CONTENT_TYPE = "text/plain";
    private static final String SOAP11_CONTENT_TYPE = "text/xml";
    private static final String SOAP12_CONTENT_TYPE = "application/soap+xml";
    private String templateType = REGEX_TEMPLATE_TYPE;
    private static final QName TEXT_ELEMENT = new QName("http://ws.apache.org/commons/ns/payload", "text");
    public static final String QUOTE_STRING_IN_PAYLOAD_FACTORY_JSON = "QUOTE_STRING_IN_PAYLOAD_FACTORY_JSON";
    private TemplateProcessor templateProcessor;
    private static final Log log = LogFactory.getLog(PayloadFactoryMediator.class);

    /**
     * Contains 2 paths - one when JSON Streaming is in use (mediateJsonStreamPayload) and the other for regular
     * builders (mediatePayload).
     * @param synCtx the current message for mediation
     * @return
     */
    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled() && super.divertMediationRoute(synCtx)) {
            return true;
        }

        String format = formatRaw;
        return mediate(synCtx, format);
    }

    private boolean mediate(MessageContext synCtx, String format) {

        if (!isDoingXml(synCtx) && !isDoingJson(synCtx)) {
            log.error("#mediate. Could not identify the payload format of the existing payload prior to mediate.");
            return false;
        }
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        StringBuilder result = new StringBuilder();
        transform(result, synCtx, format);
        String out = result.toString().trim();
        if (log.isDebugEnabled()) {
            log.debug("#mediate. Transformed payload format>>> " + out);
        }
        if (mediaType.equals(XML_TYPE)) {
            try {
                JsonUtil.removeJsonPayload(axis2MessageContext);
                OMElement omXML = AXIOMUtil.stringToOM(out);
                if (!checkAndReplaceEnvelope(omXML, synCtx)) { // check if the target of the PF 'format' is the entire SOAP envelop, not just the body.
                    axis2MessageContext.getEnvelope().getBody().addChild(omXML.getFirstElement());
                }
            } catch (XMLStreamException e) {
                handleException("Error creating SOAP Envelope from source " + out, synCtx);
            }
        } else if  (mediaType.equals(JSON_TYPE)) {
            try {
                JsonUtil.getNewJsonPayload(axis2MessageContext, out, true, true);
            } catch (AxisFault axisFault) {
                handleException("Error creating JSON Payload from source " + out, synCtx);
            }
        } else if  (mediaType.equals(TEXT_TYPE)) {
            JsonUtil.removeJsonPayload(axis2MessageContext);
            axis2MessageContext.getEnvelope().getBody().addChild(getTextElement(out));
        }
        //need to honour a content-type of the payload media-type as output from the payload 
        //{re-merging patch https://wso2.org/jira/browse/ESBJAVA-3014}
        setContentType(synCtx);
        return true;
    }

    /**
     * Calls the replace function. isFormatDynamic check is used to remove indentations which come from registry based
     * configurations.
     *
     * @param result
     * @param synCtx
     * @param format
     */
    private void transform(StringBuilder result, MessageContext synCtx, String format) {
        if (isFormatDynamic()) {
            String key = formatKey.evaluateValue(synCtx);
            Object entry = synCtx.getEntry(key);
            if (entry == null) {
                handleException("Key " + key + " not found ", synCtx);
            }
            String text = "";
            if (entry instanceof OMElement) {
                OMElement omElement = ((OMElement) entry).cloneOMElement();
                removeIndentations(omElement);
                text = omElement.toString();
            } else if (entry instanceof OMText) {
                text = ((OMText) entry).getText();
            } else if (entry instanceof String) {
                text = (String) entry;
            }
            processTemplate(result, synCtx, text);
        } else {
            processTemplate(result, synCtx, format);
        }
    }

    private void processTemplate(StringBuilder result, MessageContext synCtx, String text) {

        try {
            result.append(templateProcessor.processTemplate(text, mediaType, synCtx));
        } catch (TemplateProcessorException e) {
            handleException(e.getMessage(), synCtx);
        }
    }

    /**
     * Sets the content type based on the request content type and payload factory media type. This should be called
     * at the end before returning from the mediate() function.
     *
     * @param synCtx
     */
    private void setContentType(MessageContext synCtx) {

        org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        if (mediaType.equals(XML_TYPE)) {
            if (!XML_CONTENT_TYPE.equals(a2mc.getProperty(Constants.Configuration.MESSAGE_TYPE)) &&
                    !SOAP11_CONTENT_TYPE.equals(a2mc.getProperty(Constants.Configuration.MESSAGE_TYPE)) &&
                    !SOAP12_CONTENT_TYPE.equals(a2mc.getProperty(Constants.Configuration.MESSAGE_TYPE))) {
                a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, XML_CONTENT_TYPE);
                a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, XML_CONTENT_TYPE);
                handleSpecialProperties(XML_CONTENT_TYPE, a2mc);
            }
        } else if (mediaType.equals(JSON_TYPE)) {
            a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, JSON_CONTENT_TYPE);
            a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, JSON_CONTENT_TYPE);
            handleSpecialProperties(JSON_CONTENT_TYPE, a2mc);
        } else if (mediaType.equals(TEXT_TYPE)) {
            a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, TEXT_CONTENT_TYPE);
            a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, TEXT_CONTENT_TYPE);
            handleSpecialProperties(TEXT_CONTENT_TYPE, a2mc);
        }
        a2mc.removeProperty("NO_ENTITY_BODY");
    }

    // This is copied from PropertyMediator, required to change Content-Type
    private void handleSpecialProperties(Object resultValue,
                                         org.apache.axis2.context.MessageContext axis2MessageCtx) {

        axis2MessageCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, resultValue);
        Object o = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        Map headers = (Map) o;
        if (headers != null) {
            headers.remove(HTTP.CONTENT_TYPE);
            headers.put(HTTP.CONTENT_TYPE, resultValue);
        }
    }

    private boolean checkAndReplaceEnvelope(OMElement resultElement, MessageContext synCtx) {
        OMElement firstChild = resultElement.getFirstElement();
        QName resultQName = firstChild.getQName();
        if (resultQName.getLocalPart().equals("Envelope") && (
                resultQName.getNamespaceURI().equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI) ||
                        resultQName.getNamespaceURI().
                                equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI))) {
            SOAPEnvelope soapEnvelope = AXIOMUtils.getSOAPEnvFromOM(resultElement.getFirstElement());
            if (soapEnvelope != null) {
                try {
                    soapEnvelope.buildWithAttachments();
                    synCtx.setEnvelope(soapEnvelope);
                } catch (AxisFault axisFault) {
                    handleException("Unable to attach SOAPEnvelope", axisFault, synCtx);
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Helper function to remove indentations.
     * @param element
     */
    private void removeIndentations(OMElement element) {
        List<OMText> removables = new ArrayList<>();
        removeIndentations(element, removables);
        for (OMText node : removables) {
            node.detach();
        }
    }

    /**
     * Helper function to remove indentations.
     * @param element
     * @param removables
     */
    private void removeIndentations(OMElement element, List<OMText> removables) {
        Iterator children = element.getChildren();
        while (children.hasNext()) {
            Object next = children.next();
            if (next instanceof OMText) {
                OMText text = (OMText) next;
                if (text.getText().trim().equals("")) {
                    removables.add(text);
                }
            } else if (next instanceof OMElement) {
                removeIndentations((OMElement) next, removables);
            }
        }
    }

    public String getFormat() {
        return formatRaw;
    }

    public void setFormat(String format) {
        this.formatRaw = format;
    }

    @Override
    public String getType() {
        return mediaType;
    }

    public void setType(String type) {
        this.mediaType = type;
    }

    public String getTemplateType() {

        return templateType;
    }

    public void setTemplateType(String templateType) {

        this.templateType = templateType;
    }

    /**
     * To get the key which is used to pick the format definition from the local registry
     *
     * @return return the key which is used to pick the format definition from the local registry
     */
    public Value getFormatKey() {
        return formatKey;
    }

    /**
     * To set the local registry key in order to pick the format definition
     *
     * @param key the local registry key
     */
    public void setFormatKey(Value key) {
        this.formatKey = key;
    }

    public boolean isFormatDynamic() {

        return isFormatDynamic;
    }

    public void setFormatDynamic(boolean formatDynamic) {
        this.isFormatDynamic = formatDynamic;
    }

    @Override
    public String getOutputType() {
        return mediaType;
    }


    private OMElement getTextElement(String content) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement textElement = factory.createOMElement(TEXT_ELEMENT);
        if (content == null) {
            content = "";
        }
        textElement.setText(content);
        return textElement;
    }

    @Override
    public boolean isContentAltering() {
        return true;
    }

    public boolean isEscapeXmlChars() {
        return escapeXmlChars;
    }

    public void setEscapeXmlChars(boolean escapeXmlChars) {
        this.escapeXmlChars = escapeXmlChars;
    }

    /**
     * Converts String to OMElement
     *
     * @param value String value to convert
     * @return parsed OMElement
     */
    private OMElement convertStringToOM(String value) throws XMLStreamException, OMException {
        javax.xml.stream.XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(new StringReader(value));
        StAXBuilder builder = new StAXOMBuilder(xmlReader);
        return builder.getDocumentElement();
    }

    public TemplateProcessor getTemplateProcessor() {

        return templateProcessor;
    }

    public void setTemplateProcessor(TemplateProcessor templateProcessor) {

        this.templateProcessor = templateProcessor;
    }

    private boolean isDoingJson(MessageContext messageContext) {

        return JsonUtil.hasAJsonPayload(((Axis2MessageContext) messageContext).getAxis2MessageContext());
    }

    private boolean isDoingXml(MessageContext messageContext) {

        return !isDoingJson(messageContext);
    }
}
