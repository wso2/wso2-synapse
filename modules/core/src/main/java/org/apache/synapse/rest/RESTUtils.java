/*
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

package org.apache.synapse.rest;

import org.apache.synapse.MessageContext;
import org.apache.synapse.api.ApiUtils;
import org.apache.synapse.api.dispatch.RESTDispatcher;

import java.util.List;

/**
 * @deprecated Replaced by {@link ApiUtils}
 */
@Deprecated
public class RESTUtils {

    /**
     * @deprecated  Replaced by {@link ApiUtils#trimSlashes(String)}
     */
    @Deprecated
    public static String trimSlashes(String url) {
        return ApiUtils.trimSlashes(url);
    }

    /**
     * @deprecated  Replaced by {@link ApiUtils#trimTrailingSlashes(String)}
     */
    @Deprecated
    public static String trimTrailingSlashes(String url) {
        return ApiUtils.trimTrailingSlashes(url);
    }

    /**
     * @deprecated  Replaced by {@link ApiUtils#getFullRequestPath(MessageContext)}
     */
    @Deprecated
    public static String getFullRequestPath(MessageContext synCtx) {
        return ApiUtils.getFullRequestPath(synCtx);
    }

    /**
     * Populate Message context properties for the query parameters extracted from the url
     *
     * @param synCtx MessageContext of the request
     *
     * @deprecated  Replaced by {@link ApiUtils#populateQueryParamsToMessageContext(MessageContext)}
     */
    @Deprecated
    public static void populateQueryParamsToMessageContext(MessageContext synCtx) {
        ApiUtils.populateQueryParamsToMessageContext(synCtx);
    }

    /**
     * @deprecated  Replaced by {@link ApiUtils#getSubRequestPath(MessageContext)}
     */
    @Deprecated
    public static String getSubRequestPath(MessageContext synCtx) {
        return ApiUtils.getSubRequestPath(synCtx);
    }

    /**
     * @deprecated  Replaced by {@link ApiUtils#getDispatchers()}
     */
    @Deprecated
    public static List<RESTDispatcher> getDispatchers() {
        return ApiUtils.getDispatchers();
    }

    /**
     * Identify the API by matching the context of the invoking api
     * with the path of each api in the api list.
     *
     * @param path    request path
     * @param context API context
     * @return true if the invoking api context matches with the path
     * and false if the two values don't match
     *
     * @deprecated  Replaced by {@link ApiUtils#matchApiPath(String, String)}
     */
    @Deprecated
    public static boolean matchApiPath(String path, String context) {
        return ApiUtils.matchApiPath(path, context);
    }
}
