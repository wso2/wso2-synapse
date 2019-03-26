/*
 Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.unittest.data.holders;

import java.util.ArrayList;

/**
 * Class responsible for the holding the data of synapse configuration data.
 */
public class ArtifactData {

    //test-artifact data
    private String testArtifactType;
    private String testArtifact;
    private String testArtifactNameOrKey;

    //supportive-artifacts data
    private ArrayList<String> supportiveArtifact = new ArrayList<>();
    private ArrayList<String> supportiveArtifactNameOrKey = new ArrayList<>();
    private ArrayList<String> supportiveArtifactType = new ArrayList<>();

    /**
     * Get test-artifact type.
     *
     * @return artifact type in descriptor data
     */
    public String getTestArtifactType() {
        return testArtifactType;
    }

    /**
     * Get test-artifact name or key.
     *
     * @return artifact name in descriptor data
     */
    public String getTestArtifactNameOrKey() {
        return testArtifactNameOrKey;
    }

    /**
     * Get test-artifact.
     *
     * @return artifact in descriptor data
     */
    public String getTestArtifact() {
        return testArtifact;
    }

    /**
     * Set test-artifact type.
     *
     * @param artifactType type of the artifact in descriptor data
     */
    public void setTestArtifactType(String artifactType) {
        this.testArtifactType = artifactType;
    }

    /**
     * Set test-artifact name or key.
     *
     * @param artifactName name of the artifact in descriptor data
     */
    public void setTestArtifactNameOrKey(String artifactName) {
        this.testArtifactNameOrKey = artifactName;
    }

    /**
     * Set test-artifact.
     *
     * @param artifact receiving artifact in descriptor data
     */
    public void setTestArtifact(String artifact) {
        this.testArtifact = artifact;
    }

    /**
     * Get supportive-artifact.
     *
     * @param elementIndex index of supportive artifact
     * @return artifact
     */
    public String getSupportiveArtifact(int elementIndex) {
        return supportiveArtifact.get(elementIndex);
    }

    /**
     * Get supportive-artifact name or key.
     *
     * @param elementIndex index of supportive artifact
     * @return name or key
     */
    public String getSupportiveArtifactNameOrKey(int elementIndex) {
        return supportiveArtifactNameOrKey.get(elementIndex);
    }

    /**
     * Get supportive-artifact type.
     *
     * @param elementIndex index of supportive artifact
     * @return type
     */
    public String getSupportiveArtifactType(int elementIndex) {
        return supportiveArtifactType.get(elementIndex);
    }

    /**
     * Add supportive-artifact.
     *
     * @param supportiveArtifact artifact of supportive artifact
     */
    public void addSupportiveArtifact(String supportiveArtifact) {
        this.supportiveArtifact.add(supportiveArtifact);
    }

    /**
     * Add supportive-artifact name or key.
     *
     * @param supportiveArtifactNameOrKey name or key of supportive artifact
     */
    public void addSupportiveArtifactNameOrKey(String supportiveArtifactNameOrKey) {
        this.supportiveArtifactNameOrKey.add(supportiveArtifactNameOrKey);
    }

    /**
     * Add supportive-artifact type.
     *
     * @param supportiveArtifactType type of supportive artifact
     */
    public void addSupportiveArtifactType(String supportiveArtifactType) {
        this.supportiveArtifactType.add(supportiveArtifactType);
    }
}
