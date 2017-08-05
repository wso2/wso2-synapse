/*
 *     Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *     WSO2 Inc. licenses this file to you under the Apache License,
 *     Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.synapse.transport.http.conn;

import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;

/**
 * wrapper class for HTTP requests with may or may not contain payload extending org.apache.http.message.BasicHttpEntityEnclosingRequest
 * Added to support body for HTTP DELETE requests
 */
public class BasicHttpEntityPossibleEnclosingRequest extends BasicHttpEntityEnclosingRequest {

    public BasicHttpEntityPossibleEnclosingRequest(String method, String uri) {
        super(method, uri);
    }

    public BasicHttpEntityPossibleEnclosingRequest(String method, String uri, ProtocolVersion ver) {
        super(method, uri, ver);
    }

    public BasicHttpEntityPossibleEnclosingRequest(RequestLine requestline) {
        super(requestline);
    }

}
