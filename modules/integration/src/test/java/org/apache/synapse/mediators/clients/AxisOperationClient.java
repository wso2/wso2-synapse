/*
*Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.apache.synapse.mediators.clients;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.config.Axis2ClientConfiguration;
import java.io.IOException;

public class AxisOperationClient {
    private static final Log log = LogFactory.getLog(AxisOperationClient.class);
    private MessageContext outMsgCtx;
    private ConfigurationContext configContext;
    private ServiceClient serviceClient;
    private OperationClient operationClient;
    private SOAPFactory fac;
    private SOAPEnvelope envelope;
    private Axis2ClientConfiguration clientConfig;

    public AxisOperationClient(Axis2ClientConfiguration clientConfig) {
        this.clientConfig = clientConfig;
    }

    private void init() throws AxisFault {
        configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                clientConfig.getClientRepo(), clientConfig.getAxis2Xml());
        serviceClient = new ServiceClient(configContext, null);
    }

    /**
     * @param trpUrl Transport URL
     * @param addUrl WS-Addressing URL
     * @param symbol Stock quote symbol
     * @param iterations No of iterations
     * @return Response OMElement
     * @throws IOException
     */
    public OMElement sendMultipleQuoteRequest(String trpUrl, String addUrl, String symbol,
                                              int iterations)
            throws IOException {
        init();
        return createMultipleQuoteRequest(trpUrl, addUrl, symbol, iterations);
    }

    /**
     * Send custom payload
     *
     * <ns:getQuote>
     *    <ns:request>
     *        <ns:symbols>
     *            <ns:company></ns:company>
     *        </ns:symbols>
     *    </ns:request>
     * </ns:getQuote>
     * @param trpUrl Transport url
     * @param addUrl WS-Addressing EPR url
     * @param symbol Stock quote symbol
     * @param action Soap action
     * @return
     * @throws AxisFault
     */
    public OMElement sendCustomPayload(String trpUrl, String addUrl, String symbol, String action) throws AxisFault {
        init();
        OMElement payload = createCustomPayload(symbol);
        operationClient = serviceClient.createClient(ServiceClient.ANON_OUT_IN_OP);
        setMessageContext(addUrl, trpUrl, action);
        outMsgCtx.setEnvelope(createSOAPEnvelope(payload));
        operationClient.addMessageContext(outMsgCtx);
        operationClient.execute(true);
        MessageContext inMsgtCtx = operationClient.getMessageContext("In");
        SOAPEnvelope response = inMsgtCtx.getEnvelope();
        return response;
    }

    /**
     *
     * @param trpUrl Transport url
     * @param addUrl WS-Addressing EPR url
     * @param payload Request payload
     * @param action Soap Action
     * @return  Soap envelope
     * @throws AxisFault
     */

    public OMElement send(String trpUrl, String addUrl, OMElement payload, String action) throws AxisFault {
        init();
        operationClient = serviceClient.createClient(ServiceClient.ANON_OUT_IN_OP);
        setMessageContext(addUrl, trpUrl, action);
        outMsgCtx.setEnvelope(createSOAPEnvelope(payload));
        operationClient.addMessageContext(outMsgCtx);
        operationClient.execute(true);
        MessageContext inMsgtCtx = operationClient.getMessageContext("In");
        SOAPEnvelope response = inMsgtCtx.getEnvelope();
        return response;
    }

    /**
     * Creating the multiple quote request body
     *
     * @param symbol Stock quote symbol
     * @param iterations No of iterations
     * @return Request payload
     */
    private OMElement createMultipleQuoteRequestBody(String symbol, int iterations) {
        OMNamespace omNs = fac.createOMNamespace("http://services.samples", "ns");
        OMElement method = fac.createOMElement("getQuote", omNs);

        for (int i = 0; i < iterations; i++) {
            OMElement requestElement = fac.createOMElement("request", omNs);
            OMElement symbolElement = fac.createOMElement("symbol", omNs);
            symbolElement.addChild(fac.createOMText(requestElement, symbol));
            requestElement.addChild(symbolElement);
            method.addChild(requestElement);
        }
        return method;
    }

    /**
     * Creating the multiple quote request
     *
     * @param trpUrl Transport URL
     * @param addUrl WS-Addressing EPR url
     * @param symbol Stock quote symbol
     * @param iterations No of iterations
     * @return Response from the backend
     * @throws IOException
     */
    private OMElement createMultipleQuoteRequest(String trpUrl, String addUrl, String symbol,
                                                 int iterations) throws IOException {
        init();
        operationClient = serviceClient.createClient(ServiceClient.ANON_OUT_IN_OP);
        setMessageContext(addUrl, trpUrl, null);
        outMsgCtx.setEnvelope(createSOAPEnvelope(symbol, iterations));
        operationClient.addMessageContext(outMsgCtx);
        operationClient.execute(true);
        MessageContext inMsgtCtx = operationClient.getMessageContext("In");
        SOAPEnvelope response = inMsgtCtx.getEnvelope();
        return response;

    }

    /**
     * Create custom payload
     * @param symbol Stock quote symbol
     * @return Request payload
     */
    private OMElement createCustomPayload(String symbol) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://services.samples", "ns");
        OMElement payload = fac.createOMElement("getQuote", omNs);
        OMElement request = fac.createOMElement("request", omNs);
        OMElement symbols = fac.createOMElement("symbols", omNs);
        OMElement company = fac.createOMElement("company", omNs);
        company.setText(symbol);
        symbols.addChild(company);
        request.addChild(symbols);
        payload.addChild(request);
        return payload;
    }

    /**
     * Creating the message context of the soap message     *
     * @param addUrl WS-Addressing EPR url
     * @param trpUrl Transport url
     * @param action Soap action
     */
    private void setMessageContext(String addUrl, String trpUrl, String action) {
        outMsgCtx = new MessageContext();
        //assigning message context&rsquo;s option object into instance variable
        Options options = outMsgCtx.getOptions();
        //setting properties into option
        if (trpUrl != null && !"null".equals(trpUrl)) {
            options.setProperty(Constants.Configuration.TRANSPORT_URL, trpUrl);
        }
        if (addUrl != null && !"null".equals(addUrl)) {
            options.setTo(new EndpointReference(addUrl));
        }
        if(action != null && !"null".equals(action)) {
            options.setAction(action);
        }
    }

    /**
     * Create the soap envelope
     *
     * @param symbol Stock quote symbol
     * @param iterations No of iterations
     * @return Soap envelope containing request payload
     */
    private SOAPEnvelope createSOAPEnvelope(String symbol, int iterations) {
        fac = OMAbstractFactory.getSOAP11Factory();
        envelope = fac.getDefaultEnvelope();
        envelope.getBody().addChild(createMultipleQuoteRequestBody(symbol, iterations));
        return envelope;
    }

    /**
     * @param payload Request payload
     * @return Soap envelope containing request payload
     */

    private SOAPEnvelope createSOAPEnvelope(OMElement payload) {
        fac = OMAbstractFactory.getSOAP11Factory();
        envelope = fac.getDefaultEnvelope();
        envelope.getBody().addChild(payload);
        return envelope;
    }

    /**
     *   Destroy objects
     */
    public void destroy() {
        try {
            serviceClient.cleanup();
            configContext.cleanupContexts();
            configContext.terminate();
        } catch (AxisFault axisFault) {
            log.error("Error while cleaning up the service clients", axisFault);
        }
        outMsgCtx = null;
        serviceClient = null;
        operationClient = null;
        configContext = null;
        envelope = null;
        fac = null;
        
    }
}
