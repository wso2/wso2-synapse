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
    private long interval;
    private boolean isSuspend;

    private String injectingSeq;
    private String onErrorSeq;
    private Map<String,String> parametersMap = new LinkedHashMap<String,String>();

    private String fileName;
    PollingProcessor pollingProcessor;
    private SynapseEnvironment synapseEnvironment;

    public InboundEndpoint() {

    }

    public void init(SynapseEnvironment se) {
        log.info("Initializing Inbound Endpoint: " + getName());
        synapseEnvironment = se;
        pollingProcessor = getPollingProcessor();
        if(pollingProcessor != null){
        	pollingProcessor.init();
        }else{ 
        	log.error("Polling processor not found for Inbound EP : " + name + " Protocol: " + protocol + " Class" + classImpl);
        }
    }
    /**
     * Register pluggable mediator serializers from the classpath
     *
     * This looks for JAR files containing a META-INF/services that adheres to the following
     * http://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html
     */
    private PollingProcessor getPollingProcessor(){
        if (log.isDebugEnabled()) {
            log.debug("Registering mediator extensions found in the classpath.. ");
        }
        // Get polling processors
        Iterator<PollingProcessorFactory>it = Service.providers(PollingProcessorFactory.class);
        while (it.hasNext()) {
        	PollingProcessorFactory factory =  it.next();
        	Properties properties = Utils.paramsToProperties(parametersMap);
        	return factory.creatPollingProcessor(protocol, classImpl, name, properties, interval, injectingSeq, onErrorSeq, synapseEnvironment);
        }
        return null;
    }
    
    public void destroy() {
        log.info("Destroying Inbound Endpoint: " + getName());
        if(pollingProcessor != null){
        	pollingProcessor.destroy();
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

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
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

    public void setParametersMap(Map<String, String> parametersMap) {
        this.parametersMap = parametersMap;
    }

    public void addParameter(String name, String value) {
        parametersMap.put(name, value);
    }

    public String getParameter(String name) {
        return parametersMap.get(name);
    }

	public String getClassImpl() {
		return classImpl;
	}

	public void setClassImpl(String classImpl) {
		this.classImpl = classImpl;
	}

}
