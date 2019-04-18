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

import javafx.util.Pair;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.deployers.SequenceDeployer;
import org.apache.synapse.deployers.ProxyServiceDeployer;
import org.apache.synapse.deployers.APIDeployer;
import org.apache.synapse.deployers.EndpointDeployer;
import org.apache.synapse.deployers.LocalEntryDeployer;

/**
 * Util class for deploying synapse artifacts to the synapse engine.
 */
class ConfigurationDeployer {

    /**
     * Method of deploying sequence artifact in synapse.
     *
     * @param inputElement synapse configuration artifact as OMElement type
     * @param fileName     name of the file
     * @return response of the artifact deployment and the synapse configuration as a Pair<>
     */
    Pair<SynapseConfiguration, String> deploySequenceArtifact(OMElement inputElement, String fileName)
            throws AxisFault {

        //create new sequence deployer object
        SequenceDeployer sequenceDeployer = new SequenceDeployer();

        //create a synapse configuration and set all axis2 configuration to it
        SynapseConfiguration synapseConfiguration = UnitTestingExecutor.getExecuteInstance().getSynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext configurationContext = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(configurationContext, synapseConfiguration);

        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        configurationContext.setAxisConfiguration(axisConfiguration);

        //initialize sequence deployer using created configuration context
        sequenceDeployer.init(configurationContext);

        //deploy synapse artifact
        String deployedArtifact = sequenceDeployer.deploySynapseArtifact(inputElement, fileName, null);

        return new Pair<>(synapseConfiguration, deployedArtifact);
    }

    /**
     * Method of deploying proxy artifact in synapse.
     *
     * @param inputElement synapse configuration artifact as OMElement type
     * @param fileName     name of the file
     * @return response of the artifact deployment and the synapse configuration as a Pair<>
     */
    Pair<SynapseConfiguration, String> deployProxyArtifact(OMElement inputElement, String fileName)
            throws AxisFault {

        //create new proxy service deployer object
        ProxyServiceDeployer proxyServiceDeployer = new ProxyServiceDeployer();

        //create a synapse configuration and set all axis2 configuration to it
        SynapseConfiguration synapseConfiguration = UnitTestingExecutor.getExecuteInstance().getSynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext configurationContext = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(configurationContext, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        configurationContext.setAxisConfiguration(axisConfiguration);

        //initialize proxy service deployer using created configuration context
        proxyServiceDeployer.init(configurationContext);

        //deploy synapse artifact
        String deployedArtifact = proxyServiceDeployer.deploySynapseArtifact(inputElement, fileName, null);

        return new Pair<>(synapseConfiguration, deployedArtifact);
    }

    /**
     * Method of deploying API artifact in synapse.
     *
     * @param inputElement synapse configuration artifact as OMElement type
     * @param fileName     name of the file
     * @return response of the artifact deployment and the synapse configuration as a Pair<>
     */
    Pair<SynapseConfiguration, String> deployApiArtifact(OMElement inputElement, String fileName)
            throws AxisFault {

        //create new API deployer object
        APIDeployer apiResourceDeployer = new APIDeployer();

        //create a synapse configuration and set all axis2 configuration to it
        SynapseConfiguration synapseConfiguration = UnitTestingExecutor.getExecuteInstance().getSynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext configurationContext = new ConfigurationContext(axisConfiguration);

        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(configurationContext, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        configurationContext.setAxisConfiguration(axisConfiguration);

        //initialize API deployer using created configuration context
        apiResourceDeployer.init(configurationContext);

        //deploy synapse artifact
        String deployedArtifact = apiResourceDeployer.deploySynapseArtifact(inputElement, fileName, null);

        return new Pair<>(synapseConfiguration, deployedArtifact);
    }

    /**
     * Method of deploying endpoint artifact in synapse.
     *
     * @param inputElement synapse configuration artifact as OMElement type
     * @param fileName     name of the file
     * @return response of the artifact deployment and the synapse configuration as a Pair<>
     */
    Pair<SynapseConfiguration, String> deployEndpointArtifact(OMElement inputElement, String fileName)
            throws AxisFault {

        //create new sequence deployer object
        EndpointDeployer endpointDeployer = new EndpointDeployer();

        //create a synapse configuration and set all axis2 configuration to it
        SynapseConfiguration synapseConfiguration = UnitTestingExecutor.getExecuteInstance().getSynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext configurationContext = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(configurationContext, synapseConfiguration);

        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        configurationContext.setAxisConfiguration(axisConfiguration);

        //initialize sequence deployer using created configuration context
        endpointDeployer.init(configurationContext);

        //deploy synapse artifact
        String deployedArtifact = endpointDeployer.deploySynapseArtifact(inputElement, fileName, null);

        return new Pair<>(synapseConfiguration, deployedArtifact);
    }

    /**
     * Method of deploying endpoint artifact in synapse.
     *
     * @param inputElement synapse configuration artifact as OMElement type
     * @param fileName     name of the file
     * @return response of the artifact deployment and the synapse configuration as a Pair<>
     */
    Pair<SynapseConfiguration, String> deployLocalEntryArtifact(OMElement inputElement, String fileName)
            throws AxisFault {

        //create new sequence deployer object
        LocalEntryDeployer localEntryDeployer = new LocalEntryDeployer();

        //create a synapse configuration and set all axis2 configuration to it
        SynapseConfiguration synapseConfiguration = UnitTestingExecutor.getExecuteInstance().getSynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext configurationContext = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(configurationContext, synapseConfiguration);

        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        configurationContext.setAxisConfiguration(axisConfiguration);

        //initialize sequence deployer using created configuration context
        localEntryDeployer.init(configurationContext);

        //deploy synapse artifact
        String deployedArtifact = localEntryDeployer.deploySynapseArtifact(inputElement, fileName, null);

        return new Pair<>(synapseConfiguration, deployedArtifact);
    }
}
