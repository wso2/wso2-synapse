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

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Simple implementation of {@link XMLEventWriter}.
 */
public class SimpleXMLEventWriter implements XMLEventWriter {
    private final XMLStreamWriter delegate;

    public SimpleXMLEventWriter(XMLStreamWriter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        switch (event.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE:
                Attribute attribute = (Attribute) event;
                QName attrName = attribute.getName();
                delegate.writeAttribute(attrName.getPrefix(), attrName.getNamespaceURI(), attrName.getLocalPart(), attribute.getValue());
                break;
            case XMLStreamConstants.END_DOCUMENT:
                delegate.writeEndDocument();
                break;
            case XMLStreamConstants.END_ELEMENT:
                delegate.writeEndElement();
                break;
            case XMLStreamConstants.NAMESPACE:
                Namespace namespace = (Namespace) event;
                delegate.writeNamespace(namespace.getPrefix(), namespace.getNamespaceURI());
                break;
            case XMLStreamConstants.START_DOCUMENT:
                StartDocument startDocument = (StartDocument) event;
                if (startDocument.encodingSet()) { // encoding defined?
                    delegate.writeStartDocument(startDocument.getCharacterEncodingScheme(), startDocument.getVersion());
                } else {
                    delegate.writeStartDocument(startDocument.getVersion());
                }
                break;
            case XMLStreamConstants.START_ELEMENT:
                StartElement startElement = event.asStartElement();
                QName elemName = startElement.getName();
                delegate.writeStartElement(elemName.getPrefix(), elemName.getLocalPart(), elemName.getNamespaceURI());
                Iterator<?> namespaces = startElement.getNamespaces();
                while (namespaces.hasNext()) {
                    add((Namespace) namespaces.next());
                }
                Iterator<?> attributes = startElement.getAttributes();
                while (attributes.hasNext()) {
                    add((Attribute) attributes.next());
                }
                break;
            case XMLStreamConstants.CHARACTERS:
            case XMLStreamConstants.CDATA:
                Characters characters = event.asCharacters();
                if (characters.isCData()) {
                    delegate.writeCData(characters.getData());
                } else {
                    delegate.writeCharacters(characters.getData());
                }
                break;
            case XMLStreamConstants.COMMENT:
                delegate.writeComment(((Comment) event).getText());
                break;
            case XMLStreamConstants.DTD:
                delegate.writeDTD(((DTD) event).getDocumentTypeDeclaration());
                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                delegate.writeEntityRef(((EntityReference) event).getName());
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                ProcessingInstruction processingInstruction = (ProcessingInstruction) event;
                delegate.writeProcessingInstruction(processingInstruction.getTarget(), processingInstruction.getData());
                break;
            case XMLStreamConstants.SPACE:
                break;
            default:
                throw new XMLStreamException("Cannot write event " + event);
        }
    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {
        while (reader.peek() != null) {
            add(reader.nextEvent());
        }
    }

    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        delegate.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext ctxt) throws XMLStreamException {
        delegate.setNamespaceContext(ctxt);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        delegate.setPrefix(prefix, uri);
    }
}
