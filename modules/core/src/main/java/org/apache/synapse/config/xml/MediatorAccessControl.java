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

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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

    /** Namespace of the Synapse mediator implementations that the name fallback covers. */
    private static final String SYNAPSE_MEDIATOR_PACKAGE = "org.apache.synapse.mediators.";

    private static MediatorAccessControlConfig config = MediatorAccessControlConfig.NONE;

    /**
     * Resolves a mediator implementation class to the XML element it is registered under.
     *
     * Two lookups are held. byClassName is keyed on the fully qualified class name each mediator
     * serializer declares through getMediatorClassName(), which is authoritative - the serializer
     * names the implementation class directly rather than it being inferred. bySimpleName is a
     * fallback for the few registered mediators that have a factory but no serializer, and is keyed
     * on the implementation name derived from the factory class name.
     */
    private static final class MediatorElementIndex {

        final Map<String, String> byClassName;
        final Map<String, String> bySimpleName;

        MediatorElementIndex(Map<String, String> byClassName, Map<String, String> bySimpleName) {
            this.byClassName = byClassName;
            this.bySimpleName = bySimpleName;
        }
    }

    /** Built lazily and invalidated by init(), so a MediatorFactoryFinder.reset() is picked up. */
    private static volatile MediatorElementIndex elementIndex = null;

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

        // Drop any index built against a previous factory registry.
        elementIndex = null;
    }

    /**
     * Checks whether the class named by a &lt;class&gt; element may be used.
     *
     * The class mediator resolves an arbitrary fully qualified class name, so without this check
     * the configured policy is applied to the surface element only: a mediator excluded from the
     * access control list stays reachable by naming its implementation class through an allowed
     * &lt;class&gt; element. This applies the decision already configured for the element that the
     * implementation is registered under, so &lt;transaction/&gt; and
     * &lt;class name="...TransactionMediator"/&gt; resolve identically.
     *
     * Only implementations that back a registered mediator element are considered. Classes with no
     * registered element - customer mediators, and the gateway mediators that API Manager itself
     * invokes through &lt;class&gt; - are unaffected, because the mediator access control list never
     * governed them.
     *
     * MUST be called after the class is resolved but BEFORE it is instantiated: constructing a
     * class runs its static initialisers and constructor.
     *
     * @param resolved the class named by the &lt;class&gt; element
     * @throws MediatorAccessControlException if the implementation backs an element that is not permitted
     */
    public static void checkByClass(Class<?> resolved) {

        MediatorAccessControlConfig mediatorAccessControlConfig = config;
        if (resolved == null || AccessControlListType.NONE == mediatorAccessControlConfig.listType) {
            return;
        }

        MediatorElementIndex index = getElementIndex();
        String elementName = index.byClassName.get(resolved.getName());

        if (elementName == null && resolved.getName().startsWith(SYNAPSE_MEDIATOR_PACKAGE)) {
            // Mediators that have a factory but no serializer cannot be matched by class name, so
            // they fall back to the name derived from the factory. Restricted to Synapse's own
            // mediator package, since a name derived from a factory is not an identity: without the
            // restriction a customer class that happened to share the name would inherit a decision
            // meant for the Synapse implementation.
            elementName = index.bySimpleName.get(resolved.getSimpleName().toLowerCase(Locale.ROOT));
        }

        if (elementName == null) {
            // Not the implementation of any registered mediator element.
            return;
        }

        if (isBlocked(mediatorAccessControlConfig, elementName.toLowerCase(Locale.ROOT))) {
            String msg = "Mediator implementation '" + resolved.getName()
                    + "' is registered under the mediator element '" + elementName
                    + "', which is not permitted by mediator access control. It cannot be reached "
                    + "through a class mediator either.";
            // Logged at WARN because the aggregated error raised by the caller reports only the
            // element name, which is indistinguishable from a direct element rejection.
            log.warn(msg);
            throw new MediatorAccessControlException(msg, elementName);
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
    * Evaluates the configured allowlist or blocklist without throwing.
    *
    * @param mediatorAccessControlConfig the current access control configuration
    * @param normalizedName the mediator element name, lowercased
    * @return true if the name is not permitted
    */
    private static boolean isBlocked(MediatorAccessControlConfig mediatorAccessControlConfig,
                                     String normalizedName) {

        if (AccessControlListType.BLOCK_LIST == mediatorAccessControlConfig.listType) {
            return mediatorAccessControlConfig.mediators.contains(normalizedName);
        } else if (AccessControlListType.ALLOW_LIST == mediatorAccessControlConfig.listType) {
            return !mediatorAccessControlConfig.mediators.contains(normalizedName);
        }
        return false;
    }

    /**
    * Builds the implementation to element index by joining the two registries Synapse already
    * maintains.
    *
    * MediatorFactoryFinder maps an element QName to its factory class, giving the element name.
    * MediatorSerializerFinder maps a fully qualified implementation class name - declared by each
    * serializer through getMediatorClassName() - to that serializer, giving the implementation
    * class. The two are joined on the stem shared by a mediator's factory and serializer class
    * names, for example TransactionMediatorFactory and TransactionMediatorSerializer both reducing
    * to "TransactionMediator".
    *
    * Taking the class from the serializer rather than inferring it from the factory name matters
    * where a mediator does not follow the naming convention: ThrowErrorMediatorSerializer declares
    * org.apache.synapse.mediators.v2.ThrowError, which no derivation from
    * ThrowErrorMediatorFactory would produce. It also keys the result on the fully qualified name,
    * so a customer class cannot inherit a decision by sharing a simple name.
    *
    * A mediator that has a factory but no serializer cannot be resolved this way and is recorded in
    * the fallback map under the name derived from its factory.
    *
    * @return the index, never null
    */
    private static MediatorElementIndex getElementIndex() {

        MediatorElementIndex existing = elementIndex;
        if (existing != null) {
            return existing;
        }

        Map<String, String> byClassName = new HashMap<>();
        Map<String, String> bySimpleName = new HashMap<>();
        try {
            // stem -> implementation class name, from the serializers
            Map<String, String> stemToClassName = new HashMap<>();
            for (Map.Entry<String, MediatorSerializer> entry :
                    MediatorSerializerFinder.getInstance().getSerializerMap().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String stem = stripSuffix(entry.getValue().getClass().getSimpleName(), "Serializer");
                if (stem != null) {
                    stemToClassName.put(stem, entry.getKey());
                }
            }

            // stem -> element name, from the factories
            for (Map.Entry<QName, Class> entry :
                    MediatorFactoryFinder.getInstance().getFactoryMap().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String stem = stripSuffix(entry.getValue().getSimpleName(), "Factory");
                if (stem == null) {
                    continue;
                }
                String elementName = entry.getKey().getLocalPart();
                String className = stemToClassName.get(stem);
                if (className != null) {
                    byClassName.put(className, elementName);
                } else {
                    bySimpleName.put(stem.toLowerCase(Locale.ROOT), elementName);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Mediator element index built. Resolved by class name: " + byClassName.size()
                        + ", resolved by factory-derived name: " + bySimpleName.size()
                        + " " + bySimpleName.keySet());
            }
        } catch (Exception e) {
            // Never let index construction break deployment.
            log.warn("Could not build the mediator implementation to element index. Class mediator "
                    + "access control checks will be skipped this cycle.", e);
        }

        MediatorElementIndex built = new MediatorElementIndex(
                Collections.unmodifiableMap(byClassName), Collections.unmodifiableMap(bySimpleName));
        elementIndex = built;
        return built;
    }

    /**
    * @param name a class simple name
    * @param suffix the suffix to remove
    * @return name without the suffix, or null if it does not end with it or nothing would remain
    */
    private static String stripSuffix(String name, String suffix) {

        if (name == null || !name.endsWith(suffix) || name.length() == suffix.length()) {
            return null;
        }
        return name.substring(0, name.length() - suffix.length());
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
