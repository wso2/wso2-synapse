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

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 * Abstract XML output factory.
 */
public abstract class AbstractXMLOutputFactory extends XMLOutputFactory {
    private boolean repairingNamespaces;

    @Override
    public XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException {
        if (result instanceof StreamResult) {
            StreamResult streamResult = (StreamResult) result;
            OutputStream output = streamResult.getOutputStream();
            if (output != null) {
                return createXMLStreamWriter(output);
            }
            Writer writer = streamResult.getWriter();
            if (writer != null) {
                return createXMLStreamWriter(writer);
            }
            if (result.getSystemId() != null) {
                throw new XMLStreamException("Cannot open system id as URL for writing: " + result.getSystemId());
            } else {
                throw new XMLStreamException("Invalid stream result: none of output, writer, systemId set");
            }
        }
        throw new XMLStreamException("Unsupported result type: " + result.getClass());
    }

    @Override
    public XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException {
        return createXMLEventWriter(createXMLStreamWriter(result));
    }

    @Override
    public XMLEventWriter createXMLEventWriter(OutputStream stream) throws XMLStreamException {
        return createXMLEventWriter(createXMLStreamWriter(stream));
    }

    @Override
    public XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding) throws XMLStreamException {
        return createXMLEventWriter(createXMLStreamWriter(stream, encoding));
    }

    @Override
    public XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException {
        return createXMLEventWriter(createXMLStreamWriter(stream));
    }

    public abstract XMLEventWriter createXMLEventWriter(XMLStreamWriter writer) throws XMLStreamException;

    @Override
    public boolean isPropertySupported(String name) {
        if (XMLOutputFactory.IS_REPAIRING_NAMESPACES.equals(name)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        if (IS_REPAIRING_NAMESPACES.equals(name)) {
            return Boolean.valueOf(repairingNamespaces);
        } else {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }
    }

    @Override
    public void setProperty(String name, Object value) throws IllegalArgumentException {
        if (IS_REPAIRING_NAMESPACES.equals(name)) {
            repairingNamespaces = ((Boolean) value).booleanValue();
        } else {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }
    }
}
