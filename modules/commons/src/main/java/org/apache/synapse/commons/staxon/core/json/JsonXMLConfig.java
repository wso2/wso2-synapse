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
package org.apache.synapse.commons.staxon.core.json;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;

/**
 * <p>Json XML factory configuration interface.</p>
 * <p>A <code>JsonXMLConfig</code> instance can be used to configure either
 * of <code>JsonXMLInputFactory</code> or <code>JsonXMLOutputFactory</code>.</p>
 * <p/>
 * <p>Default values are defined by static {@link #DEFAULT} instance.
 *
 * @see JsonXMLInputFactory
 * @see JsonXMLOutputFactory
 */
public interface JsonXMLConfig {
    /**
     * <p>Default configuration:</p>
     * <ul>
     * <li><em>autoArray</em> - <code>false</code></li>
     * <li><em>autoPrimitive</em> - <code>false</code></li>
     * <li><em>multiplePI</em> - <code>true</code></li>
     * <li><em>namespaceDeclarations</em> - <code>true</code></li>
     * <li><em>namespaceSeparator</em> - <code>':'</code></li>
     * <li><em>prettyPrint</em> - <code>false</code></li>
     * <li><em>virtualRoot</em> - <code>null</code></li>
     * </ul>
     */
    public static final JsonXMLConfig DEFAULT = new JsonXMLConfig() {
        @Override
        public boolean isAutoArray() {
            return false;
        }

        @Override
        public boolean isAutoPrimitive() {
            return false;
        }

        @Override
        public boolean isMultiplePI() {
            return true;
        }

        @Override
        public boolean isNamespaceDeclarations() {
            return true;
        }

        @Override
        public char getNamespaceSeparator() {
            return ':';
        }

        @Override
        public boolean isPrettyPrint() {
            return false;
        }

        @Override
        public QName getVirtualRoot() {
            return null;
        }

        @Override
        public boolean isRepairingNamespaces() {
            return false;
        }

        @Override
        public  String getCustomRegex(){
            return null;
        }
    };

    /**
     * <p>Trigger arrays automatically?</p>
     *
     * @return auto array flag
     * @see JsonXMLOutputFactory#PROP_AUTO_ARRAY
     */
    public boolean isAutoArray();

    /**
     * <p>Convert element text to number/boolean/null primitives automatically?</p>
     *
     * @return auto primitive flag
     * @see JsonXMLOutputFactory#PROP_AUTO_ARRAY
     */
    public boolean isAutoPrimitive();

    /**
     * <p>Whether to use the {@link JsonXMLStreamConstants#MULTIPLE_PI_TARGET}
     * processing instruction to indicate an array start.
     * If <code>true</code>, a PI is used to inform the writer to begin an array,
     * passing the name of following multiple elements as data.
     * The writer will close arrays automatically.</p>
     * If <code>true</code>, this reader will insert a PI with the field
     * name as PI data.
     * <p/>
     * <p>Note that the element given in the PI may occur zero times,
     * indicating an "empty array".</p>
     *
     * @return multiple PI flag
     * @see JsonXMLInputFactory#PROP_MULTIPLE_PI
     * @see JsonXMLOutputFactory#PROP_MULTIPLE_PI
     */
    public boolean isMultiplePI();

    /**
     * <p>Whether to write namespace declarations.</p>
     *
     * @return namespace declarations flag
     * @see JsonXMLOutputFactory#PROP_NAMESPACE_DECLARATIONS
     */
    public boolean isNamespaceDeclarations();

    /**
     * <p>Namespace prefix separator.</p>
     *
     * @return namespace separator
     * @see JsonXMLInputFactory#PROP_NAMESPACE_SEPARATOR
     * @see JsonXMLOutputFactory#PROP_NAMESPACE_SEPARATOR
     */
    public char getNamespaceSeparator();

    /**
     * <p>Format output for better readability?</p>
     *
     * @return pretty print flag
     * @see JsonXMLOutputFactory#PROP_PRETTY_PRINT
     */
    public boolean isPrettyPrint();

    /**
     * <p>JSON documents may have have multiple root properties. However,
     * XML requires a single root element. This property specifies
     * the root as a "virtual" element, which will be removed from the stream
     * when writing and added to the stream when reading.
     *
     * @return virtual root
     * @see JsonXMLInputFactory#PROP_VIRTUAL_ROOT
     * @see JsonXMLOutputFactory#PROP_VIRTUAL_ROOT
     */
    public QName getVirtualRoot();

    /**
     * <p>Repair namespaces when writing</+>
     *
     * @return namespace-repairing flag
     * @see XMLOutputFactory#IS_REPAIRING_NAMESPACES
     */
    public boolean isRepairingNamespaces();


    /**
     *<p>Format output ignoring scientific numbers</p>
     * @see JsonXMLOutputFactory#PROP_CUSTOM_REGEX
     * @return autoprimitive enabled with scientific Number Ignore flag
     */
    public String getCustomRegex();

}