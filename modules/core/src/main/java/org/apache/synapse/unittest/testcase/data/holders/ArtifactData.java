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
package org.apache.synapse.unittest.testcase.data.holders;

import org.apache.synapse.unittest.testcase.data.classes.Artifact;
import org.apache.synapse.unittest.testcase.data.classes.RegistryResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class responsible for the holding the data of synapse configuration data.
 */
public class ArtifactData {

    private int supportiveArtifactCount;
    private Artifact testArtifact;
    private ArrayList<Artifact> supportiveArtifacts = new ArrayList<>();
    private Map<String, RegistryResource> registryResources = new HashMap<>();
    private ArrayList<String> connectorResources = new ArrayList<>();

    /**
     * Set supportive-artifact.
     *
     * @param supportiveArtifactCount receiving supportive artifact count
     */
    public void setSupportiveArtifactCount(int supportiveArtifactCount) {
        this.supportiveArtifactCount = supportiveArtifactCount;
    }

    /**
     * Set artifact.
     *
     * @param testArtifact receiving test-artifact
     */
    public void setTestArtifact(Artifact testArtifact) {
        this.testArtifact = testArtifact;
    }

    /**
     * Add supportive-artifact.
     *
     * @param supportiveArtifacts receiving test-artifact
     */
    public void addSupportiveArtifact(Artifact supportiveArtifacts) {
        this.supportiveArtifacts.add(supportiveArtifacts);
    }

    /**
     * Add registry-resources.
     *
     * @param key registry resource key
     * @param registryResource registry resource object
     */
    public void addRegistryResource(String key, RegistryResource registryResource) {
        this.registryResources.put(key, registryResource);
    }

    /**
     * Add connector-resources.
     *
     * @param base64Encode connector resource key
     */
    public void addConnectorResource(String base64Encode) {
        this.connectorResources.add(base64Encode);
    }

    /**
     * Get test-artifact.
     *
     * @return testArtifact
     */
    public Artifact getTestArtifact() {
        return testArtifact;
    }

    /**
     * Get supportive-artifact.
     *
     * @return supportive-Artifact
     */
    public Artifact getSupportiveArtifact(int elementIndex) {
        return supportiveArtifacts.get(elementIndex);
    }

    /**
     * Get registry resource.
     *
     * @return registry resource
     */
    public RegistryResource getRegistryResource(String key) {
        return registryResources.get(key);
    }

    /**
     * Get registry resources.
     *
     * @return registry resources
     */
    public Map<String, RegistryResource> getRegistryResources() {
        return registryResources;
    }

    /**
     * Get connector resources.
     *
     * @return connector resources
     */
    public List<String> getConnectorResources() {
        return connectorResources;
    }

    /**
     * Get supportive-artifact count.
     *
     * @return count of supportive artifact
     */
    public int getSupportiveArtifactCount() {
        return supportiveArtifactCount;
    }
}
