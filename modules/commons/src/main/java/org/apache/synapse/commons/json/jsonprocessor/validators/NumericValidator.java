package validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import utils.DataTypeConverter;

/**
 * validate numeric instances according to the given schema.
 */
public class NumericValidator {

    // use without instantiating.
    private NumericValidator() {
    }

    // Logger instance
    private static Log logger = LogFactory.getLog(NumericValidator.class.getName());

    private static final String INTEGER_STRING = "integer";
    private static final String NUMBER_STRING = "number";

    private static final String MINIMUM_VALUE = "minimum";
    private static final String MAXIMUM_VALUE = "maximum";
    private static final String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    private static final String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    private static final String MULTIPLE_OF = "multipleOf";

    /**
     * Take JSON schema, number as a string input and validate.
     *
     * @param inputObject JSON schema.
     * @param value       numeric value
     * @return JsonPrimitive contains a number
     * @throws ParserException    Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    public static JsonPrimitive validateNumeric(JsonObject inputObject, String value) throws ParserException,
            ValidatorException {
        Double multipleOf;
        if (value == null) {
            throw new ValidatorException("Expected a number but found null");
        }
        //replacing enclosing quotes
        value = value.replaceAll(ValidatorConstants.QUOTE_REPLACE_REGEX, "");
        if (isNumeric(value)) {
            String type = null;
            if (inputObject.has(ValidatorConstants.TYPE_KEY)) {
                type = inputObject.get(ValidatorConstants.TYPE_KEY).getAsString().replaceAll(ValidatorConstants
                        .REGEX, "");
            }
            // handling multiples of condition
            Double doubleValue = DataTypeConverter.convertToDouble(value);
            if (inputObject.has(MULTIPLE_OF)) {
                multipleOf = DataTypeConverter.convertToDouble(inputObject.get(MULTIPLE_OF).getAsString().replaceAll
                        (ValidatorConstants.REGEX, ""));
                if (doubleValue % multipleOf != 0) {
                    ValidatorException exception = new ValidatorException("Number " + value + " is not a multiple of " +
                            "" + multipleOf);
                    logger.error("multipleOf constraint in " + inputObject.toString() + " is violated by the input " +
                            value, exception);
                    throw new ValidatorException(exception);
                }
            }
            // handling maximum and minimum
            if (inputObject.has(MINIMUM_VALUE)) {
                String minimumString = inputObject.get(MINIMUM_VALUE).getAsString().replaceAll(ValidatorConstants.REGEX,
                        "");
                if (!minimumString.isEmpty() && doubleValue < DataTypeConverter.convertToDouble(minimumString)) {
                    ValidatorException exception = new ValidatorException("Number " + value + " is less than the " +
                            "minimum allowed value");
                    logger.error("minimumValue constraint in " + inputObject.toString() + " is violated by the input " +
                            ": " + value, exception);
                    throw exception;
                }
            }
            if (inputObject.has(MAXIMUM_VALUE)) {
                String maximumString = inputObject.get(MAXIMUM_VALUE).getAsString().replaceAll(ValidatorConstants.REGEX,
                        "");
                if (!maximumString.isEmpty() && doubleValue > DataTypeConverter.convertToDouble(maximumString)) {
                    ValidatorException exception = new ValidatorException("Number " + value + " is greater than the " +
                            "maximum allowed value");
                    logger.error("maximumValue constraint in " + inputObject.toString() + " is violated by the input " +
                            ": " + value, exception);
                    throw exception;
                }
            }
            // handling exclusive maximum and minimum
            if (inputObject.has(EXCLUSIVE_MINIMUM)) {
                String minimumString = inputObject.get(EXCLUSIVE_MINIMUM).getAsString().replaceAll(ValidatorConstants
                                .REGEX,
                        "");
                if (!minimumString.isEmpty() && doubleValue <= DataTypeConverter.convertToDouble(minimumString)) {
                    ValidatorException exception = new ValidatorException("Number " + value + " is less than the " +
                            "minimum allowed value");
                    logger.error("exclusiveMinimum constraint in " + inputObject.toString() + " is violated by the " +
                            "input : " + value, exception);
                    throw exception;
                }
            }
            if (inputObject.has(EXCLUSIVE_MAXIMUM)) {
                String maximumString = inputObject.get(EXCLUSIVE_MAXIMUM).getAsString().replaceAll(ValidatorConstants
                                .REGEX,
                        "");
                if (!maximumString.isEmpty() && doubleValue >= DataTypeConverter.convertToDouble(maximumString)) {
                    ValidatorException exception = new ValidatorException("Number " + value + " is greater than the " +
                            "maximum allowed value");
                    logger.error("exclusiveMaximum constraint in " + inputObject.toString() + " is violated by the " +
                            "input : " + value, exception);
                    throw exception;
                }
            }
            // Enum validations
            if (inputObject.has(ValidatorConstants.ENUM)) {
                JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
                if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(doubleValue))) {
                    ValidatorException exception = new ValidatorException("Number \"" + value + "\" not contains any " +
                            "element from the enum");
                    logger.error("input " + value + " not contains any value from the enum in " + inputObject
                            .toString(), exception);
                    throw exception;
                }
            }
            //Const validation
            if (inputObject.has(ValidatorConstants.CONST) && !doubleValue.equals(inputObject.getAsJsonPrimitive
                    (ValidatorConstants.CONST).getAsDouble())) {
                ValidatorException exception = new ValidatorException("Number \"" + value + "\" is not equal to the " +
                        "const value");
                logger.error("input " + value + " not contains the const defined in " + inputObject
                        .toString(), exception);
                throw exception;
            }
            // convert to integer of give value is a float
            if (type != null && type.equals(INTEGER_STRING)) {
                return new JsonPrimitive(DataTypeConverter.convertToInt(value));
            } else {
                // this condition address both type number and empty json schemas
                return new JsonPrimitive(doubleValue);
            }
        }
        ParserException exception = new ParserException("\"" + value + "\"" + " is not a number");
        logger.error("A number expected in the schema " + inputObject.toString() + " but received " + value, exception);
        throw exception;
    }

    /**
     * Check whether a given number is numeric. (alternative :- commons-lang3 isCreatable())
     * @param str input string.
     * @return number or not.
     */
    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
