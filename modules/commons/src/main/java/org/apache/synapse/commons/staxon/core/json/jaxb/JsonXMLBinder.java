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
package org.apache.synapse.commons.staxon.core.json.jaxb;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.synapse.commons.staxon.core.json.JsonXMLConfig;
import org.apache.synapse.commons.staxon.core.json.JsonXMLConfigBuilder;
import org.apache.synapse.commons.staxon.core.json.JsonXMLInputFactory;
import org.apache.synapse.commons.staxon.core.json.JsonXMLOutputFactory;
import org.apache.synapse.commons.staxon.core.json.JsonXMLStreamConstants;
import org.apache.synapse.commons.staxon.core.json.util.XMLMultipleStreamWriter;

/**
 * Read/write instances of JAXB-annotated classes from/to JSON.
 */
public class JsonXMLBinder {
    private final JsonXMLRootProvider rootProvider;
    private final boolean writeDocumentArray;

    public JsonXMLBinder() {
        this(true);
    }

    protected JsonXMLBinder(boolean writeDocumentArray) {
        this(new JsonXMLRootProvider(), writeDocumentArray);
    }

    protected JsonXMLBinder(JsonXMLRootProvider rootProvider, boolean writeDocumentArray) {
        this.rootProvider = rootProvider;
        this.writeDocumentArray = writeDocumentArray;
    }

    private JsonXMLConfig toJsonXMLConfig(Class<?> type, JsonXML config) throws JAXBException {
        return new JsonXMLConfigBuilder().
                autoArray(config.autoArray()).
                autoPrimitive(config.autoPrimitive()).
                multiplePI(true).
                namespaceDeclarations(config.namespaceDeclarations()).
                namespaceSeparator(config.namespaceSeparator()).
                prettyPrint(config.prettyPrint()).
                virtualRoot(config.virtualRoot() ? rootProvider.getName(type) : null).
                build();
    }

    protected JsonXMLInputFactory createInputFactory(Class<?> type, JsonXML config) throws JAXBException {
        return new JsonXMLInputFactory(toJsonXMLConfig(type, config));
    }

    protected XMLStreamReader createXMLStreamReader(Class<?> type, JsonXML config, Reader stream) throws XMLStreamException, JAXBException {
        return createInputFactory(type, config).createXMLStreamReader(stream);
    }

    protected JsonXMLOutputFactory createOutputFactory(Class<?> type, JsonXML config) throws JAXBException {
        return new JsonXMLOutputFactory(toJsonXMLConfig(type, config));
    }

    protected XMLStreamWriter createXMLStreamWriter(Class<?> type, JsonXML config, Writer stream) throws XMLStreamException, JAXBException {
        XMLStreamWriter writer = createOutputFactory(type, config).createXMLStreamWriter(stream);
        if (config.multiplePaths().length > 0) {
            writer = new XMLMultipleStreamWriter(writer, !config.virtualRoot(), config.multiplePaths());
        }
        return writer;
    }

    public boolean isBindable(Class<?> type) {
        return type.isAnnotationPresent(XmlRootElement.class) || type.isAnnotationPresent(XmlType.class);
    }

    private void checkBindable(Class<?> type) throws JAXBException {
        if (!isBindable(type)) {
            throw new JAXBException("Cannot bind type: " + type.getName());
        }
    }

    protected <T> T unmarshal(Class<? extends T> type, JsonXML config, Unmarshaller unmarshaller, XMLStreamReader reader) throws JAXBException, XMLStreamException {
        if (type.isAnnotationPresent(XmlRootElement.class)) {
            return type.cast(unmarshaller.unmarshal(reader));
        } else if (type.isAnnotationPresent(XmlType.class)) {
            return unmarshaller.unmarshal(reader, type).getValue();
        } else { // good luck
            return type.cast(unmarshaller.unmarshal(reader, type));
        }
    }

    protected void marshal(Class<?> type, JsonXML config, Marshaller marshaller, XMLStreamWriter writer, Object value)
            throws JAXBException, XMLStreamException {
        Object element = null;
        if (type.isAnnotationPresent(XmlRootElement.class)) {
            element = value;
        } else if (type.isAnnotationPresent(XmlType.class)) {
            element = rootProvider.createElement(type, value);
            if (element == null) {
                throw new JAXBException("Cannot create JAXBElement");
            }
        } else { // good luck...
            element = value;
        }
        marshaller.marshal(element, writer);
    }

    public <T> T readObject(Class<? extends T> type, JsonXML config, JAXBContext context, Reader stream)
            throws XMLStreamException, JAXBException {
        checkBindable(type);
        XMLStreamReader reader = createXMLStreamReader(type, config, stream);
        T result;
        if (reader.isCharacters() && reader.getText() == null) { // hack: read null
            result = null;
        } else {
            reader.require(XMLStreamConstants.START_DOCUMENT, null, null);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            result = unmarshal(type, config, unmarshaller, reader);
            reader.require(XMLStreamConstants.END_DOCUMENT, null, null);
        }
        reader.close();
        return result;
    }

    public void writeObject(Class<?> type, JsonXML config, JAXBContext context, Writer stream, Object value)
            throws XMLStreamException, JAXBException {
        checkBindable(type);
        XMLStreamWriter writer = createXMLStreamWriter(type, config, stream);
        if (value == null) { // hack: write null
            writer.writeCharacters(null);
        } else {
            Marshaller marshaller = context.createMarshaller();
            marshal(type, config, marshaller, writer, value);
        }
        writer.close();
    }

    public <T> List<T> readArray(Class<? extends T> type, JsonXML config, JAXBContext context, Reader stream)
            throws XMLStreamException, JAXBException {
        checkBindable(type);
        XMLStreamReader reader = createXMLStreamReader(type, config, stream);
        List<T> result;
        if (reader.isCharacters() && reader.getText() == null) { // hack: read null
            result = null;
        } else {
            boolean documentArray = JsonXMLStreamConstants.MULTIPLE_PI_TARGET.equals(reader.getPITarget());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            while (reader.hasNext() && !reader.isStartElement() && !reader.isCharacters()) {
                reader.next();
            }
            result = new ArrayList<T>();
            while (reader.hasNext() || reader.isCharacters() && reader.getText() == null) {
                if (reader.isCharacters() && reader.getText() == null) { // hack: read null
                    result.add(null);
                    if (reader.hasNext()) {
                        reader.next();
                    } else {
                        break;
                    }
                } else {
                    result.add(unmarshal(type, config, unmarshaller, reader));
                    if (documentArray && reader.hasNext()) { // move to next document
                        reader.next();
                    }
                }
            }
        }
        reader.close();
        return result;
    }

    public void writeArray(Class<?> type, JsonXML config, JAXBContext context, Writer stream, Collection<?> collection)
            throws XMLStreamException, JAXBException {
        checkBindable(type);
        XMLStreamWriter writer = createXMLStreamWriter(type, config, stream);
        if (collection == null) { // hack: write null
            writer.writeCharacters(null);
        } else {
            Marshaller marshaller = context.createMarshaller();
            if (!writeDocumentArray) {
                writer.writeStartDocument();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            }
            writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET);
            for (Object value : collection) {
                if (value == null) { // hack: write null
                    writer.writeCharacters(null);
                } else {
                    marshal(type, config, marshaller, writer, value);
                }
            }
            if (!writeDocumentArray) {
                writer.writeEndDocument();
            }
        }
        writer.close();
    }
}
