/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.json.staxon;

import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamToken;

import java.io.IOException;
import java.io.StringReader;

final class Test {
    public void testCase3() {
        String zero = "{\"jsonArray\":{\"jsonElement\":{\"vale\":1}}}";
        String one = "{\"array\":[1]}";          // COLON   =>   START_ARRAY
        String two = "[1, 2, 3]";                // null   =>   START_ARRAY
        String three = "{\"array\":[[1]]}";      // START_ARRAY   =>   START_ARRAY
        String four = "{\"type\":\"Polygon\",\"coordinates\":[[[116.0865381,-8.608804],[116.127196,-8.608804],[116.127196,-8.554822],[116.0865381,-8.554822]]]}";
        // START_ARRAY   =>   START_ARRAY and COMMA   =>   START_ARRAY
        StringReader reader = new StringReader(four);
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new JsonScanner(reader), true);
        try {
            JsonStreamToken token;
            while ((token = source.peek()) != JsonStreamToken.NONE) {
                source.poll(token);
            }
            source.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
