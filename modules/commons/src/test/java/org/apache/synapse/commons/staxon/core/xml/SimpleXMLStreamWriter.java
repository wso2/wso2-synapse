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
package org.apache.synapse.commons.staxon.core.xml;

import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.synapse.commons.staxon.core.base.AbstractXMLStreamWriter;

/**
 * Simple XML Stream Writer.
 * <p/>
 * The scope info is the element tag name.
 */
public class SimpleXMLStreamWriter extends AbstractXMLStreamWriter<String> {
    private final Writer writer;

    public SimpleXMLStreamWriter(Writer writer, boolean repairNamespaces) {
        super(null, repairNamespaces);
        this.writer = writer;
    }

    private void writeEscaped(String string, boolean attribute) throws IOException {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
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
                    if (c == '"' && attribute) {
                        writer.write("&quot;");
                    } else {
                        writer.write(c);
                    }
            }
        }
    }

    private String getTagName(String prefix, String localName) {
        return XMLConstants.DEFAULT_NS_PREFIX.equals(prefix) ? localName : prefix + ':' + localName;
    }

    @Override
    public void flush() throws XMLStreamException {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    protected String writeStartElementTag(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        if (getScope().isRoot() && getScope().getLastChild() != null) {
            throw new XMLStreamException("Multiple roots within document");
        }
        String tagName = getTagName(prefix, localName);
        try {
            writer.write('<');
            writer.write(tagName);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
        return tagName;
    }

    @Override
    protected void writeStartElementTagEnd() throws XMLStreamException {
        try {
            if (getScope().isEmptyElement()) {
                writer.write('/');
            }
            writer.write('>');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    protected void writeEndElementTag() throws XMLStreamException {
        try {
            writer.write('<');
            writer.write('/');
            writer.write(getScope().getInfo());
            writer.write('>');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    protected void writeAttr(String prefix, String localName, String namespaceURI, String value) throws XMLStreamException {
        try {
            writer.write(' ');
            if (!XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                writer.write(prefix);
                writer.write(':');
            }
            writer.write(localName);
            writer.write('=');
            writer.write('"');
            writeEscaped(value, true);
            writer.write('"');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    protected void writeNsDecl(String prefix, String namespaceURI) throws XMLStreamException {
        try {
            writer.write(' ');
            writer.write(XMLConstants.XMLNS_ATTRIBUTE);
            if (!XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                writer.write(':');
                writer.write(prefix);
            }
            writer.write('=');
            writer.write('"');
            writer.write(namespaceURI);
            writer.write('"');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    protected void writeData(Object data, int type) throws XMLStreamException {
        try {
            switch (type) {
                case XMLStreamConstants.CHARACTERS:
                    writeEscaped(data.toString(), false);
                    break;
                case XMLStreamConstants.CDATA:
                    writer.write("<![CDATA[");
                    writer.write(data.toString());
                    writer.write("]]>");
                    break;
                case XMLStreamConstants.COMMENT:
                    writer.write("<!--");
                    writeEscaped(data.toString(), false);
                    writer.write("-->");
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    writer.write('&');
                    writer.write(data.toString());
                    writer.write(';');
                    break;
                default:
                    throw new UnsupportedOperationException("Cannot write data of type " + type);
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    protected void writePI(String target, String data) throws XMLStreamException {
        try {
            writer.write("<?");
            writer.write(target);
            if (data != null) {
                writer.write(' ');
                writer.write(data);
            }
            writer.write("?>");
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

}