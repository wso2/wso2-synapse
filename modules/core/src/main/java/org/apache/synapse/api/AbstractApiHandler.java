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

package org.apache.synapse.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.api.version.ContextVersionStrategy;
import org.apache.synapse.api.version.DefaultStrategy;
import org.apache.synapse.api.version.URLBasedVersionStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents an abstract API handler, which can process messages via APIs.
 */
public abstract class AbstractApiHandler {
    private static final Log log = LogFactory.getLog(AbstractApiHandler.class);

    public abstract boolean process(MessageContext synCtx);

    protected abstract boolean dispatchToAPI(MessageContext synCtx);

    protected boolean dispatchToAPI(Collection<API> apiSet, MessageContext synCtx) {
        //Since swapping elements are not possible with sets, Collection is converted to a List
        List<API> defaultStrategyApiSet = new ArrayList<API>(apiSet);
        API defaultAPI = null;

        Object apiObject = synCtx.getProperty(RESTConstants.PROCESSED_API);
        if (apiObject != null) {
            if (identifyAPI((API) apiObject, synCtx, defaultStrategyApiSet)) {
                return true;
            }
        } else {
            for (API api : apiSet) {
                if (identifyAPI(api, synCtx, defaultStrategyApiSet)) {
                    return true;
                }
            }
        }

        for (API api : defaultStrategyApiSet) {
            api.setLogSetterValue();
            if (api.canProcess(synCtx)) {
                if (log.isDebugEnabled()) {
                    log.debug("Located specific API: " + api.getName() + " for processing message");
                }
                apiProcess(synCtx, api);
                return true;
            }
        }

        if (defaultAPI != null && defaultAPI.canProcess(synCtx)) {
            defaultAPI.setLogSetterValue();
            apiProcess(synCtx, defaultAPI);
            return true;
        }

        return false;
    }

    protected void apiProcess(MessageContext synCtx, API api) {
        Integer statisticReportingIndex = 0;
        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            statisticReportingIndex = OpenEventCollector
                    .reportEntryEvent(synCtx, api.getAPIName(), api.getAspectConfiguration(), ComponentType.API);
            api.process(synCtx);
            CloseEventCollector.tryEndFlow(synCtx, api.getAPIName(), ComponentType.API, statisticReportingIndex, true);
        } else {
            api.process(synCtx);
        }
    }

    //Process APIs which have context or url strategy
    protected void apiProcessNonDefaultStrategy(MessageContext synCtx, API api) {
        Integer statisticReportingIndex = 0;
        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            statisticReportingIndex = OpenEventCollector
                    .reportEntryEvent(synCtx, api.getAPIName() + "_" + api.getVersion(), api.getAspectConfiguration(),
                            ComponentType.API);
            api.process(synCtx);
            CloseEventCollector.tryEndFlow(synCtx, api.getAPIName(), ComponentType.API, statisticReportingIndex, true);
        } else {
            api.process(synCtx);
        }
    }

    protected boolean identifyAPI(API api, MessageContext synCtx, List defaultStrategyApiSet) {
        API defaultAPI = null;
        api.setLogSetterValue();
        if ("/".equals(api.getContext())) {
            defaultAPI = api;
        } else if (api.getVersionStrategy().getClass().getName().equals(DefaultStrategy.class.getName())) {
            //APIs whose VersionStrategy is bound to an instance of DefaultStrategy, should be skipped and processed at
            // last.Otherwise they will be always chosen to process the request without matching the version.
            defaultStrategyApiSet.add(api);
        } else if (api.getVersionStrategy().getClass().getName().equals(ContextVersionStrategy.class.getName())
                || api.getVersionStrategy().getClass().getName().equals(URLBasedVersionStrategy.class.getName())) {
            api.setLogSetterValue();
            if (api.canProcess(synCtx)) {
                if (log.isDebugEnabled()) {
                    log.debug("Located specific API: " + api.getName() + " for processing message");
                }
                apiProcessNonDefaultStrategy(synCtx, api);
                return true;
            }
        } else if (api.canProcess(synCtx)) {
            if (log.isDebugEnabled()) {
                log.debug("Located specific API: " + api.getName() + " for processing message");
            }
            api.process(synCtx);
            return true;
        }
        return false;
    }
}
