/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.apache.synapse.config.xml;

import org.apache.synapse.SynapseException;

/**
 * Thrown when a mediator is blocked by mediator access control policy.
 * This is a subtype of SynapseException so existing catch blocks still work,
 * but it allows callers that need to aggregate multiple violations to catch
 * it specifically and continue processing.
 */
public class MediatorAccessControlException extends SynapseException {

    private static final long serialVersionUID = 1L;

    private final String mediatorName;

    public MediatorAccessControlException(String msg, String mediatorName) {
        super(msg);
        this.mediatorName = mediatorName;
    }

    public String getMediatorName() {
        return mediatorName;
    }
}
