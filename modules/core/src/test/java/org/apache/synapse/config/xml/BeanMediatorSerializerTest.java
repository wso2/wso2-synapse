/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.bean.BeanConstants;
import org.apache.synapse.mediators.bean.BeanMediator;
import org.apache.synapse.mediators.bean.Target;
import org.apache.synapse.util.MockBean;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;

/**
 * This is the test class for BeanMediatorSerializer class.
 */
public class BeanMediatorSerializerTest {

    /**
     * Test serializeSpecificMediator with CREATE action and assert the action.
     */
    @Test
    public void testSerializeSpecificMediator() {
        BeanMediatorSerializer beanMediatorSerializer = new BeanMediatorSerializer();
        BeanMediator mediator = new BeanMediator();
        mediator.setAction(BeanMediator.Action.CREATE);
        mediator.setVarName("loc");
        mediator.setClazz(MockBean.class);
        OMElement element = beanMediatorSerializer.serializeSpecificMediator(mediator);
        Assert.assertEquals("CREATE action is not performed", "CREATE",
                element.getAttributeValue(new QName(BeanConstants.ACTION)));
    }

    /**
     * Test serializeSpecificMediator with SET_PROPERTY action and assert the action.
     */
    @Test
    public void testSerializeSpecificMediator2() {
        BeanMediatorSerializer beanMediatorSerializer = new BeanMediatorSerializer();
        BeanMediator mediator = new BeanMediator();
        mediator.setAction(BeanMediator.Action.SET_PROPERTY);
        mediator.setVarName("loc");
        mediator.setPropertyName("testProperty");
        mediator.setValue(new Value("testValue"));
        OMElement element = beanMediatorSerializer.serializeSpecificMediator(mediator);
        Assert.assertEquals("SET_PROPERTY action is not performed", "SET_PROPERTY",
                element.getAttributeValue(new QName(BeanConstants.ACTION)));
    }

    /**
     * Test serializeSpecificMediator with GET_PROPERTY action and assert the action.
     */
    @Test
    public void testSerializeSpecificMediator3() {
        BeanMediatorSerializer beanMediatorSerializer = new BeanMediatorSerializer();

        BeanMediator mediator = new BeanMediator();
        mediator.setAction(BeanMediator.Action.GET_PROPERTY);
        mediator.setVarName("loc");
        mediator.setPropertyName("testProperty");
        Target target = new Target("attr", TestUtils.createOMElement("<target attr=\"testTarget\">"));
        mediator.setTarget(target);
        OMElement element = beanMediatorSerializer.serializeSpecificMediator(mediator);
        Assert.assertEquals("GET_PROPERTY action is not performed", "GET_PROPERTY",
                element.getAttributeValue(new QName(BeanConstants.ACTION)));
    }

    /**
     * Test serializeSpecificMediator with no action specified.
     */
    @Test(expected = SynapseException.class)
    public void testSerializeSpecificMediator4() {
        BeanMediatorSerializer beanMediatorSerializer = new BeanMediatorSerializer();
        BeanMediator mediator = new BeanMediator();
        beanMediatorSerializer.serializeSpecificMediator(mediator);
    }

}

