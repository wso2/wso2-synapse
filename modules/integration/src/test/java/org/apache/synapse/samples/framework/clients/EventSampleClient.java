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

package org.apache.synapse.samples.framework.clients;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.config.Axis2ClientConfiguration;

import javax.xml.namespace.QName;


public class EventSampleClient {

    private static final Log log = LogFactory.getLog(EventSampleClient.class);

    private Options options;
    private ServiceClient serviceClient;
    private Axis2ClientConfiguration configuration;
    private OMFactory factory;
    private OMElement message;
    private OMNamespace schemaNamespace;
    private OMNamespace nss11;
    private OMNamespace addressingNamespace;
    private OMNamespace eventingNamespace;

    public EventSampleClient(Axis2ClientConfiguration configuration) {
        this.configuration = configuration;
        factory = OMAbstractFactory.getOMFactory();
        schemaNamespace = factory.createOMNamespace("http://www.w3.org/2001/XMLSchema", "xmlns");
        nss11 =
                factory.createOMNamespace("http://schemas.xmlsoap.org/soap/envelope", "s11");
        addressingNamespace = factory.createOMNamespace(
                "http://schemas.xmlsoap.org/ws/2004/08/addressing", "wsa");
        eventingNamespace =
                factory.createOMNamespace("http://schemas.xmlsoap.org/ws/2004/08/eventing", "wse");
    }

    private void initializeClient(String addUrl) throws Exception {
        options = new Options();
        ConfigurationContext configContext;
        configContext = ConfigurationContextFactory.
                createConfigurationContextFromFileSystem(configuration.getClientRepo(),
                        configuration.getAxis2Xml());
        serviceClient = new ServiceClient(configContext, null);

        if (addUrl != null && !"null".equals(addUrl)) {
            serviceClient.engageModule("addressing");
            options.setTo(new EndpointReference(addUrl));
        }
        serviceClient.setOptions(options);

        message = factory.createOMElement("message", null);
    }

    private void deInitializeClient() {
        try {
            if (serviceClient != null) {
                serviceClient.cleanup();
            }
        } catch (Exception ignore) {
        }
    }

    public SampleClientResult subscribe(String addUrl, String address, String expires, String topic) {
        OMElement subscribeOm = factory.createOMElement("Subscribe", eventingNamespace);
        OMElement deliveryOm = factory.createOMElement("Delivery", eventingNamespace);
        deliveryOm.addAttribute(factory.createOMAttribute("Mode", null,
                "http://schemas.xmlsoap.org/ws/2004/08/eventing/DeliveryModes/Push"));
        OMElement notifyToOm = factory.createOMElement("NotifyTo", eventingNamespace);
        OMElement addressOm = factory.createOMElement("Address", addressingNamespace);
        factory.createOMText(addressOm, address);
        OMElement expiresOm = factory.createOMElement("Expires", eventingNamespace);
        factory.createOMText(expiresOm, expires);
        OMElement filterOm = factory.createOMElement("Filter", eventingNamespace);
        filterOm.addAttribute(factory.createOMAttribute("Dialect", null,
                "http://synapse.apache.org/eventing/dialect/topicFilter"));
        factory.createOMText(filterOm, topic);


        notifyToOm.addChild(addressOm);
        deliveryOm.addChild(notifyToOm);
        subscribeOm.addChild(deliveryOm);
        if (!(expires.equals("*"))) {
            subscribeOm.addChild(expiresOm); // Add only if the value provided
        }
        subscribeOm.addChild(filterOm);

        log.info("Subscribing: " + subscribeOm.toString());
        SampleClientResult clientResult = new SampleClientResult();
        try {
            initializeClient(addUrl);
            options.setAction("http://schemas.xmlsoap.org/ws/2004/08/eventing/Subscribe");

            OMElement response = serviceClient.sendReceive(subscribeOm);
            log.info("Subscribed to topic " + topic);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            log.info("Response Received: " + response.toString());
            String subId =
                    response.getFirstChildWithName(
                            new QName(eventingNamespace.getNamespaceURI(), "SubscriptionManager"))
                            .getFirstChildWithName(
                                    new QName(addressingNamespace.getNamespaceURI(), "ReferenceParameters"))
                            .getFirstChildWithName(
                                    new QName(eventingNamespace.getNamespaceURI(), "Identifier")).getText();
            log.info("Subscription identifier: " + subId);
            clientResult.addProperty("subId", subId);
            clientResult.incrementResponseCount();
        } catch (Exception e) {
            log.error("Fault Received : " + e.toString(), e);
            clientResult.setException(e);
        }
        deInitializeClient();
        return clientResult;

    }


    public SampleClientResult unsubscribe(String addUrl, String identifier) {
        /** Send unsubscribe message
         (01) <s12:Envelope
         (02)     xmlns:s12="http://www.w3.org/2003/05/soap-envelope"
         (03)     xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing"
         (04)     xmlns:wse="http://schemas.xmlsoap.org/ws/2004/08/eventing"
         (05)     xmlns:ow="http://www.example.org/oceanwatch" >
         (06)   <s12:Header>
         (07)     <wsa:Action>
         (08)       http://schemas.xmlsoap.org/ws/2004/08/eventing/Unsubscribe
         (09)     </wsa:Action>
         (10)     <wsa:MessageID>
         (11)       uuid:2653f89f-25bc-4c2a-a7c4-620504f6b216
         (12)     </wsa:MessageID>
         (13)     <wsa:ReplyTo>
         (14)      <wsa:Address>http://www.example.com/MyEventSink</wsa:Address>
         (15)     </wsa:ReplyTo>
         (16)     <wsa:To>
         (17)       http://www.example.org/oceanwatch/SubscriptionManager
         (18)     </wsa:To>
         (19)     <wse:Identifier>
         (20)       uuid:22e8a584-0d18-4228-b2a8-3716fa2097fa
         (21)     </wse:Identifier>
         (22)   </s12:Header>
         (23)   <s12:Body>
         (24)     <wse:Unsubscribe />
         (25)   </s12:Body>
         (26) </s12:Envelope>*/
        OMElement subscribeOm = factory.createOMElement("Unsubscribe", eventingNamespace);

        log.info("UnSubscribing: " + subscribeOm.toString());
        SampleClientResult clientResult = new SampleClientResult();
        try {
            initializeClient(addUrl);
            options.setAction("http://schemas.xmlsoap.org/ws/2004/08/eventing/Unsubscribe");

            OMElement identifierOm = factory.createOMElement("Identifier", eventingNamespace);
            factory.createOMText(identifierOm, identifier);
            serviceClient.addHeader(identifierOm);
            OMElement response = serviceClient.sendReceive(subscribeOm);
            log.info("UnSubscribed to ID " + identifier);
            Thread.sleep(1000);
            log.info("UnSubscribe Response Received: " + response.toString());
            clientResult.incrementResponseCount();
        } catch (Exception e) {
            log.error("Fault Received : " + e.toString(), e);
            clientResult.setException(e);
        }
        deInitializeClient();
        return clientResult;
    }

    public SampleClientResult renew(String addUrl, String expires, String identifier) {
        /**
         * (01) <s12:Envelope
         (02)     xmlns:s12="http://www.w3.org/2003/05/soap-envelope"
         (03)     xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing"
         (04)     xmlns:wse="http://schemas.xmlsoap.org/ws/2004/08/eventing"
         (05)     xmlns:ow="http://www.example.org/oceanwatch" >
         (06)   <s12:Header>
         (07)     <wsa:Action>
         (08)       http://schemas.xmlsoap.org/ws/2004/08/eventing/Renew
         (09)     </wsa:Action>
         (10)     <wsa:MessageID>
         (11)       uuid:bd88b3df-5db4-4392-9621-aee9160721f6
         (12)     </wsa:MessageID>
         (13)     <wsa:ReplyTo>
         (14)      <wsa:Address>http://www.example.com/MyEventSink</wsa:Address>
         (15)     </wsa:ReplyTo>
         (16)     <wsa:To>
         (17)       http://www.example.org/oceanwatch/SubscriptionManager
         (18)     </wsa:To>
         (19)     <wse:Identifier>
         (20)       uuid:22e8a584-0d18-4228-b2a8-3716fa2097fa
         (21)     </wse:Identifier>
         (22)   </s12:Header>
         (23)   <s12:Body>
         (24)     <wse:Renew>
         (25)       <wse:Expires>2004-06-26T21:07:00.000-08:00</wse:Expires>
         (26)     </wse:Renew>
         (27)   </s12:Body>
         (28) </s12:Envelope>
         */

        OMElement subscribeOm = factory.createOMElement("Renew", eventingNamespace);
        OMElement expiresOm = factory.createOMElement("Expires", eventingNamespace);
        factory.createOMText(expiresOm, expires);
        subscribeOm.addChild(expiresOm);


        log.info("SynapseSubscription Renew \n" + subscribeOm.toString());
        SampleClientResult clientResult = new SampleClientResult();
        try {
            initializeClient(addUrl);
            OMElement identifierOm = factory.createOMElement("Identifier", eventingNamespace);
            factory.createOMText(identifierOm, identifier);
            serviceClient.addHeader(identifierOm);
            options.setAction("http://schemas.xmlsoap.org/ws/2004/08/eventing/Renew");
            OMElement response = serviceClient.sendReceive(subscribeOm);
            log.info("SynapseSubscription Renew to ID " + identifier);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            log.info("SynapseSubscription Renew Response Received: " + response.toString());
            clientResult.incrementResponseCount();
        } catch (Exception e) {
            log.error("Fault Received : " + e.toString(), e);
            clientResult.setException(e);
        }
        deInitializeClient();
        return clientResult;
    }

    public SampleClientResult getStatus(String addUrl, String identifier) {
        /**
         * (01) <s12:Envelope
         (02)     xmlns:s12="http://www.w3.org/2003/05/soap-envelope"
         (03)     xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing"
         (04)     xmlns:wse="http://schemas.xmlsoap.org/ws/2004/08/eventing"
         (05)     xmlns:ow="http://www.example.org/oceanwatch" >
         (06)   <s12:Header>
         (07)     <wsa:Action>
         (08)       http://schemas.xmlsoap.org/ws/2004/08/eventing/GetStatus
         (09)     </wsa:Action>
         (10)     <wsa:MessageID>
         (11)       uuid:bd88b3df-5db4-4392-9621-aee9160721f6
         (12)     </wsa:MessageID>
         (13)     <wsa:ReplyTo>
         (14)       <wsa:Address>http://www.example.com/MyEventSink</wsa:Address>
         (15)     </wsa:ReplyTo>
         (16)     <wsa:To>
         (17)       http://www.example.org/oceanwatch/SubscriptionManager
         (18)     </wsa:To>
         (19)     <wse:Identifier>
         (20)       uuid:22e8a584-0d18-4228-b2a8-3716fa2097fa
         (21)     </wse:Identifier>
         (22)   </s12:Header>
         (23)   <s12:Body>
         (24)     <wse:GetStatus />
         (25)   </s12:Body>
         (26) </s12:Envelope>
         */
        OMElement subscribeOm = factory.createOMElement("GetStatus", eventingNamespace);

        log.info("GetStatus using: " + subscribeOm.toString());
        SampleClientResult clientResult = new SampleClientResult();
        try {
            initializeClient(addUrl);
            options.setAction("http://schemas.xmlsoap.org/ws/2004/08/eventing/GetStatus");

            OMElement identifierOm = factory.createOMElement("Identifier", eventingNamespace);
            factory.createOMText(identifierOm, identifier);
            serviceClient.addHeader(identifierOm);
            OMElement response = serviceClient.sendReceive(subscribeOm);
            log.info("GetStatus to ID " + identifier);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            log.info("GetStatus Response Received: " + response.toString());
            clientResult.incrementResponseCount();
        } catch (Exception e) {
            log.error("Fault Received : " + e.toString(), e);
            clientResult.setException(e);
        }
        deInitializeClient();
        return clientResult;
    }

    public SampleClientResult sendEvent(String addUrl, String symbol, String price, String qty,
                                        String topic, String topicns) {
        SampleClientResult clientResult = new SampleClientResult();
        try {
            initializeClient(addUrl);
            OMNamespace aipNamespace = factory.createOMNamespace(topicns, "aip");
            // set the target topic
            OMElement topicOm = factory.createOMElement("Topic", aipNamespace);
            factory.createOMText(topicOm, topic);
            serviceClient.addHeader(topicOm);
            // set for fire and forget
            options.setProperty(MessageContext.CLIENT_API_NON_BLOCKING, Boolean.FALSE);

            OMElement payload =
                    AXIOMUtil.stringToOM("<m:placeOrder xmlns:m=\"http://services.samples\">\n" +
                            "    <m:order>\n" +
                            "        <m:price>" + price + "</m:price>\n" +
                            "        <m:quantity>" + qty + "</m:quantity>\n" +
                            "        <m:symbol>" + symbol + "</m:symbol>\n" +
                            "    </m:order>\n" +
                            "</m:placeOrder>");

            log.info("Sending Event : \n" + payload.toString());
            serviceClient.fireAndForget(payload);
            log.info("Event sent to topic " + topic);
            Thread.sleep(1000);
            clientResult.incrementResponseCount();
        } catch (Exception e) {
            log.error("Fault Received : " + e.toString(), e);
            clientResult.setException(e);
        }
        deInitializeClient();
        return clientResult;
    }
}