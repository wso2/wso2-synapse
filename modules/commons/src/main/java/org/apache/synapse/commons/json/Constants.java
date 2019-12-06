/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.json;

public final class Constants {
    private Constants() {
    }

    public static final String JSON_STRING = "JSON_STRING";

    /**
     * The JSON Key for wrapper type JSON Object
     */
    public static final String K_OBJECT = "\"jsonObject\"";
    /**
     * The JSON Key for wrapper type anonymous JSON array
     */
    public static final String K_ARRAY = "\"jsonArray\"";
    /**
     * The JSON Key for wrapper type anonymous JSON array elements
     */
    public static final String K_ARRAY_ELEM = "\"jsonElement\"";

    public static final String ID = "_JsonReader";
    /**
     * Used when the local name starts with a digit character.
     */
    public static final String PRECEDING_DIGIT_S = "_PD_";
    /**
     * Final prefix for local names that have preceding digits
     */
    public static final String PRECEDING_DIGIT = ID + PRECEDING_DIGIT_S;
    /**
     * Used when the local name starts with the $ character.
     */
    public static final String PRECEDING_DOLLOR_S = "_PS_";
    public static final String PRECEDING_DOLLOR = ID + PRECEDING_DOLLOR_S;
    /**
     * The Dollar character
     */
    public static final int C_DOLLOR = '$';
    /**
     * The underscore character
     */
    public static final int C_USOCRE = '_';

    public static final String ID_KEY = ID + "_";

    // Constants used to control the behavior of JSON to XML conversion
    // Preserve the namespace declarations() in the JSON output in the XML -> JSON transformation.
    public static final String SYNAPSE_COMMONS_JSON_PRESERVE_NAMESPACE = "synapse.commons.json.preserve.namespace";

    // Build valid XML NCNames when building XML element names in the JSON -> XML transformation.
    public static final String SYNAPSE_COMMONS_JSON_BUILD_VALID_NC_NAMES = "synapse.commons.json.buildValidNCNames";

    // Enable primitive types in json out put in the XML -> JSON transformation.
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_AUTO_PRIMITIVE = "synapse.commons.json.output.autoPrimitive";

    // The namespace prefix separate character in the JSON output of the XML -> JSON transformation
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_NAMESPACE_SEP_CHAR =
            "synapse.commons.json.output.namespaceSepChar";

    // Add XML namespace declarations in the JSON output in the XML -> JSON transformation.
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_ENABLE_NS_DECLARATIONS =
            "synapse.commons.json.output.enableNSDeclarations";

    // Disable auto primitive conversion in XML -> JSON transformation
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_DISABLE_AUTO_PRIMITIVE_REGEX =
            "synapse.commons.json.output.disableAutoPrimitive.regex";

    // Property to set the JSON output to an array element in XML -> JSON transformation
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_JSON_OUT_AUTO_ARRAY =
            "synapse.commons.json.output.jsonoutAutoArray";

    // Property to set the JSON output to an xml multiple processing instruction in XML -> JSON transformation
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_JSON_OUT_MULTIPLE_PI =
            "synapse.commons.json.output.jsonoutMultiplePI";

    // Property to set the XML output to an array element in XML -> JSON transformation
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_XML_OUT_AUTO_ARRAY =
            "synapse.commons.json.output.xmloutAutoArray";

    // Property to set the XML output to an xml multiple processing instruction in XML -> JSON transformation
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_XML_OUT_MULTIPLE_PI =
            "synapse.commons.json.output.xmloutMultiplePI";

    // Property to set and empty element to empty JSON string in XML -> JSON transformation
    public static final String SYNAPSE_COMMONS_JSON_OUTPUT_EMPTY_XML_ELEM_TO_EMPTY_STR =
            "synapse.commons.json.output.emptyXmlElemToEmptyStr";

    // Property to set whether the user expects synapse to append a default charset encoding(UTF-8)
    // to the outgoing request
    public static final String SET_CONTENT_TYPE_CHARACTER_ENCODING = "setCharacterEncoding";

    /**
     * Property to inform Staxon library to include xml multiple processing instruction in JSON -> XML transformation
     */
    public static final String SYNAPSE_JSON_TO_XML_PROCESS_INSTRUCTION_ENABLE = "synapse.json.to.xml.processing.instruction.enabled";

    /*Property which holds the synapse commons json stream of payload*/
    public static final String ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM = "org.apache.synapse.commons.json.JsonInputStream";

    public static final String SYNAPSE_COMMONS_ENABLE_XML_NIL_READ_WRITE = "synapse.commons.enableXmlNilReadWrite";

    public static final String SYNAPSE_COMMONS_JSON_DISABLE_AUTO_PRIMITIVE_CUSTOM_REPLACE_REGEX =
            "synapse.commons.json.json.output.disableAutoPrimitive.customReplaceRegex";

    public static final String SYNAPSE_COMMONS_JSON_DISABLE_AUTO_PRIMITIVE_CUSTOM_REPLACE_SEQUENCE =
            "synapse.commons.json.json.output.disableAutoPrimitive.customReplaceSequence";

    public static final String SYNAPSE_COMMONS_ENABLE_XML_NULL_FOR_EMPTY_ELEMENT =
            "synapse.commons.enableXmlNullForEmptyElement";

    // Property to preserve spaces in XML -> JSON transformation
    public static final String PRESERVE_SPACES = "PRESERVE_SPACES";

}
