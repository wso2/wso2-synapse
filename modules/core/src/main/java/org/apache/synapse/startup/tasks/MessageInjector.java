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

package org.apache.synapse.startup.tasks;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseHandler;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.task.Task;
import org.apache.synapse.util.PayloadHelper;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Injects a Message into a named sequence or a proxy service configured in the Synapse
 * mediation engine. By default this task implementation will inject messages into the
 * main sequence.
 */
public class MessageInjector implements Task, ManagedLifecycle {

    /**
     * Holds the logger for logging purposes
     */
    private Log log = LogFactory.getLog(MessageInjector.class);

    /**
     * Holds the Message to be injected
     */
    private OMElement message = null;

    /**
     * Holds the to address for the message to be injected
     */
    private String to = null;

    /**
     * Could be one of either "soap11" | "soap12" | "pox" | "get"
     */
    private String format = null;

    /**
     * SOAPAction of the message to be set, in case of the format is soap11
     */
    private String soapAction = null;

    /**
     * Holds the SynapseEnv to which the message will be injected
     */
    private SynapseEnvironment synapseEnvironment;

    public final static String SOAP11_FORMAT = "soap11";
    public final static String SOAP12_FORMAT = "soap12";
    public final static String POX_FORMAT = "pox";
    public final static String GET_FORMAT = "get";

    private final static String INJECT_TO_PROXY = "proxy";
    private final static String INJECT_TO_SEQUENCE = "sequence";
    private final static String INJECT_TO_MAIN_SEQ = "main";

    /**
     *  Artifact type which message should be injected
     *  Could be one of "proxy" | "sequence" | "main"
     */
    private String injectTo = INJECT_TO_MAIN_SEQ;

    /**
     * Name of the sequence which message should be injected
     */
    private String sequenceName = null;

    /**
     * Name of the proxy service which message should be injected
     */
    private String proxyName = null;

    /**
     * Registry path for message to inject
     */
    private String registryKey = null;

    /**
     * Invoke handlers when calling a sequence
     */
    private String invokeHandlers = null;

    /**
     * Store additional properties required at the runtime
     */
    private Map<String, Object> runtimeProperties = null;

    /**
     * Initializes the Injector
     *
     * @param se
     *          SynapseEnvironment of synapse
     */
    public void init(SynapseEnvironment se) {
		synapseEnvironment = se;
        runtimeProperties = new HashMap<String, Object>();
	}

    /**
     * Set the message to be injected
     *
     * @param elem
     *          OMElement describing the message
     */
    public void setMessage(OMElement elem) {
		log.debug("set message " + elem.toString());
		message = elem;
	}

    /**
     * Set the to address of the message to be injected
     *
     * @param url
     *          String containing the to address
     */
    public void setTo(String url) {
		to = url;
	}

    /**
     * Sets the format of the message
     *
     * @param format could be one of either "soap11" | "soap12" | "pox" | "get"
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Enable / Disable invoking the handlers.
     * Applicable only when injectTo = "sequence" | "main"
     * @param invokeHandlers should be "true" | "false"
     */
    public void setInvokeHandlers(String invokeHandlers) {
        this.invokeHandlers = invokeHandlers;
    }

    /**
     * Sets the SOAPAction and valid only when the format is given as soap11
     *
     * @param soapAction SOAPAction header value to be set
     */
    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    /**
     * Artifact type which message should be injected
     * @param injectTo Could be one of "proxy" | "sequence" | "main"
     */
    public void setInjectTo(String injectTo) {
        this.injectTo = injectTo;
    }

    /**
     * Set name of the sequence which message should be injected
     * @param sequenceName sequence name
     */
    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    /**
     * Set name of the proxy service which message should be injected
     * @param proxyName proxy service name
     */
    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
    }

    /**
     * Set a property to be used at runtime. These properties will get inserted in message context created
     * at here
     *
     * @param key    Key of the property
     * @param value  Value of the property
     */
    public void addRuntimeProperty(String key, Object value) {
        if (runtimeProperties.get(key) != null && log.isDebugEnabled()) {
            log.debug("Key :" + key + " will be override.");
        }
        runtimeProperties.put(key, value);
    }

    /**
     * Get a property stored for runtime use
     *
     * @param key   Key of the property to retrieve
     * @return   Value of the property. If property does not exists, this will throw NullPointerException
     */
    public Object getRuntimeProperty(String key) throws NullPointerException {
        return runtimeProperties.get(key);
    }

    /**
     * This will be invoked by the scheduler to inject the message
     * in to the SynapseEnvironment
     */
    public void execute() {

        if (log.isDebugEnabled()) {
            log.debug("execute");
        }

		if (synapseEnvironment == null) {
            handleError("Synapse Environment not set");
            return;
		}

        if(synapseEnvironment.getTaskManager() != null && !synapseEnvironment.getTaskManager().isInitialized()){
            log.warn("Task Manager not initialized. Not executing the cycle");
            return;
        }

		if (message == null && registryKey == null) {
            handleError("message or registry-key not set");
            return;
		}

        if (INJECT_TO_PROXY.equalsIgnoreCase(injectTo)) {

            if (proxyName == null || proxyName.equals("")) {
                handleError("Proxy service name not specified");
            }

            // Prepare axis2 message context
            org.apache.axis2.context.MessageContext axis2MsgCtx =
                    new org.apache.axis2.context.MessageContext();
            ConfigurationContext configurationContext = ((Axis2SynapseEnvironment) synapseEnvironment).
                    getAxis2ConfigurationContext();
            axis2MsgCtx.setConfigurationContext(configurationContext);
            axis2MsgCtx.setIncomingTransportName(Constants.TRANSPORT_LOCAL);
            axis2MsgCtx.setServerSide(true);
            axis2MsgCtx.setMessageID(UIDGenerator.generateURNString());

            try {
                AxisService axisService = configurationContext.getAxisConfiguration().
                        getService(proxyName);
                if (axisService == null) {
                    handleError("Proxy Service: " + proxyName + " not found");
                }
                axis2MsgCtx.setAxisService(axisService);
            } catch (AxisFault axisFault) {
                handleError("Error occurred while attempting to find the Proxy Service");
            }

            if (to != null) {
                axis2MsgCtx.setTo(new EndpointReference(to));
            }

            SOAPEnvelope envelope = null;
            if (format == null) {
                envelope = OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope();
            } else if (SOAP11_FORMAT.equalsIgnoreCase(format)) {
                envelope = OMAbstractFactory.getSOAP11Factory().createSOAPEnvelope();
            } else if (SOAP12_FORMAT.equalsIgnoreCase(format)) {
                envelope = OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope();
            } else if (POX_FORMAT.equalsIgnoreCase(format)) {
                envelope = OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope();
                axis2MsgCtx.setDoingREST(true);
            } else if (GET_FORMAT.equalsIgnoreCase(format)) {
                envelope = OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope();
                axis2MsgCtx.setDoingREST(true);
                axis2MsgCtx.setProperty(Constants.Configuration.HTTP_METHOD,
                        Constants.Configuration.HTTP_METHOD_GET);
            } else {
                handleError("incorrect format specified");
            }

            try {
                PayloadHelper.setXMLPayload(envelope, message.cloneOMElement());
                axis2MsgCtx.setEnvelope(envelope);
            } catch (AxisFault axisFault) {
                handleError("Error in setting the message payload : " + message);
            }

            if (soapAction != null) {
                axis2MsgCtx.setSoapAction(soapAction);
            }

            try {
                if (log.isDebugEnabled()) {
                    log.debug("injecting message to proxy service : " + proxyName);
                }
                AxisEngine.receive(axis2MsgCtx);
            } catch (AxisFault axisFault) {
                handleError("Error occurred while invoking proxy service : " + proxyName);
            }

        } else {
            MessageContext mc = synapseEnvironment.createMessageContext();
            mc.setMessageID(UIDGenerator.generateURNString());
            mc.pushFaultHandler(new MediatorFaultHandler(mc.getFaultSequence()));
            if (to != null) {
                mc.setTo(new EndpointReference(to));
            }

            if (registryKey == null) {
                if (format == null) {
                    PayloadHelper.setXMLPayload(mc, message.cloneOMElement());
                } else {
                    try {
                        if (SOAP11_FORMAT.equalsIgnoreCase(format)) {
                            mc.setEnvelope(OMAbstractFactory.getSOAP11Factory().createSOAPEnvelope());
                        } else if (SOAP12_FORMAT.equalsIgnoreCase(format)) {
                            mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
                        } else if (POX_FORMAT.equalsIgnoreCase(format)) {
                            mc.setDoingPOX(true);
                        } else if (GET_FORMAT.equalsIgnoreCase(format)) {
                            mc.setDoingGET(true);
                        }
                        PayloadHelper.setXMLPayload(mc, message.cloneOMElement());
                    } catch (AxisFault axisFault) {
                        handleError("Error in setting the message payload : " + message);
                    }
                }
            } else {
                Object entry = mc.getEntry(registryKey);
                if (entry == null) {
                    handleError("Key " + registryKey + " not found ");
                }
                String text = "";
                if (entry instanceof OMElement) {
                    OMElement e = (OMElement) entry;
                    removeIndentations(e);
                    text = e.toString();
                } else if (entry instanceof OMText) {
                    text = ((OMText) entry).getText();
                } else if (entry instanceof String) {
                    text = (String) entry;
                }

                OMElement omXML = null;
                try {
                    omXML = AXIOMUtil.stringToOM(text);

                    if (format == null) {
                        PayloadHelper.setXMLPayload(mc, omXML);
                    } else {
                        if (SOAP11_FORMAT.equalsIgnoreCase(format)) {
                            mc.setEnvelope(OMAbstractFactory.getSOAP11Factory().createSOAPEnvelope());
                        } else if (SOAP12_FORMAT.equalsIgnoreCase(format)) {
                            mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
                        } else if (POX_FORMAT.equalsIgnoreCase(format)) {
                            mc.setDoingPOX(true);
                        } else if (GET_FORMAT.equalsIgnoreCase(format)) {
                            mc.setDoingGET(true);
                        }
                        PayloadHelper.setXMLPayload(mc, omXML);
                    }
                } catch (XMLStreamException e) {
                    handleError("Error parsing XML for JSON conversion, please check your property values return valid XML");
                } catch (AxisFault axisFault) {
                    handleError("Error in setting the message payload : " + omXML);
                }
            }

            if (soapAction != null) {
                mc.setSoapAction(soapAction);
            }

            // Adding runtime properties to SynapseMessageContext, if exists
            if (runtimeProperties != null && runtimeProperties.size()>0) {
                for (Map.Entry<String, Object> entry : runtimeProperties.entrySet()) {
                    mc.setProperty(entry.getKey(), entry.getValue());
                }
            }

            if (INJECT_TO_SEQUENCE.equalsIgnoreCase(injectTo)) {
                if (sequenceName == null || sequenceName.equals("")) {
                    handleError("Sequence name not specified");
                }
                SequenceMediator seq = (SequenceMediator) synapseEnvironment.getSynapseConfiguration().
                        getSequence(sequenceName);
                if (seq != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("injecting message to sequence : " + sequenceName);
                    }
                    if (Boolean.parseBoolean(invokeHandlers)) {
                        List handlers = mc.getEnvironment().getSynapseHandlers();
                        Iterator<SynapseHandler> iterator = handlers.iterator();
                        while (iterator.hasNext()) {
                            SynapseHandler handler = iterator.next();
                            if (!handler.handleRequestInFlow(mc)) {
                                return;
                            }
                        }
                    }
                    mc.pushFaultHandler(new MediatorFaultHandler(mc.getFaultSequence()));
                    synapseEnvironment.injectAsync(mc, seq);
                } else {
                    handleError("Sequence: " + sequenceName + " not found");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("injecting message to main sequence");
                }
                if (Boolean.parseBoolean(invokeHandlers)) {
                    List handlers = mc.getEnvironment().getSynapseHandlers();
                    Iterator<SynapseHandler> iterator = handlers.iterator();
                    while (iterator.hasNext()) {
                        SynapseHandler handler = iterator.next();
                        if (!handler.handleRequestInFlow(mc)) {
                            return;
                        }
                    }
                }
                synapseEnvironment.injectMessage(mc);
            }
        }

    }

    /**
     * Destroys the Injector
     */
    public void destroy() {
    }

    /**
     * Log the error and throws a SynapseException
     * @param msg the log message
     */
    private void handleError(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Helper function to remove indentations.
     * @param element
     */
    private void removeIndentations(OMElement element) {
        List<OMText> removables = new ArrayList<OMText>();
        removeIndentations(element, removables);
        for (OMText node : removables) {
            node.detach();
        }
    }

    /**
     * Helper function to remove indentations.
     * @param element
     * @param removables
     */
    private void removeIndentations(OMElement element, List<OMText> removables) {
        Iterator children = element.getChildren();
        while (children.hasNext()) {
            Object next = children.next();
            if (next instanceof OMText) {
                OMText text = (OMText) next;
                if (text.getText().trim().equals("")) {
                    removables.add(text);
                }
            } else if (next instanceof OMElement) {
                removeIndentations((OMElement) next, removables);
            }
        }
    }

}
