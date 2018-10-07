package contants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValidatorConstants {

    private ValidatorConstants() {
    }

    public static final String REGEX = "^\"|\"$";
    public static final String TYPE_KEY = "type";
    public static final String ITEM_KEY = "items";
    public static final String ENUM = "enum";
    public static final String CONST = "const";

    public static final Set<String> NUMERIC_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"numeric", "integer"}
    ));
    public static final Set<String> BOOLEAN_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"boolean"}
    ));
    public static final Set<String> NOMINAL_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"String", "string"}
    ));
    public static final Set<String> OBJECT_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"object"}
    ));
    public static final Set<String> ARRAY_KEYS = new HashSet<>(Arrays.asList(
            new String[]{"array"}
    ));
}
