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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.api.API;
import org.apache.synapse.api.Resource;
import org.apache.synapse.config.xml.AnonymousListMediator;
import org.apache.synapse.config.xml.SwitchCase;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.CommentMediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.builtin.PropertyMediator;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.eip.splitter.CloneMediator;
import org.apache.synapse.mediators.eip.splitter.IterateMediator;
import org.apache.synapse.mediators.filters.FilterMediator;
import org.apache.synapse.mediators.filters.SwitchMediator;
import org.apache.synapse.mediators.template.InvokeMediator;
import org.apache.synapse.mediators.template.TemplateMediator;
import org.apache.synapse.mediators.v2.ScatterGather;
import org.apache.synapse.mediators.v2.VariableMediator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages unique hierarchical ID assignment for mediators in deployed artifacts to enable detailed error logging.
 * 
 * <p>ID Format: {@code <artifact-type>:<artifact-name>/<path>/<position>.<MediatorType>[context]}
 * <p>Example: {@code sequence:MainSequence/1.Log}, {@code proxy:MyProxy/in/2.Property[userId]}
 */
public class MediatorIdentityManager {

    private static final Log log = LogFactory.getLog(MediatorIdentityManager.class);

    // Artifact type prefixes
    private static final String PREFIX_API = "api:";
    private static final String PREFIX_SEQUENCE = "sequence:";
    private static final String PREFIX_TEMPLATE = "template:";
    private static final String PREFIX_PROXY = "proxy:";
    
    // Path suffixes for sequence types
    private static final String PATH_IN = "/in";
    private static final String PATH_OUT = "/out";
    private static final String PATH_FAULT = "/fault";
    private static final String PATH_ELSE = "/else";
    private static final String PATH_DEFAULT = "/default";
    private static final String PATH_TARGET = "/target";
    private static final String PATH_FOREACH = "/foreach";
    private static final String PATH_CASE = "/case";

    // Singleton instance
    private static final MediatorIdentityManager instance = new MediatorIdentityManager();

    private MediatorIdentityManager() {
    }

    /**
     * Get singleton instance.
     *
     * @return singleton instance
     */
    public static MediatorIdentityManager getInstance() {
        return instance;
    }

    /**
     * Assign IDs to all mediators in an API.
     *
     * @param api API to process
     */
    public void assignMediatorIds(API api) {
        if (api == null || api.getName() == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Assigning mediator IDs for API: " + api.getName());
        }

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
            
            String resourcePath = PREFIX_API + api.getName() + "/" + methodStr + "[" + uriTemplate + "]";
            
            if (resource.getInSequence() != null) {
                assignMediatorIds(resource.getInSequence(), resourcePath + PATH_IN, new AtomicInteger(0));
            }
            if (resource.getOutSequence() != null) {
                assignMediatorIds(resource.getOutSequence(), resourcePath + PATH_OUT, new AtomicInteger(0));
            }
            if (resource.getFaultSequence() != null) {
                assignMediatorIds(resource.getFaultSequence(), resourcePath + PATH_FAULT, new AtomicInteger(0));
            }
        }
    }

    /**
     * Assign IDs to all mediators in a Sequence.
     *
     * @param sequence Sequence to process
     */
    public void assignMediatorIds(SequenceMediator sequence) {
        if (sequence == null || sequence.getName() == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Assigning mediator IDs for Sequence: " + sequence.getName());
        }

        String sequencePath = PREFIX_SEQUENCE + sequence.getName();
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Mediator> children = sequence.getList();
        if (children != null && !children.isEmpty()) {
            for (Mediator child : children) {
                assignMediatorIds(child, sequencePath, counter);
            }
        }
    }

    /**
     * Assign IDs to all mediators in a Template.
     *
     * @param template Template to process
     */
    public void assignMediatorIds(TemplateMediator template) {
        if (template == null || template.getName() == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Assigning mediator IDs for Template: " + template.getName());
        }

        String templatePath = PREFIX_TEMPLATE + template.getName();
        AtomicInteger counter = new AtomicInteger(0);

        List<Mediator> children = template.getList();
        if (children != null && !children.isEmpty()) {
            for (Mediator child : children) {
                assignMediatorIds(child, templatePath, counter);
            }
        }
    }

    /**
     * Assign IDs to all mediators in a ProxyService.
     *
     * @param proxyService ProxyService to process
     */
    public void assignMediatorIds(ProxyService proxyService) {
        if (proxyService == null || proxyService.getName() == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Assigning mediator IDs for ProxyService: " + proxyService.getName());
        }

        String proxyPath = PREFIX_PROXY + proxyService.getName();

        if (proxyService.getTargetInLineInSequence() != null) {
            assignMediatorIds(proxyService.getTargetInLineInSequence(), proxyPath + PATH_IN, new AtomicInteger(0));
        }
        if (proxyService.getTargetInLineOutSequence() != null) {
            assignMediatorIds(proxyService.getTargetInLineOutSequence(), proxyPath + PATH_OUT, new AtomicInteger(0));
        }
        if (proxyService.getTargetInLineFaultSequence() != null) {
            assignMediatorIds(proxyService.getTargetInLineFaultSequence(), proxyPath + PATH_FAULT, new AtomicInteger(0));
        }
    }

    /**
     * Recursively assign hierarchical IDs to mediators.
     *
     * @param mediator        mediator to process
     * @param path            path prefix for identification
     * @param positionCounter counter for mediator positions at current level
     */
    private void assignMediatorIds(Mediator mediator, String path, AtomicInteger positionCounter) {
        if (mediator == null) {
            return;
        }

        // Skip CommentMediator
        if (mediator instanceof CommentMediator) {
            return;
        }

        // Handle sequence references
        if (mediator instanceof SequenceMediator) {
            SequenceMediator seqMediator = (SequenceMediator) mediator;
            
            // For sequence references (has a key), assign ID but don't recurse into children
            // because the actual mediators are in the referenced sequence, not this one
            if (seqMediator.getKey() != null) {
                int position = positionCounter.incrementAndGet();
                String mediatorId = path + "/" + position + ".CallSequence";
                if (mediator instanceof AbstractMediator) {
                    ((AbstractMediator) mediator).setMediatorId(mediatorId);
                }
                return;
            }
            
            // Anonymous sequence containers
            if (seqMediator.getName() == null) {
                if (mediator instanceof ListMediator) {
                    List<Mediator> children = ((ListMediator) mediator).getList();
                    if (children != null && !children.isEmpty()) {
                        for (Mediator child : children) {
                            assignMediatorIds(child, path, positionCounter);
                        }
                    }
                }
                return;
            }
        }

        // Assign ID to actual mediator
        String mediatorType = mediator.getClass().getSimpleName();
        if (mediatorType.endsWith("Mediator")) {
            mediatorType = mediatorType.substring(0, mediatorType.length() - 8);
        }
        
        int position = positionCounter.incrementAndGet();
        String mediatorId = path + "/" + position + "." + mediatorType;
        
        // Handle InvokeMediator (connectors/modules/templates)
        if (mediator instanceof InvokeMediator) {
            InvokeMediator invokeMediator = (InvokeMediator) mediator;
            if (invokeMediator.getTargetTemplate() != null) {
                String targetTemplate = invokeMediator.getTargetTemplate();
                int lastDotBeforeConnector = targetTemplate.lastIndexOf(".connector.");
                int lastDotBeforeModule = targetTemplate.lastIndexOf(".module.");
                
                if (lastDotBeforeConnector != -1) {
                    String connectorName = targetTemplate.substring(lastDotBeforeConnector + ".connector.".length());
                    mediatorId = path + "/" + position + "." + connectorName;
                } else if (lastDotBeforeModule != -1) {
                    String moduleName = targetTemplate.substring(lastDotBeforeModule + ".module.".length());
                    mediatorId = path + "/" + position + "." + moduleName;
                } else {
                    // Template reference - extract template name
                    String templateName = targetTemplate;
                    int lastDot = targetTemplate.lastIndexOf('.');
                    if (lastDot != -1) {
                        templateName = targetTemplate.substring(lastDot + 1);
                    }
                    mediatorId = path + "/" + position + ".CallTemplate[" + templateName + "]";
                }
            }
        }
        
        // Add context for specific mediators
        if (mediator instanceof PropertyMediator) {
            PropertyMediator propMediator = (PropertyMediator) mediator;
            if (propMediator.getName() != null) {
                mediatorId += "[" + propMediator.getName() + "]";
            }
        } else if (mediator instanceof VariableMediator) {
            VariableMediator varMediator = (VariableMediator) mediator;
            if (varMediator.getName() != null) {
                mediatorId += "[" + varMediator.getName() + "]";
            }
        }
        
        // Set the ID
        if (mediator instanceof AbstractMediator) {
            ((AbstractMediator) mediator).setMediatorId(mediatorId);
        }

        // Process children for container mediators
        if (mediator instanceof ListMediator) {
            List<Mediator> children = ((ListMediator) mediator).getList();
            if (children != null && !children.isEmpty()) {
                AtomicInteger childCounter = new AtomicInteger(0);
                for (Mediator child : children) {
                    assignMediatorIds(child, mediatorId, childCounter);
                }
            }
        }

        // Handle FilterMediator (then/else branches)
        if (mediator instanceof FilterMediator) {
            FilterMediator filter = (FilterMediator) mediator;
            // Filter has inline child mediators (then path) and separate else branch
            // Child mediators are in the list inherited from AbstractListMediator
            if (filter.getElseMediator() != null) {
                assignMediatorIds(filter.getElseMediator(), mediatorId + PATH_ELSE, new AtomicInteger(0));
            }
        }

        // Handle SwitchMediator (case branches)
        if (mediator instanceof SwitchMediator) {
            SwitchMediator switchMediator = (SwitchMediator) mediator;
            List<SwitchCase> cases = switchMediator.getCases();
            if (cases != null) {
                for (int i = 0; i < cases.size(); i++) {
                    SwitchCase switchCase = cases.get(i);
                    if (switchCase != null) {
                        AnonymousListMediator caseMediator = switchCase.getCaseMediator();
                        if (caseMediator != null && caseMediator.getList() != null) {
                            String caseRegex = (switchCase.getRegex() != null) ? 
                                    "[" + switchCase.getRegex().pattern() + "]" : "[case" + (i + 1) + "]";
                            // Process each mediator in the case
                            AtomicInteger pos = new AtomicInteger(0);
                            for (Mediator m : caseMediator.getList()) {
                                assignMediatorIds(m, mediatorId + PATH_CASE + caseRegex, pos);
                            }
                        }
                    }
                }
            }
            if (switchMediator.getDefaultCase() != null) {
                SwitchCase defaultSwitchCase = switchMediator.getDefaultCase();
                AnonymousListMediator defaultCase = defaultSwitchCase.getCaseMediator();
                if (defaultCase != null && defaultCase.getList() != null) {
                    AtomicInteger pos = new AtomicInteger(0);
                    for (Mediator m : defaultCase.getList()) {
                        assignMediatorIds(m, mediatorId + PATH_DEFAULT, pos);
                    }
                }
            }
        }

        // Handle CloneMediator
        if (mediator instanceof CloneMediator) {
            CloneMediator clone = (CloneMediator) mediator;
            List<Target> targets = clone.getTargets();
            if (targets != null) {
                for (int i = 0; i < targets.size(); i++) {
                    Target target = targets.get(i);
                    if (target.getSequence() != null) {
                        assignMediatorIds(target.getSequence(), mediatorId + PATH_TARGET + "[" + (i + 1) + "]", 
                                new AtomicInteger(0));
                    }
                }
            }
        }

        // Handle IterateMediator
        if (mediator instanceof IterateMediator) {
            IterateMediator iterate = (IterateMediator) mediator;
            if (iterate.getTarget() != null && iterate.getTarget().getSequence() != null) {
                assignMediatorIds(iterate.getTarget().getSequence(), mediatorId + PATH_TARGET, 
                        new AtomicInteger(0));
            }
        }

        // Handle ForEachMediator
        if (mediator instanceof ForEachMediator) {
            ForEachMediator forEach = (ForEachMediator) mediator;
            if (forEach.getSequenceRef() == null && forEach.getSequence() != null) {
                assignMediatorIds(forEach.getSequence(), mediatorId + PATH_FOREACH, new AtomicInteger(0));
            }
        }

        // Handle ScatterGather
        if (mediator instanceof ScatterGather) {
            ScatterGather scatterGather = (ScatterGather) mediator;
            List<Target> targets = scatterGather.getTargets();
            if (targets != null) {
                for (int i = 0; i < targets.size(); i++) {
                    Target target = targets.get(i);
                    if (target.getSequence() != null) {
                        assignMediatorIds(target.getSequence(), mediatorId + PATH_TARGET + "[" + (i + 1) + "]", 
                                new AtomicInteger(0));
                    }
                }
            }
        }
    }
}
