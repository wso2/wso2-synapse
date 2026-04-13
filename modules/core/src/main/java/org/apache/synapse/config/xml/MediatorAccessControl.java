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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Centralized mediator access control that enforces allowlist or blocklist
 * policies at deployment time (XML parsing).
 *
 * Configuration via synapse.properties:
 *
 *   synapse.mediators.access.control.mode = NONE | BLOCK_LIST | ALLOW_LIST
 *   synapse.mediators.access.control.list = dbreport,dblookup,script,class
 */
public final class MediatorAccessControl {

    private static final Log log = LogFactory.getLog(MediatorAccessControl.class);

    private static final String MEDIATOR_ACCESS_CONTROL_MODE = "synapse.mediators.access.control.mode";
    private static final String MEDIATOR_ACCESS_CONTROL_LIST = "synapse.mediators.access.control.list";
    private static final String MODE_ALLOW_LIST = "allow_list";
    private static final String MODE_BLOCK_LIST = "block_list";
    private static final String MODE_NONE = "none";

    private enum AccessControlListType {
        NONE, ALLOW_LIST, BLOCK_LIST
    }

    /**
     * Immutable snapshot of access control configuration.
     */
    private static final class MediatorAccessControlConfig {

        static final MediatorAccessControlConfig NONE = new MediatorAccessControlConfig(
            AccessControlListType.NONE, Collections.<String>emptySet());

        final AccessControlListType listType;
        final Set<String> mediators;

        MediatorAccessControlConfig(AccessControlListType listType, Set<String> mediators) {
            this.listType = listType;
            this.mediators = mediators;
        }
    }

    private static MediatorAccessControlConfig config = MediatorAccessControlConfig.NONE;

    private MediatorAccessControl() {
    }

    /**
     * Loads access control configuration from synapse.properties.
     * Called once during MediatorFactoryFinder initialization.
     */
    public static void init() {

        String mode = SynapsePropertiesLoader.getPropertyValue(
                MEDIATOR_ACCESS_CONTROL_MODE, MODE_NONE).trim().toLowerCase(Locale.ROOT);

        if (MODE_BLOCK_LIST.equals(mode)) {
            String list = SynapsePropertiesLoader.getPropertyValue(MEDIATOR_ACCESS_CONTROL_LIST, "");
            Set<String> blocked = parseMediatorList(list);
            config = new MediatorAccessControlConfig(AccessControlListType.BLOCK_LIST, blocked);
            if (blocked.isEmpty()) {
                log.warn("Mediator access control mode is 'block_list' but no mediators are listed in '"
                        + MEDIATOR_ACCESS_CONTROL_LIST + "'. No mediators will be blocked. "
                        + "Configure the property to specify mediators to block.");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Mediator blocklist active. Blocked mediators: " + blocked);
                }
            }
        } else if (MODE_ALLOW_LIST.equals(mode)) {
            String list = SynapsePropertiesLoader.getPropertyValue(MEDIATOR_ACCESS_CONTROL_LIST, "");
            Set<String> allowed = parseMediatorList(list);
            config = new MediatorAccessControlConfig(AccessControlListType.ALLOW_LIST, allowed);
            if (allowed.isEmpty()) {
                log.warn("Mediator access control mode is 'allow_list' but no mediators are listed in '"
                        + MEDIATOR_ACCESS_CONTROL_LIST + "'. All mediators will be blocked. "
                        + "Configure the property to specify allowed mediators.");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Mediator allowlist active. Allowed mediators: " + allowed);
                }
            }
        } else if (!MODE_NONE.equals(mode)) {
            log.warn("Unknown mediator access control mode: '" + mode
                    + "'. Expected 'none', 'block_list', or 'allow_list'. Defaulting to 'none'.");
            config = MediatorAccessControlConfig.NONE;
        } else {
            config = MediatorAccessControlConfig.NONE;
        }
    }

    /**
     * Checks whether a mediator identified by its XML element local name is permitted.
     * Used at deployment time (MediatorFactoryFinder) to block parsing of restricted mediators.
     *
     * @param localName the local name of the mediator XML element (e.g. "dbreport", "script")
     * @throws SynapseException if the mediator is not permitted
     */
    public static void checkByElementName(String localName) {

        MediatorAccessControlConfig mediatorAccessControlConfig = config;
        if (AccessControlListType.NONE == mediatorAccessControlConfig.listType) {
            return;
        }
        String normalized = localName.toLowerCase(Locale.ROOT);
        checkMediatorName(mediatorAccessControlConfig, normalized, localName);
    }

    /**
    * Checks the given normalized mediator name against the configured allowlist or blocklist.
    *
    * @param mediatorAccessControlConfig the current access control configuration
    * @param normalizedName the mediator name normalized to match the XML element naming convention (e.g. "dbreport")
    * @param displayName the original name used for error messages (e.g. "DBReportMediator" or "dbreport")
    * @throws MediatorAccessControlException if the mediator is not permitted according to the access control policy
    */
    private static void checkMediatorName(MediatorAccessControlConfig mediatorAccessControlConfig,
                                          String normalizedName, String displayName) {

        if (AccessControlListType.BLOCK_LIST == mediatorAccessControlConfig.listType) {
            if (mediatorAccessControlConfig.mediators.contains(normalizedName)) {
                String msg = "Mediator '" + displayName + "' is blocked by mediator access control.";
                log.debug(msg);
                throw new MediatorAccessControlException(msg, displayName);
            }
        } else if (AccessControlListType.ALLOW_LIST == mediatorAccessControlConfig.listType) {
            if (!mediatorAccessControlConfig.mediators.contains(normalizedName)) {
                String msg = "Mediator '" + displayName + "' is not in the allowed mediators list.";
                log.debug(msg);
                throw new MediatorAccessControlException(msg, displayName);
            }
        }
    }

    /**
    * Parses a comma-separated list of mediator names from the configuration into a Set.
    * Trims whitespace and converts to lowercase for consistent matching.
    *
    * @param commaSeparatedList the raw list from configuration (e.g. "dbreport, dblookup, script")
    * @return an unmodifiable Set of normalized mediator names
    */
    private static Set<String> parseMediatorList(String commaSeparatedList) {

        if (commaSeparatedList == null || commaSeparatedList.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        for (String entry : commaSeparatedList.split(",")) {
            String trimmed = entry.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
