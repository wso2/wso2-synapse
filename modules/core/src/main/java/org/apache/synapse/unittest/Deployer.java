/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.axis2.Initializer;
import org.apache.synapse.deployers.ProxyServiceDeployer;
import org.apache.synapse.deployers.SequenceDeployer;

/**
 * Util class for deploying synapse artifacts to the synapse engine
 */

public class Deployer {

    public Pair<SynapseConfiguration, String> deploySequence(OMElement inputElement, String fileName) throws Exception {

        SequenceDeployer sequenceDeployer = new SequenceDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);

        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        sequenceDeployer.init(cfgCtx);
        String deployedArtifact = sequenceDeployer.deploySynapseArtifact(inputElement, fileName, null);

        return new Pair<>(synapseConfiguration, deployedArtifact);
    }

    public Pair<SynapseConfiguration, String> deployProxy(OMElement inputElement, String fileName) throws Exception {

        ProxyServiceDeployer proxyServiceDeployer = new ProxyServiceDeployer();

        SynapseConfiguration synapseConfiguration = Initializer.getInstance().getSynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        proxyServiceDeployer.init(cfgCtx);
        String deployedArtifact = proxyServiceDeployer.deploySynapseArtifact(inputElement, fileName, null);

        return new Pair<>(synapseConfiguration, deployedArtifact);
    }
}
