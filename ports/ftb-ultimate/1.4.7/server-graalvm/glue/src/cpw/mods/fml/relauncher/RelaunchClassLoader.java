package cpw.mods.fml.relauncher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Closed-world stand-in for FML's transforming class loader. Every class in the image was already run through the
 * registered transformers at build time (ServerAot), so nothing is rewritten here; delegation to the application loader
 * does all the loading. The bookkeeping API that coremods and FML call is kept.
 */
public class RelaunchClassLoader extends ClassLoader {
    /** the loader FML's bootstrap created; Loader looks it up through its own (application) class loader instead */
    public static RelaunchClassLoader current;

    private final List<URL> sources = new ArrayList<URL>();
    private final List<IClassTransformer> transformers = new ArrayList<IClassTransformer>();

    public RelaunchClassLoader(URL[] urls) {
        super(RelaunchClassLoader.class.getClassLoader());
        for (URL u : urls) sources.add(u);
        current = this;
    }

    /** replaces (RelaunchClassLoader) casts of a class' loader, see retro.aot.Patches */
    public static RelaunchClassLoader cast(Object loader) {
        return loader instanceof RelaunchClassLoader ? (RelaunchClassLoader) loader : current;
    }

    public void registerTransformer(String transformerClassName) {
        // already applied ahead of time
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    public void addURL(URL url) {
        sources.add(url);
    }

    public URL[] getURLs() {
        return sources.toArray(new URL[0]);
    }

    public List<URL> getSources() {
        return sources;
    }

    public List<IClassTransformer> getTransformers() {
        return transformers;
    }

    void addTransformerExclusion(String toExclude) {
    }

    public byte[] getClassBytes(String name) throws IOException {
        InputStream is = getResourceAsStream(name.replace('.', '/') + ".class");
        if (is == null) return null;
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int r; (r = is.read(buf)) != -1;) b.write(buf, 0, r);
            return b.toByteArray();
        } finally {
            is.close();
        }
    }
}
