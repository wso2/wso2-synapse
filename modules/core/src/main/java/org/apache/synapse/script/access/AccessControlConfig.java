/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.apache.synapse.script.access;

import java.util.List;

/**
 * Holds the configurations that are used for access control in the Script Mediator.
 */
public class AccessControlConfig {

    private boolean isAccessControlEnabled;
    private AccessControlListType accessControlListType;
    private List<String> accessControlList;

    public AccessControlConfig(boolean isAccessControlEnabled, AccessControlListType accessControlListType,
                               List<String> accessControlList) {
        this.isAccessControlEnabled = isAccessControlEnabled;
        this.accessControlListType = accessControlListType;
        this.accessControlList = accessControlList;
    }

    public boolean isAccessControlEnabled() {
        return isAccessControlEnabled;
    }

    public AccessControlListType getAccessControlListType() {
        return accessControlListType;
    }

    public List<String> getAccessControlList() {
        return accessControlList;
    }
}

