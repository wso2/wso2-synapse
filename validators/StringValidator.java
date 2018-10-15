package validators;

import com.google.gson.*;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import utils.DataTypeConverter;

/**
 * This class validate strings against the given schema object.
 */
public class StringValidator {

    // Use without instantiating.
    private StringValidator() {
    }

    // Logger instance
    private static Log logger = LogFactory.getLog(StringValidator.class.getName());

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
            ValidatorException exception = new ValidatorException("Expected string but found null");
            logger.error("Expected a string in the schema " + inputObject.toString() + " but found null input",
                    exception);
            throw exception;
        }
        // String length validations
        if (inputObject.has(MAX_LENGTH)) {
            String maxLengthString = inputObject.get(MAX_LENGTH).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!maxLengthString.isEmpty()) {
                maxLength = DataTypeConverter.convertToInt(maxLengthString);
                if (value.length() > maxLength) {
                    ValidatorException exception = new ValidatorException("String \"" + value + "\" violated the max " +
                            "length constraint");
                    logger.error("input string : " + value + " violated the maxLength constraint defined in : " +
                            inputObject.toString(), exception);
                    throw exception;
                }
            }
        }
        if (inputObject.has(MIN_LENGTH)) {
            String minLengthString = inputObject.get(MIN_LENGTH).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!minLengthString.isEmpty()) {
                minLength = DataTypeConverter.convertToInt(minLengthString);
                if (value.length() < minLength) {
                    ValidatorException exception = new ValidatorException("String \"" + value + "\" violated the min " +
                            "length constraint");
                    logger.error("input string : " + value + " violated the minLength constraint defined in : " +
                            inputObject.toString(), exception);
                    throw exception;
                }
            }
        }
        // String pattern validations
        if (inputObject.has(STR_PATTERN)) {
            String patternString = inputObject.get(STR_PATTERN).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!patternString.isEmpty() && !value.matches(patternString)) {
                ValidatorException exception = new ValidatorException("String \"" + value + "\" violated the regex " +
                        "constraint " + patternString);
                logger.error("input string : " + value + " not matching with any regex defined in : " + inputObject
                        .toString(), exception);
                throw exception;
            }
        }
        // Enum validations
        if (inputObject.has(ValidatorConstants.ENUM)) {
            JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
            if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(value))) {
                ValidatorException exception = new ValidatorException("String \"" + value + "\" not contains any " +
                        "element from the enum");
                logger.error("input string : " + value + " not contains any value defined in the enum of : " +
                        inputObject.toString(), exception);
                throw exception;
            }
        }
        //Const validation
        if (inputObject.has(ValidatorConstants.CONST) && !value.equals(inputObject.getAsJsonPrimitive
                (ValidatorConstants.CONST).getAsString())) {
            ValidatorException exception = new ValidatorException("String \"" + value + "\" is not equal to the const" +
                    " value");
            logger.error("input string : " + value + " not contains the const value defined in : " + inputObject
                    .toString(), exception);
            throw exception;
        }
        return new JsonPrimitive(value);
    }
}
