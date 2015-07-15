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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.junit.Assert;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.xml.SimpleXMLInputFactory;

public class SimpleXMLEventAllocatorTest {
    private SimpleXMLEventAllocator eventAllocator = new SimpleXMLEventAllocator();

    private XMLStreamReader createXmlStreamReader(String xml) throws XMLStreamException {
        return new SimpleXMLInputFactory().createXMLStreamReader(new StringReader(xml));
    }

    private void verify(XMLEvent event, int expectedEventType, String expectedWriteAsEncodedUnicode) throws XMLStreamException {
        Assert.assertEquals(expectedEventType, event.getEventType());
        Assert.assertNull(event.getSchemaType());
        Assert.assertNotNull(event.getLocation());
        StringWriter stringWriter = new StringWriter();
        event.writeAsEncodedUnicode(stringWriter);
        Assert.assertEquals(expectedWriteAsEncodedUnicode, stringWriter.toString());
    }

    @Test
    public void testStartEndDocument() throws XMLStreamException {
        XMLStreamReader reader = createXmlStreamReader("<? xml ?>");
        StartDocument startDocument = (StartDocument) eventAllocator.allocate(reader);
        verify(startDocument, XMLStreamConstants.START_DOCUMENT, "<?xml version=\"1.0\"?>");
        Assert.assertEquals("UTF-8", startDocument.getCharacterEncodingScheme());
        Assert.assertFalse(startDocument.encodingSet());
        Assert.assertEquals("1.0", startDocument.getVersion());
        Assert.assertFalse(startDocument.isStandalone());
        Assert.assertFalse(startDocument.standaloneSet());
        reader.next();
        verify((EndDocument) eventAllocator.allocate(reader), XMLStreamConstants.END_DOCUMENT, "");

        reader = createXmlStreamReader("<? xml version=\"1.1\" ?>");
        startDocument = (StartDocument) eventAllocator.allocate(reader);
        Assert.assertEquals("1.1", startDocument.getVersion());

        reader = createXmlStreamReader("<? xml encoding=\"UTF-16\"?>");
        startDocument = (StartDocument) eventAllocator.allocate(reader);
        Assert.assertEquals("UTF-16", startDocument.getCharacterEncodingScheme());
        Assert.assertTrue(startDocument.encodingSet());
    }

    @Test
    public void testStartEndElement() throws XMLStreamException {
        XMLStreamReader reader = createXmlStreamReader("<alice/>");
        StartElement startElement = eventAllocator.allocate(reader).asStartElement();
        verify(startElement, XMLStreamConstants.START_ELEMENT, "<alice>");
        Assert.assertEquals(new QName("alice"), startElement.getName());
        Assert.assertNotNull(startElement.getNamespaceContext());
        Assert.assertFalse(startElement.getAttributes().hasNext());
        Assert.assertFalse(startElement.getNamespaces().hasNext());
        Assert.assertNull(startElement.getNamespaceURI("foo"));
        reader.next();
        EndElement endElement = eventAllocator.allocate(reader).asEndElement();
        verify(endElement, XMLStreamConstants.END_ELEMENT, "</alice>");
        Assert.assertEquals(new QName("alice"), endElement.getName());
        Assert.assertFalse(endElement.getNamespaces().hasNext());
    }

    @Test
    public void testCharactersCData() throws XMLStreamException {
        XMLStreamReader reader = createXmlStreamReader("<alice>bob</alice>");
        reader.next();
        Characters characters = eventAllocator.allocate(reader).asCharacters();
        verify(characters, XMLStreamConstants.CHARACTERS, "bob");
        Assert.assertEquals("bob", characters.getData());
        Assert.assertFalse(characters.isCData());
        Assert.assertFalse(characters.isWhiteSpace());
        Assert.assertFalse(characters.isIgnorableWhiteSpace());

        reader = createXmlStreamReader("<alice><![CDATA[bob]]></alice>");
        reader.next();
        characters = eventAllocator.allocate(reader).asCharacters();
        verify(characters, XMLStreamConstants.CDATA, "<![CDATA[bob]]>");
        Assert.assertEquals("bob", characters.getData());
        Assert.assertTrue(characters.isCData());

        reader = createXmlStreamReader("<alice><![CDATA[ ]]></alice>");
        reader.next();
        characters = eventAllocator.allocate(reader).asCharacters();
        verify(characters, XMLStreamConstants.CDATA, "<![CDATA[ ]]>");
        Assert.assertEquals(" ", characters.getData());
        Assert.assertTrue(characters.isWhiteSpace());
        Assert.assertFalse(characters.isIgnorableWhiteSpace());
    }

    @Test
    public void testAttributeNamespace() throws XMLStreamException {
        XMLStreamReader reader = createXmlStreamReader("<alice david=\"edgar\" xmlns=\"http://foo\"/>");
        StartElement startElement = eventAllocator.allocate(reader).asStartElement();
        verify(startElement, XMLStreamConstants.START_ELEMENT, "<alice xmlns=\"http://foo\" david=\"edgar\">");

        Iterator<?> attributes = startElement.getAttributes();
        Assert.assertTrue(attributes.hasNext());
        Attribute attribute = (Attribute) attributes.next();
        verify(attribute, XMLStreamConstants.ATTRIBUTE, "david=\"edgar\"");
        Assert.assertEquals(new QName("david"), attribute.getName());
        Assert.assertEquals("edgar", attribute.getValue());
        Assert.assertFalse(attributes.hasNext());

        Iterator<?> namespaces = startElement.getNamespaces();
        Assert.assertTrue(namespaces.hasNext());
        Namespace namespace = (Namespace) namespaces.next();
        verify(namespace, XMLStreamConstants.NAMESPACE, "xmlns=\"http://foo\"");
        Assert.assertEquals(new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns"), namespace.getName());
        Assert.assertEquals("http://foo", namespace.getValue());
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, namespace.getPrefix());
        Assert.assertEquals("http://foo", namespace.getNamespaceURI());
        Assert.assertFalse(namespaces.hasNext());

        reader = createXmlStreamReader("<alice p:david=\"edgar\" xmlns:p=\"http://foo\"/>");
        startElement = eventAllocator.allocate(reader).asStartElement();
        verify(startElement, XMLStreamConstants.START_ELEMENT, "<alice xmlns:p=\"http://foo\" p:david=\"edgar\">");

        attributes = startElement.getAttributes();
        Assert.assertTrue(attributes.hasNext());
        attribute = (Attribute) attributes.next();
        verify(attribute, XMLStreamConstants.ATTRIBUTE, "p:david=\"edgar\"");
        Assert.assertEquals(new QName("http://foo", "david", "p"), attribute.getName());
        Assert.assertEquals("edgar", attribute.getValue());
        Assert.assertFalse(attributes.hasNext());

        namespaces = startElement.getNamespaces();
        Assert.assertTrue(namespaces.hasNext());
        namespace = (Namespace) namespaces.next();
        verify(namespace, XMLStreamConstants.NAMESPACE, "xmlns:p=\"http://foo\"");
        Assert.assertEquals(new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "p", "xmlns"), namespace.getName());
        Assert.assertEquals("http://foo", namespace.getValue());
        Assert.assertEquals("p", namespace.getPrefix());
        Assert.assertEquals("http://foo", namespace.getNamespaceURI());
        Assert.assertFalse(namespaces.hasNext());
    }

    @Test
    public void testComment() throws XMLStreamException {
        XMLStreamReader reader = createXmlStreamReader("<!-- james -->");
        Comment comment = (Comment) eventAllocator.allocate(reader);
        verify(comment, XMLStreamConstants.COMMENT, "<!-- james -->");
        Assert.assertEquals(" james ", comment.getText());
    }

    @Test
    public void testProcessingInstruction() throws XMLStreamException {
        XMLStreamReader reader = createXmlStreamReader("<? joe ?>");
        ProcessingInstruction processingInstruction = (ProcessingInstruction) eventAllocator.allocate(reader);
        verify(processingInstruction, XMLStreamConstants.PROCESSING_INSTRUCTION, "<?joe?>");
        Assert.assertEquals("joe", processingInstruction.getTarget());
        Assert.assertNull(processingInstruction.getData());

        reader = createXmlStreamReader("<?joe jim?>");
        processingInstruction = (ProcessingInstruction) eventAllocator.allocate(reader);
        Assert.assertEquals("joe", processingInstruction.getTarget());
        Assert.assertEquals("jim", processingInstruction.getData());
    }
}
