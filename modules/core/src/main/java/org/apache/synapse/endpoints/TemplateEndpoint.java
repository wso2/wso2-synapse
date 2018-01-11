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

package org.apache.synapse.endpoints;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

import java.util.HashMap;
import java.util.Map;

public class TemplateEndpoint extends AbstractEndpoint {
    private static final Log log = LogFactory.getLog(TemplateEndpoint.class);

    private String template = null;

    private Endpoint realEndpoint = null;

    private Map<String, String> parameters = new HashMap<String, String>();

    private String address = null;

    @Override
    public void send(MessageContext synCtx) {
        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            Integer currentIndex = null;
            if (getDefinition() != null) {
                currentIndex = OpenEventCollector.reportChildEntryEvent(synCtx, getReportingName(),
                        ComponentType.ENDPOINT, getDefinition().getAspectConfiguration(), true);
            }
            try {
                sendMessage(synCtx);
            } finally {
                if (currentIndex != null) {
                    CloseEventCollector.closeEntryEvent(synCtx, getReportingName(), ComponentType.MEDIATOR,
                            currentIndex, false);
                }
            }
        } else {
            sendMessage(synCtx);
        }
    }

    public void sendMessage(MessageContext synCtx) {
        reLoadAndInitEndpoint(synCtx.getEnvironment());

        if (realEndpoint != null) {
            realEndpoint.send(synCtx);
        } else {
            informFailure(synCtx, SynapseConstants.ENDPOINT_IN_DIRECT_NOT_READY,
                    "Couldn't find the endpoint with the name " + getName() +
                            " & template : " + template);
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameterValue(String name) {
        return parameters.get(name);
    }

    public void addParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        super.init(synapseEnvironment);

        Template endpointTemplate = synapseEnvironment.getSynapseConfiguration().
                getEndpointTemplate(template);

        if (endpointTemplate == null) {
            //if template is not already available we will warn the user
            //thus template endpoint will get initalized at runtime
            log.warn("Template " + template +
                    " cannot be found for the endpoint " + getName());
            return;
        }

        reLoadAndInitEndpoint(synapseEnvironment);
    }

    /**
     * Reload as needed , either from registry , local entries or predefined endpoints
     * @param se synapse environment
     */
    private synchronized void reLoadAndInitEndpoint(SynapseEnvironment se) {
        SynapseConfiguration synCfg = se.getSynapseConfiguration();
        long previousEntryVersion = -1;//used to keep track of template changes in registry
        //always do reloading at init
        boolean reLoad = (realEndpoint == null);
        Entry entry = synCfg.getEntryDefinition(template);
        if (!reLoad) {
            if (entry != null && entry.isDynamic()) {
                //get previous entry version to track template changes
                previousEntryVersion = entry.getVersion();
                if (!entry.isCached() || entry.isExpired()) {
                    MBeanRegistrar.getInstance().unRegisterMBean("Endpoint", this.getName());
                    reLoad = true;
                }
            } else {
                // this endpoint is static -->
                // since template-endpoint is static, should ONLY be loaded at initialization to prevent
                // reloading every single time this endpoint is executed..
                // incase tempalate config has changed this endpoint should be redeployed
                reLoad = false;
            }
        }

        if (reLoad) {
            if (log.isDebugEnabled()) {
                log.debug("Loading template endpoint with key : " + template);
            }

            Template eprTemplate = synCfg.getEndpointTemplate(template);

            if (eprTemplate != null) {
                //create real EP if not already created or if there is any change in template
                if (realEndpoint == null || previousEntryVersion != entry.getVersion()) {
                    realEndpoint = eprTemplate.create(this, synCfg.getProperties());
                }
            } else {
                log.warn("Couldn't retrieve the endpoint template with the key:" + template);
            }

            if (realEndpoint != null && !realEndpoint.isInitialized()) {
                realEndpoint.init(se);
            }
        }
    }

    public boolean readyToSend() {
        if (realEndpoint != null && realEndpoint.readyToSend()) {
            if (log.isDebugEnabled()) {
                log.debug("Template Endpoint" + this.toString()
                          + " is at ready state");
            }
            return true;
        }

        if (log.isDebugEnabled()) {
        	log.debug("Template Endpoint " + this.toString()
                 + " is not in a ready state to process message");
        }

        return false;
    }
    
    
	public Endpoint getRealEndpoint() {
		return realEndpoint;
	}
}
