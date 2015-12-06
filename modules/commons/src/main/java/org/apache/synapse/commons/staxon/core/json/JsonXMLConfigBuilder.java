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

/**
 * <p>Configuration builder with "fluid" interface.</p>
 * <pre>
 * JsonXMLConfig config = new JsonXMLConfigBuilder().virtualRoot("foo").prettyPrint(true).build();
 * </pre>
 * <p>Initially, values are set according to {@link JsonXMLConfig#DEFAULT}.</p>
 *
 * @see JsonXMLConfig
 */
public class JsonXMLConfigBuilder {
    protected final JsonXMLConfigImpl config;

    /**
     * Create a new builder.
     */
    public JsonXMLConfigBuilder() {
        this(new JsonXMLConfigImpl());
    }

    protected JsonXMLConfigBuilder(JsonXMLConfigImpl config) {
        this.config = config;
    }

    /**
     * Build a new configuration.
     *
     * @return configuration instance
     */
    public JsonXMLConfig build() {
        return config.clone();
    }

    /**
     * Set autoArray property and return receiver.
     *
     * @param autoArray
     * @return this
     */
    public JsonXMLConfigBuilder autoArray(boolean autoArray) {
        config.setAutoArray(autoArray);
        return this;
    }

    /**
     * Set autoPrimitive property and return receiver.
     *
     * @param autoPrimitive
     * @return this
     */
    public JsonXMLConfigBuilder autoPrimitive(boolean autoPrimitive) {
        config.setAutoPrimitive(autoPrimitive);
        return this;
    }

    /**
     * Set multiplePI property and return receiver.
     *
     * @param multiplePI
     * @return this
     */
    public JsonXMLConfigBuilder multiplePI(boolean multiplePI) {
        config.setMultiplePI(multiplePI);
        return this;
    }

    /**
     * Set namespaceDeclarations property and return receiver.
     *
     * @param namespaceDeclarations
     * @return this
     */
    public JsonXMLConfigBuilder namespaceDeclarations(boolean namespaceDeclarations) {
        config.setNamespaceDeclarations(namespaceDeclarations);
        return this;
    }

    /**
     * Set namespaceSeparator property and return receiver.
     *
     * @param namespaceSeparator
     * @return this
     */
    public JsonXMLConfigBuilder namespaceSeparator(char namespaceSeparator) {
        config.setNamespaceSeparator(namespaceSeparator);
        return this;
    }

    /**
     * Set prettyPrint property and return receiver.
     *
     * @param prettyPrint
     * @return this
     */
    public JsonXMLConfigBuilder prettyPrint(boolean prettyPrint) {
        config.setPrettyPrint(prettyPrint);
        return this;
    }

    /**
     * Set virtualRoot property and return receiver.
     *
     * @param virtualRoot
     * @return this
     */
    public JsonXMLConfigBuilder virtualRoot(QName virtualRoot) {
        config.setVirtualRoot(virtualRoot);
        return this;
    }

    /**
     * Set virtualRoot property and return receiver.
     *
     * @param virtualRoot (parsed with {@link QName#valueOf(String)})
     * @return this
     */
    public JsonXMLConfigBuilder virtualRoot(String virtualRoot) {
        config.setVirtualRoot(QName.valueOf(virtualRoot));
        return this;
    }

    /**
     * Set repairingNamespaces property and return receiver.
     *
     * @param repairingNamespaces
     * @return this
     */
    public JsonXMLConfigBuilder repairingNamespaces(boolean repairingNamespaces) {
        config.setRepairingNamespaces(repairingNamespaces);
        return this;
    }

    /**
     * Set Custom Regex for ignore  Auto Primitive mode
     * @param regex
     * @return this
     */
    public JsonXMLConfigBuilder customRegex(String regex) {
        config.setCustomRegex(regex);
        return this;
    }
}
