package retro.glue;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime half of the EnumHelper replacement. ServerAot bakes every constant a mod would add into the enum class itself;
 * addEnum then only has to hand out the matching, already existing constant (the n-th one when a name is added repeatedly).
 */
public final class Enums {
    private static final Map<String, Integer> calls = new HashMap<String, Integer>();

    public static synchronized Enum<?> find(Class<?> enumClass, String name) {
        String key = enumClass.getName() + "#" + name;
        Integer prev = calls.get(key);
        int want = prev == null ? 1 : prev + 1;
        calls.put(key, want);
        int seen = 0;
        for (Object c : enumClass.getEnumConstants()) {
            Enum<?> e = (Enum<?>) c;
            if (e.name().equals(name) && ++seen == want) return e;
        }
        throw new IllegalStateException("EnumHelper.addEnum(" + enumClass.getName() + ", " + name + ") call #" + want
                + " was not recorded at build time; re-run the enum recording pass");
    }

    private Enums() {}
}
