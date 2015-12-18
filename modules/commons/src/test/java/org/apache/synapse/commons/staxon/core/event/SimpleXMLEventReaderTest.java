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
import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

import org.junit.Assert;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.xml.SimpleXMLInputFactory;

public class SimpleXMLEventReaderTest {
    private XMLEventReader createXmlEventReader(String xml) throws XMLStreamException {
        return new SimpleXMLInputFactory().createXMLEventReader(new StringReader(xml));
    }

    @Test
    public void testStartEndDocument() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<? xml version=\"1.0\" ?>");
        Assert.assertEquals(XMLStreamConstants.START_DOCUMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_DOCUMENT, reader.nextEvent().getEventType());
    }

    @Test
    public void testStartEndElement() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice/>");
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());

        reader = createXmlEventReader("<alice></alice>");
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());
    }

    @Test
    public void testCharactersCData() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice>bob</alice>");
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.CHARACTERS, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());

        reader = createXmlEventReader("<alice><![CDATA[bob]]></alice>");
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.CDATA, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());
    }

    @Test
    public void testAttributeNamespace() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice david=\"edgar\" xmlns=\"http://foo\"/>");
        XMLEvent event = reader.nextEvent();
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, event.getEventType());
        Iterator<?> attributes = event.asStartElement().getAttributes();
        Assert.assertEquals(XMLStreamConstants.ATTRIBUTE, ((Attribute) attributes.next()).getEventType());
        Assert.assertFalse(attributes.hasNext());
        Iterator<?> namespaces = event.asStartElement().getNamespaces();
        Assert.assertEquals(XMLStreamConstants.NAMESPACE, ((Namespace) namespaces.next()).getEventType());
        Assert.assertFalse(namespaces.hasNext());
    }

    @Test
    public void testComment() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<!--james-->");
        Assert.assertEquals(XMLStreamConstants.COMMENT, reader.nextEvent().getEventType());
    }

    @Test
    public void testProcessingInstruction() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?joe?>");
        Assert.assertEquals(XMLStreamConstants.PROCESSING_INSTRUCTION, reader.nextEvent().getEventType());
    }

    @Test
    public void testPeek() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\">bob</alice>");
        while (reader.hasNext()) {
            Assert.assertEquals(reader.peek(), reader.nextEvent());
        }
    }

    @Test
    public void testGetElementText() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice david=\"edgar\">bob</alice>");
        reader.next();
        Assert.assertEquals("bob", reader.getElementText());

        reader = createXmlEventReader("<alice david=\"edgar\">bob</alice>");
        reader.next();
        reader.peek();
        Assert.assertEquals("bob", reader.getElementText());
    }

    @Test
    public void testNextTag() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\"><edgar/></alice>");
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag().getEventType());
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());

        reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\"><edgar/></alice>");
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag().getEventType());
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag().getEventType());
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
    }
}
