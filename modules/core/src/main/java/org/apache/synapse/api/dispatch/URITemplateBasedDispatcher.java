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

package org.apache.synapse.api.dispatch;

import org.apache.synapse.MessageContext;
import org.apache.synapse.api.ApiUtils;
import org.apache.synapse.api.Resource;
import org.apache.synapse.rest.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class URITemplateBasedDispatcher implements RESTDispatcher {

    public Resource findResource(MessageContext synCtx, Collection<Resource> resources) {
        String url = ApiUtils.getSubRequestPath(synCtx);
        for (Resource r : resources) {
            DispatcherHelper helper = r.getDispatcherHelper();
            if (helper instanceof URITemplateHelper) {
                URITemplateHelper templateHelper = (URITemplateHelper) helper;
                Map<String, String> variables = new HashMap<String, String>();
                if (templateHelper.getUriTemplate().matches(url, variables)) {
                    for (Map.Entry<String, String> entry : variables.entrySet()) {
                        synCtx.setProperty(RESTConstants.REST_URI_VARIABLE_PREFIX + entry.getKey(),
                                entry.getValue());
                    }
                    synCtx.setProperty(RESTConstants.SELECTED_RESOURCE, r);
                    synCtx.setProperty(RESTConstants.RESOURCE_PATH, url);
                    return r;
                }
            }

        }
        return null;
    }
}
