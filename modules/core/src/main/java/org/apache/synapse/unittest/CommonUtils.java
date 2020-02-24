/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.unittest;

import com.google.gson.JsonParser;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.AbstractMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Class responsible for the common utilities in unit test.
 */
class CommonUtils {

    private CommonUtils() {
    }

    /**
     * Get stack trace from the exception.
     *
     * @param exception exception
     * @return exception stack trace as a string
     */
    static String stackTraceToString(Throwable exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getMessage());
        sb.append(Constants.NEW_LINE);

        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append(element.toString());
            sb.append(Constants.NEW_LINE);
        }
        return sb.toString();
    }

    /**
     * Get stack trace from the exception with custom error message.
     *
     * @param exception exception
     * @param customErrorMessage custom error message
     * @return exception stack trace as a string
     */
    static String stackTraceToString(Throwable exception, String customErrorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(customErrorMessage);
        sb.append(Constants.NEW_LINE);
        sb.append(exception.getMessage());
        sb.append(Constants.NEW_LINE);

        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append(element.toString());
            sb.append(Constants.NEW_LINE);
        }
        return sb.toString();
    }

    /**
     * Check input string type JSON, XML or TEXT.
     *
     * @param inputString input message
     * @return type of the input string and trimmed input string as a Map.Entry
     */
    static Map.Entry<String, String> checkInputStringFormat(String inputString) {
        String inputStringFormat;
        //trim the string
        String trimedString = inputString.trim();

        //remove CDATA tag from the string if exists
        if (trimedString.startsWith("<![CDATA[")) {
            trimedString = trimedString.substring(9);
            int i = trimedString.indexOf("]]>");
            if (i == -1) {
                throw new IllegalStateException("argument starts with <![CDATA[ but cannot find pairing ]]>");
            }
            trimedString = trimedString.substring(0, i);
        }

        //check the input string format
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(trimedString)));
            inputStringFormat = Constants.XML_FORMAT;
        } catch (Exception e) {
            try {
                new JsonParser().parse(trimedString).getAsJsonObject();
                inputStringFormat = Constants.JSON_FORMAT;
            } catch (Exception exception) {
                inputStringFormat = Constants.TEXT_FORMAT;
            }
        }

        return new AbstractMap.SimpleEntry<>(inputStringFormat, trimedString);
    }
}
