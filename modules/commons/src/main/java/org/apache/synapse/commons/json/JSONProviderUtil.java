package org.apache.synapse.commons.json;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
/**
 * This class contains some util methods to use with JSON providers which are used in JSONPath library
 * 
 */
public class JSONProviderUtil {
	
	// withDefaultPrettyPrinter() will format the JSON with indenting the content and adds additional overhead
	private static ObjectWriter objectWriter = new ObjectMapper().writer()/*.withDefaultPrettyPrinter()*/; 
	
	/**
	 * When we use Jackson as JSON parser, it uses java Map and List classes and
	 * toString methods does not format the content as a valid JSON. This method
	 * will return the JSON format string by reading the contents of any given
	 * object.
	 * 
	 * @param object input object to convert as JSON String
	 * @return JSON String of the given object
	 */
	public static String objectToString(Object object) {
		String json = null;
		try {
			json = objectWriter.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return json;
	}
	
}
