package validators;

import com.google.gson.*;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import utils.DataTypeConverter;

/**
 * This class validate strings against the given schema object.
 */
public class StringValidator {

    // Use without instantiating.
    private StringValidator() {
    }

    private static int minLength;
    private static int maxLength;

    public static final String MIN_LENGTH = "minLength";
    public static final String MAX_LENGTH = "maxLength";
    public static final String STR_PATTERN = "pattern";

    /**
     * Validate a given string against its schema.
     *
     * @param inputObject json schema as an object.
     * @param value       input string.
     * @return if valid return a JsonPrimitive created using input string.
     * @throws ValidatorException Didn't met validation criteria.
     * @throws ParserException    Exception occurs in data type conversions.
     */
    public static JsonPrimitive validateNominal(JsonObject inputObject, String value) throws ValidatorException,
            ParserException {
        if (value == null) {
            throw new ValidatorException("Expected string but found null");
        }
        // String length validations
        if (inputObject.has(MAX_LENGTH)) {
            String maxLengthString = inputObject.get(MAX_LENGTH).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!maxLengthString.isEmpty()) {
                maxLength = DataTypeConverter.convertToInt(maxLengthString);
                if (value.length() > maxLength) {
                    throw new ValidatorException("String \"" + value + "\" violated the max length constraint");
                }
            }
        }
        if (inputObject.has(MIN_LENGTH)) {
            String minLengthString = inputObject.get(MIN_LENGTH).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!minLengthString.isEmpty()) {
                minLength = DataTypeConverter.convertToInt(minLengthString);
                if (value.length() < minLength) {
                    throw new ValidatorException("String \"" + value + "\" violated the min length constraint");
                }
            }
        }
        // String pattern validations
        if (inputObject.has(STR_PATTERN)) {
            String patternString = inputObject.get(STR_PATTERN).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!patternString.isEmpty() && !value.matches(patternString)) {
                throw new ValidatorException("String \"" + value + "\" violated the regex constraint " + patternString);
            }
        }
        // Enum validations
        if (inputObject.has(ValidatorConstants.ENUM)) {
            JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
            if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(value))) {
                throw new ValidatorException("String \"" + value + "\" not contains any element from the enum");
            }
        }
        //Const validation
        if (inputObject.has(ValidatorConstants.CONST) && !value.equals(inputObject.getAsJsonPrimitive
                (ValidatorConstants.CONST).getAsString())) {
            throw new ValidatorException("String \"" + value + "\" is not equal to the const value");
        }
        return new JsonPrimitive(value);
    }
}
