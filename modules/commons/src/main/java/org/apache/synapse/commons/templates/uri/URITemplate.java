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

package org.apache.synapse.commons.templates.uri;

import org.apache.synapse.commons.templates.uri.parser.Node;
import org.apache.synapse.commons.templates.uri.parser.URITemplateParser;

import java.util.Map;

public class URITemplate {

    private Node syntaxTree;
    private boolean allowOptionalQuery;

    public URITemplate(String template) throws URITemplateException {
        if (!"/".equals(template) && template.endsWith("/")) {
            template = template.substring(0, template.length() - 1);
        }
        URITemplateParser parser = new URITemplateParser();
        syntaxTree = parser.parse(template);
        allowOptionalQuery = !syntaxTree.hasQueryTemplate();
    }

    public String expand(Map<String,String> variables) {
        return null;
    }

    public boolean matches(String uri, Map<String,String> variables) {
        /* if (uri.endsWith("/") && uri.length() > 1) {
            uri = uri.substring(0, uri.length() - 1);
        } */

        int matchLength = syntaxTree.matchAll(uri, variables);
        if(matchLength == uri.length()){
            return true;
        }

        // The following logic was put in to support ESBJAVA-4260
        // If the pattern do not contain a query segment we would accept optional query parameters

        if( allowOptionalQuery && matchLength > 0 && '?' == uri.charAt(matchLength) ){
            return true;
        }

        return false;
    }
}
