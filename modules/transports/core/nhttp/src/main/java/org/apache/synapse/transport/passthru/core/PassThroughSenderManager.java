/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.passthru.core;

import org.apache.log4j.Logger;
import org.apache.synapse.transport.passthru.PassThroughHttpSender;


/**
 * Class responsible for handle outBound side shared PassThroughHttpSender
 */
public class PassThroughSenderManager {

    private static final Logger logger = Logger.getLogger(PassThroughSenderManager.class);

    private static PassThroughSenderManager passThroughSenderManager;

    private PassThroughHttpSender SharedPassThroughHttpSender;

    private PassThroughSenderManager(PassThroughHttpSender passThroughHttpSender) {
        this.SharedPassThroughHttpSender = passThroughHttpSender;
    }

    public PassThroughHttpSender getSharedPassThroughHttpSender() {
        return SharedPassThroughHttpSender;
    }

    /**
     * @param passThroughHttpSender register shared PassThroughHttpSender
     */
    public static void registerPassThroughHttpSender(PassThroughHttpSender passThroughHttpSender) {
        if (passThroughSenderManager == null){
            synchronized (PassThroughSenderManager.class) {
                passThroughSenderManager = new PassThroughSenderManager(passThroughHttpSender);
            }
        }
    }

    /**
     * @return return passThroughSenderManager
     */
    public static PassThroughSenderManager getInstance() {
        if (passThroughSenderManager != null) {
            return passThroughSenderManager;
        } else {
            logger.error("PassThroughSenderManager not initialized properly may be " +
                    "PassThroughHttpSender might not have  started properly so PassThroughSenderManager is null");
            return null;
        }
    }
}
