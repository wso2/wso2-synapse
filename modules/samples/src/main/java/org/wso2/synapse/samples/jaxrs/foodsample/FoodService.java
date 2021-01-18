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

package org.wso2.synapse.samples.jaxrs.foodsample;

import org.apache.commons.codec.binary.Base64;
import org.wso2.synapse.samples.jaxrs.foodsample.bean.Token;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@javax.inject.Singleton
@Path("/foodservice/")
public class FoodService {

    private int unauthorizedReqCount = 0;
    private int tokenReqCount = 0;

    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessToken(@Context HttpHeaders httpHeaders,
                                   MultivaluedMap<String, String> tokenRequestParams) {

        tokenReqCount++;

        String basicHeader = httpHeaders.getHeaderString("Authorization");

        if (validateCredentials(basicHeader, tokenRequestParams)) {
            return Response.status(Response.Status.OK).entity(new Token(Constants.accessToken, Constants.expiresIn,
                    Constants.tokenType)).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid Credentials").build();
    }

    @GET
    @Path("/food")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFoodItem(@Context HttpHeaders httpHeaders) {

        String authorizationHeader = httpHeaders.getHeaderString("Authorization");
        if (authorizationHeader != null) {
            String token = authorizationHeader.split(" ")[1];
            if (token.equals(Constants.accessToken)) {
                return Response.status(Response.Status.OK).entity(tokenReqCount).build();
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    @GET
    @Path("/unauthorized")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnauthorizedItem(@Context HttpHeaders httpHeaders) {

        unauthorizedReqCount++;
        return Response.status(Response.Status.UNAUTHORIZED).entity(unauthorizedReqCount).build();
    }

    private boolean validateCredentials(String basicHeader, MultivaluedMap<String, String> tokenRequestParams) {

        String credentials = basicHeader.substring(6).trim();

        String decodedCredentials = new String(new Base64().decode(credentials.getBytes()));
        String clientId = decodedCredentials.split(":")[0];
        String clientSecret = decodedCredentials.split(":")[1];

        String refreshToken = tokenRequestParams.getFirst("refresh_token");
        String clientIdInBody = tokenRequestParams.getFirst("clientId");
        String clientSecretInBody = tokenRequestParams.getFirst("clientSecret");

        if (refreshToken != null && !refreshToken.equals(Constants.refreshToken)) {
            return false;
        }

        if (!clientId.equals(Constants.clientId) || !clientIdInBody.equals(Constants.clientId) ||
                !clientSecret.equals(Constants.clientSecret) || !clientSecretInBody.equals(Constants.clientSecret)) {
            return false;
        }
        return true;
    }
}
