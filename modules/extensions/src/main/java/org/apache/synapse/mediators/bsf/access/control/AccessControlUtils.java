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
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

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

    /**
     * Creates a GraalVM Context.Builder with security restrictions applied as per the provided
     * AccessControlConfig.
     * Since we have used Nashorn compatibility mode, we need to allow experimental options and set the other parameters
     * as below which were the defaults when using the GraalJSEngineFactory directly.
     *
     * @param classAccessControlConfig Access control config related to Java class access
     * @return Context.Builder with security restrictions applied
     */
    public static Context.Builder createSecureGraalContext(AccessControlConfig classAccessControlConfig) {

        Context.Builder builder = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .option("js.syntax-extensions", "true")
                .option("js.load", "true")
                .option("js.script-engine-global-scope-import", "true")
                .option("js.charset", "UTF-8")
                .option("js.global-arguments", "true")
                .option("js.print", "true")
                .allowEnvironmentAccess(EnvironmentAccess.INHERIT)
                .useSystemExit(true)
                .allowAllAccess(true)
                .allowHostAccess(createNashornHostAccess())
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

    private static HostAccess createNashornHostAccess() {

        HostAccess.Builder b = HostAccess.newBuilder(HostAccess.ALL);
        b.targetTypeMapping(Value.class, String.class, (v) -> {
            return !v.isNull();
        }, (v) -> {
            return toString(v);
        }, HostAccess.TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Number.class, Integer.class, (n) -> {
            return true;
        }, (n) -> {
            return n.intValue();
        }, HostAccess.TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Number.class, Double.class, (n) -> {
            return true;
        }, (n) -> {
            return n.doubleValue();
        }, HostAccess.TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Number.class, Long.class, (n) -> {
            return true;
        }, (n) -> {
            return n.longValue();
        }, HostAccess.TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Number.class, Boolean.class, (n) -> {
            return true;
        }, (n) -> {
            return toBoolean(n.doubleValue());
        }, HostAccess.TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(String.class, Boolean.class, (n) -> {
            return true;
        }, (n) -> {
            return !n.isEmpty();
        }, HostAccess.TargetMappingPrecedence.LOWEST);
        return b.build();
    }

    private static String toString(Value value) {

        return toPrimitive(value).toString();
    }

    private static boolean isPrimitive(Value value) {

        return value.isString() || value.isNumber() || value.isBoolean() || value.isNull();
    }

    private static Value toPrimitive(Value value) {

        if (value.hasMembers()) {
            String[] var1 = new String[]{"toString", "valueOf"};
            int var2 = var1.length;

            for (int var3 = 0; var3 < var2; ++var3) {
                String methodName = var1[var3];
                if (value.canInvokeMember(methodName)) {
                    Value maybePrimitive = value.invokeMember(methodName, new Object[0]);
                    if (isPrimitive(maybePrimitive)) {
                        return maybePrimitive;
                    }
                }
            }
        }
        if (isPrimitive(value)) {
            return value;
        } else {
            throw new ClassCastException();
        }
    }

    private static boolean toBoolean(double d) {

        return d != 0.0 && !Double.isNaN(d);
    }
}
