/*
 * Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.util;

import org.apache.logging.log4j.ThreadContext;
import org.apache.synapse.MessageContext;

/**
 * MediatorIdLogSetter manages setting and clearing mediator IDs in ThreadContext for logging.
 * Stores the current mediator ID in MessageContext to handle thread switches.
 */
public class MediatorIdLogSetter {
    private static final MediatorIdLogSetter instance = new MediatorIdLogSetter();
    private static final String MEDIATOR_ID_KEY = "MediatorId";
    private static final String MSG_CTX_MEDIATOR_ID = "_CURRENT_MEDIATOR_ID";

    private MediatorIdLogSetter() {
    }

    /**
     * Get the singleton instance.
     * 
     * @return the singleton instance
     */
    public static MediatorIdLogSetter getInstance() {
        return instance;
    }

    /**
     * Sets the mediator ID in ThreadContext and MessageContext for logging.
     * Storing in MessageContext allows restoration after thread switches.
     * 
     * @param synCtx MessageContext to store the mediator ID
     * @param mediatorId The mediator ID to set (e.g., "api:helloAPI/GET[/]/1.Log")
     * @return true if the ID was set, false if mediatorId was null/empty
     */
    public boolean setMediatorId(MessageContext synCtx, String mediatorId) {
        if (mediatorId == null || mediatorId.isEmpty()) {
            return false;
        }
        
        // Store in MessageContext for thread switch restoration
        synCtx.setProperty(MSG_CTX_MEDIATOR_ID, mediatorId);
        
        // Set in ThreadContext for immediate logging
        ThreadContext.put(MEDIATOR_ID_KEY, mediatorId);
        return true;
    }

    /**
     * Clears the mediator ID from ThreadContext.
     * Note: Does not clear from MessageContext to allow restoration on thread switches.
     */
    public void clearMediatorId() {
        ThreadContext.remove(MEDIATOR_ID_KEY);
    }

    /**
     * Syncs the mediator ID from MessageContext to ThreadContext.
     * Called when execution switches to a new thread to restore the ThreadContext.
     * 
     * @param synCtx MessageContext containing the mediator ID
     */
    public void syncToThreadContext(MessageContext synCtx) {
        String mediatorId = (String) synCtx.getProperty(MSG_CTX_MEDIATOR_ID);
        if (mediatorId != null && !mediatorId.isEmpty()) {
            ThreadContext.put(MEDIATOR_ID_KEY, mediatorId);
        } else {
            ThreadContext.remove(MEDIATOR_ID_KEY);
        }
    }

    /**
     * Gets the mediator ID from ThreadContext for the current thread.
     * 
     * @return the mediator ID if present, null otherwise
     */
    public String getMediatorId() {
        return ThreadContext.get(MEDIATOR_ID_KEY);
    }
}
