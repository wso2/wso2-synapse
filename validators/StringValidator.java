package validators;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import utils.DataTypeConverter;

/**
 * This class validate strings against the given schema object.
 */
public class StringValidator {

    private static int minLength;
    private static int maxLength;

    public static final String MIN_LENGTH = "minLength";
    public static final String MAX_LENGTH = "maxLength";
    public static final String STR_PATTERN = "pattern";

    public static JsonPrimitive parseNominal(JsonObject inputObject, String value) throws ValidatorException, ParserException {
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
        if (inputObject.has(STR_PATTERN)) {
            String patternString = inputObject.get(STR_PATTERN).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!patternString.isEmpty()) {
                if (!value.matches(patternString)) {
                    throw new ValidatorException("String \"" + value + "\" violated the regex constraint " +
                            patternString);
                }
            }
        }
        return new JsonPrimitive(value);
    }
}
