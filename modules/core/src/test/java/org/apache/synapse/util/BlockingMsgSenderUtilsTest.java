/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.util;

import junit.framework.TestCase;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.Constants;
import org.apache.axis2.transport.TransportUtils;
import org.apache.synapse.config.xml.endpoints.HTTPEndpointFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.message.senders.blocking.BlockingMsgSenderUtils;
import org.apache.synapse.transport.nhttp.NhttpConstants;

public class BlockingMsgSenderUtilsTest extends TestCase {

    private static final String[] allowedAxis2Properties = {
            "JSON_OBJECT",
            "JSON_STRING",
            "setCharacterEncoding",
            NhttpConstants.DISTRIBUTED_TRANSACTION,
            NhttpConstants.DISTRIBUTED_TRANSACTION_MANAGER,
            Constants.Configuration.ENABLE_REST,
            Constants.Configuration.HTTP_METHOD,
            Constants.Configuration.MESSAGE_TYPE,
            Constants.Configuration.CONTENT_TYPE,
            NhttpConstants.REST_URL_POSTFIX,
            NhttpConstants.REQUEST_HOST_HEADER
    };

    public void testAllowedPropertiesExtraction() throws Exception {
        HTTPEndpointFactory endpointFactory = new HTTPEndpointFactory();
        final String endpointXmlStr =
                "<endpoint><http method=\"get\" uri-template=\"https://test.wso2.com/\" /></endpoint>";
        OMElement omElement = AXIOMUtil.stringToOM(endpointXmlStr);
        EndpointDefinition endpoint = endpointFactory.createEndpointDefinition(omElement);

        final String payloadXmlStr = "<test>value</test>";
        Axis2MessageContext axisInMsgCtx = TestUtils.getAxis2MessageContext(payloadXmlStr, null);

        for (String propertyName: allowedAxis2Properties) {
            String propertyValue = String.format("%s_VALUE", propertyName);
            axisInMsgCtx.getAxis2MessageContext().setProperty(propertyName, propertyValue);
        }

        axisInMsgCtx.getAxis2MessageContext().setProperty(Constants.Configuration.ENABLE_REST, true);

        for (int i = 0; i < 10; i++) {
            String propertyName = String.format("CUSTOM_TEST_PROPERTY_%d", i);
            String propertyValue = String.format("CUSTOM_TEST_PROPERTY_%d_VALUE", i);
            axisInMsgCtx.getAxis2MessageContext().setProperty(propertyName, propertyValue);
        }

        org.apache.axis2.context.MessageContext axisOutMsgCtx = new org.apache.axis2.context.MessageContext();
        SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        axisOutMsgCtx.setEnvelope(envelope);
        BlockingMsgSenderUtils.fillMessageContext(endpoint, axisOutMsgCtx, axisInMsgCtx);

        for (String propertyName: allowedAxis2Properties) {
            assertNotNull(
                    String.format("Allowed Axis2 message context property %s is not copied", propertyName),
                    axisOutMsgCtx.getProperty(propertyName)
            );
        }

        for (int i = 0; i < 10; i++) {
            String propertyName = String.format("CUSTOM_TEST_PROPERTY_%d", i);
            assertNull(
                    String.format("Axis2 message context property %s is not allowed", propertyName),
                    axisOutMsgCtx.getProperty(propertyName)
            );
        }

        assertTrue("Configuration 'enableREST' is not set", TransportUtils.isDoingREST(axisOutMsgCtx));
    }
}
