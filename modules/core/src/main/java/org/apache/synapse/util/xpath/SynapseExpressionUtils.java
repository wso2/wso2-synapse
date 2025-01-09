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

import org.apache.synapse.util.synapse.expression.constants.ExpressionConstants;
import org.jaxen.JaxenException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for Synapse Expressions
 */
public class SynapseExpressionUtils {

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
            if (synapseExpression.contains(ExpressionConstants.PAYLOAD_$)
                    || synapseExpression.contains(ExpressionConstants.PAYLOAD)) {
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
        return findKeyword(expr, ExpressionConstants.PAYLOAD)
                || findKeyword(expr, ExpressionConstants.PAYLOAD_$);
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
