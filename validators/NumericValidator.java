package validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import org.apache.commons.lang3.math.NumberUtils;
import utils.DataTypeConverter;

/**
 * validate numeric instances according to the given schema.
 */
public class NumericValidator {

    // use without instantiating.
    private NumericValidator() {
    }

    private static Double multipleOf;
    private static final String INTEGER_STRING = "integer";
    private static final String NUMBER_STRING = "number";

    private static final String MINIMUM_VALUE = "minimum";
    private static final String MAXIMUM_VALUE = "maximum";
    private static final String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    private static final String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    private static final String MULTIPLE_OF = "multipleOf";

    /**
     * Take JSON schema, number as a string input and validate.
     * @param inputObject JSON schema.
     * @param value numeric value
     * @return JsonPrimitive contains a number
     * @throws ParserException Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    public static JsonPrimitive validateNumeric(JsonObject inputObject, String value) throws ParserException,
            ValidatorException {
        if (value == null) {
            throw new ValidatorException("Expected a number but found null");
        }
        if (NumberUtils.isCreatable(value)) {
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
                    throw new ValidatorException("Number " + value + " is not a multiple of " + multipleOf);
                }
            }
            // handling maximum and minimum
            if (inputObject.has(MINIMUM_VALUE)) {
                String minimumString = inputObject.get(MINIMUM_VALUE).getAsString().replaceAll(ValidatorConstants.REGEX,
                        "");
                if (!minimumString.isEmpty() && doubleValue < DataTypeConverter.convertToDouble(minimumString)) {
                    throw new ValidatorException("Number " + value + " is less than the minimum allowed value");

                }
            }
            if (inputObject.has(MAXIMUM_VALUE)) {
                String maximumString = inputObject.get(MAXIMUM_VALUE).getAsString().replaceAll(ValidatorConstants.REGEX,
                        "");
                if (!maximumString.isEmpty() && doubleValue > DataTypeConverter.convertToDouble(maximumString)) {
                    throw new ValidatorException("Number " + value + " is greater than the maximum allowed value");

                }
            }
            // handling exclusive maximum and minimum
            if (inputObject.has(EXCLUSIVE_MINIMUM)) {
                String minimumString = inputObject.get(EXCLUSIVE_MINIMUM).getAsString().replaceAll(ValidatorConstants.REGEX,
                        "");
                if (!minimumString.isEmpty() && doubleValue <= DataTypeConverter.convertToDouble(minimumString)) {
                    throw new ValidatorException("Number " + value + " is less than the minimum allowed value");

                }
            }
            if (inputObject.has(EXCLUSIVE_MAXIMUM)) {
                String maximumString = inputObject.get(EXCLUSIVE_MAXIMUM).getAsString().replaceAll(ValidatorConstants.REGEX,
                        "");
                if (!maximumString.isEmpty() && doubleValue >= DataTypeConverter.convertToDouble(maximumString)) {
                    throw new ValidatorException("Number " + value + " is greater than the maximum allowed value");

                }
            }
            // Enum validations
            if (inputObject.has(ValidatorConstants.ENUM)) {
                JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
                if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(doubleValue))) {
                    throw new ValidatorException("Number \"" + value + "\" not contains any element from the enum");
                }
            }
            //Const validation
            if (inputObject.has(ValidatorConstants.CONST) && !doubleValue.equals(inputObject.getAsJsonPrimitive
                    (ValidatorConstants.CONST).getAsDouble())) {
                throw new ValidatorException("Number \"" + value + "\" is not equal to the const value");
            }
            // convert to integer of give value is a float
            if (type != null && type.equals(INTEGER_STRING)) {
                return new JsonPrimitive(DataTypeConverter.convertToInt(value));
            } else {
                // this condition address both type number and empty json schemas
                return new JsonPrimitive(doubleValue);
            }
        }
        throw new ParserException("\"" + value + "\"" + " is not a number");
    }
}
