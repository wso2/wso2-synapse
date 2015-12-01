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

package org.apache.synapse.debug;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;

/**
 * A separate dedicated thread that handles debug commands parsing and processing. Thread spans the
 * the lifecycle of ESB runtime, if ESB environment is created debug mode.
 */
public class SynapseDebugTCPListener extends Thread {

    private SynapseDebugManager debugManager;
    private SynapseDebugInterface debugInterface;
    private boolean isDebugModeInProgress = false;
    private static final Log log = LogFactory.getLog(SynapseDebugTCPListener.class);


    public SynapseDebugTCPListener(SynapseDebugManager debugManager,
                                   SynapseDebugInterface debugInterface) {
        this.debugManager = debugManager;
        this.debugInterface = debugInterface;
    }

    public void setDebugModeInProgress(boolean isDebugModeInProgress) {
        this.isDebugModeInProgress = isDebugModeInProgress;
    }

    public boolean getDebugModeInProgress() {
        return isDebugModeInProgress;
    }

    @Override
    public void run() {
        String debug_line = null;
        while (isDebugModeInProgress) {
            try {
                //this is a blocking call
                debug_line = debugInterface.getPortListenReader().readLine();
                if (debug_line != null) {
                    debugManager.processDebugCommand(debug_line);
                    debug_line = null;
                }
            } catch (IOException ex) {
                log.error("Unable to process debug commands", ex);
            }

        }

    }

    public void shutDownListener() {
        this.isDebugModeInProgress = false;
    }

}
