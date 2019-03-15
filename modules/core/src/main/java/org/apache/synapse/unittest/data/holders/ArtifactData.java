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

/**
 * Class responsible for the holding the data of synapse configuration data.
 */
public class ArtifactData {

    private String artifactType;
    private String artifact;
    private String artifactName;

    /**
     * Get artifact type.
     *
     * @return artifact type in descriptor data
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Get artifact name.
     *
     * @return artifact name in descriptor data
     */
    public String getArtifactName() {
        return artifactName;
    }

    /**
     * Get artifact.
     *
     * @return artifact in descriptor data
     */
    public String getArtifact() {
        return artifact;
    }

    /**
     * Set artifact type.
     *
     * @param artifactType type of the artifact in descriptor data
     */
    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    /**
     * Set artifact name.
     *
     * @param artifactName name of the artifact in descriptor data
     */
    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    /**
     * Set artifact.
     *
     * @param artifact receiving artifact in descriptor data
     */
    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

}
