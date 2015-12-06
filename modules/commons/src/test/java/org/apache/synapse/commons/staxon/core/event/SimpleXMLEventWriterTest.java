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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.xml.SimpleXMLInputFactory;
import org.apache.synapse.commons.staxon.core.xml.SimpleXMLOutputFactory;

public class SimpleXMLEventWriterTest {
    private XMLEventReader createXmlEventReader(String xml) throws XMLStreamException {
        return new SimpleXMLInputFactory().createXMLEventReader(new StringReader(xml));
    }

    private XMLEventWriter createXmlEventWriter(StringWriter stringWriter) throws XMLStreamException {
        return new SimpleXMLOutputFactory().createXMLEventWriter(stringWriter);
    }

    @Test
    public void testStartEndDocument() throws XMLStreamException {
        StringWriter stringWriter = new StringWriter();
        String xml = "<?xml version=\"1.0\"?>";
        createXmlEventWriter(stringWriter).add(createXmlEventReader(xml));
        Assert.assertEquals(xml, stringWriter.toString());
    }

    @Test
    public void testStartEndElement() throws XMLStreamException {
        StringWriter stringWriter = new StringWriter();
        createXmlEventWriter(stringWriter).add(createXmlEventReader("<alice/>"));
        Assert.assertEquals("<alice></alice>", stringWriter.toString());

        stringWriter = new StringWriter();
        createXmlEventWriter(stringWriter).add(createXmlEventReader("<alice></alice>"));
        Assert.assertEquals("<alice></alice>", stringWriter.toString());
    }

    @Test
    public void testCharactersCData() throws XMLStreamException {
        StringWriter stringWriter = new StringWriter();
        createXmlEventWriter(stringWriter).add(createXmlEventReader("<alice>bob</alice>"));
        Assert.assertEquals("<alice>bob</alice>", stringWriter.toString());

        stringWriter = new StringWriter();
        createXmlEventWriter(stringWriter).add(createXmlEventReader("<alice><![CDATA[bob]]></alice>"));
        Assert.assertEquals("<alice><![CDATA[bob]]></alice>", stringWriter.toString());
    }

    @Test
    public void testAttributeNamespace() throws XMLStreamException {
        StringWriter stringWriter = new StringWriter();
        createXmlEventWriter(stringWriter).add(createXmlEventReader("<alice xmlns=\"http://foo\" david=\"edgar\"/>"));
        Assert.assertEquals("<alice xmlns=\"http://foo\" david=\"edgar\"></alice>", stringWriter.toString());
    }

    @Test
    public void testComment() throws XMLStreamException {
        StringWriter stringWriter = new StringWriter();
        createXmlEventWriter(stringWriter).add(createXmlEventReader("<!--james-->"));
        Assert.assertEquals("<!--james-->", stringWriter.toString());
    }

    @Test
    public void testProcessingInstruction() throws XMLStreamException {
        StringWriter stringWriter = new StringWriter();
        createXmlEventWriter(stringWriter).add(createXmlEventReader("<?joe?>"));
        Assert.assertEquals("<?joe?>", stringWriter.toString());
    }
}
