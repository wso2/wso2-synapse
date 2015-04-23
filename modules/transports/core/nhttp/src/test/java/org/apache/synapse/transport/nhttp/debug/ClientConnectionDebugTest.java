/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.transport.nhttp.debug;

import junit.framework.TestCase;

import java.util.regex.Pattern;

public class ClientConnectionDebugTest extends TestCase{

    /**
     * Test whether the formatted time is correct from format method
     */
    public void testFormat(){
        long unixTimeStamp;
        String formattedTime;
        //Format to match with HH:mm:ss.SSS
        Pattern timeFormat = Pattern.compile("[0-2][0-9]:[0-5][0-9]:[0-5][0-9].[0-9][0-9][0-9]");

        ClientConnectionDebug clientConnectionDebug = new ClientConnectionDebug(null);
        unixTimeStamp = System.currentTimeMillis();
        formattedTime = clientConnectionDebug.format(unixTimeStamp);

        assertTrue(timeFormat.matcher(formattedTime).matches());
    }
}
