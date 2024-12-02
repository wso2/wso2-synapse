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
package org.apache.synapse.util.synapse.expression.constants;

/**
 * Constants for Synapse Expressions.
 */
public class ExpressionConstants {
    public static final String AND = "and";
    public static final String OR = "or";
    public static final String NOT = "not";
    public static final String TO_LOWER = "toLower";
    public static final String TO_UPPER = "toUpper";
    public static final String LENGTH = "length";
    public static final String SUBSTRING = "subString";
    public static final String STARTS_WITH = "startsWith";
    public static final String ENDS_WITH = "endsWith";
    public static final String CONTAINS = "contains";
    public static final String TRIM = "trim";
    public static final String REPLACE = "replace";
    public static final String SPLIT = "split";
    public static final String INDEX_OF = "indexOf";
    public static final String ABS = "abs";
    public static final String CEIL = "ceil";
    public static final String FLOOR = "floor";
    public static final String SQRT = "sqrt";
    public static final String LOG = "log";
    public static final String POW = "pow";
    public static final String B64ENCODE = "base64encode";
    public static final String B64DECODE = "base64decode";
    public static final String URL_ENCODE = "urlEncode";
    public static final String URL_DECODE = "urlDecode";
    public static final String IS_NUMBER = "isNumber";
    public static final String IS_STRING = "isString";
    public static final String IS_ARRAY = "isArray";
    public static final String IS_OBJECT = "isObject";
    public static final String OBJECT = "object";
    public static final String ARRAY = "array";
    public static final String REGISTRY = "registry";
    public static final String EXISTS = "exists";
    public static final String XPATH = "xpath";
    public static final String SECRET = "secret";
    public static final String NOW = "now";
    public static final String FORMAT_DATE_TIME = "formatDateTime";
    public static final String CHAR_AT = "charAt";

    public static final String ROUND = "round";
    public static final String INTEGER = "integer";
    public static final String FLOAT = "float";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";

    public static final String PAYLOAD = "payload";
    public static final String PAYLOAD_$ = "$";
    public static final String SYNAPSE_EXPRESSION_IDENTIFIER_START = "${";
    public static final String SYNAPSE_EXPRESSION_IDENTIFIER_END = "}";
    public static final String AXIS2 = "axis2";
    public static final String QUERY_PARAM = "queryParams";
    public static final String URI_PARAM = "uriParams";
    public static final String FUNC_PARAM = "functionParams";


    public static final String UNKNOWN = "unknown";
    public static final String VAULT_LOOKUP = "wso2:vault-lookup('";
}
