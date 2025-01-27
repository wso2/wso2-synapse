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
package org.apache.synapse.util.synapse.expression.ast;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.util.synapse.expression.constants.ExpressionConstants;
import org.apache.synapse.util.synapse.expression.context.EvaluationContext;
import org.apache.synapse.util.synapse.expression.exception.EvaluationException;
import org.apache.synapse.util.synapse.expression.utils.ExpressionUtils;
import org.jaxen.JaxenException;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Represents a node in the AST that holds a predefined function.
 * Ex: toUpper() toLower()
 */
public class PredefinedFunctionNode implements ExpressionNode {

    private final String functionName;
    private final List<ExpressionNode> arguments;

    public PredefinedFunctionNode(ArgumentListNode arguments, String functionName) {
        this.arguments = arguments.getArguments();
        this.functionName = functionName;
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context, boolean isObjectValue) {
        if (arguments.isEmpty()) {
            return handleNoArgumentFunctions();
        } else if (arguments.size() == 1) {
            return handleSingleArgumentFunctions(context, isObjectValue);
        } else if (arguments.size() == 2) {
            return handleDoubleArgumentFunctions(context, isObjectValue);
        } else if (arguments.size() == 3) {
            return handleTripleArgumentFunctions(context, isObjectValue);
        }
        throw new EvaluationException("Invalid number of arguments: " + arguments.size()
                + " provided for the function: " + functionName);
    }

    private ExpressionResult handleNoArgumentFunctions() {
        if (functionName.equals(ExpressionConstants.NOW)) {
            return new ExpressionResult(System.currentTimeMillis());
        }
        throw new EvaluationException("Invalid function: " + functionName + " with no arguments");
    }

    private ExpressionResult handleSingleArgumentFunctions(EvaluationContext context, boolean isObjectValue) {
        ExpressionResult result = null;
        // do not evaluate the source for exists function - since we need to catch the exception
        if (!functionName.equals(ExpressionConstants.EXISTS)) {
            result = arguments.get(0).evaluate(context, isObjectValue);
            checkArguments(result, "source");
        }
        switch (functionName) {
            case ExpressionConstants.LENGTH:
                return handleLengthFunction(result);
            case ExpressionConstants.TO_LOWER:
                return handleToLowerFunction(result);
            case ExpressionConstants.TO_UPPER:
                return handleToUpperFunction(result);
            case ExpressionConstants.TRIM:
                return handleTrimFunction(result);
            case ExpressionConstants.ABS:
                return handleAbsFunction(result);
            case ExpressionConstants.CEIL:
                return handleCeilFunction(result);
            case ExpressionConstants.FLOOR:
                return handleFloorFunction(result);
            case ExpressionConstants.ROUND:
                return handleRoundFunction(result);
            case ExpressionConstants.SQRT:
                return handleSqrtFunction(result);
            case ExpressionConstants.B64ENCODE:
                return handleBase64EncodeFunction(result);
            case ExpressionConstants.B64DECODE:
                return handleBase64DecodeFunction(result);
            case ExpressionConstants.URL_ENCODE:
                return handleUrlEncodeFunction(result);
            case ExpressionConstants.URL_DECODE:
                return handleUrlDecodeFunction(result);
            case ExpressionConstants.IS_STRING:
                return new ExpressionResult(result.isString());
            case ExpressionConstants.IS_NUMBER:
                return new ExpressionResult(result.isInteger() || result.isDouble());
            case ExpressionConstants.IS_ARRAY:
                return new ExpressionResult(result.isArray());
            case ExpressionConstants.IS_OBJECT:
                return new ExpressionResult(result.isObject());
            case ExpressionConstants.STRING:
                return new ExpressionResult(result.asString());
            case ExpressionConstants.INTEGER:
                return handleIntegerConversion(result);
            case ExpressionConstants.FLOAT:
                return handleFloatConversion(result);
            case ExpressionConstants.BOOLEAN:
                return handleBooleanConversion(result);
            case ExpressionConstants.REGISTRY:
                return handleRegistryAccess(context, result, null);
            case ExpressionConstants.EXISTS:
                return handleExistsCheck(context, arguments.get(0), isObjectValue);
            case ExpressionConstants.OBJECT:
                return convertToObject(result);
            case ExpressionConstants.ARRAY:
                return convertToArray(result);
            case ExpressionConstants.XPATH:
                return evaluateXPATHExpression(context, result, isObjectValue);
            case ExpressionConstants.SECRET:
                return fetchSecretValue(context, result.asString());
            case ExpressionConstants.NOT:
                return new ExpressionResult(!result.asBoolean());
            default:
                throw new EvaluationException("Invalid function: " + functionName + " with one argument");
        }
    }

    private ExpressionResult handleDoubleArgumentFunctions(EvaluationContext context, boolean isObjectValue) {
        ExpressionResult source = arguments.get(0).evaluate(context, isObjectValue);
        ExpressionResult argument1 = arguments.get(1).evaluate(context, isObjectValue);
        checkArguments(source, "source");
        checkArguments(argument1, "argument1");
        switch (functionName) {
            case ExpressionConstants.SUBSTRING:
                return handleSubstringFunction(source, argument1);
            case ExpressionConstants.STARTS_WITH:
                return handleStartsWithFunction(source, argument1);
            case ExpressionConstants.ENDS_WITH:
                return handleEndsWithFunction(source, argument1);
            case ExpressionConstants.CONTAINS:
                return handleContainsFunction(source, argument1);
            case ExpressionConstants.SPLIT:
                return handleSplitFunction(source, argument1);
            case ExpressionConstants.POW:
                return handlePowFunction(source, argument1);
            case ExpressionConstants.B64ENCODE:
                return handleBase64EncodeFunction(source, argument1);
            case ExpressionConstants.URL_ENCODE:
                return handleUrlEncodeFunction(source, argument1);
            case ExpressionConstants.REGISTRY:
                return handleRegistryAccess(context, source, argument1);
            case ExpressionConstants.INDEX_OF:
                return handleIndexOfFunction(source, argument1);
            case ExpressionConstants.FORMAT_DATE_TIME:
                return handleFormatCurrentDateTimeFunction(source, argument1);
            case ExpressionConstants.CHAR_AT:
                return handleCharAtFunction(source, argument1);
            case ExpressionConstants.XPATH:
                return evaluateXPATHExpression(context, source, argument1.asString(), isObjectValue);
            case ExpressionConstants.ROUND:
                return handleRoundFunction(source, argument1);
            default:
                throw new EvaluationException("Invalid function: " + functionName + " with two arguments");
        }
    }

    private ExpressionResult handleTripleArgumentFunctions(EvaluationContext context, boolean isObjectValue) {
        ExpressionResult source = arguments.get(0).evaluate(context, isObjectValue);
        ExpressionResult argument1 = arguments.get(1).evaluate(context, isObjectValue);
        ExpressionResult argument2 = arguments.get(2).evaluate(context, isObjectValue);
        checkArguments(source, "source");
        checkArguments(argument1, "argument1");
        checkArguments(argument2, "argument2");
        switch (functionName) {
            case ExpressionConstants.SUBSTRING:
                return handleSubstringFunction(source, argument1, argument2);
            case ExpressionConstants.REPLACE:
                return handleReplaceFunction(source, argument1, argument2);
            case ExpressionConstants.INDEX_OF:
                return handleIndexOfFunction(source, argument1, argument2);
            case ExpressionConstants.FORMAT_DATE_TIME:
                return handleFormatDateTimeFunctions(source, argument1, argument2);
            default:
                throw new EvaluationException("Invalid function: " + functionName + " with three arguments");
        }
    }

    private void checkArguments(ExpressionResult result, String argumentName) {
        if (result == null || result.isNull()) {
            throw new EvaluationException("Null " + argumentName + " value provided for the function: " + functionName);
        }
    }

    private ExpressionResult handleLengthFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(result.asString().length());
        } else if (result.isArray()) {
            return new ExpressionResult(result.asJsonElement().getAsJsonArray().size());
        }
        throw new EvaluationException("Invalid argument provided for length function");
    }

    private ExpressionResult handleToLowerFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(result.asString().toLowerCase());
        } else if (result.isJsonPrimitive()) {
            return new ExpressionResult(new JsonPrimitive(result.asJsonElement().getAsString().toLowerCase()));
        }
        throw new EvaluationException("Invalid argument provided for toLower function");
    }

    private ExpressionResult handleToUpperFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(result.asString().toUpperCase());
        } else if (result.isJsonPrimitive()) {
            return new ExpressionResult(new JsonPrimitive(result.asJsonElement().getAsString().toUpperCase()));
        }
        throw new EvaluationException("Invalid argument provided for toUpper function");
    }

    private ExpressionResult handleTrimFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(result.asString().trim());
        }
        throw new EvaluationException("Invalid argument provided for trim function");
    }

    private ExpressionResult handleAbsFunction(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(Math.abs(result.asInt()));
        } else if (result.isDouble()) {
            return new ExpressionResult(result.asDouble().abs());
        }
        throw new EvaluationException("Invalid argument provided for abs function");
    }

    private ExpressionResult handleCeilFunction(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(result.asInt());
        } else if (result.isDouble()) {
            return new ExpressionResult(result.asDouble().setScale(0, RoundingMode.CEILING));
        }
        throw new EvaluationException("Invalid argument provided for ceil function");
    }

    private ExpressionResult handleFloorFunction(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(result.asInt());
        } else if (result.isDouble()) {
            return new ExpressionResult(result.asDouble().setScale(0, RoundingMode.FLOOR));
        }
        throw new EvaluationException("Invalid argument provided for floor function");
    }

    private ExpressionResult handleRoundFunction(ExpressionResult result) {
        if (result.isDouble()) {
            return new ExpressionResult((int) Math.round(result.asDouble().doubleValue()));
        } else if (result.isInteger()) {
            return new ExpressionResult(result.asInt());
        }
        throw new EvaluationException("Invalid argument provided for round function");
    }

    private ExpressionResult handleRoundFunction(ExpressionResult result, ExpressionResult decimalPlaces) {
        if (result.isDouble() && decimalPlaces.isInteger() && decimalPlaces.asInt() > 0) {
            return new ExpressionResult(ExpressionUtils.round(result.asDouble().doubleValue(), decimalPlaces.asInt()));
        } else if (result.isInteger() || result.isLong()) {
            return result;
        }
        throw new EvaluationException("Invalid argument provided for round function");
    }

    private ExpressionResult handleSqrtFunction(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(Math.sqrt(result.asInt()));
        } else if (result.isDouble()) {
            return new ExpressionResult(Math.sqrt(result.asDouble().doubleValue()));
        }
        throw new EvaluationException("Invalid argument provided for sqrt function");
    }

    private ExpressionResult handleBase64EncodeFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(Base64.getEncoder().encodeToString(result.asString().getBytes()));
        }
        throw new EvaluationException("Invalid argument provided for base64Encode function");
    }

    private ExpressionResult handleBase64DecodeFunction(ExpressionResult result) {
        if (result.isString()) {
            return new ExpressionResult(new String(Base64.getDecoder().decode(result.asString())));
        }
        throw new EvaluationException("Invalid argument provided for base64Decode function");
    }

    private ExpressionResult handleUrlEncodeFunction(ExpressionResult result) {
        if (result.isString()) {
            try {
                return new ExpressionResult(URLEncoder.encode(result.asString(), "UTF-8")
                        .replace("+", "%20").replace("*", "%2A"));
            } catch (UnsupportedEncodingException e) {
                throw new EvaluationException("unsupported encoding provided for urlEncode function");
            }
        }
        throw new EvaluationException("Invalid argument provided for urlEncode function");
    }

    private ExpressionResult handleUrlDecodeFunction(ExpressionResult result) {
        if (result.isString()) {
            try {
                return new ExpressionResult(URLDecoder.decode(result.asString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new EvaluationException("unsupported encoding provided for urlDecode function");
            }
        }
        throw new EvaluationException("Invalid argument provided for urlDecode function");
    }

    private ExpressionResult handleIntegerConversion(ExpressionResult result) {
        if (result.isInteger()) {
            return new ExpressionResult(result.asInt());
        }
        try {
            return new ExpressionResult(Integer.parseInt(result.asString()));
        } catch (NumberFormatException e) {
            throw new EvaluationException("Invalid argument provided for integer conversion");
        }
    }

    private ExpressionResult handleFloatConversion(ExpressionResult result) {
        if (result.isDouble()) {
            return new ExpressionResult(result.asDouble());
        }
        try {
            return new ExpressionResult(Double.parseDouble(result.asString()));
        } catch (NumberFormatException e) {
            throw new EvaluationException("Invalid argument provided for float conversion");
        }
    }

    private ExpressionResult handleBooleanConversion(ExpressionResult result) {
        if (result.isBoolean()) {
            return new ExpressionResult(result.asBoolean());
        }
        try {
            return new ExpressionResult(Boolean.parseBoolean(result.asString()));
        } catch (NumberFormatException e) {
            throw new EvaluationException("Invalid argument provided for boolean conversion");
        }
    }

    private ExpressionResult handleSubstringFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isInteger()) {
            if (source.asString().length() < argument1.asInt() || argument1.asInt() < 0) {
                throw new EvaluationException("Invalid index for subString: " + argument1.asInt()
                        + ", source string length: " + source.asString().length());
            }
            return new ExpressionResult(source.asString().substring(argument1.asInt()));
        }
        throw new EvaluationException("Invalid argument provided for subString function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleStartsWithFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            return new ExpressionResult(source.asString().startsWith(argument1.asString()));
        }
        throw new EvaluationException("Invalid argument provided for startsWith function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleEndsWithFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            return new ExpressionResult(source.asString().endsWith(argument1.asString()));
        }
        throw new EvaluationException("Invalid argument provided for endsWith function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleContainsFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            return new ExpressionResult(source.asString().contains(argument1.asString()));
        }
        throw new EvaluationException("Invalid argument provided for contains function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleSplitFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            String[] splits = source.asString().split(argument1.asString());
            JsonArray jsonArray = new JsonArray();
            for (String split : splits) {
                jsonArray.add(split);
            }
            return new ExpressionResult(jsonArray);
        }
        throw new EvaluationException("Invalid argument provided for split function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleIndexOfFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            return new ExpressionResult(source.asString().indexOf(argument1.asString()));
        }
        throw new EvaluationException("Invalid argument provided for indexOf function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleIndexOfFunction(ExpressionResult source, ExpressionResult argument1, ExpressionResult argument2) {
        if (source.isString() && argument1.isString() && argument2.isInteger()) {
            return new ExpressionResult(source.asString().indexOf(argument1.asString(), argument2.asInt()));
        }
        throw new EvaluationException("Invalid argument provided for indexOf function. source: " + source.asString()
                + ", argument1: " + argument1.asString() + ", argument2: " + argument2.asString());
    }

    private ExpressionResult handleFormatCurrentDateTimeFunction(ExpressionResult source, ExpressionResult argument1) {
        if (argument1.isString() && source.isNumeric()) {
            try {
                DateTimeFormatter formatObj = DateTimeFormatter.ofPattern(argument1.asString());
                LocalDateTime dateObj = LocalDateTime.ofInstant(Instant.ofEpochMilli(source.asLong()),
                        ZoneId.systemDefault());
                return new ExpressionResult(dateObj.format(formatObj));
            } catch (DateTimeException e) {
                throw new EvaluationException("Invalid date format provided for formatDateTime function. Format: "
                        + argument1.asString());
            }
        }
        throw new EvaluationException("Invalid argument provided for formatDateTime function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleFormatDateTimeFunctions(ExpressionResult source, ExpressionResult oldFormat,
                                                           ExpressionResult newFormat) {
        if (source.isString() && oldFormat.isString() && newFormat.isString()) {
            DateTimeFormatter oldFormatObj = null;
            try {
                oldFormatObj = DateTimeFormatter.ofPattern(oldFormat.asString());
                LocalDateTime dateObj = LocalDateTime.parse(source.asString(), oldFormatObj);
                DateTimeFormatter newFormatObj = DateTimeFormatter.ofPattern(newFormat.asString());
                return new ExpressionResult(dateObj.format(newFormatObj));
            } catch (DateTimeException | IllegalArgumentException e) {
                // try with date only
                if (oldFormatObj != null) {
                    try {
                        LocalDate dateObj = LocalDate.parse(source.asString(), oldFormatObj);
                        DateTimeFormatter newFormatObj = DateTimeFormatter.ofPattern(newFormat.asString());
                        return new ExpressionResult(dateObj.format(newFormatObj));
                    } catch (DateTimeException | IllegalArgumentException ex) {
                        // try with time only
                        try {
                            LocalTime dateObj = LocalTime.parse(source.asString(), oldFormatObj);
                            DateTimeFormatter newFormatObj = DateTimeFormatter.ofPattern(newFormat.asString());
                            return new ExpressionResult(dateObj.format(newFormatObj));
                        } catch (DateTimeException | IllegalArgumentException exc) {
                            throw new EvaluationException("Invalid date format provided for formatDateTime function. Format: "
                                    + oldFormat.asString());
                        }
                    }
                }
                throw new EvaluationException("Invalid date format provided for formatDateTime function. Format: "
                        + oldFormat.asString());
            }
        }
        throw new EvaluationException("Invalid argument provided for formatDateTime function. source: " + source.asString()
                + ", oldFormat: " + oldFormat.asString() + ", newFormat: " + newFormat.asString());
    }

    private ExpressionResult handleCharAtFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isInteger()) {
            try {

                return new ExpressionResult(String.valueOf(source.asString().charAt(argument1.asInt())));
            } catch (StringIndexOutOfBoundsException ex) {
                throw new EvaluationException("Invalid index provided for charAt function. source: " + source.asString()
                        + ", index: " + argument1.asInt());
            }
        }
        throw new EvaluationException("Invalid argument provided for charAt function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handlePowFunction(ExpressionResult source, ExpressionResult argument1) {
        if ((source.isDouble() || source.isInteger()) && (argument1.isDouble() || argument1.isInteger())) {
            return new ExpressionResult(Math.pow(source.asDouble().doubleValue(), argument1.asDouble().doubleValue()));
        }
        throw new EvaluationException("Invalid argument provided for pow function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleBase64EncodeFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            try {
                return new ExpressionResult(Base64.getEncoder().encodeToString(source.asString()
                        .getBytes(ExpressionUtils.getCharset(argument1.asString()))));
            } catch (UnsupportedCharsetException e) {
                throw new EvaluationException("Invalid charset provided for base64Encode function. Charset: "
                        + argument1.asString());
            } catch (UnsupportedEncodingException e) {
                throw new EvaluationException("Error encoding the string for base64Encode function");
            }
        }
        throw new EvaluationException("Invalid argument provided for base64Encode function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleUrlEncodeFunction(ExpressionResult source, ExpressionResult argument1) {
        if (source.isString() && argument1.isString()) {
            try {
                String result = URLEncoder.encode(source.asString(), ExpressionUtils.getCharset(argument1.asString()));
                if (argument1.asString().equals("UTF-8")) {
                    return new ExpressionResult(result.replace("+", "%20").replace("*", "%2A"));
                } else {
                    return new ExpressionResult(result);
                }
            } catch (UnsupportedCharsetException e) {
                throw new EvaluationException("Invalid charset provided for urlEncode function. Charset: "
                        + argument1.asString());
            } catch (UnsupportedEncodingException e) {
                throw new EvaluationException("Error encoding the string for urlEncode function");
            }
        }
        throw new EvaluationException("Invalid argument provided for urlEncode function. source: " + source.asString()
                + ", argument1: " + argument1.asString());
    }

    private ExpressionResult handleSubstringFunction(ExpressionResult source, ExpressionResult argument1, ExpressionResult argument2) {
        if (source.isString() && argument1.isInteger() && argument2.isInteger()) {
            if (argument2.asInt() < 0 || argument1.asInt() < 0 || argument1.asInt() >
                    argument2.asInt() || argument2.asInt() > source.asString().length()) {
                throw new EvaluationException("Invalid subString indices: start=" + argument1.asInt()
                        + ", end=" + argument2.asInt() + ", string length=" + source.asString().length());
            }
            return new ExpressionResult(source.asString().substring(argument1.asInt(), argument2.asInt()));
        }
        throw new EvaluationException("Invalid argument provided for subString function. source: " + source.asString()
                + ", argument1: " + argument1.asString() + ", argument2: " + argument2.asString());
    }

    private ExpressionResult handleReplaceFunction(ExpressionResult source, ExpressionResult argument1,
                                                   ExpressionResult argument2) {
        if (source.isString() && argument1.isString() && argument2.isString()) {
            return new ExpressionResult(source.asString().replace(argument1.asString(), argument2.asString()));
        }
        throw new EvaluationException("Invalid argument provided for replace function. source: " + source.asString()
                + ", argument1: " + argument1.asString() + ", argument2: " + argument2.asString());
    }

    private ExpressionResult handleRegistryAccess(EvaluationContext ctx, ExpressionResult regKey,
                                                  ExpressionResult propKey) {
        if (regKey.isString()) {
            if (propKey != null && propKey.isString()) {
                String prop = ctx.getRegistryResourceProperty(regKey.asString(), propKey.asString());
                if (prop != null) {
                    return new ExpressionResult(prop);
                }
                throw new EvaluationException("Could not find the property: " + propKey.asString()
                        + " in the registry resource: " + regKey.asString());
            } else {
                Object resource;
                try {
                    resource = ctx.getRegistryResource(regKey.asString());
                } catch (UnsupportedEncodingException e) {
                    throw new EvaluationException("Error retrieving the registry resource: " + regKey.asString());
                }
                if (resource != null) {
                    return new ExpressionResult(resource.toString());
                }
                throw new EvaluationException("Could not find the registry resource: " + regKey.asString());
            }
        }
        throw new EvaluationException("Invalid argument provided for registry function. regKey: " + regKey.asString()
                + ", propKey: " + propKey.asString());
    }

    private ExpressionResult handleExistsCheck(EvaluationContext context, ExpressionNode expression,
                                               boolean isObjectValue) {
        try {
            ExpressionResult result = expression.evaluate(context, isObjectValue);
            return result != null ? new ExpressionResult(true) : new ExpressionResult(false);
        } catch (EvaluationException e) {
            // this is the only method we are handling the exceptions
            return new ExpressionResult(false);
        }
    }

    private ExpressionResult convertToObject(ExpressionResult result) {
        if (result.isObject()) {
            return new ExpressionResult(result.asJsonObject());
        }
        throw new EvaluationException("Argument cannot be converted to a JSON object");
    }

    private ExpressionResult convertToArray(ExpressionResult result) {
        if (result.isArray()) {
            return new ExpressionResult(result.asJsonArray());
        }
        throw new EvaluationException("Argument cannot be converted to a JSON array");
    }

    private ExpressionResult evaluateXPATHExpression(EvaluationContext context, ExpressionResult expression,
                                                     boolean isObjectValue) {
        try {
            Object result = context.evaluateXpathExpression(expression.asString(), isObjectValue);
            if (isObjectValue) {
                return new ExpressionResult((List<?>) result);
            } else {
                return new ExpressionResult(result.toString());
            }
        } catch (JaxenException e) {
            throw new EvaluationException("Invalid XPATH expression : " + expression.asString());
        }
    }

    private ExpressionResult evaluateXPATHExpression(EvaluationContext context, ExpressionResult expression,
                                                     String variableName, boolean isObjectValue) {
        try {
            if (StringUtils.isEmpty(variableName)) {
                throw new EvaluationException("Invalid variable name provided for XPATH function");
            }
            Object result = context.evaluateXpathExpression("$var:"+ variableName + expression.asString(),
                    isObjectValue);
            if (isObjectValue) {
                return new ExpressionResult((List<?>) result);
            } else {
                return new ExpressionResult(result.toString());
            }
        } catch (JaxenException e) {
            throw new EvaluationException("Invalid XPATH expression : " + expression.asString());
        }
    }

    private ExpressionResult fetchSecretValue(EvaluationContext context, String expression) {
        try {
            String result = context.fetchSecretValue(expression);
            // if vault-lookup fails it just return the same expression as the result
            if (result.startsWith(ExpressionConstants.VAULT_LOOKUP)) {
                throw new EvaluationException("Error fetching secret value for alias: " + expression);
            }
            return new ExpressionResult(result);
        } catch (JaxenException e) {
            throw new EvaluationException("Error fetching secret value for alias: " + expression);
        }
    }
}
