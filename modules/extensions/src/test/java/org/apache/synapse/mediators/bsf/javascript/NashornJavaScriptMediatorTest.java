/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.bsf.javascript;

import junit.framework.TestCase;
import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMText;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.bsf.ScriptMediator;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.util.LinkedHashMap;

/**
 * Test functions of Nashorn Java Script Mediator
 */
public class NashornJavaScriptMediatorTest extends TestCase {

    private static final String INLINE_SCRIPT = "var state=5;";

    public void testExternalScriptWithCommentsOnNashornEngine() throws Exception {
        String request = "{\n"
                + "    \"results\": [\n"
                + "        {\n"
                + "            \"geometry\": {\n"
                + "                \"location\": {\n"
                + "                    \"lat\": -33.86726,\n"
                + "                    \"lng\": 151.195813\n"
                + "                }\n"
                + "            },\n"
                + "            \"icon\": \"bar-71.png\",\n"
                + "            \"id\": \"7eaf7\",\n"
                + "            \"name\": \"Biaggio Cafe\",\n"
                + "            \"opening_hours\": {\n"
                + "                \"open_now\": true\n"
                + "            },\n"
                + "            \"photos\": [\n"
                + "                {\n"
                + "                    \"height\": 600,\n"
                + "                    \"html_attributions\": [],\n"
                + "                    \"photo_reference\": \"CoQBegAAAI\",\n"
                + "                    \"width\": 900\n"
                + "                }\n"
                + "            ],\n"
                + "            \"price_level\": 1,\n"
                + "            \"reference\": \"CnRqAAAAtz\",\n"
                + "            \"types\": [\n"
                + "                \"bar\",\n"
                + "                \"restaurant\",\n"
                + "                \"food\",\n"
                + "                \"establishment\"\n"
                + "            ],\n"
                + "            \"vicinity\": \"48 Pirrama Road, Pyrmont\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"geometry\": {\n"
                + "                \"location\": {\n"
                + "                    \"lat\": -33.866804,\n"
                + "                    \"lng\": 151.195579\n"
                + "                }\n"
                + "            },\n"
                + "            \"icon\": \"generic_business-71.png\",\n"
                + "            \"id\": \"3ef98\",\n"
                + "            \"name\": \"Doltone House\",\n"
                + "            \"photos\": [\n"
                + "                {\n"
                + "                    \"height\": 600,\n"
                + "                    \"html_attributions\": [],\n"
                + "                    \"photo_reference\": \"CqQBmgAAAL\",\n"
                + "                    \"width\": 900\n"
                + "                }\n"
                + "            ],\n"
                + "            \"reference\": \"CnRrAAAAV\",\n"
                + "            \"types\": [\n"
                + "                \"food\",\n"
                + "                \"establishment\"\n"
                + "            ],\n"
                + "            \"vicinity\": \"48 Pirrama Road, Pyrmont\"\n"
                + "        }\n"
                + "    ],\n"
                + "    \"status\": \"OK\"\n"
                + "}";
        MessageContext mc = TestUtils.getTestContextJson(request, null);
        String scriptSrc = "function transform(mc) {\n"
                + "    payload = mc.getPayloadJSON();\n"
                + "    results = payload.results;\n"
                + "    var response = new Array();\n"
                + "    for (i = 0; i < results.length; ++i) {\n"
                + "        // this is a comment\n"
                + "        location_object = results[i];\n"
                + "        l = new Object();\n"
                + "        l.name = location_object.name;\n"
                + "        l.tags = location_object.types;\n"
                + "        l.id = \"ID:\" + (location_object.id);\n"
                + "        response[i] = l;\n"
                + "    }\n"
                + "    mc.setPayloadJSON(response);\n"
                + "}";
        String scriptSrcKey = "conf:/repository/esb/transform.js";
        Entry e = new Entry();
        DataSource dataSource = new ByteArrayDataSource(scriptSrc.getBytes());
        DataHandler dataHandler = new DataHandler(dataSource);
        OMText text = OMAbstractFactory.getOMFactory().createOMText(dataHandler, true);
        e.setKey(scriptSrcKey);
        e.setValue(text);
        mc.getConfiguration().addEntry(scriptSrcKey, e);
        Value v = new Value(scriptSrcKey);
        ScriptMediator mediator = new ScriptMediator("nashornJs", new LinkedHashMap<Value, Object>(), v, "transform",
                null);
        boolean result = mediator.mediate(mc);
        String response = JsonUtil.jsonPayloadToString(((Axis2MessageContext) mc).getAxis2MessageContext());
        String expectedResponse = "[{\"name\":\"Biaggio Cafe\",\"tags\":[\"bar\",\"restaurant\",\"food\","
                + "\"establishment\"],\"id\":\"ID:7eaf7\"},{\"name\":\"Doltone House\",\"tags\":[\"food\","
                + "\"establishment\"],\"id\":\"ID:3ef98\"}]";

        assertEquals(expectedResponse, response);
        assertEquals(true, result);
    }

    public void testInlineMediatorOnNashornEngine() throws Exception {
        MessageContext mc = TestUtils.getTestContext("<foo/>", null);
        ScriptMediator mediator = new ScriptMediator("nashornJs", INLINE_SCRIPT,null);
        boolean responese = mediator.mediate(mc);
        assertTrue(responese);
    }
}
