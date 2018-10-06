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
    public static final String ENUM = "enum";

    /**
     * Validate a given string against its schema.
     *
     * @param inputObject json schema as an object.
     * @param value       input string.
     * @return if valid return a JsonPrimitive created using input string.
     * @throws ValidatorException Didn't met validation criteria.
     * @throws ParserException    Exception occurs in data type conversions.
     */
    public static JsonPrimitive parseNominal(JsonObject inputObject, String value) throws ValidatorException,
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
        // TODO : Improve logic to return at once
        if (inputObject.has(ENUM)) {
            boolean matchFound = false;
            JsonArray enumElements = inputObject.getAsJsonArray(ENUM);
            if (enumElements.size() > 0) {
                for (JsonElement element : enumElements) {
                    // valid if value matches with any enum
                    if (value.equals(element.getAsString())) {
                        matchFound = true;
                    }
                }
                if (!matchFound) {
                    throw new ValidatorException("String \"" + value + "\" not contains any element from the enum");
                }
            }
        }
        return new JsonPrimitive(value);
    }
}
