/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.util;

import org.apache.http.protocol.HTTP;
import org.apache.synapse.MessageContext;

import java.util.Map;
import javax.xml.stream.XMLStreamException;

/*
 *  This class contains the util methods with respect to mediator properties
 */
public class MediatorPropertyUtils {

    /**
     * This method removes the current content-type header value from the Axis2 message context and
     * set the given value.
     * @param propertyName Message type property
     * @param resultValue Value to be set
     * @param axis2MessageCtx Axis2 message context
     */
    public static void handleSpecialProperties(String propertyName, Object resultValue,
                                               org.apache.axis2.context.MessageContext axis2MessageCtx) {
        if (org.apache.axis2.Constants.Configuration.MESSAGE_TYPE.equals(propertyName)) {
            axis2MessageCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, resultValue);
            Map headers = (Map) axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers != null) {
                headers.remove(HTTP.CONTENT_TYPE);
                headers.put(HTTP.CONTENT_TYPE, resultValue);
            }
        }
    }

    /**
     * This method just serializes the OMElement, when setting a message type, we need to serialize to access the
     * inner element.
     *
     * @param msgCtx Synapse MessageContext
     */
    public static void serializeOMElement(MessageContext msgCtx) throws XMLStreamException {

        msgCtx.getEnvelope().toString(); // This is an implemented method in OMElement
    }
}
