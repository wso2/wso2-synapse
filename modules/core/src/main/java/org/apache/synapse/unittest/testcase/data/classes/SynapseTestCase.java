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

import org.apache.synapse.unittest.testcase.data.holders.ArtifactData;
import org.apache.synapse.unittest.testcase.data.holders.TestCaseData;

/**
 * Class responsible for holding artifact, test case and mock services data.
 *
 */
public class SynapseTestCase {

    private ArtifactData artifacts;
    private TestCaseData testCases;

    /**
     * Get artifact data holder.
     *
     * @return artifact data holder
     */
    public ArtifactData getArtifacts() {
        return artifacts;
    }

    /**
     * Get test case data holder.
     *
     * @return test case data holder
     */
    public TestCaseData getTestCases() {
        return testCases;
    }

    /**
     * Set artifact data holder.
     *
     * @param artifacts artifact data holder
     */
    public void setArtifacts(ArtifactData artifacts) {
        this.artifacts = artifacts;
    }

    /**
     * Set test case data holder.
     *
     * @param testCases test case data holder
     */
    public void setTestCases(TestCaseData testCases) {
        this.testCases = testCases;
    }

}
