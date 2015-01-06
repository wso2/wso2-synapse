package org.apache.synapse.mediators.builtin;

import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.ForEachMediatorFactory;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.AbstractSplitMediatorTestCase;
import org.apache.synapse.mediators.eip.SplitTestHelperMediator;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;

import java.util.Properties;

/**
 *
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

	public void testForEachXpath() throws Exception {
		testCtx.getEnvelope()
		       .getBody()
		       .addChild(createOMElement("<original>"
		                                 + "<itr>test-split-context-itr1-body</itr>"
		                                 + "<itr>test-split-context-itr2-body</itr>"
		                                 + "</original>"));
		MediatorFactory fac = new ForEachMediatorFactory();

		Mediator foreach =
		                   fac.createMediator(createOMElement("<foreach "
		                                                      + "expression=\"//original/itr\" xmlns=\"http://ws.apache.org/ns/synapse\">"
		                                                      + "<target soapAction=\"urn:iterate\" sequence=\"seqRef\"></target></foreach>"),

		                                      new Properties());

		helperMediator.clearMediatedContexts();
		foreach.mediate(testCtx);

		assertEquals(2, helperMediator.getMsgCount());

		assertEquals(helperMediator.getMediatedContext(0).getSoapAction(),
		             "urn:iterate");
		assertEquals(helperMediator.getMediatedContext(1).getSoapAction(),
		             "urn:iterate");

		assertEquals("<itr>test-split-context-itr1-body</itr>",
		             helperMediator.getMediatedContext(0).getEnvelope()
		                           .getBody().getFirstElement().toString());
		assertEquals("<itr>test-split-context-itr2-body</itr>",
		             helperMediator.getMediatedContext(1).getEnvelope()
		                           .getBody().getFirstElement().toString());

	}

	public void testTargetSequenceValidity() throws Exception {
		testCtx.getEnvelope()
		       .getBody()
		       .addChild(createOMElement("<original>"
		                                 + "<itr>test-split-context-itr1-body</itr>"
		                                 + "<itr>test-split-context-itr2-body</itr>"
		                                 + "</original>"));
		MediatorFactory fac = new ForEachMediatorFactory();

		Mediator foreachInvalid =
		                          fac.createMediator(createOMElement("<foreach "
		                                                             + "expression=\"//original/itr\" xmlns=\"http://ws.apache.org/ns/synapse\">"
		                                                             + "<target soapAction=\"urn:iterate\" sequence=\"seqRefInvalid\"></target></foreach>"),

		                                             new Properties());

		boolean successInvalid = foreachInvalid.mediate(testCtx);
		assertEquals(false, successInvalid);

		Mediator foreachValid =
		                        fac.createMediator(createOMElement("<foreach "
		                                                           + "expression=\"//original/itr\" xmlns=\"http://ws.apache.org/ns/synapse\">"
		                                                           + "<target soapAction=\"urn:iterate\" sequence=\"seqRef\"></target></foreach>"),

		                                           new Properties());

		boolean successValid = foreachValid.mediate(testCtx);
		assertEquals(true, successValid);

	}

	public void testForEachJsonpath() throws Exception {

		String jsonPayload =
		                     "{\"getquote\" : [{\"symbol\":\"IBM\"},{\"symbol\":\"WSO2\"}]}";
		JsonUtil.newJsonPayload(((Axis2MessageContext) testCtx).getAxis2MessageContext(),
		                        jsonPayload, true, true);

		MediatorFactory fac = new ForEachMediatorFactory();

		Mediator foreach =
		                   fac.createMediator(createOMElement("<foreach "
		                                                      + "expression=\"json-eval($.getquote)\" xmlns=\"http://ws.apache.org/ns/synapse\">"
		                                                      + "<target soapAction=\"urn:iterate\" sequence=\"seqRef\"></target></foreach>"),

		                                      new Properties());

		helperMediator.clearMediatedContexts();
		foreach.mediate(testCtx);

		assertEquals(2, helperMediator.getMsgCount());

		assertEquals(helperMediator.getMediatedContext(0).getSoapAction(),
		             "urn:iterate");
		assertEquals(helperMediator.getMediatedContext(1).getSoapAction(),
		             "urn:iterate");

		assertEquals("<jsonObject><symbol>IBM</symbol></jsonObject>",
		             helperMediator.getMediatedContext(0).getEnvelope()
		                           .getBody().getFirstElement().toString());
		assertEquals("<jsonObject><symbol>WSO2</symbol></jsonObject>",
		             helperMediator.getMediatedContext(1).getEnvelope()
		                           .getBody().getFirstElement().toString());

	}

}
