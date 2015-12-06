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
package org.apache.synapse.commons.staxon.core.json.jaxb;

import java.io.StringWriter;

import junit.framework.Assert;

import org.junit.Test;

import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleRootElement;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleType;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleTypeWithNamespace;

public class JsonXMLMapperTest {
    @JsonXML
    static class JsonXMLDefault {
    }

    @Test
    public void testWriteXmlRootElement() throws Exception {
        JsonXMLMapper<SampleRootElement> mapper = new JsonXMLMapper<SampleRootElement>(SampleRootElement.class);
        StringWriter writer = new StringWriter();
        SampleRootElement value = new SampleRootElement();
        mapper.writeObject(writer, value);
        writer.close();
        Assert.assertEquals("{\"sampleRootElement\":null}", writer.toString());
    }

    @Test
    public void testWriteXmlType() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        JsonXMLMapper<SampleType> mapper = new JsonXMLMapper<SampleType>(SampleType.class, config);
        StringWriter writer = new StringWriter();
        SampleType value = new SampleType();
        mapper.writeObject(writer, value);
        writer.close();
        Assert.assertEquals("{\"sampleType\":null}", writer.toString());
    }

    @Test
    public void testWriteXmlTypeWithNamespace() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        JsonXMLMapper<SampleTypeWithNamespace> mapper =
                new JsonXMLMapper<SampleTypeWithNamespace>(SampleTypeWithNamespace.class, config);
        StringWriter writer = new StringWriter();
        SampleTypeWithNamespace value = new SampleTypeWithNamespace();
        mapper.writeObject(writer, value);
        writer.close();
        Assert.assertEquals("{\"ns2:sampleTypeWithNamespace\":{\"@xmlns:ns2\":\"urn:staxon:jaxb:test\"}}",
                writer.toString()); // TODO don't rely on prefix "ns2"
    }
}
