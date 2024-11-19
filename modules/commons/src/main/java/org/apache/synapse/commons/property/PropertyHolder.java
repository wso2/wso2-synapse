/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.commons.property;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Property Holder is the class used to store the properties in the map
 */
public class PropertyHolder {

    private ConcurrentHashMap<String, String> properties;

    private static class PropertyLoaderHelper {
        private static final PropertyHolder INSTANCE = new PropertyHolder();
    }

    private PropertyHolder() {}

    public static PropertyHolder getInstance() {
        return PropertyLoaderHelper.INSTANCE;
    }

    public void setProperty(String key, String value) {
        ensureMapInitialized();
        this.properties.put(key, value);
    }

    private void ensureMapInitialized() {
        if (properties == null) {
            this.properties = new ConcurrentHashMap<>();
        }
    }

    public String getPropertyValue(String key) {
        ensureMapInitialized();
        return this.properties.get(key);
    }

    public Boolean hasKey(String key) {
        ensureMapInitialized();
        return properties.containsKey(key);
    }

    public ConcurrentHashMap<String, String> getProperties() {
        return this.properties;
    }
}
