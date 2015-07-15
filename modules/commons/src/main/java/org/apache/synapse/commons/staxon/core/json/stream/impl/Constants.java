/**
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.staxon.core.json.stream.impl;

public final class Constants {
    private Constants() {
    }

    /** The JSON Key for wrapper type JSON Object */
    public static final String OBJECT = "jsonObject";
    /** The JSON Key for wrapper type anonymous JSON array */
    public static final String ARRAY = "jsonArray";
    /** The JSON Key for wrapper type anonymous JSON array elements */
    public static final String ARRAY_ELEM = "jsonElement";

    public static final String EMPTY = "jsonEmpty";
    public static final String EMPTY_VALUE = "_JsonScanner_EMPTY_OBJECT";

    public static final String ID = "_JsonReader";
    /** Used when the local name starts with a digit character. */
    public static final String PRECEDING_DIGIT_S = "_PD_";
    /** Final prefix for local names that have preceding digits */
    public static final String PRECEDING_DIGIT = ID + PRECEDING_DIGIT_S;
    /** Used when the local name starts with the $ character. */
    public static final String PRECEDING_DOLLOR_S = "_PS_";
    public static final String PRECEDING_DOLLOR = ID + PRECEDING_DOLLOR_S;

    public static final int C_DOLLOR = '$';
    public static final int C_USOCRE = '_';

    public static enum SCANNER {
        /** Instructs the XML Reader to use the generated scanner to tokenize the JSON stream. */
        DEFAULT("DEFAULT"),
        /** Instructs the XML Reader to use the JSON scanner that can process anonymous nested arrays.<br/>
         If this scanner is used, the input stream has to be read once within the message builder to identify the type <br/>
         of JSON message (ie. if the payload is a JSON Object or a JSON array object.)*/
        SCANNER_1("SCANNER_1"),
        /** Instructs the XML Reader to use the JSON scanner that can both process anonymous (nested/top level) arrays <br/>
         and adds additional tokens to the input stream to identify the type of JSON steam.
         If this scanner is used, theres no need to pre-read the input stream  within the message builder to identify the type <br/>
         of JSON message (ie. if the payload is a JSON Object or a JSON array object.)*/
        SCANNER_2("SCANNER_2");

        private final String name;

        SCANNER(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}
