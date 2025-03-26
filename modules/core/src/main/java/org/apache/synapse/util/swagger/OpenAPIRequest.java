/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.util.swagger;

import com.atlassian.oai.validator.model.Request;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.RESTConstants;
import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Request Model class for OpenAPI
 */
public class OpenAPIRequest implements Request {

    private static final Log logger = LogFactory.getLog(OpenAPIRequest.class);
    private Request.Method method;
    private String path;
    private Multimap<String, String> headers = ArrayListMultimap.create();
    private Map<String, Collection<String>> queryParams;
    private Optional<String> requestBody;
    /**
     * Build OAI Request from Message Context.
     *
     * @param synCtx Synapse message context.
     */
    public OpenAPIRequest(MessageContext synCtx, OpenAPI openAPI) {

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext)
                synCtx).getAxis2MessageContext();
        //set HTTP Method
        method = Request.Method.valueOf((String)
                synCtx.getProperty(RESTConstants.REST_METHOD));

        //extract transport headers
        Map<String, String> transportHeaders = (Map<String, String>)
                (axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
        //Set Request body
        requestBody = SchemaValidationUtils.getMessageContent(synCtx);
        Map<String, Collection<String>> headerMap = transportHeaders.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singleton(entry.getValue())));
        //Set transport headers
        String contentTypeHeader = "content-type";
        for (Map.Entry<String, Collection<String>> header : headerMap.entrySet()) {
            String headerKey = header.getKey();
            String value =  header.getValue().iterator().next();
            headerKey = headerKey.equalsIgnoreCase(contentTypeHeader) ?
                    RESTConstants.CONTENT_TYPE : headerKey.toLowerCase(Locale.ROOT);
            headers.put(headerKey, value);
        }
        //Set Request path
        String resourcePath = synCtx.getProperty(RESTConstants.RESOURCE_PATH).toString();
        if (resourcePath.contains("?")) {
            path = resourcePath.split("\\?")[0];
            try {
                queryParams = SchemaValidationUtils.getQueryParams(resourcePath);
            } catch (UnsupportedEncodingException e) {
                logger.warn("Error while extracting query params from resource path " + resourcePath, e);
            }
        } else {
            path = resourcePath;
        }
        synCtx.setProperty(RESTConstants.RESOURCE_PATH_WITHOUT_QUERY_PARAMS, path);
        validatePath(openAPI);
    }

    @Nonnull
    @Override
    public String getPath() {

        return this.path;
    }

    @Nonnull
    @Override
    public Method getMethod() {

        return this.method;
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {

        return this.requestBody;
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameters() {

        if (this.queryParams == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(this.queryParams.keySet());
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameterValues(String s) {

        if (this.queryParams == null) {
            return Collections.emptyList();
        }
        return SchemaValidationUtils.getFromMapOrEmptyList(this.queryParams, s);
    }

    @Nonnull
    @Override
    public Map<String, Collection<String>> getHeaders() {

        if (this.headers == null) {
            return Collections.emptyMap();
        }
        return headers.asMap();
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(String s) {

        if (this.headers == null) {
            return Collections.emptyList();
        }
        return SchemaValidationUtils.getFromMapOrEmptyList(this.headers.asMap(), s);
    }

    protected void validatePath(OpenAPI openAPI) {

        Paths paths = openAPI.getPaths();
        if (path.equals("/") && !paths.containsKey(path)) {
            if (paths.containsKey("/*")) {
                path = "/*";
            }
        }
    }
}
