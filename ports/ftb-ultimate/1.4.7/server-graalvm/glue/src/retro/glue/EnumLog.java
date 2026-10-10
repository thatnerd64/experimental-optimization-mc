package retro.glue;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/** Record-run only: logs every EnumHelper.addEnum call so ServerAot can bake the constants into the enum classes. */
public final class EnumLog {
    private static PrintWriter out;

    public static synchronized void log(Class<?> enumClass, String name, Class<?>[] ptypes, Object[] args) {
        try {
            if (out == null) out = new PrintWriter(new FileWriter(System.getProperty("retro.enums.out", "enum-additions.txt"), true));
            StringBuilder sb = new StringBuilder();
            sb.append(enumClass.getName()).append('\t').append(name).append('\t');
            for (int i = 0; i < ptypes.length; i++) sb.append(i > 0 ? "," : "").append(ptypes[i].getName());
            sb.append('\t');
            for (int i = 0; i < args.length; i++) sb.append(i > 0 ? "\u0001" : "").append(enc(args[i]));
            out.println(sb);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String enc(Object o) {
        if (o == null) return "N:";
        if (o instanceof Integer) return "I:" + o;
        if (o instanceof Float) return "F:" + Float.floatToIntBits((Float) o);
        if (o instanceof Double) return "D:" + Double.doubleToLongBits((Double) o);
        if (o instanceof Long) return "J:" + o;
        if (o instanceof Boolean) return "Z:" + o;
        if (o instanceof Short) return "S:" + o;
        if (o instanceof Byte) return "B:" + o;
        if (o instanceof Character) return "C:" + (int) (Character) o;
        if (o instanceof String) return "T:" + ((String) o).replace("\\", "\\\\").replace("\n", "\\n").replace("\u0001", "\\u0001");
        if (o instanceof int[]) {
            StringBuilder b = new StringBuilder("AI:");
            int[] a = (int[]) o;
            for (int i = 0; i < a.length; i++) b.append(i > 0 ? "," : "").append(a[i]);
            return b.toString();
        }
        if (o instanceof Class) return "L:" + ((Class<?>) o).getName();
        if (o instanceof Enum) return "E:" + ((Enum<?>) o).getDeclaringClass().getName() + "." + ((Enum<?>) o).name();
        return "?:" + o.getClass().getName();
    }

    private EnumLog() {}
}
