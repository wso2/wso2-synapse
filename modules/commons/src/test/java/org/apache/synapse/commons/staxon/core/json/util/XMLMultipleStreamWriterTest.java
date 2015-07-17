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

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Assert;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.json.JsonXMLOutputFactory;

public class XMLMultipleStreamWriterTest {
    XMLStreamWriter createStreamWriter(Writer writer) throws XMLStreamException {
        return new JsonXMLOutputFactory().createXMLStreamWriter(writer);
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testWriteStartElement_String() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeStartElement("bob");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[\"charlie\"]}}", result.toString());
    }

    /**
     * <code>&lt;alice xmlns:p="http://test"&gt;&lt;p:bob&gt;charlie&lt;/p:bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testWriteStartElement_String_String() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeNamespace("p", "http://test");
        writer.writeStartElement("http://test", "bob");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"@xmlns:p\":\"http://test\",\"p:bob\":[\"charlie\"]}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;p:bob xmlns:p="http://test"&gt;charlie&lt;/p:bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testWriteStartElement_String_String_String() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeStartElement("p", "bob", "http://test");
        writer.writeNamespace("p", "http://test");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"p:bob\":[{\"@xmlns:p\":\"http://test\",\"$\":\"charlie\"}]}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob/&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testWriteEmptyElement_String() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeEmptyElement("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[null]}}", result.toString());
    }

    /**
     * <code>&lt;alice xmlns:p="http://test"&gt;&lt;p:bob/&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testWriteEmptyElement_String_String() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeNamespace("p", "http://test");
        writer.writeEmptyElement("http://test", "bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"@xmlns:p\":\"http://test\",\"p:bob\":[null]}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;p:bob xmlns:p="http://test"/&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testWriteEmptyElement_String_String_String() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeEmptyElement("p", "bob", "http://test");
        writer.writeNamespace("p", "http://test");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"p:bob\":[{\"@xmlns:p\":\"http://test\"}]}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;bob&gt;david&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testArrayWithTwoElements() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeStartElement("bob");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeStartElement("bob");
        writer.writeCharacters("david");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[\"charlie\",\"david\"]}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;bob&gt;david&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testArrayWithPreviousSibling() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeEmptyElement("edgar");
        writer.writeStartElement("bob");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"edgar\":null,\"bob\":[\"charlie\"]}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;edgar&gt;&lt;bob/&gt;&lt;/edgar&gt;&lt;edgar&gt;&lt;bob/&gt;&lt;/edgar&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testZombieArray() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "/alice/edgar/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeStartElement("edgar");
        writer.writeEmptyElement("bob");
        writer.writeEndElement();
        writer.writeStartElement("edgar");
        writer.writeEmptyElement("bob");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"edgar\":{\"bob\":[null]},\"edgar\":{\"bob\":[null]}}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testMatchRootFalse() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), false, "/bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeStartElement("bob");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[\"charlie\"]}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testMatchRelative() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new XMLMultipleStreamWriter(createStreamWriter(result), true, "bob");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeStartElement("bob");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[\"charlie\"]}}", result.toString());
    }
}
