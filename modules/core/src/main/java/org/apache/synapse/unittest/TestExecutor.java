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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.unittest;

import javafx.util.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;

/**
 * Class responsible for building message context and mediating the message context through the deployed sequence and asserting the mediation result
 */
public class TestExecutor {

    private static Logger log = Logger.getLogger(TestExecutor.class.getName());

    /**
     * Create message context from the inputXmlPayload
     */
    public MessageContext createMessageContext(String inputXmlPayload) {

        MessageContext msgCtxt = null;

        try {
            msgCtxt = TestUtils.createLightweightSynapseMessageContext(inputXmlPayload);

        } catch (Exception e) {
            log.error("Exception in creating message context", e);
        }
        return msgCtxt;
    }

    /**
     * Mediate the message through the given sequence
     */
    public Pair<Boolean, MessageContext> sequenceMediate(String inputXmlPayload, SynapseConfiguration synconfig, String key) {

        Mediator sequenceMediator = synconfig.getSequence(key);
        MessageContext msgCtxt = createMessageContext(inputXmlPayload);
        boolean mediationResult = sequenceMediator.mediate(msgCtxt);

        return new Pair<>(mediationResult, msgCtxt);
    }

    /**
     * Mediate the message through the proxy service
     * @param key
     * @param xmlFragment
     * @return
     */

    public String invokeProxyService(String key, String xmlFragment) {

        try {

            StringBuilder stringBuilder;
            stringBuilder = new StringBuilder("http://10.100.4.213:8280/services/");
            String url = stringBuilder.append(key).toString();
            HttpClient client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(HTTP.CONTENT_TYPE, "application/xml");
            httpPost.setHeader(HTTP.CONTENT_TYPE, "text/xml");
            httpPost.setHeader("Action", "urn-mediate");
            StringEntity se = new StringEntity(xmlFragment);
            HttpResponse response = client.execute(httpPost);
            return response.getEntity().toString();
            }

            catch (Exception e) {
                log.error("Exception in invoking the proxy service", e);
                return null;
            }
        }

    /**
     * Asserting the payload and property values
     */
    public String doAssertions(String expectedPayload, String expectedPropVal, MessageContext msgCtxt) {

        boolean result1 = (expectedPropVal.equals(msgCtxt.getEnvelope().toString()));
        log.info(result1);

        boolean result2 = (expectedPayload.equals(msgCtxt.getEnvelope().getBody().toString()));
        log.info(result2);

        if (result1 && result2) {
            return "Unit Testing is Successful";
        } else {
            return "Unit testing failed";
        }

    }

    public String assertProperties(String expectedPropVal, String propertySet) {

        if (propertySet.equals(expectedPropVal)) {
            return "Unit Testing is Successful";
        } else {
            return "Unit testing failed";
        }
    }
}
