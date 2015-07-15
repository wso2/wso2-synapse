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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.Assert;

import org.junit.Test;

import org.apache.synapse.commons.staxon.core.json.JsonXMLInputFactory;
import org.apache.synapse.commons.staxon.core.json.JsonXMLOutputFactory;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleRootElement;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleType;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleTypeWithNamespace;

public class JsonXMLBinderTest {
    @JsonXML
    static class JsonXMLDefault {
    }

    @JsonXML(autoArray = true, namespaceDeclarations = false, namespaceSeparator = '_', prettyPrint = true, virtualRoot = true)
    static class JsonXMLCustom {
    }

    @JsonXML(virtualRoot = true, multiplePaths = "/elements")
    static class JsonXMLVirtualSampleRootElement {
    }

    @XmlType
    static class EmptyType {
    }

    @Test
    public void testIsBindable() {
        Assert.assertTrue(new JsonXMLBinder().isBindable(SampleRootElement.class));
        Assert.assertTrue(new JsonXMLBinder().isBindable(SampleType.class));
        Assert.assertFalse(new JsonXMLBinder().isBindable(getClass()));
    }

    @Test
    public void testCreateInputFactory() throws JAXBException {
        JsonXMLInputFactory factory = new JsonXMLBinder().createInputFactory(SampleRootElement.class, JsonXMLDefault.class.getAnnotation(JsonXML.class));
        Assert.assertEquals(Boolean.TRUE, factory.getProperty(JsonXMLInputFactory.PROP_MULTIPLE_PI));
        Assert.assertEquals(Character.valueOf(':'), factory.getProperty(JsonXMLInputFactory.PROP_NAMESPACE_SEPARATOR));
        Assert.assertNull(factory.getProperty(JsonXMLInputFactory.PROP_VIRTUAL_ROOT));

        factory = new JsonXMLBinder().createInputFactory(SampleRootElement.class, JsonXMLCustom.class.getAnnotation(JsonXML.class));
        Assert.assertEquals(Boolean.TRUE, factory.getProperty(JsonXMLInputFactory.PROP_MULTIPLE_PI));
        Assert.assertEquals(Character.valueOf('_'), factory.getProperty(JsonXMLInputFactory.PROP_NAMESPACE_SEPARATOR));
        Assert.assertEquals(new QName("sampleRootElement"), factory.getProperty(JsonXMLInputFactory.PROP_VIRTUAL_ROOT));
    }

    @Test
    public void testCreateOutputFactory() throws JAXBException {
        JsonXMLOutputFactory factory = new JsonXMLBinder().createOutputFactory(SampleRootElement.class, JsonXMLDefault.class.getAnnotation(JsonXML.class));
        Assert.assertEquals(Boolean.TRUE, factory.getProperty(JsonXMLOutputFactory.PROP_MULTIPLE_PI));
        Assert.assertEquals(Character.valueOf(':'), factory.getProperty(JsonXMLOutputFactory.PROP_NAMESPACE_SEPARATOR));
        Assert.assertNull(factory.getProperty(JsonXMLOutputFactory.PROP_VIRTUAL_ROOT));
        Assert.assertEquals(Boolean.TRUE, factory.getProperty(JsonXMLOutputFactory.PROP_NAMESPACE_DECLARATIONS));
        Assert.assertEquals(Boolean.FALSE, factory.getProperty(JsonXMLOutputFactory.PROP_PRETTY_PRINT));
        Assert.assertEquals(Boolean.FALSE, factory.getProperty(JsonXMLOutputFactory.PROP_AUTO_ARRAY));

        factory = new JsonXMLBinder().createOutputFactory(SampleRootElement.class, JsonXMLCustom.class.getAnnotation(JsonXML.class));
        Assert.assertEquals(Boolean.TRUE, factory.getProperty(JsonXMLOutputFactory.PROP_MULTIPLE_PI));
        Assert.assertEquals(Character.valueOf('_'), factory.getProperty(JsonXMLOutputFactory.PROP_NAMESPACE_SEPARATOR));
        Assert.assertEquals(new QName("sampleRootElement"), factory.getProperty(JsonXMLOutputFactory.PROP_VIRTUAL_ROOT));
        Assert.assertEquals(Boolean.FALSE, factory.getProperty(JsonXMLOutputFactory.PROP_NAMESPACE_DECLARATIONS));
        Assert.assertEquals(Boolean.TRUE, factory.getProperty(JsonXMLOutputFactory.PROP_PRETTY_PRINT));
        Assert.assertEquals(Boolean.TRUE, factory.getProperty(JsonXMLOutputFactory.PROP_AUTO_ARRAY));
    }

    @Test
    public void testMarshallSampleRootElement() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        StringWriter result = new StringWriter();
        Class<?> type = SampleRootElement.class;
        SampleRootElement sampleRootElement = new SampleRootElement();
        sampleRootElement.attribute = "hello";
        sampleRootElement.elements = Arrays.asList("world");

        XMLStreamWriter writer = new JsonXMLBinder().createXMLStreamWriter(type, config, result);
        Marshaller marshaller = JAXBContext.newInstance(type).createMarshaller();
        new JsonXMLBinder().marshal(type, config, marshaller, writer, sampleRootElement);
        writer.close();

        String json = "{\"sampleRootElement\":{\"@attribute\":\"hello\",\"elements\":\"world\"}}";
        Assert.assertEquals(json, result.toString());
    }

    @Test
    public void testMarshallSampleRootElementWithVirtualRoot() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        StringWriter result = new StringWriter();
        Class<?> type = SampleRootElement.class;
        SampleRootElement sampleRootElement = new SampleRootElement();
        sampleRootElement.attribute = "hello";
        sampleRootElement.elements = Arrays.asList("world");

        XMLStreamWriter writer = new JsonXMLBinder().createXMLStreamWriter(type, config, result);
        Marshaller marshaller = JAXBContext.newInstance(type).createMarshaller();
        new JsonXMLBinder().marshal(type, config, marshaller, writer, sampleRootElement);
        writer.close();

        String json = "{\"@attribute\":\"hello\",\"elements\":[\"world\"]}";
        Assert.assertEquals(json, result.toString());
    }

    @Test
    public void testMarshallSampleType() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        StringWriter result = new StringWriter();
        Class<?> type = SampleType.class;
        SampleType sampleType = new SampleType();
        sampleType.element = "hi!";

        XMLStreamWriter writer = new JsonXMLBinder().createXMLStreamWriter(type, config, result);
        Marshaller marshaller = JAXBContext.newInstance(type).createMarshaller();
        new JsonXMLBinder().marshal(type, config, marshaller, writer, sampleType);
        writer.close();

        String json = "{\"sampleType\":{\"element\":\"hi!\"}}";
        Assert.assertEquals(json, result.toString());
    }

    @Test
    public void testMarshallSampleTypeWithNamespace() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        StringWriter result = new StringWriter();
        Class<?> type = SampleTypeWithNamespace.class;
        SampleTypeWithNamespace sampleTypeWithNamespace = new SampleTypeWithNamespace();

        XMLStreamWriter writer = new JsonXMLBinder().createXMLStreamWriter(type, config, result);
        Marshaller marshaller = JAXBContext.newInstance(type).createMarshaller();
        new JsonXMLBinder().marshal(type, config, marshaller, writer, sampleTypeWithNamespace);
        writer.close();

        Matcher prefixMatcher = Pattern.compile("@xmlns:([a-z1-9]+)").matcher(result.toString());
        Assert.assertTrue(prefixMatcher.find());
        String prefix = prefixMatcher.group(1);
        String json = String.format("{\"%s:sampleTypeWithNamespace\":{\"@xmlns:%s\":\"urn:staxon:jaxb:test\"}}", prefix, prefix);
        Assert.assertEquals(json, result.toString());
    }

    @Test
    public void testUnmarshallSampleRootElement() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "{\"sampleRootElement\":{\"@attribute\":\"hello\",\"elements\":[\"world\"]}}";
        Class<SampleRootElement> type = SampleRootElement.class;

        XMLStreamReader reader = new JsonXMLBinder().createXMLStreamReader(type, config, new StringReader(json));
        Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
        SampleRootElement sampleRootElement = new JsonXMLBinder().unmarshal(type, config, unmarshaller, reader);

        Assert.assertEquals("hello", sampleRootElement.attribute);
        Assert.assertEquals("world", sampleRootElement.elements.get(0));
    }

    @Test
    public void testUnmarshallSampleRootElementWithVirtualRoot() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        String json = "{\"@attribute\":\"hello\",\"elements\":[\"world\"]}";
        Class<SampleRootElement> type = SampleRootElement.class;

        XMLStreamReader reader = new JsonXMLBinder().createXMLStreamReader(type, config, new StringReader(json));
        Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
        SampleRootElement sampleRootElement = new JsonXMLBinder().unmarshal(type, config, unmarshaller, reader);

        Assert.assertEquals("hello", sampleRootElement.attribute);
        Assert.assertEquals("world", sampleRootElement.elements.get(0));
    }

    @Test
    public void testUnmarshallSampleType() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "{\"sampleType\":{\"element\":\"hi!\"}}";
        Class<SampleType> type = SampleType.class;

        XMLStreamReader reader = new JsonXMLBinder().createXMLStreamReader(type, config, new StringReader(json));
        Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
        SampleType sampleType = new JsonXMLBinder().unmarshal(type, config, unmarshaller, reader);

        Assert.assertEquals("hi!", sampleType.element);
    }

    @Test
    public void testUnmarshallSampleTypeWithNamespace() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "{\"sampleTypeWithNamespace\":{\"@xmlns\":\"urn:staxon-jaxrs:test\"}}";
        Class<SampleTypeWithNamespace> type = SampleTypeWithNamespace.class;

        XMLStreamReader reader = new JsonXMLBinder().createXMLStreamReader(type, config, new StringReader(json));
        Unmarshaller unmarshaller = JAXBContext.newInstance(SampleTypeWithNamespace.class).createUnmarshaller();

        Assert.assertNotNull(new JsonXMLBinder().unmarshal(SampleTypeWithNamespace.class, config, unmarshaller, reader));
    }

    @Test
    public void testWriteObjectSampleRootElement() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        SampleRootElement sampleRootElement = new SampleRootElement();
        sampleRootElement.attribute = "hello";
        sampleRootElement.elements = Arrays.asList("world");

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        new JsonXMLBinder().writeObject(SampleRootElement.class, config, context, writer, sampleRootElement);

        String json = "{\"sampleRootElement\":{\"@attribute\":\"hello\",\"elements\":\"world\"}}";
        Assert.assertEquals(json, writer.toString());
    }

    @Test
    public void testWriteObjectSampleType() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        SampleType sampleType = new SampleType();
        sampleType.element = "hi!";

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        new JsonXMLBinder().writeObject(SampleType.class, config, context, writer, sampleType);

        String json = "{\"sampleType\":{\"element\":\"hi!\"}}";
        Assert.assertEquals(json, writer.toString());
    }

    @Test
    public void testWriteObjectNull() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        new JsonXMLBinder().writeObject(SampleType.class, config, context, writer, null);
        Assert.assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteObjectNullWithVirtualRoot() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        new JsonXMLBinder().writeObject(SampleType.class, config, context, writer, null);
        Assert.assertEquals("null", writer.toString());
    }

    @Test
    public void testReadObjectSampleRootElement() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "{\"sampleRootElement\":{\"@attribute\":\"hello\",\"elements\":[\"world\"]}}";

        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        SampleRootElement sampleRootElement =
                new JsonXMLBinder().readObject(SampleRootElement.class, config, context, new StringReader(json));

        Assert.assertEquals("hello", sampleRootElement.attribute);
        Assert.assertEquals("world", sampleRootElement.elements.get(0));
    }

    @Test
    public void testReadObjectSampleType() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "{\"sampleType\":{\"element\":\"hi!\"}}";

        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        SampleType sampleType =
                new JsonXMLBinder().readObject(SampleType.class, config, context, new StringReader(json));

        Assert.assertEquals("hi!", sampleType.element);
    }

    @Test
    public void testReadObjectNull() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        Assert.assertNull(new JsonXMLBinder().readObject(SampleRootElement.class, config, context, new StringReader("null")));
    }

    @Test
    public void testReadObjectNullWithVirtualRoot() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        Assert.assertNotNull(new JsonXMLBinder().readObject(SampleRootElement.class, config, context, new StringReader("null")));
    }

    @Test
    public void testWriteArraySampleRootElement() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        List<SampleRootElement> list = new ArrayList<SampleRootElement>();
        list.add(new SampleRootElement());
        list.get(0).attribute = "hello";
        list.add(new SampleRootElement());
        list.get(1).attribute = "world";

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        new JsonXMLBinder().writeArray(SampleRootElement.class, config, context, writer, list);

        String json = "[{\"sampleRootElement\":{\"@attribute\":\"hello\"}},{\"sampleRootElement\":{\"@attribute\":\"world\"}}]";
        Assert.assertEquals(json, writer.toString());
    }

    @Test
    public void testWriteArraySampleRootElement_Document() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        List<SampleRootElement> list = new ArrayList<SampleRootElement>();
        list.add(new SampleRootElement());
        list.get(0).attribute = "hello";
        list.add(new SampleRootElement());
        list.get(1).attribute = "world";

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        new JsonXMLBinder(false).writeArray(SampleRootElement.class, config, context, writer, list);

        String json = "{\"sampleRootElement\":[{\"@attribute\":\"hello\"},{\"@attribute\":\"world\"}]}";
        Assert.assertEquals(json, writer.toString());
    }


    @Test
    public void testWriteArraySampleType() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        List<SampleType> list = new ArrayList<SampleType>();
        list.add(new SampleType());
        list.get(0).element = "hello";
        list.add(new SampleType());
        list.get(1).element = "world";

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        new JsonXMLBinder().writeArray(SampleType.class, config, context, writer, list);

        String json = "[{\"sampleType\":{\"element\":\"hello\"}},{\"sampleType\":{\"element\":\"world\"}}]";
        Assert.assertEquals(json, writer.toString());
    }

    @Test
    public void testWriteArraySampleRootElementWithNull1() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        List<SampleRootElement> list = new ArrayList<SampleRootElement>();
        list.add(new SampleRootElement());
        list.get(0).attribute = "hello";
        list.add(null);

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        new JsonXMLBinder().writeArray(SampleRootElement.class, config, context, writer, list);

        String json = "[{\"@attribute\":\"hello\"},null]";
        Assert.assertEquals(json, writer.toString());
    }

    @Test
    public void testWriteArraySampleRootElementWithNull2() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        List<SampleRootElement> list = new ArrayList<SampleRootElement>();
        list.add(null);
        list.add(new SampleRootElement());
        list.get(1).attribute = "hello";

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        new JsonXMLBinder().writeArray(SampleRootElement.class, config, context, writer, list);

        String json = "[null,{\"@attribute\":\"hello\"}]";
        Assert.assertEquals(json, writer.toString());
    }

    @Test
    public void testWriteArraySampleRootElementWithNull3() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        List<SampleRootElement> list = new ArrayList<SampleRootElement>();
        list.add(null);

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        new JsonXMLBinder().writeArray(SampleRootElement.class, config, context, writer, list);

        String json = "[null]";
        Assert.assertEquals(json, writer.toString());
    }

    @Test
    public void testWriteArrayEmpty1() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        new JsonXMLBinder().writeArray(SampleRootElement.class, config, context, writer, new ArrayList<SampleRootElement>());
        Assert.assertEquals("[]", writer.toString());
    }

    @Test
    public void testWriteArrayEmpty2() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        new JsonXMLBinder().writeArray(SampleRootElement.class, config, context, writer, new ArrayList<SampleRootElement>());
        Assert.assertEquals("[]", writer.toString());
    }

    @Test
    public void testWriteArrayNull() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        new JsonXMLBinder().writeArray(SampleType.class, config, context, writer, null);
        Assert.assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteArrayNullWithVirtualRoot() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        new JsonXMLBinder().writeArray(SampleType.class, config, context, writer, null);
        Assert.assertEquals("null", writer.toString());
    }

    @Test
    public void testReadArraySampleRootElement() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "{\"sampleRootElement\":[{\"@attribute\":\"hello\"},{\"@attribute\":\"world\"}]}";

        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        List<SampleRootElement> list =
                new JsonXMLBinder().readArray(SampleRootElement.class, config, context, new StringReader(json));

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("hello", list.get(0).attribute);
        Assert.assertEquals("world", list.get(1).attribute);
    }

    @Test
    public void testReadArraySampleRootElement_DocumentArray() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "[{\"sampleRootElement\":{\"@attribute\":\"hello\"}},{\"sampleRootElement\":{\"@attribute\":\"world\"}}]";

        JAXBContext context = JAXBContext.newInstance(SampleRootElement.class);
        List<SampleRootElement> list =
                new JsonXMLBinder().readArray(SampleRootElement.class, config, context, new StringReader(json));

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("hello", list.get(0).attribute);
        Assert.assertEquals("world", list.get(1).attribute);
    }

    @Test
    public void testReadArraySampleTypeWithNull1() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "{\"sampleType\":[{\"element\":\"hi!\"},null]}";

        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        List<SampleType> list = new JsonXMLBinder().readArray(SampleType.class, config, context, new StringReader(json));

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("hi!", list.get(0).element);
        Assert.assertNull(list.get(1).element);
    }

    @Test
    public void testReadArraySampleTypeWithNull2() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "{\"sampleType\":[null,{\"element\":\"hi!\"}]}";

        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        List<SampleType> list = new JsonXMLBinder().readArray(SampleType.class, config, context, new StringReader(json));

        Assert.assertEquals(2, list.size());
        Assert.assertNull(list.get(0).element);
        Assert.assertEquals("hi!", list.get(1).element);
    }

    @Test
    public void testReadArraySampleTypeWithNull_DocumentArray1() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "[{\"sampleType\":{\"element\":\"hi!\"}},null]";

        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        List<SampleType> list = new JsonXMLBinder().readArray(SampleType.class, config, context, new StringReader(json));

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("hi!", list.get(0).element);
        Assert.assertNull(list.get(1));
    }

    @Test
    public void testReadArraySampleTypeWithNull_DocumentArray2() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        String json = "[null,{\"sampleType\":{\"element\":\"hi!\"}}]";

        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        List<SampleType> list = new JsonXMLBinder().readArray(SampleType.class, config, context, new StringReader(json));

        Assert.assertEquals(2, list.size());
        Assert.assertNull(list.get(0));
        Assert.assertEquals("hi!", list.get(1).element);
    }

    @Test
    public void testReadArrayNull() throws Exception {
        JsonXML config = JsonXMLDefault.class.getAnnotation(JsonXML.class);
        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        Assert.assertNull(new JsonXMLBinder().readArray(SampleType.class, config, context, new StringReader("null")));
    }

    @Test
    public void testReadArrayNullWithVirtualRoot() throws Exception {
        JsonXML config = JsonXMLVirtualSampleRootElement.class.getAnnotation(JsonXML.class);
        JAXBContext context = JAXBContext.newInstance(SampleType.class);
        Assert.assertNotNull(new JsonXMLBinder().readArray(SampleType.class, config, context, new StringReader("null")));
    }

}
