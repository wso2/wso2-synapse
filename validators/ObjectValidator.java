package validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import utils.DataTypeConverter;
import utils.GSONDataTypeConverter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class will validate json objects according to the schema.
 */
public class ObjectValidator {

    // Use without instantiating.
    private ObjectValidator() {
    }

    private final static String ADDITIONAL_PROPERTIES = "additionalProperties";
    private final static String MAX_PROPERTIES = "maxProperties";
    private final static String MIN_PROPERTIES = "minProperties";
    private final static String PATTERN_PROPERTIES = "patternProperties";
    private final static String REQUIRED = "required";

    /**
     * This method will validate a given JSON input object according to
     *
     * @param object
     * @param schema
     * @return
     * @throws ParserException    Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    public static JsonObject validateObject(JsonObject object, JsonObject schema) throws ParserException,
            ValidatorException {
        int minimumProperties = -1;
        int maximumProperties = -1;
        // check whether all the required elements are present.
        // keyword MUST be an array. Elements of this array, if any, MUST be strings, and MUST be unique.
        if (schema.has(REQUIRED) && schema.get(REQUIRED).isJsonArray()) {
            JsonArray requiredArray = schema.getAsJsonArray(REQUIRED);
            for (JsonElement element : requiredArray) {
                if (!object.has(element.getAsString())) {
                    throw new ValidatorException("Object does not have all the elements required in the schema");
                }
            }
        }
        if (schema.has(MIN_PROPERTIES)) {
            String minPropertiesString = schema.get(MIN_PROPERTIES).getAsString().replaceAll
                    (ValidatorConstants.REGEX, "");
            if (!minPropertiesString.isEmpty()) {
                minimumProperties = DataTypeConverter.convertToInt(minPropertiesString);
            }
        }
        if (schema.has(MAX_PROPERTIES)) {
            String maxPropertiesString = schema.get(MAX_PROPERTIES).getAsString().replaceAll
                    (ValidatorConstants.REGEX, "");
            if (!maxPropertiesString.isEmpty()) {
                maximumProperties = DataTypeConverter.convertToInt(maxPropertiesString);
            }
        }
        JsonObject schemaObject = null;
        if (schema.has("properties")) {
            schemaObject = (JsonObject) schema.get("properties");
        }
        Set<Map.Entry<String, JsonElement>> entryInput = object.entrySet();

        // doing structural validation
        doStructuralValidation(maximumProperties, minimumProperties, entryInput);

        ArrayList<String> inputProperties = new ArrayList();
        ArrayList<String> patternProperties = new ArrayList();
        ArrayList<String> schemaProperties = new ArrayList();

        if (schemaObject != null && !schemaObject.entrySet().isEmpty()) {
            for (Map.Entry<String, JsonElement> entry : schemaObject.entrySet()) {
                if (object.has(entry.getKey())) {
                    schemaProperties.add(entry.getKey());
                }
            }
        }

        // validate children elements according to the schema only if properties are defined for each item.
        processSchemaProperties(schemaObject, entryInput, inputProperties);

        // handling pattern properties
        if (schema.has(PATTERN_PROPERTIES)) {
            processPatternProperties(object, schema, patternProperties);
        }

        // handling additionalProperties
        processAdditionalProperties(object, schema, inputProperties, patternProperties, schemaProperties);

        return object;
    }

    /**
     * This method will process a given JSON object according to "properties" in respective JSON schema.
     *
     * @param schemaObject    JSON schema.
     * @param entryInput      JSON input as map.
     * @param inputProperties array which should be updated with matching properties in schema.
     * @throws ParserException    Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    private static void processSchemaProperties(JsonObject schemaObject, Set<Map.Entry<String, JsonElement>>
            entryInput, ArrayList<String> inputProperties) throws ValidatorException, ParserException {
        if (schemaObject != null) {
            // Calling validation for internal items.
            for (Map.Entry<String, JsonElement> entry : entryInput) {
                String keyValue = entry.getKey();
                inputProperties.add(keyValue);
                if (schemaObject.has(keyValue) && schemaObject.get(keyValue).isJsonObject()) {
                    JsonObject schemaObj = schemaObject.getAsJsonObject(keyValue);
                    if (schemaObj.has(ValidatorConstants.TYPE_KEY)) {
                        String type = schemaObj.get(ValidatorConstants.TYPE_KEY).toString().replaceAll
                                (ValidatorConstants.REGEX, "");
                        validateAndUpdateEntriesMap(schemaObject, entry, type);
                    }
                }
            }
        }
    }

    /**
     * This method will validate and update the entries of input object map.
     *
     * @param schemaObject JSON schema.
     * @param entry        entry from the map.
     * @param type         data type.
     * @throws ParserException    Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    private static void validateAndUpdateEntriesMap(JsonObject schemaObject, Map.Entry<String, JsonElement> entry,
                                                    String type) throws ValidatorException, ParserException {
        if (schemaObject.has(entry.getKey()) && schemaObject.get(entry.getKey()).isJsonObject()) {
            JsonObject schema = schemaObject.get(entry.getKey()).getAsJsonObject();
            if (ValidatorConstants.BOOLEAN_KEYS.contains(type)) {
                entry.setValue(BooleanValidator.validateBoolean(schema, entry.getValue().getAsString()));
            } else if (ValidatorConstants.NOMINAL_KEYS.contains(type)) {
                entry.setValue(StringValidator.validateNominal(schema, entry.getValue().getAsString()));
            } else if (ValidatorConstants.NUMERIC_KEYS.contains(type)) {
                entry.setValue(NumericValidator.validateNumeric(schema, entry.getValue().getAsString()));
            } else if (ValidatorConstants.ARRAY_KEYS.contains(type)) {
                entry.setValue(ArrayValidator.validateArray(GSONDataTypeConverter.getMapFromString(entry.getValue()
                        .getAsString()), schema));
            } else if (ValidatorConstants.OBJECT_KEYS.contains(type)) {
                entry.setValue(ObjectValidator.validateObject(entry.getValue().getAsJsonObject(), schema));
            }
        }
    }

    /**
     * This method will handle the additional properties constraint.
     *
     * @param object            input JSON object.
     * @param schema            JSON schema.
     * @param inputProperties   array which contains all keys of input object.
     * @param patternProperties array which contains all matching keys with pattern properties.
     * @param schemaProperties  array which contains all schema keys.
     * @throws ParserException    Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    private static void processAdditionalProperties(JsonObject object, JsonObject schema, ArrayList<String>
            inputProperties, ArrayList<String> patternProperties, ArrayList<String> schemaProperties)
            throws ParserException, ValidatorException {
        if (schema.has(ADDITIONAL_PROPERTIES)) {
            inputProperties.removeAll(schemaProperties);
            inputProperties.removeAll(patternProperties);
            if (schema.get(ADDITIONAL_PROPERTIES).isJsonPrimitive()) {
                boolean allowAdditional = DataTypeConverter.convertToBoolean(schema.get(ADDITIONAL_PROPERTIES)
                        .getAsString());
                if (!allowAdditional && !inputProperties.isEmpty()) {
                    throw new ValidatorException("Object has additional properties than allowed in schema");
                }
            } else if (schema.get(ADDITIONAL_PROPERTIES).isJsonObject()) {
                JsonObject additionalSchema = schema.get(ADDITIONAL_PROPERTIES).getAsJsonObject();
                validateMultipleObjectsUsingOneSchema(inputProperties, object, additionalSchema);
            }
        }
    }

    /**
     * This  method will process input objects matching with "patternProperties" regular expression.
     *
     * @param object            input JSON object.
     * @param schema            input schema.
     * @param patternProperties arrayList which should be updated with matching keys of patternProperty.
     * @throws ParserException    Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    private static void processPatternProperties(JsonObject object, JsonObject schema, ArrayList<String>
            patternProperties) throws ParserException,
            ValidatorException {
        Set<String> inputObjectKeyset = object.keySet();
        JsonObject patternsObject = schema.getAsJsonObject(PATTERN_PROPERTIES);
        Set<Map.Entry<String, JsonElement>> patterns = patternsObject.entrySet();
        for (Map.Entry<String, JsonElement> pattern : patterns) {
            String regex = pattern.getKey();
            JsonObject tempSchema = pattern.getValue().getAsJsonObject();
            String type = tempSchema.get(ValidatorConstants.TYPE_KEY).getAsString().replaceAll(ValidatorConstants
                    .REGEX, "");
            // get the list of keys matched the regular expression
            ArrayList<String> matchingKeys = getMatchRegexAgainstStringSet(inputObjectKeyset, regex);
            for (String key : matchingKeys) {
                // updating patternProperties array for later use in additional properties.
                if (!patternProperties.contains(key)) {
                    patternProperties.add(key);
                }
                parseAndReplaceValues(object, tempSchema, type, key);
            }
        }
    }

    /**
     * Get matching set of keys inside object, for a given regular expression.
     *
     * @param keyset set of keys inside object.
     * @param regex  regular expression.
     * @return array of matching keys.
     */
    private static ArrayList<String> getMatchRegexAgainstStringSet(Set<String> keyset, String regex) {
        ArrayList<String> matchingKeys = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        for (String item : keyset) {
            matcher = pattern.matcher(item);
            if (matcher.find()) {
                matchingKeys.add(item);
            }
        }
        return matchingKeys;
    }

    /**
     * Doing structural validations for a given JSON object according to schema.
     *
     * @param maximumProperties maximum number of properties allowed.
     * @param minimumProperties minimum number of properties allowed.
     * @param entryInput        input object as a Map.
     * @throws ValidatorException validation exception occurs.
     */
    private static void doStructuralValidation(int maximumProperties, int minimumProperties, Set<Map.Entry<String,
            JsonElement>> entryInput) throws ValidatorException {
        int numOfProperties = entryInput.size();

        if (minimumProperties != -1 && numOfProperties < minimumProperties) {
            throw new ValidatorException("Object violates the minimum number of properties constraint");
        }
        if (maximumProperties != -1 && numOfProperties > maximumProperties) {
            throw new ValidatorException("Object violates the maximum number of properties constraint");
        }
    }

    /**
     * Given a single schema object and array of keys, this method will validate all input object keys
     * according to the schema.
     *
     * @param keysArray array of keys.
     * @param input     input object.
     * @param schema    schema object.
     */
    private static void validateMultipleObjectsUsingOneSchema(ArrayList<String> keysArray, JsonObject input,
                                                              JsonObject schema) throws ValidatorException,
            ParserException {
        if (!schema.entrySet().isEmpty() && !keysArray.isEmpty()) {
            String type = schema.get(ValidatorConstants.TYPE_KEY).toString().replaceAll
                    (ValidatorConstants.REGEX, "");
            for (String key : keysArray) {
                parseAndReplaceValues(input, schema, type, key);
            }
        }
    }

    /**
     * This method will parse and replace specific object defined by the key.
     *
     * @param input  input object.
     * @param schema JSON schema.
     * @param type   type of the object.
     * @param key    key of the specific object/ element.
     * @throws ParserException    Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    private static void parseAndReplaceValues(JsonObject input, JsonObject schema, String type, String key) throws
            ParserException, ValidatorException {
        JsonElement result = null;
        if (ValidatorConstants.NUMERIC_KEYS.contains(type)) {
            result = NumericValidator.validateNumeric(schema, input.get(key).toString());
        } else if (ValidatorConstants.NOMINAL_KEYS.contains(type)) {
            result = StringValidator.validateNominal(schema, input.get(key).toString());
        } else if (ValidatorConstants.BOOLEAN_KEYS.contains(type)) {
            result = BooleanValidator.validateBoolean(schema, input.get(key).toString());
        } else if (ValidatorConstants.ARRAY_KEYS.contains(type)) {
            result = ArrayValidator.validateArray(GSONDataTypeConverter.getMapFromString(
                    input.get(key).toString()), schema);
        } else if (ValidatorConstants.OBJECT_KEYS.contains(type)) {
            result = ObjectValidator.validateObject(input.get(key).getAsJsonObject(), schema);
        }
        input.remove(key);
        input.add(key, result);
    }
}
