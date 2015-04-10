/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.ForEachMediatorFactory;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.AbstractSplitMediatorTestCase;

import java.util.Properties;

/**
 * Unit tests for ForEach mediator
 */
public class ForEachMediatorTest extends AbstractSplitMediatorTestCase {

    MessageContext testCtx;
    ForEachHelperMediator helperMediator;

    protected void setUp() throws Exception {
        super.setUp();
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration config = new AxisConfiguration();
        testCtx =
                new Axis2MessageContext(
                        new org.apache.axis2.context.MessageContext(),
                        synCfg,
                        new Axis2SynapseEnvironment(
                                new ConfigurationContext(
                                        config),
                                synCfg));
        ((Axis2MessageContext) testCtx).getAxis2MessageContext()
                .setConfigurationContext(new ConfigurationContext(
                        config));
        SOAPEnvelope envelope =
                OMAbstractFactory.getSOAP11Factory()
                        .getDefaultEnvelope();
        testCtx.setEnvelope(envelope);
        testCtx.setSoapAction("urn:test");
        SequenceMediator seqMed = new SequenceMediator();
        helperMediator = new ForEachHelperMediator();
        helperMediator.init(testCtx.getEnvironment());
        seqMed.addChild(helperMediator);

        SequenceMediator seqMedInvalid = new SequenceMediator();
        SendMediator sendMediator = new SendMediator();
        sendMediator.init(testCtx.getEnvironment());
        seqMedInvalid.addChild(sendMediator);

        testCtx.getConfiguration().addSequence("seqRef", seqMed);
        testCtx.getConfiguration().addSequence("seqRefInvalid", seqMedInvalid);
        testCtx.getConfiguration().addSequence("main", new SequenceMediator());
        testCtx.getConfiguration().addSequence("fault", new SequenceMediator());

        testCtx.setEnvelope(envelope);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Testing when the xpath returns a list of elements
     *
     * @throws Exception
     */
    public void testForEachXpathList() throws Exception {
        testCtx.getEnvelope()
                .getBody()
                .addChild(createOMElement("<original>"
                        + "<itr>test-split-context-itr1-body</itr>"
                        + "<itr>test-split-context-itr2-body</itr>"
                        + "</original>"));
        MediatorFactory fac = new ForEachMediatorFactory();

        Mediator foreach =
                fac.createMediator(createOMElement("<foreach expression=\"//original/itr\" sequence=\"seqRef\" />"),
                        new Properties());

        helperMediator.clearMediatedContexts();
        foreach.mediate(testCtx);

        assertEquals(2, helperMediator.getMsgCount());

        assertEquals("<itr>test-split-context-itr1-body</itr>",
                helperMediator.getMediatedContext(0).getEnvelope()
                        .getBody().getFirstElement().toString());
        assertEquals("<itr>test-split-context-itr2-body</itr>",
                helperMediator.getMediatedContext(1).getEnvelope()
                        .getBody().getFirstElement().toString());
    }

    /**
     * Testing validity of reference sequence. Other cases : inline sequence
     * is covered in ForEachMediatorFactory
     *
     * @throws Exception
     */
    public void testSequenceValidity() throws Exception {
        testCtx.getEnvelope()
                .getBody()
                .addChild(createOMElement("<original>"
                        + "<itr>test-split-context-itr1-body</itr>"
                        + "<itr>test-split-context-itr2-body</itr>"
                        + "</original>"));
        MediatorFactory fac = new ForEachMediatorFactory();

        Mediator foreachInvalid = fac.createMediator(createOMElement("<foreach expression=\"//original/itr\" sequence=\"seqRefInvalid\" />"),
                new Properties());

        boolean successInvalid = foreachInvalid.mediate(testCtx);
        assertEquals(false, successInvalid);

        Mediator foreachValid =
                fac.createMediator(createOMElement("<foreach "
                                + "expression=\"//original/itr\" sequence=\"seqRef\" />"),
                        new Properties());

        boolean successValid = foreachValid.mediate(testCtx);
        assertEquals(true, successValid);
    }

    /**
     * Testing when the xpath returns only one element
     *
     * @throws Exception
     */
    public void testForEachXpathNode() throws Exception {
        testCtx.getEnvelope()
                .getBody()
                .addChild(createOMElement("<original>"
                        + "<itr id=\"one\">test-split-context-itr1-body</itr>"
                        + "<itr>test-split-context-itr2-body</itr>"
                        + "</original>"));
        MediatorFactory fac = new ForEachMediatorFactory();

        Mediator foreach =
                fac.createMediator(createOMElement("<foreach "
                                + "expression=\"//original/itr[@id='one']\" sequence=\"seqRef\" />"),
                        new Properties());

        helperMediator.clearMediatedContexts();
        foreach.mediate(testCtx);

        assertEquals(1, helperMediator.getMsgCount());

        assertEquals("<itr id=\"one\">test-split-context-itr1-body</itr>",
                helperMediator.getMediatedContext(0).getEnvelope()
                        .getBody().getFirstElement().toString());
    }


    /**
     * Testing when the relative xpath returns a list of elements
     *
     * @throws Exception
     */
    public void testForEachXpathListRelativePath() throws Exception {
        testCtx.getEnvelope()
                .getBody()
                .addChild(createOMElement("<original>"
                        + "<itr>test-split-context-itr1-body</itr>"
                        + "<itr>test-split-context-itr2-body</itr>"
                        + "</original>"));
        MediatorFactory fac = new ForEachMediatorFactory();

        Mediator foreach =
                fac.createMediator(createOMElement("<foreach "
                                + "expression=\"//itr\" sequence=\"seqRef\" />"),
                        new Properties());

        helperMediator.clearMediatedContexts();
        foreach.mediate(testCtx);

        assertEquals(2, helperMediator.getMsgCount());

        assertEquals("<itr>test-split-context-itr1-body</itr>",
                helperMediator.getMediatedContext(0).getEnvelope()
                        .getBody().getFirstElement().toString());
        assertEquals("<itr>test-split-context-itr2-body</itr>",
                helperMediator.getMediatedContext(1).getEnvelope()
                        .getBody().getFirstElement().toString());
    }

    /**
     * Testing when the xpath returns only one element
     *
     * @throws Exception
     */
    public void testForEachXpathNodeRelativePath() throws Exception {
        testCtx.getEnvelope()
                .getBody()
                .addChild(createOMElement("<original>"
                        + "<itr id=\"one\">test-split-context-itr1-body</itr>"
                        + "<itr>test-split-context-itr2-body</itr>"
                        + "</original>"));
        MediatorFactory fac = new ForEachMediatorFactory();

        Mediator foreach =
                fac.createMediator(createOMElement("<foreach "
                                + "expression=\"//itr[@id='one']\" sequence=\"seqRef\" />"),
                        new Properties());

        helperMediator.clearMediatedContexts();
        foreach.mediate(testCtx);

        assertEquals(1, helperMediator.getMsgCount());

        assertEquals("<itr id=\"one\">test-split-context-itr1-body</itr>",
                helperMediator.getMediatedContext(0).getEnvelope()
                        .getBody().getFirstElement().toString());
    }
}
