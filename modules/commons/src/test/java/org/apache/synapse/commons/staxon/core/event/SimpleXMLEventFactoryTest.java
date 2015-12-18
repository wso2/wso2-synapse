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
package org.apache.synapse.commons.staxon.core.event;

import java.io.StringWriter;
import java.util.Arrays;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.junit.Assert;
import org.junit.Test;

public class SimpleXMLEventFactoryTest {
    static final Location LOCATION = new Location() {
        @Override
        public String getSystemId() {
            return null;
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public int getLineNumber() {
            return 0;
        }

        @Override
        public int getColumnNumber() {
            return 0;
        }

        @Override
        public int getCharacterOffset() {
            return 0;
        }
    };

    SimpleXMLEventFactory factory = new SimpleXMLEventFactory(LOCATION);

    private void verify(XMLEvent event, int expectedEventType, String expectedWriteAsEncodedUnicode) throws XMLStreamException {
        Assert.assertEquals(expectedEventType, event.getEventType());
        Assert.assertNull(event.getSchemaType());
        Assert.assertSame(LOCATION, event.getLocation());
        StringWriter stringWriter = new StringWriter();
        event.writeAsEncodedUnicode(stringWriter);
        Assert.assertEquals(expectedWriteAsEncodedUnicode, stringWriter.toString());
    }

    @Test
    public void testCreateAttribute_Prefix_NamespaceURI_LocalName_Value() throws XMLStreamException {
        Attribute event = factory.createAttribute("foo", "http://foo", "bar", "foobar");
        verify(event, XMLStreamConstants.ATTRIBUTE, "foo:bar=\"foobar\"");
    }

    @Test
    public void testCreateAttribute_LocalName_Value() throws XMLStreamException {
        Attribute event = factory.createAttribute("foo", "bar");
        verify(event, XMLStreamConstants.ATTRIBUTE, "foo=\"bar\"");
    }

    @Test
    public void testCreateAttribute_Name_Value() throws XMLStreamException {
        Attribute event = factory.createAttribute(new QName("http://foo", "bar", "foo"), "foobar");
        verify(event, XMLStreamConstants.ATTRIBUTE, "foo:bar=\"foobar\"");
    }

    @Test
    public void testCreateNamespace_NamespaceURI() throws XMLStreamException {
        Namespace event = factory.createNamespace("http://foo");
        verify(event, XMLStreamConstants.NAMESPACE, "xmlns=\"http://foo\"");
    }

    @Test
    public void testCreateNamespace_Prefix_NamespaceURI() throws XMLStreamException {
        Namespace event = factory.createNamespace("foo", "http://foo");
        verify(event, XMLStreamConstants.NAMESPACE, "xmlns:foo=\"http://foo\"");
    }

    @Test
    public void testCreateStartElement_Name_Attributes_Namespaces() throws XMLStreamException {
        Namespace namespace = factory.createNamespace("foo", "http://foo");
        Attribute attribute = factory.createAttribute("foo", "bar");
        StartElement event = factory.createStartElement(new QName("http://foo", "bar", "foo"), Arrays.asList(attribute).iterator(), Arrays.asList(namespace).iterator());
        verify(event, XMLStreamConstants.START_ELEMENT, "<foo:bar xmlns:foo=\"http://foo\" foo=\"bar\">");
    }

    @Test
    public void testCreateStartElement_Prefix_NamespaceURI_LocalName() throws XMLStreamException {
        StartElement event = factory.createStartElement("foo", "http://foo", "bar");
        verify(event, XMLStreamConstants.START_ELEMENT, "<foo:bar>");
    }

    @Test
    public void testCreateStartElement_Prefix_NamespaceURI_LocalName_Attributes_Namespaces() throws XMLStreamException {
        Namespace namespace = factory.createNamespace("foo", "http://foo");
        Attribute attribute = factory.createAttribute("foo", "bar");
        StartElement event = factory.createStartElement("foo", "http://foo", "bar", Arrays.asList(attribute).iterator(), Arrays.asList(namespace).iterator());
        verify(event, XMLStreamConstants.START_ELEMENT, "<foo:bar xmlns:foo=\"http://foo\" foo=\"bar\">");
    }

    @Test
    public void testCreateStartElement_Prefix_NamespaceURI_LocalName_Attributes_Namespaces_Context() throws XMLStreamException {
        Attribute attribute = factory.createAttribute("foo", "bar");
        StartElement event = factory.createStartElement("foo", "http://foo", "bar", Arrays.asList(attribute).iterator(), null);
        verify(event, XMLStreamConstants.START_ELEMENT, "<foo:bar foo=\"bar\">");
    }

    @Test
    public void testCreateEndElement_Name_Namespaces() throws XMLStreamException {
        Namespace namespace = factory.createNamespace("foo", "http://foo");
        EndElement event = factory.createEndElement(new QName("http://foo", "bar", "foo"), Arrays.asList(namespace).iterator());
        verify(event, XMLStreamConstants.END_ELEMENT, "</foo:bar>");
        Assert.assertEquals(namespace, event.getNamespaces().next());
    }

    @Test
    public void testCreateEndElement_Prefix_NamespaceURI_LocalName() throws XMLStreamException {
        EndElement event = factory.createEndElement("foo", "http://foo", "bar");
        verify(event, XMLStreamConstants.END_ELEMENT, "</foo:bar>");
    }

    @Test
    public void testCreateEndElement_Prefix_NamespaceURI_LocalName_Namespaces() throws XMLStreamException {
        Namespace namespace = factory.createNamespace("foo", "http://foo");
        EndElement event = factory.createEndElement("foo", "http://foo", "bar", Arrays.asList(namespace).iterator());
        verify(event, XMLStreamConstants.END_ELEMENT, "</foo:bar>");
        Assert.assertEquals(namespace, event.getNamespaces().next());
    }

    @Test
    public void testCreateCharacters() throws XMLStreamException {
        Characters event = factory.createCharacters("foobar");
        verify(event, XMLStreamConstants.CHARACTERS, "foobar");
    }

    @Test
    public void testCreateCData() throws XMLStreamException {
        Characters event = factory.createCData("foobar");
        verify(event, XMLStreamConstants.CDATA, "<![CDATA[foobar]]>");
    }

    @Test
    public void testCreateSpace() throws XMLStreamException {
        Characters event = factory.createSpace(" ");
        verify(event, XMLStreamConstants.CHARACTERS, " ");
        Assert.assertTrue(event.isWhiteSpace());
        Assert.assertFalse(event.isIgnorableWhiteSpace());
    }

    @Test
    public void testCreateIgnorableSpace() throws XMLStreamException {
        Characters event = factory.createIgnorableSpace(" ");
        verify(event, XMLStreamConstants.SPACE, "");
        Assert.assertTrue(event.isWhiteSpace());
        Assert.assertTrue(event.isIgnorableWhiteSpace());
    }

    @Test
    public void testCreateStartDocument() throws XMLStreamException {
        StartDocument event = factory.createStartDocument();
        verify(event, XMLStreamConstants.START_DOCUMENT, "<?xml version=\"1.0\"?>");
    }

    @Test
    public void testCreateStartDocument_Encoding_Version_Standalone() throws XMLStreamException {
        StartDocument event = factory.createStartDocument("UTF-8", "1.1", true);
        verify(event, XMLStreamConstants.START_DOCUMENT, "<?xml version=\"1.1\" encoding=\"UTF-8\" standalone=\"yes\"?>");
    }

    @Test
    public void testCreateStartDocument_Encoding_Version() throws XMLStreamException {
        StartDocument event = factory.createStartDocument("UTF-8", "1.1");
        verify(event, XMLStreamConstants.START_DOCUMENT, "<?xml version=\"1.1\" encoding=\"UTF-8\"?>");
    }

    @Test
    public void testCreateStartDocument_Encoding() throws XMLStreamException {
        StartDocument event = factory.createStartDocument("UTF-8");
        verify(event, XMLStreamConstants.START_DOCUMENT, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    @Test
    public void testCreateEndDocument() throws XMLStreamException {
        EndDocument event = factory.createEndDocument();
        verify(event, XMLStreamConstants.END_DOCUMENT, "");
    }

    @Test
    public void testCreateEntityReference() throws XMLStreamException {
        EntityReference event = factory.createEntityReference("foo", null);
        verify(event, XMLStreamConstants.ENTITY_REFERENCE, "&foo;");
    }

    @Test
    public void testCreateComment() throws XMLStreamException {
        Comment event = factory.createComment("foo");
        verify(event, XMLStreamConstants.COMMENT, "<!--foo-->");
    }

    @Test
    public void testCreateProcessingInstruction() throws XMLStreamException {
        ProcessingInstruction event = factory.createProcessingInstruction("foo", "bar");
        verify(event, XMLStreamConstants.PROCESSING_INSTRUCTION, "<?foo bar?>");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateDTD() {
        try {
            factory.createDTD("foo");
        } catch (UnsupportedOperationException ex) {
            //empty
        }
    }
}
