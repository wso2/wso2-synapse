/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.structuring.StructuringArtifact;
import org.apache.synapse.aspects.flow.statistics.structuring.StructuringElement;
import org.apache.synapse.config.SynapseConfiguration;

import java.util.ArrayList;
import java.util.Stack;

public class StatisticIdentityGenerator {

    private static Log log = LogFactory.getLog(StatisticIdentityGenerator.class);

    private static int id = 0;

    private static int hashCode = 0;

    private static String parent;

    private static ArrayList<StructuringElement> list = new ArrayList<>();

    private static Stack<StructuringElement> stack = new Stack<>();

    private static String lastParent;

    private static boolean branching = false;

    private static SynapseConfiguration synapseConfiguration;

    public static String getIdString() {
        return String.valueOf(id++);
    }

    public static void resetId() {
        if (list.size() > 0) {
            String artifactName = parent;
            StructuringArtifact structuringArtifact = new StructuringArtifact(hashCode, artifactName, list);
            if (synapseConfiguration != null) {
                synapseConfiguration.getCompletedStructureStore().putCompletedStatisticEntry(structuringArtifact);
            }
        }

        id = 0;
        hashCode = 0;
        stack.clear();
        list.clear();
        lastParent = null;
    }

    public static void setParent(String parentName) {
        parent = parentName;
    }

    public static String getIdForComponent(String name, ComponentType componentType) {
        String idString = parent + "@" + getIdString() + ":" + name;
        hashCode += idString.hashCode();

        if (log.isDebugEnabled()) {
            log.debug("Adding Component : " + idString);
        }
//        log.info("Adding Component : " + idString);
        process(idString, componentType, false);

        return idString;
    }

    public static String getIdReferencingComponent(String name, ComponentType componentType) {
        String idString = name + "@0:" + name;
        id++;
        hashCode += idString.hashCode();
        if (log.isDebugEnabled()) {
            log.debug("Adding Referencing Component  : " + idString);
        }
        log.info("Adding Referencing Component  : " + idString);
        process(idString, componentType, false);

        return idString;
    }

    public static String getIdForFlowContinuableMediator(String mediatorName, ComponentType componentType) {
        String idString = parent + "@" + getIdString() + ":" + mediatorName;
        hashCode += idString.hashCode();

        if (log.isDebugEnabled()) {
            log.debug("Adding Flow Continuable Mediator : " + idString);
        }
//        log.info("Adding Flow Continuable Mediator : " + idString);
        process(idString, componentType, true);

        return idString;
    }

    public static void reportingBranchingEvents() {
        if (log.isDebugEnabled()) {
            log.debug("Starts branching (then/else/targets)");
        }
//        log.info("Starts branching (then/else/targets)");
        lastParent = stack.peek().getId();
        branching = true;
    }

    public static void reportingEndEvent(String name, ComponentType componentType) {
        if (log.isDebugEnabled()) {
            log.debug("Ending Component Initialization: " + name);
        }
//        log.info("Ending Component Initialization: " + name);
        // If event is a SEQ or Proxy - pop from stack, then update parent
        if (ComponentType.SEQUENCE == componentType || ComponentType.PROXYSERVICE == componentType
            || ComponentType.API ==componentType || ComponentType.RESOURCE == componentType
            || ComponentType.INBOUNDENDPOINT == componentType) {

            stack.pop();
            if (!stack.isEmpty()) {
                lastParent = stack.peek().getId();
            }
        }
        if (ComponentType.MEDIATOR == componentType) {
            stack.pop();
        }
    }

    public static void reportingFlowContinuableEndEvent(String mediatorId, ComponentType mediator) {
        if (log.isDebugEnabled()) {
            log.debug("Ending Flow Continuable Mediator Initialization: " + mediatorId);
        }
//        log.info("Ending Flow Continuable Mediator Initialization: " + mediatorId);
        lastParent = stack.peek().getId();
        stack.pop();
        branching = false;

    }

    public static void reportingEndBranchingEvent() {
//        System.out.println("Branching Ended, IF~else // Clone Targets");
    }

    public static String getHashCode() {
        if (log.isDebugEnabled()) {
            log.debug("Hash Code Given to the component is :" + hashCode);
        }
//        log.info("Hash Code Given to the component is :" + hashCode);
        return String.valueOf(hashCode);
    }

    public static void setSynapseConfiguration(SynapseConfiguration synapseConfiguration) {
        StatisticIdentityGenerator.synapseConfiguration = synapseConfiguration;
    }

    private static void process(String name, ComponentType componentType, boolean flowContinuable) {
        if (ComponentType.PROXYSERVICE == componentType || ComponentType.API == componentType || ComponentType.INBOUNDENDPOINT == componentType) {
            StructuringElement proxyElem = new StructuringElement(name, componentType, false);
            stack.push(proxyElem);
            list.add(proxyElem);
            lastParent = name;
        }

        if (ComponentType.SEQUENCE == componentType) {
            StructuringElement seqElem = new StructuringElement(name, componentType, false);
            if (stack.isEmpty()) {
                // This is directly deploying a sequence
                stack.push(seqElem);
                list.add(seqElem);
            } else {
                // There's a parent for sequence
                seqElem.setParentId(lastParent);
                stack.push(seqElem);
                list.add(seqElem);
            }
            lastParent = name;
        }

        if (ComponentType.RESOURCE == componentType) {
            StructuringElement resourceElem = new StructuringElement(name, componentType, false);
            // There must be an API, which has this resource
            resourceElem.setParentId(stack.peek().getId());
            list.add(resourceElem);
            lastParent = name;
            stack.push(resourceElem);
        }

        if (ComponentType.MEDIATOR == componentType) {
            StructuringElement medElem = new StructuringElement(name, componentType, branching);
            medElem.setParentId(lastParent);
            if (stack.isEmpty()) {
                // This is not a desired situation! Mediators always lies inside a sequence
                log.error("Sequence is missing for mediator : " + name);
            }
            list.add(medElem);
            lastParent = name;
            stack.push(medElem);
        }

        if (ComponentType.ENDPOINT == componentType) {
            StructuringElement endpointElem = new StructuringElement(name, componentType, false);

            // Add parent only the endpoint is called by a mediator
            if (!stack.isEmpty()) {
                endpointElem.setParentId(stack.peek().getId());
            }
            list.add(endpointElem);
        }
    }
}
