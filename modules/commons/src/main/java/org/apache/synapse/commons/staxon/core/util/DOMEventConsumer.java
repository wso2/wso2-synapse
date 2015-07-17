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
package org.apache.synapse.commons.staxon.core.util;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventConsumer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * DOM event consumer.
 */
public class DOMEventConsumer implements XMLEventConsumer {
    private final Document document;
    private final boolean namespaceAware;

    private Node node;

    /**
     * Create namespace-aware consumer instance.
     *
     * @param node the node to which events will be appended
     */
    public DOMEventConsumer(Node node) {
        this(node, true);
    }

    /**
     * Create consumer instance.
     *
     * @param node           the node to which events will be appended
     * @param namespaceAware whether the DOM will be namespace-aware
     */
    public DOMEventConsumer(Node node, boolean namespaceAware) {
        this.document = node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node : node.getOwnerDocument();
        this.namespaceAware = namespaceAware;
        this.node = node;
    }

    private String qName(QName name) {
        if (XMLConstants.DEFAULT_NS_PREFIX.equals(name.getPrefix())) {
            return name.getLocalPart();
        } else {
            return name.getPrefix() + ':' + name.getLocalPart();
        }
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        switch (event.getEventType()) {
            case XMLStreamConstants.CDATA:
                node.appendChild(document.createCDATASection(event.asCharacters().getData()));
                break;
            case XMLStreamConstants.CHARACTERS:
                node.appendChild(document.createTextNode(event.asCharacters().getData()));
                break;
            case XMLStreamConstants.COMMENT:
                node.appendChild(document.createComment(((Comment) event).getText()));
                break;
            case XMLStreamConstants.END_ELEMENT:
                node = node.getParentNode();
                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                node.appendChild(document.createEntityReference(((EntityReference) event).getName()));
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                ProcessingInstruction pi = (ProcessingInstruction) event;
                node.appendChild(document.createProcessingInstruction(pi.getTarget(), pi.getData()));
                break;
            case XMLStreamConstants.START_ELEMENT:
                StartElement startElement = event.asStartElement();
                QName elementName = startElement.getName();
                Element element;
                if (namespaceAware) {
                    element = document.createElementNS(elementName.getNamespaceURI(), qName(elementName));
                } else {
                    element = document.createElement(elementName.getLocalPart());
                }
                Iterator<?> namespaces = startElement.getNamespaces();
                while (namespaces.hasNext()) {
                    Namespace ns = (Namespace) namespaces.next();
                    element.setAttributeNS(ns.getName().getNamespaceURI(), qName(ns.getName()), ns.getNamespaceURI());
                }
                Iterator<?> attributes = startElement.getAttributes();
                while (attributes.hasNext()) {
                    Attribute at = (Attribute) attributes.next();
                    if (namespaceAware) {
                        element.setAttributeNS(at.getName().getNamespaceURI(), qName(at.getName()), at.getValue());
                    } else {
                        element.setAttribute(at.getName().getLocalPart(), at.getValue());
                    }
                }
                node = node.appendChild(element);
                break;
            case XMLStreamConstants.SPACE:
            case XMLStreamConstants.ENTITY_DECLARATION:
            case XMLStreamConstants.NOTATION_DECLARATION:
            case XMLStreamConstants.START_DOCUMENT:
            case XMLStreamConstants.END_DOCUMENT:
            case XMLStreamConstants.DTD:
                break;
            default:
                throw new XMLStreamException("Unexpected event type " + event.getEventType() + "; " + event);
        }
    }

    /**
     * Add all events from the given reader.
     *
     * @param reader
     * @throws XMLStreamException
     */
    public void add(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            add(reader.nextEvent());
        }
    }

    /**
     * This method will create and populate a {@link org.w3c.dom.Document} from
     * the given event reader.
     *
     * @param reader event reader
     * @return document
     * @throws XMLStreamException
     */
    public static Document consume(XMLEventReader reader) throws XMLStreamException {
        DocumentBuilder documentBuilder;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new XMLStreamException(e);
        }
        return consume(reader, documentBuilder);
    }

    /**
     * This method will create and populate a {@link org.w3c.dom.Document} from
     * the given event reader.
     *
     * @param reader          event reader
     * @param documentBuilder
     * @return document
     * @throws XMLStreamException
     */
    public static Document consume(XMLEventReader reader, DocumentBuilder documentBuilder) throws XMLStreamException {
        Document document = documentBuilder.newDocument();
        consume(reader, document);
        return document;
    }

    /**
     * This method will populate given {@link org.w3c.dom.Node} from the given
     * event reader.
     *
     * @param reader event reader
     * @param node   parent node
     * @throws XMLStreamException
     */
    public static void consume(XMLEventReader reader, Node node) throws XMLStreamException {
        boolean namespaceAware = true; // Boolean.FALSE.equals(reader.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        new DOMEventConsumer(node, namespaceAware).add(reader);
    }
}
