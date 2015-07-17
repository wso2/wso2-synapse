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
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.synapse.commons.staxon.core.base.AbstractXMLStreamReader;
import org.apache.synapse.commons.staxon.core.base.XMLStreamReaderScope;

/**
 * Simple XML Stream Reader.
 * <p/>
 * The scope info is the element tag name.
 */
public class SimpleXMLStreamReader extends AbstractXMLStreamReader<String> {
    private final Reader reader;
    private int ch;

    public SimpleXMLStreamReader(Reader reader) throws XMLStreamException {
        super(null);
        this.reader = reader;
        try {
            nextChar();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
        initialize();
    }

    private void readAttrNsDecl(String name, String value) throws XMLStreamException {
        int separator = name.indexOf(':');
        if (separator < 0) {
            if (XMLConstants.XMLNS_ATTRIBUTE.equals(name)) {
                readNsDecl(XMLConstants.DEFAULT_NS_PREFIX, value);
            } else {
                readAttr(XMLConstants.DEFAULT_NS_PREFIX, name, null, value);
            }
        } else {
            if (name.startsWith(XMLConstants.XMLNS_ATTRIBUTE) && separator == XMLConstants.XMLNS_ATTRIBUTE.length()) {
                readNsDecl(name.substring(separator + 1), value);
            } else {
                readAttr(name.substring(0, separator), name.substring(separator + 1), null, value);
            }
        }
    }

    @Override
    protected boolean consume() throws XMLStreamException, IOException {
        XMLStreamReaderScope<String> scope = getScope();
        skipWhitespace();

        if (ch == -1) {
            if (isStartDocumentRead()) {
                readEndDocument();
            }
            return false;
        }

        if (ch == '<') {
            nextChar();
            if (ch == '/') { // END_ELEMENT
                nextChar();
                String tagName = readName('>');
                if (scope.getInfo().equals(tagName)) {
                    readEndElementTag();
                } else {
                    throw new XMLStreamException("not well-formed");
                }
            } else if (ch == '?') { // START_DOPCUMENT | PROCESSING_INSTRUCTION
                nextChar();
                String target = readName('?');
                String data = null;
                if (ch != '?') {
                    data = readText('?');
                }
                nextChar(); // please, let it be '>'
                if ("xml".equals(target)) {
                    String version = null;
                    String encoding = null;
                    Boolean standalone = null;
                    if (data != null) {
                        Matcher matcher = Pattern.compile("(\\w+)\\s*=\\s*\"(\\S+)\"").matcher(data);
                        while (matcher.find()) {
                            String name = matcher.group(1);
                            String value = matcher.group(2);
                            if ("version".equals(name)) {
                                if (!"1.0".equals(value) && !"1.1".equals(value)) {
                                    throw new XMLStreamException("Bad XML version: " + value);
                                }
                                version = value;
                            } else if ("encoding".equals(name)) {
                                encoding = value;
                            } else if ("standalone".equals(name)) {
                                if (!"yes".equals(value) && !"no".equals(value)) {
                                    throw new XMLStreamException("Bad XML version: " + value);
                                }
                                standalone = value.equals("yes");
                            } else {
                                throw new XMLStreamException("Bad xml XML declaration attribute: " + name);
                            }
                        }
                    }
                    readStartDocument(version, encoding, standalone);
                } else {
                    readPI(target, data);
                }
            } else if (ch == '!') { // COMMENT | CDATA
                nextChar();
                switch (ch) {
                    case '-': // COMMENT
                        String comment = readData('-');
                        if (!comment.startsWith("-")) {
                            throw new XMLStreamException("expected comment");
                        }
                        readData(comment.substring(1, comment.length() - 3), null, XMLStreamConstants.COMMENT);
                        break;
                    case '[': // CDATA
                        String cdata = readData(']');
                        if (!cdata.startsWith("CDATA[")) {
                            throw new XMLStreamException("expected cdata");
                        }
                        readData(cdata.substring(6, cdata.length() - 3), null, XMLStreamConstants.CDATA);
                        break;
                }
            } else { // START_ELEMENT
                String tagName = readName(' ');
                int colon = tagName.indexOf(':');
                if (colon < 0) {
                    readStartElementTag(XMLConstants.DEFAULT_NS_PREFIX, tagName, null, tagName);
                } else {
                    readStartElementTag(tagName.substring(0, colon), tagName.substring(colon + 1), null, tagName);
                }
                scope = getScope();
                while (ch != '>' && ch != '/') {
                    String name = readName('=');
                    nextChar();
                    skipWhitespace();
                    int quote = ch;
                    nextChar();
                    String value = readText(quote);
                    nextChar();
                    skipWhitespace();
                    readAttrNsDecl(name, value);
                }
                if (ch == '/') {
                    nextChar(); // please, let it be '>'
                    readEndElementTag();
                } else {
                    nextChar();
                    return consume();
                }
            }
            nextChar();
        } else {
            String text = readText('<');
            readData(text, null, XMLStreamConstants.CHARACTERS);
        }
        return true;
    }


    private void nextChar() throws IOException {
        ch = reader.read();
    }

    private void skipWhitespace() throws IOException {
        if (Character.isWhitespace(ch)) {
            do {
                nextChar();
            } while (Character.isWhitespace(ch));
        }
    }

    private String readData(final int end) throws IOException {
        StringBuilder data = new StringBuilder();
        int state = 0;
        do {
            nextChar();
            data.append((char) ch);
            if ((ch == end && state != 2) || (ch == '>' && state == 2)) {
                state++;
            } else {
                state = 0;
            }
        } while (state < 3);
        return data.toString();
    }

    private String readText(final int end) throws IOException {
        final StringBuilder builder = new StringBuilder();
        while (ch != end && ch >= 0) {
            if (ch == '&') {
                nextChar();
                String entity = readName(';');
                if ("lt".equals(entity)) {
                    builder.append('<');
                } else if ("gt".equals(entity)) {
                    builder.append('>');
                } else if ("amp".equals(entity)) {
                    builder.append('&');
                } else if ("quot".equals(entity)) {
                    builder.append('"');
                } else if ("apos".equals(entity)) {
                    builder.append('\'');
                } else {
                    builder.append('?');
                }
            } else {
                builder.append((char) ch);
            }
            nextChar();
        }
        return builder.toString();
    }

    private String readName(final int end) throws IOException {
        skipWhitespace();
        final StringBuilder builder = new StringBuilder();
        do {
            builder.append((char) ch);
            nextChar();
        } while (ch != end && ch != '>' && ch != '/' && !Character.isWhitespace(ch));
        skipWhitespace();
        return builder.toString();
    }
}
