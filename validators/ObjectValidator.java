package validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import utils.DataTypeConverter;

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
    private ObjectValidator(){}

    private final static String ADDITIONAL_PROPERTIES = "additionalProperties";
    private final static String REQUIRED = "required";
    private final static String MIN_PROPERTIES = "minProperties";
    private final static String MAX_PROPERTIES = "maxProperties";
    private final static String DEPENDENCIES = "dependencies";
    private final static String PATTERN_PROPERTIES = "patternProperties";
    private final static String REGEXP = "regexp";

    /**
     * This method will validate a given JSON input object according to
     * @param object
     * @param schema
     * @return
     * @throws ParserException
     * @throws ValidatorException
     */
    public static JsonObject validateObject(JsonObject object, JsonObject schema) throws ParserException, ValidatorException {
        Set<String> inputObjectKeyset = object.keySet();
        boolean additionalProperties = true;
        int minimumProperties = -1;
        int maximumProperties = -1;
        // check whether all the required elements are present.
        if (schema.has(REQUIRED)) {
            // keyword MUST be an array. Elements of this array, if any, MUST be strings, and MUST be unique.
            if (schema.get(REQUIRED).isJsonArray()) {
                JsonArray requiredArray = schema.getAsJsonArray(REQUIRED);
                for (JsonElement element : requiredArray) {
                    if (!object.has(element.getAsString())) {
                        throw new ValidatorException("Object does not have all the elements required in the schema");
                    }
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
        JsonObject schemaObject = (JsonObject) schema.get("properties");
        Set<Map.Entry<String, JsonElement>> entryInput = object.entrySet();

        // doing structural validation
        doStructuralValidation(maximumProperties, minimumProperties, entryInput);

        // validate children elements according to the schema only if properties are defied for each item.
        if (schemaObject != null) {
            // Calling validation for internal items.
            for (Map.Entry<String, JsonElement> entry : entryInput) {
                JsonObject schemaObj = schemaObject.getAsJsonObject(entry.getKey());
                String type = schemaObj.get(ValidatorConstants.TYPE_KEY).toString().replaceAll(ValidatorConstants
                        .REGEX, "");
                if (ValidatorConstants.BOOLEAN_KEYS.contains(type)) {
                    entry.setValue(BooleanValidator.validateBoolean(schemaObject, entry.getValue().getAsString()));
                } else if (ValidatorConstants.NOMINAL_KEYS.contains(type)) {
                    entry.setValue(StringValidator.validateNominal(schemaObject, entry.getValue().getAsString()));
                } else if (ValidatorConstants.NUMERIC_KEYS.contains(type)) {
                    entry.setValue(NumericValidator.validateNumeric(schemaObject, entry.getValue().getAsString()));
                }

            }
//                    (ARRAY_KEYS.contains(type)) {
//                parseArray(entry, schemaObj);
//            } else if (OBJECT_KEYS.contains(type)) {
//                parseObject((JsonObject) entry.getValue(), schemaObj);
//            }
        }

//        if (schema.has(PATTERN_PROPERTIES)) {
//            JsonObject patternsObject = schema.getAsJsonObject(PATTERN_PROPERTIES);
//            Set<Map.Entry<String, JsonElement>> patterns = patternsObject.entrySet();
//            for (Map.Entry<String, JsonElement> pattern : patterns) {
//                String regex = pattern.getKey();
//                JsonObject tempSchema = pattern.getValue().getAsJsonObject();
//                String type = tempSchema.get(ParserConstants.TYPE_KEY).getAsString().replaceAll(ParserConstants.REGEX, "");
//                ArrayList<String> matchingKeys = getMatchRegexAgainstStringSet(inputObjectKeyset,regex);
//                for (String key: matchingKeys) {
//                    if(ParserConstants.NUMERIC_KEYS.contains(type)) {
//                        NumericValidator.parseNumeric(tempSchema,object.get(key).toString());
//                    } else if(ParserConstants.NOMINAL_KEYS.contains(type)) {
//                        NominalValidator.parseNominal(tempSchema,object.get(key).toString());
//                    }
//                }
//            }
//        }
        return object;
    }

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
     * @param maximumProperties maximum number of properties allowed.
     * @param minimumProperties minimum number of properties allowed.
     * @param entryInput input object as a Map.
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

}
