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

package org.apache.synapse.message.processor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.processor.MessageProcessor;

/**
 * Class <code>AbstractMessageProcessor</code> is handles Message processing of the messages
 * in Message Store. Abstract Message Store is assumes that Message processors can be implemented
 * using the quartz scheduler jobs. If in case we user wants a different implementation They can
 * directly use <code>MessageProcessor</code> interface for that implementations
 */
public abstract class AbstractMessageProcessor implements MessageProcessor {
    private static final Log logger = LogFactory.getLog(AbstractMessageProcessor.class.getName());

    /** Message Store associated with Message processor */
    protected String  messageStore;

    protected String description;

    protected String name;

    protected String fileName;

    protected SynapseConfiguration configuration;

    protected List<MessageConsumer> messageConsumers = new ArrayList<MessageConsumer>();

    /** Name of the artifact container from which the message processor deployed */
    protected String artifactContainerName;

    /** Whether the message processor edited via the management console */
    protected boolean isEdited;

    /** This attribute is only need for forwarding message processor. However, it here because
     * then we don't need to implement this in sampling processor with nothing */
    protected String targetEndpoint;

    /**message store parameters */
    protected Map<String, Object> parameters = null;

    @Override
    public void init(SynapseEnvironment se) {
        configuration = se.getSynapseConfiguration();
    }

    @Override
    public void setMessageStoreName(String  messageStore) {
        if (messageStore != null) {
            this.messageStore = messageStore;
        } else {
            throw new SynapseException("Error Can't set Message store to null");
        }
    }

    @Override
    public String getMessageStoreName() {
        return messageStore;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setDescription(String description) {
        this.description=description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setFileName(String filename) {
        this.fileName = filename;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public List<MessageConsumer> getMessageConsumer() {
        return messageConsumers;
    }

    @Override
    public boolean setMessageConsumer(MessageConsumer consumer) {
        if (consumer == null) {
            logger.error("[" + getName() + "] Faulty message consumer.");
            return false;
        }
        messageConsumers.add(consumer);

        return true;
    }

    @Override
    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    @Override
    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    /**
     * Whether the message processor edited through the management console
     * @return isEdited
     */
    public boolean isEdited() {
        return isEdited;
    }

    /**
     * Set whether the message processor edited through the management console
     * @param isEdited
     */
    public void setIsEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    /**
     * Get the name of the artifact container from which the message processor deployed
     * @return artifactContainerName
     */
    public String getArtifactContainerName() {
        return artifactContainerName;
    }

    /**
     * Set the name of the artifact container from which the message processor deployed
     * @param artifactContainerName
     */
    public void setArtifactContainerName(String artifactContainerName) {
        this.artifactContainerName = artifactContainerName;
    }
}
