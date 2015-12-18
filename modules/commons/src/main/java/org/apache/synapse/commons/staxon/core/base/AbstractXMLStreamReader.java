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

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Abstract XML stream reader.
 */
public abstract class AbstractXMLStreamReader<T> implements XMLStreamReader {
    class Event {
        private final int type;
        private final XMLStreamReaderScope<T> scope;
        private final String text;
        private final Object data;
        private final int lineNumber;
        private final int columnNumber;
        private final int characterOffset;

        Event(int type, XMLStreamReaderScope<T> scope) {
            this(type, scope, null, null);
        }

        Event(int type, XMLStreamReaderScope<T> scope, String text, Object data) {
            this.type = type;
            this.scope = scope;
            this.text = text;
            this.data = data;
            this.lineNumber = locationProvider.getLineNumber();
            this.columnNumber = locationProvider.getColumnNumber();
            this.characterOffset = locationProvider.getCharacterOffset();
        }

        XMLStreamReaderScope<T> getScope() {
            return scope;
        }

        int getType() {
            return type;
        }

        String getText() {
            return text;
        }

        Object getData() {
            return data;
        }

        Location getLocation() {
            return new Location() {
                @Override
                public int getLineNumber() {
                    return lineNumber;
                }

                @Override
                public int getColumnNumber() {
                    return columnNumber;
                }

                @Override
                public int getCharacterOffset() {
                    return characterOffset;
                }

                @Override
                public String getPublicId() {
                    return locationProvider.getPublicId();
                }

                @Override
                public String getSystemId() {
                    return locationProvider.getSystemId();
                }
            };
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(getEventName(type))
                    .append(": ").append(getText() != null ? getText() : getLocalName())
                    .toString();
        }
    }

    static String getEventName(int type) {
        switch (type) {
            case XMLStreamConstants.ATTRIBUTE:
                return "ATTRIBUTE";
            case XMLStreamConstants.CDATA:
                return "CDATA";
            case XMLStreamConstants.CHARACTERS:
                return "CHARACTERS";
            case XMLStreamConstants.COMMENT:
                return "COMMENT";
            case XMLStreamConstants.DTD:
                return "DTD";
            case XMLStreamConstants.END_DOCUMENT:
                return "END_DOCUMENT";
            case XMLStreamConstants.END_ELEMENT:
                return "END_ELEMENT";
            case XMLStreamConstants.ENTITY_DECLARATION:
                return "ENTITY_DECLARATION";
            case XMLStreamConstants.ENTITY_REFERENCE:
                return "ENTITY_REFERENCE";
            case XMLStreamConstants.NAMESPACE:
                return "NAMESPACE";
            case XMLStreamConstants.NOTATION_DECLARATION:
                return "NOTATION_DECLARATION";
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                return "PROCESSING_INSTRUCTION";
            case XMLStreamConstants.SPACE:
                return "SPACE";
            case XMLStreamConstants.START_DOCUMENT:
                return "START_DOCUMENT";
            case XMLStreamConstants.START_ELEMENT:
                return "START_ELEMENT";
            default:
                return String.valueOf(type); // should not happen...
        }
    }

    static boolean hasData(int type) {
        return type == XMLStreamConstants.CHARACTERS
                || type == XMLStreamConstants.COMMENT
                || type == XMLStreamConstants.CDATA
                || type == XMLStreamConstants.DTD
                || type == XMLStreamConstants.ENTITY_REFERENCE
                || type == XMLStreamConstants.SPACE;
    }

    private static final Location UNKNOWN_LOCATION = new Location() {
        @Override
        public int getCharacterOffset() {
            return -1;
        }

        @Override
        public int getColumnNumber() {
            return -1;
        }

        @Override
        public int getLineNumber() {
            return -1;
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public String getSystemId() {
            return null;
        }
    };

    private final Queue<Event> queue = new LinkedList<Event>();
    private final Location locationProvider;

    private XMLStreamReaderScope<T> scope;
    private boolean moreTokens;
    private Event event;
    private boolean startDocumentRead;

    private String encodingScheme;
    private String version;
    private Boolean standalone;

    /**
     * Create new reader instance.
     *
     * @param rootInfo root scope information
     */
    public AbstractXMLStreamReader(T rootInfo) {
        this(rootInfo, UNKNOWN_LOCATION);
    }

    /**
     * Create new reader instance.
     *
     * @param rootInfo root scope information
     */
    public AbstractXMLStreamReader(T rootInfo, Location locationProvider) {
        this.scope = new XMLStreamReaderScope<T>(XMLConstants.NULL_NS_URI, rootInfo);
        this.locationProvider = locationProvider;
    }

    private void ensureStartTagClosed() throws XMLStreamException {
        if (!scope.isStartTagClosed()) {
            scope.setStartTagClosed(true);
        }
    }

    /**
     * @return current scope
     */
    protected XMLStreamReaderScope<T> getScope() {
        return scope;
    }

    /**
     * @return <code>true</code> if <code>START_DOCUMENT</code> event has been read
     */
    protected boolean isStartDocumentRead() {
        return startDocumentRead;
    }

    /**
     * Consume initial event.
     * This method must be called by subclasses prior to any use of an instance (typically in constructor).
     *
     * @throws XMLStreamException
     */
    protected void initialize() throws XMLStreamException {
        try {
            moreTokens = consume();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }

        if (hasNext()) {
            event = queue.remove();
        } else {
            event = new Event(XMLStreamConstants.END_DOCUMENT, scope);
        }
    }

    /**
     * Main method to be implemented by subclasses.
     * This method is called by the reader when the event queue runs dry.
     * Consume some events and delegate to the various <code>readXXX()</code> methods.
     * When encountering an element start event, all attributes and namespace delarations
     * must be consumed too, otherwise these won't be available during start element.
     *
     * @return <code>true</code> if there's more to read
     * @throws XMLStreamException
     * @throws IOException
     */
    protected abstract boolean consume() throws XMLStreamException, IOException;

    /**
     * Read start document
     *
     * @param version        XML version
     * @param encodingScheme encoding scheme (may be <code>null</code>)
     * @param standalone     standalone flag (may be <code>null</code>)
     */
    protected void readStartDocument(String version, String encodingScheme, Boolean standalone) throws XMLStreamException {
        if (startDocumentRead || !scope.isRoot()) {
            throw new XMLStreamException("Cannot start document", locationProvider);
        }
        queue.add(new Event(XMLStreamConstants.START_DOCUMENT, scope));
        startDocumentRead = true;

        this.version = version;
        this.encodingScheme = encodingScheme;
        this.standalone = standalone;
    }

    /**
     * Read start element.
     * A new scope is created and made the current scope. The provided <code>scopeInfo</code> is
     * stored in the new scope and will be available via <code>getScope().getInfo()</code>.
     *
     * @param prefix       element prefix (use <code>null</code> if unknown)
     * @param localName    local name
     * @param namespaceURI (use <code>null</code> if unknown)
     * @param scopeInfo    new scope info
     * @throws XMLStreamException
     */
    protected void readStartElementTag(String prefix, String localName, String namespaceURI, T scopeInfo) throws XMLStreamException {
        if (prefix == null && namespaceURI == null) {
            throw new IllegalArgumentException("at least one of prefix and namespaceURI must not be null!");
        }
        ensureStartTagClosed();
        scope = new XMLStreamReaderScope<T>(scope, prefix, localName, namespaceURI);
        scope.setInfo(scopeInfo);
        queue.add(new Event(XMLStreamConstants.START_ELEMENT, scope));
    }

    /**
     * Read attribute.
     *
     * @param prefix       attribute prefix (use <code>null</code> if unknown)
     * @param localName    local name
     * @param namespaceURI (use <code>null</code> if unknown)
     * @param value        attribute value
     * @throws XMLStreamException
     */
    protected void readAttr(String prefix, String localName, String namespaceURI, String value) throws XMLStreamException {
        if (scope.isStartTagClosed()) {
            throw new XMLStreamException("Cannot read attribute: element has children or text", locationProvider);
        }
        if (prefix == null && namespaceURI == null) {
            throw new IllegalArgumentException("at least one of prefix and namespaceURI must not be null!");
        }
        scope.addAttribute(prefix, localName, namespaceURI, value);
    }

    /**
     * Read namespace declaration.
     *
     * @param prefix       namespace prefix (must not be <code>null</code>)
     * @param namespaceURI namespace URI (must not be <code>null</code>)
     * @throws XMLStreamException
     */
    protected void readNsDecl(String prefix, String namespaceURI) throws XMLStreamException {
        if (scope.isStartTagClosed()) {
            throw new XMLStreamException("Cannot read namespace: element has children or text", locationProvider);
        }
        if (prefix == null || namespaceURI == null) {
            throw new IllegalArgumentException("at least one of prefix and namespaceURI must not be null!");
        }
        scope.addNamespaceURI(prefix, namespaceURI);
        scope.setPrefix(prefix, namespaceURI);
    }

    /**
     * Read characters/comment/dtd/entity data.
     *
     * @param text text
     * @param data additional data exposed by {@link #getEventData()} (e.g. type conversion)
     * @param type one of <code>CHARACTERS, COMMENT, CDATA, DTD, ENTITY_REFERENCE, SPACE</code>
     * @throws XMLStreamException
     */
    protected void readData(String text, Object data, int type) throws XMLStreamException {
        if (hasData(type)) {
            ensureStartTagClosed();
            queue.add(new Event(type, scope, text, data));
        } else {
            throw new XMLStreamException("Unexpected event type " + getEventName(), locationProvider);
        }
    }

    /**
     * Read processing instruction.
     *
     * @param target PI target
     * @param data   PI data (may be <code>null</code>)
     * @throws XMLStreamException
     */
    protected void readPI(String target, String data) throws XMLStreamException {
        ensureStartTagClosed();
        String text = data == null ? target : target + ':' + data;
        queue.add(new Event(XMLStreamConstants.PROCESSING_INSTRUCTION, scope, text, null));
    }

    /**
     * Read end element.
     * This will pop the current scope and make its parent the new current scope.
     *
     * @throws XMLStreamException
     */
    protected void readEndElementTag() throws XMLStreamException {
        ensureStartTagClosed();
        queue.add(new Event(XMLStreamConstants.END_ELEMENT, scope));
        scope = scope.getParent();
    }

    /**
     * Read end document.
     */
    protected void readEndDocument() throws XMLStreamException {
        if (!startDocumentRead || !scope.isRoot()) {
            throw new XMLStreamException("Cannot end document", locationProvider);
        }
        queue.add(new Event(XMLStreamConstants.END_DOCUMENT, scope));
        startDocumentRead = false;
    }

    @Override
    public void require(int eventType, String namespaceURI, String localName) throws XMLStreamException {
        if (eventType != getEventType()) {
            throw new XMLStreamException("Expected event type " + getEventName(eventType) + ", was " + getEventName(getEventType()), getLocation());
        }
        if (namespaceURI != null && !namespaceURI.equals(getNamespaceURI())) {
            throw new XMLStreamException("Expected namespace " + namespaceURI + ", was " + getNamespaceURI(), getLocation());
        }
        if (localName != null && !localName.equals(getLocalName())) {
            throw new XMLStreamException("Expected local name " + localName + ", was " + getLocalName(), getLocation());
        }
    }

    @Override
    public String getElementText() throws XMLStreamException {
        require(XMLStreamConstants.START_ELEMENT, null, null);
        StringBuilder builder = null;
        String leadText = null;
        while (true) {
            switch (next()) {
                case XMLStreamConstants.CHARACTERS:
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.SPACE:
                case XMLStreamConstants.ENTITY_REFERENCE:
                    if (leadText == null) { // first event?
                        leadText = getText();
                    } else {
                        if (builder == null) { // second event?
                            builder = new StringBuilder(leadText);
                        }
                        builder.append(getText());
                    }
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                case XMLStreamConstants.COMMENT:
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    return builder == null ? leadText : builder.toString();
                default:
                    throw new XMLStreamException("Unexpected event type " + getEventName(), getLocation());
            }
        }
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        try {
            while (queue.isEmpty() && moreTokens) {
                moreTokens = consume();
            }
        } catch (IOException e) {
            throw new XMLStreamException(e.getMessage(), locationProvider, e);
        }
        return !queue.isEmpty();
    }

    @Override
    public int next() throws XMLStreamException {
        if (!hasNext()) {
            throw new IllegalStateException("No more events");
        }
        event = queue.remove();
        return event.getType();
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int eventType = next();
        while ((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace()) // skip whitespace
                || (eventType == XMLStreamConstants.CDATA && isWhiteSpace()) // skip whitespace
                || eventType == XMLStreamConstants.SPACE
                || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT) {
            eventType = next();
        }
        if (!isStartElement() && !isEndElement()) {
            throw new XMLStreamException("expected start or end tag", getLocation());
        }
        return eventType;
    }

    @Override
    public void close() throws XMLStreamException {
        scope = null;
        queue.clear();
    }

    @Override
    public boolean isStartElement() {
        return getEventType() == XMLStreamConstants.START_ELEMENT;
    }

    @Override
    public boolean isEndElement() {
        return getEventType() == XMLStreamConstants.END_ELEMENT;
    }

    @Override
    public boolean isCharacters() {
        return getEventType() == XMLStreamConstants.CHARACTERS;
    }

    @Override
    public boolean isWhiteSpace() {
        if (getEventType() == XMLStreamConstants.CHARACTERS || getEventType() == XMLStreamConstants.CDATA) {
            for (char ch : getText().toCharArray()) {
                if (!Character.isWhitespace(ch)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int getAttributeCount() {
        return event.getScope().getAttributeCount();
    }

    @Override
    public QName getAttributeName(int index) {
        return event.getScope().getAttributeName(index);
    }

    @Override
    public String getAttributeLocalName(int index) {
        return getAttributeName(index).getLocalPart();
    }

    @Override
    public String getAttributeValue(int index) {
        return event.getScope().getAttributeValue(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        return getAttributeName(index).getPrefix();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return getAttributeName(index).getNamespaceURI();
    }

    @Override
    public String getAttributeType(int index) {
        return null;
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return index < getAttributeCount();
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        return event.getScope().getAttributeValue(namespaceURI, localName);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return event.getScope().getNamespaceURI(prefix);
    }

    @Override
    public int getNamespaceCount() {
        return hasName() ? event.getScope().getNamespaceCount() : 0;
    }

    @Override
    public String getNamespacePrefix(int index) {
        return hasName() ? event.getScope().getNamespacePrefix(index) : null;
    }

    @Override
    public String getNamespaceURI(int index) {
        return hasName() ? event.getScope().getNamespaceURI(index) : null;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return event.getScope();
    }

    @Override
    public int getEventType() {
        return event.getType();
    }

    protected String getEventName() {
        return getEventName(getEventType());
    }

    /**
     * @return raw event data
     */
    protected final Object getEventData() {
        return event.getData();
    }

    @Override
    public Location getLocation() {
        return event.getLocation();
    }

    @Override
    public boolean hasText() {
        return hasData(getEventType());
    }

    @Override
    public String getText() {
        return hasText() ? event.getText() : null;
    }

    @Override
    public char[] getTextCharacters() {
        return hasText() ? event.getText().toCharArray() : null;
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        int count = Math.min(length, getTextLength());
        if (count > 0) {
            System.arraycopy(getTextCharacters(), sourceStart, target, targetStart, count);
        }
        return count;
    }

    @Override
    public int getTextStart() {
        return 0;
    }

    @Override
    public int getTextLength() {
        return hasText() ? event.getText().length() : 0;
    }

    @Override
    public boolean hasName() {
        return getEventType() == XMLStreamConstants.START_ELEMENT || getEventType() == XMLStreamConstants.END_ELEMENT;
    }

    @Override
    public QName getName() {
        return hasName() ? new QName(getNamespaceURI(), getLocalName(), getPrefix()) : null;
    }

    @Override
    public String getLocalName() {
        return hasName() ? event.getScope().getLocalName() : null;
    }

    @Override
    public String getNamespaceURI() {
        return hasName() ? event.getScope().getNamespaceURI() : null;
    }

    @Override
    public String getPrefix() {
        return hasName() ? event.getScope().getPrefix() : null;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getEncoding() {
        return null;
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
    public String getCharacterEncodingScheme() {
        return encodingScheme;
    }

    @Override
    public String getPITarget() {
        if (event.getType() != XMLStreamConstants.PROCESSING_INSTRUCTION) {
            return null;
        }
        int colon = event.getText().indexOf(':');
        return colon < 0 ? event.getText() : event.getText().substring(0, colon);
    }

    @Override
    public String getPIData() {
        if (event.getType() != XMLStreamConstants.PROCESSING_INSTRUCTION) {
            return null;
        }
        int colon = event.getText().indexOf(':');
        return colon < 0 ? null : event.getText().substring(colon + 1);
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new IllegalArgumentException("Unsupported property: " + name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getEventName() + ")";
    }
}
