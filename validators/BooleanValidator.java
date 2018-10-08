package validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import utils.DataTypeConverter;

/**
 * This class validate boolean values against a given schema.
 */

public class BooleanValidator {

    // Use without instantiating.
    private BooleanValidator() {
    }

    /**
     * Validate a boolean string according to a given schema.
     * @param inputObject json schema.
     * @param value boolean string.
     * @return JsonPrimitive contains the parsed boolean.
     * @throws ValidatorException exception occurs in validation.
     * @throws ParserException exception occurs when parsing.
     */
    public static JsonPrimitive validateBoolean(JsonObject inputObject, String value) throws ValidatorException,
            ParserException {
        if (value == null) {
            throw new ValidatorException("Expected a boolean but found null");
        }
        Boolean parsedValue = DataTypeConverter.convertToBoolean(value);
        // Enum validations
        if (inputObject.has(ValidatorConstants.ENUM)) {
            JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
            if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(parsedValue))) {
                throw new ValidatorException("String \"" + value + "\" not contains any element from the enum");
            }
        }
        //Const validation
        if (inputObject.has(ValidatorConstants.CONST) && !parsedValue.equals(inputObject.getAsJsonPrimitive
                (ValidatorConstants.CONST).getAsBoolean())) {
            throw new ValidatorException("String \"" + value + "\" is not equal to the const value");
        }
        return new JsonPrimitive(parsedValue);
    }
}
