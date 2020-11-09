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

package org.apache.synapse.endpoints.oauth;

public class OAuthConstants {

    public static final String OAUTH_PREFIX = "oauth_";

    // elements in the oauth synapse configuration
    public static final String AUTHENTICATION = "authentication";
    public static final String OAUTH = "oauth";
    public static final String CLIENT_CREDENTIALS = "clientCredentials";
    public static final String AUTHORIZATION_CODE = "authorizationCode";
    public static final String TOKEN_API_URL = "tokenUrl";
    public static final String OAUTH_CLIENT_ID = "clientId";
    public static final String OAUTH_CLIENT_SECRET = "clientSecret";
    public static final String OAUTH_REFRESH_TOKEN = "refreshToken";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String BASIC = "Basic ";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CLIENT_CRED_GRANT_TYPE = "grant_type=client_credentials";
    public static final String REFRESH_TOKEN_GRANT_TYPE = "grant_type=refresh_token";

    // elements in the oauth response
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String SCOPE = "scope";
    public static final String TOKEN_TYPE = "token_type";
    public static final String EXPIRES_IN = "expires_in";

    // parameters used to build oauth requests
    public static final String PARAM_CLIENT_ID = "&clientId=";
    public static final String PARAM_CLIENT_SECRET = "&clientSecret=";
    public static final String PARAM_REFRESH_TOKEN = "&refresh_token=";

    public static final String RETRIED_ON_OAUTH_FAILURE = "RETRIED_ON_OAUTH_FAILURE";

    public static final int HTTP_SC_UNAUTHORIZED = 401;
    public static final int HTTP_SC_INTERNAL_SERVER_ERROR = 500;

    // Timeout in minutes to invalidate the tokens in the cache
    public static final int TOKEN_CACHE_TIMEOUT = 50;

}
