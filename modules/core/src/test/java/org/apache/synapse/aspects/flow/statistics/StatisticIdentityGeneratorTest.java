/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.aspects.flow.statistics;

import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.aspects.flow.statistics.structuring.StructuringElement;
import org.apache.synapse.config.SynapseConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Unit tests for StatisticIdentityGenerator class.
 */
public class StatisticIdentityGeneratorTest {

    private static SynapseConfiguration synapseConfiguration;
    private static ArtifactHolder artifactHolder;
    private static final String PARENT = "testParent";
    private static final String COMPONENT = "testComponent";
    private static final int HASH_CODE = 1234;
    private static StructuringElement structuringElement;
    private static final String COMPONENT_ID = "testComponentId";

    @BeforeClass
    public static void init() {
        synapseConfiguration = new SynapseConfiguration();
        StatisticIdentityGenerator.setSynapseConfiguration(synapseConfiguration);
        artifactHolder = new ArtifactHolder();
        ArrayList<StructuringElement> structuringElementList = new ArrayList<>();
        structuringElement = new StructuringElement(COMPONENT_ID, ComponentType.ENDPOINT);
        structuringElementList.add(structuringElement);
        artifactHolder.setList(structuringElementList);
        artifactHolder.setParent(PARENT);
        artifactHolder.setHashCode(HASH_CODE);
        Stack<StructuringElement> structuringElementStack = new Stack<>();
        structuringElementStack.add(structuringElement);
        artifactHolder.setStack(structuringElementStack);
        artifactHolder.setId(0);
    }

    /**
     * Test conclude method by structuringArtifact creation.
     */
    @Test
    public void testConclude() {
        cleanUp();
        StatisticIdentityGenerator.conclude(artifactHolder);
        Assert.assertNotNull("New structuringArtifact should be inserted", synapseConfiguration
                .getCompletedStructureStore().getCompletedStructureEntries());
    }

    /**
     * Testing reporting FlowContinuableEndEvent.
     */
    @Test
    public void testReportingFlowContinuableEndEvent() {
        cleanUp();
        artifactHolder.setExitFromBox(false);
        StatisticIdentityGenerator.reportingFlowContinuableEndEvent("testMediatorId",
                ComponentType.MEDIATOR, artifactHolder);
        Assert.assertEquals("Stack element should be popped out", artifactHolder.getStack().size(), 0);
        Assert.assertTrue("must be set to true", artifactHolder.getExitFromBox());
    }

    /**
     * Testing getIdForFlowContinuableMediator.
     * Using the same to test process method for component type RESOURCE.
     */
    @Test
    public void testGetIdForFlowContinuableMediator() {
        cleanUp();
        String temp = StatisticIdentityGenerator.getIdForFlowContinuableMediator(COMPONENT, ComponentType.RESOURCE,
                artifactHolder);
        String expected = PARENT + "@0:" + COMPONENT;
        Assert.assertEquals("comparing generated id", expected, temp);
        Assert.assertEquals("process method add new item to stack for RESOURCE component type",
                artifactHolder.getStack().size(), 2);
        Assert.assertEquals("process method add new item to list for RESOURCE component type",
                artifactHolder.getList().size(), 2);
    }

    /**
     * Testing exception condition for AnonymousSequences.
     */
    @Test
    public void testGetIdForFlowContinuableMediatorAnonymous() {
        cleanUp();
        Assert.assertNull("must return null for anonymous sequence", StatisticIdentityGenerator
                .getIdForFlowContinuableMediator("AnonymousSequence",
                        ComponentType.SEQUENCE, artifactHolder));
    }

    /**
     * Test getIdForComponent method by comparing generated Ids.
     * Tse the same to validate process method for component type ENDPOINT.
     */
    @Test
    public void testGetIdForComponent() {
        cleanUp();
        String temp = StatisticIdentityGenerator.getIdForComponent(COMPONENT, ComponentType.ENDPOINT, artifactHolder);
        String expected = PARENT + "@0:" + COMPONENT;
        Assert.assertEquals("comparing generated id", expected, temp);
        Assert.assertNotNull("process method add new StructuringElement for ENDPOINT type",
                artifactHolder.getList().get(1));
    }

    /**
     * Test reportingBranchingEvents method.
     */
    @Test
    public void testReportingBranchingEvents() {
        cleanUp();
        StatisticIdentityGenerator.reportingBranchingEvents(artifactHolder);
        Assert.assertEquals("validating new parent value set by the method",
                COMPONENT_ID, artifactHolder.getLastParent());
    }

    /**
     * Test reportingEndEvent method.
     */
    @Test
    public void testReportingEndEvent() {
        addElementsForReportingEndEvent();
        StatisticIdentityGenerator.reportingEndEvent(COMPONENT, ComponentType.API, artifactHolder);
        StatisticIdentityGenerator.reportingEndEvent(COMPONENT, ComponentType.SEQUENCE, artifactHolder);
        Assert.assertEquals("validating new parent value set by the method",
                COMPONENT_ID, artifactHolder.getLastParent());
        Assert.assertEquals("stack must be empty since popped by the method", artifactHolder.getStack().size(), 0);
    }

    /**
     * Test reportingEndEventMediator.
     */
    @Test
    public void testReportingEndEventMediator() {
        cleanUp();
        artifactHolder.setLastParent(PARENT);
        StatisticIdentityGenerator.reportingEndEvent(COMPONENT, ComponentType.MEDIATOR, artifactHolder);
        Assert.assertEquals("validating new parent value set by the method", PARENT, artifactHolder.getLastParent());
        Assert.assertEquals("stack must be empty since popped by the method", artifactHolder.getStack().size(), 0);
    }

    /**
     * Asserting getIdReferencingComponent.
     * Use the same to validate process method for component types API,SEQUENCE,RESOURCE and MEDIATOR.
     */
    @Test
    public void testGetIdReferencingComponent() {
        cleanUp();
        String idString = StatisticIdentityGenerator.getIdReferencingComponent(COMPONENT, ComponentType.API,
                artifactHolder);
        Assert.assertEquals("validating idString", idString, COMPONENT + "@0:" + COMPONENT + "@indirect");
        Assert.assertEquals("process method add new item to stack for API component type",
                artifactHolder.getStack().size(), 2);
        Assert.assertEquals("process method add new item to list for API component type",
                artifactHolder.getList().size(), 2);

        idString = StatisticIdentityGenerator.getIdReferencingComponent(COMPONENT, ComponentType.SEQUENCE,
                artifactHolder);
        Assert.assertEquals("validating idString", idString, COMPONENT + "@1:" + COMPONENT + "@indirect");
        Assert.assertEquals("process method add new item to stack for SEQUENCE component type",
                artifactHolder.getStack().size(), 3);
        Assert.assertEquals("process method add new item to list for SEQUENCE component type",
                artifactHolder.getList().size(), 3);

        artifactHolder.setExitFromBox(true);
        idString = StatisticIdentityGenerator.getIdReferencingComponent(COMPONENT, ComponentType.MEDIATOR,
                artifactHolder);
        Assert.assertEquals("validating idString", idString, COMPONENT + "@2:" + COMPONENT + "@indirect");
        Assert.assertEquals("process method add new item to stack for MEDIATOR component type",
                artifactHolder.getStack().size(), 4);
        Assert.assertEquals("process method add new item to list for MEDIATOR component type",
                artifactHolder.getList().size(), 4);
        Assert.assertFalse("process method for MEDIATOR must set this to false", artifactHolder.getExitFromBox());
    }

    /**
     * Clean up code used to init stack and list of ArtifactHolder.
     */
    private void cleanUp() {
        int size = artifactHolder.getStack().size();
        if (size >= 1) {
            while (size != 0) {
                artifactHolder.getStack().pop();
                size = artifactHolder.getStack().size();
            }
        }
        artifactHolder.getStack().add(structuringElement);
        int listSize = artifactHolder.getList().size();
        if (listSize >= 1) {
            while (listSize != 0) {
                artifactHolder.getList().remove(listSize - 1);
                listSize = artifactHolder.getList().size();
            }
        }
        artifactHolder.getList().add(structuringElement);
        artifactHolder.setId(0);
    }

    private void addElementsForReportingEndEvent() {
        int size = artifactHolder.getStack().size();
        if (size >= 1) {
            while (size != 0) {
                artifactHolder.getStack().pop();
                size = artifactHolder.getStack().size();
            }
        }
        artifactHolder.getStack().add(structuringElement);
        artifactHolder.getStack().add(structuringElement);
    }
}
