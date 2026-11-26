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


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
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
import org.apache.synapse.mediators.template.TemplateParam;
import org.codehaus.jettison.json.JSONObject;

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
        temp.setParameters(new HashSet<TemplateParam>());
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
        temp.setParameters(new HashSet<TemplateParam>());
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

    public void testSetJsonPayload() throws Exception {
        MessageContext mc = TestUtils.getTestContextJson("{\"name\":\"old\"}", null);
        setMessageType(mc, "application/json; charset=UTF-8");
        setPayload(mc, "{\"name\":\"new\",\"items\":[1,2]}");
        assertJsonPayload(mc, "{\"name\":\"new\",\"items\":[1,2]}");
    }

    public void testSetInvalidJsonPayloadKeepsPayload() throws Exception {
        MessageContext mc = TestUtils.getTestContextJson("{\"name\":\"old\"}", null);
        setMessageType(mc, "application/json");
        setPayload(mc, "{\"name\":");
        assertJsonPayload(mc, "{\"name\":\"old\"}");
    }

    public void testSetXmlPayloadReplacesBody() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order><id>1</id></order>");
        setMessageType(mc, "application/xml");
        setPayload(mc, "<customer><name>new</name></customer>");
        assertBodyContainsOnly(mc, "customer");
        assertEquals("new", mc.getEnvelope().getBody().getFirstElement().getFirstElement().getText());
    }

    public void testSetSoapEnvelopePayload() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order><id>1</id></order>");
        setMessageType(mc, "text/xml");
        setPayload(mc, "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soapenv:Body><customer>new</customer></soapenv:Body></soapenv:Envelope>");
        assertBodyContainsOnly(mc, "customer");
        assertEquals("new", mc.getEnvelope().getBody().getFirstElement().getText());
    }

    public void testSetInvalidXmlPayloadKeepsPayload() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order><id>1</id></order>");
        setMessageType(mc, "application/xml");
        setPayload(mc, "<customer>");
        assertBodyContainsOnly(mc, "order");
    }

    public void testSetTextPayload() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order><id>1</id></order>");
        setMessageType(mc, "text/plain");
        setPayload(mc, "hello world");
        assertBodyContainsOnly(mc, "text");
        assertEquals("hello world", mc.getEnvelope().getBody().getFirstElement().getText());
    }

    public void testSetVariableKeepsExistingType() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order/>");
        mc.setVariable("count", 1);
        setVariable(mc, "count", "5");
        assertEquals(5, mc.getVariable("count"));
        mc.setVariable("total", 1L);
        setVariable(mc, "total", "5000000000");
        assertEquals(5000000000L, mc.getVariable("total"));
        mc.setVariable("price", 1.5d);
        setVariable(mc, "price", "2.5");
        assertEquals(2.5d, mc.getVariable("price"));
        mc.setVariable("flag", false);
        setVariable(mc, "flag", "true");
        assertEquals(Boolean.TRUE, mc.getVariable("flag"));
        mc.setVariable("obj", new JsonObject());
        setVariable(mc, "obj", "{\"a\":1}");
        assertEquals(1, ((JsonObject) mc.getVariable("obj")).get("a").getAsInt());
        mc.setVariable("doc", TestUtils.createOMElement("<a/>"));
        setVariable(mc, "doc", "<b>1</b>");
        assertEquals("b", ((OMElement) mc.getVariable("doc")).getLocalName());
        mc.setVariable("name", "x");
        setVariable(mc, "name", "abc");
        assertEquals("abc", mc.getVariable("name"));
    }

    public void testSetNewVariableAsString() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order/>");
        setVariable(mc, "fresh", "5");
        assertEquals("5", mc.getVariable("fresh"));
    }

    public void testSetInvalidVariableValueKeepsValue() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order/>");
        mc.setVariable("count", 1);
        setVariable(mc, "count", "abc");
        assertEquals(1, mc.getVariable("count"));
        OMElement doc = TestUtils.createOMElement("<a/>");
        mc.setVariable("doc", doc);
        setVariable(mc, "doc", "<a>");
        assertSame(doc, mc.getVariable("doc"));
    }

    private void setVariable(MessageContext mc, String name, String value) throws Exception {
        JSONObject variable = new JSONObject();
        variable.put("variable-name", name);
        variable.put("variable-value", value);
        JSONObject command = new JSONObject();
        command.put("command", "set");
        command.put("command-argument", "variable");
        command.put("variable", variable);
        dm.setMessageContext(mc);
        try {
            dm.processDebugCommand(command.toString());
        } finally {
            dm.setMessageContext(null);
        }
    }

    public void testSetPropertyKeepsExistingType() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order/>");
        mc.setProperty("count", 1);
        setProperty(mc, "default", "count", "5");
        assertEquals(5, mc.getProperty("count"));
        mc.setProperty("ratio", 1.5f);
        setProperty(mc, "synapse", "ratio", "2.5");
        assertEquals(2.5f, mc.getProperty("ratio"));
        mc.setProperty("small", (short) 1);
        setProperty(mc, "default", "small", "7");
        assertEquals((short) 7, mc.getProperty("small"));
        mc.setProperty("flag", true);
        setProperty(mc, "default", "flag", "FALSE");
        assertEquals(Boolean.FALSE, mc.getProperty("flag"));
        mc.setProperty("doc", TestUtils.createOMElement("<a/>"));
        setProperty(mc, "default", "doc", "<b>1</b>");
        assertEquals("b", ((OMElement) mc.getProperty("doc")).getLocalName());
        setProperty(mc, "default", "fresh", "5");
        assertEquals("5", mc.getProperty("fresh"));

        getAxis2Context(mc).setProperty("timeout", 10L);
        setProperty(mc, "axis2", "timeout", "20");
        assertEquals(20L, getAxis2Context(mc).getProperty("timeout"));
        getAxis2Context(mc).getOptions().setProperty("retries", 1);
        setProperty(mc, "axis2-client", "retries", "3");
        assertEquals(3, getAxis2Context(mc).getOptions().getProperty("retries"));
    }

    public void testSetInvalidPropertyValueKeepsValue() throws Exception {
        MessageContext mc = TestUtils.getTestContextWithAxis2("<order/>");
        mc.setProperty("count", 1);
        setProperty(mc, "default", "count", "ad");
        assertEquals(1, mc.getProperty("count"));
        mc.setProperty("flag", true);
        setProperty(mc, "default", "flag", "yes");
        assertEquals(Boolean.TRUE, mc.getProperty("flag"));
        getAxis2Context(mc).setProperty("timeout", 10L);
        setProperty(mc, "axis2", "timeout", "1.5");
        assertEquals(10L, getAxis2Context(mc).getProperty("timeout"));
    }

    private void setProperty(MessageContext mc, String context, String name, String value) throws Exception {
        JSONObject property = new JSONObject();
        property.put("property-name", name);
        property.put("property-value", value);
        JSONObject command = new JSONObject();
        command.put("command", "set");
        command.put("command-argument", "property");
        command.put("context", context);
        command.put("property", property);
        dm.setMessageContext(mc);
        try {
            dm.processDebugCommand(command.toString());
        } finally {
            dm.setMessageContext(null);
        }
    }

    private void setPayload(MessageContext mc, String payload) throws Exception {
        JSONObject property = new JSONObject();
        property.put("property-name", "Envelope");
        property.put("property-value", payload);
        JSONObject command = new JSONObject();
        command.put("command", "set");
        command.put("command-argument", "property");
        command.put("context", "Envelope");
        command.put("property", property);
        dm.setMessageContext(mc);
        try {
            dm.processDebugCommand(command.toString());
        } finally {
            dm.setMessageContext(null);
        }
    }

    private static void assertJsonPayload(MessageContext mc, String expected) {
        assertEquals(JsonParser.parseString(expected),
                JsonParser.parseString(JsonUtil.jsonPayloadToString(getAxis2Context(mc))));
    }

    private static org.apache.axis2.context.MessageContext getAxis2Context(MessageContext mc) {
        return ((Axis2MessageContext) mc).getAxis2MessageContext();
    }

    private static void setMessageType(MessageContext mc, String messageType) {
        getAxis2Context(mc).setProperty(Constants.Configuration.MESSAGE_TYPE, messageType);
    }

    private static void assertBodyContainsOnly(MessageContext mc, String localName) {
        OMElement first = mc.getEnvelope().getBody().getFirstElement();
        assertNotNull(first);
        assertEquals(localName, first.getLocalName());
        for (OMNode node = first.getNextOMSibling(); node != null; node = node.getNextOMSibling()) {
            assertFalse("unexpected element in the SOAP body", node instanceof OMElement);
        }
    }

}
