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
package org.apache.synapse.commons.staxon.core.json.util;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.synapse.commons.staxon.core.util.EventWriterDelegate;

/**
 * <p>Simple delegate writer which generates <code>&lt;?xml-multiple?></code>
 * processing instructions when writing a sequence of elements matching some
 * element path. Use this class to trigger JSON array start events like this:
 * <pre>
 * XMLOutputFactory factory = new JsonXMLOutputFactory();
 * XMLEventWriter writer = factory.createXMLEventWriter(...);
 * writer = new XMLMultipleEventWriter(writer, false, "/alice/bob", ...);
 * </pre>
 */
public class XMLMultipleEventWriter extends EventWriterDelegate {
    private final XMLMultipleProcessingInstructionHandler handler;

    /**
     * Create instance.
     *
     * @param parent        delegate
     * @param matchRoot     whether the root element is included in paths
     * @param multiplePaths added via {@link #addMultiplePath(String)}
     */
    public XMLMultipleEventWriter(XMLEventWriter parent, boolean matchRoot, String... multiplePaths) throws XMLStreamException {
        super(parent);

        this.handler = new XMLMultipleProcessingInstructionHandler(this, matchRoot, false);
        for (String path : multiplePaths) {
            addMultiplePath(path);
        }
    }

    @Override
    public void setParent(XMLEventWriter parent) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add path to trigger <code>&lt;?xml-multiple?></code> PI.
     * The path may start with <code>'/'</code> and contain local element
     * names, separated by <code>'/'</code>, e.g
     * <code>"/foo/bar"</code>, <code>"foo/bar"</code> or <code>"bar"</code>.
     *
     * @param path multiple path
     * @throws XMLStreamException if the path is invalid
     */
    public void addMultiplePath(String path) throws XMLStreamException {
        handler.addMultiplePath(path);
    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            add(reader.nextEvent());
        }
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        switch (event.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                QName name = event.asStartElement().getName();
                handler.preStartElement(name.getPrefix(), name.getLocalPart());
                super.add(event);
                handler.postStartElement();
                break;
            case XMLStreamConstants.END_ELEMENT:
                handler.preEndElement();
                super.add(event);
                handler.postEndElement();
                break;
            default:
                super.add(event);
        }
    }
}
