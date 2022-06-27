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
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.aspects.flow.statistics.structuring.StructuringArtifact;
import org.apache.synapse.aspects.flow.statistics.structuring.StructuringElement;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.ArtifactHolderStore;
import org.apache.synapse.config.SynapseConfiguration;

public class StatisticIdentityGenerator {

    private static Log log = LogFactory.getLog(StatisticIdentityGenerator.class);

    private static SynapseConfiguration synapseConfiguration;

    public static void conclude(ArtifactHolder holder) {
        if (holder.getList().size() > 0) {
            String artifactName = holder.getParent();
            StructuringArtifact structuringArtifact = new StructuringArtifact(holder.getHashCode(), artifactName, holder.getList());
            if (synapseConfiguration != null) {
                synapseConfiguration.getCompletedStructureStore().putCompletedStatisticEntry(structuringArtifact);
            }
        }
    }

    public static String getIdForComponent(String name, ComponentType componentType, ArtifactHolder holder) {
        String idString = holder.getParent() + "@" + holder.getIdString() + ":" + name;
        holder.setHashCode(holder.getHashCode() + idString.hashCode());

        if (log.isDebugEnabled()) {
            log.debug("Adding Component : " + idString);
        }
        if (RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
            ArtifactHolderStore.addStructuringElementStack(idString, holder);
        }
        process(idString, componentType, holder);

        return idString;
    }

    public static String getIdReferencingComponent(String name, ComponentType componentType, ArtifactHolder holder) {
        String idString = name + "@" + holder.getIdString() + ":" + name + "@indirect";
//        String idString = name + "@0:" + name;
//        holder.setId(holder.getId()+1);
        holder.setHashCode(holder.getHashCode() + idString.hashCode());
        if (log.isDebugEnabled()) {
            log.debug("Adding Referencing Component  : " + idString);
        }
        if (RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
            ArtifactHolderStore.addStructuringElementStack(idString, holder);
        }
        process(idString, componentType, holder);

        return idString;
    }

    public static String getIdForFlowContinuableMediator(String mediatorName, ComponentType componentType, ArtifactHolder holder) {

        if (ComponentType.SEQUENCE == componentType && mediatorName.contains("AnonymousSequence")) {
            return null;
        }

        String idString = holder.getParent() + "@" + holder.getIdString() + ":" + mediatorName;
        holder.setHashCode(holder.getHashCode() + idString.hashCode());

        if (log.isDebugEnabled()) {
            log.debug("Adding Flow Continuable Mediator : " + idString);
        }
        if (RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
            ArtifactHolderStore.addStructuringElementStack(idString, holder);
        }
        process(idString, componentType, holder);

        return idString;
    }

    public static void reportingBranchingEvents(ArtifactHolder holder) {
        if (log.isDebugEnabled()) {
            log.debug("Starts branching (then/else/targets)");
        }
        holder.setLastParent(holder.getStack().peek().getId());
    }

    public static void reportingEndEvent(String name, ComponentType componentType, ArtifactHolder holder) {
        if (log.isDebugEnabled()) {
            log.debug("Ending Component Initialization: " + name);
        }
        // If event is a SEQ or Proxy - pop from stack, then update parent
        if (ComponentType.SEQUENCE == componentType || ComponentType.PROXYSERVICE == componentType
            || ComponentType.API ==componentType || ComponentType.RESOURCE == componentType
            || ComponentType.INBOUNDENDPOINT == componentType) {

            holder.getStack().pop();
            if (!holder.getStack().isEmpty()){
                holder.setLastParent(holder.getStack().peek().getId());
            }
        }
        if (ComponentType.MEDIATOR == componentType) {
            holder.getStack().pop();
        }
    }

    public static void reportingFlowContinuableEndEvent(String mediatorId, ComponentType mediator, ArtifactHolder holder) {
        if (log.isDebugEnabled()) {
            log.debug("Ending Flow Continuable Mediator Initialization: " + mediatorId);
        }
        holder.setLastParent(holder.getStack().peek().getId());
        holder.getStack().pop();
        holder.setExitFromBox(true);
    }

    public static void reportingEndBranchingEvent(ArtifactHolder holder) {
        if (log.isDebugEnabled()) {
            log.debug("Ending Branching Event");
        }
    }


    public static void setSynapseConfiguration(SynapseConfiguration synapseConfiguration) {
        StatisticIdentityGenerator.synapseConfiguration = synapseConfiguration;
    }

    private static void process(String name, ComponentType componentType, ArtifactHolder holder) {
        if (ComponentType.PROXYSERVICE == componentType || ComponentType.API == componentType || ComponentType.INBOUNDENDPOINT == componentType) {
            StructuringElement proxyElem = new StructuringElement(name, componentType);
            holder.getStack().push(proxyElem);
            holder.getList().add(proxyElem);
            holder.setLastParent(name);
        }

        if (ComponentType.SEQUENCE == componentType) {
            StructuringElement seqElem = new StructuringElement(name, componentType);
            if (holder.getStack().isEmpty()) {
                // This is directly deploying a sequence
                holder.getStack().push(seqElem);
                holder.getList().add(seqElem);
            } else {
                // There's a parent for sequence
                seqElem.setParentId(holder.getLastParent());
                seqElem.setGroup(holder.getStack().peek().getId());
                holder.getStack().push(seqElem);
                holder.getList().add(seqElem);
            }
            holder.setLastParent(name);
        }

        if (ComponentType.RESOURCE == componentType) {
            StructuringElement resourceElem = new StructuringElement(name, componentType);
            // There must be an API, which has this resource
            resourceElem.setParentId(holder.getStack().peek().getId());
            resourceElem.setGroup(holder.getStack().peek().getId());
            holder.getList().add(resourceElem);
            holder.setLastParent(name);
            holder.getStack().push(resourceElem);
        }

        if (ComponentType.MEDIATOR == componentType) {
            StructuringElement medElem = new StructuringElement(name, componentType);
            if (holder.getExitFromBox()){
                holder.setExitFromBox(false);
            }
            medElem.setParentId(holder.getLastParent());
            medElem.setGroup(holder.getStack().peek().getId());
            if (holder.getStack().isEmpty()) {
                // This is not a desired situation! Mediators always lies inside a sequence
                log.error("Sequence is missing for mediator : " + name);
            }
            holder.getList().add(medElem);
            holder.setLastParent(name);
            holder.getStack().push(medElem);
        }

        if (ComponentType.ENDPOINT == componentType) {
            StructuringElement endpointElem = new StructuringElement(name, componentType);

            // Add parent only the endpoint is called by a mediator
            if (!holder.getStack().isEmpty()) {
                endpointElem.setParentId(holder.getStack().peek().getId());
                endpointElem.setGroup(holder.getStack().peek().getId());
            }
            holder.getList().add(endpointElem);
        }
    }
}
