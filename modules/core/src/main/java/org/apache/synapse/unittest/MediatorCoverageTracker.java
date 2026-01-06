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
import org.apache.synapse.mediators.builtin.PropertyMediator;
import org.apache.synapse.mediators.elementary.EnrichMediator;
import org.apache.synapse.mediators.filters.FilterMediator;
import org.apache.synapse.mediators.filters.SwitchMediator;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    // MediatorCoverageTracker singleton instance
    private static final MediatorCoverageTracker instance = new MediatorCoverageTracker();

    // Map to store all mediators for each artifact: artifactKey -> mediatorId -> mediator
    private final Map<String, Map<String, Mediator>> allMediators = new ConcurrentHashMap<>();

    // Set to track executed mediators: artifactKey -> set of executed mediatorIds
    private final Map<String, Set<String>> executedMediators = new ConcurrentHashMap<>();

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

        Map<String, Mediator> mediatorMap = new LinkedHashMap<>();

        // Track mediators in all resources
        for (Resource resource : api.getResources()) {
            // Build resource identifier: method[uriTemplate]
            String[] methods = resource.getMethods();
            String methodStr = (methods != null && methods.length > 0) ? 
                    String.join(",", methods) : "ANY";
            
            String uriTemplate = "/";
            if (resource.getDispatcherHelper() != null) {
                String helperString = resource.getDispatcherHelper().getString();
                if (helperString != null && !helperString.isEmpty()) {
                    uriTemplate = helperString;
                }
            }
            
            String resourcePath = "api:" + api.getName() + "/" + methodStr + "[" + uriTemplate + "]";
            
            // Track inSequence
            if (resource.getInSequence() != null) {
                registerMediatorRecursively(resource.getInSequence(), artifactKey, 
                        resourcePath + "/in", 
                        mediatorMap, new AtomicInteger(0));
            }
            // Track outSequence
            if (resource.getOutSequence() != null) {
                registerMediatorRecursively(resource.getOutSequence(), artifactKey,
                        resourcePath + "/out",
                        mediatorMap, new AtomicInteger(0));
            }
            // Track faultSequence
            if (resource.getFaultSequence() != null) {
                registerMediatorRecursively(resource.getFaultSequence(), artifactKey,
                        resourcePath + "/fault",
                        mediatorMap, new AtomicInteger(0));
            }
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

        Map<String, Mediator> mediatorMap = new LinkedHashMap<>();

        registerMediatorRecursively(sequence, artifactKey, "sequence:" + sequence.getName(), 
                mediatorMap, new AtomicInteger(0));

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
     * @param positionCounter counter for mediator positions at current level
     */
    private void registerMediatorRecursively(Mediator mediator, String artifactKey, 
                                            String path, Map<String, Mediator> mediatorMap,
                                            AtomicInteger positionCounter) {
        if (mediator == null) {
            return;
        }

        // Get mediator type without "Mediator" suffix for cleaner names
        String mediatorType = mediator.getClass().getSimpleName();
        if (mediatorType.endsWith("Mediator")) {
            mediatorType = mediatorType.substring(0, mediatorType.length() - 8);
        }
        
        // Generate position-based ID: path/position.Type
        int position = positionCounter.incrementAndGet();
        String mediatorId = path + "/" + position + "." + mediatorType;
        
        // Add context for specific mediators
        if (mediator instanceof PropertyMediator) {
            PropertyMediator propMediator =
                    (PropertyMediator) mediator;
            if (propMediator.getName() != null) {
                mediatorId += "[" + propMediator.getName() + "]";
            }
        } else if (mediator instanceof EnrichMediator) {
            mediatorId += "[enrich]";
        }

        mediatorMap.put(mediatorId, mediator);

        // Special handling for FilterMediator to register then/else branches
        if (mediator instanceof FilterMediator) {
            FilterMediator filterMediator = (FilterMediator) mediator;
            
            // Register then branch
            List<Mediator> thenChildren = filterMediator.getList();
            if (thenChildren != null && !thenChildren.isEmpty()) {
                AtomicInteger thenCounter = new AtomicInteger(0);
                for (Mediator thenChild : thenChildren) {
                    registerMediatorRecursively(thenChild, artifactKey, mediatorId + "/then", 
                            mediatorMap, thenCounter);
                }
            }
            
            // Register else branch
            if (filterMediator.getElseMediator() != null) {
                ListMediator elseMediator = filterMediator.getElseMediator();
                List<Mediator> elseChildren = elseMediator.getList();
                
                if (elseChildren != null && !elseChildren.isEmpty()) {
                    AtomicInteger elseCounter = new AtomicInteger(0);
                    for (Mediator elseChild : elseChildren) {
                        registerMediatorRecursively(elseChild, artifactKey, mediatorId + "/else", 
                                mediatorMap, elseCounter);
                    }
                }
            }
        }
        // Special handling for SwitchMediator to register case branches
        else if (mediator instanceof SwitchMediator) {
            SwitchMediator switchMediator = (SwitchMediator) mediator;
            
            // Register all case mediators
            List<SwitchCase> cases = switchMediator.getCases();
            if (cases != null && !cases.isEmpty()) {
                for (int i = 0; i < cases.size(); i++) {
                    SwitchCase switchCase = cases.get(i);
                    Mediator caseMediator = switchCase.getCaseMediator();
                    if (caseMediator instanceof ListMediator) {
                        ListMediator caseList = (ListMediator) caseMediator;
                        List<Mediator> caseChildren = caseList.getList();
                        if (caseChildren != null && !caseChildren.isEmpty()) {
                            AtomicInteger caseCounter = new AtomicInteger(0);
                            String casePath = mediatorId + "/case[" + (i + 1) + "]";
                            for (Mediator caseChild : caseChildren) {
                                registerMediatorRecursively(caseChild, artifactKey, casePath, 
                                        mediatorMap, caseCounter);
                            }
                        }
                    }
                }
            }
            
            // Register default case mediator if it exists
            SwitchCase defaultCase = switchMediator.getDefaultCase();
            if (defaultCase != null && defaultCase.getCaseMediator() != null) {
                Mediator defaultMediator = defaultCase.getCaseMediator();
                if (defaultMediator instanceof ListMediator) {
                    ListMediator defaultList = (ListMediator) defaultMediator;
                    List<Mediator> defaultChildren = defaultList.getList();
                    if (defaultChildren != null && !defaultChildren.isEmpty()) {
                        AtomicInteger defaultCounter = new AtomicInteger(0);
                        String defaultPath = mediatorId + "/default";
                        for (Mediator defaultChild : defaultChildren) {
                            registerMediatorRecursively(defaultChild, artifactKey, defaultPath, 
                                    mediatorMap, defaultCounter);
                        }
                    }
                }
            }
        }
        // For regular list mediators (SequenceMediator, etc.), register children normally
        else if (mediator instanceof ListMediator) {
            ListMediator listMediator = (ListMediator) mediator;
            List<Mediator> children = listMediator.getList();
            
            if (children != null && !children.isEmpty()) {
                // Create new counter for children at this level
                AtomicInteger childCounter = new AtomicInteger(0);
                for (Mediator child : children) {
                    registerMediatorRecursively(child, artifactKey, mediatorId, mediatorMap, childCounter);
                }
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
     * Get executed mediator identifiers.
     *
     * @param artifactKey artifact key
     * @return set of executed mediator identifiers
     */
    public Set<String> getExecutedMediatorIds(String artifactKey) {
        Set<String> executed = executedMediators.get(artifactKey);
        return executed != null ? new HashSet<>(executed) : new HashSet<>();
    }

    /**
     * Get all mediator identifiers for an artifact in registration order.
     *
     * @param artifactKey artifact key
     * @return set of all mediator identifiers (preserves insertion order)
     */
    public Set<String> getAllMediatorIds(String artifactKey) {
        Map<String, Mediator> mediators = allMediators.get(artifactKey);
        return mediators != null ? new LinkedHashSet<>(mediators.keySet()) : new LinkedHashSet<>();
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
     * Check if an artifact is already registered.
     *
     * @param artifactKey artifact key to check
     * @return true if artifact is registered, false otherwise
     */
    public boolean isArtifactRegistered(String artifactKey) {
        return allMediators.containsKey(artifactKey);
    }

    /**
     * Clear all tracking data.
     */
    public void clear() {
        allMediators.clear();
        executedMediators.clear();
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
