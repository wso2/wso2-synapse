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

import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Assert;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.xml.SimpleXMLStreamWriter;

public class SimpleXMLStreamWriterTest {
    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testTextContent() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice>bob</alice>", result.toString());
    }

    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testCooment() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeComment("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice><!--bob--></alice>", result.toString());
    }


    /**
     * <code>&lt;alice&gt;&amp;lt;&amp;gt;&amp;amp;"'&lt;/alice&gt;</code>
     */
    @Test
    public void testEscapeCharacters() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeCharacters("<>&\"'");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice>&lt;&gt;&amp;\"'</alice>", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;![CDATA[&lt;&gt;&amp;"']]&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testEscapeCData() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeCData("<>&\"'");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice><![CDATA[<>&\"']]></alice>", result.toString());
    }

    /**
     * <code>&lt;alice escape="&amp;lt;&amp;gt;&amp;amp;&amp;quot;'"&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testEscapeAttribute() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeAttribute("escape", "<>&\"'");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice escape=\"&lt;&gt;&amp;&quot;'\"></alice>", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;david&gt;edgar&lt;/david&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testNested() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeStartElement("bob");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeStartElement("david");
        writer.writeCharacters("edgar");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice><bob>charlie</bob><david>edgar</david></alice>", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;bob&gt;david&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testArray() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
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
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice><bob>charlie</bob><bob>david</bob></alice>", result.toString());
    }

    /**
     * <code>&lt;alice charlie="david"&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testAttributes() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeAttribute("charlie", "david");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice charlie=\"david\">bob</alice>", result.toString());
    }

    /**
     * <code>&lt;alice xmlns="http://some-namespace"&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testNamespaces() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.setDefaultNamespace("http://some-namespace");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeDefaultNamespace("http://some-namespace");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice xmlns=\"http://some-namespace\">bob</alice>", result.toString());
    }

    /**
     * <code>&lt;alice xmlns="http://some-namespace"&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testRepairNamespaces() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, true);
        writer.writeStartDocument();
        writer.writeStartElement("http://some-namespace", "alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice xmlns=\"http://some-namespace\">bob</alice>", result.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteElementMultipleRoots() {
        try {
            XMLStreamWriter writer = new SimpleXMLStreamWriter(new StringWriter(), false);
            writer.writeStartElement("foo");
            writer.writeEndElement();
            writer.writeStartElement("foo");
        } catch (XMLStreamException ex) {
            //empty
        }
    }

    /**
     * <code>&lt;alice&gt;&lt;bar:bob xmlns:bar="http://bar" jane="dolly"/&gt;hello&lt;/alice&gt;</code>
     */
    @Test
    public void testOther() throws XMLStreamException {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new SimpleXMLStreamWriter(result, false);
        writer.setDefaultNamespace("http://foo");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeEmptyElement("bar", "bob", "http://bar");
        writer.writeNamespace("bar", "http://bar");
        writer.writeAttribute("jane", "dolly");
        writer.writeCharacters("hello");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><alice><bar:bob xmlns:bar=\"http://bar\" jane=\"dolly\"/>hello</alice>", result.toString());
    }
}
