/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.passthru.util;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.threads.NativeWorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.testkit.tests.Setup;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.TargetRequest;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class RelayUtilsTest {

    @Before
    public void setUp() {
        //required to properly load the static block of the RelayUtils class
        PassThroughTestUtils.getPassThroughConfiguration();
    }

    @Test
    public void testShouldOverwriteContentType() {
        MessageContext msgContext1 = Mockito.mock(MessageContext.class);
        TargetRequest tgtRequest1 = Mockito.mock(TargetRequest.class);

        Mockito.when(msgContext1.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED)).thenReturn(Boolean.FALSE);
        Mockito.when(msgContext1.getProperty(PassThroughConstants.NO_ENTITY_BODY)).thenReturn(Boolean.TRUE);

        boolean result = RelayUtils.shouldOverwriteContentType(msgContext1, tgtRequest1);
        Assert.assertFalse("Content type has been chosen to overwrite when builder is not invoked and " 
                + "no incoming content type is present", result);

        Mockito.when(msgContext1.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED)).thenReturn(Boolean.TRUE);
        Mockito.when(msgContext1.getProperty(PassThroughConstants.NO_ENTITY_BODY)).thenReturn(Boolean.FALSE);

        result = RelayUtils.shouldOverwriteContentType(msgContext1, tgtRequest1);
        Assert.assertTrue("Content type has not been chosen to overwrite when there's a body " 
                + "and builder is invoked", result);

        Map<String, LinkedHashSet<String>> headers = new HashMap<>();
        LinkedHashSet<String> contentTypeHeaderSet = new LinkedHashSet<>();
        contentTypeHeaderSet.add("application/json");
        headers.put("Content-Type", contentTypeHeaderSet);
        Mockito.when(tgtRequest1.getHeaders()).thenReturn(headers);

        result = RelayUtils.shouldOverwriteContentType(msgContext1, tgtRequest1);
        Assert.assertTrue("Content type has not been chosen to overwrite when there's a content type header",
                result);
    }
}

