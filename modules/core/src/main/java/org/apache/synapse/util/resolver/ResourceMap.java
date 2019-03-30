/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.util.resolver;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.mediators.Value;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A resource map.
 * 
 * Instances of this class are used to resolve resources using registry entries.
 * This is useful for XML documents that can reference other documents (e.g. WSDL documents
 * importing XSD or other WSDL documents). A <code>ResourceMap</code> object contains a set of
 * (location, registry key) mappings. The <code>resolve</code> method can be used to
 * get retrieve the registry entry registered for a given location as an {@link InputSource}
 * object.
 */
public class ResourceMap {

    private static final Log log = LogFactory.getLog(ResourceMap.class);

    private final List<Pair> resourceLocationList = new ArrayList<>();

    /**
     * Add a resource.
     * 
     * @param location the location as it appears in referencing documents
     * @param key the registry key that points to the referenced document
     */
    public void addResource(Value location, Value key) {
        resourceLocationList.add(new Pair(location, key));
    }

    /**
     * Add a resource.
     *
     * @param location the location as it appears in referencing documents
     * @param key the registry key that points to the referenced document
     */
    public void addResource(String location, String key) {
        //todo - What should we do if an expression passes as the key
        addResource(new Value(location), new Value(key));
    }
    
    /**
     * Get the (location, registry key) mappings.
     * 
     * @return a map containing the (location, registry key) pairs
     */
    public Map<String,String> getResources() {
        Map<String, String> resourceMap = new LinkedHashMap<>();
        for (Pair entry : resourceLocationList) {
            resourceMap.put(entry.getKey().getKeyValue() == null ?
                            "{".concat(entry.getKey().getExpression().getExpression()).concat("}") :
                            entry.getKey().getKeyValue(),
                            entry.getValue().getKeyValue() == null ?
                            "{".concat(entry.getValue().getExpression().getExpression()).concat("}") :
                            entry.getValue().getKeyValue());
        }
        return Collections.unmodifiableMap(resourceMap);
    }

    /**
     * Resolve a resource for a given location.
     *
     * @param synCfg the Synapse configuration (used to access the registry)
     * @param location the location of of the resource at is appears in the referencing document
     * @return an <code>InputSource</code> object for the referenced resource
     */
    public InputSource resolve(SynapseConfiguration synCfg, String location) {
        return resolve(synCfg, location, null);
    }

    /**
     * Resolve a resource for a given location.
     * 
     * @param synCfg the Synapse configuration (used to access the registry)
     * @param location the location of of the resource at is appears in the referencing document
     * @param messageContext current message context of the received request
     * @return an <code>InputSource</code> object for the referenced resource
     */
    public InputSource resolve(SynapseConfiguration synCfg, String location, MessageContext messageContext) {
        String key = null;
        Map<String, String> locationMap = new HashMap<>();
        for(Pair entry : resourceLocationList) {
            if (messageContext != null) {
                locationMap.put(entry.getKey().evaluateValue(messageContext),
                                entry.getValue().evaluateValue(messageContext));
            } else {
                locationMap.put(entry.getKey().getKeyValue() == null ?
                                entry.getKey().getExpression().getExpression() :
                                entry.getKey().getKeyValue(),
                                entry.getValue().getKeyValue() == null ?
                                entry.getValue().getExpression().getExpression() :
                                entry.getValue().getKeyValue());
            }
        }

        if (locationMap.get(location) != null) {
            key = locationMap.get(location);
        }
        if (key == null) {
            if (log.isDebugEnabled()) {
                log.debug("No resource mapping is defined for location '" + location + "'");
            }
            return null;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Resolving location '" + location + "' to registry item '" + key + "'");
            }
            synCfg.getEntryDefinition(key);
            Object keyObject = synCfg.getEntry(key);
            if (keyObject instanceof OMElement) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ((OMElement)keyObject).serialize(baos);
                }
                catch (XMLStreamException ex) {
                    String msg = "Unable to serialize registry item '" + key + "' for location '" +
                        location + "'";
                    log.error(msg);
                    throw new SynapseException(msg, ex);
                }
                InputSource inputSource = new InputSource(
                        new ByteArrayInputStream(baos.toByteArray()));
                // We must set a system ID because Axis2 relies on this (see SYNAPSE-362). Compose a
                // valid URI with the registry key so that it uniquely identifies the resource.
                inputSource.setSystemId("synapse-reg:///" + key);
                return inputSource;
            } else {
                String msg = "Registry item '" + key + "' for location '" +
                    location + "' is not an OMElement";
                log.error(msg);
                throw new SynapseException(msg);
            }
        }
    }

    /*
    We need this class to keep a pair of location and key values in resource attribute
     */
    class Pair {

        private Value location;
        private Value key;

        Pair(Value location, Value key) {
            this.location = location;
            this.key = key;
        }

        public Value getKey() { return location; }
        public Value getValue() { return key; }
    }
}
