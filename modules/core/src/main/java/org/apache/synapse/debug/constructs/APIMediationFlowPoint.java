/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.debug.constructs;

/**
 * Defines a unique point in the mediation route that mediate through a API Sequence
 */
public class APIMediationFlowPoint extends SequenceMediationFlowPoint {
    /*api resource mapping eg :-  url-mapping, uri-template*/
    private String resourceMapping;
    /*api resource associated http method eg :- GET, POST*/
    private String resourceHTTPMethod;

    public String getResourceMapping() {
        return resourceMapping;
    }

    public void setResourceMapping(String resourceMapping) {
        this.resourceMapping = resourceMapping;
    }

    public String getResourceHTTPMethod() {
        return resourceHTTPMethod;
    }

    public void setResourceHTTPMethod(String resourceHTTPMethod) {
        this.resourceHTTPMethod = resourceHTTPMethod;
    }

}
