/**
 *  Copyright (c) 2005-2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json.jsonprocessor.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValidatorConstants {

    private ValidatorConstants() {
    }

    public static final String REGEX = "^\"|\"$";
    public static String QUOTE_REPLACE_REGEX = "^\"|\"$";
    public static final String TYPE_KEY = "type";
    public static final String ITEM_KEY = "items";
    public static final String ENUM = "enum";
    public static final String CONST = "const";

    public static final Set<String> NUMERIC_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"number", "integer"}
    ));
    public static final Set<String> BOOLEAN_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"boolean"}
    ));
    public static final Set<String> NOMINAL_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"String", "string"}
    ));
    public static final Set<String> OBJECT_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"object"}
    ));
    public static final Set<String> ARRAY_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"array"}
    ));
    public static final Set<String> NULL_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"null"}
    ));
}
