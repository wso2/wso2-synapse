package org.apache.synapse.mediators.iterate;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.mediators.clients.AxisOperationClient;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * Tests a sequence with a iterate mediator that calls an 'anonymous'
 * sequences and named sequences in the iterate target
 */
public class IterateTargetSequenceTestCase extends MediatorTestCase {

    public IterateTargetSequenceTestCase() {
        loadConfiguration("/mediators/iterateTargetSequenceTestConfig.xml");
    }

    public void testAnonymousSequence() throws Exception {
        AxisOperationClient client = getAxisOperationClient();
        OMElement response = client.sendMultipleQuoteRequest("http://localhost:8280/services/IterateAnonymousSequenceTestProxy",
                null, "WSO2", 2);
        assertNotNull("Response is null", response);
        OMElement soapBody = response.getFirstElement();
        Iterator iterator =
                soapBody.getChildrenWithName(new QName("http://services.samples",
                        "getQuoteResponse"));
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            OMElement getQuote = (OMElement) iterator.next();
            assertTrue("Invalid response received", getQuote.toString().contains("WSO2"));
        }
        assertEquals("Child Element count mismatched", 2, count);
    }

    public void testNamedSequences() throws Exception {
        AxisOperationClient client = getAxisOperationClient();
        OMElement response = client.sendMultipleQuoteRequest("http://localhost:8280/services/IterateNamedSequenceTestProxy",
                null,"WSO2", 2);
        assertNotNull("Response is null", response);
        OMElement soapBody = response.getFirstElement();
        Iterator iterator = soapBody.getChildrenWithName(new QName("http://services.samples",
                        "getQuoteResponse"));
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            OMElement getQuote = (OMElement) iterator.next();
            assertTrue("Invalid response received", getQuote.toString().contains("WSO2"));
        }
        assertEquals("Child Element count mismatched", 2, count);
    }
}
