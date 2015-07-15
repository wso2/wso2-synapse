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

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.util.StreamWriterDelegate;
import org.apache.synapse.commons.staxon.core.xml.SimpleXMLStreamWriter;

public class AbstractXMLStreamWriterTest {
    boolean jdkStreamWriter = false;

    XMLStreamWriter createXMLStreamWriter(boolean repairNamespaces) throws XMLStreamException {
        final StringWriter writer = new StringWriter();
        if (jdkStreamWriter) {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, repairNamespaces);
            return new StreamWriterDelegate(factory.createXMLStreamWriter(writer)) {
                @Override
                public String toString() {
                    return writer.toString();
                }
            };
        } else {
            return new SimpleXMLStreamWriter(writer, repairNamespaces) {
                @Override
                public String toString() {
                    return writer.toString();
                }
            };
        }
    }

    @Test
    public void testWriteAttribute0() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeAttribute("bar", "foobar");
        writer.flush();
        Assert.assertEquals("<foo bar=\"foobar\"", writer.toString());
    }

    @Test
    @Ignore
    public void testWriteAttribute0a() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeAttribute("bar", "<>'\"&");
        writer.flush();
        Assert.assertEquals("<foo bar=\"&lt;&gt;'&quot;&amp;\"", writer.toString());
    }

    @Test
    public void testWriteAttribute1a() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("foo");
        writer.writeAttribute("http://p", "bar", "foobar");
        writer.flush();
        Assert.assertEquals("<foo p:bar=\"foobar\"", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteAttribute1b() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeAttribute("http://p", "bar", "foobar");
    }

    @Test
    public void testWriteAttribute2a() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("foo");
        writer.writeAttribute("p", "http://p", "bar", "foobar");
        writer.flush();
        Assert.assertEquals("<foo p:bar=\"foobar\"", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteAttribute2b() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeAttribute("p", "http://p", "bar", "foobar");
        writer.writeEndElement();
    }

    @Test
    public void testWriteAttribute2c() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeAttribute("p", "http://p", "bar", "foobar");
        writer.writeNamespace("p", "http://p");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo p:bar=\"foobar\" xmlns:p=\"http://p\"></foo>", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteAttribute2d() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("foo");
        writer.writeAttribute("pp", "http://p", "bar", "foobar");
        writer.writeEndElement();
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteAttribute2e() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("foo");
        writer.writeAttribute("p", "http://pp", "bar", "foobar");
        writer.writeEndElement();
    }

    @Test
    public void testWriteElement0() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo></foo>", writer.toString());
    }

    @Test
    public void testWriteElement1a() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("http://p", "foo");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<p:foo></p:foo>", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteElement1b() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("http://p", "foo");
    }

    @Test
    public void testWriteElement1bRepaired() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(true);
        writer.writeStartElement("http://p", "foo");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo xmlns=\"http://p\"></foo>", writer.toString());
    }

    @Test
    public void testWriteElement2a() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("p", "foo", "http://p");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<p:foo></p:foo>", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteElement2b() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("pp", "foo", "http://p");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo xmlns=\"http://p\"></foo>", writer.toString());
    }

    @Test
    public void testWriteElement2bRepaired() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(true);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("pp", "foo", "http://p");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<pp:foo xmlns:pp=\"http://p\"></pp:foo>", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteElement2c() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("p", "foo", "http://p");
        writer.writeEndElement();
    }

    @Test
    public void testWriteElement2cRepaired() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(true);
        writer.writeStartElement("p", "foo", "http://p");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<p:foo xmlns:p=\"http://p\"></p:foo>", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteElement2d() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("p", "foo", "http://pp");
        writer.writeEndElement();
        writer.flush();
    }

    @Test
    public void testWriteElement2dRepaired() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(true);
        writer.setPrefix("p", "http://p");
        writer.writeStartElement("p", "foo", "http://pp");
        writer.writeEndElement();
        writer.flush();
    }

    @Test
    public void testWriteNamespaceAddsPrefixBinding() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("p", "foo", "http://p");
        writer.writeNamespace("p", "http://p");
        Assert.assertEquals("p", writer.getPrefix("http://p"));
        Assert.assertEquals("http://p", writer.getNamespaceContext().getNamespaceURI("p"));
        Assert.assertEquals("p", writer.getNamespaceContext().getPrefix("http://p"));
    }

    @Test
    public void testWriteEmptyElement0() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeEmptyElement("foo");
        writer.flush();
        Assert.assertEquals("<foo", writer.toString());
    }

    @Test
    public void testWriteEmptyElement1a() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeEmptyElement("http://p", "foo");
        writer.flush();
        Assert.assertEquals("<p:foo", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteEmptyElement1b() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeEmptyElement("http://p", "foo");
    }

    @Test
    public void testWriteEmptyElement1bRepaired() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(true);
        writer.writeEmptyElement("http://p", "foo");
        writer.flush();
        Assert.assertEquals("<foo xmlns=\"http://p\"", writer.toString());
    }

    @Test
    public void testWriteEmptyElement2a() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeEmptyElement("p", "foo", "http://p");
        writer.flush();
        Assert.assertEquals("<p:foo", writer.toString());
    }

    @Test
    public void testWriteEmptyElement2b() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.setPrefix("p", "http://p");
        writer.writeEmptyElement("pp", "foo", "http://p");
//		Assert.fail("expected exception: bound to another prefix"); // according to XMLStreamWriter javadoc
        writer.flush();
        Assert.assertEquals("<pp:foo", writer.toString()); // according to implementations
    }

    @Test
    public void testWriteEmptyElement2c() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeEmptyElement("p", "foo", "http://p");
        writer.flush();
        Assert.assertEquals("<p:foo", writer.toString());
    }

    @Test(expected = XMLStreamException.class)
    public void testWriteElementMultipleRoots() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeEndElement();
        writer.writeStartElement("foo");
    }

    @Test
    public void testWriterCharacters() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeCharacters("bar");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo>bar</foo>", writer.toString());
    }

    @Test
    @Ignore
    public void testWriterCharacters2() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeCharacters("<>'\"&");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo>&lt;&gt;'\"&amp;</foo>", writer.toString());
    }

    @Test
    public void testWriteCDtata() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeCData("bar");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo><![CDATA[bar]]></foo>", writer.toString());
    }

    @Test
    public void testWriteCDtata2() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeCData("<>'\"&");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo><![CDATA[<>'\"&]]></foo>", writer.toString());
    }

    @Test
    public void testWriteComment() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeComment("bar");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo><!--bar--></foo>", writer.toString());
    }


    @Test
    public void testWriteEntityRef() throws XMLStreamException {
        XMLStreamWriter writer = createXMLStreamWriter(false);
        writer.writeStartElement("foo");
        writer.writeEntityRef("bar");
        writer.writeEndElement();
        writer.flush();
        Assert.assertEquals("<foo>&bar;</foo>", writer.toString());
    }
}
