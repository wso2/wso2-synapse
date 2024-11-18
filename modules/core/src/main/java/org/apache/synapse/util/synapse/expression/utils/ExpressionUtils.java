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

package org.apache.synapse.util.synapse.expression.utils;

import java.nio.charset.Charset;

/**
 * Utility class for Synapse Expressions.
 */
public class ExpressionUtils {
    public static String escapeSpecialCharacters(String input) {
        StringBuilder escapedString = new StringBuilder();
        for (char c : input.toCharArray()) {
            if ("\\.^$|?*+()[]{}".indexOf(c) != -1) {
                escapedString.append('\\');
            }
            escapedString.append(c);
        }
        return escapedString.toString();
    }

    public static String getCharset(String charsetName) {
        if (charsetName == null) {
            return null;
        }
        return Charset.forName(charsetName).toString();
    }

    /**
     * Extracts the variable name and JSONPath from the input expression.
     *
     * @param input The input string (e.g., var.abc.students[1].name or var["abc"]["students"][1].name).
     * @return A string array where index 0 is the variable name and index 1 is the JSONPath.
     */
    public static String[] extractVariableAndJsonPath(String input) {
        if (input.startsWith("var.[\"")) {
            String remaining = input.substring(6);
            int endBracketIndex = remaining.indexOf("\"]");
            String variableName = remaining.substring(0, endBracketIndex);
            String expression = remaining.substring(endBracketIndex + 2);
            return new String[]{variableName, expression};
        } else if (input.startsWith("var[\"")) {
            String remaining = input.substring(5);
            int endBracketIndex = remaining.indexOf("\"]");
            String variableName = remaining.substring(0, endBracketIndex);
            String expression = remaining.substring(endBracketIndex + 2);
            return new String[]{variableName, expression};
        } else if (input.startsWith("var.")) {
            String remaining = input.substring(4);
            int endDotIndex = remaining.indexOf(".");
            int beginArrIndex = remaining.indexOf("[");
            String variableName = "";
            String expression = "";

            if (endDotIndex == -1 && beginArrIndex == -1) {
                variableName = remaining;
            } else if (endDotIndex == -1) {
                variableName = remaining.substring(0, beginArrIndex);
                expression = remaining.substring(beginArrIndex);
            } else if (beginArrIndex == -1) {
                variableName = remaining.substring(0, endDotIndex);
                expression = remaining.substring(endDotIndex);
            } else {
                int minIndex = Math.min(endDotIndex, beginArrIndex);
                variableName = remaining.substring(0, minIndex);
                expression = remaining.substring(minIndex);
            }
            return new String[]{variableName, expression};
        } else {
            throw new IllegalArgumentException("Invalid input format. Could not parse variable and JSONPath.");
        }
    }
}
