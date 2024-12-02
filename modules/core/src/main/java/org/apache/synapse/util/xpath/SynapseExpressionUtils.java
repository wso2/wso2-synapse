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

import org.jaxen.JaxenException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for Synapse Expressions
 */
public class SynapseExpressionUtils {

    /**
     * Checks whether the synapse expression is content aware
     *
     * @param synapseExpression synapse expression string
     * @return true if the synapse expression is content aware, false otherwise
     */
    public static boolean isSynapseExpressionContentAware(String synapseExpression) {

        // TODO : Need to improve the content aware detection logic
        if (synapseExpression.equals("payload") || synapseExpression.equals("$")
                || synapseExpression.contains("payload.") || synapseExpression.contains("$.")) {
            return true;
        } else if (synapseExpression.contains("xpath(")) {
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
                        return true;
                    }
                } catch (JaxenException e) {
                    // Ignore the exception and continue
                }
            }
        }
        return false;
    }
}
