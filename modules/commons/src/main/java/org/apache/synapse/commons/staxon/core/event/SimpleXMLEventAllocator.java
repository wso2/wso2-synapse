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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.stream.util.XMLEventConsumer;

/**
 * Simple implementation of {@link XMLEventAllocator}.
 */
public class SimpleXMLEventAllocator implements XMLEventAllocator {
    static abstract class AbstractXMLEvent implements XMLEvent {
        static <E> List<E> toList(Iterator<E> iterator) {
            if (iterator == null || !iterator.hasNext()) {
                return Collections.emptyList();
            } else {
                List<E> list = new ArrayList<E>();
                while (iterator.hasNext()) {
                    list.add(iterator.next());
                }
                return list;
            }
        }

        final int eventType;
        final Location location;

        AbstractXMLEvent(int eventType, Location location) {
            this.eventType = eventType;
            this.location = location;
        }

        @Override
        public Characters asCharacters() {
            return (Characters) this;
        }

        @Override
        public StartElement asStartElement() {
            return (StartElement) this;
        }

        @Override
        public EndElement asEndElement() {
            return (EndElement) this;
        }

        @Override
        public int getEventType() {
            return eventType;
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public QName getSchemaType() {
            return null;
        }

        @Override
        public boolean isAttribute() {
            return eventType == XMLStreamConstants.ATTRIBUTE;
        }

        @Override
        public boolean isCharacters() {
            return eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA;
        }

        @Override
        public boolean isEndDocument() {
            return eventType == XMLStreamConstants.END_DOCUMENT;
        }

        @Override
        public boolean isEndElement() {
            return eventType == XMLStreamConstants.END_ELEMENT;
        }

        @Override
        public boolean isEntityReference() {
            return eventType == XMLStreamConstants.ENTITY_REFERENCE;
        }

        @Override
        public boolean isNamespace() {
            return eventType == XMLStreamConstants.NAMESPACE;
        }

        @Override
        public boolean isProcessingInstruction() {
            return eventType == XMLStreamConstants.PROCESSING_INSTRUCTION;
        }

        @Override
        public boolean isStartDocument() {
            return eventType == XMLStreamConstants.START_DOCUMENT;
        }

        @Override
        public boolean isStartElement() {
            return eventType == XMLStreamConstants.START_ELEMENT;
        }

        @Override
        public String toString() {
            try {
                Writer writer = new StringWriter();
                writer.write(getClass().getSimpleName());
                writer.write('(');
                writeAsEncodedUnicodeInternal(writer);
                writer.write(')');
                return writer.toString();
            } catch (IOException e) {
                return super.toString();
            }
        }

        @Override
        public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
            try {
                writeAsEncodedUnicodeInternal(writer);
            } catch (IOException e) {
                throw new XMLStreamException(e);
            }

        }

        abstract void writeAsEncodedUnicodeInternal(Writer writer) throws IOException;
    }

    static class AttributeEvent extends AbstractXMLEvent implements Attribute {
        final QName name;
        final String value;
        final boolean specified;

        AttributeEvent(Location location, QName name, String value, boolean specified) {
            this(XMLStreamConstants.ATTRIBUTE, location, name, value, specified);
        }

        AttributeEvent(int eventType, Location location, QName name, String value, boolean specified) {
            super(eventType, location);
            assert eventType == XMLStreamConstants.ATTRIBUTE || eventType == XMLStreamConstants.NAMESPACE;
            this.name = name;
            this.value = value;
            this.specified = specified;
        }

        @Override
        public String getDTDType() {
            return "CDATA";
        }

        @Override
        public QName getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public boolean isSpecified() {
            return specified;
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) throws IOException {
            if (!XMLConstants.DEFAULT_NS_PREFIX.equals(name.getPrefix())) {
                writer.write(name.getPrefix());
                writer.write(':');
            }
            writer.write(name.getLocalPart());
            writer.write('=');
            writer.write('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '<':
                        writer.write("&lt;");
                        break;
                    case '>':
                        writer.write("&gt;");
                        break;
                    case '&':
                        writer.write("&amp;");
                        break;
                    case '"':
                        writer.write("&quot;");
                        break;
                    default:
                        writer.write(c);
                }
            }
            writer.write('"');
        }
    }

    static class CharactersEvent extends AbstractXMLEvent implements Characters {
        final String data;
        final boolean whitespace;

        CharactersEvent(XMLStreamReader reader) {
            this(reader.getEventType(), reader.getLocation(), reader.getText(), reader.isWhiteSpace());
        }

        CharactersEvent(int eventType, Location location, String data, boolean whitespace) {
            super(eventType, location);
            assert eventType == XMLStreamConstants.CHARACTERS
                    || eventType == XMLStreamConstants.CDATA
                    || eventType == XMLStreamConstants.SPACE;
            this.data = data;
            this.whitespace = whitespace;
        }

        @Override
        public String getData() {
            return data;
        }

        @Override
        public boolean isCData() {
            return eventType == XMLStreamConstants.CDATA;
        }

        @Override
        public boolean isIgnorableWhiteSpace() {
            return eventType == XMLStreamConstants.SPACE;
        }

        @Override
        public boolean isWhiteSpace() {
            return whitespace;
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) throws IOException {
            if (isCData()) {
                writer.write("<![CDATA[");
                writer.write(data);
                writer.write("]]>");
            } else if (!isIgnorableWhiteSpace()) { // API doc: No indentation or whitespace should be "outputted".
                for (int i = 0; i < data.length(); i++) {
                    char c = data.charAt(i);
                    switch (c) {
                        case '<':
                            writer.write("&lt;");
                            break;
                        case '>':
                            writer.write("&gt;");
                            break;
                        case '&':
                            writer.write("&amp;");
                            break;
                        default:
                            writer.write(c);
                    }
                }
            }
        }
    }

    static class CommentEvent extends AbstractXMLEvent implements Comment {
        final String text;

        CommentEvent(XMLStreamReader reader) {
            super(reader.getEventType(), reader.getLocation());
            assert eventType == XMLStreamConstants.COMMENT;
            text = reader.getText();
        }

        CommentEvent(Location location, String text) {
            super(XMLStreamConstants.COMMENT, location);
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) throws IOException {
            writer.write("<!--");
            writer.write(text);
            writer.write("-->");
        }
    }

    static class EndDocumentEvent extends AbstractXMLEvent implements EndDocument {
        EndDocumentEvent(XMLStreamReader reader) {
            super(reader.getEventType(), reader.getLocation());
            assert eventType == XMLStreamConstants.END_DOCUMENT;
        }

        EndDocumentEvent(Location location) {
            super(XMLStreamConstants.END_DOCUMENT, location);
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) {
            // silence
        }
    }

    static class EndElementEvent extends AbstractXMLEvent implements EndElement {
        final QName name;
        final List<NamespaceEvent> namespaces;

        EndElementEvent(XMLStreamReader reader) {
            super(reader.getEventType(), reader.getLocation());
            assert eventType == XMLStreamConstants.END_ELEMENT;
            name = reader.getName();
            if (reader.getNamespaceCount() == 0) {
                namespaces = Collections.emptyList();
            } else {
                namespaces = new ArrayList<NamespaceEvent>(reader.getNamespaceCount());
                for (int i = 0; i < reader.getNamespaceCount(); i++) {
                    namespaces.add(new NamespaceEvent(location, reader.getNamespaceURI(i), reader.getNamespacePrefix(i)));
                }
            }
        }

        EndElementEvent(Location location, QName name, Iterator<NamespaceEvent> namespaces) {
            super(XMLStreamConstants.END_ELEMENT, location);
            this.name = name;
            this.namespaces = toList(namespaces);
        }

        @Override
        public QName getName() {
            return name;
        }

        @Override
        public Iterator<?> getNamespaces() {
            return namespaces.iterator();
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) throws IOException {
            writer.write("</");
            if (!XMLConstants.DEFAULT_NS_PREFIX.equals(name.getPrefix())) {
                writer.write(name.getPrefix());
                writer.write(':');
            }
            writer.write(name.getLocalPart());
            writer.write('>');
        }
    }

    static class EntityReferenceEvent extends AbstractXMLEvent implements EntityReference {
        final String name;
        final EntityDeclaration declaration;

        EntityReferenceEvent(XMLStreamReader reader) {
            super(reader.getEventType(), reader.getLocation());
            assert eventType == XMLStreamConstants.ENTITY_REFERENCE;
            name = reader.getText();
            declaration = null; // TODO
        }

        EntityReferenceEvent(Location location, String name, EntityDeclaration declaration) {
            super(XMLStreamConstants.ENTITY_REFERENCE, location);
            this.name = name;
            this.declaration = declaration;
        }

        @Override
        public EntityDeclaration getDeclaration() {
            return declaration;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) throws IOException {
            writer.write('&');
            writer.write(name);
            writer.write(';');
        }
    }

    static class NamespaceEvent extends AttributeEvent implements Namespace {
        static QName createName(String prefix) {
            if (prefix == null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE);
            } else {
                return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix, XMLConstants.XMLNS_ATTRIBUTE);
            }
        }

        NamespaceEvent(Location location, String namespaceURI, String prefix) {
            super(XMLStreamConstants.NAMESPACE, location, createName(prefix), namespaceURI, true);
        }

        @Override
        public String getPrefix() {
            return isDefaultNamespaceDeclaration() ? XMLConstants.DEFAULT_NS_PREFIX : getName().getLocalPart();
        }

        @Override
        public String getNamespaceURI() {
            return getValue();
        }

        @Override
        public boolean isDefaultNamespaceDeclaration() {
            return XMLConstants.DEFAULT_NS_PREFIX.equals(getName().getPrefix());
        }
    }

    static class ProcessingInstructionEvent extends AbstractXMLEvent implements ProcessingInstruction {
        final String target;
        final String data;

        ProcessingInstructionEvent(XMLStreamReader reader) {
            super(reader.getEventType(), reader.getLocation());
            assert eventType == XMLStreamConstants.PROCESSING_INSTRUCTION;
            target = reader.getPITarget();
            data = reader.getPIData();
        }

        ProcessingInstructionEvent(Location location, String target, String data) {
            super(XMLStreamConstants.PROCESSING_INSTRUCTION, location);
            this.target = target;
            this.data = data;
        }

        @Override
        public String getTarget() {
            return target;
        }

        @Override
        public String getData() {
            return data;
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) throws IOException {
            writer.write("<?");
            writer.write(target);
            if (data != null) {
                writer.write(' ');
                writer.write(data.trim());
            }
            writer.write("?>");
        }
    }

    static class StartDocumentEvent extends AbstractXMLEvent implements StartDocument {
        final String encodingScheme;
        final String version;
        final Boolean standalone;

        StartDocumentEvent(XMLStreamReader reader) {
            super(reader.getEventType(), reader.getLocation());
            assert eventType == XMLStreamConstants.START_DOCUMENT;
            encodingScheme = reader.getCharacterEncodingScheme();
            version = reader.getVersion() == null ? "1.0" : reader.getVersion();
            standalone = reader.standaloneSet() ? Boolean.valueOf(reader.isStandalone()) : null;
        }

        StartDocumentEvent(Location location, String encoding, String version, Boolean standalone) {
            super(XMLStreamConstants.START_DOCUMENT, location);
            this.encodingScheme = encoding;
            this.version = version == null ? "1.0" : version;
            this.standalone = standalone;
        }

        @Override
        public String getCharacterEncodingScheme() {
            return encodingSet() ? encodingScheme : "UTF-8";
        }

        @Override
        public boolean encodingSet() {
            return encodingScheme != null;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public boolean isStandalone() {
            return standaloneSet() ? standalone.booleanValue() : false;
        }

        @Override
        public boolean standaloneSet() {
            return standalone != null;
        }

        @Override
        public String getSystemId() {
            return location.getSystemId();
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) throws IOException {
            writer.write("<?xml version=\"");
            writer.write(version);
            writer.write('"');
            if (encodingSet()) {
                writer.write(" encoding=\"");
                writer.write(encodingScheme);
                writer.write('"');
            }
            if (standaloneSet()) {
                writer.write(" standalone=\"");
                writer.write(standalone ? "yes" : "no");
                writer.write('"');
            }
            writer.write("?>");
        }
    }

    static class StartElementEvent extends AbstractXMLEvent implements StartElement {
        final QName name;
        final List<AttributeEvent> attributes;
        final List<NamespaceEvent> namespaces;
        final NamespaceContext context;

        StartElementEvent(XMLStreamReader reader) {
            super(reader.getEventType(), reader.getLocation());
            assert eventType == XMLStreamConstants.START_ELEMENT;
            name = reader.getName();
            context = reader.getNamespaceContext();
            if (reader.getAttributeCount() == 0) {
                attributes = Collections.emptyList();
            } else {
                attributes = new ArrayList<AttributeEvent>(reader.getAttributeCount());
                for (int i = 0; i < reader.getAttributeCount(); i++) {
                    attributes.add(new AttributeEvent(location, reader.getAttributeName(i), reader.getAttributeValue(i), reader.isAttributeSpecified(i)));
                }
            }
            if (reader.getNamespaceCount() == 0) {
                namespaces = Collections.emptyList();
            } else {
                namespaces = new ArrayList<NamespaceEvent>(reader.getNamespaceCount());
                for (int i = 0; i < reader.getNamespaceCount(); i++) {
                    namespaces.add(new NamespaceEvent(location, reader.getNamespaceURI(i), reader.getNamespacePrefix(i)));
                }
            }
        }

        StartElementEvent(Location location, QName name, Iterator<AttributeEvent> attributes, Iterator<NamespaceEvent> namespaces, NamespaceContext context) {
            super(XMLStreamConstants.START_ELEMENT, location);
            this.name = name;
            this.context = context;
            this.attributes = toList(attributes);
            this.namespaces = toList(namespaces);
        }

        @Override
        public Attribute getAttributeByName(QName name) {
            for (Attribute attribute : attributes) {
                if (attribute.getName().equals(name)) {
                    return attribute;
                }
            }
            return null;
        }

        @Override
        public Iterator<?> getAttributes() {
            return attributes.iterator();
        }

        @Override
        public QName getName() {
            return name;
        }

        @Override
        public NamespaceContext getNamespaceContext() {
            return context;
        }

        @Override
        public Iterator<?> getNamespaces() {
            return namespaces.iterator();
        }

        @Override
        public String getNamespaceURI(String prefix) {
            for (Namespace namespace : namespaces) {
                if (namespace.getPrefix().equals(prefix)) {
                    return namespace.getNamespaceURI();
                }
            }
            return null;
        }

        @Override
        void writeAsEncodedUnicodeInternal(Writer writer) throws IOException {
            writer.write('<');
            if (!XMLConstants.DEFAULT_NS_PREFIX.equals(name.getPrefix())) {
                writer.write(name.getPrefix());
                writer.write(':');
            }
            writer.write(name.getLocalPart());
            for (NamespaceEvent namespace : namespaces) {
                writer.write(' ');
                namespace.writeAsEncodedUnicodeInternal(writer);
            }
            for (AttributeEvent attribute : attributes) {
                if (attribute.isSpecified()) {
                    writer.write(' ');
                    attribute.writeAsEncodedUnicodeInternal(writer);
                }
            }
            writer.write('>');
        }
    }

    @Override
    public XMLEventAllocator newInstance() {
        return this;
    }

    @Override
    public XMLEvent allocate(XMLStreamReader reader) throws XMLStreamException {
        switch (reader.getEventType()) {
            case XMLStreamConstants.CDATA:
            case XMLStreamConstants.CHARACTERS:
            case XMLStreamConstants.SPACE:
                return new CharactersEvent(reader);
            case XMLStreamConstants.COMMENT:
                return new CommentEvent(reader);
            case XMLStreamConstants.DTD:
                throw new UnsupportedOperationException();
            case XMLStreamConstants.END_DOCUMENT:
                return new EndDocumentEvent(reader);
            case XMLStreamConstants.END_ELEMENT:
                return new EndElementEvent(reader);
            case XMLStreamConstants.ENTITY_REFERENCE:
                return new EntityReferenceEvent(reader);
            case XMLStreamConstants.NOTATION_DECLARATION:
                throw new UnsupportedOperationException();
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                return new ProcessingInstructionEvent(reader);
            case XMLStreamConstants.START_DOCUMENT:
                return new StartDocumentEvent(reader);
            case XMLStreamConstants.START_ELEMENT:
                return new StartElementEvent(reader);
            default:
                throw new XMLStreamException("Unexpected event type: " + reader.getEventType());
        }
    }

    @Override
    public void allocate(XMLStreamReader reader, XMLEventConsumer consumer) throws XMLStreamException {
        consumer.add(allocate(reader));
    }
}
