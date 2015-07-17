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
package org.apache.synapse.commons.staxon.core.json.stream.util;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.stream.XMLStreamWriter;

import org.junit.Assert;
import org.junit.Test;

import org.apache.synapse.commons.staxon.core.json.JsonXMLStreamConstants;
import org.apache.synapse.commons.staxon.core.json.JsonXMLStreamWriter;
import org.apache.synapse.commons.staxon.core.json.stream.impl.JsonStreamFactoryImpl;

public class AutoPrimitiveTargetTest {
    private AutoPrimitiveTarget createTarget(StringWriter result) throws IOException {
        return new AutoPrimitiveTarget(new JsonStreamFactoryImpl().createJsonStreamTarget(result, false), false);
    }

    private JsonXMLStreamWriter createXmlStreamWriter(StringWriter result) throws IOException {
        return new JsonXMLStreamWriter(createTarget(result), false, true, ':', true);
    }

    /**
     * <code>&lt;alice&gt;bob&lt;/alice&gt;</code>
     */
    @Test
    public void testString() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = createXmlStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeCharacters("bob");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":\"bob\"}", result.toString());
    }

    /**
     * <code>&lt;alice edgar=\"999\" &gt;&lt;bob&gt;123&lt;/bob&gt;&lt;bob&gt;123.4&lt;/bob&gt;&lt;/alice&gt;</code>
     */
    @Test
    public void testNumber() throws Exception {
        StringWriter result = new StringWriter();
        XMLStreamWriter writer = createXmlStreamWriter(result);
        writer.writeStartDocument();
        writer.writeStartElement("alice");
        writer.writeAttribute("edgar", "999");
        writer.writeProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET);
        writer.writeStartElement("bob");
        writer.writeCharacters("123");
        writer.writeEndElement();
        writer.writeStartElement("bob");
        writer.writeCharacters("123.4");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
        Assert.assertEquals("{\"alice\":{\"@edgar\":\"999\",\"bob\":[123,123.4]}}", result.toString());
    }
}
