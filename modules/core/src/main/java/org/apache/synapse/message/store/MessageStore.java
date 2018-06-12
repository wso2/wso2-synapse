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

package org.apache.synapse.message.store;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Nameable;
import org.apache.synapse.SynapseArtifact;
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.MessageProducer;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public interface MessageStore extends ManagedLifecycle, Nameable, SynapseArtifact {

    /**
     * Returns a Message Producer for this message store. <br/>
     *
     * @return A non-null message producer that can produce messages to this message store.
     */
    MessageProducer getProducer() throws SynapseException;

    /**
     * Returns a Message Consumer for this message store. <br/>
     *
     * @return A non-null message consumer that can read messages from this message store.<br/>
     */
    MessageConsumer getConsumer() throws SynapseException;

    /**
     * set the implementation specific parameters
     *
     * @param parameters A map of parameters or null
     */
    void setParameters(Map<String, Object> parameters);

    /**
     * get the implementation specific parameters of the Message store
     *
     * @return a properties map
     */
    Map<String, Object> getParameters();

    /**
     * Adds message store specific parameters
     *
     * @param name parameter name
     * @param key  parameter key value
     */
    void addParameter(String name, String key);

    /**
     * Adds message store parameter registry keys
     *
     * @param name parameter name
     * @param key  parameter registry key value
     */
    void addParameterKey(String name, String key);

    /**
     * Gets parameter registry key from the parameter name
     *
     * @param name parameter name
     * @return registry key of parameter value
     */
    String getParameterKey(String name);

    /**
     * Gets registry key mappings for the parameters
     *
     * @return registry key maps for parameter values
     */
    Map<String, String> getParameterKeyMap();

    /**
     * Set the name of the file that the Message store is configured
     *
     * @param filename Name of the file where this artifact is defined
     */
    void setFileName(String filename);

    /**
     * get the file name that the message store is configured
     *
     * @return Name of the file where this artifact is defined
     */
    String getFileName();

    /**
     * Returns the type of this message store. <br/>
     * The type of a message store can be one of following types, <br/>
     * {@link Constants#JMS_MS}, {@link Constants#INMEMORY_MS},
     * or {@link Constants#JDBC_MS}
     *
     * @return Type of the message store.
     */
    int getType();

    /**
     * Retrieves and removes the first Message in this store.
     * Message ordering will depend on the underlying implementation
     *
     * @return first message context in the store
     * @throws java.util.NoSuchElementException if store is empty
     */
    MessageContext remove() throws NoSuchElementException;

    /**
     * Delete all the Messages in the Message Store
     */
    void clear();

    /**
     * Delete and return the MessageContext with given Message id
     *
     * @param messageID message id of the Message
     * @return MessageContext instance
     */
    MessageContext remove(String messageID);

    /**
     * Returns the number of Messages  in this store.
     *
     * @return the number of Messages in this Store
     */
    int size();

    /**
     * Return the Message in given index position
     * (this may depend on the implementation)
     *
     * @param index position of the message
     * @return Message in given index position
     */
    MessageContext get(int index);

    /**
     * Get the All messages in the Message store without removing them from the queue
     *
     * @return List of all Messages
     */
    List<MessageContext> getAll();

    /**
     * Get the Message with the given ID from the Message store without removing it
     *
     * @param messageId A message ID string
     * @return Message with given ID
     */
    MessageContext get(String messageId);

    /**
     * Whether the message store edited through the management console
     *
     * @return true if Message Store config is locally edited
     */
    boolean isEdited();

    /**
     * Set whether the message store edited through the management console
     *
     * @param isEdited true if Message Store config is locally edited
     */
    void setIsEdited(boolean isEdited);

    /**
     * Get the name of the artifact container from which the message store deployed
     *
     * @return Name of artifact container
     */
    String getArtifactContainerName();

    /**
     * Set the name of the artifact container from which the message store deployed
     *
     * @param artifactContainerName name of artifact container
     */
    void setArtifactContainerName(String artifactContainerName);
}
