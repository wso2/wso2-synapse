/*
 * Copyright (c) 2022, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
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

package org.apache.synapse.util;

import com.damnhandy.uri.template.UriTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Utility class with URI Template helper methods.
 */
public class UriTemplateUtils {

    private static final Log log = LogFactory.getLog(UriTemplateUtils.class);

    private UriTemplateUtils() {
    }

    /**
     * Returns evaluated URI for a given URI template.
     * @param variables variable map with {uri.var}
     * @param template  URI template
     * @return          evaluated URI string
     * @throws URISyntaxException given string cannot be parsed as a URI
     */
    public static String getUriString(Map<String, Object> variables, UriTemplate template) throws URISyntaxException {

        String evaluatedUri;
        String uriTemplateString;
        // Handles special cases similar to uri-template="{uri.var.variable}"
        if (template.getTemplate().charAt(0) == '{' && template.getTemplate().charAt(1) != '+') {
            uriTemplateString = "{+" + template.getTemplate().substring(1);
            template = UriTemplate.fromTemplate(uriTemplateString);
        }
        template.set(variables);

        if (variables.isEmpty()) {
            evaluatedUri = template.getTemplate();
        } else {
            URI uri = new URI(template.expand());
            evaluatedUri = uri.toString();
            if (log.isDebugEnabled()) {
                log.debug("Expanded URL : " + evaluatedUri);
            }
        }
        return evaluatedUri;
    }
}
