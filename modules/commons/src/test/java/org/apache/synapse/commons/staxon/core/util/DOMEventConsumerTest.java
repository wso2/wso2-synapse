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
package org.apache.synapse.commons.staxon.core.util;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import junit.framework.Assert;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.synapse.commons.staxon.core.json.JsonXMLInputFactory;
import org.apache.synapse.commons.staxon.core.util.DOMEventConsumer;

public class DOMEventConsumerTest {
    @Test
    public void test1() throws XMLStreamException {
        StringReader json = new StringReader("{\"alice\":{\"@edgar\":\"david\",\"bob\":\"charlie\"}}");
        XMLEventReader reader = new JsonXMLInputFactory().createXMLEventReader(json);
        Document node = DOMEventConsumer.consume(reader);

        Node alice = node.getChildNodes().item(0);
        Assert.assertEquals("alice", alice.getLocalName());
        Assert.assertEquals("david", alice.getAttributes().getNamedItem("edgar").getNodeValue());

        Node bob = alice.getChildNodes().item(0);
        Assert.assertEquals("bob", bob.getLocalName());
        Assert.assertEquals("charlie", bob.getTextContent());
    }

    @Test
    public void test2() throws XMLStreamException {
        DocumentBuilder documentBuilder;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new XMLStreamException(e);
        }

        StringReader json = new StringReader("{\"alice\":{\"@edgar\":\"david\",\"bob\":\"charlie\"}}");
        XMLEventReader reader = new JsonXMLInputFactory().createXMLEventReader(json);
        Document document = documentBuilder.newDocument();
        Node node = document.createElement("foo");
        document.appendChild(node);
        DOMEventConsumer.consume(reader, node);

        Node alice = node.getChildNodes().item(0);
        Assert.assertEquals("alice", alice.getLocalName());
        Assert.assertEquals("david", alice.getAttributes().getNamedItem("edgar").getNodeValue());

        Node bob = alice.getChildNodes().item(0);
        Assert.assertEquals("bob", bob.getLocalName());
        Assert.assertEquals("charlie", bob.getTextContent());
    }

    @Test
    public void testXPath() throws XMLStreamException, XPathException {
        StringReader json = new StringReader("{\"alice\":{\"@edgar\":\"david\",\"bob\":\"charlie\"}}");
        XMLEventReader reader = new JsonXMLInputFactory().createXMLEventReader(json);
        Document document = DOMEventConsumer.consume(reader);

        XPath xpath = XPathFactory.newInstance().newXPath();
        Assert.assertEquals("charlie", xpath.evaluate("//alice[@edgar='david']/bob/text()", document));
    }
}
