/**
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.synapse.transport.passthru.config;

public class PassThroughCorrelationConfigDataHolder {
    private static boolean enable;
    private static boolean systemEnable;
    private static boolean toggled;

    private PassThroughCorrelationConfigDataHolder() {
    }

    public static boolean isEnable() {
        return (enable || systemEnable);
    }

    public static boolean isToggled() {
        return toggled;
    }

    public static void resetToggled(){
        toggled = false;
    }

    public static void setEnable(boolean enable) {
        if(PassThroughCorrelationConfigDataHolder.enable != enable){
            toggled = true;
        }
        PassThroughCorrelationConfigDataHolder.enable = enable;
    }

    public static boolean isSystemEnable() {
        return systemEnable;
    }

    public static void setSystemEnable(boolean systemEnable) {
        PassThroughCorrelationConfigDataHolder.systemEnable = systemEnable;
    }
}
