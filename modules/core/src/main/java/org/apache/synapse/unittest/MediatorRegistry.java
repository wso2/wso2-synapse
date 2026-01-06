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
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.ListMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.PropertyMediator;
import org.apache.synapse.mediators.elementary.EnrichMediator;
import org.apache.synapse.mediators.filters.FilterMediator;
import org.apache.synapse.mediators.filters.SwitchMediator;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central registry for mediators in deployed artifacts.
 * Assigns unique hierarchical IDs to all mediators and tracks their execution for coverage reporting.
 */
public class MediatorRegistry {

    private static Log log = LogFactory.getLog(MediatorRegistry.class.getName());

    // Thread-safe singleton instance
    private static final MediatorRegistry instance = new MediatorRegistry();

    // Registry: artifactKey -> ordered set of mediatorIds
    private final Map<String, LinkedHashSet<String>> registeredMediators = new ConcurrentHashMap<>();

    // Execution tracking for coverage
    private final Map<String, Set<String>> executedMediators = new ConcurrentHashMap<>();

    private MediatorRegistry() {
    }

    /**
     * Get singleton instance.
     *
     * @return singleton instance
     */
    public static MediatorRegistry getInstance() {
        return instance;
    }

    /**
     * Register an API and assign IDs to all its mediators.
     *
     * @param api API to register
     */
    public void registerAPI(API api) {
        String artifactKey = "API:" + api.getName();
        log.info("Registering API: " + api.getName());

        LinkedHashSet<String> mediatorIds = new LinkedHashSet<>();

        // Track mediators in all resources
        for (Resource resource : api.getResources()) {
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
            
            if (resource.getInSequence() != null) {
                registerMediatorTree(resource.getInSequence(), artifactKey, 
                        resourcePath + "/in", mediatorIds, new AtomicInteger(0));
            }
            if (resource.getOutSequence() != null) {
                registerMediatorTree(resource.getOutSequence(), artifactKey,
                        resourcePath + "/out", mediatorIds, new AtomicInteger(0));
            }
            if (resource.getFaultSequence() != null) {
                registerMediatorTree(resource.getFaultSequence(), artifactKey,
                        resourcePath + "/fault", mediatorIds, new AtomicInteger(0));
            }
        }

        registeredMediators.put(artifactKey, mediatorIds);
        executedMediators.put(artifactKey, ConcurrentHashMap.newKeySet());
        
        log.info("Registered API " + api.getName() + " with " + mediatorIds.size() + " mediators");
    }

    /**
     * Register a Sequence and assign IDs to all its mediators.
     *
     * @param sequence Sequence to register
     */
    public void registerSequence(SequenceMediator sequence) {
        String artifactKey = "Sequence:" + sequence.getName();
        log.info("Registering Sequence: " + sequence.getName());

        LinkedHashSet<String> mediatorIds = new LinkedHashSet<>();

        registerMediatorTree(sequence, artifactKey, "sequence:" + sequence.getName(), 
                mediatorIds, new AtomicInteger(0));

        registeredMediators.put(artifactKey, mediatorIds);
        executedMediators.put(artifactKey, ConcurrentHashMap.newKeySet());
        
        log.info("Registered Sequence " + sequence.getName() + " with " + mediatorIds.size() + " mediators");
    }

    /**
     * Recursively register mediators and assign hierarchical IDs.
     *
     * @param mediator        mediator to register
     * @param artifactKey     artifact key
     * @param path            path prefix for identification
     * @param mediatorIds     set to store mediator IDs
     * @param positionCounter counter for mediator positions at current level
     */
    private void registerMediatorTree(Mediator mediator, String artifactKey, 
                                     String path, LinkedHashSet<String> mediatorIds,
                                     AtomicInteger positionCounter) {
        if (mediator == null) {
            return;
        }

        // Get mediator type without "Mediator" suffix
        String mediatorType = mediator.getClass().getSimpleName();
        if (mediatorType.endsWith("Mediator")) {
            mediatorType = mediatorType.substring(0, mediatorType.length() - 8);
        }
        
        // Generate position-based ID
        int position = positionCounter.incrementAndGet();
        String mediatorId = path + "/" + position + "." + mediatorType;
        
        // Add context for specific mediators
        if (mediator instanceof PropertyMediator) {
            PropertyMediator propMediator = (PropertyMediator) mediator;
            if (propMediator.getName() != null) {
                mediatorId += "[" + propMediator.getName() + "]";
            }
        } else if (mediator instanceof EnrichMediator) {
            mediatorId += "[enrich]";
        }

        // Store ID in mediator
        if (mediator instanceof AbstractMediator) {
            AbstractMediator abstractMediator = (AbstractMediator) mediator;
            abstractMediator.setMediatorId(mediatorId);
            
            if (log.isDebugEnabled()) {
                log.debug("Assigned mediator ID: " + mediatorId);
            }
        }

        mediatorIds.add(mediatorId);

        // Handle child mediators
        if (mediator instanceof FilterMediator) {
            registerFilterBranches((FilterMediator) mediator, artifactKey, mediatorId, mediatorIds);
        } else if (mediator instanceof SwitchMediator) {
            registerSwitchCases((SwitchMediator) mediator, artifactKey, mediatorId, mediatorIds);
        } else if (mediator instanceof ListMediator) {
            registerListChildren((ListMediator) mediator, artifactKey, mediatorId, mediatorIds);
        }
    }

    /**
     * Register FilterMediator branches (then/else).
     */
    private void registerFilterBranches(FilterMediator filterMediator, String artifactKey, 
                                       String mediatorId, LinkedHashSet<String> mediatorIds) {
        // Register then branch
        List<Mediator> thenChildren = filterMediator.getList();
        if (thenChildren != null && !thenChildren.isEmpty()) {
            AtomicInteger thenCounter = new AtomicInteger(0);
            for (Mediator thenChild : thenChildren) {
                registerMediatorTree(thenChild, artifactKey, mediatorId + "/then", 
                        mediatorIds, thenCounter);
            }
        }
        
        // Register else branch
        if (filterMediator.getElseMediator() != null) {
            ListMediator elseMediator = filterMediator.getElseMediator();
            List<Mediator> elseChildren = elseMediator.getList();
            
            if (elseChildren != null && !elseChildren.isEmpty()) {
                AtomicInteger elseCounter = new AtomicInteger(0);
                for (Mediator elseChild : elseChildren) {
                    registerMediatorTree(elseChild, artifactKey, mediatorId + "/else", 
                            mediatorIds, elseCounter);
                }
            }
        }
    }

    /**
     * Register SwitchMediator cases.
     */
    private void registerSwitchCases(SwitchMediator switchMediator, String artifactKey, 
                                    String mediatorId, LinkedHashSet<String> mediatorIds) {
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
                            registerMediatorTree(caseChild, artifactKey, casePath, 
                                    mediatorIds, caseCounter);
                        }
                    }
                }
            }
        }
        
        // Register default case mediator
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
                        registerMediatorTree(defaultChild, artifactKey, defaultPath, 
                                mediatorIds, defaultCounter);
                    }
                }
            }
        }
    }

    /**
     * Register ListMediator children.
     */
    private void registerListChildren(ListMediator listMediator, String artifactKey, 
                                     String mediatorId, LinkedHashSet<String> mediatorIds) {
        List<Mediator> children = listMediator.getList();
        
        if (children != null && !children.isEmpty()) {
            AtomicInteger childCounter = new AtomicInteger(0);
            for (Mediator child : children) {
                registerMediatorTree(child, artifactKey, mediatorId, mediatorIds, childCounter);
            }
        }
    }

    /**
     * Mark a mediator as executed.
     *
     * @param mediator    executed mediator
     * @param artifactKey artifact key
     */
    public void markMediatorExecuted(Mediator mediator, String artifactKey) {
        if (mediator == null || artifactKey == null) {
            return;
        }

        if (mediator instanceof AbstractMediator) {
            AbstractMediator abstractMediator = (AbstractMediator) mediator;
            String mediatorId = abstractMediator.getMediatorId();
            
            if (mediatorId != null) {
                Set<String> executed = executedMediators.get(artifactKey);
                if (executed != null) {
                    executed.add(mediatorId);
                    if (log.isDebugEnabled()) {
                        log.debug("Marked mediator as executed: " + mediatorId);
                    }
                }
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
        LinkedHashSet<String> mediatorIds = registeredMediators.get(artifactKey);
        return mediatorIds != null ? mediatorIds.size() : 0;
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
     * Get all mediator identifiers for an artifact.
     *
     * @param artifactKey artifact key
     * @return set of all mediator identifiers
     */
    public Set<String> getAllMediatorIds(String artifactKey) {
        LinkedHashSet<String> mediatorIds = registeredMediators.get(artifactKey);
        return mediatorIds != null ? new LinkedHashSet<>(mediatorIds) : new LinkedHashSet<>();
    }

    /**
     * Get all registered artifact keys.
     *
     * @return set of artifact keys
     */
    public Set<String> getRegisteredArtifacts() {
        return new HashSet<>(registeredMediators.keySet());
    }

    /**
     * Check if an artifact is already registered.
     *
     * @param artifactKey artifact key to check
     * @return true if artifact is registered, false otherwise
     */
    public boolean isArtifactRegistered(String artifactKey) {
        return registeredMediators.containsKey(artifactKey);
    }

    /**
     * Clear all registry data.
     */
    public void clear() {
        registeredMediators.clear();
        executedMediators.clear();
        log.info("Cleared mediator registry");
    }

    /**
     * Clear tracking data for a specific artifact.
     *
     * @param artifactKey artifact key
     */
    public void clearArtifact(String artifactKey) {
        registeredMediators.remove(artifactKey);
        executedMediators.remove(artifactKey);
        log.info("Cleared mediator registry data for: " + artifactKey);
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
