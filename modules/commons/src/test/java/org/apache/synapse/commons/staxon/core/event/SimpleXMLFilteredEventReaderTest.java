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

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

import org.junit.Assert;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.xml.SimpleXMLInputFactory;

public class SimpleXMLFilteredEventReaderTest {
    static final EventFilter ACCEPT_ALL = new EventFilter() {
        @Override
        public boolean accept(XMLEvent event) {
            return true;
        }
    };

    static class TypeFilter implements EventFilter {
        final int type;

        public TypeFilter(int type) {
            this.type = type;
        }

        @Override
        public boolean accept(XMLEvent event) {
            return event.getEventType() != type;
        }
    }

    ;

    private static XMLInputFactory factory = new SimpleXMLInputFactory();

    private XMLEventReader createXmlEventReader(String xml, EventFilter filter) throws XMLStreamException {
        return new SimpleXMLFilteredEventReader(factory.createXMLEventReader(new StringReader(xml)), filter);
    }

    @Test
    public void testStartEndDocument() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<? xml version=\"1.0\" ?>", ACCEPT_ALL);
        Assert.assertEquals(XMLStreamConstants.START_DOCUMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_DOCUMENT, reader.nextEvent().getEventType());
    }

    @Test
    public void testStartEndElement() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice/>", ACCEPT_ALL);
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());

        reader = createXmlEventReader("<alice></alice>", ACCEPT_ALL);
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());
    }

    @Test
    public void testStartEndElement2() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice/>", new TypeFilter(XMLStreamConstants.START_ELEMENT));
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());

        reader = createXmlEventReader("<alice></alice>", new TypeFilter(XMLStreamConstants.END_ELEMENT));
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertFalse(reader.hasNext());
    }

    @Test
    public void testCharactersCData() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice>bob</alice>", ACCEPT_ALL);
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.CHARACTERS, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());

        reader = createXmlEventReader("<alice><![CDATA[bob]]></alice>", ACCEPT_ALL);
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.CDATA, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());
    }

    @Test
    public void testCharactersCData2() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice>bob</alice>", new TypeFilter(XMLStreamConstants.CHARACTERS));
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());

        reader = createXmlEventReader("<alice><![CDATA[bob]]></alice>", new TypeFilter(XMLStreamConstants.CDATA));
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextEvent().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextEvent().getEventType());
    }

    @Test
    public void testAttributeNamespace() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice david=\"edgar\" xmlns=\"http://foo\"/>", ACCEPT_ALL);
        XMLEvent event = reader.nextEvent();
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, event.getEventType());
        Iterator<?> attributes = event.asStartElement().getAttributes();
        Assert.assertEquals(XMLStreamConstants.ATTRIBUTE, ((Attribute) attributes.next()).getEventType());
        Assert.assertFalse(attributes.hasNext());
        Iterator<?> namespaces = event.asStartElement().getNamespaces();
        Assert.assertEquals(XMLStreamConstants.NAMESPACE, ((Namespace) namespaces.next()).getEventType());
        Assert.assertFalse(attributes.hasNext());
    }

    @Test
    public void testComment() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<!--james-->", ACCEPT_ALL);
        Assert.assertEquals(XMLStreamConstants.COMMENT, reader.nextEvent().getEventType());
        Assert.assertFalse(reader.hasNext());
    }

    @Test
    public void testComment2() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<!--james-->", new TypeFilter(XMLStreamConstants.COMMENT));
        Assert.assertFalse(reader.hasNext());
    }

    @Test
    public void testProcessingInstruction() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?joe?>", ACCEPT_ALL);
        Assert.assertEquals(XMLStreamConstants.PROCESSING_INSTRUCTION, reader.nextEvent().getEventType());
        Assert.assertFalse(reader.hasNext());
    }

    @Test
    public void testProcessingInstruction2() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?joe?>", new TypeFilter(XMLStreamConstants.PROCESSING_INSTRUCTION));
        Assert.assertFalse(reader.hasNext());
    }

    @Test
    public void testPeek() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\">bob</alice>", ACCEPT_ALL);
        while (reader.hasNext()) {
            Assert.assertEquals(reader.peek(), reader.nextEvent());
        }
    }

    @Test
    public void testPeek2() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\">bob</alice>", new TypeFilter(XMLStreamConstants.START_ELEMENT));
        while (reader.hasNext()) {
            Assert.assertEquals(reader.peek(), reader.nextEvent());
        }
    }

    @Test
    public void testGetElementText() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice david=\"edgar\">bob</alice>", ACCEPT_ALL);
        reader.next();
        Assert.assertEquals("bob", reader.getElementText());

        reader = createXmlEventReader("<alice david=\"edgar\">bob</alice>", ACCEPT_ALL);
        reader.next();
        reader.peek();
        Assert.assertEquals("bob", reader.getElementText());
    }

    @Test
    public void testGetElementText2() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<alice david=\"edgar\">bob</alice>", new TypeFilter(XMLStreamConstants.CHARACTERS));
        reader.next();
        Assert.assertEquals("", reader.getElementText());

        reader = createXmlEventReader("<alice david=\"edgar\">bob</alice>", new TypeFilter(XMLStreamConstants.CHARACTERS));
        reader.next();
        reader.peek();
        Assert.assertEquals("", reader.getElementText());
    }

    @Test
    public void testNextTag() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\"><edgar/></alice>", ACCEPT_ALL);
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag().getEventType());
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());

        reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\"><edgar/></alice>", ACCEPT_ALL);
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag().getEventType());
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.START_ELEMENT, reader.nextTag().getEventType());
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
    }

    @Test
    public void testNextTag2() throws XMLStreamException {
        XMLEventReader reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\"><edgar/></alice>", new TypeFilter(XMLStreamConstants.START_ELEMENT));
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());

        reader = createXmlEventReader("<?xml version=\"1.0\"?><alice david=\"edgar\"><edgar/></alice>", new TypeFilter(XMLStreamConstants.START_ELEMENT));
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
        reader.peek();
        Assert.assertEquals(XMLStreamConstants.END_ELEMENT, reader.nextTag().getEventType());
    }
}
