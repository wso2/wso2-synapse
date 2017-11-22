/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.transport.utils;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class TCPUtils {
    /**
     * Check whether the provided port is open
     *
     * @param port The port that needs to be checked
     * @param host The host address
     * @return true if the <code>port</code> is open & false otherwise
     */
    public static boolean isPortOpen(int port, String host) {
        Socket socket = null;
        boolean isPortOpen;
        try {
            socket = new Socket(host, port);
            isPortOpen = socket.isConnected();
        } catch (IOException e) {
            isPortOpen = false;
        } finally {
            try {
                if ((socket != null) && (socket.isConnected())) {
                    socket.close();
                }
            } catch (IOException e) {
                //ignore
            }
        }
        return isPortOpen;
    }

    /**
     * Check whether the provided port is open within the given timeout
     *
     * @param port The port that needs to be checked
     * @param host The host address
     * @param timeOut maximum timeout wait for port to open
     * @return true if the <code>port</code> is open within given timeout & false otherwise
     */
    public static boolean isPortOpen(int port, String host, final int timeOut) {
        boolean isPortOpen = false;
        long startTime = System.currentTimeMillis();
        while (!isPortOpen && (System.currentTimeMillis() - startTime) < timeOut) {
            isPortOpen = isPortOpen(port, host);
            if (isPortOpen) {
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isPortOpen;
    }
}
