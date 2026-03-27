/*
 * Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators;

import org.apache.synapse.Mediator;

/**
 * Represents a child sequence within a mediator (e.g., on-accept, on-reject, on-cache-hit).
 * Used by {@link MediatorWithChildren} to expose child sequence information to the mediator registry.
 */
public class ChildSequence {
    
    /** The name/identifier of the child sequence (e.g., "on-accept", "on-reject", "on-cache-hit") */
    private final String name;
    
    /** The inline mediator or sequence for this child, or null if using a sequence reference */
    private final Mediator mediator;
    
    /** The sequence key reference for this child, or null if using inline mediator */
    private final String sequenceKey;
    
    /**
     * Create a child sequence.
     * 
     * @param name the name of the child sequence (e.g., "on-accept", "on-reject", "on-cache-hit")
     * @param mediator the inline mediator (may be null)
     * @param sequenceKey the sequence reference (may be null)
     */
    public ChildSequence(String name, Mediator mediator, String sequenceKey) {
        this.name = name;
        this.mediator = mediator;
        this.sequenceKey = sequenceKey;
    }
    
    /**
     * Get the child sequence name.
     * 
     * @return the child sequence name (e.g., "on-accept", "on-reject", "on-cache-hit")
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the inline mediator for this child sequence.
     * 
     * @return the inline mediator, or null if using a sequence reference
     */
    public Mediator getMediator() {
        return mediator;
    }
    
    /**
     * Get the sequence key reference for this child sequence.
     * 
     * @return the sequence key, or null if using inline mediator
     */
    public String getSequenceKey() {
        return sequenceKey;
    }
    
    /**
     * Check if this child sequence uses an inline mediator.
     * 
     * @return true if inline mediator is present
     */
    public boolean hasInlineMediator() {
        return mediator != null;
    }
    
    /**
     * Check if this child sequence uses a sequence reference.
     * 
     * @return true if sequence key is present
     */
    public boolean hasSequenceReference() {
        return sequenceKey != null && !sequenceKey.isEmpty();
    }
}
