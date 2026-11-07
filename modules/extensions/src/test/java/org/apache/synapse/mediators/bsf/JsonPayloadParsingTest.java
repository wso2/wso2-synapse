/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.bsf;

import junit.framework.TestCase;

import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.sun.phobos.script.javascript.RhinoScriptEngineFactory;

/**
 * Tests the parsing of JSON payloads by the script mediator.
 * <p/>
 * A payload is accepted only when it is well formed JSON. Payloads that are accepted keep their
 * structure, and a payload that is rejected does not change how a later payload is parsed.
 */
public class JsonPayloadParsingTest extends TestCase {

    /**
     * Payloads that are not well formed JSON. Several of them are well formed JavaScript object
     * literals, which is why they need to be listed explicitly.
     */
    private static final String[] INVALID_PAYLOADS = {
            "{item:\"book\"}",                  // key is not quoted
            "{'item':'book'}",                  // single quoted key and value
            "{\"item\":'book'}",                // single quoted value
            "{\"item\":\"book\",}",             // trailing comma in an object
            "[1,2,]",                           // trailing comma in an array
            "{\"item\":\"book\" /* note */}",   // block comment
            "{\"item\":\"book\" // note\n}",    // line comment
            "{\"count\":0x10}",                 // hexadecimal number
            "{\"count\":+1}",                   // number with a leading plus
            "{\"count\":.5}",                   // number with a leading dot
            "{\"count\":1.}",                   // number with a trailing dot
            "{\"count\":Infinity}",             // Infinity is not a JSON value
            "{\"count\":NaN}",                  // NaN is not a JSON value
            "{\"item\":undefined}",             // undefined is not a JSON value
            "[1,,2]",                           // missing array element
            "{\"item\":\"line\tbreak\"}",       // unescaped control character in a string
            "{\"item\":\"unterminated}",        // string is not terminated
            "{\"item\":}",                      // value is missing
            "{\"item\" \"book\"}",              // separator is missing
            "{\"item\":\"book\"",               // object is not closed
            "{",                                // truncated
            "",                                 // empty
            "   ",                              // blank
            "book"                              // not JSON at all
    };

    private CommonScriptMessageContext axis2Context() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        manager.registerEngineExtension("jsEngine", new RhinoScriptEngineFactory());
        ScriptEngine engine = manager.getEngineByExtension("jsEngine");
        MessageContext mc = TestUtils.getAxis2MessageContext("<foo/>", null);
        CommonScriptMessageContext scriptMC = new CommonScriptMessageContext(mc, new JavaScriptXmlHelper());
        scriptMC.setScriptEngine(engine);
        return scriptMC;
    }

    private CommonScriptMessageContext context() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        manager.registerEngineExtension("jsEngine", new RhinoScriptEngineFactory());
        ScriptEngine engine = manager.getEngineByExtension("jsEngine");
        MessageContext mc = TestUtils.getTestContext("<foo/>", null);
        CommonScriptMessageContext scriptMC = new CommonScriptMessageContext(mc, new JavaScriptXmlHelper());
        scriptMC.setScriptEngine(engine);
        return scriptMC;
    }

    private Object parse(CommonScriptMessageContext scriptMC, String payload) throws ScriptException {
        return scriptMC.jsonSerializerCallMember("parse", payload);
    }

    public void testValidPayloadIsParsed() throws Exception {
        CommonScriptMessageContext scriptMC = context();
        Object parsed = parse(scriptMC, "{\"item\":\"book\",\"tier\":\"standard\"}");
        assertNotNull(parsed);
        assertEquals("book", String.valueOf(
                ScriptableObject.getProperty((Scriptable) parsed, "item")));
    }

    /**
     * Objects, arrays, numbers, booleans and nulls keep their structure through the parser.
     */
    public void testStructureIsPreserved() throws Exception {
        CommonScriptMessageContext scriptMC = context();
        Object parsed = parse(scriptMC,
                "{\"count\":1.5,\"active\":true,\"note\":null,\"tags\":[\"a\",\"b\"],"
                        + "\"meta\":{\"owner\":\"bob\"}}");
        assertNotNull(parsed);
        Scriptable object = (Scriptable) parsed;

        assertEquals(1.5, ((Number) ScriptableObject.getProperty(object, "count")).doubleValue(), 0.0);
        assertEquals(Boolean.TRUE, ScriptableObject.getProperty(object, "active"));
        assertNull(ScriptableObject.getProperty(object, "note"));

        Scriptable tags = (Scriptable) ScriptableObject.getProperty(object, "tags");
        assertEquals("b", String.valueOf(ScriptableObject.getProperty(tags, 1)));

        Scriptable meta = (Scriptable) ScriptableObject.getProperty(object, "meta");
        assertEquals("bob", String.valueOf(ScriptableObject.getProperty(meta, "owner")));
    }

    public void testArrayPayloadIsParsed() throws Exception {
        CommonScriptMessageContext scriptMC = context();
        Object parsed = parse(scriptMC, "[{\"item\":\"book\"},{\"item\":\"pen\"}]");
        assertNotNull(parsed);
        Scriptable first = (Scriptable) ScriptableObject.getProperty((Scriptable) parsed, 0);
        assertEquals("book", String.valueOf(ScriptableObject.getProperty(first, "item")));
    }

    public void testEmptyObjectIsParsed() throws Exception {
        assertNotNull(parse(context(), "{}"));
    }

    /**
     * Every payload that is not well formed JSON is rejected.
     */
    public void testInvalidPayloadsAreRejected() throws Exception {
        for (String payload : INVALID_PAYLOADS) {
            CommonScriptMessageContext scriptMC = context();
            try {
                parse(scriptMC, payload);
                fail("Payload should have been rejected: " + payload);
            } catch (ScriptException expected) {
                // expected
            }
        }
    }

    /**
     * A script may set an empty payload. It becomes an empty JSON object, which is what the caller
     * of the API receives, and it is not an error.
     */
    public void testEmptyPayloadCanBeSet() throws Exception {
        CommonScriptMessageContext scriptMC = axis2Context();
        scriptMC.setPayloadJSON("");

        assertEquals("An empty payload should become an empty JSON object",
                "{}", scriptMC.getPayloadText());

        Object kept = scriptMC.getPayloadJSON();
        assertNotNull("An empty payload should still keep a JSON object", kept);
        assertEquals("An empty JSON object has no members",
                Scriptable.NOT_FOUND, ScriptableObject.getProperty((Scriptable) kept, "anything"));
    }

    /**
     * A payload can still be set after an empty one.
     */
    public void testPayloadCanBeSetAfterAnEmptyPayload() throws Exception {
        CommonScriptMessageContext scriptMC = axis2Context();
        scriptMC.setPayloadJSON("");
        scriptMC.setPayloadJSON("{\"item\":\"book\"}");

        assertEquals("{\"item\":\"book\"}", scriptMC.getPayloadText());
        assertEquals("book", String.valueOf(
                ScriptableObject.getProperty((Scriptable) scriptMC.getPayloadJSON(), "item")));
    }

    /**
     * A rejected payload does not change how a later payload is parsed.
     */
    public void testParsingContinuesAfterAnInvalidPayload() throws Exception {
        CommonScriptMessageContext scriptMC = context();
        for (String payload : INVALID_PAYLOADS) {
            try {
                parse(scriptMC, payload);
                fail("Payload should have been rejected: " + payload);
            } catch (ScriptException expected) {
                // expected
            }
        }
        Object parsed = parse(scriptMC, "{\"item\":\"pen\"}");
        assertNotNull(parsed);
        assertEquals("pen", String.valueOf(
                ScriptableObject.getProperty((Scriptable) parsed, "item")));
    }
    /**
     * A script object whose keys and string values hold quotes, backslashes and control characters
     * is written as valid JSON and kept as a JSON object.
     */
    public void testObjectWithSpecialCharactersCanBeSet() throws Exception {
        MessageContext mc = TestUtils.getTestContextJson("{\"item\":\"book\"}", null);
        ScriptMediator mediator = new ScriptMediator("rhinoJs",
                "var o = {};\n"
                        + "o['k\\t\"ey'] = 'a\\tb\\nc\"d\\\\e' + String.fromCharCode(1);\n"
                        + "o.joined = 'x' + String.fromCharCode(9) + 'y';\n"
                        + "mc.setPayloadJSON(o);", null);
        assertTrue(mediator.mediate(mc));

        String payload = JsonUtil.jsonPayloadToString(((Axis2MessageContext) mc).getAxis2MessageContext());
        assertEquals("{\"k\\t\\\"ey\":\"a\\tb\\nc\\\"d\\\\e\\u0001\", \"joined\":\"x\\ty\"}", payload);
    }

    /**
     * A JSON string set as a message property is parsed by the script engine of the mediator
     * language, so it does not fail for GraalJS.
     */
    public void testJsonStringPropertyIsParsedForGraalJs() throws Exception {
        MessageContext mc = TestUtils.getAxis2MessageContext("<foo/>", null);
        ((Axis2MessageContext) mc).getAxis2MessageContext().setProperty("JSON_STRING", "{\"item\":\"book\"}");
        ScriptMediator mediator = new ScriptMediator("js",
                "mc.setProperty('item', mc.getPayloadJSON().item);", null);
        assertTrue(mediator.mediate(mc));
        assertEquals("book", mc.getProperty("item"));
    }
}
