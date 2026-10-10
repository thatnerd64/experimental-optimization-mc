package retro.aot;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Build-time bytecode fixes that make the post-transform classes valid in a closed world. */
public class Patches implements Opcodes {
    /** @return patched bytes, or the input when no patch applies */
    public static byte[] apply(String internalName, byte[] b) {
        if (internalName.equals("cpw/mods/fml/common/FMLModContainer")) b = fmlModContainer(b);
        if (internalName.equals("net/minecraftforge/common/EnumHelper") && Boolean.getBoolean("retro.enums.record")) b = enumRecord(b);
        if (internalName.equals("net/minecraftforge/common/EnumHelper") && !Boolean.getBoolean("retro.enums.record")) b = enumHelperStatic(b);
        List<String[]> adds = enumAdditions().get(internalName);
        if (adds != null) b = enumExtend(internalName, b, adds);
        if (internalName.equals("miscperipherals/core/LuaManager")) b = originResource(b);
        b = typedToArray(b);
        return relaunchCasts(internalName, b);
    }

    /** record run: hook the 4-arg addEnum(Class, String, Class[], Object[]) that all overloads funnel into */
    private static byte[] enumRecord(byte[] b) {
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, 0);
        for (Object o : cn.methods) {
            MethodNode m = (MethodNode) o;
            if (m.name.equals("addEnum") && m.desc.equals("(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Enum;")) {
                InsnList il = new InsnList();
                il.add(new VarInsnNode(ALOAD, 0));
                il.add(new VarInsnNode(ALOAD, 1));
                il.add(new VarInsnNode(ALOAD, 2));
                il.add(new VarInsnNode(ALOAD, 3));
                il.add(new MethodInsnNode(INVOKESTATIC, "retro/glue/EnumLog", "log",
                        "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)V"));
                m.instructions.insert(il);
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static Map<String, List<String[]>> additions;

    /** enum class (internal name) -> recorded additions {name, ptypes, args} in call order, from -Dretro.enums.file */
    private static synchronized Map<String, List<String[]>> enumAdditions() {
        if (additions == null) {
            additions = new HashMap<String, List<String[]>>();
            String f = System.getProperty("retro.enums.file");
            if (f != null) {
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
                    for (String line; (line = r.readLine()) != null;) {
                        String[] p = line.split("\t", -1);
                        String cls = p[0].replace('.', '/');
                        List<String[]> l = additions.get(cls);
                        if (l == null) additions.put(cls, l = new ArrayList<String[]>());
                        l.add(new String[] { p[1], p[2], p[3] });
                    }
                    r.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return additions;
    }

    /** EnumHelper.addEnum(Class, String, Class[], Object[]) becomes a lookup of the constant baked in by enumExtend */
    private static byte[] enumHelperStatic(byte[] b) {
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, 0);
        for (Object o : cn.methods) {
            MethodNode m = (MethodNode) o;
            if (m.name.equals("setup") && m.desc.equals("()V")) {   // JDK-internal reflection setup, not needed any more
                m.instructions.clear();
                m.tryCatchBlocks.clear();
                if (m.localVariables != null) m.localVariables.clear();
                m.instructions.add(new InsnNode(RETURN));
            }
            if (m.name.equals("addEnum") && m.desc.equals("(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Enum;")) {
                m.instructions.clear();
                m.tryCatchBlocks.clear();
                if (m.localVariables != null) m.localVariables.clear();
                m.instructions.add(new VarInsnNode(ALOAD, 0));
                m.instructions.add(new VarInsnNode(ALOAD, 1));
                m.instructions.add(new MethodInsnNode(INVOKESTATIC, "retro/glue/Enums", "find",
                        "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;"));
                m.instructions.add(new InsnNode(ARETURN));
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static String desc(String javaName) {
        if (javaName.equals("int")) return "I";
        if (javaName.equals("float")) return "F";
        if (javaName.equals("double")) return "D";
        if (javaName.equals("long")) return "J";
        if (javaName.equals("boolean")) return "Z";
        if (javaName.equals("short")) return "S";
        if (javaName.equals("byte")) return "B";
        if (javaName.equals("char")) return "C";
        if (javaName.startsWith("[")) return javaName.replace('.', '/');
        return "L" + javaName.replace('.', '/') + ";";
    }

    private static void push(InsnList il, int v) {
        if (v >= -1 && v <= 5) il.add(new InsnNode(ICONST_0 + v));
        else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) il.add(new IntInsnNode(BIPUSH, v));
        else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) il.add(new IntInsnNode(SIPUSH, v));
        else il.add(new LdcInsnNode(Integer.valueOf(v)));
    }

    private static void pushArg(InsnList il, String enc) {
        String tag = enc.substring(0, enc.indexOf(':'));
        String v = enc.substring(enc.indexOf(':') + 1);
        if (tag.equals("I") || tag.equals("S") || tag.equals("B") || tag.equals("C")) push(il, Integer.parseInt(v));
        else if (tag.equals("Z")) il.add(new InsnNode(Boolean.parseBoolean(v) ? ICONST_1 : ICONST_0));
        else if (tag.equals("F")) il.add(new LdcInsnNode(Float.valueOf(Float.intBitsToFloat(Integer.parseInt(v)))));
        else if (tag.equals("D")) il.add(new LdcInsnNode(Double.valueOf(Double.longBitsToDouble(Long.parseLong(v)))));
        else if (tag.equals("J")) il.add(new LdcInsnNode(Long.valueOf(v)));
        else if (tag.equals("T")) il.add(new LdcInsnNode(v.replace("\\n", "\n").replace("\\u0001", "\u0001").replace("\\\\", "\\")));
        else if (tag.equals("N")) il.add(new InsnNode(ACONST_NULL));
        else if (tag.equals("AI")) {
            String[] parts = v.isEmpty() ? new String[0] : v.split(",");
            push(il, parts.length);
            il.add(new IntInsnNode(NEWARRAY, T_INT));
            for (int i = 0; i < parts.length; i++) {
                il.add(new InsnNode(DUP));
                push(il, i);
                push(il, Integer.parseInt(parts[i]));
                il.add(new InsnNode(IASTORE));
            }
        } else throw new IllegalStateException("EnumHelper argument kind '" + tag + "' cannot be baked in: " + enc);
    }

    /** appends the recorded constants to the enum: new instances at the end of <clinit>, $VALUES grown to hold them */
    private static byte[] enumExtend(String cls, byte[] b, List<String[]> adds) {
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, 0);
        String valuesField = null;
        for (Object o : cn.methods) {
            MethodNode m = (MethodNode) o;
            if (m.name.equals("values") && m.desc.equals("()[L" + cls + ";")) {
                for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                    if (n instanceof FieldInsnNode && n.getOpcode() == GETSTATIC) { valuesField = ((FieldInsnNode) n).name; break; }
                }
            }
        }
        if (valuesField == null) throw new IllegalStateException("cannot find $VALUES of " + cls);
        for (Object o : cn.methods) {
            MethodNode m = (MethodNode) o;
            if (!m.name.equals("<clinit>")) continue;
            int L = m.maxLocals;
            String arr = "[L" + cls + ";";
            InsnList il = new InsnList();
            il.add(new FieldInsnNode(GETSTATIC, cls, valuesField, arr));
            il.add(new InsnNode(ARRAYLENGTH));
            il.add(new VarInsnNode(ISTORE, L));
            il.add(new FieldInsnNode(GETSTATIC, cls, valuesField, arr));
            il.add(new VarInsnNode(ILOAD, L));
            push(il, adds.size());
            il.add(new InsnNode(IADD));
            il.add(new MethodInsnNode(INVOKESTATIC, "java/util/Arrays", "copyOf", "([Ljava/lang/Object;I)[Ljava/lang/Object;"));
            il.add(new TypeInsnNode(CHECKCAST, arr));
            il.add(new FieldInsnNode(PUTSTATIC, cls, valuesField, arr));
            for (int i = 0; i < adds.size(); i++) {
                String[] a = adds.get(i);
                StringBuilder d = new StringBuilder("(Ljava/lang/String;I");
                if (!a[1].isEmpty()) for (String t : a[1].split(",")) d.append(desc(t));
                d.append(")V");
                il.add(new FieldInsnNode(GETSTATIC, cls, valuesField, arr));
                il.add(new VarInsnNode(ILOAD, L));
                push(il, i);
                il.add(new InsnNode(IADD));
                il.add(new TypeInsnNode(NEW, cls));
                il.add(new InsnNode(DUP));
                il.add(new LdcInsnNode(a[0]));
                il.add(new VarInsnNode(ILOAD, L));
                push(il, i);
                il.add(new InsnNode(IADD));
                if (!a[2].isEmpty()) for (String enc : a[2].split("\u0001", -1)) pushArg(il, enc);
                il.add(new MethodInsnNode(INVOKESPECIAL, cls, "<init>", d.toString()));
                il.add(new InsnNode(AASTORE));
            }
            // before every RETURN of <clinit>
            for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n.getOpcode() == RETURN) { m.instructions.insertBefore(n, cloneList(il)); break; }
            }
            m.maxLocals = L + 1;
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static InsnList cloneList(InsnList src) {
        InsnList out = new InsnList();
        java.util.Map<org.objectweb.asm.tree.LabelNode, org.objectweb.asm.tree.LabelNode> map =
                new java.util.HashMap<org.objectweb.asm.tree.LabelNode, org.objectweb.asm.tree.LabelNode>();
        for (AbstractInsnNode n = src.getFirst(); n != null; n = n.getNext()) out.add(n.clone(map));
        return out;
    }

    /**
     * JDK 8's Arrays.asList(T[]).toArray() returned a T[], and 2012-era code casts it: (Integer[]) list.toArray().
     * Since JDK 9 (and in the image) that is an Object[] and the cast throws, so call toArray(new T[0]) instead.
     */
    private static byte[] typedToArray(byte[] b) {
        if (indexOf(b, "toArray".getBytes()) < 0) return b;
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, 0);
        boolean changed = false;
        for (Object o : cn.methods) {
            MethodNode m = (MethodNode) o;
            for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                if (!(n instanceof MethodInsnNode)) continue;
                MethodInsnNode mi = (MethodInsnNode) n;
                if (!mi.name.equals("toArray") || !mi.desc.equals("()[Ljava/lang/Object;")) continue;
                if (!(mi.owner.equals("java/util/List") || mi.owner.equals("java/util/Collection")
                        || mi.owner.equals("java/util/ArrayList") || mi.owner.equals("java/util/Set")
                        || mi.owner.equals("java/util/LinkedList") || mi.owner.equals("java/util/Vector"))) continue;
                AbstractInsnNode nx = n.getNext();
                while (nx != null && (nx.getOpcode() < 0)) nx = nx.getNext();   // skip labels / line numbers
                if (!(nx instanceof TypeInsnNode) || nx.getOpcode() != CHECKCAST || !((TypeInsnNode) nx).desc.startsWith("[")) continue;
                String arr = ((TypeInsnNode) nx).desc;
                String elem = arr.substring(1);
                if (elem.startsWith("L")) elem = elem.substring(1, elem.length() - 1);
                InsnList pre = new InsnList();
                pre.add(new InsnNode(ICONST_0));
                pre.add(new TypeInsnNode(ANEWARRAY, elem));
                m.instructions.insertBefore(n, pre);
                mi.desc = "([Ljava/lang/Object;)[Ljava/lang/Object;";
                changed = true;
            }
        }
        if (!changed) return b;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    /** LuaManager.init() finds its own jar via Class.getResource(".class"); point that at the jar on disk */
    private static byte[] originResource(byte[] b) {
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, 0);
        for (Object o : cn.methods) {
            MethodNode m = (MethodNode) o;
            for (AbstractInsnNode n = m.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n instanceof MethodInsnNode && n.getOpcode() == INVOKEVIRTUAL && ((MethodInsnNode) n).owner.equals("java/lang/Class")
                        && ((MethodInsnNode) n).name.equals("getResource")) {
                    m.instructions.set(n, new MethodInsnNode(INVOKESTATIC, "retro/glue/Origin", "resource",
                            "(Ljava/lang/Class;Ljava/lang/String;)Ljava/net/URL;"));
                }
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static final String RCL = "cpw/mods/fml/relauncher/RelaunchClassLoader";

    /**
     * Code written for FML's relaunch design does (RelaunchClassLoader) getClass().getClassLoader(). In a closed world
     * every class is loaded by the one application loader, so route those casts to the stub loader FML bootstrapped.
     */
    private static byte[] relaunchCasts(String name, byte[] b) {
        if (name.equals(RCL) || name.equals("cpw/mods/fml/common/ModClassLoader")) return b;
        if (indexOf(b, RCL.getBytes()) < 0) return b;
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, 0);
        boolean changed = false;
        for (Object o : cn.methods) {
            MethodNode m = (MethodNode) o;
            for (AbstractInsnNode n = m.instructions.getFirst(); n != null;) {
                AbstractInsnNode next = n.getNext();
                if (n instanceof TypeInsnNode && n.getOpcode() == CHECKCAST && ((TypeInsnNode) n).desc.equals(RCL)) {
                    m.instructions.set(n, new MethodInsnNode(INVOKESTATIC, RCL, "cast", "(Ljava/lang/Object;)L" + RCL + ";"));
                    changed = true;
                }
                n = next;
            }
        }
        if (!changed) return b;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static int indexOf(byte[] hay, byte[] needle) {
        outer:
        for (int i = 0; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    /**
     * Jar-signature verification reads the certificates of the mod class' CodeSource, which an image doesn't have
     * (and the mod classes were re-emitted into an unsigned jar anyway). The pack's jars are local and trusted, so
     * FMLModContainer.constructMod treats every mod as having no declared fingerprint and never posts the
     * FMLFingerprintViolationEvent (Railcraft and others turn that event into a crash).
     */
    private static byte[] fmlModContainer(byte[] b) {
        ClassNode cn = new ClassNode();
        new ClassReader(b).accept(cn, 0);
        for (Object o : cn.methods) {
            MethodNode m = (MethodNode) o;
            if (!m.name.equals("constructMod")) continue;
            for (AbstractInsnNode n = m.instructions.getFirst(); n != null;) {
                AbstractInsnNode next = n.getNext();
                if (n instanceof MethodInsnNode && ((MethodInsnNode) n).name.equals("getProtectionDomain")) {
                    // Class.getProtectionDomain().getCodeSource().getCertificates()  ->  null
                    AbstractInsnNode a = n.getNext(), c = a.getNext();
                    next = c.getNext();
                    m.instructions.remove(a);
                    m.instructions.remove(c);
                    InsnNode pop = new InsnNode(POP);
                    m.instructions.set(n, pop);
                    m.instructions.insert(pop, new InsnNode(ACONST_NULL));
                } else if (n instanceof LdcInsnNode && "certificateFingerprint".equals(((LdcInsnNode) n).cst)) {
                    ((LdcInsnNode) n).cst = "certificateFingerprint.checkedAtBuild";
                } else if (n instanceof FieldInsnNode && n.getOpcode() == GETFIELD
                        && ((FieldInsnNode) n).name.equals("fingerprintNotPresent")) {
                    m.instructions.insertBefore(n, new InsnNode(POP));
                    m.instructions.set(n, new InsnNode(ICONST_0));
                }
                n = next;
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
