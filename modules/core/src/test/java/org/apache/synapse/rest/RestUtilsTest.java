/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.rest;

import junit.framework.Assert;
import org.apache.synapse.MessageContext;
import org.apache.synapse.api.ApiUtils;
import org.apache.synapse.config.SynapseConfiguration;

/**
 * Test whether query parameters of a url are not dropped.
 */
public class RestUtilsTest extends RESTMediationTestCase {

    public void testGetFullRequestPath() throws Exception {
        SynapseConfiguration synapseConfig = new SynapseConfiguration();
        MessageContext msgCtx = getMessageContext(synapseConfig, false,
                "http://localhos:9443/test/admin?PARAM1=1&PARAM2=2", "GET");

        String url = ApiUtils.getFullRequestPath(msgCtx);
        Assert.assertTrue(url.contains("PARAM1=1&PARAM2=2"));
    }

}