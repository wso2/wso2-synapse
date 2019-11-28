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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.Base64;
import org.apache.axis2.AxisFault;
import org.apache.axis2.deployment.Deployer;
import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.EntryFactory;
import org.apache.synapse.config.xml.SynapseImportFactory;
import org.apache.synapse.config.xml.SynapseImportSerializer;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.deployers.LibraryArtifactDeployer;
import org.apache.synapse.deployers.SynapseArtifactDeploymentStore;
import org.apache.synapse.libraries.imports.SynapseImport;
import org.apache.synapse.libraries.model.Library;
import org.apache.synapse.libraries.util.LibDeployerUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Class responsible for the deploy the connector resources uses for Unit test flow.
 * Mock the Connector Deployment methods from mediation only usage for the synapse unit testing framework
 */
class ConnectorDeployer {

    private static Logger log = Logger.getLogger(ConnectorDeployer.class.getName());

    private static final String SYNAPSE_CONFIG_LOCK = "synapse.config.lock";
    private static final String SYNAPSE_LIBS = "synapse-libs";
    private static final String SYNAPSE_LIBRARY_EXTENSION = "zip";

    private ConnectorDeployer(){}

    /**
     * Extract and load connector resources.
     *
     * @param connectorList list which has connector resources
     */
    static void deployConnectorResources(List<String> connectorList) {
        for (int x = 0; x < connectorList.size(); x++) {
            String javaTempTestDir = System.getProperty(Constants.PRAM_TEMP_DIR) + File.separator
                    + Constants.CONNECTOR_TEST_FOLDER;
            createDir(javaTempTestDir);

            BufferedOutputStream out = null;
            try {
                byte[] decodedConnector = Base64.decode(connectorList.get(x));
                String connectorZip = javaTempTestDir + File.separator + "connector-" + x + ".zip";
                out = new BufferedOutputStream(new FileOutputStream(connectorZip), 4096);
                out.write(decodedConnector);

                //deploy connector zip
                LibDeployerUtils.createSynapseLibrary(connectorZip);
                SynapseConfiguration configuration = UnitTestingExecutor.getExecuteInstance().getSynapseConfiguration();
                AxisConfiguration axisConfiguration = configuration.getAxisConfiguration();

                Deployer deployer = getSynapseLibraryDeployer(axisConfiguration);
                deployer.deploy(new DeploymentFileData(new File(connectorZip), deployer));
                try {
                    String artifactName = getArtifactName(connectorZip, axisConfiguration);
                    if (artifactName != null) {
                        if (configuration.getSynapseImports().get(artifactName) == null) {
                            String libName = artifactName.substring(artifactName.lastIndexOf("}") + 1);
                            String libraryPackage = artifactName.substring(1, artifactName.lastIndexOf("}"));
                            updateStatus(artifactName, libName, libraryPackage, "enabled", axisConfiguration);
                        }
                    }
                } catch (AxisFault axisFault) {
                    log.error("Unable to update status for the synapse library : ", axisFault);
                }

            } catch (DeploymentException e) {
                log.error("Error while deploying the synapse library : ", e);
            } catch (IOException e) {
                log.error("Error while writing the output stream of connector zip file", e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        log.error("Error while closing the output stream of connector zip file", e);
                    }
                }
            }
        }
    }

    /**
     * Get the deployer for the Synapse Library.
     *
     * @param axisConfig AxisConfiguration instance
     * @return Deployer instance
     */
    private static Deployer getSynapseLibraryDeployer(AxisConfiguration axisConfig) {
        try {
            String synapseLibraryPath = axisConfig.getRepository().getPath() + SYNAPSE_LIBS;
            DeploymentEngine deploymentEngine = (DeploymentEngine) axisConfig.getConfigurator();
            deploymentEngine.addDeployer(new LibraryArtifactDeployer(), synapseLibraryPath, SYNAPSE_LIBRARY_EXTENSION);

            return deploymentEngine.getDeployer(synapseLibraryPath, SYNAPSE_LIBRARY_EXTENSION);
        } catch (Exception e) {
            log.error("Error occured while getting the deployer");
            return null;
        }
    }

    /**
     * Get the library artifact name.
     *
     * @param axisConfig AxisConfiguration of the current tenant
     */
    private static String getArtifactName(String filePath, AxisConfiguration axisConfig) {
        SynapseArtifactDeploymentStore deploymentStore;
        deploymentStore = getSynapseConfiguration(axisConfig).getArtifactDeploymentStore();
        return deploymentStore.getArtifactNameForFile(filePath);
    }

    /**
     * Performing the action of enabling/disabling the given meidation library.
     *
     * @param libName library name
     * @param packageName package name
     * @param status current status
     * @param axisConfig AxisConfiguration of the current tenant
     * @throws AxisFault asxis fault
     */
    private static boolean updateStatus(String libQName, String libName, String packageName, String status,
                                        AxisConfiguration axisConfig) throws AxisFault {
        try {
            SynapseConfiguration synapseConfiguration = getSynapseConfiguration(axisConfig);
            SynapseImport synapseImport = synapseConfiguration.getSynapseImports().get(libQName);
            if (synapseImport == null && libName != null && packageName != null) {
                addImport(libName, packageName, axisConfig);
                synapseImport = synapseConfiguration.getSynapseImports().get(libQName);
            }
            Library synLib = synapseConfiguration.getSynapseLibraries().get(libQName);
            if (libQName != null && synLib != null) {
                if ("enabled".equals(status)) {
                    synapseImport.setStatus(true);
                    synLib.setLibStatus(true);
                    synLib.loadLibrary();
                    deployingLocalEntries(synLib, synapseConfiguration, axisConfig);
                } else {
                    synapseImport.setStatus(false);
                    synLib.setLibStatus(false);
                    synLib.unLoadLibrary();
                    undeployingLocalEntries(synLib, synapseConfiguration, axisConfig);
                }
            }

        } catch (Exception e) {
            log.error("Unable to update status for :  " + libQName, e);
        }
        return true;
    }

    /**
     * Undeploy the local entries deployed from the lib.
     *
     * @param axisConfig AxisConfiguration of the current tenant
     */
    private static void undeployingLocalEntries(Library library, SynapseConfiguration config,
                                                AxisConfiguration axisConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Start : Removing Local registry entries from the configuration");
        }
        for (Map.Entry<String, Object> libararyEntryMap : library.getLocalEntryArtifacts()
                .entrySet()) {
            File localEntryFileObj = (File) libararyEntryMap.getValue();
            OMElement document = getOMElement(localEntryFileObj);
            deleteEntry(document.toString(), axisConfig);
        }
        if (log.isDebugEnabled()) {
            log.debug("End : Removing Local registry entries from the configuration");
        }
    }

    /**
     * Remove a local entry.
     *
     * @param element element string
     * @param axisConfig AxisConfiguration of the current tenant
     */
    private static boolean deleteEntry(String element, AxisConfiguration axisConfig) {

        final Lock lock = getLock(axisConfig);
        String entryKey = null;
        try {
            lock.lock();
            OMElement elem;
            try {
                elem = nonCoalescingStringToOm(element);
            } catch (XMLStreamException e) {
                log.error("Error while converting the file content : ",  e);
                return false;
            }

            if (elem.getQName().getLocalPart().equals(XMLConfigConstants.ENTRY_ELT.getLocalPart())) {

                entryKey = elem.getAttributeValue(new QName("key"));
                entryKey = entryKey.trim();
                log.debug("Adding local entry with key : " + entryKey);

                SynapseConfiguration synapseConfiguration = getSynapseConfiguration(axisConfig);
                Entry entry = synapseConfiguration.getDefinedEntries().get(entryKey);
                if (entry != null) {
                    synapseConfiguration.removeEntry(entryKey);
                    if (log.isDebugEnabled()) {
                        log.debug("Deleted local entry with key : " + entryKey);
                    }
                    return true;
                } else {
                    log.warn("No entry exists by the key : " + entryKey);
                    return false;
                }
            }
        } catch (SynapseException syne) {
            log.error("Unable to delete the local entry : " + entryKey, syne);
        } catch (Exception e) {
            log.error("Unable to delete the local entry : " + entryKey, e);
        } finally {
            lock.unlock();
        }
        return false;
    }

    /**
     * Deploy the local entries from lib.
     *
     * @param axisConfig AxisConfiguration of the current tenant
     */
    private static void deployingLocalEntries(Library library, SynapseConfiguration config,
                                              AxisConfiguration axisConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Start : Adding Local registry entries to the configuration");
        }
        for (Map.Entry<String, Object> libararyEntryMap : library.getLocalEntryArtifacts()
                .entrySet()) {
            File localEntryFileObj = (File) libararyEntryMap.getValue();
            OMElement document = getOMElement(localEntryFileObj);
            addEntry(document.toString(), axisConfig);
        }
        if (log.isDebugEnabled()) {
            log.debug("End : Adding Local registry entries to the configuration");
        }
    }

    /**
     * Add a local entry.
     *
     * @param element ekement string
     * @param axisConfig AxisConfiguration of the current tenant
     */
    private static boolean addEntry(String element, AxisConfiguration axisConfig) {
        final Lock lock = getLock(axisConfig);
        try {
            lock.lock();
            OMElement elem;
            try {
                elem = nonCoalescingStringToOm(element);
            } catch (XMLStreamException e) {
                log.error("Error while converting the file content : ",  e);
                return false;
            }

            if (elem.getQName().getLocalPart().equals(XMLConfigConstants.ENTRY_ELT.getLocalPart())) {

                String entryKey = elem.getAttributeValue(new QName("key"));
                entryKey = entryKey.trim();
                SynapseConfiguration synapseConfiguration = getSynapseConfiguration(axisConfig);
                if (log.isDebugEnabled()) {
                    log.debug("Adding local entry with key : " + entryKey);
                }
                if (synapseConfiguration.getLocalRegistry().containsKey(entryKey)) {
                    log.error("An Entry with key " + entryKey +
                            " is already used within the configuration");
                } else {
                    Entry entry =
                            EntryFactory.createEntry(elem,
                                    synapseConfiguration.getProperties());
                    entry.setFileName(generateFileName(entry.getKey()));
                    synapseConfiguration.addEntry(entryKey, entry);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Local registry entry : " + entryKey + " added to the configuration");
                }
                return true;
            } else {
                log.warn("Error adding local entry. Invalid definition");
            }
        } catch (SynapseException syne) {
            log.error("Unable to add local entry ", syne);
        } catch (OMException e) {
            log.error("Unable to add local entry. Invalid XML ", e);
        } catch (Exception e) {
            log.error("Unable to add local entry. Invalid XML ", e);
        } finally {
            lock.unlock();
        }
        return false;
    }

    /**
     * Convert string as OMElement.
     *
     * @param xmlStr string  which want to convert as OMElement
     * @return coverted OMElement
     */
    private static OMElement nonCoalescingStringToOm(String xmlStr) throws XMLStreamException {
        StringReader strReader = new StringReader(xmlStr);
        XMLInputFactory xmlInFac = XMLInputFactory.newInstance();
        xmlInFac.setProperty("javax.xml.stream.isCoalescing", false);
        XMLStreamReader parser = xmlInFac.createXMLStreamReader(strReader);
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        return builder.getDocumentElement();
    }

    /**
     * Acquires the lock for parameters.
     *
     * @param axisConfig AxisConfiguration instance
     * @return Lock instance
     */
    private static Lock getLock(AxisConfiguration axisConfig) {
        Parameter parameter = axisConfig.getParameter(SYNAPSE_CONFIG_LOCK);
        if (parameter != null) {
            return (Lock) parameter.getValue();
        } else {
            log.warn(SYNAPSE_CONFIG_LOCK + " is null, Recreating a new lock");
            Lock lock = new ReentrantLock();
            try {
                axisConfig.addParameter(SYNAPSE_CONFIG_LOCK, lock);
                return lock;
            } catch (AxisFault axisFault) {
                log.error("Error while setting " + SYNAPSE_CONFIG_LOCK, axisFault);
            }
        }

        return null;
    }

    /**
     * Get OMElement from the file.
     *
     * @param file file which want to convert as OMElement
     * @return file data as OMElement
     */
    private static OMElement getOMElement(File file) {
        OMElement document = null;

        FileInputStream is;
        try {
            is = FileUtils.openInputStream(file);
        } catch (IOException e) {
            log.error("Error while opening the file: " + file.getName() + " for reading", e);
            return null;
        }

        try {
            document = (new StAXOMBuilder(is)).getDocumentElement();
            document.build();
            is.close();
        } catch (XMLStreamException e) {
            log.error("Error while parsing the content of the file: " + file.getName(), e);
        } catch (IOException e) {
            log.warn("Error while closing the input stream from the file: " + file.getName(), e);
        } catch (Exception e) {
            log.error("Error while building the content of the file: " + file.getName(), e);
        }

        return document;
    }

    /**
     * Helper method to retrieve the Synapse configuration from the relevant axis configuration.
     *
     * @param axisConfig AxisConfiguration of the current tenant
     * @return extracted SynapseConfiguration from the relevant AxisConfiguration
     */
    private static SynapseConfiguration getSynapseConfiguration(AxisConfiguration axisConfig) {
        return (SynapseConfiguration) axisConfig.getParameter(
                SynapseConstants.SYNAPSE_CONFIG).getValue();
    }

    /**
     * Performing the action of importing the given meidation library.
     *
     * @param libName library name
     * @param packageName package name
     * @param axisConfig AxisConfiguration of the current tenant
     * @throws AxisFault exception
     */
    private static void addImport(String libName, String packageName, AxisConfiguration axisConfig) throws AxisFault {
        SynapseImport synImport = new SynapseImport();
        synImport.setLibName(libName);
        synImport.setLibPackage(packageName);
        OMElement impEl = SynapseImportSerializer.serializeImport(synImport);
        if (impEl != null) {
            try {
                addImport(impEl.toString(), axisConfig);
            } catch (AxisFault axisFault) {
                log.error("Could not add Synapse Import", axisFault);
            }
        } else {
            log.error("Could not add Synapse Import. Invalid import params for libName : " +
                    libName + " packageName : " + packageName);
        }
    }

    /**
     * Get an XML configuration element for a message processor from the FE and
     * creates and add the MessageStore to the synapse configuration.
     *
     * @param xml string that contain the message processor configuration.
     * @param axisConfig AxisConfiguration of the current tenant
     * @throws AxisFault if some thing goes wrong when creating a MessageProcessor with the given xml.
     */
    private static void addImport(String xml, AxisConfiguration axisConfig) throws AxisFault {
        try {
            OMElement imprtElem = createElement(xml);
            SynapseImport synapseImport = SynapseImportFactory.createImport(imprtElem, null);
            if (synapseImport != null && synapseImport.getName() != null) {
                SynapseConfiguration synapseConfiguration = getSynapseConfiguration(axisConfig);
                String fileName = generateFileName(synapseImport.getName());
                synapseImport.setFileName(fileName);
                synapseConfiguration.addSynapseImport(synapseImport.getName(), synapseImport);
                String synImportQualfiedName = LibDeployerUtils.getQualifiedName(synapseImport);
                Library synLib =
                        synapseConfiguration.getSynapseLibraries()
                                .get(synImportQualfiedName);
                if (synLib != null) {
                    LibDeployerUtils.loadLibArtifacts(synapseImport, synLib);
                }
            } else {
                log.error("Unable to create a Synapse Import for :  " + xml, null);
            }

        } catch (XMLStreamException e) {
            log.error("Unable to create a Synapse Import for :  " + xml, e);
        }

    }

    /**
     * Creates an <code>OMElement</code> from the given string.
     *
     * @param str the XML string
     * @return the <code>OMElement</code> representation of the given string
     * @throws XMLStreamException if building the <code>OmElement</code> is unsuccessful
     */
    private static OMElement createElement(String str) throws XMLStreamException {
        InputStream in = new ByteArrayInputStream(str.getBytes());
        return new StAXOMBuilder(in).getDocumentElement();
    }

    /**
     * Generate a file name from input string.
     *
     * @param name name of the input
     * @return file name of the input string
     */
    private static String generateFileName(String name) {
        return name.replaceAll("[\\/?*|:<> ]", "_") + ".xml";
    }

    /**
     * Create a directory for given path.
     *
     * @param path path of the directory
     */
    private static void createDir(String path) {
        File temp = new File(path);
        if (!temp.exists() && !temp.mkdir()) {
            log.error("Error while creating directory : " + path);
        }
    }
}
