package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.v2.ThrowError;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.jaxen.JaxenException;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;

public class ThrowErrorMediatorSerializerTest {

//    @Test
//    public void testSerializeThrowErrorMediator() throws JaxenException {
//        ThrowErrorMediatorSerializer throwErrorMediatorSerializer = new ThrowErrorMediatorSerializer();
//        ThrowError mediator = new ThrowError();
//        String type = "TRANSPORT:TIMEOUT";
//        String err = "Error occurred";
//        String exp = "${payload.err}";
//        mediator.setErrorMsg(new Value(err));
//        mediator.setType(type);
//        OMElement element = throwErrorMediatorSerializer.serializeSpecificMediator(mediator);
//        Assert.assertEquals("invalid type", type, element.getAttributeValue(new QName("type")));
//        Assert.assertEquals("invalid error message", err, element.getAttributeValue(new QName("errorMessage")));
//        mediator.setErrorMsg(new Value(new SynapseExpression(exp)));
//        element = throwErrorMediatorSerializer.serializeSpecificMediator(mediator);
//        Assert.assertEquals("invalid error message", "{" + exp + "}", element.getAttributeValue(new QName("errorMessage")));
//    }
}
