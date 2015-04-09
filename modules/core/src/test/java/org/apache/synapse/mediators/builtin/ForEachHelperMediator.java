/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.builtin;

import org.apache.axis2.AxisFault;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper mediator class to test ForEach mediator
 */
public class ForEachHelperMediator extends AbstractMediator implements ManagedLifecycle {

    private List mediatedContext = new ArrayList();
    private int msgcount;

    public boolean mediate(MessageContext synCtx) {

        try {
            mediatedContext.add(MessageHelper.cloneMessageContext(synCtx));
        } catch (AxisFault e) {
            e.printStackTrace();
        }
        msgcount++;
        return false;
    }

    public MessageContext getMediatedContext(int position) {
        if (mediatedContext.size() > position) {
            return (MessageContext) mediatedContext.get(position);
        } else {
            return null;
        }
    }

    public void clearMediatedContexts() {
        mediatedContext.clear();
        msgcount = 0;
    }


    public void init(SynapseEnvironment se) {
        msgcount = 0;
    }

    public int getMsgCount() {
        return msgcount;
    }

    public void destroy() {
        clearMediatedContexts();
        msgcount = 0;
    }
}
