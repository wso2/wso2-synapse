/*
 *  Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;

import java.util.Stack;

/**
 * Maintains a per-message stack of span wrapper IDs, tracking the chain of currently open spans so that the most
 * recently started span can be resolved as the parent of the next one.
 * <p>
 * The stack is stored as a message context property ({@link SynapseConstants#PARENT_STACK_PROPERTY}) and is therefore
 * carried along the message flow, and cloned when the message context is cloned.
 */
public final class ParentSpanWrapperStackManager {

    private ParentSpanWrapperStackManager() {}

    /**
     * Pushes the given span wrapper ID onto the parent stack of the message, creating the stack if it does not
     * exist yet.
     *
     * @param spanWrapperId ID of the span wrapper that has just been started.
     * @param synCtx        Message context that owns the stack.
     */
    public static void push(String spanWrapperId, MessageContext synCtx) {
        if (spanWrapperId == null || synCtx == null) {
            return;
        }
        Stack<String> parentStack = getStack(synCtx);
        if (parentStack == null) {
            parentStack = new Stack<>();
            synCtx.setProperty(SynapseConstants.PARENT_STACK_PROPERTY, parentStack);
        }
        parentStack.push(spanWrapperId);
    }

    /**
     * Pops the topmost span wrapper ID from the parent stack of the message, if a non-empty stack exists.
     *
     * @param synCtx Message context that owns the stack.
     */
    public static void pop(MessageContext synCtx) {
        if (synCtx == null) {
            return;
        }
        Stack<String> parentStack = getStack(synCtx);
        if (parentStack != null && !parentStack.isEmpty()) {
            parentStack.pop();
        }
    }

    /**
     * Returns the span wrapper ID at the top of the parent stack without removing it.
     *
     * @param synCtx Message context that owns the stack.
     * @return       The current parent span wrapper ID, or {@code null} if there is no parent.
     */
    public static String peekParentSpanWrapperId(MessageContext synCtx) {
        if (synCtx == null) {
            return null;
        }
        Stack<String> parentStack = getStack(synCtx);
        if (parentStack != null && !parentStack.isEmpty()) {
            return parentStack.peek();
        }
        return null;
    }

    /**
     * Creates a copy of the given parent stack. Used when a message context is cloned, so that each branch
     * maintains its own independent stack.
     *
     * @param parentStack Parent stack to copy.
     * @return            A new stack containing the same span wrapper IDs.
     */
    public static Stack<String> copyOf(Stack<String> parentStack) {
        Stack<String> clone = new Stack<>();
        if (parentStack != null){
            clone.addAll(parentStack);
        }
        return clone;
    }

    @SuppressWarnings("unchecked")
    private static Stack<String> getStack(MessageContext synCtx) {
        Object parentStackObj = synCtx.getProperty(SynapseConstants.PARENT_STACK_PROPERTY);
        if (parentStackObj instanceof Stack) {
            return (Stack<String>) parentStackObj;
        }
        return null;
    }
}
