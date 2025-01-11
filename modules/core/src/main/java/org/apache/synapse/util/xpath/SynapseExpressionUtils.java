/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.util.xpath;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for Synapse Expressions
 */
public class SynapseExpressionUtils {

    private static final String regex = "xpath\\s*\\(\\s*'([^']+)'\\s*,\\s*'([^']+)'\\s*\\)";
    private static final Pattern pattern = Pattern.compile(regex);


    public static boolean isVariableXPathExpression(String synapseExpression) {

        Matcher matcher = pattern.matcher(synapseExpression);
        return matcher.find();
    }

    public static String getVariableFromVariableXPathExpression(String synapseExpression) {

        Matcher matcher = pattern.matcher(synapseExpression);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    public static String getXPathFromVariableXPathExpression(String synapseExpression) {

        Matcher matcher = pattern.matcher(synapseExpression);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
