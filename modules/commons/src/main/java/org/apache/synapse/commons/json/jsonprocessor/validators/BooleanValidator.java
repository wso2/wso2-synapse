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
 * This class validate boolean values against a given schema.
 */
public class BooleanValidator {

    // Use without instantiating.
    private BooleanValidator() {
    }

    private static Log logger = LogFactory.getLog(BooleanValidator.class.getName());

    /**
     * Validate a boolean string according to a given schema.
     *
     * @param inputObject json schema.
     * @param value       boolean string.
     * @return JsonPrimitive contains the parsed boolean.
     * @throws ValidatorException exception occurs in validation.
     * @throws ParserException    exception occurs when parsing.
     */
    public static JsonPrimitive validateBoolean(JsonObject inputObject, String value) throws ValidatorException,
            ParserException {
        if (value == null) {
            ValidatorException exception = new ValidatorException("Expected a boolean but found null");
            logger.error("Received null input to be validated with : " + inputObject.toString(), exception);
            throw exception;
        }
        Boolean parsedValue = DataTypeConverter.convertToBoolean(value);
        // Enum validations
        if (inputObject.has(ValidatorConstants.ENUM)) {
            JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
            if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(parsedValue))) {
                ValidatorException exception = new ValidatorException("input \"" + value + "\" not contains any " +
                        "element from the enum");
                logger.error("Input : " + value + " not contains any value from the enum : " +
                        enumElements.toString(), exception);
                throw exception;
            }
        }
        //Const validation
        if (inputObject.has(ValidatorConstants.CONST) && !parsedValue.equals(inputObject.getAsJsonPrimitive
                (ValidatorConstants.CONST).getAsBoolean())) {
            ValidatorException exception = new ValidatorException("String \"" + value + "\" is not equal to the const" +
                    " value");
            logger.error("Input " + value + " not contains the value from const", exception);
            throw exception;
        }
        return new JsonPrimitive(parsedValue);
    }
}
