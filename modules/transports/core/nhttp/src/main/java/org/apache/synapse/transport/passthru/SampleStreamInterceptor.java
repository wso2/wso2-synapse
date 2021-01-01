/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.passthru;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SampleStreamInterceptor extends AbstractStreamInterceptor {

    private static final Log log = LogFactory.getLog(SampleStreamInterceptor.class);

    private Map<String, Integer> limit = new ConcurrentHashMap<>();

    private static final String SSE_DELIMITER = "\n\n";
    private static final String EVENT_COUNT_HEADER = "Event-Count";

    @Override
    public void interceptSourceRequest(ByteBuffer buffer, MessageContext ctx) {
        getEventCount(buffer, "interceptSourceRequest");
    }

    @Override
    public void interceptTargetRequest(ByteBuffer buffer, MessageContext ctx) {
        getEventCount(buffer, "interceptTargetRequest");
    }

    @Override
    public void interceptTargetResponse(ByteBuffer buffer, MessageContext ctx) {
        getEventCount(buffer, "interceptTargetResponse");
    }

    @Override
    public void interceptSourceResponse(ByteBuffer buffer, MessageContext ctx) {
        getEventCount(buffer, "interceptSourceResponse");
    }

    private int getEventCount(ByteBuffer stream, String caller) {

        int count;
        Charset charset = StandardCharsets.UTF_8; //Charset.forName("ISO-8859-1");
        String text = charset.decode(stream).toString();
        count = countMatches(text, SSE_DELIMITER);
        log.info("[ " + caller + " ]" + " Count :: " + count);
        if (count == 0) {
            log.info("Event :: " + text);
        } else if (text.contains(SSE_DELIMITER)) {
            String[] events = text.split(SSE_DELIMITER);
            for (String event : events) {
                log.info(caller + " Event : " + event);
            }
        }
        return count;
    }

    public static int countMatches(String text, String str) {
        if (isEmpty(text) || isEmpty(str)) {
            return 0;
        }
        return text.split(str, -1).length - 1;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

}
