/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.samples.framework.clients;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */
public class StockQuoteCallback implements AxisCallback {

    private static final Log log = LogFactory.getLog(StockQuoteCallback.class);

    StockQuoteSampleClient client;

    public StockQuoteCallback(StockQuoteSampleClient client) {
        this.client=client;
    }

    public void onMessage(org.apache.axis2.context.MessageContext messageContext) {
        log.info("Response received to the callback");
        OMElement result
                = messageContext.getEnvelope().getBody().getFirstElement();
        // Detach the result to make sure that the element we return to the sample client
        // is completely built
        result.detach();
        client.setResponse(result);
    }

    public void onFault(org.apache.axis2.context.MessageContext messageContext) {
        log.warn("Fault received to the callback : " + messageContext.getEnvelope().
                getBody().getFault());
    }

    public void onError(Exception e) {
        log.warn("Error inside callback : " + e);
    }

    public void onComplete() {
        client.setCompleted(true);
    }
}
