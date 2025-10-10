/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

/**
 * Singleton class to hold the access control configurations of the Script Mediator.
 */
public class ScriptAccessControl {
    private static final ScriptAccessControl instance = new ScriptAccessControl();

    private AccessControlConfig classAccessControlConfig;
    private AccessControlConfig nativeObjectAccessControlConfig;

    private ScriptAccessControl() {
    }

    public static ScriptAccessControl getInstance() {
        return instance;
    }

    public AccessControlConfig getClassAccessControlConfig() {
        return classAccessControlConfig;
    }

    public void setClassAccessControlConfig(AccessControlConfig classAccessControlConfig) {
        this.classAccessControlConfig = classAccessControlConfig;
    }

    public AccessControlConfig getNativeObjectAccessControlConfig() {
        return nativeObjectAccessControlConfig;
    }

    public void setNativeObjectAccessControlConfig(AccessControlConfig nativeObjectAccessControlConfig) {
        this.nativeObjectAccessControlConfig = nativeObjectAccessControlConfig;
    }
}
