package cpw.mods.fml.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import cpw.mods.fml.common.modloader.BaseModProxy;
import cpw.mods.fml.relauncher.RelaunchClassLoader;

/** Class lookups go straight to the image / application loader; mod jars stay on disk only for FML's discovery scan. */
public class ModClassLoader extends ClassLoader {
    private static final List<String> STANDARD_LIBRARIES = Arrays.asList("jinput.jar", "lwjgl.jar", "lwjgl_util.jar");
    private final RelaunchClassLoader mainClassLoader;
    private final List<URL> modFiles = new ArrayList<URL>();

    public ModClassLoader(ClassLoader parent) {
        super(ModClassLoader.class.getClassLoader());
        this.mainClassLoader = parent instanceof RelaunchClassLoader ? (RelaunchClassLoader) parent : RelaunchClassLoader.current;
    }

    public void addFile(File modFile) throws MalformedURLException {
        modFiles.add(modFile.toURI().toURL());
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    public File[] getParentSources() {
        List<URL> urls = mainClassLoader.getSources();
        File[] sources = new File[urls.size()];
        try {
            for (int i = 0; i < urls.size(); i++) sources[i] = new File(urls.get(i).toURI());
            return sources;
        } catch (Exception e) {
            FMLLog.log(Level.SEVERE, e, "Unable to process our input to locate the minecraft code");
            throw new LoaderException(e);
        }
    }

    public List<String> getDefaultLibraries() {
        return STANDARD_LIBRARIES;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends BaseModProxy> loadBaseModClass(String name) throws Exception {
        return (Class<? extends BaseModProxy>) Class.forName(name, false, this);
    }
}
