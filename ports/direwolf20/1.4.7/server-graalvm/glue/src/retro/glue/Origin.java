package retro.glue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * In a closed world every class lives in the application image, but a few mods locate "their" jar through
 * Class.getResource("/x/Y.class") and then open it as a JarFile. This maps such .class lookups back to the original
 * mod/coremod jar on disk (class -> jar from the build-time origins table).
 */
public final class Origin {
    private static Map<String, String> origins;

    private static synchronized Map<String, String> origins() {
        if (origins == null) {
            origins = new HashMap<String, String>();
            try {
                java.io.InputStream in = Origin.class.getResourceAsStream("/retro/origins.txt");
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    for (String l; (l = r.readLine()) != null;) {
                        int t = l.indexOf('\t');
                        if (t > 0) origins.put(l.substring(0, t), l.substring(t + 1));
                    }
                    r.close();
                }
            } catch (Exception e) {
                // leave the table empty: lookups fall through to the normal behaviour
            }
        }
        return origins;
    }

    public static URL resource(Class<?> c, String name) {
        String entry = name.startsWith("/") ? name.substring(1) : c.getName().replace('.', '/').replaceAll("[^/]*$", "") + name;
        if (entry.endsWith(".class")) {
            String jar = origins().get(entry);
            if (jar != null) {
                for (String dir : new String[] { "coremods", "mods", "lib", "." }) {
                    File f = new File(dir, jar);
                    if (f.isFile()) {
                        try {
                            return new URL("jar:" + f.getAbsoluteFile().toURI() + "!/" + entry);
                        } catch (Exception e) {
                            break;
                        }
                    }
                }
            }
        }
        return c.getResource(name);
    }

    private Origin() {}
}
