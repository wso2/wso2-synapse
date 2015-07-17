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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.json.JsonXMLOutputFactory;

public class XMLMultipleEventWriterTest {
    @Test
    public void test1() throws XMLStreamException {
        StringReader input = new StringReader("<alice><bob>charlie</bob></alice>");
        StringWriter output = new StringWriter();
        XMLEventReader reader = XMLInputFactory.newFactory().createXMLEventReader(input);
        XMLOutputFactory factory = new JsonXMLOutputFactory();
        factory.setProperty(JsonXMLOutputFactory.PROP_MULTIPLE_PI, true);
        XMLEventWriter writer = factory.createXMLEventWriter(output);
        writer = new XMLMultipleEventWriter(writer, true, "/alice/bob");
        writer.add(reader);
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[\"charlie\"]}}", output.toString());
    }

    @Test
    public void test2() throws XMLStreamException {
        StringReader input = new StringReader("<alice><bob>charlie</bob></alice>");
        StringWriter output = new StringWriter();
        XMLEventReader reader = XMLInputFactory.newFactory().createXMLEventReader(input);
        XMLOutputFactory factory = new JsonXMLOutputFactory();
        factory.setProperty(JsonXMLOutputFactory.PROP_MULTIPLE_PI, true);
        XMLEventWriter writer = factory.createXMLEventWriter(output);
        writer = new XMLMultipleEventWriter(writer, false, "/bob");
        writer.add(reader);
        writer.close();
        Assert.assertEquals("{\"alice\":{\"bob\":[\"charlie\"]}}", output.toString());
    }
}
