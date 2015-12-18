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

import java.lang.reflect.Method;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.junit.Test;

import org.apache.synapse.commons.staxon.core.json.jaxb.JsonXMLBinderTest.EmptyType;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.ObjectFactory;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleRootElement;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleType;
import org.apache.synapse.commons.staxon.core.json.jaxb.sample.SampleTypeWithNamespace;

public class JsonXMLRootProviderTest {
    @Test
    public void testGetXmlElementDeclMethod() {
        JsonXMLRootProvider provider = new JsonXMLRootProvider();
        Assert.assertNull(provider.getXmlElementDeclMethod(SampleRootElement.class));
        Assert.assertNull(provider.getXmlElementDeclMethod(EmptyType.class));
        Assert.assertNotNull(provider.getXmlElementDeclMethod(SampleType.class));
    }

    @Test
    public void testGetXmlTypeName() {
        JsonXMLRootProvider provider = new JsonXMLRootProvider();
        Assert.assertNull(provider.getXmlTypeName(SampleRootElement.class));
        Assert.assertNull(provider.getXmlTypeName(EmptyType.class));
        Assert.assertEquals(new QName("sampleType"), provider.getXmlTypeName(SampleType.class));
        Assert.assertEquals(new QName("urn:staxon:jaxb:test", "sampleTypeWithNamespace"), provider.getXmlTypeName(SampleTypeWithNamespace.class));
    }

    @Test
    public void testGetXmlRootElementName() {
        JsonXMLRootProvider provider = new JsonXMLRootProvider();
        Assert.assertEquals(new QName("sampleRootElement"), provider.getXmlRootElementName(SampleRootElement.class));
        Assert.assertNull(provider.getXmlRootElementName(EmptyType.class));
        Assert.assertNull(provider.getXmlRootElementName(SampleType.class));
        Assert.assertNull(provider.getXmlRootElementName(SampleTypeWithNamespace.class));
    }

    @Test
    public void testGetNamespaceURI_XmlRootElement() {
        JsonXMLRootProvider provider = new JsonXMLRootProvider();
        Assert.assertEquals(XMLConstants.NULL_NS_URI,
                provider.getNamespaceURI(SampleRootElement.class.getAnnotation(XmlRootElement.class), null));
    }

    @Test
    public void testGetNamespaceURI_XmlType() {
        JsonXMLRootProvider provider = new JsonXMLRootProvider();
        Assert.assertEquals(XMLConstants.NULL_NS_URI,
                provider.getNamespaceURI(SampleType.class.getAnnotation(XmlType.class), null));
        Assert.assertEquals("urn:staxon:jaxb:test",
                provider.getNamespaceURI(SampleTypeWithNamespace.class.getAnnotation(XmlType.class), null));
    }

    @Test
    public void testGetNamespaceURI_XmlElementDecl() throws Exception {
        JsonXMLRootProvider provider = new JsonXMLRootProvider();
        Method createSampleType =
                ObjectFactory.class.getMethod("createSampleType", SampleType.class);
        Assert.assertEquals(XMLConstants.NULL_NS_URI,
                provider.getNamespaceURI(createSampleType.getAnnotation(XmlElementDecl.class), null));
        Method createSampleTypeWithNamespace =
                ObjectFactory.class.getMethod("createSampleTypeWithNamespace", SampleTypeWithNamespace.class);
        Assert.assertEquals("urn:staxon:jaxb:test",
                provider.getNamespaceURI(createSampleTypeWithNamespace.getAnnotation(XmlElementDecl.class), null));
    }

    @Test
    public void testGetName() {
        JsonXMLRootProvider provider = new JsonXMLRootProvider();
        Assert.assertEquals(new QName("sampleRootElement"), provider.getName(SampleRootElement.class));
        Assert.assertEquals(new QName("sampleType"), provider.getName(SampleType.class));
        Assert.assertEquals(new QName("urn:staxon:jaxb:test", "sampleTypeWithNamespace"), provider.getName(SampleTypeWithNamespace.class));
        Assert.assertNull(provider.getName(EmptyType.class));
    }

    @Test
    public void testCreateElement() throws JAXBException {
        JsonXMLRootProvider provider = new JsonXMLRootProvider();
        JAXBElement<?> sampleRootElement = provider.createElement(SampleRootElement.class, new SampleRootElement());
        Assert.assertEquals(new QName("sampleRootElement"), sampleRootElement.getName());
        Assert.assertEquals(SampleRootElement.class, sampleRootElement.getDeclaredType());
        JAXBElement<?> sampleType = provider.createElement(SampleType.class, new SampleType());
        Assert.assertEquals(new QName("sampleType"), sampleType.getName());
        Assert.assertEquals(SampleType.class, sampleType.getDeclaredType());
        JAXBElement<?> sampleTypeWithNamespace = provider.createElement(SampleTypeWithNamespace.class, new SampleTypeWithNamespace());
        Assert.assertEquals(new QName("urn:staxon:jaxb:test", "sampleTypeWithNamespace"), sampleTypeWithNamespace.getName());
        Assert.assertEquals(SampleTypeWithNamespace.class, sampleTypeWithNamespace.getDeclaredType());
        Assert.assertNull(provider.createElement(EmptyType.class, new EmptyType()));
    }
}
