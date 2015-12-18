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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

/**
 * Simple implementation of {@link XMLEventReader}.
 */
public class SimpleXMLEventReader implements XMLEventReader {
    private final XMLEventAllocator allocator;
    private final XMLStreamReader delegate;

    private int currentEventType = -1;
    private XMLEvent peekedEvent = null;

    public SimpleXMLEventReader(XMLStreamReader delegate) {
        this(delegate, new SimpleXMLEventAllocator());
    }

    public SimpleXMLEventReader(XMLStreamReader delegate, XMLEventAllocator allocator) {
        this.delegate = delegate;
        this.allocator = allocator;
    }

    protected XMLEvent allocate() throws XMLStreamException {
        return allocator.allocate(delegate);
    }

    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        if (currentEventType != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("Expected start element event");
        }

        if (peekedEvent == null) {
            String result = delegate.getElementText();
            currentEventType = delegate.getEventType();
            if (currentEventType != XMLStreamConstants.END_ELEMENT) {
                throw new XMLStreamException("Expected end element event");
            }
            return result;
        }

        currentEventType = peekedEvent.getEventType();
        assert currentEventType == delegate.getEventType();
        peekedEvent = null;

        String leadText = null;
        switch (currentEventType) {
            case XMLStreamConstants.CDATA:
            case XMLStreamConstants.CHARACTERS:
            case XMLStreamConstants.ENTITY_REFERENCE:
                leadText = delegate.getText();
                break;
            case XMLStreamConstants.COMMENT:
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
            case XMLStreamConstants.SPACE:
                break;
            case XMLStreamConstants.END_ELEMENT:
                return "";
            default:
                throw new XMLStreamException("Unexpected event type " + currentEventType, delegate.getLocation());
        }

        StringBuilder builder = null;
        while (true) {
            currentEventType = delegate.next();
            switch (currentEventType) {
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.CHARACTERS:
                case XMLStreamConstants.ENTITY_REFERENCE:
                    if (leadText == null) { // first event?
                        leadText = delegate.getText();
                    } else {
                        if (builder == null) { // second event?
                            builder = new StringBuilder(leadText);
                        }
                        builder.append(delegate.getText());
                    }
                    break;
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                case XMLStreamConstants.SPACE:
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    return builder == null ? (leadText == null ? "" : leadText) : builder.toString();
                default:
                    throw new XMLStreamException("Unexpected event type " + currentEventType, delegate.getLocation());
            }
        }
    }

    @Override
    public Object getProperty(String name) {
        return delegate.getProperty(name);
    }


    @Override
    public boolean hasNext() {
        try {
            return peek() != null;
        } catch (XMLStreamException e) {
            throw new RuntimeException("Cannot determine next state", e);
        }
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        XMLEvent currentEvent = peek();
        if (currentEvent == null) {
            throw new XMLStreamException("no more events");
        }
        currentEventType = currentEvent.getEventType();
        peekedEvent = null;
        return currentEvent;
    }

    @Override
    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        if (peekedEvent == null) {
            currentEventType = delegate.nextTag();
            if (currentEventType != XMLStreamConstants.START_ELEMENT && currentEventType != XMLStreamConstants.END_ELEMENT) {
                throw new XMLStreamException("Expected start element event or end element event");

            }
            return allocate();
        }

        currentEventType = peekedEvent.getEventType();
        assert currentEventType == delegate.getEventType();
        XMLEvent event = peekedEvent;
        peekedEvent = null;

        switch (event.getEventType()) {
            case XMLStreamConstants.START_DOCUMENT:
                break;
            case XMLStreamConstants.COMMENT:
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
            case XMLStreamConstants.SPACE:
                break;
            case XMLStreamConstants.CDATA:
            case XMLStreamConstants.CHARACTERS:
                if (!event.asCharacters().isWhiteSpace()) {
                    throw new XMLStreamException("Encountered non-whitespace text");
                }
                break;
            case XMLStreamConstants.START_ELEMENT:
            case XMLStreamConstants.END_ELEMENT:
                return event;
            default:
                throw new XMLStreamException("Encountered unexpected event: " + event.getEventType());
        }

        while (true) {
            currentEventType = delegate.next();
            switch (currentEventType) {
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                case XMLStreamConstants.SPACE:
                    continue;
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.CHARACTERS:
                    if (!delegate.isWhiteSpace()) {
                        throw new XMLStreamException("Encountered non-whitespace text");
                    }
                    continue;
                case XMLStreamConstants.START_ELEMENT:
                case XMLStreamConstants.END_ELEMENT:
                    return allocate();
                default:
                    throw new XMLStreamException("Encountered unexpected event: " + delegate.getEventType());
            }
        }
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        if (peekedEvent == null) {
            if (currentEventType < 0) { // first event
                peekedEvent = allocate();
            } else if (delegate.hasNext()) {
                delegate.next();
                peekedEvent = allocate();
            }
        }
        return peekedEvent;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove events");
    }
}
