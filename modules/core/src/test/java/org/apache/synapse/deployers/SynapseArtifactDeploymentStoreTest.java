/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.deployers;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for SynapseArtifactDeploymentStore
 */
public class SynapseArtifactDeploymentStoreTest {
    /**
     * Test adding of artifact
     * @throws Exception
     */
    @Test
    public void testAddArtifact() throws Exception{
        SynapseArtifactDeploymentStore synapseArtifactDeploymentStore = new SynapseArtifactDeploymentStore();
        String sampleArtifact = "sampleArtifact";
        String sampleFile = "sampleFile";
        synapseArtifactDeploymentStore.addArtifact(sampleFile, sampleArtifact);
        Assert.assertTrue("Artifact not added", synapseArtifactDeploymentStore.containsFileName(sampleFile));
    }

    /**
     * Test removing of artifact
     * @throws Exception
     */
    @Test
    public void testRemoveArtifact() throws Exception{
        SynapseArtifactDeploymentStore synapseArtifactDeploymentStore = new SynapseArtifactDeploymentStore();
        String sampleArtifact = "sampleArtifact";
        String sampleFile = "sampleFile";
        synapseArtifactDeploymentStore.removeArtifactWithFileName(sampleFile);
        Assert.assertFalse("Artifact not removed", synapseArtifactDeploymentStore.containsFileName(sampleFile));
    }

    /**
     * Test adding of an updated artifact
     * @throws Exception
     */
    @Test
    public void testAddUpdatingArtifact() throws Exception{
        SynapseArtifactDeploymentStore synapseArtifactDeploymentStore = new SynapseArtifactDeploymentStore();
        String sampleArtifact = "sampleArtifact";
        String sampleFile = "sampleFile";
        synapseArtifactDeploymentStore.addUpdatingArtifact(sampleFile, sampleArtifact);
        Assert.assertTrue("Updated Artifact not added", synapseArtifactDeploymentStore.isUpdatingArtifact(sampleFile));
    }

    /**
     * Test removing of an updated artifact
     * @throws Exception
     */
    @Test
    public void testRemoveUpdatingArtifact() throws Exception{
        SynapseArtifactDeploymentStore synapseArtifactDeploymentStore = new SynapseArtifactDeploymentStore();
        String sampleArtifact = "sampleArtifact";
        String sampleFile = "sampleFile";
        synapseArtifactDeploymentStore.removeUpdatingArtifact(sampleFile);
        Assert.assertFalse("Updated Artifact not removed", synapseArtifactDeploymentStore.isUpdatingArtifact(sampleFile));
    }

    /**
     * Test adding of a restored artifact
     * @throws Exception
     */
    @Test
    public void testAddRestoredArtifact() throws Exception{
        SynapseArtifactDeploymentStore synapseArtifactDeploymentStore = new SynapseArtifactDeploymentStore();
        String sampleFile = "sampleFile";
        synapseArtifactDeploymentStore.addRestoredArtifact(sampleFile);
        Assert.assertTrue("Restored Artifact not added", synapseArtifactDeploymentStore.isRestoredFile(sampleFile));
    }

    /**
     * Test removing of a restored artifact
     * @throws Exception
     */
    @Test
    public void testRemoveRestoredArtifact() throws Exception{
        SynapseArtifactDeploymentStore synapseArtifactDeploymentStore = new SynapseArtifactDeploymentStore();
        String sampleFile = "sampleFile";
        synapseArtifactDeploymentStore.removeRestoredFile(sampleFile);
        Assert.assertFalse("Restored Artifact not removed", synapseArtifactDeploymentStore.isUpdatingArtifact(sampleFile));
    }

    /**
     * Test adding of a backed-up artifact
     * @throws Exception
     */
    @Test
    public void testAddBackedupArtifact() throws Exception{
        SynapseArtifactDeploymentStore synapseArtifactDeploymentStore = new SynapseArtifactDeploymentStore();
        String sampleFile = "sampleFile";
        synapseArtifactDeploymentStore.addBackedUpArtifact(sampleFile);
        Assert.assertTrue("Backed-up Artifact not added", synapseArtifactDeploymentStore.isBackedUpArtifact(sampleFile));
    }

    /**
     * Test removing of a backed-up artifact
     * @throws Exception
     */
    @Test
    public void testRemoveBackedupArtifact() throws Exception{
        SynapseArtifactDeploymentStore synapseArtifactDeploymentStore = new SynapseArtifactDeploymentStore();
        String sampleFile = "sampleFile";
        synapseArtifactDeploymentStore.removeBackedUpArtifact(sampleFile);
        Assert.assertFalse("Backed-up Artifact not removed", synapseArtifactDeploymentStore.isBackedUpArtifact(sampleFile));
    }
}