package utils;

import com.google.gson.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class provides functionality to convert between GSON data structures.
 */
public class GSONDataTypeConverter {

    // use without instantiating
    private GSONDataTypeConverter(){
    }

    /**
     * Given a string contains a json array, this method will return the Map.
     * This is where the single element array correction happens.
     * @param input JsonArray as a string.
     * @return map entry of json array.
     */
    public static Map.Entry<String, JsonElement> getMapFromString(String input) {
        JsonParser parser = new JsonParser();
        JsonObject temp = new JsonObject();
        JsonElement inputElement = parser.parse(input);
        JsonArray arrayObject = null;
        if (inputElement.isJsonArray()) {
            arrayObject = (JsonArray) parser.parse(input);
        } else if (inputElement.isJsonPrimitive()) {
            arrayObject = new JsonArray();
            arrayObject.add(inputElement);
        }
        temp.add("test", arrayObject);
        Set<Map.Entry<String, JsonElement>> entries = temp.entrySet();
        Iterator itr = entries.iterator();
        return (Map.Entry<String, JsonElement>) itr.next();
    }
}
