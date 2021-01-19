/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.api.inbound;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.API;
import org.apache.synapse.api.ApiConstants;
import org.apache.synapse.api.Resource;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Set;

public class InboundApiUtils {

    private static final Log log = LogFactory.getLog(InboundApiUtils.class);

    private InboundApiUtils() {
        // Prevents Instantiation
    }

    public static void addBindsTo(API api, OMElement omElement) {
        api.addAllBindsTo(extractBindsTo(omElement));
    }

    public static void addBindsTo(Resource resource, OMElement omElement) {
        resource.addAllBindsTo(extractBindsTo(omElement));
    }

    private static Set<String> extractBindsTo(OMElement omElement) {
        OMAttribute bindsToAttribute = omElement.getAttribute(new QName(ApiConstants.BINDS_TO));
        Set<String> bindsTo = new HashSet<>();
        if (bindsToAttribute != null) {
            String[] inboundEndpointNames = bindsToAttribute.getAttributeValue().split(",");
            for (String inboundEndpointName : inboundEndpointNames) {
                String trimmedInboundEndpointName = inboundEndpointName.trim();
                if (!trimmedInboundEndpointName.isEmpty()) {
                    bindsTo.add(trimmedInboundEndpointName);
                }
            }
        }
        return bindsTo;
    }

    private static void validateBindsTo(API api, Resource resource) {
        if (!api.getBindsTo().containsAll(resource.getBindsTo())) {
            handleException("A resource definition's 'binds-to' must be a subset of its API definition's 'binds-to'");
        }
    }

    public static void populateBindsTo(API api) {
        if (api.getBindsTo().isEmpty()) {
            api.addBindsTo(ApiConstants.DEFAULT_BINDING_ENDPOINT_NAME);
        }
        for (Resource resource : api.getResources()) {
            if (resource.getBindsTo().isEmpty()) {
                // Resource has no inbound endpoint bindings specified. Inherit 'binds-to' from the API.
                resource.addAllBindsTo(api.getBindsTo());
            } else {
                validateBindsTo(api, resource);
            }
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
