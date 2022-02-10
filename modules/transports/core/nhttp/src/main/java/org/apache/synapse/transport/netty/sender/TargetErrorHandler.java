/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
 *
 */
package org.apache.synapse.transport.netty.sender;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.OutOnlyAxisOperation;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.TargetConfiguration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * {@code TargetErrorHandler} will report back failure to the message receiver.
 */
public class TargetErrorHandler {

    private static final Log LOG = LogFactory.getLog(TargetErrorHandler.class);

    private final TargetConfiguration targetConfiguration;

    public TargetErrorHandler(TargetConfiguration targetConfiguration) {

        this.targetConfiguration = targetConfiguration;
    }

    public void handleError(final MessageContext msgContext, final int errorCode, final String errorMessage,
                            final Throwable exceptionToRaise) {

        if (cannotProceedWithErrorHandling(msgContext, errorCode, errorMessage, exceptionToRaise)) {
            return;
        }

        targetConfiguration.getWorkerPool().execute(new Runnable() {
            public void run() {

                MessageReceiver msgReceiver = msgContext.getAxisOperation().getMessageReceiver();

                try {
                    AxisFault axisFault = (exceptionToRaise != null
                            ? new AxisFault(errorMessage, exceptionToRaise) : new AxisFault(errorMessage));

                    MessageContext faultMessageContext =
                            MessageContextBuilder.createFaultMessageContext(msgContext, axisFault);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Sending Fault for Request with Message ID : " + msgContext.getMessageID());
                    }

                    populateProperties(faultMessageContext, msgContext, errorCode, errorMessage, exceptionToRaise);
                    msgReceiver.receive(faultMessageContext);

                } catch (AxisFault af) {
                    LOG.error("Unable to report failure back to the message receiver", af);
                }
            }
        });
    }

    /**
     * Populates required properties in the message context.
     */
    private void populateProperties(MessageContext faultMessageContext, MessageContext msgContext, int errorCode,
                                    String errorMessage, Throwable exceptionToRaise) {

        faultMessageContext.setTo(null);
        faultMessageContext.setServerSide(true);
        faultMessageContext.setDoingREST(msgContext.isDoingREST());
        faultMessageContext.setProperty(MessageContext.TRANSPORT_IN, msgContext
                .getProperty(MessageContext.TRANSPORT_IN));
        faultMessageContext.setTransportIn(msgContext.getTransportIn());
        faultMessageContext.setTransportOut(msgContext.getTransportOut());
        faultMessageContext.setOperationContext(msgContext.getOperationContext());
        faultMessageContext.setConfigurationContext(msgContext.getConfigurationContext());

        if (!(msgContext.getOperationContext().getAxisOperation() instanceof OutOnlyAxisOperation)) {
            faultMessageContext.setAxisMessage(msgContext.getOperationContext().getAxisOperation()
                    .getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
        }

        faultMessageContext.setProperty(BridgeConstants.SENDING_FAULT, Boolean.TRUE);
        faultMessageContext.setProperty(BridgeConstants.ERROR_MESSAGE, errorMessage);

        if (errorCode != -1) {
            faultMessageContext.setProperty(BridgeConstants.ERROR_CODE, getErrorCode(errorCode));
        }

        SOAPEnvelope envelope = faultMessageContext.getEnvelope();
        if (exceptionToRaise != null) {
            faultMessageContext.setProperty(BridgeConstants.ERROR_DETAIL, getStackTrace(exceptionToRaise));
            faultMessageContext.setProperty(BridgeConstants.ERROR_EXCEPTION, exceptionToRaise);
            envelope.getBody().getFault().getDetail().setText(exceptionToRaise.toString());
        } else {
            faultMessageContext.setProperty(BridgeConstants.ERROR_DETAIL, errorMessage);
            envelope.getBody().getFault().getDetail().setText(errorMessage);
        }

        faultMessageContext.setProperty(BridgeConstants.NO_ENTITY_BODY, true);
        faultMessageContext.setProperty(BridgeConstants.INTERNAL_EXCEPTION_ORIGIN,
                msgContext.getProperty(BridgeConstants.INTERNAL_EXCEPTION_ORIGIN));
    }

    private boolean cannotProceedWithErrorHandling(MessageContext msgContext, int errorCode,
                                                   String errorMessage, Throwable exceptionToRaise) {

        if (errorCode == -1 && errorMessage == null && exceptionToRaise == null) {
            return true;
        }

        return msgContext.getAxisOperation() == null || msgContext.getAxisOperation().getMessageReceiver() == null;
    }

    private int getErrorCode(int errorCode) {

        return errorCode;
    }

    private String getStackTrace(Throwable aThrowable) {

        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
