/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.deployers;


import org.apache.axiom.om.OMElement;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.xml.MultiXMLConfigurationBuilder;
import org.apache.synapse.config.xml.inbound.InboundEndpointFactory;
import org.apache.synapse.config.xml.inbound.InboundEndpointSerializer;
import org.apache.synapse.inbound.InboundEndpoint;

import java.io.File;
import java.util.Properties;

public class InboundEndpointDeployer extends AbstractSynapseArtifactDeployer {

    private static Log log = LogFactory.getLog(InboundEndpointDeployer.class);

    @Override
    public String deploySynapseArtifact(OMElement artifactConfig, String fileName, Properties properties) {

        if (log.isDebugEnabled()) {
            log.debug("InboundEndpoint deployment from file : " + fileName + " : Started");
        }

        /*properties are ignored @ToDo */

        try {
            InboundEndpoint inboundEndpoint = InboundEndpointFactory.createInboundEndpoint(artifactConfig);
            if (inboundEndpoint != null) {
                inboundEndpoint.setFileName(new File(fileName).getName());
                if (log.isDebugEnabled()) {
                    log.debug("Inbound Endpoint named '" + inboundEndpoint.getName()
                            + "' has been built from the file " + fileName);
                }
                inboundEndpoint.init(getSynapseEnvironment());
                if (log.isDebugEnabled()) {
                    log.debug("Initialized the Inbound Endpoint: " + inboundEndpoint.getName());
                }
                getSynapseConfiguration().addInboundEndpoint(inboundEndpoint.getName(), inboundEndpoint);
                if (log.isDebugEnabled()) {
                    log.debug("Inbound Endpoint deployment from file : " + fileName + " : Completed");
                }
                log.info("Inbound Endpoint named '" + inboundEndpoint.getName() +
                        "' has been deployed from file : " + fileName);
                return inboundEndpoint.getName();
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError("Inbound Endpoint deployment from the file : "
                    + fileName + " : Failed.", e);
        }


        return null;
    }

    @Override
    public String updateSynapseArtifact(OMElement artifactConfig, String fileName, String existingArtifactName, Properties properties) {

        if (log.isDebugEnabled()) {
            log.debug("Inbound Endpoint update from file : " + fileName + " has started");
        }

        try {
            /*properties are ignored*/
            InboundEndpoint inboundEndpoint = InboundEndpointFactory.createInboundEndpoint(artifactConfig);

            if (inboundEndpoint == null) {
                handleSynapseArtifactDeploymentError("Inbound Endpoint update failed. The artifact " +
                        "defined in the file: " + fileName + " is not a valid Inbound Endpoint.");
                return null;
            }

            inboundEndpoint.setFileName(new File(fileName).getName());

            if (log.isDebugEnabled()) {
                log.debug("Inbound Endpoint: " + inboundEndpoint.getName() + " has been built from the file: " + fileName);
            }

            inboundEndpoint.init(getSynapseEnvironment());
            InboundEndpoint existingInboundEndpoint = getSynapseConfiguration().getInboundEndpoint(existingArtifactName);

            if (existingArtifactName.equals(inboundEndpoint.getName())) {
                getSynapseConfiguration().updateInboundEndpoint(existingArtifactName, inboundEndpoint);
            } else {
                // The user has changed the name of the Inbound Endpoint
                // We should add the updated Inbound Endpoint as a new Inbound Endpoint and remove the old one
                getSynapseConfiguration().addInboundEndpoint(inboundEndpoint.getName(), inboundEndpoint);
                getSynapseConfiguration().removeInboundEndpoint(existingArtifactName);
                log.info("Inbound Endpoint: " + existingArtifactName + " has been undeployed");
            }

            log.info("Inbound Endpoint: " + inboundEndpoint.getName() + " has been updated from the file: " + fileName);

            waitForCompletion();
            existingInboundEndpoint.destroy();
            return inboundEndpoint.getName();

        } catch (DeploymentException e) {
            handleSynapseArtifactDeploymentError("Error while updating the Inbound Endpoint from the " +
                    "file: " + fileName);
        }
        return null;
    }


    public void undeploySynapseArtifact(String artifactName) {
        if (log.isDebugEnabled()) {
            log.debug("Undeployment of the Inbound Endpoint named : "
                    + artifactName + " : Started");
        }

        try {
            InboundEndpoint inboundEndpoint = getSynapseConfiguration().getInboundEndpoint(artifactName);
            if (inboundEndpoint != null) {
                getSynapseConfiguration().removeInboundEndpoint(artifactName);
                if (log.isDebugEnabled()) {
                    log.debug("Undeployment of the Inbound Endpoint named : "
                            + artifactName + " : Completed");
                }
                log.info("Inbound Endpoint named '" + inboundEndpoint.getName() + "' has been undeployed");
            } else if (log.isDebugEnabled()) {
                log.debug("Inbound Endpoint " + artifactName + " has already been undeployed");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "Undeployment of Inbound Endpoint named : " + artifactName + " : Failed", e);
        }
    }

    @Override
    public void restoreSynapseArtifact(String artifactName) {

        if (log.isDebugEnabled()) {
            log.debug("Restoring the Inbound Endpoint with name : " + artifactName + " : Started");
        }

        try {
            InboundEndpoint inboundEndpoint = getSynapseConfiguration().getInboundEndpoint(artifactName);
            OMElement inboundEndpointElement = InboundEndpointSerializer.serializeInboundEndpoint(inboundEndpoint);
            if (inboundEndpoint.getFileName() != null) {
                String fileName = getServerConfigurationInformation().getSynapseXMLLocation()
                        + File.separator + MultiXMLConfigurationBuilder.INBOUND_ENDPOINT_DIR
                        + File.separator + inboundEndpoint.getFileName();
                writeToFile(inboundEndpointElement, fileName);
                if (log.isDebugEnabled()) {
                    log.debug("Restoring the Inbound Endpoint with name : " + artifactName + " : Completed");
                }
                log.info("Inbound Endpoint named '" + artifactName + "' has been restored");
            } else {
                handleSynapseArtifactDeploymentError("Couldn't restore the Inbound Endpoint named '"
                        + artifactName + "', filename cannot be found");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "Restoring of the Inbound Endpoint named '" + artifactName + "' has failed", e);
        }
    }
}