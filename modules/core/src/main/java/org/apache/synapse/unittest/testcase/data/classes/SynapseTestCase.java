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

package org.apache.synapse.unittest.testcase.data.classes;

import org.apache.synapse.unittest.testcase.data.holders.ArtifactData;
import org.apache.synapse.unittest.testcase.data.holders.MockServiceData;
import org.apache.synapse.unittest.testcase.data.holders.TestCaseData;

public class SynapseTestCase {

    private ArtifactData artifacts;
    private TestCaseData testCases;
    private MockServiceData mockServices;


    public ArtifactData getArtifacts() {
        return artifacts;
    }

    public TestCaseData getTestCases() {
        return testCases;
    }

    public MockServiceData getMockServices() {
        return mockServices;
    }

    public void setArtifacts(ArtifactData artifacts) {
        this.artifacts = artifacts;
    }

    public void setTestCases(TestCaseData testCases) {
        this.testCases = testCases;
    }

    public void setMockServices(MockServiceData mockServices) {
        this.mockServices = mockServices;
    }

}
