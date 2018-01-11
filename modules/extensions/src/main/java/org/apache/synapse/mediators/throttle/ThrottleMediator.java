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

package org.apache.synapse.mediators.throttle;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.*;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.ReliantContinuationState;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.debug.constructs.EnclosedInlinedSequence;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.commons.throttle.core.AccessRateController;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessController;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessReplicator;
import org.apache.synapse.commons.throttle.core.Throttle;
import org.apache.synapse.commons.throttle.core.ThrottleConstants;
import org.apache.synapse.commons.throttle.core.ThrottleDataHolder;
import org.apache.synapse.commons.throttle.core.ThrottleConfiguration;
import org.apache.synapse.commons.throttle.core.ThrottleException;
import org.apache.synapse.commons.throttle.core.ThrottleContext;
import org.apache.synapse.commons.throttle.core.AccessInformation;
import org.apache.synapse.commons.throttle.core.ThrottleFactory;

/**
 * The Mediator for the throttling - Throttling will occur according to the ws-policy
 * which is specified as the key for lookup from the registry or the inline policy
 * Throttling can be applied on IP or Domain of the remote caller.Now has support for either
 * Distributed or Standalone throttling schemes. Support two modes of throttling Concurrency and
 * Access rate based throttling In concurrency based throttling - throttling ignores remote callers
 * IP or Domains Considers only concurrent message flows at a given time. Access rate based
 * throttling is bound to a particular remote caller.
 */

public class ThrottleMediator extends AbstractMediator implements ManagedLifecycle,
        FlowContinuableMediator, EnclosedInlinedSequence {

    /* The key for getting the throttling policy - key refers to a/an [registry] entry */
    private String policyKey = null;
    /* InLine policy object - XML  */
    private OMElement inLinePolicy = null;
    /* The reference to the sequence which will execute when access is denied   */
    private String onRejectSeqKey = null;
    /* The in-line sequence which will execute when access is denied */
    private Mediator onRejectMediator = null;
    /* The reference to the sequence which will execute when access is allowed  */
    private String onAcceptSeqKey = null;
    /* The in-line sequence which will execute when access is allowed */
    private Mediator onAcceptMediator = null;
    /* The concurrent access control group id */
    private String id;
    /* Access rate controller - limit the remote caller access*/
    private AccessRateController accessControler;
    /* ConcurrentAccessController - limit the remote callers concurrent access */
    private ConcurrentAccessController concurrentAccessController = null;
    /* Replicates the concurrent access of remote across the cluster */
    private ConcurrentAccessReplicator concurrentAccessReplicator;
    /* Configuration where Throttle data is being kept */
    private ConfigurationContext configContext;
    /*Throttle Data is kept inside this holder eg :- Caller contexts */
    private ThrottleDataHolder dataHolder;
    /* The property key that used when the ConcurrentAccessController
    look up from ThrottleDataHolder */
    private String key;
    /* Is this env. support clustering*/
    private boolean isClusteringEnable = false;
    /* The Throttle object - holds all runtime and configuration data */
    private Throttle throttle;
    /* Lock used to ensure thread-safe creation of the throttle
    when throttle is created dynamically */
    private final Object throttleLock = new Object();
    /* Last version of dynamic policy resource*/
    private long version;

    public ThrottleMediator() {
    }

    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);
        boolean isResponse = synCtx.isResponse();
        boolean canAccess = true;
        if (!isResponse) {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Start : Throttle mediator");
                if (synLog.isTraceTraceEnabled()) {
                    synLog.traceTrace("Message : " + synCtx.getEnvelope());
                }
            }
            //we consider dynamic loading of policy loading only for the request flow mediation
            //we ignore policy initialization for the response flow case we use the existing policy
            //reference throttling only applies for request flow mediation
            doInitializeThrottleDynamicPolicy(synCtx, synLog);

            //in cluster environment local reference to concurrent access controller should be
            //updated, local reference kept inside Throttle mediator maybe expired as a
            //consequence of global concurrent access change with
            //respect to controller we maintain at configuration context
            if (isClusteringEnable) {
                reloadDistributedConcurrentAccessController(synLog);
            }
            //throttle by concurrency is given the highest priority, if another throttling
            // scheme fails eg :- rate based from this point onward Eg:- access rate
            // checks we need to rollback the previous
            if (concurrentAccessController != null) {
                canAccess = doThrottleByConcurrency(isResponse, synLog);
            }


            //if the access is success through concurrency throttle and if this is a request message
            //then do access rate based throttling
            if (throttle != null && !isResponse && canAccess) {
                org.apache.axis2.context.MessageContext axisMC =
                        ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                canAccess = doThrottleByAccessRate(synCtx, axisMC, configContext, synLog);
            }
            // all the replication functionality of the access rate and concurrency based
            // throttling handles by itself at throttle core level but for the the concurrency case
            // this is done explicitly forcing as we make concurrent global state change available
            // to other cluster nodes as synchronous manner
            if (isClusteringEnable && concurrentAccessController != null) {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Going to replicates the  " +
                            "states of the ConcurrentAccessController with key : " + key);
                }
                concurrentAccessReplicator.replicate(key, concurrentAccessController);
            }

            if (concurrentAccessController != null) {
                //maintain properties in message context for the concurrency throttling
                synCtx.setProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE, true);
                //maintain properties in message context for the concurrency throttling
                synCtx.setProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE_KEY, key);
                //maintain properties in message context for the concurrency throttling
                synCtx.setProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_CONTROLLER,
                        concurrentAccessController);
            }
            //maintain properties in message context for the concurrency throttling
            if (isClusteringEnable) {
                synCtx.setProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_REPLICATOR,
                        concurrentAccessReplicator);
            }
            //depending on the subsequent throttling controller outcome from this point
            // onwards mediation flow branches to either accept sequence or reject sequence
            if (canAccess) {
                //accept case
                // property is set to identify whether a request is accepted or rejected looking at
                // the throttling controller outcome.
                synCtx.setProperty(SynapseConstants.SYNAPSE_IS_CONCURRENT_ACCESS_ALLOWED, true);
                if (onAcceptSeqKey != null) {
                    Mediator mediator = synCtx.getSequence(onAcceptSeqKey);
                    if (mediator != null) {
                        ContinuationStackManager.updateSeqContinuationState(synCtx,
                                getMediatorPosition());
                        return mediator.mediate(synCtx);
                    } else {
                        handleException("Unable to find onAccept sequence with key : "
                                + onAcceptSeqKey, synCtx);
                    }
                } else if (onAcceptMediator != null) {
                    ContinuationStackManager.addReliantContinuationState(synCtx, 0,
                            getMediatorPosition());
                    boolean result = onAcceptMediator.mediate(synCtx);
                    if (result) {
                        ContinuationStackManager.removeReliantContinuationState(synCtx);
                    }
                    return result;
                } else {
                    return true;
                }

            } else {
                //reject case
                // property is set to identify whether a request is accepted or rejected looking at
                // the throttling controller outcome.
                synCtx.setProperty(SynapseConstants.SYNAPSE_IS_CONCURRENT_ACCESS_ALLOWED, false);
                if (onRejectSeqKey != null) {
                    Mediator mediator = synCtx.getSequence(onRejectSeqKey);
                    if (mediator != null) {
                        ContinuationStackManager.updateSeqContinuationState(synCtx,
                                getMediatorPosition());
                        return mediator.mediate(synCtx);
                    } else {
                        handleException("Unable to find onReject sequence with key : "
                                + onRejectSeqKey, synCtx);
                    }
                } else if (onRejectMediator != null) {
                    ContinuationStackManager.addReliantContinuationState(synCtx, 1,
                            getMediatorPosition());
                    boolean result = onRejectMediator.mediate(synCtx);
                    if (result) {
                        ContinuationStackManager.removeReliantContinuationState(synCtx);
                    }
                    return result;
                } else {
                    return false;
                }
            }
        }

        synLog.traceOrDebug("End : Throttle mediator");
        return canAccess;
    }

    public boolean mediate(MessageContext synCtx, ContinuationState continuationState) {
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Throttle mediator : Mediating from ContinuationState");
        }

        boolean result;
        boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
        int subBranch = ((ReliantContinuationState) continuationState).getSubBranch();
        if (subBranch == 0) {
            if (!continuationState.hasChild()) {
                result = ((SequenceMediator) onAcceptMediator).
                        mediate(synCtx, continuationState.getPosition() + 1);
            } else {
                FlowContinuableMediator mediator =
                        (FlowContinuableMediator) ((SequenceMediator) onAcceptMediator).
                                getChild(continuationState.getPosition());

                result = mediator.mediate(synCtx, continuationState.getChildContState());

                if (isStatisticsEnabled) {
                    ((Mediator) mediator).reportCloseStatistics(synCtx, null);
                }
            }
            if (isStatisticsEnabled) {
                onAcceptMediator.reportCloseStatistics(synCtx, null);
            }
        } else {
            if (!continuationState.hasChild()) {
                result = ((SequenceMediator) onRejectMediator).
                        mediate(synCtx, continuationState.getPosition() + 1);
            } else {
                FlowContinuableMediator mediator =
                        (FlowContinuableMediator) ((SequenceMediator) onRejectMediator).getChild(
                                continuationState.getPosition());

                result = mediator.mediate(synCtx, continuationState.getChildContState());

                if (isStatisticsEnabled) {
                    ((Mediator) mediator).reportCloseStatistics(synCtx, null);
                }
            }
            if (isStatisticsEnabled) {
                onRejectMediator.reportCloseStatistics(synCtx, null);
            }
        }

        return result;
    }

    /**
     * Helper method that handles the concurrent access through throttle
     *
     * @param isResponse Current Message is response or not
     * @param synLog     the Synapse log to use
     * @return true if the caller can access ,o.w. false
     */
    private boolean doThrottleByConcurrency(boolean isResponse, SynapseLog synLog) {
        boolean canAccess = true;
        if (concurrentAccessController != null) {
            // do the concurrency throttling
            int concurrentLimit = concurrentAccessController.getLimit();
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Concurrent access controller for ID : " + id +
                        " allows : " + concurrentLimit + " concurrent accesses");
            }
            int available;
            if (!isResponse) {
                available = concurrentAccessController.getAndDecrement();
                canAccess = available > 0;
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Concurrency Throttle : Access " +
                            (canAccess ? "allowed" : "denied") + " :: " + available
                            + " of available of " + concurrentLimit + " connections");
                }
            }
        }
        return canAccess;
    }

    /**
     * Helper method that handles the access-rate based throttling
     *
     * @param synCtx MessageContext(Synapse)
     * @param axisMC MessageContext(Axis2)
     * @param cc     ConfigurationContext
     * @param synLog the Synapse log to use
     * @return ue if the caller can access ,o.w. false
     */
    private boolean doThrottleByAccessRate(MessageContext synCtx,
                                           org.apache.axis2.context.MessageContext axisMC,
                                           ConfigurationContext cc,
                                           SynapseLog synLog) {

        String callerId = null;
        boolean canAccess = true;
        //remote ip of the caller
        String remoteIP = (String) axisMC.getPropertyNonReplicable(
                org.apache.axis2.context.MessageContext.REMOTE_ADDR);
        //domain name of the caller
        String domainName = (String) axisMC.getPropertyNonReplicable(NhttpConstants.REMOTE_HOST);
        //Using remote caller domain name , If there is a throttle configuration for
        // this domain name ,then throttling will occur according to that configuration
        if (domainName != null) {
            // do the domain based throttling
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("The Domain Name of the caller is :" + domainName);
            }
            // loads the DomainBasedThrottleContext
            ThrottleContext context
                    = throttle.getThrottleContext(ThrottleConstants.DOMAIN_BASED_THROTTLE_KEY);
            if (context != null) {
                //loads the DomainBasedThrottleConfiguration
                ThrottleConfiguration config = context.getThrottleConfiguration();
                if (config != null) {
                    //checks the availability of a policy configuration for  this domain name
                    callerId = config.getConfigurationKeyOfCaller(domainName);
                    if (callerId != null) {  // there is configuration for this domain name
                        //If this is a clustered env.
                        if (isClusteringEnable) {
                            context.setConfigurationContext(cc);
                            context.setThrottleId(id);
                        }

                        try {
                            //Checks for access state
                            AccessInformation accessInformation = accessControler.canAccess(context,
                                    callerId, ThrottleConstants.DOMAIN_BASE);
                            canAccess = accessInformation.isAccessAllowed();

                            if (synLog.isTraceOrDebugEnabled()) {
                                synLog.traceOrDebug("Access " + (canAccess ? "allowed" : "denied")
                                        + " for Domain Name : " + domainName);
                            }

                            //In the case of both of concurrency throttling and
                            //rate based throttling have enabled ,
                            //if the access rate less than maximum concurrent access ,
                            //then it is possible to occur death situation.To avoid that reset,
                            //if the access has denied by rate based throttling
                            if (!canAccess && concurrentAccessController != null) {
                                concurrentAccessController.incrementAndGet();
                                if (isClusteringEnable) {
                                    dataHolder.setConcurrentAccessController
                                            (key, concurrentAccessController);
                                }
                            }
                        } catch (ThrottleException e) {
                            handleException("Error occurred during throttling", e, synCtx);
                        }
                    }
                }
            }
        } else {
            synLog.traceOrDebug("The Domain name of the caller cannot be found");
        }

        //At this point , any configuration for the remote caller hasn't found ,
        //therefore trying to find a configuration policy based on remote caller ip
        if (callerId == null) {
            //do the IP-based throttling
            if (remoteIP == null) {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("The IP address of the caller cannot be found");
                }
                canAccess = true;

            } else {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("The IP Address of the caller is :" + remoteIP);
                }
                try {
                    // Loads the IPBasedThrottleContext
                    ThrottleContext context =
                            throttle.getThrottleContext(ThrottleConstants.IP_BASED_THROTTLE_KEY);
                    if (context != null) {
                        //Loads the IPBasedThrottleConfiguration
                        ThrottleConfiguration config = context.getThrottleConfiguration();
                        if (config != null) {
                            //Checks the availability of a policy configuration for  this ip
                            callerId = config.getConfigurationKeyOfCaller(remoteIP);
                            if (callerId != null) {   // there is configuration for this ip
                                //For clustered env.
                                if (isClusteringEnable) {
                                    context.setConfigurationContext(cc);
                                    context.setThrottleId(id);
                                }

                                //Checks access state
                                AccessInformation accessInformation = accessControler.canAccess(
                                        context,
                                        callerId,
                                        ThrottleConstants.IP_BASE);

                                canAccess = accessInformation.isAccessAllowed();
                                if (synLog.isTraceOrDebugEnabled()) {
                                    synLog.traceOrDebug("Access " +
                                            (canAccess ? "allowed" : "denied")
                                            + " for IP : " + remoteIP);
                                }
                                //In the case of both of concurrency throttling and
                                //rate based throttling have enabled ,
                                //if the access rate less than maximum concurrent access ,
                                //then it is possible to occur death situation.To avoid that reset,
                                //if the access has denied by rate based throttling
                                if (!canAccess && concurrentAccessController != null) {
                                    concurrentAccessController.incrementAndGet();
                                    if (isClusteringEnable) {
                                        dataHolder.setConcurrentAccessController
                                                (key, concurrentAccessController);
                                    }
                                }
                            }
                        }
                    }
                } catch (ThrottleException e) {
                    handleException("Error occurred during throttling", e, synCtx);
                }
            }
        }
        return canAccess;
    }

    /**
     * Helper method that handles dynamic policy initialization
     *
     * @param synCtx MessageContext(Synapse)
     */
    private void doInitializeThrottleDynamicPolicy(MessageContext synCtx, SynapseLog synLog) {

        if (policyKey == null) {
            return;
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Throttle mediator : Initializing dynamic Policy");
        }

        // If the policy has specified as a registry key.
        // load or re-load policy from registry or local entry if not already available

        Entry entry = synCtx.getConfiguration().getEntryDefinition(policyKey);
        if (entry == null) {
            handleException("Cannot find throttling policy using key : "
                    + policyKey, synCtx);

        } else {

            boolean reCreate = false;
            // if the key refers to a dynamic resource
            if (entry.isDynamic()) {
                if ((!entry.isCached() || entry.isExpired()) && version != entry.getVersion()) {
                    reCreate = true;
                    version = entry.getVersion();
                }
            }
            //we ignore the static initialization case
            if (reCreate || throttle == null) {
                Object entryValue = synCtx.getEntry(policyKey);
                if (entryValue == null) {
                    handleException(
                            "Null throttling policy returned by Entry : "
                                    + policyKey, synCtx);

                } else {
                    if (!(entryValue instanceof OMElement)) {
                        handleException("Policy returned from key : " + policyKey +
                                " is not an OMElement", synCtx);

                    } else {
                        //Check for reload in a cluster environment
                        // For clustered environment ,if the concurrent access controller
                        // is not null and throttle is not null , then must reload.
                        if (isClusteringEnable && concurrentAccessController != null
                                && throttle != null) {
                            concurrentAccessController = null; // set null ,
                            // because need to reload when throttle gets created again
                        }

                        try {
                            // creation of throttle should be thread safe if multiple request flow
                            // try to create a throttle object
                            synchronized (throttleLock) {
                                // Creates the throttle from the policy
                                throttle = ThrottleFactory.createMediatorThrottle(
                                        PolicyEngine.getPolicy((OMElement) entryValue));

                                //For non-clustered  environment , must re-initiates
                                //For  clustered  environment,
                                //concurrent access controller is null ,
                                //then must re-initiates
                                if (throttle != null && (concurrentAccessController == null
                                        || !isClusteringEnable)) {
                                    concurrentAccessController =
                                            throttle.getConcurrentAccessController();
                                    if (concurrentAccessController != null) {
                                        dataHolder.setConcurrentAccessController
                                                (key, concurrentAccessController);
                                    } else {
                                        dataHolder.removeConcurrentAccessController(key);
                                    }
                                }
                            }
                        } catch (ThrottleException e) {
                            handleException("Error processing the throttling policy",
                                    e, synCtx);
                        }
                    }
                }
            }
        }

    }

    /**
     * Helper method that handles the update the local reference to the concurrent controller
     *
     * @param synLog the Synapse log to use
     */
    private void reloadDistributedConcurrentAccessController(SynapseLog synLog) {
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Throttle mediator : Updating local " +
                    "reference to the Concurrent Access controller");
        }
        concurrentAccessController = dataHolder.getConcurrentAccessController(key);
    }

    public void init(SynapseEnvironment se) {

        if (onAcceptMediator instanceof ManagedLifecycle) {
            ((ManagedLifecycle) onAcceptMediator).init(se);
        } else if (onAcceptSeqKey != null) {
            SequenceMediator onAcceptSeq =
                    (SequenceMediator) se.getSynapseConfiguration().
                            getSequence(onAcceptSeqKey);

            if (onAcceptSeq == null || onAcceptSeq.isDynamic()) {
                se.addUnavailableArtifactRef(onAcceptSeqKey);
            }
        }

        if (onRejectMediator instanceof ManagedLifecycle) {
            ((ManagedLifecycle) onRejectMediator).init(se);
        } else if (onRejectSeqKey != null) {
            SequenceMediator onRejectSeq =
                    (SequenceMediator) se.getSynapseConfiguration().
                            getSequence(onRejectSeqKey);

            if (onRejectSeq == null || onRejectSeq.isDynamic()) {
                se.addUnavailableArtifactRef(onRejectSeqKey);
            }
        }
        //reference to axis2 configuration context
        configContext = ((Axis2SynapseEnvironment) se).getAxis2ConfigurationContext();
        //throttling data holder initialization of
        // runtime throttle data eg :- throttle contexts
        dataHolder = (ThrottleDataHolder) configContext.
                getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
        if (dataHolder == null) {
            log.debug("Data holder not present in current Configuration Context");
            synchronized (configContext) {
                dataHolder = (ThrottleDataHolder) configContext.
                        getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
                if (dataHolder == null) {
                    dataHolder = new ThrottleDataHolder();
                    configContext.setNonReplicableProperty
                            (ThrottleConstants.THROTTLE_INFO_KEY, dataHolder);
                }
            }
        }
        //initializes whether clustering is enabled an Env. level
        ClusteringAgent clusteringAgent = configContext.getAxisConfiguration().getClusteringAgent();
        if (clusteringAgent != null) {
            isClusteringEnable = true;
        }
        //static policy initialization
        if (inLinePolicy != null) {
            log.debug("Initializing using static throttling policy : " + inLinePolicy);
            try {
                throttle = ThrottleFactory.
                        createMediatorThrottle(PolicyEngine.getPolicy(inLinePolicy));
                if (throttle != null && concurrentAccessController == null) {
                    concurrentAccessController = throttle.getConcurrentAccessController();
                    if (concurrentAccessController != null) {
                        dataHolder.setConcurrentAccessController(key, concurrentAccessController);
                    }
                }
            } catch (ThrottleException e) {
                handleException("Error processing the throttling policy", e, null);
            }
        }
        //access rate controller initialization
        accessControler = new AccessRateController();
        //replicator for global concurrent state maintenance
        if (isClusteringEnable) {
            concurrentAccessReplicator = new ConcurrentAccessReplicator(configContext);
        }

    }

    public void destroy() {
        //if synapse configuration refreshes we need to drop the previous throttle data
        if (configContext != null) {
            dataHolder.removeConcurrentAccessController(key);
        }
        if (onAcceptMediator instanceof ManagedLifecycle) {
            ((ManagedLifecycle) onAcceptMediator).destroy();
        }
        if (onRejectMediator instanceof ManagedLifecycle) {
            ((ManagedLifecycle) onRejectMediator).destroy();
        }
    }

    /**
     * To get the policy key - The key for which will used to lookup policy from the registry
     *
     * @return String
     */
    public String getPolicyKey() {
        return policyKey;
    }

    /**
     * To set the policy key - The key for which lookup from the registry
     *
     * @param policyKey Value for picking policy from the registry
     */
    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }

    /**
     * getting throttle policy which has defined as InLineXML
     *
     * @return InLine Throttle Policy
     */
    public OMElement getInLinePolicy() {
        return inLinePolicy;
    }

    /**
     * setting throttle policy which has defined as InLineXML
     *
     * @param inLinePolicy Inline policy
     */
    public void setInLinePolicy(OMElement inLinePolicy) {
        this.inLinePolicy = inLinePolicy;
    }

    public String getOnRejectSeqKey() {
        return onRejectSeqKey;
    }

    public void setOnRejectSeqKey(String onRejectSeqKey) {
        this.onRejectSeqKey = onRejectSeqKey;
    }

    public Mediator getOnRejectMediator() {
        return onRejectMediator;
    }

    public void setOnRejectMediator(Mediator onRejectMediator) {
        this.onRejectMediator = onRejectMediator;
    }

    public String getOnAcceptSeqKey() {
        return onAcceptSeqKey;
    }

    public void setOnAcceptSeqKey(String onAcceptSeqKey) {
        this.onAcceptSeqKey = onAcceptSeqKey;
    }

    public Mediator getOnAcceptMediator() {
        return onAcceptMediator;
    }

    public void setOnAcceptMediator(Mediator onAcceptMediator) {
        this.onAcceptMediator = onAcceptMediator;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.key = ThrottleConstants.THROTTLE_PROPERTY_PREFIX + id + ThrottleConstants.CAC_SUFFIX;
    }

    @Override
    public boolean isContentAware() {
        return false;
    }

    @Override
    public Mediator getInlineSequence(SynapseConfiguration synCfg, int inlinedSeqIdentifier) {
        if (inlinedSeqIdentifier == 0) {
            if (onRejectMediator != null) {
                return onRejectMediator;
            } else if (onRejectSeqKey != null) {
                return synCfg.getSequence(onRejectSeqKey);
            }
        } else if (inlinedSeqIdentifier == 1) {
            if (onAcceptMediator != null) {
                return onAcceptMediator;
            } else if (onAcceptSeqKey != null) {
                return synCfg.getSequence(onAcceptSeqKey);
            }
        }
        return null;
    }

    @Override
    public void setComponentStatisticsId(ArtifactHolder holder) {
        if (getAspectConfiguration() == null) {
            configure(new AspectConfiguration(getMediatorName()));
        }
        String mediatorId =
                StatisticIdentityGenerator.getIdForFlowContinuableMediator(getMediatorName(), ComponentType.MEDIATOR, holder);
        getAspectConfiguration().setUniqueId(mediatorId);

        String childId;
        if (onAcceptSeqKey != null) {
            childId = StatisticIdentityGenerator.getIdReferencingComponent(onAcceptSeqKey, ComponentType.SEQUENCE, holder);
            StatisticIdentityGenerator.reportingEndEvent(childId, ComponentType.SEQUENCE, holder);
        } else if (onAcceptMediator != null) {
            onAcceptMediator.setComponentStatisticsId(holder);
        }
        if (onRejectSeqKey != null) {
            childId = StatisticIdentityGenerator.getIdReferencingComponent(onRejectSeqKey, ComponentType.SEQUENCE, holder);
            StatisticIdentityGenerator.reportingEndEvent(childId, ComponentType.SEQUENCE, holder);
        } else if (onRejectMediator != null) {
            onRejectMediator.setComponentStatisticsId(holder);
        }
        StatisticIdentityGenerator.reportingFlowContinuableEndEvent(mediatorId, ComponentType.MEDIATOR, holder);
    }
}
