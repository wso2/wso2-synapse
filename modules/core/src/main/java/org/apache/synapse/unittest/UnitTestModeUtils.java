/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.unittest;

/**
 * Utility class for checking unit test mode.
 * Provides a cached check of the synapseTest system property to avoid repeated lookups.
 */
public final class UnitTestModeUtils {

    /**
     * Indicates whether the system is running in unit test mode.
     */
    private static final boolean IS_UNIT_TEST_MODE = Boolean.parseBoolean(System.getProperty("synapseTest"));

    private UnitTestModeUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns whether the system is running in unit test mode.
     *
     * @return true if running in unit test mode, false otherwise
     */
    public static boolean isUnitTestMode() {
        return IS_UNIT_TEST_MODE;
    }
}
