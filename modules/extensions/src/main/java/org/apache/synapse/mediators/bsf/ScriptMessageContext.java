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

import org.apache.axiom.om.OMException;
import org.apache.synapse.MessageContext;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * ScriptMessageContext decorates the MessageContext interface by adding methods to use when
 * deals with message payload XML/Json, in a way natural to the scripting languageS.
 */
public interface ScriptMessageContext extends MessageContext {

    /**
     * Get the XML representation of SOAP Body payload.
     * The payload is the first element inside the SOAP <Body> tags
     *
     * @return the XML SOAP Body
     * @throws ScriptException in-case of an error in getting
     * the XML representation of SOAP Body payload
     */
    Object getPayloadXML() throws ScriptException;

    /**
     * Set the SOAP body payload from XML.
     *
     * @param payload Message payload
     * @throws ScriptException For errors in converting xml To OM
     * @throws OMException     For errors in OM manipulation
     */
    void setPayloadXML(Object payload) throws OMException, ScriptException;


    /**
     * Get the JSON object representation of the JSON message body of the request.
     *
     * @return JSON object of the message body
     */
    Object getPayloadJSON();


    /**
     * Get the Message Payload as a text.
     *
     * @return Payload as text
     */
    Object getJsonText();

    /**
     * Get the Message Payload as a text.
     *
     * @return Payload as text
     */
    String getPayloadText();

    /**
     * Saves the JavaScript Object to the message context.
     * @param messageContext The message context of the sequence
     * @param jsonObject JavaScript Object which is passed to be saved in message context
     * @return true
     */
    boolean setJsonObject(MessageContext messageContext, Object jsonObject);

    /**
     * Saves the JSON String to the message context.
     * @param messageContext The message context of the sequence
     * @param jsonObject JavaScript string which is passed to be saved in message context
     * @return false if messageContext is null return true otherwise
     */
    boolean setJsonText(MessageContext messageContext, Object jsonObject);

    /**
     * Get the JSON object representation of the JSON message body of the request.
     *
     * @return JSON object of the message body
     */
    Object jsonObject(MessageContext messageContext);

    /**
     * Set a script engine.
     *
     * @param scriptEngine a ScriptEngine instance
     */
    void setScriptEngine(ScriptEngine scriptEngine);

    void addHeader(boolean mustUnderstand, Object content) throws ScriptException;

    Object getEnvelopeXML() throws ScriptException;

    /**
     * Helpers to set EPRs from a script string.
     *
     */
    void setTo(String reference);

    void setFaultTo(String reference);

    void setFrom(String reference);

    void setReplyTo(String reference);

    /**
     * Add a new property to the message.
     *
     * @param key unique identifier of property
     * @param value value of property
     * @param scope scope of the property
     * @throws ScriptException if an error occurs when converting the XML to OM
     */
    void setProperty(String key, Object value, String scope);

    /**
     * Remove property from the message.
     *
     * @param key unique identifier of property
     * @param scope scope of the property
     * @throws ScriptException if an error occurs when converting the XML to OM
     */
    void removeProperty(String key, String scope);

    /**
     * Saves the payload of this message context as a JSON payload.
     *
     * @param jsonPayload Javascript native object to be set as the message body
     * @throws ScriptException in case of creating a JSON object out of
     *                         the javascript native object.
     */
    void setPayloadJSON(Object jsonPayload) throws ScriptException;

}
