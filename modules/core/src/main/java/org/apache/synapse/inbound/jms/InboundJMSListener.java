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

package org.apache.synapse.inbound.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.inbound.PollingProcessor;
import org.apache.synapse.inbound.jms.factory.CachedJMSConnectionFactory;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InboundJMSListener implements PollingProcessor {
    private static final Log log = LogFactory.getLog(InboundJMSListener.class.getName());


    private CachedJMSConnectionFactory jmsConnectionFactory;
    private JMSPollingConsumer pollingConsumer;
    private String name;
    private Properties jmsProperties;
    private long interval;
    private String injectingSeq;
    private String onErrorSeq;
    private SynapseEnvironment synapseEnvironment;


    public InboundJMSListener(String name, Properties jmsProperties, long pollInterval, String injectingSeq, String onErrorSeq, SynapseEnvironment synapseEnvironment) {
        this.name = name;
        this.jmsProperties = jmsProperties;
        this.interval = pollInterval;
        this.injectingSeq = injectingSeq;
        this.onErrorSeq = onErrorSeq;
        this.synapseEnvironment = synapseEnvironment;
    }

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public void init() {
        log.info("Initializing inbound JMS listener for destination " + name);
        jmsConnectionFactory = new CachedJMSConnectionFactory(this.jmsProperties);
        pollingConsumer = new JMSPollingConsumer(jmsConnectionFactory, injectingSeq, onErrorSeq, synapseEnvironment);        
        pollingConsumer.registerHandler(new JMSInjectHandler(injectingSeq, onErrorSeq, synapseEnvironment, jmsProperties));
        scheduledExecutorService.scheduleAtFixedRate(pollingConsumer, 0, this.interval, TimeUnit.SECONDS);
    }

    
    public void destroy() {
        log.info("Inbound JMS listener ended for destination " + name);
        scheduledExecutorService.shutdown();
    }    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
