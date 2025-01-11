/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.expression.impl.utils;

import org.apache.axiom.om.OMNode;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for Synapse Expressions.
 */
public class ExpressionUtils {

    private static final String regex = "xpath\\s*\\(\\s*'([^']+)'\\s*,\\s*'([^']+)'\\s*\\)";
    private static final Pattern pattern = Pattern.compile(regex);

    /**
     * Checks whether the synapse expression is content aware
     *
     * @param synapseExpression synapse expression string
     * @return true if the synapse expression is content aware, false otherwise
     */
    public static boolean isSynapseExpressionContentAware(String synapseExpression) {
        boolean isContentAware = false;
        if (synapseExpression.equals("payload") || synapseExpression.equals("$")) {
            return true;
        } else {
            if (synapseExpression.contains(SynapseConstants.PAYLOAD_$)
                    || synapseExpression.contains(SynapseConstants.PAYLOAD)) {
                isContentAware = checkForPayloadReference(synapseExpression);
            }
            if (!isContentAware && synapseExpression.contains("xpath(")) {
                // TODO change the regex to support xpath + variable syntax
                Pattern pattern = Pattern.compile("xpath\\(['\"](.*?)['\"]\\s*(,\\s*['\"](.*?)['\"])?\\)?");
                Matcher matcher = pattern.matcher(synapseExpression);
                // Find all matches
                while (matcher.find()) {
                    if (matcher.group(2) != null) {
                        // evaluating xpath on a variable so not content aware
                        continue;
                    }
                    String xpath = matcher.group(1);
                    try {
                        SynapseXPath synapseXPath = new SynapseXPath(xpath);
                        if (synapseXPath.isContentAware()) {
                            isContentAware = true;
                        }
                    } catch (JaxenException e) {
                        // Ignore the exception and continue
                    }
                }
            }
            return isContentAware;
        }
    }

    private static boolean checkForPayloadReference(String expr) {
        if (expr == null || expr.isEmpty()) {
            return false;
        }
        return findKeyword(expr, SynapseConstants.PAYLOAD)
                || findKeyword(expr, SynapseConstants.PAYLOAD_$);
    }

    private static boolean findKeyword(String expr, String keyword) {
        int index = expr.indexOf(keyword);
        while (index != -1) {
            if (isValidCharBeforeKeyword(expr, index)
                    && isValidCharAfterKeyword(expr, index, keyword.length())) {
                return true;
            }
            index = expr.indexOf(keyword, index + 1);
        }
        return false;
    }

    private static boolean isValidCharBeforeKeyword(String expr, int startIndex) {
        if (startIndex > 0) {
            char prev = expr.charAt(startIndex - 1);
            return !Character.isLetterOrDigit(prev) && prev != '_' && prev != '.'
                    && prev != '"' && prev != '\'';
        }
        return true;
    }

    private static boolean isValidCharAfterKeyword(String expr, int startIndex, int keywordLen) {
        int afterIndex = startIndex + keywordLen;
        if (afterIndex < expr.length()) {
            char next = expr.charAt(afterIndex);
            return !Character.isLetterOrDigit(next) && next != '_'
                    && next != '"' && next != '\'';
        }
        return true;
    }

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
}
