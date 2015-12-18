/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.debug;


import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.core.axis2.MessageContextCreatorForAxis2;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.TestMediateHandler;
import org.apache.synapse.mediators.TestMediator;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.template.TemplateMediator;

import java.util.HashSet;

public class DebugManagerTest extends TestCase {
    private static SynapseConfiguration synConfig;
    private static Axis2SynapseEnvironment synEnv;
    private static SynapseDebugManager dm;
    private StringBuffer result = new StringBuffer();


    static {
        setupTestEnvironment();
    }


    public static void setupTestEnvironment() {
        synConfig = new SynapseConfiguration();
        synEnv = new Axis2SynapseEnvironment(synConfig);
        synEnv.setDebugEnabled(true);
        dm = SynapseDebugManager.getInstance();
        dm.init(synConfig, null, synEnv, false);
        synEnv.setDebugEnabled(false);
        synEnv.setSynapseDebugManager(dm);
    }

    public void testDebugManagerProcessCommandSetBreakPointSequence() throws Exception {
        TestMediator t1 = new TestMediator();
        TestMediator t2 = new TestMediator();
        TestMediator t3 = new TestMediator();
        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);
        synConfig.addSequence("test_sequence_1", seq);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_1\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator) seq.getChild(0)).isBreakPoint());
    }

    public void testDebugManagerProcessCommandClearBreakPointSequence() throws Exception {
        TestMediator t1 = new TestMediator();
        TestMediator t2 = new TestMediator();
        TestMediator t3 = new TestMediator();
        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);
        synConfig.addSequence("test_sequence_2", seq);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_2\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"clear\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_2\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        assertTrue(!((AbstractMediator) seq.getChild(0)).isBreakPoint());

    }

    public void testDebugManagerProcessCommandSetSkipSequence() throws Exception {
        TestMediator t1 = new TestMediator();
        TestMediator t2 = new TestMediator();
        TestMediator t3 = new TestMediator();
        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);
        synConfig.addSequence("test_sequence_3", seq);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_3\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator) seq.getChild(0)).isSkipEnabled());

    }

    public void testDebugManagerProcessCommandClearSkipSequence() throws Exception {
        TestMediator t1 = new TestMediator();
        TestMediator t2 = new TestMediator();
        TestMediator t3 = new TestMediator();
        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);
        synConfig.addSequence("test_sequence_4", seq);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_4\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"clear\",\"command-argument\":\"skip\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_4\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        assertTrue(!((AbstractMediator) seq.getChild(0)).isSkipEnabled());

    }

    public void testSkipSequence() throws Exception {
        TestMediator t1 = new TestMediator();
        t1.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T1.");
                    }
                });
        TestMediator t2 = new TestMediator();
        t2.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T2.");
                    }
                });
        TestMediator t3 = new TestMediator();
        t3.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T3");
                    }
                });

        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);
        synConfig.addSequence("test_sequence_5", seq);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_5\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_5\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"1\"}}";
        dm.processDebugCommand(debug_command);
        synEnv.setDebugEnabled(true);
        MessageContextCreatorForAxis2.setSynConfig(synConfig);
        MessageContextCreatorForAxis2.setSynEnv(synEnv);
        org.apache.axis2.context.MessageContext mc =
                new org.apache.axis2.context.MessageContext();
        AxisConfiguration axisConfig = synConfig.getAxisConfiguration();
        if (axisConfig == null) {
            axisConfig = new AxisConfiguration();
            synConfig.setAxisConfiguration(axisConfig);
        }
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfig);
        mc.setConfigurationContext(cfgCtx);
        mc.setEnvelope(TestUtils.getTestContext("<empty/>").getEnvelope());
        MessageContext synCtx = MessageContextCreatorForAxis2.getSynapseMessageContext(mc);
        seq.mediate(synCtx);
        assertTrue("T3".equals(result.toString()));
        synEnv.setDebugEnabled(false);
    }

    public void testBreakPointSequence() throws Exception {
        TestMediator t1 = new TestMediator();
        t1.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T1.");
                    }
                });
        TestMediator t2 = new TestMediator();
        t2.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T2.");
                    }
                });
        TestMediator t3 = new TestMediator();
        t3.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T3");
                    }
                });

        SequenceMediator seq = new SequenceMediator();
        seq.addChild(t1);
        seq.addChild(t2);
        seq.addChild(t3);
        synConfig.addSequence("test_sequence_6", seq);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_6\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"sequence-key\":\"test_sequence_6\"," +
                "\"sequence-type\": \"named\",\"mediator-position\": \"1\"}}";
        dm.processDebugCommand(debug_command);
        MessageContext synCtx = TestUtils.getTestContext("<empty/>");
        seq.mediate(synCtx);
        assertTrue("T1.T2.T3".equals(result.toString()));

    }

    public void testDebugManagerProcessCommandSetBreakPointTemplate() throws Exception {
        TestMediator t1 = new TestMediator();
        TestMediator t2 = new TestMediator();
        TestMediator t3 = new TestMediator();
        TemplateMediator temp = new TemplateMediator();
        temp.addChild(t1);
        temp.addChild(t2);
        temp.addChild(t3);
        temp.setName("test_sequence_template_1");
        synConfig.addSequenceTemplate(temp.getName(), temp);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"template\",\"template\":{\"template-key\":\"test_sequence_template_1\"," +
                "\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator) temp.getChild(0)).isBreakPoint());
    }

    public void testDebugManagerProcessCommandClearBreakPointTemplate() throws Exception {
        TestMediator t1 = new TestMediator();
        TestMediator t2 = new TestMediator();
        TestMediator t3 = new TestMediator();
        TemplateMediator temp = new TemplateMediator();
        temp.addChild(t1);
        temp.addChild(t2);
        temp.addChild(t3);
        temp.setName("test_sequence_template_2");
        synConfig.addSequenceTemplate(temp.getName(), temp);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"template\",\"template\":{\"template-key\":\"test_sequence_template_2\"," +
                "\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"clear\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"template\",\"template\":{\"template-key\":\"test_sequence_template_2\"," +
                "\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        assertTrue(!((AbstractMediator) temp.getChild(0)).isBreakPoint());
    }


    public void testDebugManagerProcessCommandSetSkipTemplate() throws Exception {
        TestMediator t1 = new TestMediator();
        TestMediator t2 = new TestMediator();
        TestMediator t3 = new TestMediator();
        TemplateMediator temp = new TemplateMediator();
        temp.addChild(t1);
        temp.addChild(t2);
        temp.addChild(t3);
        temp.setName("test_sequence_template_3");
        synConfig.addSequenceTemplate(temp.getName(), temp);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\"," +
                "\"mediation-component\":\"template\",\"template\":{\"template-key\":\"test_sequence_template_3\"," +
                "\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator) temp.getChild(0)).isSkipEnabled());
    }

    public void testDebugManagerProcessCommandClearSkipTemplate() throws Exception {
        TestMediator t1 = new TestMediator();
        TestMediator t2 = new TestMediator();
        TestMediator t3 = new TestMediator();
        TemplateMediator temp = new TemplateMediator();
        temp.addChild(t1);
        temp.addChild(t2);
        temp.addChild(t3);
        temp.setName("test_sequence_template_4");
        synConfig.addSequenceTemplate(temp.getName(), temp);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\"," +
                "\"mediation-component\":\"template\",\"template\":{\"template-key\":\"test_sequence_template_4\"," +
                "\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"clear\",\"command-argument\":\"skip\",\"mediation-component\":\"template\"," +
                "\"template\":{\"template-key\":\"test_sequence_template_4\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        assertTrue(!((AbstractMediator) temp.getChild(0)).isSkipEnabled());
    }


    public void testSkipTemplate() throws Exception {
        TestMediator t1 = new TestMediator();
        t1.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T1.");
                    }
                });
        TestMediator t2 = new TestMediator();
        t2.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T2.");
                    }
                });
        TestMediator t3 = new TestMediator();
        t3.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T3");
                    }
                });

        TemplateMediator temp = new TemplateMediator();
        temp.addChild(t1);
        temp.addChild(t2);
        temp.addChild(t3);
        temp.setName("test_sequence_template_5");
        temp.setParameters(new HashSet<String>());
        synConfig.addSequenceTemplate(temp.getName(), temp);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\",\"mediation-component\":\"template\"," +
                "\"template\":{\"template-key\":\"test_sequence_template_5\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\",\"mediation-component\":\"template\"," +
                "\"template\":{\"template-key\":\"test_sequence_template_5\",\"mediator-position\": \"1\"}}";
        dm.processDebugCommand(debug_command);
        synEnv.setDebugEnabled(true);
        MessageContextCreatorForAxis2.setSynConfig(synConfig);
        MessageContextCreatorForAxis2.setSynEnv(synEnv);
        org.apache.axis2.context.MessageContext mc =
                new org.apache.axis2.context.MessageContext();
        AxisConfiguration axisConfig = synConfig.getAxisConfiguration();
        if (axisConfig == null) {
            axisConfig = new AxisConfiguration();
            synConfig.setAxisConfiguration(axisConfig);
        }
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfig);
        mc.setConfigurationContext(cfgCtx);
        mc.setEnvelope(TestUtils.getTestContext("<empty/>").getEnvelope());
        MessageContext synCtx = MessageContextCreatorForAxis2.getSynapseMessageContext(mc);
        temp.mediate(synCtx);
        assertTrue("T3".equals(result.toString()));
        synEnv.setDebugEnabled(false);
    }

    public void testBreakPointTemplate() throws Exception {
        TestMediator t1 = new TestMediator();
        t1.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T1.");
                    }
                });
        TestMediator t2 = new TestMediator();
        t2.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T2.");
                    }
                });
        TestMediator t3 = new TestMediator();
        t3.setHandler(
                new TestMediateHandler() {
                    public void handle(MessageContext synCtx) {
                        result.append("T3");
                    }
                });

        TemplateMediator temp = new TemplateMediator();
        temp.addChild(t1);
        temp.addChild(t2);
        temp.addChild(t3);
        temp.setName("test_sequence_template_6");
        temp.setParameters(new HashSet<String>());
        synConfig.addSequenceTemplate(temp.getName(), temp);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\",\"mediation-component\":" +
                "\"template\",\"template\":{\"template-key\":\"test_sequence_template_6\",\"mediator-position\": \"0\"}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\",\"mediation-component\":\"template\"," +
                "\"template\":{\"template-key\":\"test_sequence_template_6\",\"mediator-position\": \"1\"}}";
        dm.processDebugCommand(debug_command);
        MessageContext synCtx = TestUtils.getTestContext("<empty/>");
        synEnv.setDebugEnabled(false);
        temp.mediate(synCtx);
        assertTrue("T1.T2.T3".equals(result.toString()));

    }


    public void testDebugManagerProcessCommandSetBreakPointProxyInSequence() throws Exception {
        ProxyService ps = new ProxyService("test_proxy_1");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator inSeq = new SequenceMediator();
        inSeq.addChild(in1);
        inSeq.addChild(in2);
        inSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator outSeq = new SequenceMediator();
        outSeq.addChild(out1);
        outSeq.addChild(out2);
        outSeq.addChild(out3);
        ps.setTargetInLineInSequence(inSeq);
        ps.setTargetInLineOutSequence(inSeq);
        synConfig.addProxyService(ps.getName(), ps);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_1\",\"sequence-type\":\"proxy_inseq\",\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator) ps.getTargetInLineInSequence().getChild(0)).isBreakPoint());
    }


    public void testDebugManagerProcessCommandSetBreakPointProxyOutSequence() throws Exception {
        ProxyService ps = new ProxyService("test_proxy_2");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator inSeq = new SequenceMediator();
        inSeq.addChild(in1);
        inSeq.addChild(in2);
        inSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator outSeq = new SequenceMediator();
        outSeq.addChild(out1);
        outSeq.addChild(out2);
        outSeq.addChild(out3);
        ps.setTargetInLineInSequence(inSeq);
        ps.setTargetInLineOutSequence(inSeq);
        synConfig.addProxyService(ps.getName(), ps);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_2\",\"sequence-type\":\"proxy_outseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator) ps.getTargetInLineOutSequence().getChild(0)).isBreakPoint());
    }

    public void testDebugManagerProcessCommandClearBreakPointProxyInSequence() throws Exception {
        ProxyService ps = new ProxyService("test_proxy_3");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator inSeq = new SequenceMediator();
        inSeq.addChild(in1);
        inSeq.addChild(in2);
        inSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator outSeq = new SequenceMediator();
        outSeq.addChild(out1);
        outSeq.addChild(out2);
        outSeq.addChild(out3);
        ps.setTargetInLineInSequence(inSeq);
        ps.setTargetInLineOutSequence(inSeq);
        synConfig.addProxyService(ps.getName(), ps);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\"," +
                "\"mediation-component\":\"sequence\",\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_3\"," +
                "\"sequence-type\":\"proxy_inseq\",\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"clear\",\"command-argument\":\"breakpoint\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_3\"," +
                "\"sequence-type\":\"proxy_inseq\",\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(!((AbstractMediator) ps.getTargetInLineInSequence().getChild(0)).isBreakPoint());
    }

    public void testDebugManagerProcessCommandClearBreakPointProxyOutSequence() throws Exception {
        ProxyService ps = new ProxyService("test_proxy_4");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator inSeq = new SequenceMediator();
        inSeq.addChild(in1);
        inSeq.addChild(in2);
        inSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator outSeq = new SequenceMediator();
        outSeq.addChild(out1);
        outSeq.addChild(out2);
        outSeq.addChild(out3);
        ps.setTargetInLineInSequence(inSeq);
        ps.setTargetInLineOutSequence(inSeq);
        synConfig.addProxyService(ps.getName(), ps);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_4\",\"sequence-type\":\"proxy_outseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"clear\",\"command-argument\":\"breakpoint\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_4\",\"sequence-type\":\"proxy_outseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(!((AbstractMediator) ps.getTargetInLineOutSequence().getChild(0)).isBreakPoint());
    }


    public void testDebugManagerProcessCommandSetSkipProxyInSequence() throws Exception {
        ProxyService ps = new ProxyService("test_proxy_5");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator inSeq = new SequenceMediator();
        inSeq.addChild(in1);
        inSeq.addChild(in2);
        inSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator outSeq = new SequenceMediator();
        outSeq.addChild(out1);
        outSeq.addChild(out2);
        outSeq.addChild(out3);
        ps.setTargetInLineInSequence(inSeq);
        ps.setTargetInLineOutSequence(inSeq);
        synConfig.addProxyService(ps.getName(), ps);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_5\",\"sequence-type\":\"proxy_inseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator) ps.getTargetInLineInSequence().getChild(0)).isSkipEnabled());
    }


    public void testDebugManagerProcessCommandSetSkipProxyOutSequence() throws Exception {
        ProxyService ps = new ProxyService("test_proxy_6");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator inSeq = new SequenceMediator();
        inSeq.addChild(in1);
        inSeq.addChild(in2);
        inSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator outSeq = new SequenceMediator();
        outSeq.addChild(out1);
        outSeq.addChild(out2);
        outSeq.addChild(out3);
        ps.setTargetInLineInSequence(inSeq);
        ps.setTargetInLineOutSequence(inSeq);
        synConfig.addProxyService(ps.getName(), ps);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_6\",\"sequence-type\":\"proxy_outseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator) ps.getTargetInLineOutSequence().getChild(0)).isSkipEnabled());
    }

    public void testDebugManagerProcessCommandClearSkipProxyInSequence() throws Exception {
        ProxyService ps = new ProxyService("test_proxy_7");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator inSeq = new SequenceMediator();
        inSeq.addChild(in1);
        inSeq.addChild(in2);
        inSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator outSeq = new SequenceMediator();
        outSeq.addChild(out1);
        outSeq.addChild(out2);
        outSeq.addChild(out3);
        ps.setTargetInLineInSequence(inSeq);
        ps.setTargetInLineOutSequence(inSeq);
        synConfig.addProxyService(ps.getName(), ps);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_7\",\"sequence-type\":\"proxy_inseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"clear\",\"command-argument\":\"skip\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_7\",\"sequence-type\":\"proxy_inseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(!((AbstractMediator) ps.getTargetInLineInSequence().getChild(0)).isSkipEnabled());
    }

    public void testDebugManagerProcessCommandClearSkipProxyOutSequence() throws Exception {
        ProxyService ps = new ProxyService("test_proxy_8");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator inSeq = new SequenceMediator();
        inSeq.addChild(in1);
        inSeq.addChild(in2);
        inSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator outSeq = new SequenceMediator();
        outSeq.addChild(out1);
        outSeq.addChild(out2);
        outSeq.addChild(out3);
        ps.setTargetInLineInSequence(inSeq);
        ps.setTargetInLineOutSequence(inSeq);
        synConfig.addProxyService(ps.getName(), ps);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_8\",\"sequence-type\":\"proxy_outseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        debug_command = "{\"command\":\"clear\",\"command-argument\":\"skip\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"proxy\":{\"proxy-key\":\"test_proxy_8\",\"sequence-type\":\"proxy_outseq\"," +
                "\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(!((AbstractMediator) ps.getTargetInLineOutSequence().getChild(0)).isSkipEnabled());
    }

    public void testDebugManagerProcessCommandSetBreakPointInboundSequence() throws Exception {
        InboundEndpoint inboundEndpoint = new InboundEndpoint();
        inboundEndpoint.setName("test_inbound_1");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator dispatchSeq = new SequenceMediator();
        dispatchSeq.addChild(in1);
        dispatchSeq.addChild(in2);
        dispatchSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator errorSeq = new SequenceMediator();
        errorSeq.addChild(out1);
        errorSeq.addChild(out2);
        errorSeq.addChild(out3);
        synConfig.addSequence("dispatch", dispatchSeq);
        inboundEndpoint.setInjectingSeq("dispatch");
        synConfig.addSequence("error", errorSeq);
        inboundEndpoint.setOnErrorSeq("error");
        synConfig.addInboundEndpoint(inboundEndpoint.getName(),inboundEndpoint);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"inbound\":{\"inbound-key\":\"test_inbound_1\",\"sequence-type\":\"inbound_seq\",\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator)dispatchSeq.getChild(0)).isBreakPoint());
    }

    public void testDebugManagerProcessCommandSetBreakPointInboundErrorSequence() throws Exception {
        InboundEndpoint inboundEndpoint = new InboundEndpoint();
        inboundEndpoint.setName("test_inbound_2");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator dispatchSeq = new SequenceMediator();
        dispatchSeq.addChild(in1);
        dispatchSeq.addChild(in2);
        dispatchSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator errorSeq = new SequenceMediator();
        errorSeq.addChild(out1);
        errorSeq.addChild(out2);
        errorSeq.addChild(out3);
        synConfig.addSequence("dispatch_1", dispatchSeq);
        inboundEndpoint.setInjectingSeq("dispatch_1");
        synConfig.addSequence("error_1", errorSeq);
        inboundEndpoint.setOnErrorSeq("error_1");
        synConfig.addInboundEndpoint(inboundEndpoint.getName(),inboundEndpoint);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"breakpoint\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"inbound\":{\"inbound-key\":\"test_inbound_2\",\"sequence-type\":\"inbound_faultseq\",\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator)errorSeq.getChild(0)).isBreakPoint());
    }

    public void testDebugManagerProcessCommandSetSkipInboundSequence() throws Exception {
        InboundEndpoint inboundEndpoint = new InboundEndpoint();
        inboundEndpoint.setName("test_inbound_3");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator dispatchSeq = new SequenceMediator();
        dispatchSeq.addChild(in1);
        dispatchSeq.addChild(in2);
        dispatchSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator errorSeq = new SequenceMediator();
        errorSeq.addChild(out1);
        errorSeq.addChild(out2);
        errorSeq.addChild(out3);
        synConfig.addSequence("dispatch_3", dispatchSeq);
        inboundEndpoint.setInjectingSeq("dispatch_3");
        synConfig.addSequence("error_3", errorSeq);
        inboundEndpoint.setOnErrorSeq("error_3");
        synConfig.addInboundEndpoint(inboundEndpoint.getName(),inboundEndpoint);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"inbound\":{\"inbound-key\":\"test_inbound_3\",\"sequence-type\":\"inbound_seq\",\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator)dispatchSeq.getChild(0)).isSkipEnabled());
    }

    public void testDebugManagerProcessCommandSetSkipInboundErrorSequence() throws Exception {
        InboundEndpoint inboundEndpoint = new InboundEndpoint();
        inboundEndpoint.setName("test_inbound_4");
        TestMediator in1 = new TestMediator();
        TestMediator in2 = new TestMediator();
        TestMediator in3 = new TestMediator();
        SequenceMediator dispatchSeq = new SequenceMediator();
        dispatchSeq.addChild(in1);
        dispatchSeq.addChild(in2);
        dispatchSeq.addChild(in3);
        TestMediator out1 = new TestMediator();
        TestMediator out2 = new TestMediator();
        TestMediator out3 = new TestMediator();
        SequenceMediator errorSeq = new SequenceMediator();
        errorSeq.addChild(out1);
        errorSeq.addChild(out2);
        errorSeq.addChild(out3);
        synConfig.addSequence("dispatch_4", dispatchSeq);
        inboundEndpoint.setInjectingSeq("dispatch_4");
        synConfig.addSequence("error_4", errorSeq);
        inboundEndpoint.setOnErrorSeq("error_4");
        synConfig.addInboundEndpoint(inboundEndpoint.getName(),inboundEndpoint);
        String debug_command = "{\"command\":\"set\",\"command-argument\":\"skip\",\"mediation-component\":\"sequence\"," +
                "\"sequence\":{\"inbound\":{\"inbound-key\":\"test_inbound_4\",\"sequence-type\":\"inbound_faultseq\",\"mediator-position\":\"0\"}}}";
        dm.processDebugCommand(debug_command);
        assertTrue(((AbstractMediator)errorSeq.getChild(0)).isSkipEnabled());
    }

}
