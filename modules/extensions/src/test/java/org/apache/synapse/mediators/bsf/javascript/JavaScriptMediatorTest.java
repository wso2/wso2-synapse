/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.bsf.javascript;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.bsf.ScriptMediator;
import org.apache.synapse.script.access.AccessControlConfig;
import org.apache.synapse.script.access.AccessControlConstants;
import org.apache.synapse.script.access.AccessControlListType;
import org.apache.synapse.script.access.ScriptAccessControl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.apache.synapse.script.access.AccessControlConstants.ENABLE;

public class JavaScriptMediatorTest extends TestCase {

    public void testInlineMediator() throws Exception {
        ScriptMediator mediator = new ScriptMediator("rhinoJs", "mc.getPayloadXML().b == 'petra';",null);

        MessageContext mc = TestUtils.getTestContext("<a><b>petra</b></a>", null);
        assertTrue(mediator.mediate(mc));

        mc = TestUtils.getTestContext("<a><b>sue</b></a>", null);
        assertFalse(mediator.mediate(mc));

        mc = TestUtils.getTestContext("<a><b>petra</b></a>", null);
        assertTrue(mediator.mediate(mc));
    }

    public void testInlineMediator2() throws Exception {
        ScriptMediator mediator = new ScriptMediator("rhinoJs", "mc.getPayloadXML().b == 'petra';",null);

        MessageContext mc = TestUtils.getTestContext("<a><b>petra</b></a>", null);
        assertTrue(mediator.mediate(mc));

        mc = TestUtils.getTestContext("<a><b>sue</b></a>", null);
        assertFalse(mediator.mediate(mc));

        mc = TestUtils.getTestContext("<a><b>petra</b></a>", null);
        assertTrue(mediator.mediate(mc));
    }

    public void testInlineMediatorWithImports() throws Exception {

        String scriptSourceCode = "var uuid = java.util.UUID.randomUUID().toString().replace('-','');\n";

        MessageContext mc = TestUtils.getTestContext("<foo/>", null);
        ScriptMediator mediator = new ScriptMediator("js", scriptSourceCode, null);

        boolean response = mediator.mediate(mc);
        assertTrue(response);
    }

    /**
     * Test controlling access to java classes through JS
     * @throws Exception
     */
    public void testJavaClassAccessControl() throws Exception {
        ScriptAccessControl.getInstance().setClassAccessControlConfig(
                new AccessControlConfig(true, AccessControlListType.valueOf("BLOCK_LIST"),
                        Collections.singletonList("java.util.ArrayList")));

        String scriptSourceCode =  "var s = new java.util.ArrayList();\n";

        MessageContext mc = TestUtils.getTestContext("<foo/>", null);
        ScriptMediator mediator = new ScriptMediator("rhinoJs", scriptSourceCode, null);

        System.setProperty("properties.file.path", System.getProperty("user.dir") + "/src/test/resources/file.properties");

        boolean synapseExceptionThrown = false;
        try {
            mediator.mediate(mc);
        } catch(SynapseException e) {
            synapseExceptionThrown = true;
        }

        assertTrue("As Java class access control is configured " +
                "SynapseException should be thrown during mediation", synapseExceptionThrown);

    }

    /**
     * Test controlling access to java methods through JS
     * @throws Exception
     */
    public void testJavaMethodAccessControl() throws Exception {
        ScriptAccessControl.getInstance().setNativeObjectAccessControlConfig(
                new AccessControlConfig(true, AccessControlListType.valueOf("BLOCK_LIST"),
                        Arrays.asList("getClassLoader", "loadClass")));

        String scriptSourceCode =  "var c = this.context.getClass();\n" +
                "var hashmapConstructors = c.getClassLoader().loadClass(\"java.util.HashMap\").getDeclaredConstructors();\n";

        MessageContext mc = TestUtils.getTestContext("<foo/>", null);
        ScriptMediator mediator = new ScriptMediator("rhinoJs", scriptSourceCode, null);

        System.setProperty("properties.file.path", System.getProperty("user.dir") + "/src/test/resources/file.properties");

        boolean synapseExceptionThrown = false;
        try {
            mediator.mediate(mc);
        } catch(SynapseException e) {
            synapseExceptionThrown = true;
        }

        assertTrue("As Java method access control is configured " +
                "SynapseException should be thrown during mediation", synapseExceptionThrown);

    }
}
