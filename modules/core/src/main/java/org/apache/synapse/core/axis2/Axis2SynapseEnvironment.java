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

package org.apache.synapse.core.axis2;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axiom.util.blob.OverflowBlob;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.apache.synapse.FaultHandler;
import org.apache.synapse.SynapseHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.ServerContextInformation;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.statistics.StatisticsCollector;
import org.apache.synapse.carbonext.TenantInfoConfigurator;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseHandlersLoader;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.dispatch.Dispatcher;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.MediatorWorker;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.rest.RESTRequestHandler;
import org.apache.synapse.task.SynapseTaskManager;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.util.concurrent.InboundThreadPool;
import org.apache.synapse.util.concurrent.SynapseThreadPool;
import org.apache.synapse.util.xpath.ext.SynapseXpathFunctionContextProvider;
import org.apache.synapse.util.xpath.ext.SynapseXpathVariableResolver;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * This is the Axis2 implementation of the SynapseEnvironment
 */
public class Axis2SynapseEnvironment implements SynapseEnvironment {

    private static final Log log = LogFactory.getLog(Axis2SynapseEnvironment.class);
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    private SynapseConfiguration synapseConfig;
    private ConfigurationContext configContext;
    private ExecutorService executorService;
    private ExecutorService executorServiceInbound;
    private boolean initialized = false;
    private SynapseTaskManager taskManager;
    private RESTRequestHandler restHandler;
    private List<SynapseHandler> synapseHandlers;
    private long globalTimeout = SynapseConstants.DEFAULT_GLOBAL_TIMEOUT;

    /** The StatisticsCollector object */
    private StatisticsCollector statisticsCollector = new StatisticsCollector();

    private ServerContextInformation contextInformation;

    /** Map containing Xpath Function Context Extensions */
    Map<QName, SynapseXpathFunctionContextProvider> xpathFunctionExtensions =
            new HashMap<QName, SynapseXpathFunctionContextProvider>();

    /** Map containing Xpath Variable Context Extensions */
    Map<QName, SynapseXpathVariableResolver> xpathVariableExtensions =
            new HashMap<QName, SynapseXpathVariableResolver>();

    /** Tenant info configurator */
    TenantInfoConfigurator tenantInfoConfigurator;

    /** Call mediators count */
    private int callMediatorCount = 0;

    /** Continuation is enabled/disabled*/
    private boolean continuation = false;

    /** Unavailable Artifacts referred in the configuration */
    private List<String> unavailableArtifacts = new ArrayList<String>();

    public Axis2SynapseEnvironment(SynapseConfiguration synCfg) {

        int coreThreads = SynapseThreadPool.SYNAPSE_CORE_THREADS;
        int maxThreads  = SynapseThreadPool.SYNAPSE_MAX_THREADS;
        long keepAlive  = SynapseThreadPool.SYNAPSE_KEEP_ALIVE;
        int qLength     = SynapseThreadPool.SYNAPSE_THREAD_QLEN;

        try {
            qLength = Integer.parseInt(synCfg.getProperty(SynapseThreadPool.SYN_THREAD_QLEN));
        } catch (Exception ignore) {}

        try {
            coreThreads = Integer.parseInt(synCfg.getProperty(SynapseThreadPool.SYN_THREAD_CORE));
        } catch (Exception ignore) {}

        try {
            maxThreads = Integer.parseInt(synCfg.getProperty(SynapseThreadPool.SYN_THREAD_MAX));
        } catch (Exception ignore) {}

        try {
            keepAlive = Long.parseLong(synCfg.getProperty(SynapseThreadPool.SYN_THREAD_ALIVE));
        } catch (Exception ignore) {}

        this.executorService = new SynapseThreadPool(coreThreads, maxThreads, keepAlive, qLength,
            synCfg.getProperty(SynapseThreadPool.SYN_THREAD_GROUP,
                SynapseThreadPool.SYNAPSE_THREAD_GROUP),
            synCfg.getProperty(SynapseThreadPool.SYN_THREAD_IDPREFIX,
                SynapseThreadPool.SYNAPSE_THREAD_ID_PREFIX));

		int ibCoreThreads = InboundThreadPool.INBOUND_CORE_THREADS;
		int ibMaxThreads = InboundThreadPool.INBOUND_MAX_THREADS;

		try {
			ibCoreThreads = Integer.parseInt(synCfg.getProperty(InboundThreadPool.IB_THREAD_CORE));
		} catch (Exception ignore) {
		}

		try {
			ibMaxThreads = Integer.parseInt(synCfg.getProperty(InboundThreadPool.IB_THREAD_MAX));
		} catch (Exception ignore) {
		}

		this.executorServiceInbound =
		                              new InboundThreadPool(
		                                                    ibCoreThreads,
		                                                    ibMaxThreads,
		                                                    InboundThreadPool.INBOUND_KEEP_ALIVE,
		                                                    InboundThreadPool.INBOUND_THREAD_QLEN,
		                                                    InboundThreadPool.INBOUND_THREAD_GROUP,
		                                                    InboundThreadPool.INBOUND_THREAD_ID_PREFIX);

        taskManager = new SynapseTaskManager();
        restHandler = new RESTRequestHandler();

        synapseHandlers = SynapseHandlersLoader.loadHandlers();

        this.globalTimeout = SynapseConfigUtils.getGlobalTimeoutInterval();

    }

    public Axis2SynapseEnvironment(ConfigurationContext cfgCtx,
        SynapseConfiguration synapseConfig) {
        this(synapseConfig);
        this.configContext = cfgCtx;
        this.synapseConfig = synapseConfig;
    }

    public Axis2SynapseEnvironment(ConfigurationContext cfgCtx,
        SynapseConfiguration synapseConfig, ServerContextInformation contextInformation) {
        this(cfgCtx, synapseConfig);
        this.contextInformation = contextInformation;
    }

    public boolean injectMessage(final MessageContext synCtx) {
        if (log.isDebugEnabled()) {
            log.debug("Injecting MessageContext");
        }

        //setting transport-in name as a message context property
        TransportInDescription trpInDesc = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getTransportIn();
        if (trpInDesc != null) {
            synCtx.setProperty(SynapseConstants.TRANSPORT_IN_NAME, trpInDesc.getName());
        }


        synCtx.setEnvironment(this);

        if (!invokeHandlers(synCtx)) {
            return false;
        }

        Mediator mandatorySeq = synCtx.getConfiguration().getMandatorySequence();
        // the mandatory sequence is optional and hence check for the existence before mediation
        if (mandatorySeq != null) {

            if (log.isDebugEnabled()) {
                log.debug("Start mediating the message in the " +
                        "pre-mediate state using the mandatory sequence");
            }

            if(!mandatorySeq.mediate(synCtx)) {
                if(log.isDebugEnabled()) {
                    log.debug((synCtx.isResponse() ? "Response" : "Request") + " message for the "
                            + (synCtx.getProperty(SynapseConstants.PROXY_SERVICE) != null ?
                            "proxy service " + synCtx.getProperty(SynapseConstants.PROXY_SERVICE) :
                            "message mediation") + " dropped in the " +
                            "pre-mediation state by the mandatory sequence : \n" + synCtx);
                }
                return false;
            }
        }

        String receivingSequence = (String) synCtx.getProperty(SynapseConstants.RECEIVING_SEQUENCE);
        Boolean isContinuationCall =
                (Boolean) synCtx.getProperty(SynapseConstants.CONTINUATION_CALL);
        // clear the message context properties related to endpoint in last service invocation
        Set keySet = synCtx.getPropertyKeySet();
        if (keySet != null) {
            keySet.remove(SynapseConstants.RECEIVING_SEQUENCE);
            keySet.remove(SynapseConstants.CONTINUATION_CALL);
        }

        if (isContinuationCall != null && isContinuationCall) {
            if (log.isDebugEnabled()) {
                log.debug("Response received for the Continuation Call service invocation");
            }
            return mediateFromContinuationStateStack(synCtx);
        }

        // if this is not a response to a proxy service
        String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
        if (proxyName == null || "".equals(proxyName)) {
            // set default fault handler
            synCtx.pushFaultHandler(new MediatorFaultHandler(synCtx.getFaultSequence()));
            if (receivingSequence != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Using Sequence with name: " + receivingSequence
                            + " for injected message");
                }
                Mediator seqMediator = synCtx.getSequence(receivingSequence);
                if (seqMediator != null) {
                    return seqMediator.mediate(synCtx);
                } else {
                    log.warn("Cannot find a Sequence with name: " + receivingSequence
                            + " for injecting the response message");
                    return false;
                }
            } else {
                boolean processed = restHandler.process(synCtx);
                if (processed) {
                    return true;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Using Main Sequence for injected message");
                }
                return synCtx.getMainSequence().mediate(synCtx);
            }
        }

        ProxyService proxyService = synCtx.getConfiguration().getProxyService(proxyName);
        if (proxyService != null) {
            proxyService.registerFaultHandler(synCtx);

            Mediator outSequence = getProxyOutSequence(synCtx, proxyService);
            if (receivingSequence != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Using Sequence with name: " + receivingSequence
                            + " for injected message");
                }
                Mediator seqMediator = synCtx.getSequence(receivingSequence);
                if (seqMediator != null) {
                    seqMediator.mediate(synCtx);
                } else {
                    log.warn("Cannot find a Sequence with name: " + receivingSequence
                            + " for injecting the message");
                    return false;
                }
            } else if (outSequence != null) {
                outSequence.mediate(synCtx);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(proxyService
                            + " does not specifies an out-sequence - sending the response back");
                }
                Axis2Sender.sendBack(synCtx);
            }
        }
        return true;
    }

    public void injectAsync(final MessageContext synCtx, SequenceMediator seq) {

        if (log.isDebugEnabled()) {
            log.debug("Injecting MessageContext for asynchronous mediation using the : "
                + (seq.getName() == null? "Anonymous" : seq.getName()) + " Sequence");
        }
        synCtx.setEnvironment(this);
        executorService.execute(new MediatorWorker(seq, synCtx));
    }

    /**
     * 
     * Used by inbound polling endpoints to inject the message to synapse engine
     *
     * @param synCtx message context
     * @param sequential whether message should be injected in sequential manner
     *                   without spawning new threads
     * @return Boolean - Indicate if were able to inject the message
     * @throws SynapseException
     *             - in case error occured during the mediation
     * 
     */
    public boolean injectInbound(final MessageContext synCtx, SequenceMediator seq,
            boolean sequential) throws SynapseException {

        if (log.isDebugEnabled()) {
            log.debug("Injecting MessageContext for inbound mediation using the : "
                    + (seq.getName() == null ? "Anonymous" : seq.getName()) + " Sequence");
        }

        synCtx.setEnvironment(this);
        if (!invokeHandlers(synCtx)) {
            return false;
        }

        if (!sequential) {
            try {
                executorServiceInbound.execute(new MediatorWorker(seq, synCtx));
                return true;
            } catch (RejectedExecutionException re) {
                // If the pool is full complete the execution with the same thread
                log.warn("Inbound worker pool has reached the maximum capacity and will be processing current message sequentially.");
            }
        }
        
        // Following code is reached if the sequential==true or inbound is
        // reached max level
        try {
            seq.mediate(synCtx);
            return true;
        } catch (SynapseException syne) {
            if (!synCtx.getFaultStack().isEmpty()) {
                log.warn("Executing fault handler due to exception encountered");
                ((FaultHandler) synCtx.getFaultStack().pop()).handleFault(synCtx, syne);
                return true;
            } else {
                log.warn("Exception encountered but no fault handler found - message dropped");
                throw syne;
            }
        } catch (Exception e) {
            String msg = "Unexpected error executing task/async inject";
            log.error(msg, e);
            if (synCtx.getServiceLog() != null) {
                synCtx.getServiceLog().error(msg, e);
            }
            if (!synCtx.getFaultStack().isEmpty()) {
                log.warn("Executing fault handler due to exception encountered");
                ((FaultHandler) synCtx.getFaultStack().pop()).handleFault(synCtx, e);
                return true;
            } else {
                log.warn("Exception encountered but no fault handler found - message dropped");
                throw new SynapseException(
                                           "Exception encountered but no fault handler found - message dropped",
                                           e);
            }
        } catch (Throwable e) {
            String msg = "Unexpected error executing inbound/async inject, message dropped";
            log.error(msg, e);
            if (synCtx.getServiceLog() != null) {
                synCtx.getServiceLog().error(msg, e);
            }
            throw new SynapseException(msg, e);
        }
    }
    
    /**
     * This will be used for sending the message provided, to the endpoint specified by the
     * EndpointDefinition using the axis2 environment.
     *
     * @param endpoint - EndpointDefinition to be used to find the endpoint information
     *                      and the properties of the sending process
     * @param synCtx   - Synapse MessageContext to be sent
     */
    public void send(EndpointDefinition endpoint, MessageContext synCtx) {

        //removing rampart engaged property, else outgoing security will not work
        ((Axis2MessageContext) synCtx).getAxis2MessageContext().
                removeProperty("rampart_engaged");

        if (synCtx.isResponse()) {
            if (endpoint != null) {
                if (isTransportSwitching(synCtx, endpoint)) {
                    buildMessage(synCtx);
                }
                Axis2Sender.sendOn(endpoint, synCtx);
            } else {
                String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
                boolean serviceModuleEngaged = false;
                if (proxyName != null) {
                    ProxyService proxyService = synapseConfig.getProxyService(proxyName);
                    serviceModuleEngaged = proxyService.isModuleEngaged();
                }

                if (serviceModuleEngaged || isTransportSwitching(synCtx, null)) {
                    buildMessage(synCtx);
                }
                
                //Build message in the case of inbound jms dual channel
                Boolean isInboundJMS = (Boolean)synCtx.getProperty(SynapseConstants.INBOUND_JMS_PROTOCOL);
                if (isInboundJMS != null && isInboundJMS) {
                    buildMessage(synCtx);
                }
                
                Axis2Sender.sendBack(synCtx);
            }
        } else {
            // If this request is related to session affinity endpoints - For client initiated session
            Dispatcher dispatcher =
                    (Dispatcher) synCtx.getProperty(
                            SynapseConstants.PROP_SAL_ENDPOINT_CURRENT_DISPATCHER);
            if (dispatcher != null) {
                if (!dispatcher.isServerInitiatedSession()) {
                    dispatcher.updateSession(synCtx);
                }
            }

            synCtx.setProperty(SynapseConstants.SENDING_REQUEST, true);
            if (endpoint == null || isTransportSwitching(synCtx, endpoint)) {
                buildMessage(synCtx);
            }
            Axis2Sender.sendOn(endpoint, synCtx);
        }
    }

    /**
     * This method will be used to create a new MessageContext in the Axis2 environment for
     * Synapse. This will set all the relevant parts to the MessageContext, but for this message
     * context to be useful creator has to fill in the data like envelope and operation context
     * and so on. This will set a default envelope of type soap12 and a new messageID for the
     * created message along with the ConfigurationContext is being set in to the message
     * correctly.
     *
     * @return Synapse MessageContext with the underlying axis2 message context set
     */
    public MessageContext createMessageContext() {

        if (log.isDebugEnabled()) {
            log.debug("Creating Message Context");
        }

        org.apache.axis2.context.MessageContext axis2MC
                = new org.apache.axis2.context.MessageContext();
        axis2MC.setConfigurationContext(this.configContext);

        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MC.setServiceContext(svcCtx);
        axis2MC.setOperationContext(opCtx);
        MessageContext mc = new Axis2MessageContext(axis2MC, synapseConfig, this);
        mc.setMessageID(UIDGenerator.generateURNString());
        try {
			mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
			mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
		} catch (Exception e) {
            handleException("Unable to attach the SOAP envelope to " +
                    "the created new message context", e);
        }

        return mc;
    }

    /**
     * Factory method to create the TemporaryData object as per on the parameters specified in the
     * synapse.properties file, so that the TemporaryData parameters like threshold chunk size
     * can be customized by using the properties file. This can be extended to enforce further
     * policies if required in the future.
     *
     * @return created TemporaryData object as per in the synapse.properties file
     */
    public OverflowBlob createOverflowBlob() {

        String chkSize = synapseConfig.getProperty(SynapseConstants.CHUNK_SIZE);
        String chunkNumber = synapseConfig.getProperty(SynapseConstants.THRESHOLD_CHUNKS);
        int numberOfChunks = SynapseConstants.DEFAULT_THRESHOLD_CHUNKS;
        int chunkSize = SynapseConstants.DEFAULT_CHUNK_SIZE;

        if (chkSize != null) {
            chunkSize = Integer.parseInt(chkSize);
        }

        if (chunkNumber != null) {
            numberOfChunks = Integer.parseInt(chunkNumber);
        }

        String tempPrefix = synapseConfig.getProperty(SynapseConstants.TEMP_FILE_PREFIX,
                SynapseConstants.DEFAULT_TEMPFILE_PREFIX);
        String tempSuffix = synapseConfig.getProperty(SynapseConstants.TEMP_FILE_SUFIX,
                SynapseConstants.DEFAULT_TEMPFILE_SUFIX);

        return new OverflowBlob(numberOfChunks, chunkSize, tempPrefix, tempSuffix);
    }

    /**
     * This method returns the <code>StatisticsCollector</code> responsible for
     * collecting stats for this synapse instance.
     *
     * @return Returns the <code>StatisticsCollector</code>
     */
    public StatisticsCollector getStatisticsCollector() {
        return statisticsCollector;
    }

    /**
     * To set the StatisticsCollector
     *
     * @param collector - Statistics collector to be set
     */
    @Deprecated
    public void setStatisticsCollector(StatisticsCollector collector) {
        this.statisticsCollector = collector;
    }

    /**
     * This will give the access to the synapse thread pool for the
     * advanced mediation tasks.
     *
     * @return an ExecutorService to execute the tasks in a new thread from the pool
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Has this environment properly initialized?
     *
     * @return true if ready for processing
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Mark this environment as ready for processing
     *
     * @param state true means ready for processing
     */
    public void setInitialized(boolean state) {
        this.initialized = state;
    }

    /**
     * Retrieves the {@link SynapseConfiguration} from the <code>environment</code>
     *
     * @return synapseConfig associated with the environment
     */
    public SynapseConfiguration getSynapseConfiguration() {
        return this.synapseConfig;
    }

    /**
     * Retrieve the {@link org.apache.synapse.task.SynapseTaskManager} from the
     * <code>environment</code>.
     *
     * @return SynapseTaskManager of this synapse environment
     */
    public SynapseTaskManager getTaskManager() {
        return this.taskManager;
    }

    /**
     * Retrieve the {@link org.apache.synapse.ServerContextInformation} from the <code>environment.
     * 
     * @return ServerContextInformation of the environment
     */
    public ServerContextInformation getServerContextInformation() {
        return contextInformation;
    }

    /**
     * Retrieves the {@link ConfigurationContext} associated with this <code>axis2SynapseEnv</code>
     *
     * @return configContext of the axis2 synapse environment
     */
    public ConfigurationContext getAxis2ConfigurationContext() {
        return this.configContext;
    }

    /**
     * Returns all declared xpath Function Extensions
     * @return Hash Map Containing Function Extensions with supported QName keys
     */
    public Map<QName, SynapseXpathFunctionContextProvider> getXpathFunctionExtensions() {
        return xpathFunctionExtensions;
    }

    /**
     * Returns all declared xpath Variable Extensions
     * @return Hash Map Containing Variable Extensions with supported QName keys
     */
    public Map<QName, SynapseXpathVariableResolver> getXpathVariableExtensions() {
        return xpathVariableExtensions;
    }

    public TenantInfoConfigurator getTenantInfoConfigurator() {
        return tenantInfoConfigurator;
    }

    public void setXpathFunctionExtensions(SynapseXpathFunctionContextProvider functionExt){
         if(functionExt!=null) {
             xpathFunctionExtensions.put(functionExt.getResolvingQName(), functionExt);
         }
    }


    public void setXpathVariableExtensions(SynapseXpathVariableResolver variableExt){
         if(variableExt!=null) {
             xpathVariableExtensions.put(variableExt.getResolvingQName(), variableExt);
         }
    }

    public void setTenantInfoConfigurator(TenantInfoConfigurator configurator) {
        if (configurator != null) {
            tenantInfoConfigurator = configurator;
        }
    }

     /**
     * When request is sent using a Call Mediator, mediate the response message using the
     * ContinuationState Stack
     * @param synCtx MessageContext
     * @return whether mediation is completed
     */
    private boolean mediateFromContinuationStateStack(MessageContext synCtx) {

        if (log.isDebugEnabled()) {
            log.debug("Mediating response using the ContinuationStateStack");
        }

        if (synCtx.getContinuationStateStack().isEmpty()) {
            // ideally this should never happens
            log.warn("ContinuationStateStack empty. No ContinuationState to mediate the response ");
            return false;
        }

        boolean result;
        do {
            SeqContinuationState seqContinuationState =
                    (SeqContinuationState) synCtx.getContinuationStateStack().peek();
            result = ContinuationStackManager.retrieveSequence(synCtx, seqContinuationState).
                    mediate(synCtx, seqContinuationState);
        } while (result && !synCtx.getContinuationStateStack().isEmpty());

        return result;
    }

    private boolean isTransportSwitching(MessageContext synCtx, EndpointDefinition endpoint) {

        if (endpoint != null) {
            // SendOn transport switching
            if (endpoint.getAddress() != null) {
                // If the message is sent to an explicit non-HTTP endpoint, build the message
                return !endpoint.getAddress().startsWith("http");
            } else {
                String address = synCtx.getTo().getAddress();
                if (address != null) {
                    // If the message is sent to an implicit non-HTTP endpoint, build the message
                    return !address.startsWith("http");
                }
            }
        } else {
            // SendBack transport switching
            TransportOutDescription transportOut =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext().getTransportOut();
            if (transportOut != null) {
                String transportOutName = transportOut.getName();
                if (transportOutName != null) {
                    return !transportOutName.startsWith("http");
                }
            }
        }
        return false;
    }

    private void buildMessage(MessageContext synCtx) {
        try {
            RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext(), false);
        } catch (Exception e) {
            handleException("Error while building message", e);
        }
    }

    private void handleException(String message, Throwable e) {
        log.error(message, e);
        throw new SynapseException(message, e);
    }

    /**
     * Helper method to determine out sequence of the proxy service
     *
     * @param synCtx       Current Message
     * @param proxyService Proxy Service
     * @return Out Sequence of the given proxy service, if there are any, otherwise null
     */
    private Mediator getProxyOutSequence(MessageContext synCtx, ProxyService proxyService) {
        //TODO is it meaningful  to move this method into proxy service or
        //TODO a class that Strategically detects out sequence  ?
        String sequenceName = proxyService.getTargetOutSequence();
        if (sequenceName != null && !"".equals(sequenceName)) {
            Mediator outSequence = synCtx.getSequence(sequenceName);
            if (outSequence != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Using the sequence named " + sequenceName
                            + " for the outgoing message mediation of the proxy service "
                            + proxyService);
                }
                return outSequence;
            } else {
                log.error("Unable to find the out-sequence " +
                        "specified by the name " + sequenceName);
                throw new SynapseException("Unable to find the " +
                        "out-sequence specified by the name " + sequenceName);
            }
        } else {
            Mediator outSequence = proxyService.getTargetInLineOutSequence();
            if (outSequence != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Using the anonymous out-sequence specified in the proxy service "
                            + proxyService
                            + " for outgoing message mediation");
                }
                return outSequence;
            }
        }
        return null;
    }

    /**
     * Increment/Decrement the Call mediator count in the environment by 1.
     * While Call Mediator count is updated, continuation is enabled/disabled based on the count.
     * If Call Mediator count becomes 0 continuation is disabled.
     * count cannot get negative values.
     *
     * @param isIncrement whether to increment the count
     */
    public synchronized void updateCallMediatorCount(boolean isIncrement) {
        if (isIncrement) {
            callMediatorCount++;
            continuation = true;
        } else {
            callMediatorCount--;
            if (callMediatorCount == 0) {
                continuation = false;
            }
        }
    }

    /**
     * Whether continuation is enabled in the environment.
     * ContinuationState stack operations are done only if continuation is enabled for the
     * environment.
     *
     * @return whether continuation is enabled in the environment
     */
    public boolean isContinuationEnabled() {
        return continuation;
    }

    /**
     * Add an artifact reference not available in the environment.
     * This stored reference will get cleared when artifact becomes available
     *
     * @param key artifact reference key
     */
    public synchronized void addUnavailableArtifactRef(String key) {
        unavailableArtifacts.add(key);
        updateCallMediatorCount(true);
    }

    /**
     * Remove the artifact reference which is previously marked as unavailable in environment
     * from the unavailable list
     *
     * @param key artifact reference key
     */
    public synchronized void removeUnavailableArtifactRef(String key) {
        if (unavailableArtifacts.contains(key)) {
            unavailableArtifacts.remove(key);
            updateCallMediatorCount(false);
        }
    }

    /**
     * Clear unavailability of an artifact if it is
     * previously marked as unavailable in the environment
     *
     * @param key artifact reference key
     */
    public synchronized void clearUnavailabilityOfArtifact(String key) {
        if (!unavailableArtifacts.contains(key)) {
           return;
        }

        for (Iterator<String> itr = unavailableArtifacts.iterator(); itr.hasNext(); ) {
            if (key.equals(itr.next())) {
                itr.remove();
                updateCallMediatorCount(false);
            }
        }
    }

    /**
     * Get all synapse handlers
     *
     * @return list of synapse handlers
     */
    public List<SynapseHandler> getSynapseHandlers() {
        return synapseHandlers;
    }

    /**
     * Register a synapse handler to the synapse environment
     *
     * @param handler synapse handler
     */
    public void registerSynapseHandler(SynapseHandler handler) {
        synapseHandlers.add(handler);
    }

    @Override
    public long getGlobalTimeout() {
        return globalTimeout;
    }

    public boolean injectMessage(MessageContext smc, SequenceMediator seq) {
        if (seq == null) {
            log.error("Please provide existing sequence");
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("Injecting MessageContext for asynchronous mediation using the : "
                    + (seq.getName() == null? "Anonymous" : seq.getName()) + " Sequence");
        }
        smc.setEnvironment(this);
        if (!invokeHandlers(smc)) {
            return false;
        }
        try {
            seq.mediate(smc);
            return true;
        } catch (SynapseException syne) {
            if (!smc.getFaultStack().isEmpty()) {
                warn(false, "Executing fault handler due to exception encountered", smc);
                smc.getFaultStack().pop().handleFault(smc, syne);

            } else {
                warn(false, "Exception encountered but no fault handler found - " +
                        "message dropped", smc);
            }
            return false;
        } catch (Exception e) {
            String msg = "Unexpected error executing  injecting message to sequence ," + seq;
            log.error(msg, e);
            if (smc.getServiceLog() != null) {
                smc.getServiceLog().error(msg, e);
            }
            if (!smc.getFaultStack().isEmpty()) {
                warn(false, "Executing fault handler due to exception encountered", smc);
                smc.getFaultStack().pop().handleFault(smc, e);

            } else {
                warn(false, "Exception encountered but no fault handler found - " +
                        "message dropped", smc);
            }
            return false;
        } catch (Throwable e) {
            String msg = "Unexpected error executing  injecting message to sequence ," + seq + " message dropped";
            log.error(msg, e);
            if (smc.getServiceLog() != null) {
                smc.getServiceLog().error(msg, e);
            }
            return false;
        }
    }

    private void warn(boolean traceOn, String msg, MessageContext msgContext) {
        if (traceOn) {
            trace.warn(msg);
        }
        log.warn(msg);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().warn(msg);
        }
    }


    /**
     * Invoke Synapse Handlers
     *
     * @param synCtx synapse message context
     * @return whether flow should continue further
     */
    private boolean invokeHandlers(MessageContext synCtx) {

        Iterator<SynapseHandler> iterator =
                synCtx.getEnvironment().getSynapseHandlers().iterator();

        if (iterator.hasNext()) {

            Boolean isContinuationCall =
                    (Boolean) synCtx.getProperty(SynapseConstants.CONTINUATION_CALL);

            if (synCtx.isResponse() || (isContinuationCall != null && isContinuationCall)) {
                while (iterator.hasNext()) {
                    SynapseHandler handler = iterator.next();
                    if (!handler.handleResponseInFlow(synCtx)) {
                        return false;
                    }
                }
            } else {
                while (iterator.hasNext()) {
                    SynapseHandler handler = iterator.next();
                    if (!handler.handleRequestInFlow(synCtx)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
