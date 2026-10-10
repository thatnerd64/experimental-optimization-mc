import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

/** java Loadable.java <classes.jar> <resources.jar> <out.txt>: lists classes whose members cannot be reflected on (missing signature types). */
public class Loadable {
    static URL[] extra(String[] a) throws Exception {
        List<URL> u = new ArrayList<>();
        u.add(new File(a[0]).toURI().toURL());
        u.add(new File(a[1]).toURI().toURL());
        for (int i = 3; i < a.length; i++) u.add(new File(a[i]).toURI().toURL());
        return u.toArray(new URL[0]);
    }

    public static void main(String[] a) throws Exception {
        File cj = new File(a[0]);
        URLClassLoader cl = new URLClassLoader(extra(a), ClassLoader.getPlatformClassLoader());
        List<String> names = new ArrayList<>();
        try (ZipFile z = new ZipFile(cj)) {
            for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
                String n = e.nextElement().getName();
                if (n.endsWith(".class") && !n.startsWith("META-INF/")) names.add(n.substring(0, n.length() - 6).replace('/', '.'));
            }
        }
        Collections.sort(names);
        int bad = 0;
        try (PrintWriter out = new PrintWriter(a[2])) {
            for (String n : names) {
                String why = null;
                try {
                    Class<?> c = Class.forName(n, false, cl);
                    c.getDeclaredConstructors();
                    c.getDeclaredMethods();
                    c.getDeclaredFields();
                    c.getDeclaredClasses();
                    if (c.isEnum()) c.getMethod("values");   // getEnumConstants' first step; resolves every signature, no class init
                } catch (Throwable t) {
                    why = t.getClass().getSimpleName() + ": " + t.getMessage();
                    try { if (Class.forName(n, false, cl).isEnum()) why += "   [ENUM]"; } catch (Throwable t2) { }
                }
                if (why != null) { bad++; out.println(n + "\t" + why); }
            }
        }
        System.out.println("classes=" + names.size() + " not-reflectable=" + bad);
    }
}
