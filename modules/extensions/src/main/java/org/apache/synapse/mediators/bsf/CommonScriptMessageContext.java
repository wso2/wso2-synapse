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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNode;
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
import org.apache.commons.io.output.ByteArrayOutputStream;
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
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.xml.XMLObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * CommonScriptMessageContext decorates the Synapse MessageContext adding methods to use the
 * message payload XML in a way natural to the scripting languages.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class CommonScriptMessageContext implements ScriptMessageContext {
    private static final Log logger = LogFactory.getLog(CommonScriptMessageContext.class.getName());

    private static final String JSON_OBJECT = "JSON_OBJECT";
    private static final String JSON_TEXT = "JSON_TEXT";

    /** The actual Synapse message context reference. */
    private final MessageContext mc;

    /** The OMElement to scripting language object converter for the selected language. */
    private final XMLHelper xmlHelper;

    /** To keep Script Engine instance. */
    private ScriptEngine scriptEngine;

    public CommonScriptMessageContext(MessageContext mc, XMLHelper xmlHelper) {
        this.mc = mc;
        this.xmlHelper = xmlHelper;
    }

    /**
     * Get the XML representation of SOAP Body payload.
     * The payload is the first element inside the SOAP <Body> tags
     *
     * @return the XML SOAP Body
     * @throws ScriptException in-case of an error in getting
     * the XML representation of SOAP Body payload
     */
    @Override
    public Object getPayloadXML() throws ScriptException {
        return xmlHelper.toScriptXML(mc.getEnvelope().getBody().getFirstElement());
    }

    /**
     * Set the SOAP body payload from XML.
     *
     * @param payload Message payload
     * @throws ScriptException For errors in converting xml To OM
     * @throws OMException For errors in OM manipulation
     */
    @Override
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
    @Override
    public Object getPayloadJSON() {
        return jsonObject(mc);
    }

    /**
     * Get the Message Payload as a text.
     *
     * @return Payload as text
     */
    @Override
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
    @Override
    public String getPayloadText() {
        if (JsonUtil.hasAJsonPayload(((Axis2MessageContext) mc).getAxis2MessageContext())) {
            return JsonUtil.jsonPayloadToString(((Axis2MessageContext) mc).getAxis2MessageContext());
        } else {
            return mc.getEnvelope().toString();
        }
    }

    /**
     * Saves the payload of this message context as a JSON payload.
     *
     * @param jsonPayload Javascript native object to be set as the message body
     * @throws ScriptException in case of creating a JSON object out of
     *                         the javascript native object.
     */
    public void setPayloadJSON0(Object jsonPayload) throws ScriptException {
        org.apache.axis2.context.MessageContext messageContext;
        messageContext = ((Axis2MessageContext) mc).getAxis2MessageContext();

        String jsonString;
        if (jsonPayload instanceof String) {
            jsonString = (String) jsonPayload;
        } else {
            jsonString = serializeJSON(jsonPayload);
        }
        try {
            JsonUtil.getNewJsonPayload(messageContext, jsonString, true, true);
        } catch (AxisFault axisFault) {
            throw new ScriptException(axisFault);
        }
        Object jsonObject = scriptEngine.eval('(' + jsonString + ')');
        setJsonObject(mc, jsonObject);
    }

    /**
     * Saves the JavaScript Object to the message context.
     *
     * @param messageContext The message context of the sequence
     * @param jsonObject JavaScript Object saved in this message context
     * @return true
     */
    @Override
    public boolean setJsonObject(MessageContext messageContext, Object jsonObject) {
        messageContext.setProperty(JSON_OBJECT, jsonObject);
        return true;
    }

    /**
     * Saves the JSON String to the message context.
     *
     * @param messageContext The message context of the sequence
     * @param jsonObject JavaScript Object saved in this message context
     * @return false if messageContext is null return true otherwise
     */
    @Override
    public boolean setJsonText(MessageContext messageContext, Object jsonObject) {
        if (messageContext == null) {
            return false;
        }
        messageContext.setProperty(JSON_TEXT, jsonObject);
        return true;
    }

    /**
     * Returns the JavaScript Object saved in this message context.
     *
     * @param messageContext The message context of the sequence
     * @return o JavaScript Object saved in this message context
     */
    @Override
    public Object jsonObject(MessageContext messageContext) {
        if (messageContext == null) {
            return null;
        }
        Object o = messageContext.getProperty(JSON_OBJECT);
        if (o == null) {
            if (this.scriptEngine == null) {
                logger.error("Cannot create empty JSON object. ScriptEngine instance not available.");
                return null;
            }
            try {
                return this.scriptEngine.eval("({})");
            } catch (ScriptException e) {
                logger.error("Could not return an empty JSON object. ", e);
            }
        }
        return o;
    }

    /**
     * Set a script engine.
     *
     * @param scriptEngine a ScriptEngine instance
     */
    @Override
    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    /**
     * Add a new SOAP header to the message.
     * 
     * @param mustUnderstand the value for the <code>soapenv:mustUnderstand</code> attribute
     * @param content the XML for the new header
     * @throws ScriptException if an error occurs when converting the XML to OM
     */
    @Override
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
        SOAPHeaderBlock headerBlock = header.addHeaderBlock(element.getLocalName(),
                element.getNamespace());
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
     *
     * @return return an object that represents the payload in the current scripting language
     * @throws ScriptException in-case of an error in getting
     * the XML representation of SOAP envelope
     */
    @Override
    public Object getEnvelopeXML() throws ScriptException {
        return xmlHelper.toScriptXML(mc.getEnvelope());
    }

    /**
     * This is used to set the value which specifies the receiver of the message.
     *
     * @param reference specifies the receiver of the message
     */
    @Override
    public void setTo(String reference) {
        mc.setTo(new EndpointReference(reference));
    }

    /**
     * This is used to set the value which specifies the receiver of the faults relating to the message.
     *
     * @param reference specifies the receiver of the faults relating to the message
     */
    @Override
    public void setFaultTo(String reference) {
        mc.setFaultTo(new EndpointReference(reference));
    }

    /**
     * This is used to set the value which specifies the sender of the message.
     *
     * @param reference specifies the sender of the message
     */
    @Override
    public void setFrom(String reference) {
        mc.setFrom(new EndpointReference(reference));
    }

    /**
     * This is used to set the value which specifies the receiver of the replies to the message.
     *
     * @param reference specifies the receiver of the replies to the message
     */
    @Override
    public void setReplyTo(String reference) {
        mc.setReplyTo(new EndpointReference(reference));
    }


    /**
     * {@inheritDoc}
     */
    public SynapseConfiguration getConfiguration() {
        return mc.getConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    public void setConfiguration(SynapseConfiguration cfg) {
        mc.setConfiguration(cfg);
    }

    /**
     * {@inheritDoc}
     */
    public SynapseEnvironment getEnvironment() {
        return mc.getEnvironment();
    }

    /**
     * {@inheritDoc}
     */
    public void setEnvironment(SynapseEnvironment se) {
        mc.setEnvironment(se);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getContextEntries() {
        return mc.getContextEntries();
    }

    /**
     * {@inheritDoc}
     */
    public void setContextEntries(Map<String, Object> entries) {
        mc.setContextEntries(entries);
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String key) {
        return mc.getProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object getEntry(String key) {
        return mc.getEntry(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object getLocalEntry(String key) {
        return mc.getLocalEntry(key);
    }

    /**
     * Add a new property to the message.
     *
     * @param key unique identifier of property
     * @param value value of property
     */
    public void setProperty(String key, Object value) {
        if (value instanceof XMLObject) {
            OMElement omElement = null;
            try {
                omElement = xmlHelper.toOMElement(value);
            } catch (ScriptException e) {
                mc.setProperty(key, value);
            }
            if (omElement != null) {
                mc.setProperty(key, omElement);
            }
        } else {
            mc.setProperty(key, value);
        }
    }

    /**
     * Add a new property to the message.
     *
     * @param key unique identifier of property
     * @param value value of property
     * @param scope scope of the property
     */
    @Override
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
            axis2smc.getAxis2MessageContext().getOperationContext().setProperty(key, value);
        }
    }

    /**
     * Remove property from the message.
     *
     * @param key unique identifier of property
     * @param scope scope of the property
     */
    @Override
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

    /**
     * Add special properties such as content type to the message context.
     *
     * @param key unique identifier of property
     * @param value value of property
     * @param messageContext Axis2 message context
     */
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

    /**
     * {@inheritDoc}
     */
    public Set getPropertyKeySet() {
        return mc.getPropertyKeySet();
    }

    /**
     * {@inheritDoc}
     */
    public Mediator getMainSequence() {
        return mc.getMainSequence();
    }

    /**
     * {@inheritDoc}
     */
    public Mediator getFaultSequence() {
        return mc.getFaultSequence();
    }

    /**
     * {@inheritDoc}
     */
    public Mediator getSequence(String key) {
        return mc.getSequence(key);
    }

    /**
     * {@inheritDoc}
     */
    public OMElement getFormat(String s) {
        return mc.getFormat(s);
    }

    /**
     * {@inheritDoc}
     */
    public Endpoint getEndpoint(String key) {
        return mc.getEndpoint(key);
    }

    /**
     * {@inheritDoc}
     */
    public SOAPEnvelope getEnvelope() {
        return mc.getEnvelope();
    }

    /**
     * {@inheritDoc}
     */
    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        mc.setEnvelope(envelope);
    }

    /**
     * {@inheritDoc}
     */
    public EndpointReference getFaultTo() {
        return mc.getFaultTo();
    }

    /**
     * {@inheritDoc}
     */
    public void setFaultTo(EndpointReference reference) {
        mc.setFaultTo(reference);
    }

    /**
     * {@inheritDoc}
     */
    public EndpointReference getFrom() {
        return mc.getFrom();
    }

    /**
     * {@inheritDoc}
     */
    public void setFrom(EndpointReference reference) {
        mc.setFrom(reference);
    }

    /**
     * {@inheritDoc}
     */
    public String getMessageID() {
        return mc.getMessageID();
    }

    /**
     * {@inheritDoc}
     */
    public void setMessageID(String string) {
        mc.setMessageID(string);
    }

    /**
     * {@inheritDoc}
     */
    public RelatesTo getRelatesTo() {
        return mc.getRelatesTo();
    }

    /**
     * {@inheritDoc}
     */
    public void setRelatesTo(RelatesTo[] reference) {
        mc.setRelatesTo(reference);
    }

    /**
     * {@inheritDoc}
     */
    public EndpointReference getReplyTo() {
        return mc.getReplyTo();
    }

    /**
     * {@inheritDoc}
     */
    public void setReplyTo(EndpointReference reference) {
        mc.setReplyTo(reference);
    }

    /**
     * {@inheritDoc}
     */
    public EndpointReference getTo() {
        return mc.getTo();
    }

    /**
     * {@inheritDoc}
     */
    public void setTo(EndpointReference reference) {
        mc.setTo(reference);
    }

    /**
     * {@inheritDoc}
     */
    public void setWSAAction(String actionURI) {
        mc.setWSAAction(actionURI);
    }

    /**
     * {@inheritDoc}
     */
    public String getWSAAction() {
        return mc.getWSAAction();
    }

    /**
     * {@inheritDoc}
     */
    public String getSoapAction() {
        return mc.getSoapAction();
    }

    /**
     * {@inheritDoc}
     */
    public void setSoapAction(String string) {
        mc.setSoapAction(string);
    }

    /**
     * {@inheritDoc}
     */
    public void setWSAMessageID(String messageID) {
        mc.setWSAMessageID(messageID);
    }

    /**
     * {@inheritDoc}
     */
    public String getWSAMessageID() {
        return mc.getWSAMessageID();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDoingMTOM() {
        return mc.isDoingMTOM();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDoingSWA() {
        return mc.isDoingSWA();
    }

    /**
     * {@inheritDoc}
     */
    public void setDoingMTOM(boolean b) {
        mc.setDoingMTOM(b);
    }

    /**
     * {@inheritDoc}
     */
    public void setDoingSWA(boolean b) {
        mc.setDoingSWA(b);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDoingPOX() {
        return mc.isDoingPOX();
    }

    /**
     * {@inheritDoc}
     */
    public void setDoingPOX(boolean b) {
        mc.setDoingPOX(b);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDoingGET() {
        return mc.isDoingGET();
    }

    /**
     * {@inheritDoc}
     */
    public void setDoingGET(boolean b) {
        mc.setDoingGET(b);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSOAP11() {
        return mc.isSOAP11();
    }

    /**
     * {@inheritDoc}
     */
    public void setResponse(boolean b) {
        mc.setResponse(b);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isResponse() {
        return mc.isResponse();
    }

    /**
     * {@inheritDoc}
     */
    public void setFaultResponse(boolean b) {
        mc.setFaultResponse(b);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFaultResponse() {
        return mc.isFaultResponse();
    }

    /**
     * {@inheritDoc}
     */
    public int getTracingState() {
        return mc.getTracingState();
    }

    /**
     * {@inheritDoc}
     */
    public void setTracingState(int tracingState) {
        mc.setTracingState(tracingState);
    }

    /**
     * {@inheritDoc}
     */
    public Stack<FaultHandler> getFaultStack() {
        return mc.getFaultStack();
    }

    /**
     * {@inheritDoc}
     */
    public void pushFaultHandler(FaultHandler fault) {
        mc.pushFaultHandler(fault);
    }

    /**
     * {@inheritDoc}
     */
    public void pushContinuationState(ContinuationState continuationState) {
        mc.pushContinuationState(continuationState);
    }

    /**
     * {@inheritDoc}
     */
    public Stack<ContinuationState> getContinuationStateStack() {
        return mc.getContinuationStateStack();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isContinuationEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setContinuationEnabled(boolean contStateStackEnabled) {
        mc.setContinuationEnabled(contStateStackEnabled);
    }

    /**
     * {@inheritDoc}
     */
    public Log getServiceLog() {
        return LogFactory.getLog(CommonScriptMessageContext.class);
    }

    /**
     * {@inheritDoc}
     */
    public Mediator getSequenceTemplate(String key) {
        return mc.getSequenceTemplate(key);
    }

    /**
     * Serialize json payload.
     *
     * @param obj Json object which required to be serialized
     */
    private String serializeJSON(Object obj) {
        StringWriter json = new StringWriter();
        if (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        }

        if (obj instanceof NativeObject) {
            json.append("{");
            NativeObject o = (NativeObject) obj;
            Object[] ids = o.getIds();
            boolean first = true;
            for (Object id : ids) {
                String key = (String) id;
                Object value = o.get((String) id, o);
                if (!first) {
                    json.append(", ");
                } else {
                    first = false;
                }
                json.append("\"").append(key).append("\" : ").append(serializeJSON(value));
            }
            json.append("}");
        } else if (obj instanceof NativeArray) {
            json.append("[");
            NativeArray o = (NativeArray) obj;
            Object[] ids = o.getIds();
            boolean first = true;
            for (Object id : ids) {
                Object value = o.get((Integer) id, o);
                if (!first) {
                    json.append(", ");
                } else {
                    first = false;
                }
                json.append(serializeJSON(value));
            }
            json.append("]");
        } else if (obj instanceof Object[]) {
            json.append("[");
            boolean first = true;
            for (Object value : (Object[]) obj) {
                if (!first) {
                    json.append(", ");
                } else {
                    first = false;
                }
                json.append(serializeJSON(value));
            }
            json.append("]");

        } else if (obj instanceof String) {
            json.append("\"").append(obj.toString()).append("\"");
        } else if (obj instanceof Integer ||
                obj instanceof Long ||
                obj instanceof Float ||
                obj instanceof Double ||
                obj instanceof Short ||
                obj instanceof BigInteger ||
                obj instanceof BigDecimal ||
                obj instanceof Boolean) {
            json.append(obj.toString());
        } else {
            json.append("{}");
        }
        return json.toString();
    }

    /**
     * Saves the payload of this message context as a JSON payload.
     *
     * @param jsonPayload Javascript native object to be set as the message body
     * @throws ScriptException in case of creating a JSON object out of the javascript native object.
     */
    @Override
    public void setPayloadJSON(Object jsonPayload) throws ScriptException {
        org.apache.axis2.context.MessageContext messageContext;
        messageContext = ((Axis2MessageContext) mc).getAxis2MessageContext();

        byte[] json = {'{', '}'};
        if (jsonPayload instanceof String) {
            json = jsonPayload.toString().getBytes();
        } else if (jsonPayload != null) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                serializeJson(jsonPayload, out);
                json = out.toByteArray();
            } catch (IOException e) {
                logger.error("#setPayloadJSON. Could not retrieve bytes from JSON object.", e);
            }
        }
        // save this JSON object as the new payload.
        try {
            JsonUtil.getNewJsonPayload(messageContext, json, 0, json.length, true, true);
        } catch (AxisFault axisFault) {
            throw new ScriptException(axisFault);
        }
        //JsonUtil.setContentType(messageContext);
        Object jsonObject = scriptEngine.eval(JsonUtil.newJavaScriptSourceReader(messageContext));
        setJsonObject(mc, jsonObject);
    }

    /**
     * Serialize json payload and writes to output stream.
     *
     * @param obj Json object which required to be serialized
     * @param out Output stream which is required to be written with serialized json
     */
    private void serializeJson(Object obj, OutputStream out) throws IOException {
        if (out == null) {
            logger.warn("#serializeJson. Did not serialize JSON object. Object: " + obj + "  Stream: " + out);
            return;
        }
        if (obj instanceof Wrapper) {
            obj = ((Wrapper) obj).unwrap();
        }
        if (obj == null) {
            out.write("null".getBytes());
        } else if (obj instanceof NativeObject) {
            out.write('{');
            NativeObject o = (NativeObject) obj;
            Object[] ids = o.getIds();
            boolean first = true;
            for (Object id : ids) {
                String key = (String) id;
                Object value = o.get((String) id, o);
                if (!first) {
                    out.write(',');
                    out.write(' ');
                } else {
                    first = false;
                }
                out.write('"');
                out.write(key.getBytes());
                out.write('"');
                out.write(':');
                serializeJson(value, out);
            }
            out.write('}');
        } else if (obj instanceof NativeArray) {
            out.write('[');
            NativeArray o = (NativeArray) obj;
            Object[] ids = o.getIds();
            boolean first = true;
            for (Object id : ids) {
                Object value = o.get((Integer) id, o);
                if (!first) {
                    out.write(',');
                    out.write(' ');
                } else {
                    first = false;
                }
                serializeJson(value, out);
            }
            out.write(']');
        } else if (obj instanceof Object[]) {
            out.write('[');
            boolean first = true;
            for (Object value : (Object[]) obj) {
                if (!first) {
                    out.write(',');
                    out.write(' ');
                } else {
                    first = false;
                }
                serializeJson(value, out);
            }
            out.write(']');
        } else if (obj instanceof String) {
            out.write('"');
            out.write(((String) obj).getBytes());
            out.write('"');
        } else if (obj instanceof ConsString) {
            //This class represents a string composed of two components using the "+" operator
            //in java script with rhino7 upward. ex:var str = "val1" + "val2";
            out.write('"');
            out.write((((ConsString) obj).toString()).getBytes());
            out.write('"');
        } else if (obj instanceof Integer ||
                obj instanceof Long ||
                obj instanceof Float ||
                obj instanceof Double ||
                obj instanceof Short ||
                obj instanceof BigInteger ||
                obj instanceof BigDecimal ||
                obj instanceof Boolean) {
            out.write(obj.toString().getBytes());
        } else {
            out.write('{');
            out.write('}');
        }
    }


    /**
     * {@inheritDoc}
     */
	public Mediator getDefaultConfiguration(String arg0) {
	   return mc.getDefaultConfiguration(arg0);
    }

    /**
     * {@inheritDoc}
     */
    public String getMessageString() {
        return mc.getMessageString();
    }

    /**
     * {@inheritDoc}
     */
    public void setMessageFlowTracingState(int state) {
        mc.setMessageFlowTracingState(state);
    }

    /**
     * {@inheritDoc}
     */
    public int getMessageFlowTracingState() {
        return SynapseConstants.TRACING_OFF;
    }
}
