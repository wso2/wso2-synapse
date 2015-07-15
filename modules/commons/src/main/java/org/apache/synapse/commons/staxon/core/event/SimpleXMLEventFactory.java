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

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

/**
 * Simple implementation of {@link XMLEventFactory}.
 */
public class SimpleXMLEventFactory extends XMLEventFactory {
    private Location location;

    public SimpleXMLEventFactory() {
        this(null);
    }

    public SimpleXMLEventFactory(Location location) {
        setLocation(location);
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public Attribute createAttribute(String prefix, String namespaceURI, String localName, String value) {
        return createAttribute(new QName(namespaceURI, localName, prefix), value);
    }

    @Override
    public Attribute createAttribute(String localName, String value) {
        return createAttribute(new QName(localName), value);
    }

    @Override
    public Attribute createAttribute(QName name, String value) {
        return new SimpleXMLEventAllocator.AttributeEvent(location, name, value, true);
    }

    @Override
    public Namespace createNamespace(String namespaceURI) {
        return createNamespace(XMLConstants.DEFAULT_NS_PREFIX, namespaceURI);
    }

    @Override
    public Namespace createNamespace(String prefix, String namespaceURI) {
        return new SimpleXMLEventAllocator.NamespaceEvent(location, namespaceURI, prefix);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    StartElement createStartElement(QName name, Iterator attributes, Iterator namespaces, NamespaceContext context) {
        return new SimpleXMLEventAllocator.StartElementEvent(location, name, attributes, namespaces, context);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public StartElement createStartElement(QName name, Iterator attributes, Iterator namespaces) {
        return createStartElement(name, attributes, namespaces, null);
    }

    @Override
    public StartElement createStartElement(String prefix, String namespaceUri, String localName) {
        return createStartElement(new QName(namespaceUri, localName, prefix), null, null);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator attributes, Iterator namespaces) {
        return createStartElement(new QName(namespaceUri, localName, prefix), attributes, namespaces);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator attributes, Iterator namespaces, NamespaceContext context) {
        return createStartElement(new QName(namespaceUri, localName, prefix), attributes, namespaces, context);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public EndElement createEndElement(QName name, Iterator namespaces) {
        return new SimpleXMLEventAllocator.EndElementEvent(location, name, namespaces);
    }

    @Override
    public EndElement createEndElement(String prefix, String namespaceUri, String localName) {
        return createEndElement(new QName(namespaceUri, localName, prefix), null);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public EndElement createEndElement(String prefix, String namespaceUri, String localName, Iterator namespaces) {
        return createEndElement(new QName(namespaceUri, localName, prefix), namespaces);
    }

    @Override
    public Characters createCharacters(String content) {
        return new SimpleXMLEventAllocator.CharactersEvent(XMLStreamConstants.CHARACTERS, location, content, false);
    }

    @Override
    public Characters createCData(String content) {
        return new SimpleXMLEventAllocator.CharactersEvent(XMLStreamConstants.CDATA, location, content, false);
    }

    @Override
    public Characters createSpace(String content) {
        return new SimpleXMLEventAllocator.CharactersEvent(XMLStreamConstants.CHARACTERS, location, content, true);
    }

    @Override
    public Characters createIgnorableSpace(String content) {
        return new SimpleXMLEventAllocator.CharactersEvent(XMLStreamConstants.SPACE, location, content, true);
    }

    @Override
    public StartDocument createStartDocument() {
        return new SimpleXMLEventAllocator.StartDocumentEvent(location, null, null, null);
    }

    @Override
    public StartDocument createStartDocument(String encoding, String version, boolean standalone) {
        return new SimpleXMLEventAllocator.StartDocumentEvent(location, encoding, version, standalone);
    }

    @Override
    public StartDocument createStartDocument(String encoding, String version) {
        return new SimpleXMLEventAllocator.StartDocumentEvent(location, encoding, version, null);
    }

    @Override
    public StartDocument createStartDocument(String encoding) {
        return new SimpleXMLEventAllocator.StartDocumentEvent(location, encoding, null, null);
    }

    @Override
    public EndDocument createEndDocument() {
        return new SimpleXMLEventAllocator.EndDocumentEvent(location);
    }

    @Override
    public EntityReference createEntityReference(String name, EntityDeclaration declaration) {
        return new SimpleXMLEventAllocator.EntityReferenceEvent(location, name, declaration);
    }

    @Override
    public Comment createComment(String text) {
        return new SimpleXMLEventAllocator.CommentEvent(location, text);
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String target, String data) {
        return new SimpleXMLEventAllocator.ProcessingInstructionEvent(location, target, data);
    }

    @Override
    public DTD createDTD(String dtd) {
        throw new UnsupportedOperationException("DTD event is not supported");
    }
}
