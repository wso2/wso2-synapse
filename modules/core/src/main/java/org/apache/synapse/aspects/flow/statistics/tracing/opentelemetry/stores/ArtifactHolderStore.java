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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores;

import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.aspects.flow.statistics.structuring.StructuringElement;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Maintains the unique names of synapse configurations, and serves them when needed for parenting when applicable.
 * An artifact holder's structuring element stack represents the complete path from child to parent.
 */
public class ArtifactHolderStore {
    private static ConcurrentMap<String, Stack<StructuringElement>> structuringElementStacks =
            new ConcurrentHashMap<>();

    /**
     * Prevents Instantiation.
     */
    private ArtifactHolderStore() {}

    /**
     * Adds a deep clone of the provided artifact holder's structuring element stack,
     * in order to keep track of parents for serving when necessary.
     * @param componentUniqueId         Unique id of the component.
     * @param artifactHolderReference   Artifact holder object.
     */
    public static synchronized void addStructuringElementStack(String componentUniqueId,
                                                               ArtifactHolder artifactHolderReference) {
        if (artifactHolderReference != null) {
            Stack<StructuringElement> stackCopy = getCopiedStack(componentUniqueId, artifactHolderReference.getStack());
            structuringElementStacks.put(componentUniqueId, stackCopy);
        }
    }

    /**
     * Gets the structuring element stack, that is denoted with the given unique component id.
     * @param componentUniqueId Unique component id.
     * @return                  Structuring Elements stack denoted by the given component uniq id,
     *                          which contains the path from child to the parent.
     */
    public static Stack<StructuringElement> getStructuringElementStack(String componentUniqueId) {
        if (componentUniqueId != null) {
            return structuringElementStacks.get(componentUniqueId);
        }
        return null;
    }

    private static Stack<StructuringElement> getCopiedStack(String componentUniqueId, Stack<StructuringElement> stack) {
        Stack<StructuringElement> stackCopy = new Stack<>();
        if (structuringElementStacks.containsKey(componentUniqueId)) {
            // Get the existing stack, which is already a copy
            stackCopy = structuringElementStacks.get(componentUniqueId);
        }
        if (stack != null) {
            for (StructuringElement structuringElement : stack) {
                stackCopy.push(structuringElement);
            }
        }
        return stackCopy;
    }
}
