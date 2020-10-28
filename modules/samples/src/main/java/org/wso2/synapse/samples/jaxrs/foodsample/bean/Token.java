/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.synapse.samples.jaxrs.foodsample.bean;

public class Token {

    String access_token;
    String expires_in;
    String token_type;

    public Token(String access_token, String expires_in, String token_type) {

        this.access_token = access_token;
        this.expires_in = expires_in;
        this.token_type = token_type;
    }

    public String getAccess_token() {

        return access_token;
    }

    public void setAccess_token(String access_token) {

        this.access_token = access_token;
    }

    public String getExpires_in() {

        return expires_in;
    }

    public void setExpires_in(String expires_in) {

        this.expires_in = expires_in;
    }

    public String getToken_type() {

        return token_type;
    }

    public void setToken_type(String token_type) {

        this.token_type = token_type;
    }
}
