/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config;

public class Utils {

    public static final String RESOURCES_IDENTIFIER = "resources:";
    public static final String CONVERTED_RESOURCES_IDENTIFIER = "gov:mi-resources/";

    public static String transformFileKey(String fileKey) {

        if (fileKey.startsWith(RESOURCES_IDENTIFIER)) {
            return CONVERTED_RESOURCES_IDENTIFIER + fileKey.substring(RESOURCES_IDENTIFIER.length());
        }
        return fileKey;
    }
}
