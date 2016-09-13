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

import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.dispatch.DispatcherHelper;
import org.apache.synapse.rest.dispatch.RESTDispatcher;
import org.apache.synapse.rest.version.DefaultStrategy;
import org.apache.synapse.rest.version.URLBasedVersionStrategy;
import org.apache.synapse.rest.version.VersionStrategy;
import org.apache.synapse.config.xml.rest.VersionStrategyFactory;
import org.apache.synapse.transport.http.conn.SynapseDebugInfoHolder;
import org.apache.synapse.transport.http.conn.SynapseWireLogHolder;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.util.*;

public class API extends AbstractRESTProcessor implements ManagedLifecycle, AspectConfigurable {

    private String host;
    private int port = -1;
    private String context;
    private Map<String,Resource> resources = new LinkedHashMap<String,Resource>();
    private List<Handler> handlers = new ArrayList<Handler>();

    private int protocol = RESTConstants.PROTOCOL_HTTP_AND_HTTPS;

    private VersionStrategy versionStrategy = new DefaultStrategy(this);

    private String fileName;

    private  Log apiLog;
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    private int traceState = SynapseConstants.TRACING_UNSET;

    private String artifactContainerName;

    private boolean isEdited = false;

    private AspectConfiguration aspectConfiguration;

    public API(String name, String context) {
        super(name);
        setContext(context);
    }

    public void setContext(String context) {
        if (!context.startsWith("/")) {
            handleException("API context must begin with '/' character");
        }
        this.context = RESTUtils.trimTrailingSlashes(context);
        apiLog = LogFactory.getLog(SynapseConstants.API_LOGGER_PREFIX + name);

    }

    public void setArtifactContainerName (String name) {
        artifactContainerName = name;
    }

    public String getArtifactContainerName() {
        return artifactContainerName;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setIsEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    public void setLogSetterValue () {
        CustomLogSetter.getInstance().setLogAppender(artifactContainerName);
    }

    /**
     * Get the fully qualified name of this API
     *
     * @return returns the key combination for API NAME + VERSION
     */
    public String getName() {
        // check if a versioning strategy exists
        if (versionStrategy.getVersion() != null && !"".equals(versionStrategy.getVersion()) ) {
            return name + ":v" +versionStrategy.getVersion();
        }
        return name;
    }

    public int getProtocol() {
        return protocol;
    }

    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }

    public String getAPIName() {
        return name;
    }

    public String getVersion(){
        return versionStrategy.getVersion();
    }

    public String getContext() {
        return context;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void addResource(Resource resource) {
        DispatcherHelper dispatcherHelper = resource.getDispatcherHelper();
        if (dispatcherHelper != null) {
            String mapping = dispatcherHelper.getString();
            for (Resource r : resources.values()) {
                DispatcherHelper helper = r.getDispatcherHelper();
                if (helper != null && helper.getString().equals(mapping) &&
                        resourceMatches(resource, r)) {
                    handleException("Two resources cannot have the same path mapping and methods");
                }
            }
        } else {
            for (Resource r : resources.values()) {
                DispatcherHelper helper = r.getDispatcherHelper();
                if (helper == null) {
                    handleException("Only one resource can be designated as default");
                }
            }
        }
        resources.put(resource.getName(), resource);
    }

    private boolean resourceMatches(Resource r1, Resource r2) {
        String[] methods1 = r1.getMethods();
        String[] methods2 = r2.getMethods();
        for (String m1 : methods1) {
            for (String m2 : methods2) {
                if (m1.equals(m2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Resource[] getResources() {
        return resources.values().toArray(new Resource[resources.size()]);
    }

    public void addHandler(Handler handler) {
        handlers.add(handler);
    }

    public Handler[] getHandlers() {
        return handlers.toArray(new Handler[handlers.size()]);
    }

    boolean canProcess(MessageContext synCtx) {
        if (synCtx.isResponse()) {
            String apiName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API);
            String version = synCtx.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION) == null ?
                             "": (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION);
            //if api name is not matching OR versions are different
            if (!getName().equals(apiName) || !versionStrategy.getVersion().equals(version)) {
                return false;
            }
        } else {
            String path = RESTUtils.getFullRequestPath(synCtx);
            if (!path.startsWith(context + "/") && !path.startsWith(context + "?") &&
                    !context.equals(path) && !"/".equals(context)) {
                auditDebug("API context: " + context + " does not match request URI: " + path);
                return false;
            }

            if(!versionStrategy.isMatchingVersion(synCtx)){
                return false;
            }

            org.apache.axis2.context.MessageContext msgCtx =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();

            if (host != null || port != -1) {
                String hostHeader = getHostHeader(msgCtx);
                if (hostHeader != null) {
                    if (host != null && !host.equals(extractHostName(hostHeader))) {
                        auditDebug("API host: " + host + " does not match host information " +
                                    "in the request: " + hostHeader);
                        return false;
                    }

                    if (port != -1 && port != extractPortNumber(hostHeader,
                            msgCtx.getIncomingTransportName())) {
                        auditDebug("API port: " + port + " does not match port information " +
                                    "in the request: " + hostHeader);
                        return false;
                    }
                } else {
                    auditDebug("Host information not available on the message");
                    return false;
                }
            }
            if (protocol == RESTConstants.PROTOCOL_HTTP_ONLY &&
                    !Constants.TRANSPORT_HTTP.equals(msgCtx.getIncomingTransportName())) {
                if (log.isDebugEnabled()) {
                    log.debug("Protocol information does not match - Expected HTTP");
                }
                synCtx.setProperty(SynapseConstants.TRANSPORT_DENIED,new Boolean(true));
                synCtx.setProperty(SynapseConstants.IN_TRANSPORT,msgCtx.getTransportIn().getName());
                log.warn("Trying to access API : "+name+" on restricted transport chanel ["+msgCtx.getTransportIn().getName()+"]");
                return false;
            }

            if (protocol == RESTConstants.PROTOCOL_HTTPS_ONLY &&
                    !Constants.TRANSPORT_HTTPS.equals(msgCtx.getIncomingTransportName())) {
                if (log.isDebugEnabled()) {
                    log.debug("Protocol information does not match - Expected HTTPS");
                }
                synCtx.setProperty(SynapseConstants.TRANSPORT_DENIED,new Boolean(true));
                synCtx.setProperty(SynapseConstants.IN_TRANSPORT,msgCtx.getTransportIn().getName());
                log.warn("Trying to access API : "+name+" on restricted transport chanel ["+msgCtx.getTransportIn().getName()+"]");
                return false;
            }
        }

        return true;
    }

    void process(MessageContext synCtx) {

        auditDebug("Processing message with ID: " + synCtx.getMessageID() + " through the " +
                    "API: " + name);

        synCtx.setProperty(RESTConstants.SYNAPSE_REST_API, getName());
        synCtx.setProperty(RESTConstants.SYNAPSE_REST_API_VERSION, versionStrategy.getVersion());
        synCtx.setProperty(RESTConstants.REST_API_CONTEXT, context);
        synCtx.setProperty(RESTConstants.SYNAPSE_REST_API_VERSION_STRATEGY, versionStrategy.getVersionType());

        // get API log for this message and attach to the message context
        ((Axis2MessageContext) synCtx).setServiceLog(apiLog);

        // Calculate REST_URL_POSTFIX from full request path
        String restURLPostfix = (String) synCtx.getProperty(RESTConstants.REST_FULL_REQUEST_PATH);
        if (!synCtx.isResponse() && restURLPostfix != null) {  // Skip for response path
            if (!restURLPostfix.startsWith("/")) {
				restURLPostfix = "/" + restURLPostfix;
			} 
			if (restURLPostfix.startsWith(context)) {
				restURLPostfix = restURLPostfix.substring(context.length());				
			}
			if (versionStrategy instanceof URLBasedVersionStrategy) {
				String version = versionStrategy.getVersion();
				if (restURLPostfix.startsWith(version)) {
					restURLPostfix = restURLPostfix.substring(version.length());
				} else if (restURLPostfix.startsWith("/" + version)) {
					restURLPostfix = restURLPostfix.substring(version.length() + 1);
				}
			}
			((Axis2MessageContext) synCtx).getAxis2MessageContext().
							setProperty(NhttpConstants.REST_URL_POSTFIX,restURLPostfix);
		}

        for (Handler handler : handlers) {
            auditDebug("Processing message with ID: " + synCtx.getMessageID() + " through " +
                        "handler: " + handler.getClass().getName());

            boolean proceed;
            if (synCtx.isResponse()) {
                proceed = handler.handleResponse(synCtx);
            } else {
                proceed = handler.handleRequest(synCtx);
            }

            if (!proceed) {
                return;
            }
        }

        if (synCtx.isResponse()) {
            String resourceName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);
            if (resourceName != null) {
                Resource resource = resources.get(resourceName);
                if (resource != null) {
                    resource.process(synCtx);
                }
            } else if (log.isDebugEnabled()) {
                auditDebug("No resource information on the response: " + synCtx.getMessageID());
            }
            return;
        }

        String path = RESTUtils.getFullRequestPath(synCtx);
        String subPath;
        if (versionStrategy.getVersionType().equals(VersionStrategyFactory.TYPE_URL)) {
            //for URL based
            //request --> http://{host:port}/context/version/path/to/resource
            subPath = path.substring(context.length() + versionStrategy.getVersion().length() + 1);
        } else {
            subPath = path.substring(context.length());
        }
        if ("".equals(subPath)) {
            subPath = "/";
        }
        synCtx.setProperty(RESTConstants.REST_SUB_REQUEST_PATH, subPath);

        org.apache.axis2.context.MessageContext msgCtx =
                        ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        String hostHeader = getHostHeader(msgCtx);
        if (hostHeader != null) {
            synCtx.setProperty(RESTConstants.REST_URL_PREFIX,
                    msgCtx.getIncomingTransportName() + "://" + hostHeader);
        }

        Set<Resource> acceptableResources = new LinkedHashSet<Resource>();
        for (Resource r : resources.values()) {
            if (r.canProcess(synCtx)) {
                acceptableResources.add(r);
            }
        }

        boolean processed = false;
        if (!acceptableResources.isEmpty()) {
            for (RESTDispatcher dispatcher : RESTUtils.getDispatchers()) {
                Resource resource = dispatcher.findResource(synCtx, acceptableResources);
                if (resource != null) {
                    if (synCtx.getEnvironment().isDebuggerEnabled()) {
                        if (!synCtx.isResponse()) {
                            SynapseWireLogHolder wireLogHolder = (SynapseWireLogHolder) ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                                    .getProperty(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
                            if (wireLogHolder == null) {
                                wireLogHolder = new SynapseWireLogHolder();
                            }
                            if (synCtx.getProperty(RESTConstants.SYNAPSE_REST_API) != null && !synCtx.getProperty(RESTConstants.SYNAPSE_REST_API).toString().isEmpty()) {
                                wireLogHolder.setApiName(synCtx.getProperty(RESTConstants.SYNAPSE_REST_API).toString());
                                if (resource.getDispatcherHelper() != null) {
                                    if (resource.getDispatcherHelper().getString() != null && !resource.getDispatcherHelper().getString().isEmpty()) {
                                        wireLogHolder.setResourceUrlString(resource.getDispatcherHelper().getString());
                                    }
                                }
                            }
                            ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, wireLogHolder);
                        }

                    }
                    resource.process(synCtx);
                    return;
                }
            }
            handleResourceNotFound(synCtx);
        } else {
            //This will get executed only in unhappy path. So ok to have the iterator.
            boolean resourceFound = false;
            boolean matchingMethodFound = false;
            for (RESTDispatcher dispatcher : RESTUtils.getDispatchers()) {
                Resource resource = dispatcher.findResource(synCtx, resources.values());
                if (resource != null) {
                    resourceFound = true;
                    String method = (String) msgCtx.getProperty(Constants.Configuration.HTTP_METHOD);
                    matchingMethodFound = resource.hasMatchingMethod(method);
                    break;
                }
            }
            if (!resourceFound) {
                handleResourceNotFound(synCtx);
            } else if (resourceFound && !matchingMethodFound) {
                //Resource found, but in that resource, requested method not allowed. So sending method not allowed http status (405)
                msgCtx.setProperty(SynapseConstants.HTTP_SC, HttpStatus.SC_METHOD_NOT_ALLOWED);
                msgCtx.removeProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                msgCtx.setProperty("NIO-ACK-Requested", true);
            } else {
                //Resource found, and matching method also found, which means request is BAD_REQUEST(400)
                msgCtx.setProperty(SynapseConstants.HTTP_SC, HttpStatus.SC_BAD_REQUEST);
                msgCtx.setProperty("NIO-ACK-Requested", true);
            }
        }
    }

    /**
     * Helper method to use when no matching resource found
     *
     * @param synCtx
     */
    private void handleResourceNotFound(MessageContext synCtx) {
        auditDebug("No matching resource was found for the request: " + synCtx.getMessageID());
        Mediator sequence = synCtx.getSequence(RESTConstants.NO_MATCHING_RESOURCE_HANDLER);
        if (sequence != null) {
            sequence.mediate(synCtx);
        } else {
            //Matching resource with method not found
            org.apache.axis2.context.MessageContext msgCtx =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            msgCtx.setProperty(SynapseConstants.HTTP_SC, HttpStatus.SC_NOT_FOUND);
            msgCtx.removeProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            msgCtx.setProperty("NIO-ACK-Requested", true);
        }
    }

    private String getHostHeader(org.apache.axis2.context.MessageContext msgCtx) {
        Map transportHeaders = (Map) msgCtx.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String hostHeader = null;
        if (transportHeaders != null) {
            hostHeader = (String) transportHeaders.get(HTTP.TARGET_HOST);
        }

        if (hostHeader == null) {
            hostHeader = (String) msgCtx.getProperty(NhttpConstants.SERVICE_PREFIX);
        }
        return hostHeader;
    }

    private String extractHostName(String hostHeader) {
        int index = hostHeader.indexOf(':');
        if (index != -1) {
            return hostHeader.substring(0, index);
        } else {
            return hostHeader;
        }
    }

    private int extractPortNumber(String hostHeader, String transport) {
        int index = hostHeader.indexOf(':');
        if (index != -1) {
            return Integer.parseInt(hostHeader.substring(index + 1));
        } else if (Constants.TRANSPORT_HTTP.equals(transport)) {
            return 80;
        } else {
            return 443;
        }
    }

    public void init(SynapseEnvironment se) {
        if (resources.isEmpty()) {
            handleException("The API: " + getName() + " has been configured without " +
                    "any resource definitions");
        }

        auditInfo("Initializing API: " + getName());
        for (Resource resource : resources.values()) {
            resource.init(se);
        }
        
        for (Handler handler : handlers) {
            if (handler instanceof ManagedLifecycle) {
                ((ManagedLifecycle) handler).init(se);
            }
        }
    }

    public void destroy() {
        auditInfo("Destroying API: " + getName());
        for (Resource resource : resources.values()) {
            resource.destroy();
        }

        for (Handler handler : handlers) {
            if (handler instanceof ManagedLifecycle) {
                ((ManagedLifecycle) handler).destroy();
            }
        }
    }

    public VersionStrategy getVersionStrategy() {
        return versionStrategy;
    }

    public void setVersionStrategy(VersionStrategy versionStrategy) {
        this.versionStrategy = versionStrategy;
    }

    public Resource getResource(String resourceName) {
        return resources.get(resourceName);
    }


    private boolean trace() {
        return this.aspectConfiguration.isTracingEnabled();
    }

    /**
     * Write to the general log, as well as any API specific logs the audit message at INFO
     * @param message the INFO level audit message
     */
    private void auditInfo(String message) {
        log.info(message);
        apiLog.info(message);

        //TODO - Implement 'trace' attribute support in API configuration.
        if (trace()) {
            trace.info(message);
        }
    }

    /**
     * Write to the general log, as well as any API specific logs the audit message at DEBUG
     * @param message the DEBUG level audit message
     */
    private void auditDebug(String message) {
        if (log.isDebugEnabled()){
            log.debug(message);
            apiLog.debug(message);

            //TODO - Implement 'trace' attribute support in API configuration.
            if (trace()) {
               trace.debug(message);
            }

        }

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
        String apiId = StatisticIdentityGenerator.getIdForComponent(name, ComponentType.API, holder);
        aspectConfiguration.setUniqueId(apiId);
        for (Resource resource : resources.values()) {
            resource.setComponentStatisticsId(holder);
        }
        StatisticIdentityGenerator.reportingEndEvent(apiId, ComponentType.API, holder);
    }
}
