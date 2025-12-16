/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.unittest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.api.API;
import org.apache.synapse.api.Resource;
import org.apache.synapse.config.xml.SwitchCase;
import org.apache.synapse.mediators.ListMediator;
import org.apache.synapse.mediators.base.SequenceMediator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracker for monitoring mediator execution coverage during unit tests.
 */
public class MediatorCoverageTracker {

    private static Log log = LogFactory.getLog(MediatorCoverageTracker.class.getName());

    // Thread-safe singleton instance
    private static final MediatorCoverageTracker instance = new MediatorCoverageTracker();

    // Map to store all mediators for each artifact: artifactKey -> mediatorId -> mediator
    private final Map<String, Map<String, Mediator>> allMediators = new ConcurrentHashMap<>();

    // Set to track executed mediators: artifactKey -> set of executed mediatorIds
    private final Map<String, Set<String>> executedMediators = new ConcurrentHashMap<>();

    // Counter for generating unique mediator IDs
    private final Map<String, AtomicInteger> mediatorCounters = new ConcurrentHashMap<>();

    private MediatorCoverageTracker() {
    }

    /**
     * Get singleton instance.
     *
     * @return singleton instance
     */
    public static MediatorCoverageTracker getInstance() {
        return instance;
    }

    /**
     * Register an API for coverage tracking.
     *
     * @param api API to track
     */
    public void registerAPI(API api) {
        String artifactKey = "API:" + api.getName();
        log.info("Registering API for coverage tracking: " + api.getName());

        Map<String, Mediator> mediatorMap = new HashMap<>();
        mediatorCounters.put(artifactKey, new AtomicInteger(0));

        // Track mediators in all resources
        int resourceIndex = 0;
        for (Resource resource : api.getResources()) {
            String resourcePath = "api/" + api.getName() + "/res" + resourceIndex;
            
            // Track inSequence
            if (resource.getInSequence() != null) {
                registerMediatorRecursively(resource.getInSequence(), artifactKey, 
                        resourcePath + "/in", 
                        mediatorMap);
            }
            // Track outSequence
            if (resource.getOutSequence() != null) {
                registerMediatorRecursively(resource.getOutSequence(), artifactKey,
                        resourcePath + "/out",
                        mediatorMap);
            }
            // Track faultSequence
            if (resource.getFaultSequence() != null) {
                registerMediatorRecursively(resource.getFaultSequence(), artifactKey,
                        resourcePath + "/fault",
                        mediatorMap);
            }
            resourceIndex++;
        }

        allMediators.put(artifactKey, mediatorMap);
        executedMediators.put(artifactKey, ConcurrentHashMap.newKeySet());
        
        log.info("Registered API " + api.getName() + " with " + mediatorMap.size() + " mediators");
    }

    /**
     * Register a Sequence for coverage tracking.
     *
     * @param sequence Sequence to track
     */
    public void registerSequence(SequenceMediator sequence) {
        String artifactKey = "Sequence:" + sequence.getName();
        log.info("Registering Sequence for coverage tracking: " + sequence.getName());

        Map<String, Mediator> mediatorMap = new HashMap<>();
        mediatorCounters.put(artifactKey, new AtomicInteger(0));

        registerMediatorRecursively(sequence, artifactKey, "seq/" + sequence.getName(), 
                mediatorMap);

        allMediators.put(artifactKey, mediatorMap);
        executedMediators.put(artifactKey, ConcurrentHashMap.newKeySet());
        
        log.info("Registered Sequence " + sequence.getName() + " with " + mediatorMap.size() + " mediators");
    }

    /**
     * Recursively register all mediators in a mediator tree.
     *
     * @param mediator     mediator to register
     * @param artifactKey  artifact key
     * @param path         path prefix for identification
     * @param mediatorMap  map to store mediators
     */
    private void registerMediatorRecursively(Mediator mediator, String artifactKey, 
                                            String path, Map<String, Mediator> mediatorMap) {
        if (mediator == null) {
            return;
        }

        // Generate unique ID for this mediator with improved format
        int index = mediatorCounters.get(artifactKey).incrementAndGet();
        String mediatorType = mediator.getClass().getSimpleName();
        String mediatorId = path + "/" + index + ":" + mediatorType;

        mediatorMap.put(mediatorId, mediator);

        // If this is a list mediator, recursively register children
        if (mediator instanceof ListMediator) {
            ListMediator listMediator = (ListMediator) mediator;
            List<Mediator> children = listMediator.getList();
            
            if (children != null && !children.isEmpty()) {
                for (int i = 0; i < children.size(); i++) {
                    Mediator child = children.get(i);
                    String childPath = mediatorId;
                    registerMediatorRecursively(child, artifactKey, childPath, mediatorMap);
                }
            }
        }
        
        // Special handling for FilterMediator to register else branch
        if (mediator instanceof org.apache.synapse.mediators.filters.FilterMediator) {
            org.apache.synapse.mediators.filters.FilterMediator filterMediator = 
                    (org.apache.synapse.mediators.filters.FilterMediator) mediator;
            
            // Register the else mediator if it exists
            if (filterMediator.getElseMediator() != null) {
                ListMediator elseMediator = filterMediator.getElseMediator();
                List<Mediator> elseChildren = elseMediator.getList();
                
                if (elseChildren != null && !elseChildren.isEmpty()) {
                    for (Mediator elseChild : elseChildren) {
                        registerMediatorRecursively(elseChild, artifactKey, mediatorId, mediatorMap);
                    }
                }
            }
        }
        
        // Special handling for SwitchMediator to register all case branches
        if (mediator instanceof org.apache.synapse.mediators.filters.SwitchMediator) {
            org.apache.synapse.mediators.filters.SwitchMediator switchMediator = 
                    (org.apache.synapse.mediators.filters.SwitchMediator) mediator;
            
            // Register all case mediators
            List<SwitchCase> cases = switchMediator.getCases();
            if (cases != null && !cases.isEmpty()) {
                for (SwitchCase switchCase : cases) {
                    Mediator caseMediator = switchCase.getCaseMediator();
                    if (caseMediator != null) {
                        registerMediatorRecursively(caseMediator, artifactKey, mediatorId, mediatorMap);
                    }
                }
            }
            
            // Register default case mediator if it exists
            SwitchCase defaultCase = switchMediator.getDefaultCase();
            if (defaultCase != null && defaultCase.getCaseMediator() != null) {
                registerMediatorRecursively(defaultCase.getCaseMediator(), artifactKey, 
                        mediatorId, mediatorMap);
            }
        }
    }

    /**
     * Mark a mediator as executed.
     *
     * @param mediator     executed mediator
     * @param artifactKey  artifact key
     */
    public void markMediatorExecuted(Mediator mediator, String artifactKey) {
        if (mediator == null || artifactKey == null) {
            return;
        }

        Map<String, Mediator> mediatorMap = allMediators.get(artifactKey);
        if (mediatorMap == null) {
            return;
        }

        // Find the mediator ID by comparing object references
        for (Map.Entry<String, Mediator> entry : mediatorMap.entrySet()) {
            if (entry.getValue() == mediator) {
                Set<String> executed = executedMediators.get(artifactKey);
                if (executed != null) {
                    executed.add(entry.getKey());
                    log.debug("Marked mediator as executed: " + entry.getKey());
                }
                break;
            }
        }
    }

    /**
     * Get total mediator count for an artifact.
     *
     * @param artifactKey artifact key
     * @return total mediator count
     */
    public int getTotalMediatorCount(String artifactKey) {
        Map<String, Mediator> mediatorMap = allMediators.get(artifactKey);
        return mediatorMap != null ? mediatorMap.size() : 0;
    }

    /**
     * Get executed mediator count for an artifact.
     *
     * @param artifactKey artifact key
     * @return executed mediator count
     */
    public int getExecutedMediatorCount(String artifactKey) {
        Set<String> executed = executedMediators.get(artifactKey);
        return executed != null ? executed.size() : 0;
    }

    /**
     * Get list of executed mediator identifiers.
     *
     * @param artifactKey artifact key
     * @return set of executed mediator identifiers
     */
    public Set<String> getExecutedMediatorIds(String artifactKey) {
        Set<String> executed = executedMediators.get(artifactKey);
        return executed != null ? new HashSet<>(executed) : new HashSet<>();
    }

    /**
     * Get all registered artifact keys.
     *
     * @return set of artifact keys
     */
    public Set<String> getRegisteredArtifacts() {
        return new HashSet<>(allMediators.keySet());
    }

    /**
     * Clear all tracking data.
     */
    public void clear() {
        allMediators.clear();
        executedMediators.clear();
        mediatorCounters.clear();
        log.info("Cleared all mediator coverage tracking data");
    }

    /**
     * Clear tracking data for a specific artifact.
     *
     * @param artifactKey artifact key
     */
    public void clearArtifact(String artifactKey) {
        allMediators.remove(artifactKey);
        executedMediators.remove(artifactKey);
        mediatorCounters.remove(artifactKey);
        log.info("Cleared mediator coverage tracking data for: " + artifactKey);
    }

    /**
     * Reset execution tracking (keep registered mediators but clear execution data).
     */
    public void resetExecution() {
        for (String key : executedMediators.keySet()) {
            executedMediators.get(key).clear();
        }
        log.info("Reset mediator execution tracking");
    }
}
