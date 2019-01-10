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
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
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
    private Map<String, String> parametersMap = new LinkedHashMap<String, String>();
    private Map<String, String> parameterKeyMap = new LinkedHashMap<String, String>();
    private String fileName;
    private SynapseEnvironment synapseEnvironment;
    private InboundRequestProcessor inboundRequestProcessor;
    /** car file name which this endpoint deployed from */
    private String artifactContainerName;
    /** Whether the deployed inbound endpoint is edited via the management console */
    private boolean isEdited;
    private AspectConfiguration aspectConfiguration;
    /** regex for secure vault expression */
    private static final String secureVaultRegex = "\\{wso2:vault-lookup\\('(.*?)'\\)\\}";

    public void init(SynapseEnvironment se) {
        log.info("Initializing Inbound Endpoint: " + getName());
        synapseEnvironment = se;
        if(isSuspend){
      	  log.info("Inbound endpoint " + name + " is currently suspended.");
      	  return;
        }
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
        Iterator<InboundRequestProcessorFactory> it = java.util.ServiceLoader.load(InboundRequestProcessorFactory.class).iterator();
        InboundProcessorParams params = populateParams();
        while (it.hasNext()) {
            InboundRequestProcessorFactory factory = it.next();
            InboundRequestProcessor inboundRequestProcessor =
                                factory.createInboundProcessor(params);
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

        Properties props = Utils.paramsToProperties(parametersMap);
        //replacing values by secure vault
        resolveSecureVaultExpressions(props);
        inboundProcessorParams.setProperties(props);
        return inboundProcessorParams;
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

    private void resolveSecureVaultExpressions(Properties props) {
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
}
