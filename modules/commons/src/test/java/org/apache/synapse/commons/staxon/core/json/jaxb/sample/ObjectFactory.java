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
package org.apache.synapse.commons.staxon.core.json.jaxb.sample;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {
    public SampleRootElement createSampleRootElement() {
        return new SampleRootElement();
    }

    @XmlElementDecl(name = "sampleType")
    public JAXBElement<SampleType> createSampleType(SampleType value) {
        return new JAXBElement<SampleType>(new QName("sampleType"), SampleType.class, null, value);
    }

    @XmlElementDecl(name = "sampleTypeWithNamespace", namespace = "urn:staxon:jaxb:test")
    public JAXBElement<SampleTypeWithNamespace> createSampleTypeWithNamespace(SampleTypeWithNamespace value) {
        return new JAXBElement<SampleTypeWithNamespace>(new QName("urn:staxon:jaxb:test", "sampleTypeWithNamespace"), SampleTypeWithNamespace.class, null, value);
    }
}
