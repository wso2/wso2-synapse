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
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

/**
 * Root name/element provider.
 */
public class JsonXMLRootProvider {
    protected String getNamespaceURI(XmlType xmlType, XmlSchema xmlSchema) {
        if ("##default".equals(xmlType.namespace())) {
            return xmlSchema == null ? XMLConstants.NULL_NS_URI : xmlSchema.namespace();
        } else {
            return xmlType.namespace();
        }
    }

    protected String getNamespaceURI(XmlRootElement xmlRootElement, XmlSchema xmlSchema) {
        if ("##default".equals(xmlRootElement.namespace())) {
            return xmlSchema == null ? XMLConstants.NULL_NS_URI : xmlSchema.namespace();
        } else {
            return xmlRootElement.namespace();
        }
    }

    protected String getNamespaceURI(XmlElementDecl xmlElementDecl, XmlSchema xmlSchema) {
        if ("##default".equals(xmlElementDecl.namespace())) {
            return xmlSchema == null ? XMLConstants.NULL_NS_URI : xmlSchema.namespace();
        } else {
            return xmlElementDecl.namespace();
        }
    }

    protected String getPrefix(String namespaceURI, XmlSchema xmlSchema) {
        if (xmlSchema != null) {
            for (XmlNs xmlns : xmlSchema.xmlns()) {
                if (xmlns.namespaceURI().equals(namespaceURI)) {
                    return xmlns.prefix();
                }
            }
        }
        return XMLConstants.DEFAULT_NS_PREFIX;
    }

    /**
     * Calculate root element name for an <code>@XmlRootElement</code>-annotated type.
     *
     * @param type
     * @return element name
     */
    protected QName getXmlRootElementName(Class<?> type) {
        XmlRootElement xmlRootElement = type.getAnnotation(XmlRootElement.class);
        if (xmlRootElement == null) {
            return null;
        }
        String localName;
        if ("##default".equals(xmlRootElement.name())) {
            localName = Character.toLowerCase(type.getSimpleName().charAt(0)) + type.getSimpleName().substring(1);
        } else {
            localName = xmlRootElement.name();
        }
        XmlSchema xmlSchema = type.getPackage().getAnnotation(XmlSchema.class);
        String namespaceURI = getNamespaceURI(xmlRootElement, xmlSchema);
        return new QName(namespaceURI, localName, getPrefix(namespaceURI, xmlSchema));
    }

    /**
     * Calculate root element name for an <code>@XmlType</code>-annotated type.
     *
     * @param type
     * @return element name
     */
    protected QName getXmlTypeName(Class<?> type) {
        Method method = getXmlElementDeclMethod(type);
        if (method == null) {
            return null;
        }
        XmlElementDecl xmlElementDecl = method.getAnnotation(XmlElementDecl.class);
        if (xmlElementDecl == null) {
            return null;
        }
        XmlSchema xmlSchema = type.getPackage().getAnnotation(XmlSchema.class);
        String namespaceURI = getNamespaceURI(xmlElementDecl, xmlSchema);
        return new QName(namespaceURI, xmlElementDecl.name(), getPrefix(namespaceURI, xmlSchema));
    }

    /**
     * Determine <code>@XmlElementDecl</code>-annotated factory method to create {@link JAXBElement}
     * for an <code>@XmlType</code>-annotated type
     *
     * @param type
     * @return element
     */
    protected Method getXmlElementDeclMethod(Class<?> type) {
        XmlType xmlType = type.getAnnotation(XmlType.class);
        if (xmlType == null) {
            return null;
        }
        Class<?> factoryClass = xmlType.factoryClass();
        if (factoryClass == XmlType.DEFAULT.class) {
            String defaultObjectFactoryName = type.getPackage().getName() + ".ObjectFactory";
            try {
                factoryClass = Thread.currentThread().getContextClassLoader().loadClass(defaultObjectFactoryName);
            } catch (Exception e) {
                factoryClass = type;
            }
        }
        if (factoryClass.getAnnotation(XmlRegistry.class) == null) {
            return null;
        }
        XmlSchema xmlSchema = type.getPackage().getAnnotation(XmlSchema.class);
        String namespaceURI = getNamespaceURI(xmlType, xmlSchema);
        for (Method method : factoryClass.getDeclaredMethods()) {
            XmlElementDecl xmlElementDecl = method.getAnnotation(XmlElementDecl.class);
            if (xmlElementDecl != null && namespaceURI.equals(getNamespaceURI(xmlElementDecl, xmlSchema))) {
                if (method.getReturnType() == JAXBElement.class) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 1 && parameterTypes[0] == type) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Calculate root element name for an
     * <code>@XmlRootElement</code> or <code>@XmlType</code>-annotaed type.
     *
     * @param type
     * @return name or <code>null</code>
     */
    public QName getName(Class<?> type) {
        if (type.getAnnotation(XmlRootElement.class) != null) {
            return getXmlRootElementName(type);
        } else if (type.getAnnotation(XmlType.class) != null) {
            return getXmlTypeName(type);
        } else {
            return null;
        }
    }

    /**
     * Create root element for an
     * <code>@XmlRootElement</code> or <code>@XmlType</code>-annotaed type.
     *
     * @param type
     * @param value
     * @return root element (or <code>null</code>)
     */
    public JAXBElement<?> createElement(Class<?> type, Object value) throws JAXBException {
        if (type.getAnnotation(XmlRootElement.class) != null) {
            QName name = getXmlRootElementName(type);
            if (name != null) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                JAXBElement<?> genericElement = new JAXBElement(name, type, value);
                return genericElement;
            }
        } else if (type.getAnnotation(XmlType.class) != null) {
            Method method = getXmlElementDeclMethod(type);
            if (method != null) {
                try {
                    return (JAXBElement<?>) method.invoke(method.getDeclaringClass().newInstance(), value);
                } catch (Exception e) {
                    throw new JAXBException("Cannot create JAXBElement", e);
                }
            }
        }
        return null;
    }
}
