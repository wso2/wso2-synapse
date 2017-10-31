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

package org.apache.synapse.metrics;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.mockito.Mockito;

/**
 * Unit tests for GlobalRequestHandlerTest class
 */
public class GlobalRequestCountHandlerTest extends TestCase {

    /**
     * Invoking invoke method and assert for the incremented counter value
     * @throws Exception
     */
    public void testInvoke() throws Exception {
        GlobalRequestCountHandler globalRequestCountHandler = new GlobalRequestCountHandler();
        MessageContext messageContext;
        Counter counter = new Counter();
        Parameter parameter = new Parameter();
        parameter.setValue(counter);
        messageContext = Mockito.mock(MessageContext.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(parameter).when(messageContext).getParameter(Mockito.anyString());
        Handler.InvocationResponse response = globalRequestCountHandler.invoke(messageContext);
        Assert.assertEquals("Asserting the response of the method",response, Handler.InvocationResponse.CONTINUE);
        Assert.assertEquals("Counter should be incremented to 1", counter.getCount(), 1);
    }
}
