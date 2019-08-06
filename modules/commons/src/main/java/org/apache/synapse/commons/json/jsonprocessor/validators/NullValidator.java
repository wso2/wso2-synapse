package validators;

import com.google.gson.JsonObject;
import exceptions.ValidatorException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class validate null values against a given schema.
 */
public class NullValidator {

    // use without instantiating.
    private NullValidator() {
    }

    // Logger instance
    private static Log logger = LogFactory.getLog(NullValidator.class.getName());

    /**
     * Validate a null input against schema.
     *
     * @param inputObject input schema.
     * @param value       null value.
     * @return JsonPrimitive of null.
     * @throws ValidatorException exception occurs in validation.
     */
    public static void validateNull(JsonObject inputObject, String value) throws ValidatorException {
        if (value != null && !(value.equals("") || value.equals("null") || value.equals("\"\"") || value.equals
                ("\"null\""))) {
            ValidatorException exception = new ValidatorException("Expected a null but found a value");
            logger.error("Received not null input" + value + " to be validated with : " + inputObject
                    .toString(), exception);
            throw exception;
        }
    }
}
