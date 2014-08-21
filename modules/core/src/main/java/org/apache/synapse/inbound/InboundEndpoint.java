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
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.xml.MediatorSerializer;
import org.apache.synapse.core.SynapseEnvironment;

import sun.misc.Service;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class InboundEndpoint implements ManagedLifecycle {

    protected Log log = LogFactory.getLog(InboundEndpoint.class);

    private String name;
    private String protocol;
    private String classImpl;
    private boolean isSuspend;
    private String injectingSeq;
    private String onErrorSeq;
    private String outSequence;
    private Map<String, String> parametersMap = new LinkedHashMap<String, String>();
    private String fileName;
    private SynapseEnvironment synapseEnvironment;
    private InboundRequestProcessor inboundRequestProcessor;


    public InboundEndpoint() {

    }

    public void init(SynapseEnvironment se) {
        log.info("Initializing Inbound Endpoint: " + getName());
        synapseEnvironment = se;
        inboundRequestProcessor = getInboundRequestProcessor();
        if (inboundRequestProcessor != null) {
            inboundRequestProcessor.init();
        } else {
            log.error("Inbound Request processor not found for Inbound EP : " + name + " Protocol: " + protocol
                    + " Class" + classImpl);
        }
        InboundResponseSender inboundResponseSender = getInboundResponseSender();
        registerResponseSenders(inboundResponseSender, inboundResponseSender.getType());

    }

    /**
     * Register pluggables from the classpath
     * <p/>
     * This looks for JAR files containing a META-INF/services that adheres to the following
     * http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html
     */
    private InboundRequestProcessor getInboundRequestProcessor() {
        if (log.isDebugEnabled()) {
            log.debug("Registering InboundRequestProcessor found in the classpath.. ");
        }
        Iterator<InboundRequestProcessorFactory> it = Service.providers(InboundRequestProcessorFactory.class);
        while (it.hasNext()) {
            InboundRequestProcessorFactory factory = it.next();
            Properties properties = Utils.paramsToProperties(parametersMap);

            InboundRequestProcessor inboundRequestProcessor = factory.createInboundProcessor(protocol, classImpl, name,
                    properties, injectingSeq, onErrorSeq,
                    synapseEnvironment);
            if (inboundRequestProcessor != null) {
                return inboundRequestProcessor;
            }
        }
        return null;
    }

    private InboundResponseSender getInboundResponseSender() {
        if (log.isDebugEnabled()) {
            log.debug("Registering InboundResponseProcessor found in the classpath.. ");
        }
        Iterator<InboundResponseSenderFactory> it = Service.providers(InboundResponseSenderFactory.class);
        while (it.hasNext()) {
            InboundResponseSenderFactory factory = it.next();
            if (protocol != null) {
                InboundResponseSender inboundResponseSender = factory.getInboundResponseSender(protocol);
                if (inboundResponseSender != null) {
                    return inboundResponseSender;
                }
            }
        }
        return null;
    }

    private void registerResponseSenders(InboundResponseSender inboundResponseSender, String type) {
        InboundEndpointUtils.addResponseSender(type, inboundResponseSender);
    }

    public void destroy() {
        log.info("Destroying Inbound Endpoint: " + getName());
        if (inboundRequestProcessor != null) {
            inboundRequestProcessor.destroy();
        }
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

    public String getParameter(String name) {
        return parametersMap.get(name);
    }

    public String getOutSequence() {
        return outSequence;
    }

    public void setOutSequence(String outSequence) {
        this.outSequence = outSequence;
    }

    public String getClassImpl() {
        return classImpl;
    }

    public void setClassImpl(String classImpl) {
        this.classImpl = classImpl;
    }
}
