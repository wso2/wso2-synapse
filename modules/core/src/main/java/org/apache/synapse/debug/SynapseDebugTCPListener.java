/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.debug;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;

/**
 * a separate thread that handles debug commands parsing and processing
 **/

public class SynapseDebugTCPListener extends Thread {
    private SynapseDebugManager debugManager;
    private SynapseDebugInterface debugInterface;
    private boolean isDebugModeInProgress=false;
    private static final Log log = LogFactory.getLog(SynapseDebugTCPListener.class);


    public SynapseDebugTCPListener(SynapseDebugManager debugManager, SynapseDebugInterface debugInterface){
        this.debugManager=debugManager;
        this.debugInterface=debugInterface;
    }

    public void setDebugModeInProgress(boolean isDebugModeInProgress){
        this.isDebugModeInProgress=isDebugModeInProgress;
    }

    public boolean getDebugModeInProgress(){
        return isDebugModeInProgress;
    }

    @Override
    public void run() {
        String debug_line=null;
        while(isDebugModeInProgress){
            try {
                    debug_line = debugInterface.getPortListenReader().readLine();
                    if (debug_line != null) {
                        debugManager.processDebugCommand(debug_line);
                        debug_line = null;
                    }
            }catch (IOException ex){
                log.error("Unable to process debug commands",ex);
            }

        }


    }

    public void shutDownListener(){
         this.isDebugModeInProgress=false;
    }


}
