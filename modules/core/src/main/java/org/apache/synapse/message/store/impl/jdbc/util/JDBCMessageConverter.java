/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.message.store.impl.jdbc.util;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.message.store.impl.jdbc.message.JDBCAxis2Message;
import org.apache.synapse.message.store.impl.jdbc.message.JDBCSynapseMessage;
import org.apache.synapse.message.store.impl.jdbc.message.StorableMessage;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * JDBC helper for StorableMessage
 */
public class JDBCMessageConverter {

    /**
     * Synapse environment of the store
     */
    private static SynapseEnvironment synapseEnvironment;

    /**
     * Logger for the class
     */
    private static Log log = LogFactory.getLog(JDBCMessageConverter.class);

    /**
     * Prefix to identify a OMElemet type property
     */
    private static final String OM_ELEMENT_PREFIX = "OM_ELEMENT_PREFIX_";

    /**
     * Create SynapseMessage out of StorableMessage
     *
     * @param message StorableMessage
     * @return synCtx SynapseMessage
     */
    public static MessageContext createMessageContext(StorableMessage message) {
        SynapseConfiguration configuration = synapseEnvironment.getSynapseConfiguration();
        MessageContext synCtx = null;
        org.apache.axis2.context.MessageContext msgCtx = ((Axis2SynapseEnvironment)
                synapseEnvironment).getAxis2ConfigurationContext().createMessageContext();
        AxisConfiguration axisConfiguration = msgCtx.getConfigurationContext().getAxisConfiguration();
        JDBCAxis2Message jdbcAxis2MessageContext = message.getAxis2Message();
        SOAPEnvelope envelope = getSoapEnvelope(jdbcAxis2MessageContext.getSoapEnvelope());

        try {
            msgCtx.setEnvelope(envelope);
            // Set the  properties
            msgCtx.getOptions().setAction(jdbcAxis2MessageContext.getAction());
            if (jdbcAxis2MessageContext.getRelatesToMessageId() != null) {
                msgCtx.addRelatesTo(new RelatesTo(jdbcAxis2MessageContext.getRelatesToMessageId()));
            }
            msgCtx.setMessageID(jdbcAxis2MessageContext.getMessageID());
            msgCtx.setDoingREST(jdbcAxis2MessageContext.isDoingPOX());
            msgCtx.setDoingMTOM(jdbcAxis2MessageContext.isDoingMTOM());
            msgCtx.setDoingSwA(jdbcAxis2MessageContext.isDoingSWA());

            AxisService axisService =
                    axisConfiguration.getServiceForActivation(jdbcAxis2MessageContext.getService());

            AxisOperation axisOperation =
                    axisService.getOperation(jdbcAxis2MessageContext.getOperationName());

            msgCtx.setFLOW(jdbcAxis2MessageContext.getFlow());
            ArrayList executionChain = new ArrayList();
            if (jdbcAxis2MessageContext.getFlow() ==
                    org.apache.axis2.context.MessageContext.OUT_FLOW) {
                executionChain.addAll(axisOperation.getPhasesOutFlow());
                executionChain.addAll(axisConfiguration.getOutFlowPhases());

            } else if (jdbcAxis2MessageContext.getFlow() ==
                    org.apache.axis2.context.MessageContext.OUT_FAULT_FLOW) {
                executionChain.addAll(axisOperation.getPhasesOutFaultFlow());
                executionChain.addAll(axisConfiguration.getOutFlowPhases());
            }
            msgCtx.setExecutionChain(executionChain);
            ConfigurationContext configurationContext = msgCtx.getConfigurationContext();

            msgCtx.setAxisService(axisService);
            ServiceGroupContext serviceGroupContext =
                    configurationContext.createServiceGroupContext(axisService.getAxisServiceGroup());
            ServiceContext serviceContext = serviceGroupContext.getServiceContext(axisService);

            OperationContext operationContext = serviceContext.createOperationContext(
                    jdbcAxis2MessageContext.getOperationName());
            msgCtx.setServiceContext(serviceContext);
            msgCtx.setOperationContext(operationContext);
            msgCtx.setAxisService(axisService);
            msgCtx.setAxisOperation(axisOperation);
            if (jdbcAxis2MessageContext.getReplyToAddress() != null) {
                msgCtx.setReplyTo(new EndpointReference(jdbcAxis2MessageContext.getReplyToAddress().trim()));
            }

            if (jdbcAxis2MessageContext.getFaultToAddress() != null) {
                msgCtx.setFaultTo(new EndpointReference(jdbcAxis2MessageContext.getFaultToAddress().trim()));
            }

            if (jdbcAxis2MessageContext.getFromAddress() != null) {
                msgCtx.setFrom(new EndpointReference(jdbcAxis2MessageContext.getFromAddress().trim()));
            }

            if (jdbcAxis2MessageContext.getToAddress() != null) {
                msgCtx.getOptions().setTo(new EndpointReference(jdbcAxis2MessageContext.getToAddress().trim()));
            }

            msgCtx.setProperties(jdbcAxis2MessageContext.getProperties());
            msgCtx.setTransportIn(axisConfiguration.
                    getTransportIn(jdbcAxis2MessageContext.getTransportInName()));
            msgCtx.setTransportOut(axisConfiguration.
                    getTransportOut(jdbcAxis2MessageContext.getTransportOutName()));

            if (jdbcAxis2MessageContext.getJsonStream() != null) {
                JsonUtil.newJsonPayload(msgCtx,
                        new ByteArrayInputStream(jdbcAxis2MessageContext.getJsonStream()), true, true);
            }
            JDBCSynapseMessage jdbcSynpaseMessageContext = message.getSynapseMessage();

            synCtx = new Axis2MessageContext(msgCtx, configuration, synapseEnvironment);
            synCtx.setTracingState(jdbcSynpaseMessageContext.getTracingState());

            Iterator<String> it = jdbcSynpaseMessageContext.getProperties().keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = jdbcSynpaseMessageContext.getProperties().get(key);
                synCtx.setProperty(key, value);
            }

            Iterator<String> propertyObjects = jdbcSynpaseMessageContext.getPropertyObjects().keySet().iterator();
            while (propertyObjects.hasNext()) {
                String key = propertyObjects.next();
                Object value = jdbcSynpaseMessageContext.getPropertyObjects().get(key);
                if (key.startsWith(OM_ELEMENT_PREFIX)) {
                    String originalKey = key.substring(OM_ELEMENT_PREFIX.length(), key.length());
                    ByteArrayInputStream is = new ByteArrayInputStream((byte[]) value);
                    try {
                        StAXOMBuilder builder = new StAXOMBuilder(is);
                        OMElement omElement = builder.getDocumentElement();
                        synCtx.setProperty(originalKey, omElement);
                    } catch (XMLStreamException e) {
                        log.error("Error while deserializing the OM element prefix ", e);
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            log.error("Error while closing input stream ", e);
                        }
                    }
                }
            }
            synCtx.setFaultResponse(jdbcSynpaseMessageContext.isFaultResponse());
            synCtx.setResponse(jdbcSynpaseMessageContext.isResponse());
        } catch (Exception e) {
            log.error("Error while deserializing the JDBC Persistent Message ", e);
        }
        return synCtx;
    }

    /**
     * Create StorableMessage out of MessageContext
     *
     * @param synCtx MessageContext
     * @return jdbcMsg  StorableMessage
     */
    public static StorableMessage createStorableMessage(MessageContext synCtx) {
        StorableMessage jdbcMsg = new StorableMessage();
        JDBCAxis2Message jdbcAxis2MessageContext = new JDBCAxis2Message();
        JDBCSynapseMessage jdbcSynpaseMessageContext = new JDBCSynapseMessage();
        Axis2MessageContext axis2MessageContext = null;
        if (synCtx instanceof Axis2MessageContext) {

            /**
             * Serializing the Axis2 Message Context
             */
            try {
                axis2MessageContext = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext msgCtx =
                        axis2MessageContext.getAxis2MessageContext();

                jdbcAxis2MessageContext.setMessageID(msgCtx.getMessageID());
                jdbcAxis2MessageContext.setOperationAction(msgCtx.getAxisOperation().getSoapAction());
                jdbcAxis2MessageContext.setOperationName(msgCtx.getAxisOperation().getName());
                jdbcAxis2MessageContext.setAction(msgCtx.getOptions().getAction());
                jdbcAxis2MessageContext.setService(msgCtx.getAxisService().getName());

                if (JsonUtil.hasAJsonPayload(msgCtx)) {
                    jdbcAxis2MessageContext.setJsonStream(JsonUtil.jsonPayloadToByteArray(msgCtx));
                }
                if (msgCtx.getRelatesTo() != null) {
                    jdbcAxis2MessageContext.setRelatesToMessageId(msgCtx.getRelatesTo().getValue());
                }
                if (msgCtx.getReplyTo() != null) {
                    jdbcAxis2MessageContext.setReplyToAddress(msgCtx.getReplyTo().getAddress());
                }
                if (msgCtx.getFaultTo() != null) {
                    jdbcAxis2MessageContext.setFaultToAddress(msgCtx.getFaultTo().getAddress());
                }
                if (msgCtx.getTo() != null) {
                    jdbcAxis2MessageContext.setToAddress(msgCtx.getTo().getAddress());
                }
                if (msgCtx.getFrom() != null) {
                    jdbcAxis2MessageContext.setFromAddress(msgCtx.getFrom().getAddress());
                }

                jdbcAxis2MessageContext.setDoingPOX(msgCtx.isDoingREST());
                jdbcAxis2MessageContext.setDoingMTOM(msgCtx.isDoingMTOM());
                jdbcAxis2MessageContext.setDoingSWA(msgCtx.isDoingSwA());

                String soapEnvelope = msgCtx.getEnvelope().toString();
                jdbcAxis2MessageContext.setSoapEnvelope(soapEnvelope);
                jdbcAxis2MessageContext.setFlow(msgCtx.getFLOW());
                jdbcAxis2MessageContext.setTransportInName(msgCtx.getTransportIn().getName());
                jdbcAxis2MessageContext.setTransportOutName(msgCtx.getTransportOut().getName());

                Iterator<String> it = msgCtx.getProperties().keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    Object v = msgCtx.getProperty(key);
                    String value = null;
                    if (v != null) {
                        value = v.toString();
                    }
                    jdbcAxis2MessageContext.addProperty(key, value);
                }
            } catch (Exception e) {
                log.error("Incomplete Serialized Message !", e);
            }
            jdbcMsg.setAxis2Message(jdbcAxis2MessageContext);

            jdbcSynpaseMessageContext.setFaultResponse(synCtx.isFaultResponse());
            jdbcSynpaseMessageContext.setTracingState(synCtx.getTracingState());
            jdbcSynpaseMessageContext.setResponse(synCtx.isResponse());

            Set<String> its = synCtx.getPropertyKeySet();
            for (String key : its) {
                Object v = synCtx.getProperty(key);
                if (v instanceof String) {
                    jdbcSynpaseMessageContext.addProperty(key, (String) v);
                } else if (v instanceof ArrayList && ((ArrayList) v).get(0) instanceof OMElement) {
                    OMElement elem = ((OMElement) ((ArrayList) v).get(0));
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        elem.serialize(bos);
                        byte[] bytes = bos.toByteArray();
                        jdbcSynpaseMessageContext.addPropertyObject(OM_ELEMENT_PREFIX + key, bytes);
                    } catch (XMLStreamException e) {
                        log.error("Error while converting OMElement to byte array", e);
                    } finally {
                        try {
                            bos.close();
                        } catch (IOException e) {
                            log.error("Error while closing output stream ", e);
                        }
                    }
                }
            }
            jdbcMsg.setSynapseMessage(jdbcSynpaseMessageContext);
        } else {
            throw new SynapseException("Only Axis2 Messages are supported with JDBCMessage store");
        }
        return jdbcMsg;
    }

    /**
     * Get SOAPEnvelope from String
     *
     * @param soapEnvelpe String to convert
     * @return Successfully built SOAPEnvelope or null
     */
    private static SOAPEnvelope getSoapEnvelope(String soapEnvelpe) {
        try {
            XMLStreamReader xmlReader =
                    StAXUtils.createXMLStreamReader(new ByteArrayInputStream(getUTF8Bytes(soapEnvelpe)));
            StAXBuilder builder = new StAXSOAPModelBuilder(xmlReader);
            SOAPEnvelope soapEnvelope = (SOAPEnvelope) builder.getDocumentElement();
            soapEnvelope.build();
            String soapNamespace = soapEnvelope.getNamespace().getNamespaceURI();
            if (soapEnvelope.getHeader() == null) {
                SOAPFactory soapFactory = null;
                if (soapNamespace.equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                    soapFactory = OMAbstractFactory.getSOAP12Factory();
                } else {
                    soapFactory = OMAbstractFactory.getSOAP11Factory();
                }
                soapFactory.createSOAPHeader(soapEnvelope);
            }
            return soapEnvelope;
        } catch (XMLStreamException e) {
            log.error("Error while deserializing the SOAP ", e);
            return null;
        }
    }

    /**
     * Set the synapseEnvironment
     *
     * @param synapseEnvironment SynapseEnvironment of the message
     */
    public static void setSynapseEnvironment(SynapseEnvironment synapseEnvironment) {
        JDBCMessageConverter.synapseEnvironment = synapseEnvironment;
    }

    /**
     * Get UTF8Bytes out of String
     *
     * @param soapEnvelpe String of soapEnvelope
     * @return bytes       An array of bytes
     */
    private static byte[] getUTF8Bytes(String soapEnvelpe) {
        byte[] bytes = null;
        try {
            bytes = soapEnvelpe.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to extract bytes in UTF-8 encoding. "
                    + "Extracting bytes in the system default encoding", e);
            bytes = soapEnvelpe.getBytes();
        }
        return bytes;
    }
}