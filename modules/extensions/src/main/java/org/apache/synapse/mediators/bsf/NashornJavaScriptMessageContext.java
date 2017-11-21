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

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.OperationContext;
import org.apache.bsf.xml.XMLHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * NashornJavaScriptMessageContext impliments the ScriptMessageContext specific to Nashorn java script engine.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class NashornJavaScriptMessageContext implements ScriptMessageContext {
    private static final Log logger = LogFactory.getLog(NashornJavaScriptMessageContext.class.getName());

    private static final String JSON_OBJECT = "JSON_OBJECT";
    private static final String JSON_TEXT = "JSON_TEXT";

    /** The actual Synapse message context reference. */
    private final MessageContext mc;

    /** The OMElement to scripting language object converter for the selected language. */
    private final XMLHelper xmlHelper;

    /** To keep Script Engine instance. */
    private ScriptEngine scriptEngine;

    /**
     * Reference to an empty JSON object.
     */
    private Object emptyJsonObject;

    /**
     * Reference to JSON object which is used to serialize json.
     */
    private ScriptObjectMirror jsonSerializer;

    public NashornJavaScriptMessageContext(MessageContext mc, XMLHelper xmlHelper, ScriptObjectMirror
            emptyJsonObject, ScriptObjectMirror jsonSerializer) {
        this.mc = mc;
        this.xmlHelper = xmlHelper;
        this.emptyJsonObject = emptyJsonObject;
        this.jsonSerializer = jsonSerializer;
    }

    /**
     * Get the XML representation of SOAP Body payload.
     * The payload is the first element inside the SOAP <Body> tags
     *
     * @return the XML SOAP Body
     */
    public Object getPayloadXML() {
        return mc.getEnvelope().getBody().getFirstElement();
    }

    /**
     * Set the SOAP body payload from XML.
     *
     * @param payload Message payload
     * @throws ScriptException For errors in converting xml To OM
     * @throws OMException     For errors in OM manipulation
     */
    public void setPayloadXML(Object payload) throws OMException, ScriptException {
        SOAPBody body = mc.getEnvelope().getBody();
        OMElement firstChild = body.getFirstElement();
        OMElement omElement = xmlHelper.toOMElement(payload);
        if (firstChild == null) {
            body.addChild(omElement);
        } else {
            firstChild.insertSiblingAfter(omElement);
            firstChild.detach();
        }
    }

    /**
     * Get the JSON object representation of the JSON message body of the request.
     *
     * @return JSON object of the message body
     */
    public Object getPayloadJSON() {
        return jsonObject(mc);
    }

    public Object getJsonText() {
        if (mc == null) {
            return "";
        }
        Object text = mc.getProperty(JSON_TEXT);
        return text == null ? "{}" : text;
    }

    /**
     * Get the Message Payload as a text.
     *
     * @return Payload as text
     */
    public String getPayloadText() {
        if (JsonUtil.hasAJsonPayload(((Axis2MessageContext) mc).getAxis2MessageContext())) {
            return JsonUtil.jsonPayloadToString(((Axis2MessageContext) mc).getAxis2MessageContext());
        } else {
            return mc.getEnvelope().toString();
        }
    }

    /**
     * Saves the JavaScript Object to the message context.
     * @param messageContext The message context of the sequence
     * @param jsonObject JavaScript Object which is passed to be saved in message context
     * @return true
     */
    public boolean setJsonObject(MessageContext messageContext, Object jsonObject) {
        messageContext.setProperty(JSON_OBJECT, jsonObject);
        return true;
    }

    /**
     * Saves the JSON String to the message context.
     * @param messageContext The message context of the sequence
     * @param jsonObject JavaScript string which is passed to be saved in message context
     * @return false if messageContext is null return true otherwise
     */
    public boolean setJsonText(MessageContext messageContext, Object jsonObject) {
        if (messageContext == null) {
            return false;
        }
        messageContext.setProperty(JSON_TEXT, jsonObject);
        return true;
    }

    /**
     * Returns the JavaScript Object saved in this message context.
     * @param messageContext The message context of the sequence
     * @return o JavaScript Object saved in this message context
     */
    public Object jsonObject(MessageContext messageContext) {
        if (messageContext == null) {
            return null;
        }
        Object jsonObject = messageContext.getProperty(JSON_OBJECT);
        if (jsonObject == null) {
            return emptyJsonObject;
        }
        return jsonObject;
    }

    /**
     * Set a script engine.
     *
     * @param scriptEngine a ScriptEngine instance
     */
    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    /**
     * Returns the parsed xml document.
     * @param text xml string or document needed to be parser
     * @return parsed document
     */
    public Document parseXml(String text) throws ScriptException {
        InputSource sax = new InputSource(new java.io.StringReader(text));
        DOMParser parser = new DOMParser();
        Document doc;
        try {
            parser.parse(sax);
            doc = parser.getDocument();
            doc.getDocumentElement().normalize();
        } catch (SAXException | IOException e) {
            ScriptException scriptException = new ScriptException("Failed to parse provided xml");
            scriptException.initCause(e);
            throw scriptException;
        }

        return doc;
    }

    /**
     * Returns the parsed xml document.
     * @param stream input stream of xml string or document needed to be parsed
     * @return parsed document
     */
    public OMElement getParsedOMElement(InputStream stream) {
        OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(stream);
        return builder.getDocumentElement();
    }

    /**
     * Returns the Axiom xpath.
     * @param expression Xpath expression
     * @return Axiom xpath is returned
     */
    public AXIOMXPath getXpathResult(String expression) throws JaxenException {
        return new AXIOMXPath(expression);
    }

    /**
     * Add a new SOAP header to the message.
     *
     * @param mustUnderstand the value for the <code>soapenv:mustUnderstand</code> attribute
     * @param content the XML for the new header
     * @throws ScriptException if an error occurs when converting the XML to OM
     */
    public void addHeader(boolean mustUnderstand, Object content) throws ScriptException {
        SOAPEnvelope envelope = mc.getEnvelope();
        SOAPFactory factory = (SOAPFactory) envelope.getOMFactory();
        SOAPHeader header = envelope.getHeader();
        if (header == null) {
            header = factory.createSOAPHeader(envelope);
        }

        OMElement element = xmlHelper.toOMElement(content);
        // We can't add the element directly to the SOAPHeader. Instead, we need to copy the
        // information over to a SOAPHeaderBlock.
        SOAPHeaderBlock headerBlock = header.addHeaderBlock(element.getLocalName(), element.getNamespace());
        for (Iterator it = element.getAllAttributes(); it.hasNext(); ) {
            headerBlock.addAttribute((OMAttribute) it.next());
        }
        headerBlock.setMustUnderstand(mustUnderstand);
        OMNode child = element.getFirstOMChild();
        while (child != null) {
            // Get the next child before addChild will detach the node from its original place.
            OMNode next = child.getNextOMSibling();
            headerBlock.addChild(child);
            child = next;
        }
    }

    /**
     * Get the XML representation of the complete SOAP envelope.
     * @return return an object that represents the payload in the current scripting language
     * @throws ScriptException in-case of an error in getting
     * the XML representation of SOAP envelope
     */
    public Object getEnvelopeXML() throws ScriptException {
        SOAPEnvelope envelope = mc.getEnvelope();
        return envelope.toString();
    }

    /**
     *
     * Helpers to set EPRs from a script string.
     *
     */
    public void setTo(String reference) {
        mc.setTo(new EndpointReference(reference));
    }

    public void setFaultTo(String reference) {
        mc.setFaultTo(new EndpointReference(reference));
    }

    public void setFrom(String reference) {
        mc.setFrom(new EndpointReference(reference));
    }

    public void setReplyTo(String reference) {
        mc.setReplyTo(new EndpointReference(reference));
    }

    /**
     * All the remainder just use the underlying MessageContext.
     */
    public SynapseConfiguration getConfiguration() {
        return mc.getConfiguration();
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        mc.setConfiguration(cfg);
    }

    public SynapseEnvironment getEnvironment() {
        return mc.getEnvironment();
    }

    public void setEnvironment(SynapseEnvironment se) {
        mc.setEnvironment(se);
    }

    public Map<String, Object> getContextEntries() {
        return mc.getContextEntries();
    }

    public void setContextEntries(Map<String, Object> entries) {
        mc.setContextEntries(entries);
    }

    public Object getProperty(String key) {
        return mc.getProperty(key);
    }

    public Object getEntry(String key) {
        return mc.getEntry(key);
    }

    public Object getLocalEntry(String key) {
        return mc.getLocalEntry(key);
    }

    public void setProperty(String key, Object value) {
        try {
            OMElement omElement = xmlHelper.toOMElement(value);
            mc.setProperty(key, omElement);
        } catch (ScriptException e) {
            //Try to convert the value into OMElement if it fails it means value is not a representation of xml so
            // set as key value pair
            mc.setProperty(key, value);
        }
    }

    public void setProperty(String key, Object value, String scope) {
        if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
            setProperty(key, value);
        } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)) {
            //Setting property into the  Axis2 Message Context
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            axis2MessageCtx.setProperty(key, value);
            handleSpecialProperties(key, value, axis2MessageCtx);

        } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)) {
            //Setting Transport Headers
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

            if (headers != null && headers instanceof Map) {
                Map headersMap = (Map) headers;
                headersMap.put(key, value);
            }
            if (headers == null) {
                Map headersMap = new HashMap();
                headersMap.put(key, value);
                axis2MessageCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
            }
        } else if (XMLConfigConstants.SCOPE_OPERATION.equals(scope)) {
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            axis2MessageCtx.getOperationContext().setProperty(key, value);
        }
    }

    public void removeProperty(String key, String scope) {
        if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
            Set pros = mc.getPropertyKeySet();
            if (pros != null) {
                pros.remove(key);
            }
        } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)) {
            //Removing property from the Axis2 Message Context
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            axis2MessageCtx.removeProperty(key);

        } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)) {
            // Removing transport headers
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers != null && headers instanceof Map) {
                Map headersMap = (Map) headers;
                headersMap.remove(key);
            }
        } else if (XMLConfigConstants.SCOPE_OPERATION.equals(scope)) {
            // Removing operation scope headers
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            OperationContext axis2oc = axis2MessageCtx.getOperationContext();
            axis2oc.removeProperty(key);
        }

    }

    private void handleSpecialProperties(String key, Object value,
                                         org.apache.axis2.context.MessageContext messageContext) {
        if (org.apache.axis2.Constants.Configuration.MESSAGE_TYPE.equals(key)) {
            messageContext.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, value);
            Object o = messageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            Map headers = (Map) o;
            if (headers != null) {
                headers.put(HTTP.CONTENT_TYPE, value);
            }
        }
    }

    public Set getPropertyKeySet() {
        return mc.getPropertyKeySet();
    }

    public Mediator getMainSequence() {
        return mc.getMainSequence();
    }

    public Mediator getFaultSequence() {
        return mc.getFaultSequence();
    }

    public Mediator getSequence(String key) {
        return mc.getSequence(key);
    }

    public OMElement getFormat(String s) {
       return mc.getFormat(s);
    }

    public Endpoint getEndpoint(String key) {
        return mc.getEndpoint(key);
    }

    public SOAPEnvelope getEnvelope() {
        return mc.getEnvelope();
    }

    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        mc.setEnvelope(envelope);
    }

    public EndpointReference getFaultTo() {
        return mc.getFaultTo();
    }

    public void setFaultTo(EndpointReference reference) {
        mc.setFaultTo(reference);
    }

    public EndpointReference getFrom() {
        return mc.getFrom();
    }

    public void setFrom(EndpointReference reference) {
        mc.setFrom(reference);
    }

    public String getMessageID() {
        return mc.getMessageID();
    }

    public void setMessageID(String string) {
        mc.setMessageID(string);
    }

    public RelatesTo getRelatesTo() {
        return mc.getRelatesTo();
    }

    public void setRelatesTo(RelatesTo[] reference) {
        mc.setRelatesTo(reference);
    }

    public EndpointReference getReplyTo() {
        return mc.getReplyTo();
    }

    public void setReplyTo(EndpointReference reference) {
        mc.setReplyTo(reference);
    }

    public EndpointReference getTo() {
        return mc.getTo();
    }

    public void setTo(EndpointReference reference) {
        mc.setTo(reference);
    }

    public void setWSAAction(String actionURI) {
        mc.setWSAAction(actionURI);
    }

    public String getWSAAction() {
        return mc.getWSAAction();
    }

    public String getSoapAction() {
        return mc.getSoapAction();
    }

    public void setSoapAction(String string) {
        mc.setSoapAction(string);
    }

    public void setWSAMessageID(String messageID) {
        mc.setWSAMessageID(messageID);
    }

    public String getWSAMessageID() {
        return mc.getWSAMessageID();
    }

    public boolean isDoingMTOM() {
        return mc.isDoingMTOM();
    }

    public boolean isDoingSWA() {
        return mc.isDoingSWA();
    }

    public void setDoingMTOM(boolean b) {
        mc.setDoingMTOM(b);
    }

    public void setDoingSWA(boolean b) {
        mc.setDoingSWA(b);
    }

    public boolean isDoingPOX() {
        return mc.isDoingPOX();
    }

    public void setDoingPOX(boolean b) {
        mc.setDoingPOX(b);
    }

    public boolean isDoingGET() {
        return mc.isDoingGET();
    }

    public void setDoingGET(boolean b) {
        mc.setDoingGET(b);
    }

    public boolean isSOAP11() {
        return mc.isSOAP11();
    }

    public void setResponse(boolean b) {
        mc.setResponse(b);
    }

    public boolean isResponse() {
        return mc.isResponse();
    }

    public void setFaultResponse(boolean b) {
        mc.setFaultResponse(b);
    }

    public boolean isFaultResponse() {
        return mc.isFaultResponse();
    }

    public int getTracingState() {
        return mc.getTracingState();
    }

    public void setTracingState(int tracingState) {
        mc.setTracingState(tracingState);
    }

    public Stack<FaultHandler> getFaultStack() {
        return mc.getFaultStack();
    }

    public void pushFaultHandler(FaultHandler fault) {
        mc.pushFaultHandler(fault);
    }

    public void pushContinuationState(ContinuationState continuationState) {
    }

    public Stack<ContinuationState> getContinuationStateStack() {
        return null;
    }

    public boolean isContinuationEnabled() {
        return false;
    }

    public void setContinuationEnabled(boolean contStateStackEnabled) {
    }

    public Log getServiceLog() {
        return LogFactory.getLog(NashornJavaScriptMessageContext.class);
    }

    public Mediator getSequenceTemplate(String key) {
        return mc.getSequenceTemplate(key);
    }

     /**
     * Saves the payload of this message context as a JSON payload.
     *
     * @param jsonPayload Javascript native object to be set as the message body
     * @throws ScriptException in case of creating a JSON object out of the javascript native object.
     */
    public void setPayloadJSON(Object jsonPayload) throws ScriptException {
        try {
            String jsonString = (String) jsonSerializer.callMember("stringify", jsonPayload);
            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
            org.apache.axis2.context.MessageContext messageContext;
            messageContext = ((Axis2MessageContext) mc).getAxis2MessageContext();
            JsonUtil.getNewJsonPayload(messageContext, stream, true, true);
            messageContext.setProperty(JSON_OBJECT, jsonPayload);
        } catch (AxisFault axisFault) {
            throw new ScriptException(axisFault);
        }
    }

    public Mediator getDefaultConfiguration(String arg0) {
        return mc.getDefaultConfiguration(arg0);
    }

    public String getMessageString() {
        return mc.getMessageString();
    }

    public void setMessageFlowTracingState(int state) {
        mc.setMessageFlowTracingState(state);
    }

    public int getMessageFlowTracingState() {
        return SynapseConstants.TRACING_OFF;
    }
}

