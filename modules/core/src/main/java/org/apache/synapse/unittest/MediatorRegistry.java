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

package org.apache.synapse.unittest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.api.API;
import org.apache.synapse.api.Resource;
import org.apache.synapse.config.xml.SwitchCase;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.ListMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.CommentMediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.builtin.PropertyMediator;
import org.apache.synapse.mediators.filters.FilterMediator;
import org.apache.synapse.mediators.filters.SwitchMediator;
import org.apache.synapse.mediators.template.InvokeMediator;
import org.apache.synapse.mediators.template.TemplateMediator;
import org.apache.synapse.mediators.v2.VariableMediator;

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

    // Singleton instance
    private static final MediatorRegistry instance = new MediatorRegistry();

    // Registry: artifactKey -> ordered set of mediatorIds
    private final Map<String, LinkedHashSet<String>> registeredMediators = new ConcurrentHashMap<>();

    // Execution tracking for coverage
    private final Map<String, Set<String>> executedMediators = new ConcurrentHashMap<>();

    // Dependency tracking: artifactKey -> set of referenced sequence names
    private final Map<String, Set<String>> artifactDependencies = new ConcurrentHashMap<>();

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
        if (api == null || api.getName() == null) {
            log.warn("Cannot register null API or API with null name");
            return;
        }
        
        String artifactKey = "API:" + api.getName();
        if (log.isDebugEnabled()) {
            log.debug("Registering API for coverage tracking: " + api.getName());
        }

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
            
            // Check for direct key references (e.g., <inSequence key="sample-seq"/>)
            if (resource.getInSequenceKey() != null) {
                addArtifactDependency(artifactKey, "Sequence", resource.getInSequenceKey());
            }
            if (resource.getOutSequenceKey() != null) {
                addArtifactDependency(artifactKey, "Sequence", resource.getOutSequenceKey());
            }
            if (resource.getFaultSequenceKey() != null) {
                addArtifactDependency(artifactKey, "Sequence", resource.getFaultSequenceKey());
            }
            
            // Register inline sequences
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
        
        if (log.isDebugEnabled()) {
            log.debug("Registered API " + api.getName() + " with " + mediatorIds.size() + " mediators");
        }
    }

    /**
     * Register a Sequence and assign IDs to all its mediators.
     * If sequenceKey is provided, uses it as the registration key (for registry sequences).
     * Otherwise, uses sequence.getName() (for deployed sequence artifacts).
     *
     * @param sequence Sequence to register
     * @param sequenceKey Optional key to use for registration (null = use sequence name)
     */
    public void registerSequence(SequenceMediator sequence, String sequenceKey) {
        if (sequence == null) {
            return;
        }
        
        // Use provided key, or fall back to sequence name
        String key = (sequenceKey != null && !sequenceKey.isEmpty()) ? sequenceKey : sequence.getName();
        
        if (key == null || key.isEmpty()) {
            return;
        }
        
        String artifactKey = "Sequence:" + key;
        if (log.isDebugEnabled()) {
            log.debug("Registering Sequence for coverage tracking: " + key);
        }

        LinkedHashSet<String> mediatorIds = new LinkedHashSet<>();

        String sequencePath = "sequence:" + key;
        AtomicInteger counter = new AtomicInteger(0);
        
        // Track onError sequence dependency
        if (sequence.getErrorHandler() != null) {
            addArtifactDependency(artifactKey, "Sequence", sequence.getErrorHandler());
            if (log.isDebugEnabled()) {
                log.debug("Detected onError sequence in " + artifactKey + " -> " + sequence.getErrorHandler());
            }
        }
        
        List<Mediator> children = sequence.getList();
        if (children != null && !children.isEmpty()) {
            for (Mediator child : children) {
                registerMediatorTree(child, artifactKey, sequencePath, mediatorIds, counter);
            }
        }

        registeredMediators.put(artifactKey, mediatorIds);
        executedMediators.put(artifactKey, ConcurrentHashMap.newKeySet());
        
        if (log.isDebugEnabled()) {
            log.debug("Registered Sequence " + key + " with " + mediatorIds.size() + " mediators");
        }
    }

    /**
     * Register a Template and assign IDs to all its mediators.
     *
     * @param template Template to register
     */
    public void registerTemplate(TemplateMediator template) {
        if (template == null || template.getName() == null) {
            log.warn("Cannot register null Template or Template with null name");
            return;
        }

        String artifactKey = "Template:" + template.getName();
        if (log.isDebugEnabled()) {
            log.debug("Registering Template for coverage tracking: " + template.getName());
        }

        LinkedHashSet<String> mediatorIds = new LinkedHashSet<>();

        // Register children directly
        // The template is just a container, not an actual mediator to count
        String templatePath = "template:" + template.getName();
        AtomicInteger counter = new AtomicInteger(0);

        List<Mediator> children = template.getList();
        if (children != null && !children.isEmpty()) {
            for (Mediator child : children) {
                registerMediatorTree(child, artifactKey, templatePath, mediatorIds, counter);
            }
        }

        registeredMediators.put(artifactKey, mediatorIds);
        executedMediators.put(artifactKey, ConcurrentHashMap.newKeySet());

        if (log.isDebugEnabled()) {
            log.debug("Registered Template " + template.getName() + " with " + mediatorIds.size() + " mediators");
        }
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

        // Skip CommentMediator
        if (mediator instanceof CommentMediator) {
            log.debug("Skipping CommentMediator (XML comment node)");
            return;
        }

        // Sequence references
        if (mediator instanceof SequenceMediator) {
            SequenceMediator seqMediator = (SequenceMediator) mediator;
            
            // Check if this is a sequence reference (has a key)
            if (seqMediator.getKey() != null) {
                String refSeqName = extractSequenceNameFromKey(seqMediator.getKey());
                if (refSeqName != null) {
                    addArtifactDependency(artifactKey, "Sequence", refSeqName);
                    if (log.isDebugEnabled()) {
                        log.debug("Detected sequence reference in " + artifactKey + " -> " + refSeqName);
                    }
                }
                return;
            }
            
            // Check for anonymous sequence containers
            if (seqMediator.getName() == null) {
                // Anonymous inline sequence - process children without counting container
                if (mediator instanceof ListMediator) {
                    List<Mediator> children = ((ListMediator) mediator).getList();
                    if (children != null && !children.isEmpty()) {
                        for (Mediator child : children) {
                            registerMediatorTree(child, artifactKey, path, mediatorIds, positionCounter);
                        }
                    }
                }
                return;
            }
        }

        String mediatorId = null;
        // Register actual mediators
        // Get mediator type without "Mediator" suffix
        String mediatorType = mediator.getClass().getSimpleName();
        if (mediatorType.endsWith("Mediator")) {
            mediatorType = mediatorType.substring(0, mediatorType.length() - 8);
        }
        
        // Generate position-based ID
        int position = positionCounter.incrementAndGet();
        
        // For InvokeMediator (connectors or templates), use the connector/template name instead of "Invoke"
        if (mediator instanceof InvokeMediator) {
            InvokeMediator invokeMediator = (InvokeMediator) mediator;
            if (invokeMediator.getTargetTemplate() != null) {
                String targetTemplate = invokeMediator.getTargetTemplate();
                // Extract short name from qualified name (e.g., "org.wso2.carbon.connector.http.get" -> "http.get")
                int lastDotBeforeConnector = targetTemplate.lastIndexOf(".connector.");
                if (lastDotBeforeConnector != -1) {
                    // This is a connector
                    String connectorName = targetTemplate.substring(lastDotBeforeConnector + ".connector.".length());
                    mediatorId = path + "/" + position + "." + connectorName;
                } else {
                    // This is a template reference - track as dependency
                    addArtifactDependency(artifactKey, "Template", targetTemplate);
                    if (log.isDebugEnabled()) {
                        log.debug("Detected template reference in " + artifactKey + " -> " + targetTemplate);
                    }
                    mediatorId = path + "/" + position + ".call-template[" + targetTemplate + "]";
                }
            } else {
                mediatorId = path + "/" + position + "." + mediatorType;
            }
        } else {
            mediatorId = path + "/" + position + "." + mediatorType;
        }
        
        // Add context for specific mediators
        if (mediator instanceof PropertyMediator) {
            PropertyMediator propMediator = (PropertyMediator) mediator;
            if (propMediator.getName() != null) {
                mediatorId += "[" + propMediator.getName() + "]";
                
                // Skip test-injected properties that have been marked by the test framework
                if (propMediator.isTestInjected()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Skipping test-injected property mediator: " + mediatorId);
                    }
                    return;
                }
            }
        } else if (mediator instanceof VariableMediator) {
            VariableMediator varMediator = (VariableMediator) mediator;
            if (varMediator.getName() != null) {
                mediatorId += "[" + varMediator.getName() + "]";
            }
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
        } else if (mediator instanceof ForEachMediator) {
            registerForEachSequence((ForEachMediator) mediator, artifactKey, mediatorId, mediatorIds);
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
     * Register ForEachMediator's inline sequence.
     */
    private void registerForEachSequence(ForEachMediator forEachMediator, String artifactKey,
                                        String mediatorId, LinkedHashSet<String> mediatorIds) {
        // Register inline sequence if present
        SequenceMediator sequence = forEachMediator.getSequence();
        if (sequence != null && sequence instanceof ListMediator) {
            List<Mediator> children = ((ListMediator) sequence).getList();
            if (children != null && !children.isEmpty()) {
                log.debug("Registering ForEach mediator inline sequence");
                AtomicInteger childCounter = new AtomicInteger(0);
                for (Mediator child : children) {
                    registerMediatorTree(child, artifactKey, mediatorId, mediatorIds, childCounter);
                }
            }
        }
        
        // Track sequence reference as dependency if present
        String sequenceRef = forEachMediator.getSequenceRef();
        if (sequenceRef != null && !sequenceRef.isEmpty()) {
            addArtifactDependency(artifactKey, "Sequence", sequenceRef);
            if (log.isDebugEnabled()) {
                log.debug("Detected sequence reference in ForEach mediator: " + artifactKey + " -> " + sequenceRef);
            }
        }
    }

    /**
     * Register mediators in a ListMediator's children.
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
                // Check if this mediatorId was registered during artifact deployment
                LinkedHashSet<String> registeredIds = registeredMediators.get(artifactKey);
                if (registeredIds == null || !registeredIds.contains(mediatorId)) {
                    // This is an internal mediator not registered for coverage
                    // Don't track it to avoid inflating coverage counts
                    if (log.isDebugEnabled()) {
                        log.debug("Skipping unregistered mediator (likely internal): " + mediatorId + 
                                " for artifact: " + artifactKey);
                    }
                    return;
                }
                
                Set<String> executed = executedMediators.get(artifactKey);
                if (executed != null) {
                    executed.add(mediatorId);
                    if (log.isDebugEnabled()) {
                        log.debug("Marked mediator as executed: " + mediatorId + " for artifact: " + artifactKey);
                    }
                }
            }
        }
    }

    /**
     * Get total mediator count for an artifact.
     *
     * @param artifactKey artifact key
     * @param includeReferences if true, includes mediators from referenced sequences (aggregated count)
     * @return total mediator count
     */
    public int getTotalMediatorCount(String artifactKey, boolean includeReferences) {
        if (!includeReferences) {
            LinkedHashSet<String> mediatorIds = registeredMediators.get(artifactKey);
            return mediatorIds != null ? mediatorIds.size() : 0;
        }
        return getTotalMediatorCount(artifactKey, new HashSet<>());
    }

    /**
     * Get total mediator count including referenced sequences.
     *
     * @param artifactKey artifact key
     * @param visited set of already visited artifacts to avoid double counting
     * @return total mediator count including dependencies
     */
    private int getTotalMediatorCount(String artifactKey, Set<String> visited) {
        if (visited.contains(artifactKey)) {
            return 0;
        }
        visited.add(artifactKey);

        // Get this artifact's mediator count
        LinkedHashSet<String> mediatorIds = registeredMediators.get(artifactKey);
        int total = mediatorIds != null ? mediatorIds.size() : 0;

        // Add mediators from referenced artifacts (sequences and templates)
        Set<String> referencedArtifacts = artifactDependencies.get(artifactKey);
        if (referencedArtifacts != null) {
            for (String refArtifactKey : referencedArtifacts) {
                if (!isArtifactRegistered(refArtifactKey)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Referenced artifact not registered: " + refArtifactKey);
                    }
                    continue;
                }

                // Recursively count referenced artifact mediators
                total += getTotalMediatorCount(refArtifactKey, visited);
            }
        }

        return total;
    }

    /**
     * Get executed mediator count for an artifact.
     *
     * @param artifactKey artifact key
     * @param includeReferences if true, includes executed mediators from referenced sequences (aggregated count)
     * @return executed mediator count
     */
    public int getExecutedMediatorCount(String artifactKey, boolean includeReferences) {
        if (!includeReferences) {
            Set<String> executed = executedMediators.get(artifactKey);
            return executed != null ? executed.size() : 0;
        }
        return getExecutedMediatorCount(artifactKey, new HashSet<>());
    }

    /**
     * Get executed mediator count including referenced sequences.
     *
     * @param artifactKey artifact key
     * @param visited set of already visited artifacts to avoid double counting
     * @return executed mediator count including dependencies
     */
    private int getExecutedMediatorCount(String artifactKey, Set<String> visited) {
        if (visited.contains(artifactKey)) {
            return 0;
        }
        visited.add(artifactKey);

        // Get this artifact's executed mediator count
        Set<String> executedSet = executedMediators.get(artifactKey);
        int executed = executedSet != null ? executedSet.size() : 0;

        // Add executed mediators from referenced artifacts (sequences and templates)
        Set<String> referencedArtifacts = artifactDependencies.get(artifactKey);
        if (referencedArtifacts != null) {
            for (String refArtifactKey : referencedArtifacts) {
                if (!isArtifactRegistered(refArtifactKey)) {
                    continue;
                }

                // Recursively count executed mediators in referenced artifacts
                executed += getExecutedMediatorCount(refArtifactKey, visited);
            }
        }

        return executed;
    }

    /**
     * Get executed mediator identifiers for an artifact (own mediators only, not aggregated).
     *
     * @param artifactKey artifact key
     * @return set of executed mediator identifiers
     */
    public Set<String> getExecutedMediatorIds(String artifactKey) {
        Set<String> executed = executedMediators.get(artifactKey);
        return executed != null ? new HashSet<>(executed) : new HashSet<>();
    }

    /**
     * Get all mediator identifiers for an artifact (own mediators only, not aggregated).
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
     * Record that an artifact references another artifact (sequence or template).
     *
     * @param artifactKey the artifact key
     * @param artifactType the type of referenced artifact ("Sequence" or "Template")
     * @param referencedName the name of the referenced artifact
     */
    private void addArtifactDependency(String artifactKey, String artifactType, String referencedName) {
        if (referencedName != null && !referencedName.isEmpty()) {
            String referencedArtifactKey = artifactType + ":" + referencedName;
            artifactDependencies.computeIfAbsent(artifactKey, k -> ConcurrentHashMap.newKeySet())
                               .add(referencedArtifactKey);
            if (log.isDebugEnabled()) {
                log.debug("Recorded dependency: " + artifactKey + " -> " + referencedArtifactKey);
            }
        }
    }

    /**
     * Extract sequence name from a Value object (key attribute).
     *
     * @param keyValue the Value object from SequenceMediator.getKey()
     * @return sequence name or null
     */
    private String extractSequenceNameFromKey(Value keyValue) {
        if (keyValue == null) {
            return null;
        }
        
        String keyStr = keyValue.getKeyValue();
        if (keyStr != null && !keyStr.isEmpty()) {
            return keyStr;
        }

        return null;
    }

    /**
     * Get the set of artifacts referenced by an artifact.
     *
     * @param artifactKey artifact key
     * @return set of referenced artifact keys (e.g., "Sequence:xxx", "Template:xxx") (empty if none)
     */
    public Set<String> getReferencedArtifacts(String artifactKey) {
        Set<String> refs = artifactDependencies.get(artifactKey);
        return refs != null ? new HashSet<>(refs) : new HashSet<>();
    }

    /**
     * Clear all registry data.
     */
    public void clear() {
        registeredMediators.clear();
        executedMediators.clear();
        artifactDependencies.clear();
        log.debug("Cleared mediator registry");
    }
}
