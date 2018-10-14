package parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import utils.GSONDataTypeConverter;
import validators.*;

/**
 * This class will parse a given JSON input according to a given schema.
 * Supported inout formats - String and Gson JsonObject
 */
public class JavaJsonParser {

    // Use without instantiating
    private JavaJsonParser() {
    }

    // JSON parser instance
    private static JsonParser parser = new JsonParser();

    /**
     * This method parse a given JSON string according to the given schema. Both as string.
     *
     * @param inputString input String.
     * @param inputSchema input Schema.
     * @return corrected String.
     * @throws ValidatorException Exception occurs in validation process.
     * @throws ParserException    Exception occurs in data type parsing.
     */
    public static String parseJson(String inputString, String inputSchema) throws ValidatorException, ParserException {
        JsonObject schemaObject = null;
        JsonElement schema = parser.parse(inputSchema);
        if (schema.isJsonObject()) {
            schemaObject = schema.getAsJsonObject();
        } else if (schema.isJsonPrimitive()) {
            // if schema is primitive it should be a boolean
            boolean valid = schema.getAsBoolean();
            if (valid) {
                return inputString;
            } else if (!valid) throw new ValidatorException("JSON schema is not valid for all elements");
            else throw new ValidatorException("Unexpected JSON schema");
        } else {
            throw new ValidatorException("Unexpected JSON schema");
        }
        return parseJson(inputString, schemaObject);
    }

    /**
     * This method will parse a given JSON string according to the given schema. Schema as an Object.
     * Can use this method when using caching.
     *
     * @param inputString input JSON string.
     * @param schema      already parsed JSON schema.
     * @return corrected JSON string.
     * @throws ValidatorException Exception occurs in validation process.
     * @throws ParserException    Exception occurs in data type parsing.
     */
    public static String parseJson(String inputString, Object schema) throws ValidatorException, ParserException {
        JsonElement input = parser.parse(inputString);
        JsonElement result = null;
        JsonObject schemaObject = (JsonObject) schema;
        String type = schemaObject.get(ValidatorConstants.TYPE_KEY).toString().replaceAll(ValidatorConstants
                .REGEX, "");
        if (ValidatorConstants.BOOLEAN_KEYS.contains(type)) {
            result = BooleanValidator.validateBoolean(schemaObject, input.toString());
        } else if (ValidatorConstants.NOMINAL_KEYS.contains(type)) {
            result = StringValidator.validateNominal(schemaObject, input.toString());
        } else if (ValidatorConstants.NUMERIC_KEYS.contains(type)) {
            result = NumericValidator.validateNumeric(schemaObject, input.toString());
        } else if (ValidatorConstants.ARRAY_KEYS.contains(type)) {
            result = ArrayValidator.validateArray(GSONDataTypeConverter.getMapFromString(input.toString()),
                    schemaObject);
        } else if (ValidatorConstants.NULL_KEYS.contains(type)) {
            // todo add null implementation
        } else if (ValidatorConstants.OBJECT_KEYS.contains(type)) {
            if (input.isJsonObject()) {
                result = ObjectValidator.validateObject(input.getAsJsonObject(), schemaObject);
            } else {
                throw new ValidatorException("Expected a json object input");
            }
        }
        return result.toString();
    }
}
