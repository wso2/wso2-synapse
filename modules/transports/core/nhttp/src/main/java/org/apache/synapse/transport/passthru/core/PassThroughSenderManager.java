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
import org.apache.synapse.transport.passthru.PassThroughHttpSSLSender;
import org.apache.synapse.transport.passthru.PassThroughHttpSender;


/**
 * Class responsible for handle outBound side shared PassThroughHttpSender
 */
public class PassThroughSenderManager {

    private static final Logger logger = Logger.getLogger(PassThroughSenderManager.class);

    private static PassThroughSenderManager passThroughSenderManager;

    private PassThroughHttpSender sharedPassThroughHttpSender;


    private PassThroughSenderManager(PassThroughHttpSender passThroughHttpSender) {
       this.sharedPassThroughHttpSender = passThroughHttpSender;
    }

    /**
     * @return Shared PassThroughHttpSender
     */
    public PassThroughHttpSender getSharedPassThroughHttpSender() {
        return sharedPassThroughHttpSender;
    }


    /**
     * @return return passThroughSenderManager
     */
    public static PassThroughSenderManager registerPassThroughHttpSender(PassThroughHttpSender passThroughHttpSender) {
        if (passThroughSenderManager == null) {
            synchronized (PassThroughSenderManager.class) {
                if (passThroughSenderManager == null) {
                    passThroughSenderManager = new PassThroughSenderManager(passThroughHttpSender);
                }
            }
        }
        return passThroughSenderManager;
    }


    public static PassThroughSenderManager getInstance(){
        return passThroughSenderManager;
    }
}
