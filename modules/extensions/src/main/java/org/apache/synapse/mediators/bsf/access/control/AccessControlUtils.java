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


import java.util.Comparator;
import java.util.List;

import org.apache.synapse.script.access.AccessControlConfig;
import org.apache.synapse.script.access.AccessControlListType;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

/**
 * Utility methods related to Script Mediator access control.
 */
public class AccessControlUtils {

    /**
     * Returns whether the provided string which represents a Java class or native object is accessible or not.
     * The allowing/blocking will be determined by the provided AccessControlConfig, based on the matching/comparing
     * done as specified in the comparator.
     * @param string                Java class name or native object name.
     * @param accessControlConfig   Access control config of the Script Mediator.
     * @param comparator            The comparator based on which, the provided Java class/native object name is
     *                              matched against the provided access control config.
     * @return                      Whether the access is allowed or not.
     */
    public static boolean isAccessAllowed(String string, AccessControlConfig accessControlConfig,
                                          Comparator<String> comparator) {
        if (accessControlConfig == null || !accessControlConfig.isAccessControlEnabled()) {
            return true; // Access control is not applicable
        }

        List<String> accessControlList = accessControlConfig.getAccessControlList();
        boolean doesMatchExist = false;
        for (String item : accessControlList) {
            if (comparator.compare(string, item) > -1) {
                doesMatchExist = true;
                break;
            }
        }

        if (accessControlConfig.getAccessControlListType() == AccessControlListType.BLOCK_LIST) {
            return !doesMatchExist;
        }
        if (accessControlConfig.getAccessControlListType() == AccessControlListType.ALLOW_LIST) {
            return doesMatchExist;
        }
        return true; // Ideally we won't reach here
    }

    public static Context.Builder createSecureGraalContext(AccessControlConfig classAccessControlConfig) {

        Context.Builder builder = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(s -> isAccessAllowed(s, classAccessControlConfig, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        if (o1 != null && o1.startsWith(o2)) {
                            return 0;
                        }
                        return -1;
                    }
                }));
        return builder;
    }
}
