/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.parentresolving;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers.TracingUtils;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.ArtifactHolderStore;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.SpanStore;
import org.apache.synapse.aspects.flow.statistics.structuring.StructuringElement;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

/**
 * Resolves parents based on the artifact holder, which is related to aspect configuration of elements.
 * Elements are uniquely named at the time of loading synapse configurations, and such names are referred in the
 * structuring element stack of an artifact holder - which states the path from a child to its holder.
 * Statistic data unit component unique id will give the same name, at the time of events collection.
 * This is used to correlate the aspect configuration with the statistic data unit.
 */
public class ArtifactHolderBasedParentResolver extends AbstractParentResolver {

    /**
     * Resolves parent span, based on the artifact holder.
     * The structuring element stack denoted by the child element's component unique id will be acquired,
     * and the parent span wrapper is found with respect to the component unique id that has been mentioned
     * in the stack.
     * When not found, null is returned.
     * @param child     Child statistic data unit.
     * @param spanStore Span store object.
     * @param synCtx    Message context of the child statistic data unit.
     * @return          Resolved parent span wrapper. Null when not available.
     */
    public static SpanWrapper resolveParent(StatisticDataUnit child, SpanStore spanStore, MessageContext synCtx) {
        String childUniqueId = child.getComponentId();
        Stack<StructuringElement> structuringElementStack =
                ArtifactHolderStore.getStructuringElementStack(childUniqueId);
        String parentComponentUniqueId = getReportedParentComponentUniqueId(structuringElementStack);
        if (parentComponentUniqueId != null) {
            return findSpanWrapperByComponentUniqueId(parentComponentUniqueId, child, spanStore, synCtx);
        }
        return null;
    }

    /**
     * Returns the parent component unique id, which can be obtained from the given structuring element stack.
     * Returns null, when none is obtained.
     * @param structuringElementStack   Structuring element stack of an artifact holder.
     * @return                          Parent component unique id.
     */
    private static String getReportedParentComponentUniqueId(Stack<StructuringElement> structuringElementStack) {
        String parentComponentUniqueId = null;
        if (structuringElementStack != null && !structuringElementStack.isEmpty()) {
            parentComponentUniqueId = structuringElementStack.peek().getId();
        }
        return parentComponentUniqueId;
    }

    /**
     * Returns the span wrapper which matches to the given component unique id.
     * When the span wrapper denoted by the component unique id contains anonymous sequences, suitable one among those
     * will be chosen and returned.
     * Otherwise, it will be examined whether is there any other copy exists with the same parent component unique id,
     * and the reference to the suitable one will be returned.
     * @param parentComponentUniqueId
     * @param child
     * @param spanStore
     * @param childSynCtx
     * @return
     */
    private static SpanWrapper findSpanWrapperByComponentUniqueId(String parentComponentUniqueId,
                                                                  StatisticDataUnit child,
                                                                  SpanStore spanStore,
                                                                  MessageContext childSynCtx) {
        SpanWrapper parent = spanStore.getSpanWrapperByComponentUniqueId(parentComponentUniqueId);
        if (parent != null) {
            if (!parent.getAnonymousSequences().isEmpty()) {
                /*
                The chosen parent contains anonymous sequences, which means that
                the actual parent should be an appropriate anonymous sequence.
                */
                return resolveAnonymousSequence(parent, child, spanStore, childSynCtx);
            }

            /*
            In cases like the the following one - where multiple copies of a proxy out sequence are created,
            the parent won't just be the one identified with the component unique id,
            but one of its copies with the same parent unique id.

            <target>
                <inSequence> <iterate> ... </iterate> </inSequence>
                <outSequence> <aggregate> ... </aggregate> </outSequence>
            </target>

            In such cases,
            find the appropriate copy of the parent
            which doesn't already contain an element with the same child unique id as a child.
            */
            if (!isAlreadyAParent(parent, child.getComponentId())) {
                return parent;
            }
            for (SpanWrapper spanWrapper : spanStore.getSpanWrappers().values()) {
                if (Objects.equals(parentComponentUniqueId, spanWrapper.getStatisticDataUnit().getComponentId()) &&
                        !isAlreadyAParent(spanWrapper, child.getComponentId())) {
                    return spanWrapper;
                }
            }
        }
        return parent;
    }

    /**
     * Returns whether the given parent span wrapper already has a child with the given child component unique id.
     * This only considers a 'component unique id - component unique id' relationship
     * @param parent                    Parent span wrapper.
     * @param childComponentUniqueId    Child component unique id (a.k.a structured element id).
     * @return                          Whether the parent already has a child with the child component unique id.
     */
    private static boolean isAlreadyAParent(SpanWrapper parent, String childComponentUniqueId) {
        return parent.getChildStructuredElementIds().contains(childComponentUniqueId);
    }

    /**
     * Resolves the appropriate anonymous sequence which is contained by the given anonymous sequence container,
     * as the parent.
     * The message context's system identity hash code is used to find the appropriate anonymous sequence.
     * A span wrapper has a list of message context system identity hash codes (so does the anonymous sequence
     * container), which denotes the message contexts that are known by it.
     * A known message context is a message context, that the component denoted by the span wrapper has branched
     * (travelled) through.
     *
     * When the message context of the child is known by any of the anonymous sequences,
     * that anonymous sequence is resolved.
     * When it is unknown by any of those, a known message context is mapped and derived from the unknown message
     * context of the child, and the respective anonymous sequence is resolved.
     * When this too doesn't resolve an anonymous sequence directly, the latest anonymous sequence is returned.
     * @param anonymousSequenceContainer    A span wrapper which contains one or more anonymous sequences.
     * @param child                         Child statistic data unit.
     * @param spanStore                     Span store.
     * @param childSynCtx                   Message context of the child.
     * @return                              Resolved anonymous sequence.
     */
    private static SpanWrapper resolveAnonymousSequence(SpanWrapper anonymousSequenceContainer,
                                                        StatisticDataUnit child,
                                                        SpanStore spanStore,
                                                        MessageContext childSynCtx) {
        SpanWrapper parent =
                resolveAnonymousSequenceFromKnownSynCtx(
                        TracingUtils.getSystemIdentityHashCode(childSynCtx),
                        anonymousSequenceContainer);
        if (parent != null) {
            return parent;
        }

        /*
        SynCtx is not known. Identify it by going through synCtx's of message flow representation parents.
        */
        parent = resolveAnonymousSequenceFromUnknownSynCtx(child, childSynCtx, anonymousSequenceContainer, spanStore);
        if (parent != null) {
            return parent;
        }

        return anonymousSequenceContainer.getLatestAnonymousSequence();
    }

    /**
     * Resolves the anonymous sequence span wrapper which knows the message context, that is represented by the given
     * system identity hash code. Returns null when nothing is resolved.
     * @param synCtxHashCode                System identity hash code of a message context.
     * @param anonymousSequenceContainer    Span wrapper which contains one or more anonymous sequences.
     * @return                              Resolved anonymous sequence span wrapper.
     */
    private static SpanWrapper resolveAnonymousSequenceFromKnownSynCtx(String synCtxHashCode,
                                                                       SpanWrapper anonymousSequenceContainer) {
        for (SpanWrapper anonymousSequence : anonymousSequenceContainer.getAnonymousSequences().values()) {
            if (anonymousSequence.getKnownSynCtxHashCodes().contains(synCtxHashCode)) {
                return anonymousSequence;
            }
        }
        return null;
    }

    /**
     * Finds known message context system identity hash code related to the given unknown message context,
     * and resolves an anonymous sequence for that. Returns null when nothing is returned.
     * @param child                         Child statistic data unit.
     * @param unknownSynCtx                 Unknown message context.
     * @param anonymousSequenceContainer    Span wrapper that contains one or more anonymous sequences.
     * @param spanStore                     Span store.
     * @return                              Resolved anonymous sequence span wrapper.
     */
    private static SpanWrapper resolveAnonymousSequenceFromUnknownSynCtx(StatisticDataUnit child,
                                                                         MessageContext unknownSynCtx,
                                                                         SpanWrapper anonymousSequenceContainer,
                                                                         SpanStore spanStore) {
        Set<String> knownSynCtxHashCodes = new HashSet<>();
        for (SpanWrapper anonymousSequence : anonymousSequenceContainer.getAnonymousSequences().values()) {
            knownSynCtxHashCodes.addAll(anonymousSequence.getKnownSynCtxHashCodes());
        }

        String knownSynCtxHashCode =
                findKnownSynCtxHashCode(
                        child,
                        TracingUtils.getSystemIdentityHashCode(unknownSynCtx),
                        knownSynCtxHashCodes,
                        spanStore);
        if (knownSynCtxHashCode != null) {
            return resolveAnonymousSequenceFromKnownSynCtx(knownSynCtxHashCode, anonymousSequenceContainer);
        }
        return null;
    }

    /**
     * Returns the known message context system identity hash code, related to the given unknown message context
     * system identity hash code.
     * The parent index that is given by the statistic data unit message flow
     * representation is used to find an element which has the known message context, because this flow represents
     * how message contexts have been branched.
     * Returns null if no known message context system identity hash codes are found.
     * @param child                 Child statistic data unit.
     * @param unknownSynCtxHashCode System identity hash code of an unknown message context.
     * @param knownSynCtxHashCodes  System identity hash codes of known message contexts.
     * @param spanStore             Span store.
     * @return                      System identity hash code of a known message context.
     */
    private static String findKnownSynCtxHashCode(StatisticDataUnit child,
                                                  String unknownSynCtxHashCode,
                                                  Set<String> knownSynCtxHashCodes,
                                                  SpanStore spanStore) {
        SpanWrapper messageFlowParent = null;
        if (child != null) {
            messageFlowParent = spanStore.getSpanWrapper(String.valueOf(child.getParentIndex()));
        }
        while (messageFlowParent != null) {
            // Check whether any of the synCtx hash code - that the parent has gone through, is known
            for (String synCtxIdentityHashCode : messageFlowParent.getKnownSynCtxHashCodes()) {
                if (knownSynCtxHashCodes.contains(synCtxIdentityHashCode)) {
                    /*
                    Add unknown synCtx hash code as a known synCtx hash code,
                    to the referrer who has the related known synCtx
                     */
                    messageFlowParent.addKnownSynCtxHashCodeToAllParents(unknownSynCtxHashCode);
                    return synCtxIdentityHashCode;
                }
            }
            messageFlowParent =
                    spanStore.getSpanWrapper(String.valueOf(messageFlowParent.getStatisticDataUnit().getParentIndex()));
        }
        return null;
    }
}
