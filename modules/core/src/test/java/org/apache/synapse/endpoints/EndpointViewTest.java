/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.endpoints;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for unit testing of EndpointView.
 */
public class EndpointViewTest extends TestCase {


    /**
     * Creates a basic EndpointView with a given number of children added to the endpoint.
     *
     * @param numberOfChildren the child endpoints inside the endpoint attached to the end point view. If
     *                         specified as 0, the children list will be null
     * @return the created end point view
     */
    private EndpointView createEndPointView(int numberOfChildren) {
        AbstractEndpoint endpoint = new AddressEndpoint();
        MessageContext synCtx = new TestMessageContext();
        synCtx.setConfiguration(new SynapseConfiguration());
        SynapseEnvironment environment = new Axis2SynapseEnvironment(
                new ConfigurationContext(new AxisConfiguration()), synCtx.getConfiguration());
        if (numberOfChildren > 0) {
            List<Endpoint> children = new ArrayList<>(numberOfChildren);
            for (int i = 0; i < numberOfChildren; i++) {
                AbstractEndpoint child = new AddressEndpoint();
                child.init(environment);
                child.setEnableMBeanStats(true);
                child.setName("endpoint" + i);
                children.add(child);
            }
            endpoint.setChildren(children);
        }
        endpoint.init(environment);
        return new EndpointView("endpoint", endpoint);
    }

    /**
     * Method to create mock endpoint to be used by the endpoint view.
     *
     * @param numberOfChildren the child endpoints inside the endpoint attached to the end point view. If
     *                         specified as 0, the children list will be null
     * @return the created end point view
     */
    private AbstractEndpoint createMockEndPoint(int numberOfChildren) {
        AbstractEndpoint abstractEndpoint = Mockito.mock(AddressEndpoint.class);
        if (numberOfChildren > 0) {
            List<Endpoint> children = new ArrayList<>();
            for (int i = 0; i < numberOfChildren; i++) {
                AbstractEndpoint child = Mockito.mock(AddressEndpoint.class);
                EndpointContext endpointContext = Mockito.mock(EndpointContext.class);
                Mockito.when(child.getContext()).thenReturn(endpointContext);
                children.add(child);

            }
            Mockito.when(abstractEndpoint.getChildren()).thenReturn(children);
        } else {
            Mockito.when(abstractEndpoint.getChildren()).thenReturn(null);
        }
        EndpointContext endpointContext = Mockito.mock(EndpointContext.class);
        Mockito.when(abstractEndpoint.getContext()).thenReturn(endpointContext);
        return abstractEndpoint;
    }

    /**
     * Method to create an endpoint view with a mocked endpoint with the specified number of children.
     *
     * @param numberOfChildren the child endpoints inside the endpoint attached to the end point view. If
     *                         specified as 0, the children list will be null
     * @return the created end point view
     */
    private EndpointView createMockEndPointView(int numberOfChildren) {
        AbstractEndpoint abstractEndpoint = createMockEndPoint(numberOfChildren);
        return new EndpointView("endpoint", abstractEndpoint);
    }

    /**
     * Performs the following operation to test destroy of an endpoint view.
     * 1. Increment suspension and timeout counts
     * 2. Call method destroy of endpoint view
     * 3. Assert counts to be reset
     */
    public void testDestroy() {
        EndpointView endpointView = new EndpointView("endpoint", null);
        endpointView.incrementSuspensions();
        endpointView.incrementTimeouts();
        endpointView.destroy();
        Assert.assertEquals("Last minute suspension count is not reset", 0,
                            endpointView.getLastMinuteEndpointSuspensions());
        Assert.assertEquals("Last 5 minute count is not reset", 0, endpointView.getLast5MinuteEndpointSuspensions());
        Assert.assertEquals("Last 15 minute count is not reset", 0, endpointView.getLast15MinuteEndpointSuspensions());
        Assert.assertEquals("Last minute timeout count is not reset", 0, endpointView.getLastMinuteEndpointTimeouts());
        Assert.assertEquals("Last 5 minute timeout count is not reset", 0,
                            endpointView.getLast5MinuteEndpointTimeouts());
        Assert.assertEquals("Last 15 minute count is not reset", 0, endpointView.getLast15MinuteEndpointTimeouts());
    }

    /**
     * Switches on the endpoint view and tests if the active state of the endpoint view is updated. Additionally,
     * asserts for the active children count to make sure that all children are activated.
     *
     * @throws Exception if an exception occurs while activating endpoint view
     */
    public void testSwitchOn() throws Exception {
        EndpointView endpointView = createEndPointView(2);
        endpointView.switchOn();
        Assert.assertEquals("Switching on endpoint view has not activated all children",
                            2, endpointView.getActiveChildren());
        Assert.assertTrue("Endpoint view is not active", endpointView.isActive());

        endpointView = createEndPointView(0);
        endpointView.switchOn();
        Assert.assertTrue("Endpoint view is not active", endpointView.isActive());
    }

    /**
     * Switches off the endpoint view and tests if the active state of the endpoint view is updated. Additionally,
     * asserts for the active children count to make sure that all children are de-activated.
     *
     * @throws Exception if an exception occurs while switching off the endpoint view
     */
    public void testSwitchOff() throws Exception {
        EndpointView endpointView = createEndPointView(2);
        endpointView.switchOn();
        endpointView.switchOff();
        Assert.assertEquals("Switching off endpoint view has not deactivated all children",
                            0, endpointView.getActiveChildren());
        Assert.assertFalse("Endpoint view is active", endpointView.isActive());

        endpointView = createEndPointView(0);
        endpointView.switchOn();
        endpointView.switchOff();
        Assert.assertFalse("Endpoint view is active", endpointView.isActive());
    }

    /**
     * Tests if the endpoint view correctly returns the active state with and without the presence of children
     * endpoints. The endpoint should be inactive if and only if all child endpoints are inactive.
     *
     * @throws Exception if an error occurs while accessing the active children of the endpoint view
     */
    public void testIsActive() throws Exception {
        AbstractEndpoint endpoint = createMockEndPoint(3);
        EndpointView endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getChildren().get(2).getContext().isState(EndpointContext.ST_ACTIVE)).thenReturn(true);
        Assert.assertTrue("Endpoint view is not active with active child endpoint", endpointView.isActive());

        endpoint = createMockEndPoint(0);
        EndpointContext endpointContext = Mockito.mock(EndpointContext.class);
        Mockito.when(endpoint.getContext()).thenReturn(endpointContext);
        endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getContext().isState(EndpointContext.ST_ACTIVE)).thenReturn(true);
        Assert.assertTrue("Endpoint view is not active", endpointView.isActive());
        Mockito.when(endpoint.getContext().isState(EndpointContext.ST_ACTIVE)).thenReturn(false);
        Assert.assertFalse("Endpoint is active", endpointView.isActive());
    }

    /**
     * Tests if the endpoint view correctly returns the timed out state with and without the presence of children
     * endpoints. The endpoint view should be timed out if and only if all child endpoints are timed out.
     *
     * @throws Exception if an error occurs while accessing the state of the endpoint view
     */
    public void testIsTimedout() throws Exception {
        AbstractEndpoint endpoint = createMockEndPoint(2);
        EndpointView endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getChildren().get(0).getContext().isState(EndpointContext.ST_TIMEOUT)).thenReturn(true);
        Assert.assertFalse("Endpoint view is timed out with active child endpoints", endpointView.isTimedout());
        Mockito.when(endpoint.getChildren().get(1).getContext().isState(EndpointContext.ST_TIMEOUT)).thenReturn(true);
        Assert.assertTrue("Endpoint view is not timed out with all child endpoints being timed out",
                          endpointView.isTimedout());

        endpoint = createMockEndPoint(0);
        endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getContext().isState(EndpointContext.ST_TIMEOUT)).thenReturn(true);
        Assert.assertTrue("Endpoint view is not timed out", endpointView.isTimedout());
        Mockito.when(endpoint.getContext().isState(EndpointContext.ST_TIMEOUT)).thenReturn(false);
        Assert.assertFalse("Endpoint view is timed out", endpointView.isTimedout());
    }

    /**
     * Tests if the endpoint view correctly returns the suspended state with and without the presence of children
     * endpoints. The endpoint view should be suspended if and only if all child endpoints are suspended.
     *
     * @throws Exception if an error occurs while accessing the state of the endpoint view
     */
    public void testIsSuspended() throws Exception {
        AbstractEndpoint endpoint = createMockEndPoint(2);
        EndpointView endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getChildren().get(0).getContext().isState(EndpointContext.ST_SUSPENDED)).thenReturn(true);
        Assert.assertFalse("Endpoint view is suspended with active child endpoints", endpointView.isSuspended());
        Mockito.when(endpoint.getChildren().get(1).getContext().isState(EndpointContext.ST_SUSPENDED)).thenReturn(true);
        Assert.assertTrue("Endpoint view is not suspended with all child endpoints being suspended",
                          endpointView.isSuspended());

        endpoint = createMockEndPoint(0);
        endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getContext().isState(EndpointContext.ST_SUSPENDED)).thenReturn(true);
        Assert.assertTrue("Endpoint view is not suspended", endpointView.isSuspended());
        Mockito.when(endpoint.getContext().isState(EndpointContext.ST_SUSPENDED)).thenReturn(false);
        Assert.assertFalse("Endpoint view is suspended", endpointView.isSuspended());
    }

    /**
     * Tests if the endpoint view correctly returns the switched off state with and without the presence of children
     * endpoints. The endpoint view should be switched if and only if all child endpoints are switched off.
     *
     * @throws Exception if an error occurs while accessing the state of the endpoint view
     */
    public void testIsSwitchedOff() throws Exception {
        AbstractEndpoint endpoint = createMockEndPoint(2);
        EndpointView endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getChildren().get(0).getContext().isState(EndpointContext.ST_OFF)).thenReturn(true);
        Assert.assertFalse("Endpoint view switched off with active child endpoints", endpointView.isSwitchedOff());
        Mockito.when(endpoint.getChildren().get(1).getContext().isState(EndpointContext.ST_OFF)).thenReturn(true);
        Assert.assertTrue("Endpoint view is not switched off with all child endpoints being switched off",
                          endpointView.isSwitchedOff());

        endpoint = createMockEndPoint(0);
        endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getContext().isState(EndpointContext.ST_OFF)).thenReturn(true);
        Assert.assertTrue("Endpoint view is not switched off", endpointView.isSwitchedOff());
        Mockito.when(endpoint.getContext().isState(EndpointContext.ST_OFF)).thenReturn(false);
        Assert.assertFalse("Endpoint view is swtiched off", endpointView.isSwitchedOff());
    }

    /**
     * Asserts if the the endpoint view correctly returns the total number of child endpoints included in the
     * endpoint associated with the view.
     *
     * @throws Exception if an error occurs while accessing child endpoints
     */
    public void testGetTotalChildren() throws Exception {
        EndpointView endpointView = createMockEndPointView(3);
        Assert.assertEquals("Incorrect child count", 3, endpointView.getTotalChildren());

        endpointView = createMockEndPointView(0);
        Assert.assertEquals("Incorrect child count", 0, endpointView.getTotalChildren());
    }

    /**
     * Asserts if the the endpoint view correctly returns the number of active child endpoints included in the
     * endpoint associated with the view.
     *
     * @throws Exception if an error occurs while accessing child endpoints
     */
    public void testGetActiveChildren() throws Exception {

        //Assert with children
        AbstractEndpoint endpoint = createMockEndPoint(5);
        Mockito.when(endpoint.getChildren().get(0).getContext().isState(EndpointContext.ST_ACTIVE)).thenReturn(true);
        Mockito.when(endpoint.getChildren().get(1).getContext().isState(EndpointContext.ST_ACTIVE)).thenReturn(false);
        Mockito.when(endpoint.getChildren().get(2).getContext().isState(EndpointContext.ST_ACTIVE)).thenReturn(true);
        Mockito.when(endpoint.getChildren().get(3).getContext().isState(EndpointContext.ST_ACTIVE)).thenReturn(false);
        Mockito.when(endpoint.getChildren().get(4).getContext().isState(EndpointContext.ST_ACTIVE)).thenReturn(true);
        EndpointView endpointView = new EndpointView("endpoint", endpoint);
        Assert.assertEquals("Incorrect active children count", 3, endpointView.getActiveChildren());

        //Assert when endpoint has no children
        EndpointView endpointViewWithoutChildren = createMockEndPointView(0);
        Assert.assertEquals("Incorrect active children count", 0,
                            endpointViewWithoutChildren.getActiveChildren());
    }

    /**
     * Asserts if the the endpoint view correctly returns the number of child endpoints in the ready state.
     *
     * @throws Exception if an error occurs while accessing child endpoints
     */
    public void testGetReadyChildren() throws Exception {
        AbstractEndpoint endpoint = createMockEndPoint(3);
        EndpointView endpointView = new EndpointView("endpoint", endpoint);
        Mockito.when(endpoint.getChildren().get(0).getContext().readyToSend()).thenReturn(true);
        Assert.assertEquals("Incorrect ready children count", 1, endpointView.getReadyChildren());

        endpointView = createMockEndPointView(0);
        Assert.assertEquals("Ready children count should be empty", 0, endpointView.getReadyChildren());
    }

    /**
     * Changes statics, calls the reset method and asserts if all statistics are reset correctly.
     */
    public void testResetStatistics() {
        EndpointView endpointView = createEndPointView(1);
        changeStatistics(endpointView);
        endpointView.resetStatistics();
        assertResetStatistics(endpointView);

        endpointView = createEndPointView(0);
        changeStatistics(endpointView);
        endpointView.resetStatistics();
        assertResetStatistics(endpointView);
    }

    /**
     * Tests if the receiving faults table and the fault receiving counts are correctly updated when
     * 'incrementFaultsReceiving' is called.
     */
    public void testIncrementFaultsReceiving() {
        EndpointView endpointView = createMockEndPointView(0);
        endpointView.incrementFaultsReceiving(100);
        Assert.assertEquals("Receiving fault count not incremented", 1, endpointView.getFaultsReceiving());
        Assert.assertEquals("Receiving fault table not updated", 1,
                            endpointView.getReceivingFaultTable().get(100).longValue());
        endpointView.incrementFaultsReceiving(100);
        Assert.assertEquals("Receiving fault count not incremented", 2, endpointView.getFaultsReceiving());
        Assert.assertEquals("Receiving fault table not updated", 2,
                            endpointView.getReceivingFaultTable().get(100).longValue());
    }

    /**
     * Tests if the sending faults table and the sending fault counts are correctly updated when
     * 'incrementFaultsSending' is called.
     */
    public void testIncrementFaultsSending() {
        EndpointView endpointView = createMockEndPointView(0);
        endpointView.incrementFaultsSending(100);
        Assert.assertEquals("Sending fault count not incremented", 1, endpointView.getFaultsSending());
        Assert.assertEquals("Sending fault table not updated", 1,
                            endpointView.getSendingFaultTable().get(100).longValue());
        endpointView.incrementFaultsSending(100);
        Assert.assertEquals("Sending fault count not incremented", 2, endpointView.getFaultsSending());
        Assert.assertEquals("Sending fault table not updated", 2,
                            endpointView.getSendingFaultTable().get(100).longValue());
    }

    /**
     * Tests if the receiving faults table is correctly updated when a receiving fault with an error code is reported.
     */
    public void testReportReceivingFault() {
        EndpointView endpointView = createMockEndPointView(0);
        endpointView.reportReceivingFault(100);
        Assert.assertEquals("Receiving fault table not updated with new entry", 1,
                            endpointView.getReceivingFaultTable().get(100).longValue());
        endpointView.reportReceivingFault(100);
        Assert.assertEquals("Receiving fault table not updated", 2,
                            endpointView.getReceivingFaultTable().get(100).longValue());
    }

    /**
     * Tests if the receiving faults table is correctly updated when a sending fault with an error code is reported.
     */
    public void testReportSendingFault() {
        EndpointView endpointView = createMockEndPointView(0);
        endpointView.reportSendingFault(100);
        Assert.assertEquals("Sending fault table not updated with new entry", 1,
                            endpointView.getSendingFaultTable().get(100).longValue());
        endpointView.reportSendingFault(100);
        Assert.assertEquals("Receiving fault table not updated", 2,
                            endpointView.getSendingFaultTable().get(100).longValue());
    }

    /**
     * Tests if the suspention counts are correctly updated when 'incrementSuspensions' is called.
     */
    public void testIncrementSuspensions() {
        EndpointView endpointView = createMockEndPointView(0);
        endpointView.incrementSuspensions();
        Assert.assertEquals("Consecutive endpoint suspension count not incremented", 1,
                            endpointView.getConsecutiveEndpointSuspensions());
        Assert.assertEquals("Total endpoint suspension count not incremented", 1,
                            endpointView.getTotalEndpointSuspensions());
    }

    /**
     * Tests if the timeout counts are correctly updated when 'incrementTimeouts' is called.
     */
    public void testIncrementTimeouts() {
        EndpointView endpointView = createMockEndPointView(0);
        endpointView.incrementTimeouts();
        Assert.assertEquals("Consecutive endpoint timeout count not incremented", 1,
                            endpointView.getConsecutiveEndpointTimeouts());
        Assert.assertEquals("Total endpoint suspension count not incremented", 1,
                            endpointView.getTotalEndpointTimeouts());
    }

    /**
     * Tests if the consecutive suspension count is correctly updated when 'incrementSuspensions' is called.
     */
    public void testResetConsecutiveSuspensions() {
        EndpointView endpointView = createMockEndPointView(0);
        endpointView.incrementSuspensions();
        endpointView.resetConsecutiveSuspensions();
        Assert.assertEquals("Consecutive endpoint suspension count not reset", 0,
                            endpointView.getConsecutiveEndpointSuspensions());
    }

    /**
     * Method to change statistics of and endpoint view.
     *
     * @param endpointView the endpoint view of whic statistics should be changed.
     */
    private void changeStatistics(EndpointView endpointView) {
        endpointView.incrementMessagesReceived();
        endpointView.incrementFaultsReceiving(0);
        endpointView.incrementTimeoutsReceiving();
        endpointView.incrementBytesReceived(10);
        endpointView.notifyReceivedMessageSize(10);
        endpointView.incrementMessagesSent();
        endpointView.incrementFaultsSending(0);
        endpointView.incrementTimeoutsSending();
        endpointView.incrementMessagesSent();
        endpointView.notifySentMessageSize(10);
        endpointView.incrementTimeoutsSending();
        endpointView.incrementBytesSent(10);
        endpointView.reportResponseCode(100);
    }

    /**
     * Method to assert reset statistics of an endpoint view.
     *
     * @param endpointView the endpoint view of which statistics should be asserted
     */
    private void assertResetStatistics(EndpointView endpointView) {
        Assert.assertEquals("message receive count not reset", 0, endpointView.getMessagesReceived());
        Assert.assertEquals("Faults receive count not reset", 0, endpointView.getFaultsReceiving());
        Assert.assertEquals("Receive timeout count not reset", 0, endpointView.getTimeoutsReceiving());
        Assert.assertEquals("Bytes received not reset", 0, endpointView.getBytesReceived());
        Assert.assertEquals("Min message size received not reset", 0, endpointView.getMinSizeReceived());
        Assert.assertEquals("Max message size received not reset", 0, endpointView.getMaxSizeReceived());
        Assert.assertEquals("Average message size received not reset", 0.0, endpointView.getAvgSizeReceived());
        Assert.assertEquals("Receiving fault table not cleared", 0, endpointView.getReceivingFaultTable().size());
        Assert.assertEquals("Message sent count not reset", 0, endpointView.getMessagesSent());
        Assert.assertEquals("Faults sent count not reset", 0, endpointView.getFaultsSending());
        Assert.assertEquals("Sending timeout count not reset", 0, endpointView.getTimeoutsSending());
        Assert.assertEquals("Bytes send not reset", 0, endpointView.getBytesSent());
        Assert.assertEquals("Min message size sent not reset", 0, endpointView.getMinSizeSent());
        Assert.assertEquals("Max message size sent not reset", 0, endpointView.getMaxSizeSent());
        Assert.assertEquals("Average message size setn not reset", 0.0, endpointView.getAvgSizeSent());
        Assert.assertEquals("Sending failut table not cleared", 0, endpointView.getSendingFaultTable().size());
        Assert.assertEquals("Response code table not cleared", 0, endpointView.getResponseCodeTable().size());
    }

}
