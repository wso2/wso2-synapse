/*
 *  Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.inbound;

import org.apache.synapse.core.SynapseEnvironment;

import java.util.Properties;

/**
 * This is the entity which holds parameters which is related to an InboundEndpoint
 */
public class InboundProcessorParams {

    private String name;
    private String protocol;
    private String classImpl;
    private Properties properties;
    private String injectingSeq;
    private String onErrorSeq;
    private SynapseEnvironment synapseEnvironment;

    /**
     * Get the name of the inbound endpoint
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the inbound endpoint
     *
     * @param name name of the endpoint
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Protocol of inbound endpoint.
     * <p/>
     * This may become null if classImpl is used
     *
     * @return protocol of InboundEndpoint
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set the protocol of the Inbound Endpoint
     *
     * @param protocol protocol name
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Get the custom class implementation of InboundEndpoint
     * This will return the FQN of the InboundEndpoint custom class implementation.
     *
     * @return FQN of class implementation
     */
    public String getClassImpl() {
        return classImpl;
    }

    /**
     * Set the custom class implementation of InboundEndpoint
     * @param classImpl FQN of custom class implementation
     */
    public void setClassImpl(String classImpl) {
        this.classImpl = classImpl;
    }

    /**
     * Get the properties associated with the InboundEndpoint
     *
     * @return associated properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Set properties for InboundEndpoint
     *
     * @param properties properties
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Get the sequence which message should be dispatched to
     *
     * @return sequence name
     */
    public String getInjectingSeq() {
        return injectingSeq;
    }

    /**
     * Set the sequence which message should be dispatched to
     *
     * @param injectingSeq name of the sequence
     */
    public void setInjectingSeq(String injectingSeq) {
        this.injectingSeq = injectingSeq;
    }

    /**
     * Get the sequence which get invoked when something goes wrong in InboundEndpoint
     *
     * @return onError sequence for InboundEndpoint
     */
    public String getOnErrorSeq() {
        return onErrorSeq;
    }

    /**
     * Set the sequence which get invoked when something goes wrong in InboundEndpoint
     *
     * @param onErrorSeq onError sequence name
     */
    public void setOnErrorSeq(String onErrorSeq) {
        this.onErrorSeq = onErrorSeq;
    }

    /**
     * Get the synapse environment
     *
     * @return synapse environment
     */
    public SynapseEnvironment getSynapseEnvironment() {
        return synapseEnvironment;
    }

    /**
     * Set the synapse environment
     *
     * @param synapseEnvironment synapse environment
     */
    public void setSynapseEnvironment(SynapseEnvironment synapseEnvironment) {
        this.synapseEnvironment = synapseEnvironment;
    }

}
