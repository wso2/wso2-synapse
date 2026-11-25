/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.template;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.Constants;
import org.apache.axis2.format.PlainTextBuilder;
import org.apache.axis2.format.PlainTextFormatter;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.transport.passthru.PassThroughConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * An invoke with a response variable and overwriteBody false keeps the body before the template and puts it back after
 * it. A template that does not touch the body, as a connector operation that does not read it, leaves the same element
 * in the body, and that element must stay readable.
 */
public class InvokeMediatorKeptBodyTest extends TestCase {

    private static final String TEMPLATE = "keptBodyTemplate";
    private static final String VARIABLE = "result";
    private static final String TEXT = "line one\nline two with \u00e9\n";
    private static final String XML = "<order><id>7</id><item>tea</item></order>";
    private static final String JSON = "{\"order\":{\"id\":7,\"items\":[\"tea\",true]}}";

    /**
     * A text body from the plain text builder that nobody has read comes back readable, and the plain text formatter
     * writes it, as a respond after the invoke does.
     */
    public void testAnUnreadTextBodyStaysReadable() throws Exception {
        MessageContext synCtx = message(false);
        org.apache.axis2.context.MessageContext axis2 = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        OMElement text = new PlainTextBuilder().processDocument(
                new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8)), "text/plain; charset=UTF-8", axis2);
        replaceBody(synCtx, text);
        axis2.setProperty(Constants.Configuration.MESSAGE_TYPE, "text/plain");
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

        assertTrue(invoke().mediate(synCtx));

        assertSame(text, synCtx.getEnvelope().getBody().getFirstElement());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OMOutputFormat format = new OMOutputFormat();
        format.setCharSetEncoding("UTF-8");
        new PlainTextFormatter().writeTo(axis2, format, out, true);
        assertEquals(TEXT, new String(out.toByteArray(), StandardCharsets.UTF_8));
        assertEquals(TEXT, synCtx.getEnvelope().getBody().getFirstElement().getText());
    }

    public void testAnXmlBodyIsTheSameAfterTheInvoke() throws Exception {
        MessageContext synCtx = message(false);
        replaceBody(synCtx, AXIOMUtil.stringToOM(XML));
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

        assertTrue(invoke().mediate(synCtx));

        assertEquals(XML, synCtx.getEnvelope().getBody().getFirstElement().toString());
    }

    public void testAJsonPayloadIsTheSameAfterTheInvoke() throws Exception {
        MessageContext synCtx = message(false);
        org.apache.axis2.context.MessageContext axis2 = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        JsonUtil.getNewJsonPayload(axis2, JSON, true, true);
        axis2.setProperty(Constants.Configuration.MESSAGE_TYPE, "application/json");
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

        assertTrue(invoke().mediate(synCtx));

        assertEquals(JSON, JsonUtil.jsonPayloadToString(axis2));
    }

    /**
     * A template that replaces the body gets the kept body put back after it, as before.
     */
    public void testTheKeptBodyComesBackAfterATemplateThatReplacesTheBody() throws Exception {
        MessageContext synCtx = message(true);
        replaceBody(synCtx, AXIOMUtil.stringToOM(XML));
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

        assertTrue(invoke().mediate(synCtx));

        assertEquals(XML, synCtx.getEnvelope().getBody().getFirstElement().toString());
    }

    // A message whose configuration holds the template. The template sets the response variable, as a connector
    // operation does, and with replacing it puts another element in place of the body.
    private static MessageContext message(boolean replacing) throws Exception {
        SynapseConfiguration config = new SynapseConfiguration();
        TemplateMediator template = new TemplateMediator();
        template.setName(TEMPLATE);
        template.setParameters(Arrays.asList(new TemplateParam(SynapseConstants.RESPONSE_VARIABLE, false, null),
                new TemplateParam(SynapseConstants.OVERWRITE_BODY, false, null)));
        template.addChild(new AbstractMediator() {
            @Override
            public boolean mediate(MessageContext synCtx) {
                synCtx.setVariable(VARIABLE, new DefaultConnectorResponse());
                if (replacing) {
                    try {
                        replaceBody(synCtx, AXIOMUtil.stringToOM("<replaced/>"));
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
                return true;
            }
        });
        config.addSequenceTemplate(TEMPLATE, template);
        return TestUtils.createSynapseMessageContext("<empty/>", config);
    }

    private static InvokeMediator invoke() {
        InvokeMediator invoke = new InvokeMediator();
        invoke.setTargetTemplate(TEMPLATE);
        invoke.addInvokeParam(SynapseConstants.RESPONSE_VARIABLE, new InvokeParam(new Value(VARIABLE)));
        invoke.addInvokeParam(SynapseConstants.OVERWRITE_BODY, new InvokeParam(new Value("false")));
        return invoke;
    }

    private static void replaceBody(MessageContext synCtx, OMElement element) {
        SOAPBody body = synCtx.getEnvelope().getBody();
        OMElement first = body.getFirstElement();
        if (first != null) {
            first.detach();
        }
        body.addChild(element);
    }
}
