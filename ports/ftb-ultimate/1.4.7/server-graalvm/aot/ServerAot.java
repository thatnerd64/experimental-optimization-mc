package retro.aot;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cpw.mods.fml.relauncher.DummyDownloader;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLRelauncher;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.RelaunchClassLoader;
import cpw.mods.fml.relauncher.RelaunchLibraryManager;

/**
 * Runs the real FML 1.4.7 server relaunch chain (coremods, access transformers, side stripping) headlessly and
 * dumps every class AFTER transformation, so the result is a plain closed-world program.
 * args: <gameDir> <serverJar> <outDir>      gameDir has lib/ mods/ coremods/ config/
 * Runs as the app main (on the system class loader); the worker half runs inside the RelaunchClassLoader.
 */
public class ServerAot {
    public static void main(String[] args) throws Exception {
        File gameDir = new File(args[0]).getCanonicalFile();
        File serverJar = new File(args[1]).getCanonicalFile();
        File outDir = new File(args[2]).getCanonicalFile();
        outDir.mkdirs();

        List<URL> urls = new ArrayList<URL>();
        urls.add(serverJar.toURI().toURL());
        for (String d : new String[] { "lib", "coremods" }) {
            File[] fs = new File(gameDir, d).listFiles();
            if (fs != null) { Arrays.sort(fs); for (File f : fs) urls.add(f.toURI().toURL()); }
        }
        File[] mods = new File(gameDir, "mods").listFiles();
        if (mods != null) { Arrays.sort(mods); for (File f : mods) urls.add(f.toURI().toURL()); }
        // the worker class itself must be loadable inside the RelaunchClassLoader
        urls.add(new File(ServerAot.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toURI().toURL());

        RelaunchClassLoader loader = new RelaunchClassLoader(urls.toArray(new URL[0]));
        Thread.currentThread().setContextClassLoader(loader);

        set(FMLRelauncher.class, "side", "SERVER");
        set(FMLRelauncher.class, "logFileNamePattern", "ForgeModLoader-server-%g.log");
        set(FMLRelaunchLog.class, "minecraftHome", gameDir);
        Method cfg = FMLRelaunchLog.class.getDeclaredMethod("configureLogging");
        cfg.setAccessible(true);
        cfg.invoke(null);
        Method bld = FMLInjectionData.class.getDeclaredMethod("build", File.class, RelaunchClassLoader.class);
        bld.setAccessible(true);
        bld.invoke(null, gameDir, loader);
        set(RelaunchLibraryManager.class, "downloadMonitor", new DummyDownloader());
        RelaunchLibraryManager.handleLaunch(gameDir, loader);
        System.out.println("Registered transformers: " + loader.getTransformers().size());

        Class<?> w = Class.forName("retro.aot.ServerAot", true, loader);
        w.getMethod("dump", File.class, File.class, File.class, RelaunchClassLoader.class)
                .invoke(null, gameDir, serverJar, outDir, loader);
        System.exit(0);
    }

    private static void set(Class<?> c, String name, Object v) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, v);
    }

    // ---- runs inside the RelaunchClassLoader ----
    public static void dump(File gameDir, File serverJar, File outDir, RelaunchClassLoader loader) throws Exception {
        Method runTrans = RelaunchClassLoader.class.getDeclaredMethod("runTransformers", String.class, byte[].class);
        runTrans.setAccessible(true);

        List<File> jars = new ArrayList<File>();
        jars.add(serverJar);
        for (String d : new String[] { "coremods", "mods", "lib" }) {
            File[] fs = new File(gameDir, d).listFiles();
            if (fs == null) continue;
            Arrays.sort(fs);
            for (File f : fs) if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) jars.add(f);
        }

        Set<String> seen = new HashSet<String>();
        int total = 0, changed = 0, errors = 0, skippedCount = 0;
        PrintWriter skipped = new PrintWriter(new FileWriter(new File(outDir, "client-only-skipped.txt")));
        File out = new File(outDir, "classes.jar");
        PrintWriter origin = new PrintWriter(new FileWriter(new File(outDir, "origins.txt")));
        StringBuilder originTable = new StringBuilder();
        Set<String> seenRes = new HashSet<String>();
        int resCount = 0;
        JarOutputStream res = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(new File(outDir, "resources.jar")), 1 << 18));
        JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(out), 1 << 18));
        try {
            for (File jar : jars) {
                ZipFile zf = new ZipFile(jar);
                try {
                    for (Enumeration<? extends ZipEntry> en = zf.entries(); en.hasMoreElements();) {
                        ZipEntry ze = en.nextElement();
                        String n = ze.getName();
                        if (ze.isDirectory()) continue;
                        if (!n.endsWith(".class")) {
                            String low = n.toLowerCase();
                            if (n.startsWith("META-INF/") && !n.startsWith("META-INF/services/")) continue;
                            if (low.endsWith(".ogg") || low.endsWith(".wav") || low.endsWith(".xcf") || low.endsWith(".java")) continue;
                            if (!seenRes.add(n)) continue;
                            res.putNextEntry(new ZipEntry(n));
                            res.write(read(zf.getInputStream(ze)));
                            res.closeEntry();
                            resCount++;
                            continue;
                        }
                        if (n.startsWith("META-INF/")) continue;
                        if (!seen.add(n)) continue;
                        total++;
                        byte[] orig = read(zf.getInputStream(ze));
                        byte[] t = orig;
                        try {
                            t = (byte[]) runTrans.invoke(loader, n.substring(0, n.length() - 6).replace('/', '.'), orig);
                            if (t == null) t = orig;
                        } catch (Throwable e) {
                            Throwable c = e.getCause() != null ? e.getCause() : e;
                            if (String.valueOf(c.getMessage()).contains("for invalid side")) {
                                skipped.println(n);   // @SideOnly(CLIENT) class: can never load on a dedicated server
                                skippedCount++;
                                continue;
                            }
                            errors++;
                            System.err.println("transformer error on " + n + ": " + c);
                        }
                        t = Patches.apply(n.substring(0, n.length() - 6), t);
                        if (!Arrays.equals(t, orig)) changed++;
                        jos.putNextEntry(new ZipEntry(n));
                        jos.write(t);
                        jos.closeEntry();
                        origin.println(n + "\t" + jar.getName());
                        originTable.append(n).append('\t').append(jar.getName()).append('\n');
                    }
                } finally { zf.close(); }
            }
            res.putNextEntry(new ZipEntry("retro/origins.txt"));
            res.write(originTable.toString().getBytes("UTF-8"));
            res.closeEntry();
        } finally { jos.close(); res.close(); origin.close(); skipped.close(); }

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outDir, "containers.txt")), "UTF-8"));
        for (String c : FMLInjectionData.containers) pw.println(c);
        pw.close();
        System.out.println("classes=" + total + " changedByTransformers=" + changed + " transformerErrors=" + errors + " clientOnlySkipped=" + skippedCount + " resources=" + resCount
                + " containers=" + FMLInjectionData.containers);
    }

    private static byte[] read(InputStream is) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        for (int r; (r = is.read(buf)) != -1;) b.write(buf, 0, r);
        is.close();
        return b.toByteArray();
    }
}
