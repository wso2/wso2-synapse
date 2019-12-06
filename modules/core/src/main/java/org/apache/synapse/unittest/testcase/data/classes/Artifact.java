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

package org.apache.synapse.unittest.testcase.data.classes;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;

import javax.xml.stream.XMLStreamException;

public class Artifact {

    private String artifactType;
    private OMElement artifactData;
    private String artifactNameOrKey;
    private String transportMethod;

    /**
     * Get artifact type.
     *
     * @return artifact type in descriptor data
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Get artifact.
     *
     * @return artifact in descriptor data
     */
    public OMElement getArtifact() {
        return artifactData;
    }

    /**
     * Get artifact name or key.
     *
     * @return artifact name in descriptor data
     */
    public String getArtifactNameOrKey() {
        return artifactNameOrKey;
    }

    /**
     * Get artifact transport method.
     *
     * @return artifact transport method in descriptor data
     */
    public String getTransportMethod() {
        return transportMethod;
    }

    /**
     * Set test-artifact type.
     *
     * @param artifactType type of the artifact in descriptor data
     */
    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    /**
     * Set test-artifact.
     *
     * @param artifact receiving artifact in descriptor data
     */
    public void setArtifact(String artifact) throws XMLStreamException {
        this.artifactData = AXIOMUtil.stringToOM(artifact);
    }

    /**
     * Set test-artifact name or key.
     *
     * @param artifactNameOrKey name of the artifact in descriptor data
     */
    public void setArtifactNameOrKey(String artifactNameOrKey) {
        this.artifactNameOrKey = artifactNameOrKey;
    }

    /**
     * Set test-artifact transport method.
     *
     * @param transportMethod transport method of the artifact in descriptor data
     */
    public void setTransportMethod(String transportMethod) {
        this.transportMethod = transportMethod;
    }
}
