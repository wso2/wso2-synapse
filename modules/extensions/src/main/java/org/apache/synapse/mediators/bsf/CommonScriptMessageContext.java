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
 * CommonScriptMessageContext decorates the MessageContext interface by adding methods to use when
 * deals with message payload XML/Json, in a way natural to the scripting languageS
 */
public interface CommonScriptMessageContext extends MessageContext{
    Object getPayloadXML() throws ScriptException;

    void setPayloadXML(Object payload) throws OMException, ScriptException;

    Object getPayloadJSON();

    Object getJsonText();

    String getPayloadText();

    void setPayloadJSON0(Object jsonPayload) throws ScriptException;

    boolean setJsonObject(MessageContext messageContext, Object jsonObject);

    boolean setJsonText(MessageContext messageContext, Object jsonObject);

    Object jsonObject(MessageContext messageContext);

    void setScriptEngine(ScriptEngine scriptEngine);

    void addHeader(boolean mustUnderstand, Object content) throws ScriptException;

    Object getEnvelopeXML() throws ScriptException;

    // helpers to set EPRs from a script string
    void setTo(String reference);

    void setFaultTo(String reference);

    void setFrom(String reference);

    void setReplyTo(String reference);

    void setProperty(String key, Object value, String scope);

    void removeProperty(String key, String scope);

    void setPayloadJSON(Object jsonPayload) throws ScriptException;

    void addComponentToMessageFlow(String mediatorId);
}
