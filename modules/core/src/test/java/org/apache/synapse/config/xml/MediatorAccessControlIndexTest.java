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

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * Covers the join that resolves a mediator implementation class to the XML element it is registered
 * under. A regression here silently stops class mediator access control from governing a class, so
 * the join is exercised directly rather than through a deployed configuration.
 */
public class MediatorAccessControlIndexTest extends TestCase {

    // --- stand-ins whose simple names drive the join -------------------------------------------

    private abstract static class StubSerializer implements MediatorSerializer {
        private final String className;
        StubSerializer(String className) { this.className = className; }
        public OMElement serializeMediator(OMElement parent, Mediator m) { return null; }
        public String getMediatorClassName() { return className; }
    }

    private static class AlphaMediatorSerializer extends StubSerializer {
        AlphaMediatorSerializer(String c) { super(c); }
    }
    private static class AlphaMediatorV2Serializer extends StubSerializer {
        AlphaMediatorV2Serializer(String c) { super(c); }
    }
    private static class BetaMediatorSerializer extends StubSerializer {
        BetaMediatorSerializer(String c) { super(c); }
    }
    private static class NotFollowingConvention extends StubSerializer {
        NotFollowingConvention(String c) { super(c); }
    }

    private static class AlphaMediatorFactory { }
    /** Simple name collides with a Synapse mediator, but the package does not. */
    private static class GammaMediator { }
    private static class Alpha { }
    private static class BetaMediatorFactory { }
    private static class GammaMediatorFactory { }

    private static QName q(String local) { return new QName("http://ws.apache.org/ns/synapse", local); }

    private static Map<String, MediatorSerializer> serializers(MediatorSerializer... entries) {
        Map<String, MediatorSerializer> m = new HashMap<String, MediatorSerializer>();
        for (MediatorSerializer s : entries) {
            m.put(s.getMediatorClassName(), s);
        }
        return m;
    }

    // --- tests ---------------------------------------------------------------------------------

    /** The ordinary case: one factory, one serializer, joined on the shared stem. */
    public void testResolvesImplementationToItsElement() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("alpha"), AlphaMediatorFactory.class);

        MediatorAccessControl.MediatorElementIndex index = MediatorAccessControl.buildIndex(
                serializers(new AlphaMediatorSerializer("com.example.AlphaMediator")), factories);

        assertEquals("alpha", index.byClassName.get("com.example.AlphaMediator"));
    }

    /**
     * An element served by more than one implementation. ForEachMediatorV2Serializer matches no
     * factory of its own, so without the prefix sweep its class is dropped and left ungoverned.
     */
    public void testAdditionalImplementationOfTheSameElementIsResolved() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("alpha"), AlphaMediatorFactory.class);

        MediatorAccessControl.MediatorElementIndex index = MediatorAccessControl.buildIndex(
                serializers(new AlphaMediatorSerializer("com.example.AlphaMediator"),
                            new AlphaMediatorV2Serializer("com.example.v2.AlphaMediatorV2")),
                factories);

        assertEquals("alpha", index.byClassName.get("com.example.AlphaMediator"));
        assertEquals("the additional implementation must inherit the same element",
                "alpha", index.byClassName.get("com.example.v2.AlphaMediatorV2"));
    }

    /**
     * Two factories sharing a simple name under different elements. The class they both reduce to
     * must be excluded rather than bound to whichever was written last, which would let a
     * restricted implementation inherit a permitted element.
     */
    public void testClassClaimedByTwoElementsIsExcluded() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("alpha"), AlphaMediatorFactory.class);
        factories.put(q("shadow"), org.apache.synapse.config.xml.other.AlphaMediatorFactory.class);

        MediatorAccessControl.MediatorElementIndex index = MediatorAccessControl.buildIndex(
                serializers(new AlphaMediatorSerializer("com.example.AlphaMediator")), factories);

        assertFalse("an ambiguous class must not resolve to any element",
                index.byClassName.containsKey("com.example.AlphaMediator"));
    }

    /**
     * Two serializers sharing a simple name: neither class may be resolved by class name. A
     * Synapse mediator of that name can still resolve through the factory-derived fallback, which
     * is what the warning now says and what this asserts.
     */
    public void testAmbiguousSerializerStemResolvesNothingByClassName() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("alpha"), AlphaMediatorFactory.class);

        Map<String, MediatorSerializer> sers = serializers(
                new AlphaMediatorSerializer("com.example.AlphaMediator"));
        sers.put("com.other.AlphaMediator", new AlphaMediatorSerializer("com.other.AlphaMediator"));

        MediatorAccessControl.MediatorElementIndex index =
                MediatorAccessControl.buildIndex(sers, factories);

        assertFalse(index.byClassName.containsKey("com.example.AlphaMediator"));
        assertFalse(index.byClassName.containsKey("com.other.AlphaMediator"));
        assertEquals("the factory falls through to the derived-name map",
                "alpha", index.bySimpleName.get("alphamediator"));
    }

    /** A factory with no serializer falls back to the name derived from the factory. */
    public void testFactoryWithoutSerializerFallsBackToDerivedName() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("gamma"), GammaMediatorFactory.class);

        MediatorAccessControl.MediatorElementIndex index =
                MediatorAccessControl.buildIndex(serializers(), factories);

        assertEquals("gamma", index.bySimpleName.get("gammamediator"));
    }

    /** A serializer not following the naming convention must not derail the rest of the join. */
    public void testUnconventionalSerializerNameIsSkippedNotFatal() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("beta"), BetaMediatorFactory.class);

        MediatorAccessControl.MediatorElementIndex index = MediatorAccessControl.buildIndex(
                serializers(new NotFollowingConvention("com.example.Odd"),
                            new BetaMediatorSerializer("com.example.BetaMediator")), factories);

        assertEquals("beta", index.byClassName.get("com.example.BetaMediator"));
        assertFalse(index.byClassName.containsKey("com.example.Odd"));
    }

    /** The index records the registry sizes it was built from, so later registration is detected. */
    public void testIndexRecordsRegistrySizes() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("alpha"), AlphaMediatorFactory.class);
        factories.put(q("beta"), BetaMediatorFactory.class);

        MediatorAccessControl.MediatorElementIndex index = MediatorAccessControl.buildIndex(
                serializers(new AlphaMediatorSerializer("com.example.AlphaMediator")), factories);

        assertEquals(1, index.serializerCount);
        assertEquals(2, index.factoryCount);
    }

    /**
     * The other half of the ambiguity fix: two factories with no serializer sharing a simple name.
     * This is the branch that governs the AnnotatedCommandMediator fallback.
     */
    public void testDerivedNameClaimedByTwoElementsIsExcluded() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("gamma"), GammaMediatorFactory.class);
        factories.put(q("shadow"), org.apache.synapse.config.xml.other.GammaMediatorFactory.class);

        MediatorAccessControl.MediatorElementIndex index =
                MediatorAccessControl.buildIndex(serializers(), factories);

        assertFalse("an ambiguous derived name must not resolve to any element",
                index.bySimpleName.containsKey("gammamediator"));
    }

    /** An exact class-name match resolves regardless of package. */
    public void testResolveElementMatchesExactClassName() {

        Map<QName, Class> factories = new HashMap<QName, Class>();
        factories.put(q("alpha"), AlphaMediatorFactory.class);
        MediatorAccessControl.MediatorElementIndex index = MediatorAccessControl.buildIndex(
                serializers(new AlphaMediatorSerializer(Alpha.class.getName())), factories);

        assertEquals("alpha", MediatorAccessControl.resolveElement(index, Alpha.class));
    }

    /**
     * The package gate. A class outside org.apache.synapse.mediators must not inherit a decision
     * through the derived-name fallback merely by sharing a simple name with a Synapse mediator.
     */
    public void testResolveElementAppliesThePackageGateToTheFallback() {

        Map<String, String> bySimpleName = new HashMap<String, String>();
        bySimpleName.put("gammamediator", "gamma");
        MediatorAccessControl.MediatorElementIndex index = new MediatorAccessControl
                .MediatorElementIndex(new HashMap<String, String>(), bySimpleName, 0, 0);

        assertNull("a customer class must not match by simple name",
                MediatorAccessControl.resolveElement(index, GammaMediator.class));
    }

    /** A class inside the Synapse mediator package does resolve through the fallback. */
    public void testResolveElementAllowsTheFallbackInsideTheSynapsePackage() {

        Map<String, String> bySimpleName = new HashMap<String, String>();
        bySimpleName.put("stubmediator", "stub");
        MediatorAccessControl.MediatorElementIndex index = new MediatorAccessControl
                .MediatorElementIndex(new HashMap<String, String>(), bySimpleName, 0, 0);

        assertEquals("stub", MediatorAccessControl.resolveElement(
                index, org.apache.synapse.mediators.StubMediator.class));
    }
}
