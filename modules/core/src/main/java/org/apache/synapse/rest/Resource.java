/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.rest;

import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.Constants;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.rest.dispatch.DispatcherHelper;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Resource extends AbstractRESTProcessor implements ManagedLifecycle, AspectConfigurable {

    /**
     * List of HTTP methods applicable on this method. Empty list means all methods
     * are applicable.
     */
    private Set<String> methods = new HashSet<String>(4);

    private String contentType;

    private String userAgent;

    private int protocol = RESTConstants.PROTOCOL_HTTP_AND_HTTPS;

    AspectConfiguration aspectConfiguration;

    /**
     * In-lined sequence to be executed upon receiving messages
     */
    private SequenceMediator inSequence;

    private SequenceMediator outSequence;

    private SequenceMediator faultSequence;

    /**
     * Identifier of the sequence to be executed upon receiving a message
     */
    private String inSequenceKey;

    private String outSequenceKey;

    private String faultSequenceKey;

    /**
     * DispatcherHelper instance which is  used to determine whether a particular resource
     * should be dispatched to this resource or not
     */
    private DispatcherHelper dispatcherHelper;

    public Resource() {
        super(UIDGenerator.generateUID());
    }

    protected String getName() {
        return name;
    }

    public SequenceMediator getInSequence() {
        return inSequence;
    }

    public void setInSequence(SequenceMediator inSequence) {
        this.inSequence = inSequence;
    }

    public SequenceMediator getOutSequence() {
        return outSequence;
    }

    public void setOutSequence(SequenceMediator outSequence) {
        this.outSequence = outSequence;
    }

    public String getInSequenceKey() {
        return inSequenceKey;
    }

    public void setInSequenceKey(String inSequenceKey) {
        this.inSequenceKey = inSequenceKey;
    }

    public String getOutSequenceKey() {
        return outSequenceKey;
    }

    public void setOutSequenceKey(String outSequenceKey) {
        this.outSequenceKey = outSequenceKey;
    }

    public SequenceMediator getFaultSequence() {
        return faultSequence;
    }

    public void setFaultSequence(SequenceMediator faultSequence) {
        this.faultSequence = faultSequence;
    }

    public String getFaultSequenceKey() {
        return faultSequenceKey;
    }

    public void setFaultSequenceKey(String faultSequenceKey) {
        this.faultSequenceKey = faultSequenceKey;
    }

    public boolean addMethod(String method) {
        for (RESTConstants.METHODS allowedMethod : RESTConstants.METHODS.values()) {
            if (allowedMethod.name().equals(method)) {
                methods.add(method);
                return true;
            }
        }
        return false;
    }

    public String[] getMethods() {
        return methods.toArray(new String[methods.size()]);
    }

    /**
     * Helper method to check whether API supports the incoming HTTP method.
     *
     * @param method
     * @return true if support false otherwise.
     */
    public boolean hasMatchingMethod(String method) {
        if (RESTConstants.METHOD_OPTIONS.equals(method)) {
            return true; // OPTIONS requests are always welcome
        } else if (!methods.isEmpty()) {
            if (!methods.contains(method)) {
                if (log.isDebugEnabled()) {
                    log.debug("HTTP method does not match");
                }
                return false;
            }
        }
        return true;
    }

    public DispatcherHelper getDispatcherHelper() {
        return dispatcherHelper;
    }

    public void setDispatcherHelper(DispatcherHelper dispatcherHelper) {
        this.dispatcherHelper = dispatcherHelper;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        if (contentType.indexOf('/') == -1 || contentType.split("/").length != 2) {
            throw new SynapseException("Invalid content type: " + contentType);
        }
        this.contentType = contentType;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getProtocol() {
        return protocol;
    }

    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }

    @Override
    boolean canProcess(MessageContext synCtx) {
        if (synCtx.isResponse()) {
            return true;
        }

        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).
                getAxis2MessageContext();
        if (protocol == RESTConstants.PROTOCOL_HTTP_ONLY &&
                !Constants.TRANSPORT_HTTP.equals(msgCtx.getIncomingTransportName())) {
            if (log.isDebugEnabled()) {
                log.debug("Protocol information does not match - Expected HTTP");
            }
            return false;
        }

        if (protocol == RESTConstants.PROTOCOL_HTTPS_ONLY &&
                !Constants.TRANSPORT_HTTPS.equals(msgCtx.getIncomingTransportName())) {
            if (log.isDebugEnabled()) {
                log.debug("Protocol information does not match - Expected HTTPS");
            }
            return false;
        }

        String method = (String) msgCtx.getProperty(Constants.Configuration.HTTP_METHOD);
        synCtx.setProperty(RESTConstants.REST_METHOD, method);

        if (RESTConstants.METHOD_OPTIONS.equals(method)) {
            return true; // OPTIONS requests are always welcome
        } else if (!methods.isEmpty()) {
            if (!methods.contains(method)) {
                if (log.isDebugEnabled()) {
                    log.debug("HTTP method does not match");
                }
                return false;
            }
        }

        Map transportHeaders = (Map) msgCtx.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if ((contentType != null || userAgent != null) && transportHeaders == null) {
            if (log.isDebugEnabled()) {
                log.debug("Transport headers not available on the message");
            }
            return false;
        }

        boolean hasPayload = !Boolean.TRUE.equals(msgCtx.getProperty(NhttpConstants.NO_ENTITY_BODY));
        if (contentType != null && hasPayload) {
            String type = (String) transportHeaders.get(HTTP.CONTENT_TYPE);
            if (!contentType.equals(type)) {
                if (log.isDebugEnabled()) {
                    log.debug("Content type does not match - Expected: " + contentType + ", " +
                            "Found: " + type);
                }
                return false;
            }
        }

        if (userAgent != null) {
            String agent = (String) transportHeaders.get(HTTP.USER_AGENT);
            if (agent == null || !agent.matches(this.userAgent)) {
                if (log.isDebugEnabled()) {
                    log.debug("User agent does not match - Expected: " + userAgent + ", " +
                            "Found: " + agent);
                }
                return false;
            }
        }

        return true;
    }

    void process(MessageContext synCtx) {
        Integer statisticReportingIndex = null;
        boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
        if (!synCtx.isResponse()) {
            if (getDispatcherHelper() != null) {
                synCtx.setProperty(RESTConstants.REST_URL_PATTERN, getDispatcherHelper().getString());
            }
        }
        if (isStatisticsEnabled) {
            statisticReportingIndex = OpenEventCollector.reportChildEntryEvent(synCtx, getResourceName(synCtx, name),
                    ComponentType.RESOURCE, getAspectConfiguration(), true);
        }

        if (log.isDebugEnabled()) {
            log.debug("Processing message with ID: " + synCtx.getMessageID() + " through the " +
                    "resource: " + name);
        }

        if (!synCtx.isResponse()) {
            String method = (String) synCtx.getProperty(RESTConstants.REST_METHOD);
            if (RESTConstants.METHOD_OPTIONS.equals(method) && sendOptions(synCtx)) {
                if (isStatisticsEnabled) {
                    CloseEventCollector.closeEntryEvent(synCtx, getResourceName(synCtx, name), ComponentType.RESOURCE,
                            statisticReportingIndex, true);
                }
                return;
            }

            synCtx.setProperty(RESTConstants.SYNAPSE_RESOURCE, name);
            String path = RESTUtils.getFullRequestPath(synCtx);

            int queryIndex = path.indexOf('?');
            if (queryIndex != -1) {
                String query = path.substring(queryIndex + 1);
                String[] entries = query.split(RESTConstants.QUERY_PARAM_DELIMITER);
                String name = null, value ;
                for (String entry : entries) {
                    int index = entry.indexOf('=');
                    if (index != -1) {
                        try {
                            name = entry.substring(0, index);
                            value = URLDecoder.decode(entry.substring(index + 1),
                                    RESTConstants.DEFAULT_ENCODING);
                            synCtx.setProperty(RESTConstants.REST_QUERY_PARAM_PREFIX + name, value);
                        } catch (UnsupportedEncodingException uee) {
                            handleException("Error processing " + method + " request for : " + path, uee);
                        } catch (IllegalArgumentException e) {
                            String errorMessage = "Error processing " + method + " request for : " + path
                                    + " due to an error in the request sent by the client";
                            synCtx.setProperty(SynapseConstants.ERROR_CODE, HttpStatus.SC_BAD_REQUEST);
                            synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
                            org.apache.axis2.context.MessageContext inAxisMsgCtx =
                                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                            inAxisMsgCtx.setProperty(SynapseConstants.HTTP_SC, HttpStatus.SC_BAD_REQUEST);
                            handleException(errorMessage, e);

                        }
                    } else {
                        // If '=' sign isn't presnet in the entry means that the '&' character is part of
                        // the query parameter value. If so query parameter value should be updated appending
                        // the remaining characters.
                        String existingValue = (String) synCtx.getProperty(RESTConstants.REST_QUERY_PARAM_PREFIX + name);
                        value = RESTConstants.QUERY_PARAM_DELIMITER + entry;
                        synCtx.setProperty(RESTConstants.REST_QUERY_PARAM_PREFIX + name, existingValue + value);
                    }
                }
            }
        }

        SequenceMediator sequence = synCtx.isResponse() ? outSequence : inSequence;
        if (sequence != null) {
            registerFaultHandler(synCtx);
            sequence.mediate(synCtx);
            if (isStatisticsEnabled) {
                CloseEventCollector.closeEntryEvent(synCtx, getResourceName(synCtx, name), ComponentType.RESOURCE,
                        statisticReportingIndex, true);
            }
            return;
        }

        String sequenceKey = synCtx.isResponse() ? outSequenceKey : inSequenceKey;
        if (sequenceKey != null) {
            registerFaultHandler(synCtx);
            Mediator referredSequence = synCtx.getSequence(sequenceKey);
            if (referredSequence != null) {
                referredSequence.mediate(synCtx);
            } else {
                throw new SynapseException("Specified sequence: " + sequenceKey + " cannot " +
                        "be found");
            }
            if (isStatisticsEnabled) {
                CloseEventCollector.closeEntryEvent(synCtx, getResourceName(synCtx, name), ComponentType.RESOURCE,
                        statisticReportingIndex, true);
            }
            return;
        }

        // Neither a sequence nor a sequence key has been specified. If this message is a
        // response, simply send it back to the client.
        if (synCtx.isResponse()) {
            if (log.isDebugEnabled()) {
                log.debug("No out-sequence configured. Sending the response back.");
            }
            registerFaultHandler(synCtx);
            Axis2Sender.sendBack(synCtx);
        } else if (log.isDebugEnabled()) {
            log.debug("No in-sequence configured. Dropping the request.");
        }
        if (isStatisticsEnabled) {
            CloseEventCollector
                    .closeEntryEvent(synCtx, getResourceName(synCtx, name), ComponentType.RESOURCE, statisticReportingIndex,
                            true);
        }
    }

    public void registerFaultHandler(MessageContext synCtx) {
        if (faultSequence != null) {
            synCtx.pushFaultHandler(new MediatorFaultHandler(faultSequence));
        } else if (faultSequenceKey != null) {
            Mediator faultSequence = synCtx.getSequence(faultSequenceKey);
            if (faultSequence != null) {
                synCtx.pushFaultHandler(new MediatorFaultHandler(faultSequence));
            } else {
                synCtx.pushFaultHandler(new MediatorFaultHandler(synCtx.getFaultSequence()));
            }
        } else {
            synCtx.pushFaultHandler(new MediatorFaultHandler(synCtx.getFaultSequence()));
        }
    }

    private boolean sendOptions(MessageContext synCtx) {
        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).
                getAxis2MessageContext();
        Map<String,String> transportHeaders = (Map<String,String>) msgCtx.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        if (methods.contains(RESTConstants.METHOD_OPTIONS)) {
            // Resource should mediate the OPTIONS request
            String maxForwardsHeader = transportHeaders.get(HttpHeaders.MAX_FORWARDS);
            if (maxForwardsHeader != null) {
                int maxForwards = Integer.parseInt(maxForwardsHeader);
                if (maxForwards == 0) {
                    // Resource should respond to the OPTIONS request
                    synCtx.setResponse(true);
                    synCtx.setTo(null);
                    transportHeaders.put(HttpHeaders.ALLOW, getSupportedMethods());
                    Axis2Sender.sendBack(synCtx);
                    return true;
                } else {
                    transportHeaders.put(HttpHeaders.MAX_FORWARDS, String.valueOf(maxForwards - 1));
                }
            }
            return false;

        } else {
            // Resource should respond to the OPTIONS request
            synCtx.setResponse(true);
            synCtx.setTo(null);
            transportHeaders.put(HttpHeaders.ALLOW, getSupportedMethods());
            Axis2Sender.sendBack(synCtx);
            return true;
        }
    }

    private String getSupportedMethods() {
        StringBuilder value = new StringBuilder("");
        if (methods.isEmpty()) {
            value.append(RESTConstants.REST_ALL_SUPPORTED_METHODS);
        } else {
            for (String method : methods) {
                if (RESTConstants.METHOD_OPTIONS.equals(method)) {
                    continue;
                }

                if (value.length() > 0) {
                    value.append(", ");
                }
                value.append(method);
            }
        }
        return value.toString();
    }

    public void init(SynapseEnvironment se) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing resource with ID: " + name);
        }

        if (inSequence != null) {
            inSequence.init(se);
        }
        if (outSequence != null) {
            outSequence.init(se);
        }
        if (faultSequence != null) {
            faultSequence.init(se);
        }
    }

    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying resource with ID: " + name);
        }

        if (inSequence != null && inSequence.isInitialized()) {
            inSequence.destroy();
        }
        if (outSequence != null && outSequence.isInitialized()) {
            outSequence.destroy();
        }
        if (faultSequence != null && faultSequence.isInitialized()) {
            faultSequence.destroy();
        }
    }

    private static String getResourceName(MessageContext messageContext, String resourceId) {
        Object synapseRestApi = messageContext.getProperty(RESTConstants.REST_API_CONTEXT);
        Object restUrlPattern = messageContext.getProperty(RESTConstants.REST_URL_PATTERN);
        if (synapseRestApi != null) {
            String textualStringName;
            if (restUrlPattern != null) {
                textualStringName = (String) synapseRestApi + restUrlPattern;
            } else {
                textualStringName = (String) synapseRestApi;
            }
            return textualStringName;
        }
        return resourceId;
    }

    @Override
    public void configure(AspectConfiguration aspectConfiguration) {
        this.aspectConfiguration = aspectConfiguration;
    }

    @Override
    public AspectConfiguration getAspectConfiguration() {
        return aspectConfiguration;
    }

    public void setComponentStatisticsId(ArtifactHolder holder) {
        StatisticIdentityGenerator.reportingBranchingEvents(holder);
        if (aspectConfiguration == null) {
            aspectConfiguration = new AspectConfiguration(name);
        }
        String resourceId =
                StatisticIdentityGenerator.getIdForComponent(getResourceClassName(), ComponentType.RESOURCE, holder);
        aspectConfiguration.setUniqueId(resourceId);

        String childId = null;
        if (inSequenceKey != null) {
            childId = StatisticIdentityGenerator.getIdReferencingComponent(inSequenceKey, ComponentType.SEQUENCE, holder);
            StatisticIdentityGenerator.reportingEndEvent(childId, ComponentType.SEQUENCE, holder);
        }
        if (inSequence != null) {
            inSequence.setComponentStatisticsId(holder);
        }
        if (outSequenceKey != null) {
            childId = StatisticIdentityGenerator.getIdReferencingComponent(outSequenceKey, ComponentType.SEQUENCE, holder);
            StatisticIdentityGenerator.reportingEndEvent(childId, ComponentType.SEQUENCE, holder);
        }
        if (outSequence != null) {
            outSequence.setComponentStatisticsId(holder);
        }
        if (faultSequenceKey != null) {
            childId = StatisticIdentityGenerator.getIdReferencingComponent(faultSequenceKey, ComponentType.SEQUENCE, holder);
            StatisticIdentityGenerator.reportingEndEvent(childId, ComponentType.SEQUENCE, holder);
        }
        if (faultSequence != null) {
            faultSequence.setComponentStatisticsId(holder);
        }
        StatisticIdentityGenerator.reportingEndEvent(resourceId, ComponentType.RESOURCE, holder);
    }

    /**
     * Returns the name of the class of Resource.
     * @return Resource class name.
     */
    public String getResourceClassName(){
        String cls = getClass().getName();
        return cls.substring(cls.lastIndexOf(".") + 1);
    }
}
