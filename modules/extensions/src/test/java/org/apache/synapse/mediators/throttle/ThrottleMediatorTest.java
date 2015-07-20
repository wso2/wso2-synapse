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
package org.apache.synapse.mediators.throttle;

import junit.framework.TestCase;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.commons.throttle.core.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * Throttle Mediator Test - This class test throttling when policy has specified as both of
 * InLine and a registry key
 */

public class ThrottleMediatorTest extends TestCase {
    private static final String REMOTE_ADDR = "REMOTE_ADDR";

    private static final String POLICY = " <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org" +
            "/ws/2004/09/policy\"\n" +
            "                xmlns:throttle=\"http://www.wso2.org/products/" +
            "wso2commons/throttle\">\n" +
            "        <throttle:ThrottleAssertion>\n" +
            "            <throttle:MaximumConcurrentAccess>10</throttle:MaximumConcurrentAccess>\n"+
            "            <wsp:All>\n" +
            "                <throttle:ID throttle:type=\"IP\">other</throttle:ID>\n" +
            "                <wsp:ExactlyOne>\n" +
            "                    <wsp:All>\n" +
            "                        <throttle:MaximumCount>8</throttle:MaximumCount>\n" +
            "                        <throttle:UnitTime>800000</throttle:UnitTime>\n" +
            "                        <throttle:ProhibitTimePeriod wsp:Optional=\"true\">10" +
            "</throttle:ProhibitTimePeriod>\n" +
            "                    </wsp:All>\n" +
            "                    <throttle:IsAllow>true</throttle:IsAllow>\n" +
            "                </wsp:ExactlyOne>\n" +
            "            </wsp:All>\n" +
            "            <wsp:All>\n" +
            "                <throttle:ID throttle:type=\"IP\">192.168.8.200-192.168.8.222" +
            "</throttle:ID>\n" +
            "                <wsp:ExactlyOne>\n" +
            "                    <wsp:All>\n" +
            "                        <throttle:MaximumCount>3</throttle:MaximumCount>\n" +
            "                        <throttle:UnitTime>800000</throttle:UnitTime>\n" +
            "                        <throttle:ProhibitTimePeriod wsp:Optional=\"true\">10000" +
            "</throttle:ProhibitTimePeriod>\n" +
            "                    </wsp:All>\n" +
            "                    <throttle:IsAllow>true</throttle:IsAllow>\n" +
            "                </wsp:ExactlyOne>\n" +
            "            </wsp:All>\n" +
            "            <wsp:All>\n" +
            "                <throttle:ID throttle:type=\"IP\">192.168.8.201</throttle:ID>\n" +
            "                <wsp:ExactlyOne>\n" +
            "                    <wsp:All>\n" +
            "                        <throttle:MaximumCount>200</throttle:MaximumCount>\n" +
            "                        <throttle:UnitTime>600000</throttle:UnitTime>\n" +
            "                        <throttle:ProhibitTimePeriod wsp:Optional=\"true\">" +
            "</throttle:ProhibitTimePeriod>\n" +
            "                    </wsp:All>\n" +
            "                    <throttle:IsAllow>true</throttle:IsAllow>\n" +
            "                </wsp:ExactlyOne>\n" +
            "            </wsp:All>\n" +
            "            <wsp:All>\n" +
            "                <throttle:ID throttle:type=\"IP\">192.168.8.198</throttle:ID>\n" +
            "                <wsp:ExactlyOne>\n" +
            "                    <wsp:All>\n" +
            "                        <throttle:MaximumCount>50</throttle:MaximumCount>\n" +
            "                        <throttle:UnitTime>500000</throttle:UnitTime>\n" +
            "                        <throttle:ProhibitTimePeriod wsp:Optional=\"true\">" +
            "</throttle:ProhibitTimePeriod>\n" +
            "                    </wsp:All>\n" +
            "                    <throttle:IsAllow>true</throttle:IsAllow>\n" +
            "                </wsp:ExactlyOne>\n" +
            "            </wsp:All>\n" +
            "        </throttle:ThrottleAssertion>\n" +
            "    </wsp:Policy>";

    private static final String NEW_POLICY = "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org" +
            "/ws/2004/09/policy\"\n" +
            "            xmlns:throttle=\"http://www.wso2.org/products/wso2commons/throttle\">\n" +
            "<throttle:MediatorThrottleAssertion>\n" +
            "    <throttle:MaximumConcurrentAccess>10</throttle:MaximumConcurrentAccess>\n" +
            "    <wsp:Policy>\n" +
            "        <throttle:ID throttle:type=\"IP\">other</throttle:ID>\n" +
            "        <wsp:Policy>\n" +
            "            <throttle:Control>\n" +
            "                <wsp:Policy>\n" +
            "                    <throttle:MaximumCount>8</throttle:MaximumCount>\n" +
            "                    <throttle:UnitTime>800000</throttle:UnitTime>\n" +
            "                    <throttle:ProhibitTimePeriod wsp:Optional=\"true\">10\n" +
            "                    </throttle:ProhibitTimePeriod>\n" +
            "                </wsp:Policy>\n" +
            "            </throttle:Control>\n" +
            "        </wsp:Policy>\n" +
            "    </wsp:Policy>\n" +
            "    <wsp:Policy>\n" +
            "        <throttle:ID throttle:type=\"IP\">192.168.8.200-192.168.8.222" +
            "</throttle:ID>\n" +
            "        <wsp:Policy>\n" +
            "            <throttle:Control>\n" +
            "                <wsp:Policy>\n" +
            "                    <throttle:MaximumCount>3</throttle:MaximumCount>\n" +
            "                    <throttle:UnitTime>800000</throttle:UnitTime>\n" +
            "                    <throttle:ProhibitTimePeriod wsp:Optional=\"true\">10000\n" +
            "                    </throttle:ProhibitTimePeriod>\n" +
            "                </wsp:Policy>\n" +
            "            </throttle:Control>\n" +
            "        </wsp:Policy>\n" +
            "    </wsp:Policy>\n" +
            "    <wsp:Policy>\n" +
            "        <throttle:ID throttle:type=\"IP\">192.168.8.201</throttle:ID>\n" +
            "        <wsp:Policy>\n" +
            "            <throttle:Control>\n" +
            "                <wsp:Policy>\n" +
            "                    <throttle:MaximumCount>200</throttle:MaximumCount>\n" +
            "                    <throttle:UnitTime>600000</throttle:UnitTime>\n" +
            "                    <throttle:ProhibitTimePeriod wsp:Optional=\"true\">" +
            "</throttle:ProhibitTimePeriod>\n" +
            "                </wsp:Policy>\n" +
            "            </throttle:Control>\n" +
            "        </wsp:Policy>\n" +
            "    </wsp:Policy>\n" +
            "    <wsp:Policy>\n" +
            "        <throttle:ID throttle:type=\"IP\">192.168.8.198</throttle:ID>\n" +
            "        <wsp:Policy>\n" +
            "            <throttle:Control>\n" +
            "                <wsp:Policy>\n" +
            "                    <throttle:MaximumCount>50</throttle:MaximumCount>\n" +
            "                    <throttle:UnitTime>500000</throttle:UnitTime>\n" +
            "                    <throttle:ProhibitTimePeriod wsp:Optional=\"true\">" +
            "</throttle:ProhibitTimePeriod>\n" +
            "                </wsp:Policy>\n" +
            "            </throttle:Control>\n" +
            "        </wsp:Policy>\n" +
            "    </wsp:Policy>\n" +
            "</throttle:MediatorThrottleAssertion>\n" +
            "</wsp:Policy>";


    private static final String CONCURRENCY_THROTTLING_POLICY =
            "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\"\n" +
                "                           xmlns:throttle=\"http://www.wso2.org/products/" +
                "wso2commons/throttle\">\n" +
                "    <throttle:MediatorThrottleAssertion>\n" +
                "        <throttle:MaximumConcurrentAccess>4</throttle:MaximumConcurrentAccess>\n" +
                "    </throttle:MediatorThrottleAssertion>\n" +
                "</wsp:Policy>";

    public ThrottleMediatorTest() {
        super(ThrottleMediatorTest.class.getName());
    }

    public static MessageContext createLightweightSynapseMessageContext(
            String payload) throws Exception {
        org.apache.axis2.context.MessageContext mc =
                new org.apache.axis2.context.MessageContext();
        SynapseConfiguration config = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment(config);
        MessageContext synMc = new Axis2MessageContext(mc, config, env);
        SOAPEnvelope envelope =
                OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc =
                OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);

        envelope.getBody().addChild(createOMElement(payload));

        synMc.setEnvelope(envelope);
        return synMc;
    }

    public static OMElement createOMElement(String xml) {
        try {
            XMLStreamReader reader = XMLInputFactory
                    .newInstance().createXMLStreamReader(new StringReader(xml));
            StAXOMBuilder builder = new StAXOMBuilder(reader);
            return builder.getDocumentElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public void testMediate() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(POLICY.getBytes());
        StAXOMBuilder builde = new StAXOMBuilder(in);
        ThrottleTestMediator throttleMediator = new ThrottleTestMediator();
        throttleMediator.setPolicyKey("throttlepolicy");
        MessageContext synCtx = createLightweightSynapseMessageContext("<empty/>");
        synCtx.setProperty(REMOTE_ADDR, "192.168.8.212");
        SynapseConfiguration synCfg = new SynapseConfiguration();
        Entry prop = new Entry();
        prop.setKey("throttlepolicy");
        prop.setType(Entry.INLINE_XML);
        prop.setValue(builde.getDocumentElement());
        synCfg.addEntry("throttlepolicy", prop);
        synCtx.setConfiguration(synCfg);
        for (int i = 0; i < 6; i++) {
            try {
                throttleMediator.mediate(synCtx);
                Thread.sleep(1000);
            } catch (Exception e) {

                if (i == 3) {
                    assertTrue(e.getMessage().lastIndexOf("IP_BASE") > 0);
                }
                if (i == 4) {
                    assertTrue(e.getMessage().lastIndexOf("IP_BASE") > 0);
                }
                if (i == 5) {
                    assertTrue(e.getMessage().lastIndexOf("IP_BASE") > 0);
                }
            }
        }

    }

    public void testMediateWithInLineXML() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(NEW_POLICY.getBytes());
        StAXOMBuilder build = new StAXOMBuilder(in);
        ThrottleTestMediator throttleMediator = new ThrottleTestMediator();
        throttleMediator.setInLinePolicy(build.getDocumentElement());
        MessageContext synCtx = createLightweightSynapseMessageContext("<empty/>");
        synCtx.setProperty(REMOTE_ADDR, "192.168.8.212");
        SynapseConfiguration synCfg = new SynapseConfiguration();
        synCtx.setConfiguration(synCfg);
        for (int i = 0; i < 6; i++) {
            try {
                throttleMediator.mediate(synCtx);
                Thread.sleep(1000);
            } catch (Exception e) {

                if (i == 3) {
                    assertTrue(e.getMessage().lastIndexOf("IP_BASE") > 0);
                }
                if (i == 4) {
                    assertTrue(e.getMessage().lastIndexOf("IP_BASE") > 0);
                }
                if (i == 5) {
                    assertTrue(e.getMessage().lastIndexOf("IP_BASE") > 0);
                }
            }
        }
    }

    public void testConcurrencyThrottlingMediationWithInLineXML() throws Exception {

        ByteArrayInputStream in =
                new ByteArrayInputStream(CONCURRENCY_THROTTLING_POLICY.getBytes());
        StAXOMBuilder build = new StAXOMBuilder(in);
        ConcurrencyThrottleTestMediator throttleMediator = new ConcurrencyThrottleTestMediator();
        throttleMediator.setInLinePolicy(build.getDocumentElement());
        throttleMediator.init();

        MessageContext[] synCtx = new MessageContext[6];

        for (int counter = 0; counter < 6; counter++) {
            synCtx[counter] = createLightweightSynapseMessageContext("<empty/>");
        }

        synCtx[0].setProperty(REMOTE_ADDR, "192.168.8.212");
        synCtx[1].setProperty(REMOTE_ADDR, "192.168.8.213");
        synCtx[2].setProperty(REMOTE_ADDR, "192.168.8.214");
        synCtx[3].setProperty(REMOTE_ADDR, "192.168.8.215");
        synCtx[4].setProperty(REMOTE_ADDR, "192.168.8.216");
        synCtx[5].setProperty(REMOTE_ADDR, "192.168.8.217");

        SynapseConfiguration synCfg = new SynapseConfiguration();

        for (int i = 0; i < 6; i++) {
            synCtx[i].setConfiguration(synCfg);
        }

        ArrayList<ConcurrentMediationFlow> mediationFlows = new ArrayList<ConcurrentMediationFlow>();
        CountDownLatch syncMainThreadLatch = new CountDownLatch(6);
        CyclicBarrier parallelFlowBarrier = new CyclicBarrier(6);

        for (int i = 0; i < 6; i++) {
            ConcurrentMediationFlow mediationFlow = new ConcurrentMediationFlow();
            mediationFlow.setThrottleMediator(throttleMediator);
            mediationFlow.setMessageContext(synCtx[i]);
            mediationFlow.setCountDownLatch(syncMainThreadLatch);
            mediationFlow.setBarrier(parallelFlowBarrier);
            mediationFlows.add(mediationFlow);
            mediationFlow.start();
        }

        syncMainThreadLatch.await();

        Exception ex = null;
        boolean isExceptionThrown = false;
        int deniedFlowCount = 0;
        for (int i = 0; i < 6; i++) {
            ConcurrentMediationFlow medFlow = mediationFlows.get(i);
            if (medFlow.getException() != null) {
                isExceptionThrown = true;
                ex = medFlow.getException();
                deniedFlowCount++;
            }
        }

        assertTrue("Concurrent Access controller failed to deny 6 concurrent " +
                "request while the allowed access level is 4", isExceptionThrown);
        assertNotNull("Concurrent Access controller failed deny 6 concurrent " +
                "request while the allowed access level is 4", ex);
        assertTrue("Concurrent Access controller failed deny 6 concurrent " +
                "request while the allowed access level is 4",
                ex.getClass() == SynapseException.class);
        assertTrue("Concurrent Access controller failed deny 6 concurrent " +
                "request while the allowed access level is 4", deniedFlowCount > 0);
        assertTrue("Concurrent Access controller failed deny 6 concurrent " +
                        "request while the allowed access level is 4",
                throttleMediator.getConcurrentAccessController().getAndDecrement() == 4);

    }


    class ConcurrentMediationFlow extends Thread {
        private ConcurrencyThrottleTestMediator throttleMediator;
        private MessageContext synCtx;
        private Exception ex = null;
        private CountDownLatch syncMainThreadLatch;
        private CyclicBarrier parallelFlowBarrier;

        @Override
        public void run() {
            try {
                parallelFlowBarrier.await();
                boolean canAccess = throttleMediator.mediate(synCtx);
                Thread.sleep(500);
                synCtx.setResponse(true);
                if (canAccess) {
                    throttleMediator.mediate(synCtx);
                }
            } catch (Exception ex) {
                if (ex.getClass() == SynapseException.class) {
                    this.ex = ex;
                }
            } finally {
                this.syncMainThreadLatch.countDown();
            }
        }

        public void setCountDownLatch(CountDownLatch latch) {
            this.syncMainThreadLatch = latch;
        }

        public void setBarrier(CyclicBarrier barrier) {
            this.parallelFlowBarrier = barrier;
        }

        public void setThrottleMediator(ConcurrencyThrottleTestMediator med) {
            this.throttleMediator = med;
        }

        public void setMessageContext(MessageContext mes) {
            this.synCtx = mes;
        }

        public Exception getException() {
            return ex;
        }

    }

    class ThrottleTestMediator extends AbstractMediator {

        private String policyKey = null;

        Throttle throttle = null;
        private OMElement inLinePolicy = null;

        public boolean mediate(MessageContext synCtx) {

            init(synCtx);
            try {
                return canAcess(synCtx);
            } catch (
                    ThrottleException e) {
                throw new SynapseException(e.getMessage());
            }

        }

        protected boolean canAcess(MessageContext synContext)
                throws SynapseException, ThrottleException {

            if (throttle == null) {
                throw new ThrottleException("Can not find a throttle");

            }
            //IP based throttling
            String remoteIP = (String) synContext.
                    getProperty(org.apache.axis2.context.MessageContext.REMOTE_ADDR);
            if (remoteIP == null) {
                throw new ThrottleException("IP address of the caller can not find - " +
                        "Currently only support caller-IP base access control" +
                        "- Thottling will not happen ");

            } else {
                ThrottleContext throttleContext
                        = throttle.getThrottleContext(ThrottleConstants.IP_BASED_THROTTLE_KEY);
                if (throttleContext == null) {
                    throw new ThrottleException("Can not find a configuartion for " +
                            "IP Based Throttle");

                }
                AccessRateController accessControler;
                try {
                    accessControler = new AccessRateController();
                    boolean canAccess = accessControler.canAccess(
                            throttleContext, remoteIP, ThrottleConstants.IP_BASE).isAccessAllowed();
                    if (!canAccess) {
                        throw new SynapseException("Access has currently been denied by" +
                                " the IP_BASE throttle for IP :\t" + remoteIP);
                    }
                    return canAccess;
                } catch (ThrottleException e) {
                    return true;
                }
            }
        }

        protected void init(MessageContext synContext) {

            boolean reCreate = false; // It is not need to recreate ,if property is not dyanamic
            OMElement policyOmElement = null;

            if (policyKey != null) {
                Entry entry = synContext.getConfiguration().getEntryDefinition(policyKey);
                if (entry == null) {
                    return;
                }
                Object entryValue = entry.getValue();

                if (!(entryValue instanceof OMElement)) {
                    return;
                }
                // if entry is dynamic, need to check wheather updated or not
                if ((!entry.isCached() || entry.isExpired())) {
                    reCreate = true;
                }
                policyOmElement = (OMElement) entryValue;
            } else if (inLinePolicy != null) {
                policyOmElement = inLinePolicy;
            }
            if (policyOmElement == null) {
                return;
            }
            if (!reCreate) {
                //The first time creation
                if (throttle == null) {
                    createThrottleMetaData(policyOmElement);
                }
            } else {
                createThrottleMetaData(policyOmElement);
            }

        }

        protected void createThrottleMetaData(OMElement policyOmElement) {

            try {
                throttle = ThrottleFactory.createMediatorThrottle(
                        PolicyEngine.getPolicy(policyOmElement));
            } catch (ThrottleException e) {

                throw new SynapseException(e.getMessage());
            }
        }

        public String getType() {
            return "ThrottleMediator";
        }


        public String getPolicyKey() {
            return policyKey;
        }

        public void setPolicyKey(String policyKey) {
            this.policyKey = policyKey;
        }

        public OMElement getInLinePolicy() {
            return inLinePolicy;
        }

        public void setInLinePolicy(OMElement inLinePolicy) {
            this.inLinePolicy = inLinePolicy;
        }

    }

    class ConcurrencyThrottleTestMediator extends AbstractMediator {

        private String policyKey = null;
        private ConcurrentAccessController concurrentAccessController = null;
        private Throttle throttle = null;
        private OMElement inLinePolicy = null;
        private Object throttleLock = new Object();

        public boolean mediate(MessageContext synCtx) {
            try {
                return canAccess(synCtx);
            } catch (ThrottleException e) {
                throw new SynapseException(e.getMessage());
            }
        }

        protected boolean canAccess(MessageContext synContext)
                throws SynapseException, ThrottleException {

            if (throttle == null) {
                throw new ThrottleException("Can not find a throttle");

            }

            synchronized (throttleLock) {
                if (concurrentAccessController == null && throttle != null) {
                    concurrentAccessController = throttle.getConcurrentAccessController();
                }
            }

            boolean canAccess = true;
            if (!synContext.isResponse()) {
                int available = concurrentAccessController.getAndDecrement();
                canAccess = available > 0;
            } else {
                int available = concurrentAccessController.incrementAndGet();
            }

            String remoteIP = (String) synContext.
                    getProperty(org.apache.axis2.context.MessageContext.REMOTE_ADDR);

            if (!canAccess) {
                throw new SynapseException("Concurrent Access has currently been denied by" +
                        " the Concurrent Access Controller for IP :\t" + remoteIP);
            }
            return canAccess;
        }

        public void init() {
            if (inLinePolicy != null) {
                createThrottleMetaData(inLinePolicy);
            }
        }

        protected void createThrottleMetaData(OMElement policyOmElement) {
            try {
                throttle = ThrottleFactory.createMediatorThrottle(
                        PolicyEngine.getPolicy(policyOmElement));
            } catch (ThrottleException e) {
                throw new SynapseException(e.getMessage());
            }
        }

        public String getType() {
            return "ThrottleMediator";
        }

        public String getPolicyKey() {
            return policyKey;
        }

        public void setPolicyKey(String policyKey) {
            this.policyKey = policyKey;
        }

        public OMElement getInLinePolicy() {
            return inLinePolicy;
        }

        public void setInLinePolicy(OMElement inLinePolicy) {
            this.inLinePolicy = inLinePolicy;
        }

        public ConcurrentAccessController getConcurrentAccessController() {
            return this.concurrentAccessController;
        }
    }
}
