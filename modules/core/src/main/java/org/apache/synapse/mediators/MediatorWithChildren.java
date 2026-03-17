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

import java.util.List;

/**
 * Marker interface for mediators that contain child sequences (e.g., on-accept/on-reject, on-cache-hit, etc.).
 * This allows the mediator registry to discover and register child mediators
 * without requiring compile-time dependencies on specific mediator implementations.
 * 
 * <p>Examples of mediators with children:
 * <ul>
 *   <li>ThrottleMediator - has on-accept and on-reject child sequences</li>
 *   <li>CacheMediator - has on-cache-hit child sequence</li>
 * </ul>
 */
public interface MediatorWithChildren {
    
    /**
     * Get all child sequences in this mediator.
     * 
     * @return list of child sequences, or empty list if no children are configured
     */
    List<ChildSequence> getChildren();
}
