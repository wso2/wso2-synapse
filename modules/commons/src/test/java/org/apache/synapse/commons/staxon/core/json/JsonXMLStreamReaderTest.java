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
package org.apache.synapse.commons.staxon.core.json;

import java.io.StringReader;
import java.math.BigDecimal;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.Assert;
import org.junit.Test;

public class JsonXMLStreamReaderTest {
    void verify(XMLStreamReader reader, int expectedEventType, String expectedLocalName, String expectedText) {
        Assert.assertEquals(expectedEventType, reader.getEventType());
        Assert.assertEquals(expectedLocalName, reader.getLocalName());
        Assert.assertEquals(expectedText, reader.getText());
    }

    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testTextContent() throws Exception {
        String input = "{\"alice\":\"bob\"}";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;david&gt;edgar&lt;/david&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testNested() throws Exception {
        String input = "{\"alice\":{\"bob\":\"charlie\",\"david\":\"edgar\"}}";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "bob", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "charlie");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "bob", null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "david", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "edgar");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "david", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;bob&gt;david&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testArray() throws Exception {
        String input = "{\"alice\":{\"bob\":[\"charlie\",\"david\"]}}";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.PROCESSING_INSTRUCTION, null, null);
        Assert.assertEquals(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, reader.getPITarget());
        Assert.assertEquals("bob", reader.getPIData());
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "bob", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "charlie");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "bob", null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "bob", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "david");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "bob", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice charlie="david"&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testAttributes() throws Exception {
        String input = "{\"alice\":{\"@charlie\":\"david\",\"$\":\"bob\"}}";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        Assert.assertEquals(1, reader.getAttributeCount());
        Assert.assertEquals("david", reader.getAttributeValue(null, "charlie"));
        Assert.assertEquals("david", reader.getAttributeValue(XMLConstants.NULL_NS_URI, "charlie"));
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice xmlns="http://some-namespace"&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testNamespaces() throws Exception {
        String input = "{\"alice\":{\"@xmlns\":\"http://some-namespace\",\"$\":\"bob\"}}";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        Assert.assertEquals("http://some-namespace", reader.getNamespaceURI());
        Assert.assertEquals(0, reader.getAttributeCount());
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        Assert.assertEquals("http://some-namespace", reader.getNamespaceURI());
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice xmlns="http://foo" xmlns:bar="http://bar"&gt;bob&lt;/alice&gt;</code>
     * with badgerfish notation
     */
    @Test
    public void testNamespacesBadgerfish() throws Exception {
        String input = "{\"alice\":{\"@xmlns\":{\"$\":\"http://foo\",\"bar\":\"http://bar\"},\"$\":\"bob\"}}";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        Assert.assertEquals("http://foo", reader.getNamespaceURI());
        Assert.assertEquals("http://bar", reader.getNamespaceURI("bar"));
        Assert.assertEquals(0, reader.getAttributeCount());
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        Assert.assertEquals("http://foo", reader.getNamespaceURI());
        Assert.assertEquals("http://bar", reader.getNamespaceURI("bar"));
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testRootArray() throws Exception {
        String input = "{\"alice\":[\"bob\",\"bob\"]}";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.PROCESSING_INSTRUCTION, null, null);
        Assert.assertEquals(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, reader.getPITarget());
        Assert.assertEquals("alice", reader.getPIData());
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testRootArrayWithVirtualRoot() throws Exception {
        String input = "[\"bob\",\"bob\"]";
        JsonXMLInputFactory factory = new JsonXMLInputFactory();
        factory.setProperty(JsonXMLInputFactory.PROP_VIRTUAL_ROOT, new QName("alice"));
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.PROCESSING_INSTRUCTION, null, null);
        Assert.assertEquals(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, reader.getPITarget());
        Assert.assertEquals("alice", reader.getPIData());
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }


    @Test
    public void testSimpleValueArray() throws Exception {
        String input = "[\"edgar\",\"david\"]";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.PROCESSING_INSTRUCTION, null, null);
        Assert.assertEquals(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, reader.getPITarget());
        Assert.assertNull(reader.getPIData());
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "edgar");
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "david");
        Assert.assertFalse(reader.hasNext());
        reader.close();
    }

    @Test
    public void testDocumentArray() throws Exception {
        String input = "[{\"alice\":\"bob\"},{\"alice\":\"bob\"}]";
        XMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.PROCESSING_INSTRUCTION, null, null);
        Assert.assertEquals(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, reader.getPITarget());
        Assert.assertNull(reader.getPIData());
        reader.next();
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "bob");
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        Assert.assertFalse(reader.hasNext());
        reader.close();
    }

    /**
     * <code>&lt;alice&gt;123.40&lt;/alice&gt;</code>
     */
    @Test
    public void testNumber() throws Exception {
        String input = "{\"alice\" : 123.40}";
        JsonXMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "123.40");
        Assert.assertTrue(reader.hasNumber());
        Assert.assertFalse(reader.hasBoolean());
        Assert.assertEquals(new BigDecimal("123.40"), reader.getNumber());
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice&gt;false&lt;/alice&gt;</code>
     */
    @Test
    public void testBoolean() throws Exception {
        String input = "{\"alice\" : false}";
        JsonXMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.CHARACTERS, null, "false");
        Assert.assertFalse(reader.hasNumber());
        Assert.assertTrue(reader.hasBoolean());
        Assert.assertFalse(reader.getBoolean());
        reader.next();
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }

    /**
     * <code>&lt;alice/&gt;</code>
     */
    @Test
    public void testNull() throws Exception {
        String input = "{\"alice\" : null}";
        JsonXMLStreamReader reader = new JsonXMLInputFactory().createXMLStreamReader(new StringReader(input));
        verify(reader, XMLStreamConstants.START_DOCUMENT, null, null);
        reader.next();
        verify(reader, XMLStreamConstants.START_ELEMENT, "alice", null);
        reader.next();
//		verify(reader, XMLStreamConstants.CHARACTERS, null, null); // null is not reported
        verify(reader, XMLStreamConstants.END_ELEMENT, "alice", null);
        reader.next();
        verify(reader, XMLStreamConstants.END_DOCUMENT, null, null);
        reader.close();
    }
}
