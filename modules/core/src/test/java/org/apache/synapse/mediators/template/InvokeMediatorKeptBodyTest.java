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
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.Constants;
import org.apache.axis2.format.BinaryBuilder;
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
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.activation.DataHandler;
import javax.xml.stream.XMLStreamException;

/**
 * An invoke with a response variable and overwriteBody false keeps the body before the template and puts it back after
 * it. A template that does not touch the body, as a connector operation that does not read it, leaves the same element
 * in the body. A sourced element that nobody has read stays in place and must stay readable. Every other body is put
 * back as before, so a plain body is built inside the invoke and a body that is not well formed fails there.
 */
public class InvokeMediatorKeptBodyTest extends TestCase {

    private static final String TEMPLATE = "keptBodyTemplate";
    private static final String VARIABLE = "result";
    private static final String TEXT = "line one\nline two with \u00e9\n";
    private static final String XML = "<order><id>7</id><item>tea</item></order>";
    private static final String JSON = "{\"order\":{\"id\":7,\"items\":[\"tea\",true]}}";
    private static final String BROKEN = "<order><id>7</id><item>tea</it";
    // The JSON stream of a message. The name Constants is taken by the axis2 constants in this class.
    private static final String JSON_STREAM =
            org.apache.synapse.commons.json.Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM;

    /**
     * A text body from the plain text builder that nobody has read comes back readable, and the plain text formatter
     * writes it, as a respond after the invoke does.
     */
    public void testAnUnreadTextBodyStaysReadable() throws Exception {
        MessageContext synCtx = message(false, false, false);
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

    /**
     * A text body that was read before the invoke is put back as before and is still written whole.
     */
    public void testAReadTextBodyIsPutBackAndStaysRight() throws Exception {
        MessageContext synCtx = message(false, false, false);
        org.apache.axis2.context.MessageContext axis2 = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        OMElement text = new PlainTextBuilder().processDocument(
                new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8)), "text/plain; charset=UTF-8", axis2);
        replaceBody(synCtx, text);
        axis2.setProperty(Constants.Configuration.MESSAGE_TYPE, "text/plain");
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        assertEquals(TEXT, text.getText());
        assertTrue(((OMSourcedElement) text).isExpanded());

        assertTrue(invoke().mediate(synCtx));

        assertSame(text, synCtx.getEnvelope().getBody().getFirstElement());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OMOutputFormat format = new OMOutputFormat();
        format.setCharSetEncoding("UTF-8");
        new PlainTextFormatter().writeTo(axis2, format, out, true);
        assertEquals(TEXT, new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    /**
     * A body that a parser still reads and that is not well formed fails inside the invoke, because the put back
     * builds it, so the step fails and not the response after it.
     */
    public void testABodyThatIsNotWellFormedFailsInsideTheInvoke() throws Exception {
        MessageContext synCtx = message(false, false, false);
        replaceBody(synCtx, parsedElement(BROKEN));
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        InvokeMediator invoke = invoke();

        try {
            invoke.mediate(synCtx);
            fail("A body that is not well formed must fail inside the invoke.");
        } catch (RuntimeException expected) {
            Throwable cause = expected;
            while (cause != null && !(cause instanceof XMLStreamException)) {
                cause = cause.getCause();
            }
            assertNotNull("The failure must come from the parse of the body: " + expected, cause);
        }
    }

    /**
     * A well formed body that a parser still reads is put back as before: the same element, built by the put back.
     */
    public void testAWellFormedParsedBodyIsPutBackAndBuilt() throws Exception {
        MessageContext synCtx = message(false, false, false);
        OMElement order = parsedElement(XML);
        replaceBody(synCtx, order);
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        assertFalse(order.isComplete());

        assertTrue(invoke().mediate(synCtx));

        assertSame(order, synCtx.getEnvelope().getBody().getFirstElement());
        assertTrue(order.isComplete());
        assertEquals(XML, order.toString());
    }

    /**
     * A parsed body with a processing instruction is written after the invoke as the put back left it, built.
     */
    public void testABodyWithAProcessingInstructionIsWrittenAsBefore() throws Exception {
        String document = "<doc><?keep this?><v>1</v></doc>";
        OMElement built = parsedElement(document);
        built.build();
        StringWriter expected = new StringWriter();
        built.serializeAndConsume(expected);
        MessageContext synCtx = message(false, false, false);
        replaceBody(synCtx, parsedElement(document));
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

        assertTrue(invoke().mediate(synCtx));

        StringWriter written = new StringWriter();
        synCtx.getEnvelope().getBody().getFirstElement().serializeAndConsume(written);
        assertEquals(expected.toString(), written.toString());
    }

    /**
     * A binary body is put back as before and keeps its bytes.
     */
    public void testABinaryBodyIsPutBackAsBefore() throws Exception {
        MessageContext synCtx = message(false, false, false);
        org.apache.axis2.context.MessageContext axis2 = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        byte[] bytes = new byte[4096];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 31);
        }
        OMElement binary = new BinaryBuilder().processDocument(new ByteArrayInputStream(bytes),
                "application/octet-stream", axis2);
        replaceBody(synCtx, binary);
        axis2.setProperty(Constants.Configuration.MESSAGE_TYPE, "application/octet-stream");
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

        assertTrue(invoke().mediate(synCtx));

        OMElement first = synCtx.getEnvelope().getBody().getFirstElement();
        assertSame(binary, first);
        DataHandler data = (DataHandler) ((OMText) first.getFirstOMChild()).getDataHandler();
        ByteArrayOutputStream read = new ByteArrayOutputStream();
        try (InputStream in = data.getInputStream()) {
            byte[] buffer = new byte[1024];
            for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
                read.write(buffer, 0, n);
            }
        }
        assertTrue(Arrays.equals(bytes, read.toByteArray()));
    }

    public void testAnXmlBodyIsTheSameAfterTheInvoke() throws Exception {
        MessageContext synCtx = message(false, false, false);
        replaceBody(synCtx, AXIOMUtil.stringToOM(XML));
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

        assertTrue(invoke().mediate(synCtx));

        assertEquals(XML, synCtx.getEnvelope().getBody().getFirstElement().toString());
    }

    public void testAJsonPayloadIsTheSameAfterTheInvoke() throws Exception {
        MessageContext synCtx = message(false, false, false);
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
        MessageContext synCtx = message(true, false, false);
        replaceBody(synCtx, AXIOMUtil.stringToOM(XML));
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

        assertTrue(invoke().mediate(synCtx));

        assertEquals(XML, synCtx.getEnvelope().getBody().getFirstElement().toString());
    }

    /**
     * A template that leaves an unread text body in place and marks the message as one with no body gets the mark
     * removed after the invoke, as the put back of the kept body removes it.
     */
    public void testTheMarkOfNoBodyIsRemovedWhenTheBodyStaysInPlace() throws Exception {
        MessageContext synCtx = message(false, true, false);
        org.apache.axis2.context.MessageContext axis2 = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        OMElement text = unreadText(synCtx);

        assertTrue(invoke().mediate(synCtx));

        assertSame(text, synCtx.getEnvelope().getBody().getFirstElement());
        assertNull(axis2.getProperty(PassThroughConstants.NO_ENTITY_BODY));
        assertEquals(TEXT, text.getText());
    }

    /**
     * A template that leaves an unread text body in place and leaves a JSON stream on the message gets the stream
     * removed after the invoke, as the put back of the kept body removes it, and the body stays readable.
     */
    public void testTheJsonStreamIsRemovedWhenTheBodyStaysInPlace() throws Exception {
        MessageContext synCtx = message(false, false, true);
        org.apache.axis2.context.MessageContext axis2 = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        OMElement text = unreadText(synCtx);

        assertTrue(invoke().mediate(synCtx));

        assertSame(text, synCtx.getEnvelope().getBody().getFirstElement());
        assertNull(axis2.getProperty(JSON_STREAM));
        assertEquals(TEXT, text.getText());
    }

    // A message whose configuration holds the template. The template sets the response variable, as a connector
    // operation does. With replacing it puts another element in place of the body, with marking it marks the
    // message as one with no body, and with streaming it leaves a JSON stream on the message.
    private static MessageContext message(boolean replacing, boolean marking, boolean streaming) throws Exception {
        SynapseConfiguration config = new SynapseConfiguration();
        TemplateMediator template = new TemplateMediator();
        template.setName(TEMPLATE);
        template.setParameters(Arrays.asList(new TemplateParam(SynapseConstants.RESPONSE_VARIABLE, false, null),
                new TemplateParam(SynapseConstants.OVERWRITE_BODY, false, null)));
        template.addChild(new AbstractMediator() {
            @Override
            public boolean mediate(MessageContext synCtx) {
                synCtx.setVariable(VARIABLE, new DefaultConnectorResponse());
                if (marking) {
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                            .setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
                }
                if (streaming) {
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(
                            JSON_STREAM,
                            new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
                }
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

    // A text body from the plain text builder that nobody has read, in the body of the message.
    private static OMElement unreadText(MessageContext synCtx) throws Exception {
        org.apache.axis2.context.MessageContext axis2 = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        OMElement text = new PlainTextBuilder().processDocument(
                new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8)), "text/plain; charset=UTF-8", axis2);
        replaceBody(synCtx, text);
        axis2.setProperty(Constants.Configuration.MESSAGE_TYPE, "text/plain");
        synCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        return text;
    }

    // The root of a document that a parser reads as far as it is asked, out of its document with nothing built, as the
    // application/xml builder puts it into the body.
    private static OMElement parsedElement(String xml) throws XMLStreamException {
        return new StAXOMBuilder(StAXUtils.createXMLStreamReader(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))).getDocumentElement(true);
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
