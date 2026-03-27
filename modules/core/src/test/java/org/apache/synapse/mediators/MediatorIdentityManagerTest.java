/*
 * Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.mediators;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.config.xml.rest.APIFactory;
import org.apache.synapse.config.xml.ProxyServiceFactory;
import org.apache.synapse.config.xml.SequenceMediatorFactory;
import org.apache.synapse.config.xml.TemplateMediatorFactory;
import org.apache.synapse.api.API;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.eip.splitter.CloneMediator;
import org.apache.synapse.mediators.eip.splitter.IterateMediator;
import org.apache.synapse.mediators.filters.FilterMediator;
import org.apache.synapse.mediators.filters.SwitchMediator;
import org.apache.synapse.mediators.template.TemplateMediator;

import java.util.Properties;

/**
 * Test cases for MediatorIdentityManager to verify hierarchical ID assignment.
 */
public class MediatorIdentityManagerTest extends TestCase {

    private MediatorIdentityManager idManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        idManager = MediatorIdentityManager.getInstance();
    }

    /**
     * Test ID assignment for a simple sequence with basic mediators.
     */
    public void testSimpleSequenceIdAssignment() {
        String sequenceXml = 
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<sequence name=\"TestSequence\" trace=\"disable\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "    <log category=\"INFO\" logMessageID=\"false\" logFullPayload=\"false\">" +
                "        <message>Sample log</message>" +
                "    </log>" +
                "    <variable name=\"varName\" type=\"STRING\" value=\"sample-value\"/>" +
                "</sequence>";

        OMElement sequenceElement = createOMElement(sequenceXml);
        SequenceMediator sequence = (SequenceMediator) new SequenceMediatorFactory().createMediator(sequenceElement, new Properties());

        idManager.assignMediatorIds(sequence);

        // Verify mediator IDs
        assertEquals("sequence:TestSequence/1.Log", 
                ((AbstractMediator) sequence.getList().get(0)).getMediatorId());
        assertEquals("sequence:TestSequence/2.Variable[varName]", 
                ((AbstractMediator) sequence.getList().get(1)).getMediatorId());
    }

    /**
     * Test ID assignment for an API with resources.
     */
    public void testAPIIdAssignment() {
        String apiXml = 
                "<api name=\"TestAPI\" context=\"/test\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "  <resource methods=\"GET\" uri-template=\"/users/{id}\">" +
                "    <inSequence>" +
                "      <log level=\"custom\"/>" +
                "      <property name=\"requestType\" value=\"GET\"/>" +
                "      <respond/>" +
                "    </inSequence>" +
                "    <outSequence>" +
                "      <log level=\"full\"/>" +
                "      <send/>" +
                "    </outSequence>" +
                "  </resource>" +
                "</api>";

        OMElement apiElement = createOMElement(apiXml);
        API api = APIFactory.createAPI(apiElement, new Properties());

        idManager.assignMediatorIds(api);

        // Verify inSequence mediator IDs
        SequenceMediator inSequence = api.getResources()[0].getInSequence();
        assertEquals("api:TestAPI/GET[/users/{id}]/in/1.Log", 
                ((AbstractMediator) inSequence.getList().get(0)).getMediatorId());
        assertEquals("api:TestAPI/GET[/users/{id}]/in/2.Property[requestType]", 
                ((AbstractMediator) inSequence.getList().get(1)).getMediatorId());
        assertEquals("api:TestAPI/GET[/users/{id}]/in/3.Respond", 
                ((AbstractMediator) inSequence.getList().get(2)).getMediatorId());
        
        // Verify outSequence mediator IDs
        SequenceMediator outSequence = api.getResources()[0].getOutSequence();
        assertEquals("api:TestAPI/GET[/users/{id}]/out/1.Log", 
                ((AbstractMediator) outSequence.getList().get(0)).getMediatorId());
        assertEquals("api:TestAPI/GET[/users/{id}]/out/2.Send", 
                ((AbstractMediator) outSequence.getList().get(1)).getMediatorId());
    }

    /**
     * Test ID assignment for a ProxyService.
     */
    public void testProxyServiceIdAssignment() {
        String proxyXml = 
                "<proxy name=\"TestProxy\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "  <target>" +
                "    <inSequence>" +
                "      <log level=\"custom\"/>" +
                "      <property name=\"proxyProp\" value=\"test\"/>" +
                "    </inSequence>" +
                "    <outSequence>" +
                "      <log level=\"full\"/>" +
                "      <send/>" +
                "    </outSequence>" +
                "    <faultSequence>" +
                "      <log level=\"custom\"/>" +
                "      <drop/>" +
                "    </faultSequence>" +
                "  </target>" +
                "</proxy>";

        OMElement proxyElement = createOMElement(proxyXml);
        ProxyService proxy = ProxyServiceFactory.createProxy(proxyElement, new Properties());

        idManager.assignMediatorIds(proxy);

        // Verify inSequence mediator IDs
        SequenceMediator inSequence = proxy.getTargetInLineInSequence();
        assertEquals("proxy:TestProxy/in/1.Log", 
                ((AbstractMediator) inSequence.getList().get(0)).getMediatorId());
        assertEquals("proxy:TestProxy/in/2.Property[proxyProp]", 
                ((AbstractMediator) inSequence.getList().get(1)).getMediatorId());
        
        // Verify outSequence mediator IDs
        SequenceMediator outSequence = proxy.getTargetInLineOutSequence();
        assertEquals("proxy:TestProxy/out/1.Log", 
                ((AbstractMediator) outSequence.getList().get(0)).getMediatorId());
        assertEquals("proxy:TestProxy/out/2.Send", 
                ((AbstractMediator) outSequence.getList().get(1)).getMediatorId());
        
        // Verify faultSequence mediator IDs
        SequenceMediator faultSequence = proxy.getTargetInLineFaultSequence();
        assertEquals("proxy:TestProxy/fault/1.Log", 
                ((AbstractMediator) faultSequence.getList().get(0)).getMediatorId());
        assertEquals("proxy:TestProxy/fault/2.Drop", 
                ((AbstractMediator) faultSequence.getList().get(1)).getMediatorId());
    }

    /**
     * Test ID assignment for a Template.
     */
    public void testTemplateIdAssignment() {
        String templateXml = 
                "<template name=\"TestTemplate\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "    <parameter name=\"message\"/>" +
                "    <sequence>" +
                "        <log level=\"custom\">" +
                "            <property expression=\"$func:message\" name=\"GREETING_MESSAGE\"/>" +
                "        </log>" +
                "    </sequence>" +
                "</template>";

        OMElement templateElement = createOMElement(templateXml);
        TemplateMediator template = (TemplateMediator) new TemplateMediatorFactory().createMediator(templateElement, new Properties());

        idManager.assignMediatorIds(template);

        // Verify mediator IDs
        assertEquals("template:TestTemplate/1.Log",
                ((AbstractMediator) template.getList().get(0)).getMediatorId());
    }

    /**
     * Test ID assignment for a sequence with nested mediators (Filter mediator).
     */
    public void testSequenceWithFilterMediatorIdAssignment() {
        String sequenceXml = 
                "<sequence name=\"FilterTestSequence\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "  <log level=\"custom\"/>" +
                "  <filter xpath=\"//user/status='active'\">" +
                "    <then>" +
                "      <log level=\"simple\"/>" +
                "      <property name=\"status\" value=\"active\"/>" +
                "    </then>" +
                "    <else>" +
                "      <log level=\"simple\"/>" +
                "      <property name=\"status\" value=\"inactive\"/>" +
                "    </else>" +
                "  </filter>" +
                "  <log level=\"full\"/>" +
                "</sequence>";

        OMElement sequenceElement = createOMElement(sequenceXml);
        SequenceMediator sequence = (SequenceMediator) new SequenceMediatorFactory().createMediator(sequenceElement, new Properties());

        idManager.assignMediatorIds(sequence);

        // Verify top-level mediators
        assertEquals("sequence:FilterTestSequence/1.Log", 
                ((AbstractMediator) sequence.getList().get(0)).getMediatorId());
        
        FilterMediator filterMediator = (FilterMediator) sequence.getList().get(1);
        assertEquals("sequence:FilterTestSequence/2.Filter", 
                filterMediator.getMediatorId());
        
        // Verify then branch mediators
        assertEquals("sequence:FilterTestSequence/2.Filter/then/1.Log", 
                ((AbstractMediator) filterMediator.getList().get(0)).getMediatorId());
        assertEquals("sequence:FilterTestSequence/2.Filter/then/2.Property[status]", 
                ((AbstractMediator) filterMediator.getList().get(1)).getMediatorId());
        
        // Verify else branch mediators
        assertEquals("sequence:FilterTestSequence/2.Filter/else/1.Log", 
                ((AbstractMediator) filterMediator.getElseMediator().getList().get(0)).getMediatorId());
        assertEquals("sequence:FilterTestSequence/2.Filter/else/2.Property[status]", 
                ((AbstractMediator) filterMediator.getElseMediator().getList().get(1)).getMediatorId());
        
        assertEquals("sequence:FilterTestSequence/3.Log", 
                ((AbstractMediator) sequence.getList().get(2)).getMediatorId());
    }

    /**
     * Test ID assignment for a sequence with Switch mediator.
     */
    public void testSequenceWithSwitchMediatorIdAssignment() {
        String sequenceXml = 
                "<sequence name=\"SwitchTestSequence\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "  <log level=\"custom\"/>" +
                "  <switch source=\"//user/role\">" +
                "    <case regex=\"admin\">" +
                "      <log level=\"simple\"/>" +
                "      <property name=\"role\" value=\"admin\"/>" +
                "    </case>" +
                "    <case regex=\"user\">" +
                "      <log level=\"simple\"/>" +
                "      <property name=\"role\" value=\"user\"/>" +
                "    </case>" +
                "    <default>" +
                "      <log level=\"simple\"/>" +
                "      <property name=\"role\" value=\"guest\"/>" +
                "    </default>" +
                "  </switch>" +
                "  <log level=\"full\"/>" +
                "</sequence>";

        OMElement sequenceElement = createOMElement(sequenceXml);
        SequenceMediator sequence = (SequenceMediator) new SequenceMediatorFactory().createMediator(sequenceElement, new Properties());

        idManager.assignMediatorIds(sequence);

        // Verify top-level mediators
        assertEquals("sequence:SwitchTestSequence/1.Log", 
                ((AbstractMediator) sequence.getList().get(0)).getMediatorId());
        
        SwitchMediator switchMediator = (SwitchMediator) sequence.getList().get(1);
        assertEquals("sequence:SwitchTestSequence/2.Switch", 
                switchMediator.getMediatorId());
        
        // Verify case 1 (admin) mediators
        assertEquals("sequence:SwitchTestSequence/2.Switch/case[admin]/1.Log",
                ((AbstractMediator) switchMediator.getCases().get(0).getCaseMediator().getList().get(0)).getMediatorId());
        assertEquals("sequence:SwitchTestSequence/2.Switch/case[admin]/2.Property[role]",
                ((AbstractMediator) switchMediator.getCases().get(0).getCaseMediator().getList().get(1)).getMediatorId());
        
        // Verify case 2 (user) mediators
        assertEquals("sequence:SwitchTestSequence/2.Switch/case[user]/1.Log",
                ((AbstractMediator) switchMediator.getCases().get(1).getCaseMediator().getList().get(0)).getMediatorId());
        assertEquals("sequence:SwitchTestSequence/2.Switch/case[user]/2.Property[role]",
                ((AbstractMediator) switchMediator.getCases().get(1).getCaseMediator().getList().get(1)).getMediatorId());
        
        // Verify default case mediators
        assertEquals("sequence:SwitchTestSequence/2.Switch/default/1.Log",
                ((AbstractMediator) switchMediator.getDefaultCase().getCaseMediator().getList().get(0)).getMediatorId());
        assertEquals("sequence:SwitchTestSequence/2.Switch/default/2.Property[role]",
                ((AbstractMediator) switchMediator.getDefaultCase().getCaseMediator().getList().get(1)).getMediatorId());
        
        assertEquals("sequence:SwitchTestSequence/3.Log", 
                ((AbstractMediator) sequence.getList().get(2)).getMediatorId());
    }

    /**
     * Test ID assignment for a sequence with Clone mediator.
     */
    public void testCloneMediatorIdAssignment() {
        String sequenceXml = 
                "<sequence name=\"CloneTestSequence\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "  <log level=\"custom\"/>" +
                "  <clone>" +
                "    <target>" +
                "      <sequence>" +
                "        <log level=\"simple\"/>" +
                "        <property name=\"target\" value=\"1\"/>" +
                "      </sequence>" +
                "    </target>" +
                "    <target>" +
                "      <sequence>" +
                "        <log level=\"simple\"/>" +
                "        <property name=\"target\" value=\"2\"/>" +
                "      </sequence>" +
                "    </target>" +
                "  </clone>" +
                "  <log level=\"full\"/>" +
                "</sequence>";

        OMElement sequenceElement = createOMElement(sequenceXml);
        SequenceMediator sequence = (SequenceMediator) new SequenceMediatorFactory().createMediator(sequenceElement, new Properties());

        idManager.assignMediatorIds(sequence);

        // Verify top-level mediators
        assertEquals("sequence:CloneTestSequence/1.Log", 
                ((AbstractMediator) sequence.getList().get(0)).getMediatorId());
        
        CloneMediator cloneMediator = (CloneMediator) sequence.getList().get(1);
        assertEquals("sequence:CloneTestSequence/2.Clone", 
                cloneMediator.getMediatorId());
        
        // Verify nested mediators in clone targets
        // Target 1 mediators
        Target target1 = cloneMediator.getTargets().get(0);
        SequenceMediator target1Seq = target1.getSequence();
        assertEquals("sequence:CloneTestSequence/2.Clone/target[1]/1.Log",
                ((AbstractMediator) target1Seq.getList().get(0)).getMediatorId());
        assertEquals("sequence:CloneTestSequence/2.Clone/target[1]/2.Property[target]",
                ((AbstractMediator) target1Seq.getList().get(1)).getMediatorId());
        
        // Target 2 mediators
        Target target2 = cloneMediator.getTargets().get(1);
        SequenceMediator target2Seq = target2.getSequence();
        assertEquals("sequence:CloneTestSequence/2.Clone/target[2]/1.Log",
                ((AbstractMediator) target2Seq.getList().get(0)).getMediatorId());
        assertEquals("sequence:CloneTestSequence/2.Clone/target[2]/2.Property[target]",
                ((AbstractMediator) target2Seq.getList().get(1)).getMediatorId());
        
        assertEquals("sequence:CloneTestSequence/3.Log", 
                ((AbstractMediator) sequence.getList().get(2)).getMediatorId());
    }

    /**
     * Test ID assignment for a sequence with ForEach mediator.
     */
    public void testForEachMediatorIdAssignment() {
        String sequenceXml = 
                "<sequence name=\"ForEachTestSequence\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "  <log level=\"custom\"/>" +
                "  <foreach expression=\"//users/user\">" +
                "    <sequence>" +
                "      <log level=\"simple\"/>" +
                "      <property name=\"userName\" expression=\"//user/name\"/>" +
                "    </sequence>" +
                "  </foreach>" +
                "  <log level=\"full\"/>" +
                "</sequence>";

        OMElement sequenceElement = createOMElement(sequenceXml);
        SequenceMediator sequence = (SequenceMediator) new SequenceMediatorFactory().createMediator(sequenceElement, new Properties());

        idManager.assignMediatorIds(sequence);

        // Verify top-level mediators
        assertEquals("sequence:ForEachTestSequence/1.Log", 
                ((AbstractMediator) sequence.getList().get(0)).getMediatorId());
        
        ForEachMediator forEachMediator = (ForEachMediator) sequence.getList().get(1);
        assertEquals("sequence:ForEachTestSequence/2.ForEach", 
                forEachMediator.getMediatorId());
        
        // Verify nested mediators inside foreach sequence
        SequenceMediator forEachSequence = forEachMediator.getSequence();
        assertEquals("sequence:ForEachTestSequence/2.ForEach/1.Log",
                ((AbstractMediator) forEachSequence.getList().get(0)).getMediatorId());
        assertEquals("sequence:ForEachTestSequence/2.ForEach/2.Property[userName]",
                ((AbstractMediator) forEachSequence.getList().get(1)).getMediatorId());
        
        assertEquals("sequence:ForEachTestSequence/3.Log", 
                ((AbstractMediator) sequence.getList().get(2)).getMediatorId());
    }

    /**
     * Test ID assignment for a sequence with Iterate mediator.
     */
    public void testIterateMediatorIdAssignment() {
        String sequenceXml = 
                "<sequence name=\"IterateTestSequence\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "  <log level=\"custom\"/>" +
                "  <iterate expression=\"//users/user\">" +
                "    <target>" +
                "      <sequence>" +
                "        <log level=\"simple\"/>" +
                "        <property name=\"userName\" expression=\"//user/name\"/>" +
                "      </sequence>" +
                "    </target>" +
                "  </iterate>" +
                "  <log level=\"full\"/>" +
                "</sequence>";

        OMElement sequenceElement = createOMElement(sequenceXml);
        SequenceMediator sequence = (SequenceMediator) new SequenceMediatorFactory().createMediator(sequenceElement, new Properties());

        idManager.assignMediatorIds(sequence);

        // Verify top-level mediators
        assertEquals("sequence:IterateTestSequence/1.Log", 
                ((AbstractMediator) sequence.getList().get(0)).getMediatorId());
        
        IterateMediator iterateMediator = (IterateMediator) sequence.getList().get(1);
        assertEquals("sequence:IterateTestSequence/2.Iterate", 
                iterateMediator.getMediatorId());
        
        // Verify nested mediators inside iterate target sequence
        SequenceMediator iterateSequence = iterateMediator.getTarget().getSequence();
        assertEquals("sequence:IterateTestSequence/2.Iterate/1.Log",
                ((AbstractMediator) iterateSequence.getList().get(0)).getMediatorId());
        assertEquals("sequence:IterateTestSequence/2.Iterate/2.Property[userName]",
                ((AbstractMediator) iterateSequence.getList().get(1)).getMediatorId());
        
        assertEquals("sequence:IterateTestSequence/3.Log", 
                ((AbstractMediator) sequence.getList().get(2)).getMediatorId());
    }

    /**
     * Helper method to create OM element from XML string.
     */
    private OMElement createOMElement(String xml) {
        try {
            return AXIOMUtil.stringToOM(xml);
        } catch (Exception e) {
            fail("Failed to create OM element: " + e.getMessage());
            return null;
        }
    }
}
