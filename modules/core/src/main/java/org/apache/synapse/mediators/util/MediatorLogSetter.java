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

/**
 * MediatorLogSetter manages the mediator ID in ThreadContext for logging purposes.
 */
public class MediatorLogSetter {
    private static final MediatorLogSetter instance = new MediatorLogSetter();
    private static final String MEDIATOR_ID_KEY = "MediatorId";

    private MediatorLogSetter() {
    }

    /**
     * Get the singleton instance.
     * 
     * @return the singleton instance
     */
    public static MediatorLogSetter getInstance() {
        return instance;
    }

    /**
     * Sets the mediator ID in ThreadContext for the current thread.
     * This ID will be automatically included in all log entries made on this thread.
     * 
     * @param mediatorId The mediator ID to set (e.g., "api:helloAPI/GET[/]/in/1.Log")
     */
    public void setMediatorId(String mediatorId) {
        if (mediatorId != null && !mediatorId.isEmpty()) {
            ThreadContext.put(MEDIATOR_ID_KEY, mediatorId);
        }
    }

    /**
     * Clears the mediator ID from ThreadContext for the current thread.
     * Should be called in a finally block to prevent ThreadContext leaks.
     */
    public void clearMediatorId() {
        ThreadContext.remove(MEDIATOR_ID_KEY);
    }
}
