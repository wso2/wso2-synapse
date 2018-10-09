package validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import contants.ValidatorConstants;
import exceptions.ParserException;
import exceptions.ValidatorException;
import utils.DataTypeConverter;
import utils.GSONDataTypeConverter;

import java.util.Map;

/**
 * This class will validate json arrays according to the schema.
 * Structural validations only.
 */
public class ArrayValidator {

    // Use without instantiation
    private ArrayValidator() {
    }

    private static final String MIN_ITEMS = "minItems";
    private static final String MAX_ITEMS = "maxItems";
    private static final String ITEMS = "items";
    private static final String UNIQUE_ITEMS = "uniqueItems";

    private static int minItems;
    private static int maxItems;
    private static String arrayItems;
    private static boolean uniqueItems;
    private static int currentCount;

    /**
     * This method will validates an input array according to a given schema.
     *
     * @param input  input array as a Map.
     * @param schema JSON schema as an object.
     * @return Validated JSON array.
     * @throws ValidatorException Exception occurs in validation process.
     * @throws ParserException    Exception occurs in data type parsing.
     */
    public static JsonArray validateArray(Map.Entry<String, JsonElement> input, JsonObject
            schema) throws ValidatorException, ParserException {
        minItems = -1;
        maxItems = -1;
        currentCount = 0;
        if (schema.has(UNIQUE_ITEMS)) {
            String uniqueItemsString = schema.get(UNIQUE_ITEMS).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!uniqueItemsString.isEmpty()) {
                uniqueItems = DataTypeConverter.convertToBoolean(uniqueItemsString);
            }
        }
        if (schema.has(MIN_ITEMS)) {
            String minItemsString = schema.get(MIN_ITEMS).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!minItemsString.isEmpty()) {
                minItems = DataTypeConverter.convertToInt(minItemsString);
                if (minItems < 0) {
                    throw new ValidatorException("Invalid minItems constraint in the schema");
                }
            }
        }
        if (schema.has(MAX_ITEMS)) {
            String maxItemsString = schema.get(MAX_ITEMS).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!maxItemsString.isEmpty()) {
                maxItems = DataTypeConverter.convertToInt(maxItemsString);
                if (maxItems < 0) {
                    throw new ValidatorException("Invalid maxItems constraint in the schema");
                }
            }
        }
        // Convert the input to an array. If possible, do the single element array correction. Ex 45 -> [45]
        // else throw an error
        JsonArray inputArray = null;
        if (input.getValue().isJsonArray()) {
            inputArray = input.getValue().getAsJsonArray();
        } else if (input.getValue().isJsonPrimitive()) {
            inputArray = SingleElementArrayCorrection(input.getValue().getAsString());
        } else {
            throw new ValidatorException("Expected array but found " + input.getValue().getAsString());
        }

        // processing the items property in JSON array.
        if (schema.has(ITEMS)) {
            // Items must be either a valid JSON Schema or an array of valid JSON Schemas.
            if (schema.get(ITEMS).isJsonArray()) {
                // Items - valid JSON array.
                JsonArray schemaArray = schema.get(ITEMS).getAsJsonArray();
                ProcessSchemaWithItemsArray(inputArray, schemaArray);
                // take all instances from json array and iteratively validate them.
            } else if (schema.get(ITEMS).isJsonObject()) {
                // Item is a JSON object
                JsonObject schemaObject = schema.get(ITEMS).getAsJsonObject();
                ProcessSchemaWithOneItem(inputArray, schemaObject);
            }
        }


        /*if (input.getValue().isJsonPrimitive() || input.getValue().isJsonNull()) {
            if (minItems != -1 && minItems > 1) {
                throw new ValidatorException("Array violated the minimum no of items constraint");
            }
        } else {
            JsonArray arr;
            if (input.getValue().isJsonArray()) {
                arr = (JsonArray) input.getValue();
                int arrSize = arr.size();
                if (minItems != -1 && minItems > arrSize) {
                    throw new ValidatorException("Array violated the minimum no of items constraint");
                } else if (maxItems != -1 && maxItems < arrSize) {
                    throw new ValidatorException("Array violated the maximum no of items constraint");
                }
                if(uniqueItems) {
                    JsonArray tempArray = new JsonArray();
                    tempArray.add(arr.get(0));
                    if(arrSize>1) {
                        for(int i=1;i<arrSize;i++) {
                            if(tempArray.contains(arr.get(i))) {
                                throw new ValidatorException("Array has duplicate elements");
                            } tempArray.add(arr.get(i));
                        }
                    }
                }
            }
        }*/
        return inputArray;
    }

    /**
     * JSON structure correction. Convert single elements to arrays.
     *
     * @param element String payload.
     * @return Json array.
     */
    private static JsonArray SingleElementArrayCorrection(String element) {
        JsonElement jsonElement = new JsonPrimitive(element);
        JsonArray array = new JsonArray();
        array.add(jsonElement);
        return array;
    }

    /**
     * Validate JSON array when both items and schema are arrays.
     * Ex:- {"type":"array", "items":[{"type": "boolean"},{"type": "numeric"}]}
     *
     * @param inputArray  input data as json array.
     * @param schemaArray inout schema as json array.
     * @throws ValidatorException validation exception occurs.
     * @throws ParserException    parsing exception occurs.
     */
    private static void ProcessSchemaWithItemsArray(JsonArray inputArray, JsonArray schemaArray) throws
            ValidatorException, ParserException {
        int i = 0;
        for (JsonElement element : schemaArray) {
            JsonObject tempObj = element.getAsJsonObject();
            String type = tempObj.get(ValidatorConstants.TYPE_KEY).toString().replaceAll(ValidatorConstants
                    .REGEX, "");
            if (ValidatorConstants.BOOLEAN_KEYS.contains(type)) {
                inputArray.set(i, BooleanValidator.validateBoolean(tempObj, inputArray.get(i)
                        .getAsString()));
            } else if (ValidatorConstants.NOMINAL_KEYS.contains(type)) {
                inputArray.set(i, StringValidator.validateNominal(tempObj, inputArray.get(i)
                        .getAsString()));
            } else if (ValidatorConstants.NUMERIC_KEYS.contains(type)) {
                inputArray.set(i, NumericValidator.validateNumeric(tempObj, inputArray.get(i)
                        .getAsString()));
            } else if (ValidatorConstants.ARRAY_KEYS.contains(type)) {
                inputArray.set(i, ArrayValidator.validateArray(
                        GSONDataTypeConverter.getMapFromString(inputArray.get(i).getAsString()), tempObj));
            } else if (ValidatorConstants.NULL_KEYS.contains(type)) {
                // todo add null implementation
            }
            i++;
        }
    }

    /**
     * Validate JSON array when items is a single JSON object.
     * Ex:- {"type":"array", "items":{"type": "boolean"}}
     *
     * @param inputArray   input data as json array.
     * @param schemaObject input schema as json object.
     * @throws ValidatorException validation exception occurs.
     * @throws ParserException    parsing exception occurs.
     */
    private static void ProcessSchemaWithOneItem(JsonArray inputArray, JsonObject schemaObject) throws
            ValidatorException, ParserException {
        String type = schemaObject.get(ValidatorConstants.TYPE_KEY).toString().replaceAll(ValidatorConstants
                .REGEX, "");
        int i = 0;
        if (ValidatorConstants.BOOLEAN_KEYS.contains(type)) {
            for (JsonElement element : inputArray) {
                inputArray.set(i, BooleanValidator.validateBoolean(schemaObject, element.getAsString()));
                i++;
            }
        } else if (ValidatorConstants.NUMERIC_KEYS.contains(type)) {
            for (JsonElement element : inputArray) {
                inputArray.set(i, NumericValidator.validateNumeric(schemaObject, element.getAsString()));
                i++;
            }
        } else if (ValidatorConstants.NOMINAL_KEYS.contains(type)) {
            for (JsonElement element : inputArray) {
                inputArray.set(i, StringValidator.validateNominal(schemaObject, element.getAsString()));
                i++;
            }
        } else if (ValidatorConstants.ARRAY_KEYS.contains(type)) {
            for (JsonElement element : inputArray) {
                inputArray.set(i, ArrayValidator.validateArray(GSONDataTypeConverter.getMapFromString(
                        element.getAsString()), schemaObject));
                i++;
            }
        } else if (ValidatorConstants.OBJECT_KEYS.contains(type)) {
            // todo add object implementation
        } else if (ValidatorConstants.NULL_KEYS.contains(type)) {
            // todo add null implementation
        }
    }
}
