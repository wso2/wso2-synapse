/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.unittest;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.registry.AbstractRegistry;
import org.apache.synapse.registry.RegistryEntry;
import org.apache.synapse.unittest.testcase.data.classes.RegistryResource;
import org.apache.synapse.util.SynapseBinaryDataSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Class responsible for the add and return registry resources uses for Unit test flow.
 * Mock the AbstractRegistry class only usage of the synapse unit testing framework
 */
public class UnitTestMockRegistry extends AbstractRegistry {

    private static final Log log = LogFactory.getLog(UnitTestMockRegistry.class);

    //The list of configuration properties
    protected final Properties properties = new Properties();

    //The list of registry resources
    private Map<String, RegistryResource> testMockRegistry = new HashMap<>();

    private static final String LOCAL_REGISTRY_PATH = "/_system/local";
    private static final String CONFIGURATION_REGISTRY_PATH = "/_system/config";
    private static final String GOVERNANCE_REGISTRY_PATH = "/_system/governance";
    private static final String FILE = "http://wso2.org/projects/esb/registry/types/file";

    private static UnitTestMockRegistry initializeRegistry = new UnitTestMockRegistry();

    /**
     * Return initialized UnitTestingExecutor initializeThread object.
     */
    public static synchronized UnitTestMockRegistry getInstance() {
        return initializeRegistry;
    }

    /**
     * Add new registry resource to the map.
     *
     * @param key resource key
     * @param resource resource object
     */
    public void addResource(String key, RegistryResource resource) {
        testMockRegistry.put(key, resource);
    }

    /**
     * Clear registry resource map.
     */
    public void clearResources() {
        testMockRegistry.clear();
    }

    @Override
    public OMNode lookup(String key) {
        if (key == null) {
            handleException("Resource cannot be found.");
        }

        if (key.isEmpty()) {
            handleException("Resource cannot be empty");
        }

        String resourcePath = getAbsolutePathToRegistry(key);

        if (resourcePath != null && testMockRegistry.containsKey(resourcePath)) {
            RegistryResource resource = testMockRegistry.get(resourcePath);
            String sourceOfResource = resource.getArtifact();

            OMNode omNode = null;
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceOfResource.getBytes());

                try {
                    XMLStreamReader parser = XMLInputFactory.newInstance().
                            createXMLStreamReader(inputStream);
                    StAXOMBuilder builder = new StAXOMBuilder(parser);
                    omNode = builder.getDocumentElement();

                } catch (OMException ignored) {
                    omNode = readNonXML(resource);

                } catch (XMLStreamException ignored) {
                    omNode = readNonXML(resource);

                } catch (Exception e) {
                    log.error("Error while reading the resource '" + key + "'", e);
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing the input stream", e);
                    }
                }
            } catch (Exception e) {
                log.error("Creating OMNode from registry resource artifact failed", e);
            }
            return omNode;
        } else {
            return null;
        }
    }

    /**
     * Get absolute path for the registry resource in the registry.
     *
     * @param key resource key
     * @return  resource resource absolute path
     */
    private String getAbsolutePathToRegistry(String key) {
        if (!key.contains(":")) {
            return key;
        }

        String[] entryArray = key.split(":");
        String entryType = entryArray[0];
        String entryPath = entryArray[1];

        if (!entryPath.startsWith(File.separator)) {
            entryPath = File.separator + entryPath;
        }

        String resourcePath;
        if (entryType.equals(Constants.LOCAL_REGISTRY_TYPE)) {
            resourcePath = LOCAL_REGISTRY_PATH + entryPath;
        } else if (entryType.equals(Constants.CONFIGURATION_REGISTRY_TYPE)) {
            resourcePath = CONFIGURATION_REGISTRY_PATH + entryPath;
        } else if (entryType.equals(Constants.GOVERNANCE_REGISTRY_TYPE)) {
            resourcePath = GOVERNANCE_REGISTRY_PATH + entryPath;
        } else {
            return null;
        }

        return resourcePath;
    }

    @Override
    public Properties getResourceProperties(String entryKey) {
        Properties propertySet = new Properties();

        String filePathAsKey = entryKey.substring(0, entryKey.length() - 1) + ".properties";
        String resourcePath = getAbsolutePathToRegistry(filePathAsKey);
        boolean isFoundPropertyFile = false;

        //check registry has .properties file for the properties with key
        if (resourcePath != null && testMockRegistry.containsKey(resourcePath)) {
            isFoundPropertyFile = true;
        } else {
            //check registry has file for the properties with key
            filePathAsKey = entryKey.substring(0, entryKey.length() - 1);
            resourcePath = getAbsolutePathToRegistry(filePathAsKey);

            if (resourcePath != null && testMockRegistry.containsKey(resourcePath)) {
                isFoundPropertyFile = true;
            }
        }

        //check properties file found in the registry if not return null
        if (!isFoundPropertyFile) {
            return null;
        }

        RegistryResource resource = testMockRegistry.get(resourcePath);
        String sourceOfResource = resource.getArtifact().trim();

        try {
            InputStream input = new ByteArrayInputStream(sourceOfResource.getBytes(StandardCharsets.UTF_8));
            propertySet.load(input);

        } catch (IOException e) {
            log.error("Error in loading properties from registry resource", e);
        }

        return propertySet;
    }

    /**
     * Helper method to handle non-XMl resources.
     *
     * @param resource Registry resource
     * @return The content as an OMNode
     */
    private OMNode readNonXML(RegistryResource resource) {
        if (log.isDebugEnabled()) {
            log.debug("The resource at the specified path does not contain well-formed XML - Processing as text");
        }

        if (resource != null) {
            if (resource.getMediaType() == null || resource.getMediaType().equals("text/plain")) {
                // for non-xml text content	or no media type defined
                return OMAbstractFactory.getOMFactory().createOMText(resource.getArtifact());
            }
            //For media types other than text/plain
            ByteArrayInputStream inputStream = new ByteArrayInputStream(resource.getArtifact().getBytes());
            try {
                OMFactory omFactory = OMAbstractFactory.getOMFactory();
                return omFactory.createOMText(
                        new DataHandler(new SynapseBinaryDataSource(inputStream, resource.getMediaType())), true);
            } catch (IOException e) {
                log.error("Error while getting a stream from resource content ", e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Error while closing the input stream", e);
                }
            }
        }
        return null;
    }

    private void handleException(String msg) {
        throw new SynapseException(msg);
    }

    @Override
    public RegistryEntry getRegistryEntry(String key) {
        // get information from the actual resource
        TestMediationRegistryEntryImpl entryEmbedded = new TestMediationRegistryEntryImpl();
        String resourcePath = getAbsolutePathToRegistry(key);
        RegistryResource resource = testMockRegistry.get(resourcePath);

        if (resourcePath != null && testMockRegistry.containsKey(resourcePath)) {
            String resourceName = resource.getRegistryPath() + resource.getRegistryResourceName();
            Date date = new Date();
            long timestamp = date.getTime();

            entryEmbedded.setKey(key);
            entryEmbedded.setName(resourceName);
            entryEmbedded.setType(FILE);
            entryEmbedded.setDescription("Resource at : " + resourceName);
            entryEmbedded.setLastModified(timestamp);
            entryEmbedded.setVersion(timestamp);
            entryEmbedded.setCachableDuration(15000);

            return entryEmbedded;
        }

        return null;
    }

    @Override
    public OMNode lookupFormat(String key) {
        return null;
    }

    @Override
    public RegistryEntry[] getChildren(RegistryEntry entry) {
        return new RegistryEntry[0];
    }

    @Override
    public RegistryEntry[] getDescendants(RegistryEntry entry) {
        return new RegistryEntry[0];
    }

    @Override
    public void delete(String path) {
    }

    @Override
    public void newResource(String path, boolean isDirectory) {
    }

    @Override
    public void newNonEmptyResource(String path, boolean isDirectory, String contentType, String content,
                                    String propertyName) {
    }

    @Override
    public void updateResource(String path, Object value) {
    }

    @Override
    public void updateRegistryEntry(RegistryEntry entry) {
    }

    @Override
    public boolean isResourceExists(String path) {
        return false;
    }
}
