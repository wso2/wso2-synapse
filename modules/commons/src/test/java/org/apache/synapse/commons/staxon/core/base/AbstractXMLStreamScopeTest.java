/*
 * Copyright 2011, 2012 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.commons.staxon.core.base;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Test;

public class AbstractXMLStreamScopeTest {
    static class TestScope extends AbstractXMLStreamScope {
        public TestScope(String defaultNamespace) {
            super(defaultNamespace);
        }

        public TestScope(NamespaceContext parent) {
            super(parent);
        }

        public TestScope(AbstractXMLStreamScope parent, String prefix, String localName, String namespaceURI) {
            super(parent, prefix, localName, namespaceURI);
        }
    }

    @Test
    public void testRootScope_NULL_NS_URI() {
        TestScope scope = new TestScope(XMLConstants.NULL_NS_URI);
        Assert.assertTrue(scope.isRoot());
        Assert.assertNull(scope.getPrefix());
        Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getNamespaceURI());
        Assert.assertNull(scope.getLocalName());
        Assert.assertNull(scope.getLastChild());
        Assert.assertNull(scope.getParent());
        Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getNamespaceURI("foo"));
        Assert.assertNull(scope.getPrefix(XMLConstants.NULL_NS_URI));
        Assert.assertNull(scope.getNonEmptyPrefix(XMLConstants.NULL_NS_URI));
        Assert.assertNull(scope.getPrefix("http://foo"));
        Assert.assertTrue(scope.isStartTagClosed());
        Assert.assertNull(scope.getAttributes());
    }

    @Test
    public void testRootScopeWithDefaultNamespace() {
        TestScope scope = new TestScope("http://foo");
        Assert.assertTrue(scope.isRoot());
        Assert.assertNull(scope.getPrefix());
        Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getNamespaceURI());
        Assert.assertNull(scope.getLocalName());
        Assert.assertNull(scope.getLastChild());
        Assert.assertNull(scope.getParent());
        Assert.assertEquals("http://foo", scope.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getNamespaceURI("foo"));
        Assert.assertNull(scope.getPrefix(XMLConstants.NULL_NS_URI));
        Assert.assertNull(scope.getNonEmptyPrefix(XMLConstants.NULL_NS_URI));
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, scope.getPrefix("http://foo"));
        Assert.assertTrue(scope.isStartTagClosed());
        Assert.assertNull(scope.getAttributes());

        Iterator<?> prefixes = scope.getPrefixes("http://foo");
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, prefixes.next());
        Assert.assertFalse(prefixes.hasNext());

        scope.setPrefix("foo", "http://foo");
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, scope.getPrefix("http://foo"));
        Assert.assertEquals("http://foo", scope.getNamespaceURI("foo"));

        prefixes = scope.getPrefixes("http://foo");
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, prefixes.next());
        Assert.assertEquals("foo", prefixes.next());
        Assert.assertFalse(prefixes.hasNext());

        scope.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, "http://bar");
        Assert.assertEquals("http://bar", scope.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        Assert.assertEquals("foo", scope.getPrefix("http://foo"));
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, scope.getPrefix("http://bar"));

        prefixes = scope.getPrefixes("http://foo");
        Assert.assertEquals("foo", prefixes.next());
        Assert.assertFalse(prefixes.hasNext());

        prefixes = scope.getPrefixes("http://bar");
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, prefixes.next());
        Assert.assertFalse(prefixes.hasNext());
    }

    @Test
    public void testElementScopeWithPrefix() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), "bar", "test", null);
        Assert.assertFalse(scope.isRoot());
        Assert.assertEquals("bar", scope.getPrefix());
        Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getNamespaceURI());
        Assert.assertEquals("test", scope.getLocalName());

        Assert.assertNull(scope.getLastChild());
        Assert.assertNotNull(scope.getParent());
        Assert.assertSame(scope, scope.getParent().getLastChild());
        Assert.assertEquals("http://foo", scope.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getNamespaceURI("foo"));
        Assert.assertNull(scope.getPrefix(XMLConstants.NULL_NS_URI));
        Assert.assertNull(scope.getNonEmptyPrefix(XMLConstants.NULL_NS_URI));
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, scope.getPrefix("http://foo"));
        Assert.assertFalse(scope.isStartTagClosed());
        Assert.assertNull(scope.getAttributes());

        scope.setPrefix("bar", "http://bar");
        Assert.assertEquals("bar", scope.getPrefix("http://bar"));
        Assert.assertEquals("http://bar", scope.getNamespaceURI());
        Assert.assertEquals("http://bar", scope.getNamespaceURI("bar"));

        scope.setStartTagClosed(true);
        Assert.assertTrue(scope.isStartTagClosed());

        scope = new TestScope(scope, "bar", "test", null);
        scope.setPrefix("bar", "http://bar2");
        Assert.assertEquals("http://bar2", scope.getNamespaceURI());
        Assert.assertEquals("http://bar", scope.getParent().getNamespaceURI());

        scope.setPrefix("bar2", "http://bar");
        Iterator<?> prefixes = scope.getPrefixes("http://bar");
        Assert.assertEquals("bar2", prefixes.next());
        Assert.assertFalse(prefixes.hasNext()); // "bar" has been re-bound to "http://bar2"
    }

    @Test
    public void testElementScopeWithNamespaceURI() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), null, "test", "http://bar");
        Assert.assertFalse(scope.isRoot());
        Assert.assertEquals("test", scope.getLocalName());
        Assert.assertNull(scope.getPrefix());
        Assert.assertEquals("http://bar", scope.getNamespaceURI());

        Assert.assertNull(scope.getLastChild());
        Assert.assertNotNull(scope.getParent());
        Assert.assertSame(scope, scope.getParent().getLastChild());
        Assert.assertEquals("http://foo", scope.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getNamespaceURI("foo"));
        Assert.assertNull(scope.getPrefix(XMLConstants.NULL_NS_URI));
        Assert.assertNull(scope.getNonEmptyPrefix(XMLConstants.NULL_NS_URI));
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, scope.getPrefix("http://foo"));
        Assert.assertFalse(scope.isStartTagClosed());
        Assert.assertNull(scope.getAttributes());

        scope.setPrefix("bar", "http://bar");
        Assert.assertEquals("bar", scope.getPrefix());
        Assert.assertEquals("bar", scope.getPrefix("http://bar"));
        Assert.assertEquals("http://bar", scope.getNamespaceURI());
        Assert.assertEquals("http://bar", scope.getNamespaceURI("bar"));

        scope.setStartTagClosed(true);
        Assert.assertTrue(scope.isStartTagClosed());

        scope = new TestScope(scope, "bar", "test", null);
        scope.setPrefix("bar", "http://bar2");
        Assert.assertEquals("http://bar2", scope.getNamespaceURI());
        Assert.assertEquals("http://bar", scope.getParent().getNamespaceURI());

        scope.setPrefix("bar2", "http://bar");
        Iterator<?> prefixes = scope.getPrefixes("http://bar");
        Assert.assertEquals("bar2", prefixes.next());
        Assert.assertFalse(prefixes.hasNext()); // "bar" has been re-bound to "http://bar2"
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeUnboundPrefix() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), "bar", "test", null);
        scope.setStartTagClosed(true);
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeNamespaceNotBound() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), null, "test", "http://bar");
        scope.setStartTagClosed(true);
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeNamespacePrefixMismatch() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), "foobar", "test", "http://bar");
        scope.setPrefix("foobar", "http://foo");
        scope.setStartTagClosed(true);
    }

    @Test
    public void testElementScopeWithAttributes() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), XMLConstants.DEFAULT_NS_PREFIX, "test", null);

        scope.addAttribute(null, "attr1", XMLConstants.NULL_NS_URI, "value1");
        scope.addAttribute(XMLConstants.DEFAULT_NS_PREFIX, "attr2", null, "value2");
        scope.addAttribute(XMLConstants.DEFAULT_NS_PREFIX, "attr3", XMLConstants.NULL_NS_URI, "value3");
        scope.addAttribute(null, "attr4", "http://foo", "value4");
        scope.addAttribute("foo", "attr5", null, "value5");
        scope.addAttribute("foo", "attr6", "http://foo", "value6");

        scope.setPrefix("foo", "http://foo");
        scope.setStartTagClosed(true);

        Assert.assertEquals(scope.getAttributes().size(), 6);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals("attr" + (i + 1), scope.getAttributes().get(i).getLocalName());
            Assert.assertEquals("value" + (i + 1), scope.getAttributes().get(i).getValue());
            Assert.assertEquals(XMLConstants.NULL_NS_URI, scope.getAttributes().get(i).getNamespaceURI());
            Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, scope.getAttributes().get(i).getPrefix());
        }
        for (int i = 3; i < 6; i++) {
            Assert.assertEquals("attr" + (i + 1), scope.getAttributes().get(i).getLocalName());
            Assert.assertEquals("value" + (i + 1), scope.getAttributes().get(i).getValue());
            Assert.assertEquals("http://foo", scope.getAttributes().get(i).getNamespaceURI());
            Assert.assertEquals("foo", scope.getAttributes().get(i).getPrefix());
        }
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeAttributeInvalid1() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), XMLConstants.DEFAULT_NS_PREFIX, "test", null);
        scope.addAttribute(null, "attr1", "http://foo", "value1");
        scope.setStartTagClosed(true);
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeAttributeInvalid2() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), XMLConstants.DEFAULT_NS_PREFIX, "test", null);
        scope.addAttribute(XMLConstants.DEFAULT_NS_PREFIX, "attr1", "http://foo", "value1");
        scope.setStartTagClosed(true);
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeAttributeInvalid3() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), XMLConstants.DEFAULT_NS_PREFIX, "test", null);
        scope.addAttribute("bar", "attr1", null, "value1");
        scope.setStartTagClosed(true);
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeAttributeInvalid4() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), XMLConstants.DEFAULT_NS_PREFIX, "test", null);
        scope.addAttribute("bar", "attr1", "http://foo", "value1");
        scope.setStartTagClosed(true);
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeAttributeInvalid5() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), XMLConstants.DEFAULT_NS_PREFIX, "test", null);
        scope.setPrefix("foo", "http://foo");
        scope.addAttribute("bar", "attr1", "http://foo", "value1");
        scope.setStartTagClosed(true);
    }

    @Test(expected = XMLStreamException.class)
    public void testElementScopeAttributeInvalid6() throws XMLStreamException {
        TestScope scope = new TestScope(new TestScope("http://foo"), XMLConstants.DEFAULT_NS_PREFIX, "test", null);
        scope.setPrefix("bar", "http://bar");
        scope.addAttribute("bar", "attr1", "http://foo", "value1");
        scope.setStartTagClosed(true);
    }
}
