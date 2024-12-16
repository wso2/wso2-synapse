package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.v2.TriggerError;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.jaxen.JaxenException;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;

public class TriggerErrorMediatorSerializerTest {

    @Test
    public void testSerializeTriggerErrorMediator() throws JaxenException {
        TriggerErrorMediatorSerializer triggerErrorMediatorSerializer = new TriggerErrorMediatorSerializer();
        TriggerError mediator = new TriggerError();
        String type = "TRANSPORT:TIMEOUT";
        String err  = "Error occurred";
        String exp = "${payload.err}";
        mediator.setExpression(new SynapseExpression(exp));
        mediator.setType(type);
        OMElement element = triggerErrorMediatorSerializer.serializeSpecificMediator(mediator);
        Assert.assertEquals("invalid type", type, element.getAttributeValue(new QName("type")));
        Assert.assertEquals("invalid expression", exp, element.getAttributeValue(new QName("expression")));
        mediator.setExpression(null);
        mediator.setErrorMsg(err);
        element = triggerErrorMediatorSerializer.serializeSpecificMediator(mediator);
        Assert.assertEquals("invalid error message", err, element.getAttributeValue(new QName("errorMessage")));
    }
}
