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

import org.apache.axiom.om.OMNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.List;

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
     * @param input The input string (e.g., vars.abc.students[1].name or vars["abc"]["students"][1].name).
     * @return A string array where index 0 is the variable name and index 1 is the JSONPath.
     */
    public static String[] extractVariableAndJsonPath(String input) {
        String VAR_DOT_ARR = "vars.[\"";
        String VAR_ARR = "vars[\"";
        String VAR_DOT = "vars.";
        if (input.startsWith(VAR_DOT_ARR)) {
            String remaining = input.substring(VAR_DOT_ARR.length());
            int endBracketIndex = remaining.indexOf("\"]");
            String variableName = remaining.substring(0, endBracketIndex);
            String expression = remaining.substring(endBracketIndex + 2);
            return new String[]{variableName, expression};
        } else if (input.startsWith(VAR_ARR)) {
            String remaining = input.substring(VAR_ARR.length());
            int endBracketIndex = remaining.indexOf("\"]");
            String variableName = remaining.substring(0, endBracketIndex);
            String expression = remaining.substring(endBracketIndex + 2);
            return new String[]{variableName, expression};
        } else if (input.startsWith(VAR_DOT)) {
            String remaining = input.substring(VAR_DOT.length());
            int endDotIndex = remaining.indexOf(".");
            int beginArrIndex = remaining.indexOf("[");
            String variableName;
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

    /**
     * Checks whether the given variable is a JSON object.
     * @param variable The variable value to be checked.
     * @return true if the variable is a JSON object, false otherwise.
     */
    public static boolean isXMLVariable(Object variable) {
        boolean isXML = false;
        if (variable instanceof OMNode) {
            isXML = true;
        } else if (variable instanceof List) {
            List<?> list = (List<?>) variable;
            for (Object obj : list) {
                if (obj instanceof OMNode) {
                    isXML = true;
                    break;
                }
            }
        }
        return isXML;
    }

    /**
     * Rounds the given value to the specified number of decimal places.
     * @param value The value to be rounded.
     * @param decimalPlaces The number of decimal places to round to.
     * @return The rounded value.
     */
    public static Double round (double value, int decimalPlaces) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
