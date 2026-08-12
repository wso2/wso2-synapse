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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
    static final class MediatorElementIndex {

        /** Returned, and deliberately not cached, when the index cannot be built. */
        static final MediatorElementIndex EMPTY = new MediatorElementIndex(
            Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), -1, -1);

        final Map<String, String> byClassName;
        final Map<String, String> bySimpleName;
        /** Registry sizes this index was built from, used to detect runtime registration. */
        final int serializerCount;
        final int factoryCount;

        MediatorElementIndex(Map<String, String> byClassName, Map<String, String> bySimpleName,
                             int serializerCount, int factoryCount) {
            this.byClassName = byClassName;
            this.bySimpleName = bySimpleName;
            this.serializerCount = serializerCount;
            this.factoryCount = factoryCount;
        }
    }

    /** Built lazily and invalidated by init(), so a MediatorFactoryFinder.reset() is picked up. */
    private static volatile MediatorElementIndex elementIndex = null;

    private MediatorAccessControl() {
    }

    /**
     * Discards the cached implementation to element index so that it is rebuilt on next use.
     *
     * Called when a mediator factory or serializer is registered at runtime. The size comparison in
     * getElementIndex() catches a registration that grows a registry, but a registration that
     * replaces an existing entry - an extension supplying its own factory for an element that
     * already exists, or a redeployed extension jar, which is always a replacement because
     * ExtensionDeployer does not remove entries on undeploy - leaves the size unchanged. Being told
     * is exact where comparing sizes is only a proxy.
     */
    public static void invalidateIndex() {

        elementIndex = null;
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
    * Resolves a mediator implementation class to the element it is registered under.
    *
    * An exact match on the fully qualified name is preferred. Mediators that have a factory but no
    * serializer cannot be matched that way and fall back to the name derived from the factory; that
    * fallback is restricted to Synapse's own mediator package, because a name derived from a
    * factory is not an identity and without the restriction a customer class sharing the name would
    * inherit a decision meant for the Synapse implementation.
    *
    * Package private so the resolution, including that package gate, can be tested directly.
    *
    * @param index the implementation to element index
    * @param resolved the class named by the &lt;class&gt; element
    * @return the element name, or null if the class backs no registered mediator element
    */
    static String resolveElement(MediatorElementIndex index, Class<?> resolved) {

        String elementName = index.byClassName.get(resolved.getName());
        if (elementName == null && resolved.getName().startsWith(SYNAPSE_MEDIATOR_PACKAGE)) {
            elementName = index.bySimpleName.get(resolved.getSimpleName().toLowerCase(Locale.ROOT));
        }
        return elementName;
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

        String elementName = resolveElement(getElementIndex(), resolved);

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

        if (!isBlocked(mediatorAccessControlConfig, normalizedName)) {
            return;
        }
        String msg = AccessControlListType.BLOCK_LIST == mediatorAccessControlConfig.listType
                ? "Mediator '" + displayName + "' is blocked by mediator access control."
                : "Mediator '" + displayName + "' is not in the allowed mediators list.";
        log.debug(msg);
        throw new MediatorAccessControlException(msg, displayName);
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
    * Returns the implementation to element index, building it if it is absent or if either
    * registry has changed size since it was built.
    *
    * MediatorFactoryFinder and MediatorSerializerFinder are both mutated at runtime by
    * ExtensionDeployer when a mediator extension is deployed, outside the initialisation that
    * init() hooks. Comparing the registry sizes the index was built from means such a registration
    * is picked up, rather than leaving the new implementation ungoverned for the lifetime of the
    * JVM.
    *
    * The factory registry is copied under the MediatorFactoryFinder class monitor, which is the
    * lock reset() and loadMediatorFactories() hold. That does not make the copy atomic against
    * ExtensionDeployer, which puts into the map without holding it, so a concurrent registration
    * can still surface as a ConcurrentModificationException. That is handled rather than prevented:
    * the build fails, no index is cached, and the next class mediator retries.
    *
    * The serializer registry is copied outside that monitor deliberately. The factory monitor does
    * not guard it, and MediatorSerializerFinder.getInstance() can trigger class initialisation that
    * runs ServiceLoader over extension serializers; holding the factory monitor across that would
    * establish a lock order between the two finders for no benefit.
    *
    * @return the index, or MediatorElementIndex.EMPTY if it could not be built. The empty result is
    *         deliberately not cached, so a transient failure is retried rather than disabling this
    *         check for the lifetime of the JVM.
    */
    private static MediatorElementIndex getElementIndex() {

        int serializerCount;
        int factoryCount;
        try {
            serializerCount = MediatorSerializerFinder.getInstance().getSerializerMap().size();
            factoryCount = MediatorFactoryFinder.getInstance().getFactoryMap().size();
        } catch (Throwable t) {
            logIndexFailure(t);
            return MediatorElementIndex.EMPTY;
        }

        MediatorElementIndex existing = elementIndex;
        if (existing != null && existing.serializerCount == serializerCount
                && existing.factoryCount == factoryCount) {
            return existing;
        }

        MediatorElementIndex built;
        try {
            Map<String, MediatorSerializer> serializers =
                    new HashMap<>(MediatorSerializerFinder.getInstance().getSerializerMap());
            Map<QName, Class> factories;
            synchronized (MediatorFactoryFinder.class) {
                factories = new HashMap<>(MediatorFactoryFinder.getInstance().getFactoryMap());
            }
            built = buildIndex(serializers, factories);
        } catch (Throwable t) {
            // Throwable, not Exception: MediatorSerializerFinder builds its singleton in a static
            // field initialiser, so a failing extension serializer surfaces as
            // ExceptionInInitializerError or NoClassDefFoundError.
            logIndexFailure(t);
            return MediatorElementIndex.EMPTY;
        }

        elementIndex = built;
        return built;
    }

    private static void logIndexFailure(Throwable t) {

        log.error("Could not build the mediator implementation to element index. Class mediator "
                + "access control cannot evaluate the resolved class until it can be built, so "
                + "class mediators are permitted in the meantime; the build is retried on the next "
                + "class mediator.", t);
    }

    /**
    * Joins the two registries Synapse already maintains into an implementation to element index.
    *
    * The serializer registry maps a fully qualified implementation class name - declared by each
    * serializer through getMediatorClassName() - to that serializer, giving the implementation
    * class. The factory registry maps an element QName to its factory class, giving the element
    * name. The two are joined on the stem shared by a mediator's factory and serializer class
    * names, for example TransactionMediatorFactory and TransactionMediatorSerializer both reducing
    * to "TransactionMediator".
    *
    * Taking the class from the serializer rather than inferring it from the factory name matters
    * where a mediator does not follow the naming convention: ThrowErrorMediatorSerializer declares
    * org.apache.synapse.mediators.v2.ThrowError, which no derivation from
    * ThrowErrorMediatorFactory would produce. Keying on the fully qualified name also means a class
    * cannot inherit a decision by sharing a simple name.
    *
    * One element may be served by more than one implementation - foreach is built as either
    * ForEachMediator or ForEachMediatorV2 depending on its attributes - so a serializer stem that
    * matches no factory is resolved against the longest factory stem it is prefixed by. Anything
    * still unresolved is reported, because a silently dropped entry is exactly a class this check
    * would fail to govern.
    *
    * A name that resolves to more than one element is excluded rather than guessed at, on either
    * side of the join. Guessing risks both failing to govern a restricted class and refusing a
    * legitimate one.
    *
    * Package private and free of static state so the join can be tested directly.
    *
    * @param serializers implementation class name to serializer
    * @param factories element QName to factory class
    * @return the index, never null
    */
    static MediatorElementIndex buildIndex(Map<String, MediatorSerializer> serializers,
                                           Map<QName, Class> factories) {

        Map<String, String> stemToClassName = new HashMap<>();
        Set<String> ambiguousStems = new HashSet<>();
        for (Map.Entry<String, MediatorSerializer> entry : serializers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String serializerName = entry.getValue().getClass().getSimpleName();
            String stem = stripSuffix(serializerName, "Serializer");
            if (stem == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Mediator serializer '" + serializerName + "' does not follow the "
                        + "<Mediator>Serializer naming convention, so '" + entry.getKey()
                        + "' cannot be resolved to a mediator element by class name.");
                }
                continue;
            }
            String previous = stemToClassName.put(stem, entry.getKey());
            if (previous != null && !previous.equals(entry.getKey())) {
                ambiguousStems.add(stem);
                log.warn("Mediator serializers for '" + previous + "' and '" + entry.getKey()
                        + "' share the simple name '" + serializerName + "'. Neither class will be "
                        + "resolved to a mediator element by class name; a Synapse mediator may "
                        + "still resolve through the factory-derived name.");
            }
        }

        Map<String, String> byClassName = new HashMap<>();
        Map<String, String> bySimpleName = new HashMap<>();
        Set<String> ambiguousClasses = new HashSet<>();
        Set<String> ambiguousSimpleNames = new HashSet<>();
        Set<String> consumedStems = new HashSet<>();

        for (Map.Entry<QName, Class> entry : factories.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String factoryName = entry.getValue().getSimpleName();
            String stem = stripSuffix(factoryName, "Factory");
            if (stem == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Mediator factory '" + factoryName + "' does not follow the "
                        + "<Mediator>Factory naming convention, so element '"
                        + entry.getKey().getLocalPart() + "' has no implementation mapping.");
                }
                continue;
            }
            String elementName = entry.getKey().getLocalPart();
            String className = ambiguousStems.contains(stem) ? null : stemToClassName.get(stem);
            if (className != null) {
                String previous = byClassName.put(className, elementName);
                if (previous != null && !previous.equals(elementName)) {
                    // Two factories share a simple name under different elements, so the class they
                    // both reduce to would bind to whichever was written last.
                    ambiguousClasses.add(className);
                    log.warn("Mediator implementation '" + className + "' is claimed by more than "
                            + "one mediator element ('" + previous + "' and '" + elementName
                            + "'). It will not be resolved to an element by class mediator access "
                            + "control.");
                }
                consumedStems.add(stem);
            } else {
                String key = stem.toLowerCase(Locale.ROOT);
                String previous = bySimpleName.put(key, elementName);
                if (previous != null && !previous.equals(elementName)) {
                    ambiguousSimpleNames.add(key);
                    log.warn("Mediator name '" + stem + "' is claimed by more than one mediator "
                            + "element ('" + previous + "' and '" + elementName + "'). It will not "
                            + "be resolved to an element by class mediator access control.");
                }
            }
        }

        byClassName.keySet().removeAll(ambiguousClasses);
        bySimpleName.keySet().removeAll(ambiguousSimpleNames);

        // Additional implementations of an element already resolved above: match the leftover
        // serializer stem to the longest factory stem it extends.
        for (Map.Entry<String, String> leftover : stemToClassName.entrySet()) {
            String stem = leftover.getKey();
            String className = leftover.getValue();
            if (consumedStems.contains(stem) || ambiguousStems.contains(stem)
                    || byClassName.containsKey(className)) {
                continue;
            }
            // Longest first, falling through when a candidate's class was excluded as ambiguous.
            List<String> candidates = new ArrayList<>(consumedStems);
            Collections.sort(candidates, new Comparator<String>() {
                public int compare(String a, String b) { return b.length() - a.length(); }
            });
            String elementName = null;
            String matchedStem = null;
            for (String candidate : candidates) {
                if (!stem.startsWith(candidate)) {
                    continue;
                }
                String candidateElement = byClassName.get(stemToClassName.get(candidate));
                if (candidateElement != null) {
                    elementName = candidateElement;
                    matchedStem = candidate;
                    break;
                }
            }
            if (elementName != null) {
                byClassName.put(className, elementName);
                if (log.isDebugEnabled()) {
                    log.debug("Mediator implementation '" + className + "' resolved to element '"
                            + elementName + "' because its serializer name extends '" + matchedStem
                            + "Serializer'.");
                }
            } else {
                log.warn("Mediator implementation '" + className + "' could not be resolved to a "
                        + "mediator element: its serializer name '" + stem + "Serializer' matches "
                        + "no registered mediator factory, and no factory it extends resolves to an "
                        + "unambiguous element. Class mediator access control will not govern this "
                        + "class.");
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Mediator element index built. Resolved by class name: " + byClassName.size()
                    + ", resolved by factory-derived name: " + bySimpleName.size()
                    + " " + bySimpleName.keySet());
        }

        return new MediatorElementIndex(Collections.unmodifiableMap(byClassName),
                Collections.unmodifiableMap(bySimpleName), serializers.size(), factories.size());
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
