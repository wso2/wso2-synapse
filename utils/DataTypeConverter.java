package utils;

import com.sun.org.apache.xpath.internal.operations.Bool;
import exceptions.ParserException;
import org.apache.commons.lang.BooleanUtils;

public class DataTypeConverter {


    public static Boolean convertToBoolean(String value) throws ParserException {
        if (value.equals("true") || value.equals("false")) {
            boolean result = Boolean.parseBoolean(value);
            return result;
        }
        throw new ParserException("Cannot convert the sting : " + value + " to boolean");
    }

    public static int convertToInt(String value) throws ParserException {
        if (!value.isEmpty()) {
            try {
                int i = Integer.parseInt(value.trim());
                return i;
            } catch (NumberFormatException nfe) {
                throw new ParserException("NumberFormatException: " + nfe.getMessage());
            }
        }
        throw new ParserException("Empty value cannot convert to int");
    }

    public static double convertToDouble(String value) throws ParserException {
        if (!value.isEmpty()) {
            try {
                double i = Double.parseDouble(value.trim());
                return i;
            } catch (NumberFormatException nfe) {
                throw new ParserException("NumberFormatException: " + nfe.getMessage());
            }
        }
        throw new ParserException("Empty value cannot convert to double");
    }
}


