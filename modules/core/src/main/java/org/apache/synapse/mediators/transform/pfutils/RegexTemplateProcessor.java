/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */


package org.apache.synapse.mediators.transform.pfutils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.transform.ArgumentDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTemplateProcessor extends TemplateProcessor {

    private static final Log log = LogFactory.getLog(RegexTemplateProcessor.class);
    private final Pattern pattern = Pattern.compile("\\$(\\d)+");

    @Override
    public String processTemplate(String template, String mediaType, MessageContext synCtx) {

        StringBuffer result = new StringBuffer();
        replace(template, result, mediaType, synCtx);
        return result.toString();
    }

    @Override
    public void init() {
        // nothing to do since no pre processing is needed
    }

    /**
     * Replaces the payload format with SynapsePath arguments which are evaluated using getArgValues().
     *
     * @param format
     * @param result
     * @param synCtx
     */
    private void replace(String format, StringBuffer result, String mediaType, MessageContext synCtx) {

        HashMap<String, ArgumentDetails>[] argValues = getArgValues(mediaType, synCtx);
        HashMap<String, ArgumentDetails> replacement;
        Map.Entry<String, ArgumentDetails> replacementEntry;
        String replacementValue;
        Matcher matcher;

        if (JSON_TYPE.equals(mediaType) || TEXT_TYPE.equals(mediaType)) {
            matcher = pattern.matcher(format);
        } else {
            matcher = pattern.matcher("<pfPadding>" + format + "</pfPadding>");
        }
        try {
            while (matcher.find()) {
                String matchSeq = matcher.group();
                replacement = getReplacementValue(argValues, matchSeq);
                replacementEntry = replacement.entrySet().iterator().next();
                replacementValue = prepareReplacementValue(mediaType, synCtx, replacementEntry);
                matcher.appendReplacement(result, replacementValue);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("#replace. Mis-match detected between number of formatters and arguments", e);
        }
        matcher.appendTail(result);
    }

    private HashMap<String, ArgumentDetails> getReplacementValue(HashMap<String, ArgumentDetails>[] argValues,
                                                                 String matchSeq) {

        HashMap<String, ArgumentDetails> replacement;
        int argIndex;
        try {
            argIndex = Integer.parseInt(matchSeq.substring(1));
        } catch (NumberFormatException e) {
            argIndex = Integer.parseInt(matchSeq.substring(2, matchSeq.length() - 1));
        }
        replacement = argValues[argIndex - 1];
        return replacement;
    }
}
