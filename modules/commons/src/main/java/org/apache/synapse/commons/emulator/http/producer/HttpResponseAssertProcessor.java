/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.emulator.http.producer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.emulator.http.dsl.dto.producer.OutgoingMessage;

public class HttpResponseAssertProcessor {

    private static final Log log = LogFactory.getLog(HttpResponseAssertProcessor.class);

    public void process(HttpResponseContext responseContext, OutgoingMessage outgoingMessage) {
        assertResponseContent(responseContext, outgoingMessage);
    }

    private void assertResponseContent(HttpResponseContext responseContext, OutgoingMessage outgoingMessage) {
        if (outgoingMessage.getBody().equalsIgnoreCase(responseContext.getRequestBody())) {
            log.info("Equal");
        } else {
            log.info("Wrong");
        }
    }

}
