/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.samples.framework.clients;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class HttpResponse {

    private int status;
    private Map<String,String> headers = new HashMap<String, String>();
    private byte[] body;

    public HttpResponse(org.apache.http.HttpResponse response) throws IOException {
        this.status = response.getStatusLine().getStatusCode();
        Header[] headers = response.getAllHeaders();
        for (Header header : headers) {
            this.headers.put(header.getName(), header.getValue());
        }
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream in = entity.getContent();
            byte[] data = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int len;
            while ((len = in.read(data)) != -1) {
                out.write(data, 0, len);
            }
            this.body = out.toByteArray();
            out.close();
            in.close();
        }
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public OMElement getBodyAsXML() {
        OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(
                new ByteArrayInputStream(this.body));
        return builder.getDocumentElement();
    }

    public String getBodyAsString() {
        return new String(this.body);
    }

    public Map<String,List<String>> getBodyAsMap() {
        String body = getBodyAsString();
        Map<String,List<String>> map = new HashMap<String,List<String>>();
        for (String line : body.split("\n")) {
            int index = line.indexOf(':');
            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();
            List<String> values = map.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                map.put(key, values);
            }
            values.add(value);
        }
        return map;
    }
}
