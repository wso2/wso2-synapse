/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.inbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.commons.handlers.MessagingHandler;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import org.wso2.securevault.commons.MiscellaneousUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entity which is responsible for exposing ESB message flow as an endpoint which can be invoked
 * by Clients. InboundEndpoint is an artifact type which can be created/modified dynamically.
 */
public class InboundEndpoint implements AspectConfigurable, ManagedLifecycle {
    protected static final Log log = LogFactory.getLog(InboundEndpoint.class);

    private String name;
    private String protocol;
    private String classImpl;
    private boolean isSuspend;
    private String injectingSeq;
    private String onErrorSeq;
    private boolean startInPausedMode;
    private Map<String, String> parametersMap = new LinkedHashMap<String, String>();
    private Map<String, String> parameterKeyMap = new LinkedHashMap<String, String>();
    private List<MessagingHandler> handlers = new ArrayList();
    private String fileName;
    private SynapseEnvironment synapseEnvironment;
    private InboundRequestProcessor inboundRequestProcessor;
    private Registry registry;
    /** car file name which this endpoint deployed from */
    private String artifactContainerName;
    /** Whether the deployed inbound endpoint is edited via the management console */
    private boolean isEdited;
    private AspectConfiguration aspectConfiguration;
    /** regex for any vault expression */
    private static final String secureVaultRegex = "\\{(.*?):vault-lookup\\('(.*?)'\\)\\}";
    private static final String REG_INBOUND_ENDPOINT_BASE_PATH = "/repository/components/org.apache.synapse.inbound/";
    private static final String INBOUND_ENDPOINT_STATE = "INBOUND_ENDPOINT_STATE";

    public void init(SynapseEnvironment se) {
        log.info("Initializing Inbound Endpoint: " + getName());
        synapseEnvironment = se;
        registry = se.getSynapseConfiguration().getRegistry();
        startInPausedMode = startInPausedMode();
        inboundRequestProcessor = getInboundRequestProcessor();
        if (inboundRequestProcessor != null) {
            try {
                inboundRequestProcessor.init();
            } catch (Exception e) {
                String msg = "Error initializing inbound endpoint " + getName();
                log.error(msg);
                throw new SynapseException(msg,e);
            }
        } else {
            String msg = "Inbound Request processor not found for Inbound EP : " + name +
                    " Protocol: " + protocol + " Class" + classImpl;
            log.error(msg);
            throw new SynapseException(msg);
        }

    }

    /**
     * Get plug-able InboundRequest processors from the classpath
     * <p/>
     * This looks for JAR files containing a META-INF/services that adheres to the following
     * http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html
     *
     * @return InboundRequest processor
     */
    private InboundRequestProcessor getInboundRequestProcessor() {
        if (log.isDebugEnabled()) {
            log.debug("Trying to fetch InboundRequestProcessor from classpath.. ");
        }
        Iterator<InboundRequestProcessorFactory> it = ServiceLoader.load(InboundRequestProcessorFactory.class,
                                                                         InboundRequestProcessorFactory.class
                                                                                 .getClassLoader()).iterator();
        InboundProcessorParams params = populateParams();
        while (it.hasNext()) {
            InboundRequestProcessorFactory factory = it.next();
            InboundRequestProcessor inboundRequestProcessor = factory.createInboundProcessor(params);
            if (inboundRequestProcessor != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Inbound Request Processor found in factory : " +
                              factory.getClass().getName());
                }
                return inboundRequestProcessor;
            }
        }
        return null;
    }

    /**
     * Populate inbound processor parameters and create object which holds parameters
     *
     * @return entity holding InboundProcessorParams
     */
    private InboundProcessorParams populateParams() {
        InboundProcessorParams inboundProcessorParams = new InboundProcessorParams();
        inboundProcessorParams.setProtocol(protocol);
        inboundProcessorParams.setClassImpl(classImpl);
        inboundProcessorParams.setName(name);
        inboundProcessorParams.setInjectingSeq(injectingSeq);
        inboundProcessorParams.setOnErrorSeq(onErrorSeq);
        inboundProcessorParams.setSynapseEnvironment(synapseEnvironment);
        inboundProcessorParams.setStartInPausedMode(startInPausedMode);

        Properties props = Utils.paramsToProperties(parametersMap);
        //replacing values by secure vault
        resolveVaultExpressions(props);
        resolveSystemSecureVaultProperties(props);
        inboundProcessorParams.setProperties(props);

        for (MessagingHandler handler: handlers) {
            inboundProcessorParams.addHandler(handler);
        }
        return inboundProcessorParams;
    }

    private void resolveSystemSecureVaultProperties(Properties props) {

        SecretResolver secretResolver = SecretResolverFactory.create(props);
        if (secretResolver.isInitialized()) {
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                props.put(entry.getKey(), MiscellaneousUtil.resolve(entry.getValue().toString(), secretResolver));
            }
        }
    }

    /**
     * Pauses the intake of new messages. Already consumed messages will continue processing.
     * This method does not wait for in-flight messages to complete.
     *
     * <p>
     * This method disables the inbound endpoint by invoking the {@link InboundRequestProcessor#pause()} method
     * on the associated request processor. If the processor is present, it attempts to pause it and logs any
     * errors encountered during the process.
     * </p>
     */
    public void pause() {
        log.info("Pausing Inbound Endpoint: " + getName());
        if (inboundRequestProcessor != null) {
            try {
                inboundRequestProcessor.pause();
            } catch (Exception e) {
                log.error("Error occurred while pausing the Inbound endpoint: " + getName(), e);
            }
        }
    }

    /**
     * Remove inbound endpoints.
     * <p>
     * This was introduced as a fix for product-ei#1206.
     *
     * @param removeTask Whether to remove scheduled task or not.
     */
    public void destroy(boolean removeTask) {
        log.info("Destroying Inbound Endpoint: " + getName());
        if (inboundRequestProcessor != null) {
            try {
                if (inboundRequestProcessor instanceof InboundTaskProcessor) {
                    ((InboundTaskProcessor) inboundRequestProcessor).destroy(removeTask);
                } else {
                    inboundRequestProcessor.destroy();
                }
            } catch (Exception e) {
                log.error("Unable to destroy Inbound endpoint", e);
            }
        }
    }

    /**
     * Remove inbound endpoints.
     */
    public void destroy() {
        destroy(true);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isSuspend() {
        return isSuspend;
    }

    public void setSuspend(boolean isSuspend) {
        this.isSuspend = isSuspend;
    }

    public String getInjectingSeq() {
        return injectingSeq;
    }

    public void setInjectingSeq(String injectingSeq) {
        this.injectingSeq = injectingSeq;
    }

    public String getOnErrorSeq() {
        return onErrorSeq;
    }

    public void setOnErrorSeq(String onErrorSeq) {
        this.onErrorSeq = onErrorSeq;
    }

    /**
     * Activates the inbound endpoint.
     * <p>
     * This method synchronizes access to ensure thread safety while activating the inbound endpoint.
     * It calls the underlying {@link InboundRequestProcessor} to perform the activation logic.
     * If the activation is successful, updates the inbound endpoint's state in the registry
     * to {@link InboundEndpointState#ACTIVE}
     * </p>
     */
    public synchronized DynamicControlOperationResult activate() {
        String errormessage = "";
        boolean isSuccess = false;

        if (Objects.isNull(this.inboundRequestProcessor)) {
            errormessage = "Unable to activate the Inbound Endpoint [" + getName() + "] because "
                    + "no associated inbound request processor was found!";
            log.error(errormessage);
        } else {
            log.info("Activating the Inbound Endpoint: " + getName());

            try {
                if (this.inboundRequestProcessor.activate()) {
                    log.info("Inbound Endpoint [" + getName() + "] is successfully activated.");
                    setInboundEndpointStateInRegistry(InboundEndpointState.ACTIVE);
                    isSuccess = true;
                } else {
                    errormessage = "Failed to activate the Inbound Endpoint: " + getName();
                    log.error(errormessage);
                }
            } catch (UnsupportedOperationException e) {
                errormessage = "Activate operation is not supported for the Inbound Endpoint: " + getName();
                log.warn(errormessage, e);
            } catch (Exception e) {
                errormessage = "Failed to activate the Inbound Endpoint: " + getName();
                log.error(errormessage, e);
            }
        }
        return new DynamicControlOperationResult(isSuccess, errormessage);
    }

    /**
     * Deactivates the inbound endpoint.
     * <p>
     * This method synchronizes access to ensure thread safety while deactivating the inbound endpoint.
     * It calls the underlying {@link InboundRequestProcessor} to perform the deactivation logic.
     * If the deactivation is successful, the method updates the inbound endpoint's state in the
     * registry to {@link InboundEndpointState#INACTIVE}.
     * </p>
     */
    public synchronized DynamicControlOperationResult deactivate() {
        String errorMessage = "";
        boolean isSuccess = false;

        if (Objects.isNull(this.inboundRequestProcessor)) {
            errorMessage = "Unable to deactivate the Inbound Endpoint [" + getName() + "] because "
                    + "no associated inbound request processor was found!";
            log.error(errorMessage);
        } else {
            log.info("Deactivating the Inbound Endpoint: " + getName());

            try {
                if (this.inboundRequestProcessor.deactivate()) {
                    log.info("Inbound Endpoint [" + getName() + "] is successfully deactivated.");
                    setInboundEndpointStateInRegistry(InboundEndpointState.INACTIVE);
                    isSuccess = true;
                } else {
                    errorMessage = "Failed to deactivate the Inbound Endpoint: " + getName();
                    log.error(errorMessage);
                }
            } catch (UnsupportedOperationException e) {
                errorMessage = "Deactivate operation is not supported for the Inbound Endpoint: " + getName();
                log.warn(errorMessage, e);
            } catch (Exception e) {
                errorMessage = "Failed to deactivate the Inbound Endpoint: " + getName();
                log.error(errorMessage, e);
            }
        }
        return new DynamicControlOperationResult(isSuccess, errorMessage);
    }

    /**
     * Checks whether the inbound endpoint is deactivated.
     * <p>
     * This method delegates the check to the underlying {@link InboundRequestProcessor},
     * which determines the deactivation state of the inbound endpoint.
     * </p>
     *
     * @return {@code true} if the inbound endpoint is deactivated; {@code false} otherwise.
     */
    public boolean isDeactivated() {

        if (Objects.isNull(this.inboundRequestProcessor)) {
            return true;
        }
        return inboundRequestProcessor.isDeactivated();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, String> getParametersMap() {
        return parametersMap;
    }

    public void addParameter(String name, String value) {
        parametersMap.put(name, value);
    }

    public void addParameter(String name, String value, String key) {
        addParameter(name, value);
        parameterKeyMap.put(name, key);
    }    
    
    public String getParameter(String name) {
        return parametersMap.get(name);
    }

    public String getParameterKey(String name) {
        return parameterKeyMap.get(name);
    }
    
    public String getClassImpl() {
        return classImpl;
    }

    public void setClassImpl(String classImpl) {
        this.classImpl = classImpl;
    }

    public void setArtifactContainerName (String name) {
        artifactContainerName = name;
    }

    public String getArtifactContainerName () {
        return artifactContainerName;
    }

    public boolean getIsEdited() {
        return isEdited;
    }

    public void setIsEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    @Override
    public void configure(AspectConfiguration aspectConfiguration) {
        this.aspectConfiguration = aspectConfiguration;
    }

    @Override
    public AspectConfiguration getAspectConfiguration() {
        return aspectConfiguration;
    }

    public void setComponentStatisticsId(ArtifactHolder holder){
        if (aspectConfiguration == null) {
            aspectConfiguration = new AspectConfiguration(name);
        }
        String apiId = StatisticIdentityGenerator.getIdForComponent(name, ComponentType.INBOUNDENDPOINT, holder);
        aspectConfiguration.setUniqueId(apiId);
        String childId = null;
        if (injectingSeq != null) {
            childId = StatisticIdentityGenerator.getIdReferencingComponent(injectingSeq, ComponentType.SEQUENCE, holder);
            StatisticIdentityGenerator.reportingEndEvent(childId, ComponentType.SEQUENCE, holder);
        }
        if (onErrorSeq != null) {
            childId = StatisticIdentityGenerator.getIdReferencingComponent(onErrorSeq, ComponentType.SEQUENCE, holder);
            StatisticIdentityGenerator.reportingEndEvent(childId, ComponentType.SEQUENCE, holder);
        }
        StatisticIdentityGenerator.reportingEndEvent(apiId, ComponentType.INBOUNDENDPOINT, holder);
    }

    private void resolveVaultExpressions(Properties props) {
        Pattern vaultLookupPattern = Pattern.compile(secureVaultRegex);
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String value = (String) entry.getValue();
            Matcher lookupMatcher = vaultLookupPattern.matcher(value);
            //setting value initially
            String newParamValue = value;
            while (lookupMatcher.find()) {
                Value expression = null;
                //getting the expression with out curly brackets
                String expressionStr = lookupMatcher.group(0).substring(1, lookupMatcher.group(0).length() - 1);
                try {
                    expression = new Value(new SynapseXPath(expressionStr));
                } catch (JaxenException e) {
                    log.error("Error while building the expression : " + expressionStr);
                }
                if (expression != null) {
                    String resolvedValue = expression.evaluateValue(synapseEnvironment.createMessageContext());
                    if (resolvedValue == null || resolvedValue.isEmpty()) {
                        log.warn("Found Empty value for expression : " + expression.getExpression());
                        resolvedValue = "";
                    }
                    //replacing the expression with resolved value
                    newParamValue = newParamValue.replaceFirst(secureVaultRegex, resolvedValue);
                    props.put(entry.getKey(), newParamValue);
                }
            }
        }

    }

    public List<MessagingHandler> getHandlers() {

        return handlers;
    }

    public void addHandler(MessagingHandler handler) {

        this.handlers.add(handler);
    }

    /**
     * Updates the state of the Inbound Endpoint in the registry.
     *
     * <p>This method ensures that the state of the Inbound Endpoint is persisted in
     * the registry for future reference. If the registry is unavailable and state
     * preservation is enabled, a warning is logged, and the state will not be updated.
     *
     * @param state the {@link InboundEndpointState} to be saved in the registry
     */
    private void setInboundEndpointStateInRegistry(InboundEndpointState state) {
        if (Objects.isNull(registry)) {
            log.warn("Registry not available! The state of the Inbound Endpoint will not be saved.");
            return;
        }
        registry.newNonEmptyResource(REG_INBOUND_ENDPOINT_BASE_PATH + getName(), false, "text/plain",
                state.toString(), INBOUND_ENDPOINT_STATE);
    }

    /**
     * Deletes the state of the Inbound Endpoint from the registry.
     *
     * <p>This method removes the registry entry corresponding to the current
     * Inbound Endpoint's state, if it exists. If the registry is unavailable,
     * the operation is skipped.
     */
    private void deleteInboundEndpointStateInRegistry() {
        if (Objects.isNull(registry)) {
            return;
        }
        if (registry.getResourceProperties(REG_INBOUND_ENDPOINT_BASE_PATH + getName()) != null) {
            registry.delete(REG_INBOUND_ENDPOINT_BASE_PATH + getName());
        }
    }

    /**
     * Determines whether the inbound endpoint should start in paused mode.
     *
     * This method evaluates the `preserveState` flag and the current state of the inbound endpoint
     * to decide if the endpoint should start in a paused state.
     *
     * - If `preserveState` is false or if the current state in the registry is {@link InboundEndpointState#INITIAL},
     *   it returns the value of `suspend` attribute in the inbound endpoint configuration`.
     * - Otherwise, it checks if the current state is {@link InboundEndpointState#INACTIVE}
     *   and returns `true` if it is, indicating the endpoint should start in paused mode.
     *
     * @return {@code true} if the inbound endpoint should start in paused mode, {@code false} otherwise.
     */
    private boolean startInPausedMode() {

        if (getInboundEndpointStateFromRegistry() == InboundEndpointState.INITIAL) {
            return isSuspend();
        }
        return (getInboundEndpointStateFromRegistry() == InboundEndpointState.INACTIVE);
    }

    /**
     * Retrieves the current state of the inbound endpoint from the registry.
     *
     * This method checks the registry for the state of the inbound endpoint associated with
     * the provided name. It first fetches the resource properties of the inbound endpoint from
     * the registry. If no properties are found, the method assumes the state is {@link InboundEndpointState#INITIAL}.
     *
     * If the state is present, it determines whether the state is {@link InboundEndpointState#ACTIVE}.
     * or {@link InboundEndpointState#INACTIVE}.
     *
     * @return The current state of the inbound endpoint, as either {@link InboundEndpointState#ACTIVE},
     * {@link InboundEndpointState#INACTIVE}, or {@link InboundEndpointState#INITIAL} if not explicitly set.
     */
    private InboundEndpointState getInboundEndpointStateFromRegistry() {
        Properties resourceProperties = null;
        if (Objects.nonNull(registry)) {
            resourceProperties = registry.getResourceProperties(REG_INBOUND_ENDPOINT_BASE_PATH + getName());
        }

        if (resourceProperties == null) {
            return InboundEndpointState.INITIAL;
        }

        String state = resourceProperties.getProperty(INBOUND_ENDPOINT_STATE);
        if (InboundEndpointState.ACTIVE.toString().equalsIgnoreCase(state)) {
            return InboundEndpointState.ACTIVE;
        }
        return InboundEndpointState.INACTIVE;
    }

    private enum InboundEndpointState {
        INITIAL, ACTIVE, INACTIVE
    }

    /**
     * Updates the state of the inbound endpoint to either paused or active based on the given parameter.
     * <p>
     * This method attempts to change the state of the inbound endpoint and ensures that the state in
     * the registry matches the expected behavior. If the operation fails to align the actual state
     * with the requested state, a warning is logged to indicate the potential for inconsistent behavior.
     *
     * @param pause {@code true} to pause the inbound endpoint, setting its state to {@code INACTIVE};
     *              {@code false} to resume the inbound endpoint, setting its state to {@code ACTIVE}.
     */
    public void updateInboundEndpointState(boolean pause) {

        if (Objects.isNull(inboundRequestProcessor)) {
            log.error("Unable to update the state of the Inbound Endpoint [" + getName() + "] as it does not exist!");
        }
        if (pause && inboundRequestProcessor.isDeactivated()) {
            setInboundEndpointStateInRegistry(InboundEndpointState.INACTIVE);
        } else if (!pause && !inboundRequestProcessor.isDeactivated()){
            setInboundEndpointStateInRegistry(InboundEndpointState.ACTIVE);
        } else {
            log.warn("The inbound endpoint [" + name + "] was requested to change its state to "
                    + (pause ? "pause" : "resume") + ", but the operation did not complete successfully "
                    + "as the actual state does not match the expected state.");
        }
    }

}
