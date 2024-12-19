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

package org.apache.synapse.deployers;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FileUtils;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.xml.EntryFactory;
import org.apache.synapse.config.xml.EntrySerializer;
import org.apache.synapse.config.xml.MultiXMLConfigurationBuilder;
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.transport.dynamicconfigurations.KeyStoreReloaderHolder;
import org.apache.synapse.transport.nhttp.config.SslSenderTrustStoreHolder;
import org.apache.synapse.util.HTTPConnectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.namespace.QName;

/**
 *  Handles the <code>LocalEntry</code> deployment and undeployment tasks
 *
 * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer
 */
public class LocalEntryDeployer extends AbstractSynapseArtifactDeployer {

    private static Log log = LogFactory.getLog(LocalEntryDeployer.class);
    private static final String RESOURCES_IDENTIFIER = "resources:";
    private static final String CONVERTED_RESOURCES_IDENTIFIER = "gov:mi-resources" + File.separator;
    private static final String HTTP_CONNECTION_IDENTIFIER = "http.init";
    private static final String CERTIFICATE_EXTENSION = ".crt";

    @Override
    public String deploySynapseArtifact(OMElement artifactConfig, String fileName,
                                        Properties properties) {

        if (log.isDebugEnabled()) {
            log.debug("LocalEntry Deployment from file : " + fileName + " : Started");
        }

        try {
            Entry e = EntryFactory.createEntry(artifactConfig, properties);
            if (e != null) {
                /**
                 * Set the artifact container name of the local entry
                 */
                e.setArtifactContainerName(customLogContent);
                e.setFileName((new File(fileName)).getName());
                if (log.isDebugEnabled()) {
                    log.debug("LocalEntry with key '" + e.getKey()
                            + "' has been built from the file " + fileName);
                }
                getSynapseConfiguration().addEntry(e.getKey(), e);
                if (log.isDebugEnabled()) {
                    log.debug("LocalEntry Deployment from file : " + fileName + " : Completed");
                }
                log.info("LocalEntry named '" + e.getKey()
                        + "' has been deployed from file : " + fileName);
                handleSSLSenderCertificates(artifactConfig);
                deployEndpointsForHTTPConnection(artifactConfig, fileName, properties);
                return e.getKey();
            } else {
                handleSynapseArtifactDeploymentError("LocalEntry Deployment Failed. The artifact " +
                        "described in the file " + fileName + " is not a LocalEntry");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "LocalEntry Deployment from the file : " + fileName + " : Failed.", e);
        }

        return null;
    }

    private void deployEndpointsForHTTPConnection(OMElement artifactConfig, String fileName, Properties properties) {

        OMElement httpInitElement =
                artifactConfig.getFirstChildWithName(
                        new QName(SynapseConstants.SYNAPSE_NAMESPACE, HTTP_CONNECTION_IDENTIFIER));
        if (httpInitElement != null) {
            OMElement generatedEndpointElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(httpInitElement);
            deployHTTPEndpointForElement(generatedEndpointElement, fileName, properties);
        }
    }

    private void deployHTTPEndpointForElement(OMElement documentElement, String fileName, Properties properties) {

        try {
            Endpoint ep = EndpointFactory.getEndpointFromElement(documentElement, false, properties);

            //Set the car name
            ep.setArtifactContainerName(customLogContent);
            if (ep != null) {
                ep.setFileName((new File(fileName)).getName());
                if (log.isDebugEnabled()) {
                    log.debug("Endpoint named '" + ep.getName()
                            + "' has been built from the http connection file " + fileName);
                }
                ep.init(getSynapseEnvironment());
                if (log.isDebugEnabled()) {
                    log.debug("Initialized the endpoint : " + ep.getName());
                }
                getSynapseConfiguration().addEndpoint(ep.getName(), ep);
                if (log.isDebugEnabled()) {
                    log.debug("Endpoint Deployment from the http connection file : " + fileName + " : Completed");
                }
                log.info("Endpoint named '" + ep.getName()
                        + "' has been deployed from the http connection file : " + fileName);
            } else {
                handleSynapseArtifactDeploymentError("Endpoint Deployment Failed. The artifact " +
                        "described in the http connection file " + fileName + " has filed to describe an Endpoint");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError("Endpoint Deployment from the http connection file : "
                    + fileName + " : Failed.", e);
        }
    }

    private void handleSSLSenderCertificates(OMElement element) throws DeploymentException {

        OMElement httpInitElement =
                element.getFirstChildWithName(new QName(SynapseConstants.SYNAPSE_NAMESPACE, HTTP_CONNECTION_IDENTIFIER));
        if (httpInitElement != null) {
            Iterator childElementIterator = httpInitElement.getChildElements();
            while (childElementIterator.hasNext()) {
                OMElement childElement = (OMElement) childElementIterator.next();
                String childElementValue = childElement.getText();
                String transformedElementValue = getTransformedElementValue(childElementValue);
                if (transformedElementValue.endsWith(CERTIFICATE_EXTENSION)) {
                    loadCertificateFileToSSLSenderTrustStore(transformedElementValue);
                    loadUpdatedSSL();
                }
            }
        }
    }

    private void loadCertificateFileToSSLSenderTrustStore(String certificateFileResourceKey) throws DeploymentException {

        String certificateFilePath = getSynapseConfiguration().getRegistry().getRegistryEntry(certificateFileResourceKey).getName();
        File certificateFile = new File(certificateFilePath);
        String certificateAlias = certificateFile.getName().split("\\.")[0];
        SslSenderTrustStoreHolder sslSenderTrustStoreHolder = SslSenderTrustStoreHolder.getInstance();
        if (sslSenderTrustStoreHolder.isValid()) {
            try (FileInputStream certificateFileInputStream = FileUtils.openInputStream(new File(certificateFilePath))) {
                KeyStore sslSenderTrustStore = sslSenderTrustStoreHolder.getKeyStore();

                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Certificate certificate = certificateFactory.generateCertificate(certificateFileInputStream);
                sslSenderTrustStore.setCertificateEntry(certificateAlias, certificate);

                try (FileOutputStream fileOutputStream = new FileOutputStream(sslSenderTrustStoreHolder.getLocation())) {
                    sslSenderTrustStore.store(fileOutputStream, sslSenderTrustStoreHolder.getPassword().toCharArray());
                }
            } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
                throw new DeploymentException("Failed to load certificate file to store: " + certificateFilePath, e);
            }
        }
    }

    private void loadUpdatedSSL() throws DeploymentException {
        SslSenderTrustStoreHolder sslSenderTrustStoreHolder = SslSenderTrustStoreHolder.getInstance();
        KeyStore sslSenderTrustStore = sslSenderTrustStoreHolder.getKeyStore();
        if (sslSenderTrustStoreHolder.isValid()) {
            try (
                    FileInputStream fileInputStream = new FileInputStream(sslSenderTrustStoreHolder.getLocation());
                    InputStream bufferedInputStream = IOUtils.toBufferedInputStream(fileInputStream)
            ) {
                sslSenderTrustStore.load(bufferedInputStream, sslSenderTrustStoreHolder.getPassword().toCharArray());
                sslSenderTrustStoreHolder.setKeyStore(sslSenderTrustStore);
                KeyStoreReloaderHolder.getInstance().reloadAllKeyStores();
            } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
                throw new DeploymentException("Failed to load updated SSL configuration from the trust store at: " + sslSenderTrustStoreHolder.getLocation(), e);
            }
        }
    }

    /**
     * Transforms the given element value if it indicates a resource file.
     *
     * @param elementValue the value of the element to be transformed
     * @return the transformed element value
     */
    private String getTransformedElementValue(String elementValue) {
        String transformedElementValue = elementValue.trim();
        if (transformedElementValue.startsWith(RESOURCES_IDENTIFIER)) {
            transformedElementValue = transformedElementValue.replace(RESOURCES_IDENTIFIER, CONVERTED_RESOURCES_IDENTIFIER);
        }
        return transformedElementValue;
    }

    @Override
    public String updateSynapseArtifact(OMElement artifactConfig, String fileName,
                                        String existingArtifactName, Properties properties) {

        if (log.isDebugEnabled()) {
            log.debug("LocalEntry update from file : " + fileName + " has started");
        }

        try {
            Entry e = EntryFactory.createEntry(artifactConfig, properties);
            if (e == null) {
                handleSynapseArtifactDeploymentError("Local entry update failed. The artifact " +
                        "defined in the file: " + fileName + " is not a valid local entry.");
                return null;
            }
            e.setFileName(new File(fileName).getName());

            if (log.isDebugEnabled()) {
                log.debug("Local entry: " + e.getKey() + " has been built from the file: " + fileName);
            }

            if (existingArtifactName.equals(e.getKey())) {
                getSynapseConfiguration().updateEntry(existingArtifactName, e);
            } else {
                // The user has changed the name of the entry
                // We should add the updated entry as a new entry and remove the old one
                getSynapseConfiguration().addEntry(e.getKey(), e);
                getSynapseConfiguration().removeEntry(existingArtifactName);
                log.info("Local entry: " + existingArtifactName + " has been undeployed");
            }

            log.info("Endpoint: " + e.getKey() + " has been updated from the file: " + fileName);
            return e.getKey();

        } catch (DeploymentException e) {
            handleSynapseArtifactDeploymentError("Error while updating the local entry from the " +
                    "file: " + fileName);
        }

        return null;
    }

    @Override
    public void undeploySynapseArtifact(String artifactName) {

        if (log.isDebugEnabled()) {
            log.debug("LocalEntry Undeployment of the entry named : "
                    + artifactName + " : Started");
        }
        
        try {
            Entry e = getSynapseConfiguration().getDefinedEntries().get(artifactName);
            if (e != null && e.getType() != Entry.REMOTE_ENTRY) {
                getSynapseConfiguration().removeEntry(artifactName);
                if (log.isDebugEnabled()) {
                    log.debug("LocalEntry Undeployment of the entry named : "
                            + artifactName + " : Completed");
                }
                log.info("LocalEntry named '" + e.getKey() + "' has been undeployed");
            } else if (log.isDebugEnabled()) {
                log.debug("Local entry " + artifactName + " has already been undeployed");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "LocalEntry Undeployement of entry named : " + artifactName + " : Failed", e);
        }
    }

    @Override
    public void restoreSynapseArtifact(String artifactName) {

        if (log.isDebugEnabled()) {
            log.debug("LocalEntry the Sequence with name : " + artifactName + " : Started");
        }

        try {
            Entry e = getSynapseConfiguration().getDefinedEntries().get(artifactName);
            OMElement entryElem = EntrySerializer.serializeEntry(e, null);
            if (e.getFileName() != null) {
                String fileName = getServerConfigurationInformation().getSynapseXMLLocation()
                        + File.separator + MultiXMLConfigurationBuilder.LOCAL_ENTRY_DIR
                        + File.separator + e.getFileName();
                writeToFile(entryElem, fileName);
                if (log.isDebugEnabled()) {
                    log.debug("Restoring the LocalEntry with name : " + artifactName + " : Completed");
                }
                log.info("LocalEntry named '" + artifactName + "' has been restored");
            } else {
                handleSynapseArtifactDeploymentError("Couldn't restore the LocalEntry named '"
                        + artifactName + "', filename cannot be found");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "Restoring of the LocalEntry named '" + artifactName + "' has failed", e);
        }
    }
}
