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

package org.apache.synapse.mediators.bsf;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.mediators.TestUtils;

public class ScriptMediatorFactoryTest extends TestCase {

    private static final OMElement INLINE_MEDIATOR_CONFIG = TestUtils.createOMElement(
       "<script language='js'>true</script>");

    private static final OMElement REG_PROP_MEDIATOR_CONFIG = TestUtils.createOMElement(
       "<script language='js' key='MyMediator'/>");
    
    private static final OMElement REG_PROP_FOO_FUNC_MEDIATOR_CONFIG = TestUtils.createOMElement(
       "<script language='js' key='MyFooMediator' function='foo'/>");

    private static final OMElement JS_ENGINE_ALIAS_MEDIATOR_CONFIG = TestUtils.createOMElement(
       "<script language='jsEngine'>true</script>");

    private static final OMElement MJS_ALIAS_MEDIATOR_CONFIG = TestUtils.createOMElement(
       "<script language='mjs'>true</script>");

    private static final OMElement GROOVY_MEDIATOR_CONFIG = TestUtils.createOMElement(
       "<script language='groovy'>true</script>");

    private static final OMElement MY_MEDIATOR = TestUtils.createOMElement(
       "<x><![CDATA[ function mediate(mc) { return true;} ]]></x>");

    private static final OMElement MY_MEDIATOR_FOO_FUNC = TestUtils.createOMElement(
       "<x><![CDATA[ function foo(mc) { return true;} ]]></x>");

    public void testInlineScriptMediatorFactory() throws XMLStreamException {
        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        Mediator mediator = mf.createMediator(INLINE_MEDIATOR_CONFIG, new Properties());
        try{
            MessageContext mc = TestUtils.getTestContext("<foo/>",null);
            assertTrue(mediator.mediate(mc));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void testRegPropMediatorFactory() throws Exception {

        Entry prop = new Entry();
        prop.setKey("MyMediator");
        prop.setValue(MY_MEDIATOR);
        Map<String,Entry> props = new HashMap<String,Entry>();
        props.put("MyMediator", prop);
        MessageContext mc = TestUtils.getTestContext("<foo/>", props);

        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        Mediator mediator = mf.createMediator(REG_PROP_MEDIATOR_CONFIG, new Properties());
        assertTrue(mediator.mediate(mc));
    }

    public void testRegPropWithFunctionMediatorFactory() throws Exception {
        Entry prop = new Entry();
        prop.setValue(MY_MEDIATOR_FOO_FUNC);
        Map<String,Entry> props = new HashMap<String,Entry>();
        props.put("MyFooMediator", prop);
        MessageContext mc = TestUtils.getTestContext("<foo/>", props);

        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        Mediator mediator = mf.createMediator(REG_PROP_FOO_FUNC_MEDIATOR_CONFIG, new Properties());
        assertTrue(mediator.mediate(mc));
    }

    /**
     * "jsEngine" is the extension under which the internal Rhino engine is registered. It is not a
     * supported script language and must not be selectable through the 'language' attribute.
     */
    public void testJsEngineAliasIsNotSelectableAsLanguage() {
        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        try {
            mf.createMediator(JS_ENGINE_ALIAS_MEDIATOR_CONFIG, new Properties());
            fail("'jsEngine' must not be usable as a script mediator language");
        } catch (SynapseException e) {
            assertTrue("Unexpected message: " + e.getMessage(),
                    e.getMessage().contains("No script engine found for language"));
        }
    }

    /**
     * A script engine factory may accept names beyond the documented ones, such as "mjs" for
     * JavaScript. A deployment that lists the languages it uses rejects those names.
     */
    public void testMjsIsRejectedWhenNotInAllowList() throws Exception {
        setAllowedLanguages("js,rhinoJs");
        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        try {
            mf.createMediator(MJS_ALIAS_MEDIATOR_CONFIG, new Properties());
            fail("'mjs' must not be usable when it is absent from the allow list");
        } catch (SynapseException e) {
            assertTrue("Unexpected message: " + e.getMessage(),
                    e.getMessage().contains("Unsupported script language"));
        }
    }

    /**
     * The allow list is not configured by default, in which case no language is restricted by it.
     */
    public void testNoRestrictionWhenAllowListIsNotConfigured() throws Exception {
        setAllowedLanguages(null);
        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        assertNotNull(mf.createMediator(INLINE_MEDIATOR_CONFIG, new Properties()));
        assertNotNull(mf.createMediator(GROOVY_MEDIATOR_CONFIG, new Properties()));
    }

    /**
     * A language that is present in the configured allow list is accepted, and surrounding
     * whitespace in the configured value is ignored.
     */
    public void testLanguageInAllowListIsAccepted() throws Exception {
        setAllowedLanguages(" js , rhinoJs ");
        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        assertNotNull(mf.createMediator(INLINE_MEDIATOR_CONFIG, new Properties()));
    }

    /**
     * A language that is absent from the configured allow list is rejected, even when an engine for
     * it is available, which shows the check is applied before the engine is resolved.
     */
    public void testLanguageNotInAllowListIsRejected() throws Exception {
        setAllowedLanguages("js,rhinoJs");
        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        try {
            mf.createMediator(GROOVY_MEDIATOR_CONFIG, new Properties());
            fail("'groovy' must not be usable when it is absent from the allow list");
        } catch (SynapseException e) {
            assertTrue("Unexpected message: " + e.getMessage(),
                    e.getMessage().contains("Unsupported script language"));
        }
    }

    /**
     * An empty allow list is a deliberate configuration meaning no language is permitted, and is
     * distinct from the property being absent.
     */
    public void testEmptyAllowListRejectsEveryLanguage() throws Exception {
        setAllowedLanguages("");
        assertEveryLanguageRejected();
    }

    /**
     * A value that contains only separators or whitespace carries no language and is treated the
     * same as an empty list.
     */
    public void testBlankAllowListRejectsEveryLanguage() throws Exception {
        setAllowedLanguages("  ");
        assertEveryLanguageRejected();
        setAllowedLanguages(",  ,");
        assertEveryLanguageRejected();
    }

    private void assertEveryLanguageRejected() {
        ScriptMediatorFactory mf = new ScriptMediatorFactory();
        OMElement[] configs = {INLINE_MEDIATOR_CONFIG, GROOVY_MEDIATOR_CONFIG,
                TestUtils.createOMElement("<script language=''>true</script>")};
        for (OMElement config : configs) {
            try {
                mf.createMediator(config, new Properties());
                fail("An empty allow list must reject every language");
            } catch (SynapseException e) {
                assertTrue("Unexpected message: " + e.getMessage(),
                        e.getMessage().contains("Unsupported script language"));
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        setAllowedLanguages(null);
        super.tearDown();
    }

    private static void setAllowedLanguages(String value) throws Exception {
        java.lang.reflect.Field field = SynapsePropertiesLoader.class.getDeclaredField("cacheProperties");
        field.setAccessible(true);
        Properties properties = (Properties) field.get(null);
        if (value == null) {
            properties.remove("synapse.script.mediator.allow.languages");
        } else {
            properties.setProperty("synapse.script.mediator.allow.languages", value);
        }
    }

}
