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

package org.apache.synapse.mediators.bsf.access.control;

import org.apache.synapse.mediators.bsf.access.control.config.AccessControlConfig;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import java.util.Comparator;

/**
 * Provides native Java objects to the sandbox, after necessary access control filtering.
 */
public class SandboxNativeJavaObject extends NativeJavaObject {
    private AccessControlConfig nativeObjectAccessControlConfig;

    public SandboxNativeJavaObject(Scriptable scope, Object javaObject, Class staticType,
                                   AccessControlConfig nativeObjectAccessControlConfig) {
        super(scope, javaObject, staticType);
        this.nativeObjectAccessControlConfig = nativeObjectAccessControlConfig;
    }

    @Override
    public Object get(String name, Scriptable start) {
        Comparator<String> equalsComparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1 != null && o1.equals(o2)) {
                    return 0;
                }
                return -1;
            }
        };
        if (AccessControlUtils.isAccessAllowed(name, nativeObjectAccessControlConfig, equalsComparator)) {
            return super.get(name, start);
        }
        return NOT_FOUND;
    }

}

