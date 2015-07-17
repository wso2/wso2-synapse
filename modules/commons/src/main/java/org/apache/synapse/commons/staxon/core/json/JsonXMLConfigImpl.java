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
 * <p>Simple JSON XML configuration.</p>
 * <p/>
 * <p>Initially, values are set according to {@link JsonXMLConfig#DEFAULT}.</p>
 *
 * @see JsonXMLConfig
 */
public class JsonXMLConfigImpl implements JsonXMLConfig, Cloneable {
    private QName virtualRoot = JsonXMLConfig.DEFAULT.getVirtualRoot();
    private boolean multiplePI = JsonXMLConfig.DEFAULT.isMultiplePI();
    private boolean prettyPrint = JsonXMLConfig.DEFAULT.isPrettyPrint();
    private boolean autoArray = JsonXMLConfig.DEFAULT.isAutoArray();
    private boolean autoPrimitive = JsonXMLConfig.DEFAULT.isAutoPrimitive();
    private boolean namespaceDeclarations = JsonXMLConfig.DEFAULT.isNamespaceDeclarations();
    private char namespaceSeparator = JsonXMLConfig.DEFAULT.getNamespaceSeparator();

    private boolean repairingNamespaces = JsonXMLConfig.DEFAULT.isRepairingNamespaces();
    private String customRegex =JsonXMLConfig.DEFAULT.getCustomRegex();

    @Override
    protected JsonXMLConfigImpl clone() {
        try {
            return (JsonXMLConfigImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); // should not happen
        }
    }

    @Override
    public boolean isAutoArray() {
        return autoArray;
    }

    public void setAutoArray(boolean autoArray) {
        this.autoArray = autoArray;
    }

    @Override
    public boolean isAutoPrimitive() {
        return autoPrimitive;
    }

    public void setAutoPrimitive(boolean autoPrimitive) {
        this.autoPrimitive = autoPrimitive;
    }

    @Override
    public boolean isMultiplePI() {
        return multiplePI;
    }

    public void setMultiplePI(boolean multiplePI) {
        this.multiplePI = multiplePI;
    }

    @Override
    public boolean isNamespaceDeclarations() {
        return namespaceDeclarations;
    }

    public void setNamespaceDeclarations(boolean namespaceDeclarations) {
        this.namespaceDeclarations = namespaceDeclarations;
    }

    @Override
    public char getNamespaceSeparator() {
        return namespaceSeparator;
    }

    public void setNamespaceSeparator(char namespaceSeparator) {
        this.namespaceSeparator = namespaceSeparator;
    }

    @Override
    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    @Override
    public QName getVirtualRoot() {
        return virtualRoot;
    }

    public void setVirtualRoot(QName virtualRoot) {
        this.virtualRoot = virtualRoot;
    }

    @Override
    public boolean isRepairingNamespaces() {
        return repairingNamespaces;
    }

    public void setRepairingNamespaces(boolean repairingNamespaces) {
        this.repairingNamespaces = repairingNamespaces;
    }

    @Override
    public String getCustomRegex() {
        return customRegex;
    }

    public void setCustomRegex(String customRegex){
        this.customRegex =customRegex;
    }

}
