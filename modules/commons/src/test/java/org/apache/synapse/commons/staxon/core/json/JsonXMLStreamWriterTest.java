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
package org.apache.synapse.commons.staxon.core.json;

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Assert;
import org.junit.Test;

public class JsonXMLStreamWriterTest {
    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testTextContent() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        //<code>&lt;alice&gt;bob&lt;/alice&gt;</code>
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":\"bob\"}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;david&gt;edgar&lt;/david&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testNested() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
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
        Assert.assertEquals("{\"alice\":{\"bob\":\"charlie\",\"david\":\"edgar\"}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;bob&gt;david&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testArray() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, "bob");
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
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;bob&gt;david&lt;/bob&gt;&lt;edgar/&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testArray2() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET);
        writer.writeStartElement("bob");
        writer.writeCharacters("charlie");
        writer.writeEndElement();
        writer.writeStartElement("bob");
        writer.writeCharacters("david");
        writer.writeEndElement();
        writer.writeEmptyElement("edgar");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[\"charlie\",\"david\"],\"edgar\":null}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testArray3() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, "bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[]}}", result.toString());
    }

    /**
     * <code>&lt;alice charlie="david"&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testAttributes() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeAttribute("charlie", "david");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"@charlie\":\"david\",\"$\":\"bob\"}}", result.toString());
    }

    /**
     * <code>&lt;alice xmlns="http://some-namespace"&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testNamespaces() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.setDefaultNamespace("http://some-namespace");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeDefaultNamespace("http://some-namespace");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"@xmlns\":\"http://some-namespace\",\"$\":\"bob\"}}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testRootArray() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, "alice");
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":[\"bob\",\"bob\"]}", result.toString());
    }

    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testRootArrayWithVirtualRoot() throws Exception {
        StringWriter result = new StringWriter();
        JsonXMLOutputFactory factory = new JsonXMLOutputFactory();
        factory.setProperty(JsonXMLOutputFactory.PROP_VIRTUAL_ROOT, new QName("alice"));
        XMLStreamWriter writer = factory.createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, "alice");
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("[\"bob\",\"bob\"]", result.toString());
    }

    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testDocumentArray() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("[{\"alice\":\"bob\"},{\"alice\":\"bob\"}]", result.toString());
    }

    @Test
    public void testSimpleValue() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeCharacters("bob");
        writer.close();
        Assert.assertEquals("\"bob\"", result.toString());
    }

    @Test
    public void testSimpleValueArray() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET);
        writer.writeCharacters("edgar");
        writer.writeCharacters("david");
        writer.close();
        Assert.assertEquals("[\"edgar\",\"david\"]", result.toString());
    }

    @Test
    public void testMixedValueArray() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET);
        writer.writeCharacters("edgar");
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("[\"edgar\",{\"alice\":\"bob\"}]", result.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testElementMultipleRoots() {
        try {
            XMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(new StringWriter());
            writer.writeStartDocument();
            writer.writeStartElement("foo");
            writer.writeEndElement();
            writer.writeStartElement("bar");
        } catch (XMLStreamException ex) {
            //empty
        }
    }

    @Test
    public void testNumber() throws Exception {
        StringWriter result = new StringWriter();
        JsonXMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeNumber(123.4);
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":123.4}", result.toString());
    }

    @Test
    public void testBoolean() throws Exception {
        StringWriter result = new StringWriter();
        JsonXMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeBoolean(true);
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":true}", result.toString());
    }

    @Test
    public void testNull() throws Exception {
        StringWriter result = new StringWriter();
        JsonXMLStreamWriter writer = new JsonXMLOutputFactory().createXMLStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
//		writer.writeCharacters(null); // writing null is not allowed
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":null}", result.toString());
    }
}
