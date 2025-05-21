/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package com.synapse.core.artifacts.api;

import com.synapse.core.artifacts.api.Resource;
import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.Message;
import com.synapse.core.synctx.MsgContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class APIMessageReceiver extends AbstractMessageReceiver {
    private static final Logger log = LogManager.getLogger(APIMessageReceiver.class);
    
    private final Resource resource;
    private final String apiContext;
    private final InboundMessageMediator mediator;

    public APIMessageReceiver(Resource resource, String apiContext, InboundMessageMediator mediator) {
        this.resource = resource;
        this.apiContext = apiContext;
        this.mediator = mediator;
    }

    @Override
    public void invokeBusinessLogic(MessageContext messageContext) throws AxisFault {
        try {
            // Convert Axis2 MessageContext to our MsgContext
            MsgContext msgContext = convertToMsgContext(messageContext);
            
            // Execute the resource's inSequence
            if (resource.getInSequence() != null) {
                boolean success = resource.getInSequence().execute(msgContext);
                
                if (!success && resource.getFaultSequence() != null) {
                    // If inSequence fails, execute faultSequence
                    resource.getFaultSequence().execute(msgContext);
                }
            } else {
                log.warn("No inSequence defined for resource: {}", resource.getUriTemplate());
            }
            
            // Convert our MsgContext back to Axis2 MessageContext
            updateAxisMessageContext(messageContext, msgContext);
            
        } catch (Exception e) {
            log.error("Error processing request", e);
            try {
                if (resource.getFaultSequence() != null) {
                    MsgContext errorContext = createErrorContext(messageContext, e);
                    resource.getFaultSequence().execute(errorContext);
                }
            } catch (Exception ex) {
                log.error("Error in fault sequence", ex);
            }
            throw new AxisFault("Error processing request", e);
        }
    }

    private MsgContext convertToMsgContext(MessageContext axisContext) throws Exception {
        MsgContext msgContext = new MsgContext();
        
        // Extract headers
        Map<String, String> headers = new HashMap<>();
        
        // Extract HTTP headers
        Object transportHeaders = axisContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> httpHeaders = (Map<String, String>) transportHeaders;
            headers.putAll(httpHeaders);
        }
        
        // Set message content
        byte[] content = axisContext.getEnvelope().toString().getBytes();
        String contentType = (String) axisContext.getProperty("ContentType");
        if (contentType == null) {
            contentType = "application/xml";
        }
        
        Message message = new Message(content, contentType);
        msgContext.setMessage(message);
        msgContext.setHeaders(headers);
        
        // Add properties that might be needed by the sequence
        msgContext.getProperties().put("HTTP_METHOD", axisContext.getProperty("HTTP_METHOD").toString());
        msgContext.getProperties().put("REST_URL_POSTFIX", axisContext.getProperty("REST_URL_POSTFIX").toString());
        msgContext.getProperties().put("API_CONTEXT", apiContext);
        msgContext.getProperties().put("RESOURCE_URI", resource.getUriTemplate());
        
        return msgContext;
    }

    private void updateAxisMessageContext(MessageContext axisContext, MsgContext msgContext) {
        // Set response content
        try {
            if (msgContext.getMessage() != null) {
                axisContext.setProperty("ContentType", msgContext.getMessage().getContentType());
                
                // Update response headers
                @SuppressWarnings("unchecked")
                Map<String, String> transportHeaders = (Map<String, String>) axisContext.getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                
                if (transportHeaders == null) {
                    transportHeaders = new HashMap<>();
                    axisContext.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, transportHeaders);
                }
                
                transportHeaders.putAll(msgContext.getHeaders());
            }
        } catch (Exception e) {
            log.error("Error updating Axis2 message context", e);
        }
    }

    private MsgContext createErrorContext(MessageContext axisContext, Exception error) {
        MsgContext errorContext = new MsgContext();
        
        // Set error details in properties
        errorContext.getProperties().put("ERROR_MESSAGE", error.getMessage());
        errorContext.getProperties().put("ERROR_EXCEPTION", error.toString());
        errorContext.getProperties().put("ERROR_CODE", "500");
        
        // Set basic headers
        Map<String, String> headers = new HashMap<>();
        errorContext.setHeaders(headers);
        
        // Create an error message
        String errorContent = "<error><message>" + error.getMessage() + "</message></error>";
        Message errorMessage = new Message(errorContent.getBytes(), "application/xml");
        errorContext.setMessage(errorMessage);
        
        return errorContext;
    }
}