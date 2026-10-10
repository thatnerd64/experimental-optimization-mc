import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.regex.*;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Minimal Minecraft 1.4.7 (protocol 51) client: RSA/AES-CFB8 login, then idles sending "player on ground" packets so the
 * server keeps the player, loads/generates chunks around it and ticks everything. Everything the server sends is discarded
 * after being counted.   java -cp guava.jar Bot.java <host> <port> <name> <seconds> <server ForgeModLoader log>
 * Optional 6th arg = server stdout log: then the bot flies along +x at 20 blocks/s (needs allow-flight=true) so the server keeps generating chunks.
 * Speaks the FML 1.4.7 handshake: fake Packet1Login carrying murmur3("FML"), then answers the mod-list request.
 */
public class Bot {
    static Cipher enc, dec;
    static DataOutputStream out;
    static InputStream in;

    static void wstr(DataOutputStream o, String s) throws IOException { o.writeShort(s.length()); o.write(s.getBytes(StandardCharsets.UTF_16BE)); }

    static synchronized void send(byte[] b) throws IOException {
        out.write(enc == null ? b : enc.update(b));
        out.flush();
    }

    static boolean matchesLoggedLogin(String logFile, double x, double z) {
        try {
            Matcher pm = Pattern.compile("logged in with entity id \\d+ at \\(([-0-9.E]+), ([-0-9.E]+), ([-0-9.E]+)\\)")
                    .matcher(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(logFile)), StandardCharsets.UTF_8));
            boolean found = false, ok = false;
            while (pm.find()) { found = true; ok |= Math.abs(Double.parseDouble(pm.group(1)) - x) < 1e-6 && Math.abs(Double.parseDouble(pm.group(3)) - z) < 1e-6; }
            return !found || ok;                              // no login line yet: fall back to the plausibility checks
        } catch (IOException e) { return true; }
    }

    static int rd() throws IOException {
        int b = in.read();
        if (b < 0) throw new EOFException();
        if (dec != null) b = dec.update(new byte[] { (byte) b })[0] & 0xff;
        return b;
    }

    static int rshort() throws IOException { return (rd() << 8 | rd()) << 16 >> 16; }

    static byte[] rbytes(int n) throws IOException { byte[] b = new byte[n]; for (int i = 0; i < n; i++) b[i] = (byte) rd(); return b; }

    public static void main(String[] a) throws Exception {
        String host = a[0]; int port = Integer.parseInt(a[1]); String name = a[2]; int secs = Integer.parseInt(a[3]);
        Socket s = new Socket(host, port);
        s.setSoTimeout(20000);
        out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        in = new BufferedInputStream(s.getInputStream());
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(bo);
        d.writeByte(0x02); d.writeByte(51); wstr(d, name); wstr(d, host + "\0FML"); d.writeInt(port);
        send(bo.toByteArray());
        int id = rd();
        if (id != 0xFD) { System.out.println("expected 0xFD key request, got 0x" + Integer.toHexString(id)); return; }
        int sl = rshort(); rbytes(sl * 2);                 // server id (UTF-16)
        byte[] pub = rbytes(rshort());
        byte[] token = rbytes(rshort());
        PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pub));
        byte[] secret = new byte[16]; new SecureRandom().nextBytes(secret);
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, pk);
        byte[] es = rsa.doFinal(secret);
        rsa.init(Cipher.ENCRYPT_MODE, pk);
        byte[] et = rsa.doFinal(token);
        bo.reset(); d.writeByte(0xFC); d.writeShort(es.length); d.write(es); d.writeShort(et.length); d.write(et);
        send(bo.toByteArray());
        id = rd();
        if (id != 0xFC) { System.out.println("expected 0xFC, got 0x" + Integer.toHexString(id)); return; }
        rbytes(rshort()); rbytes(rshort());
        enc = Cipher.getInstance("AES/CFB8/NoPadding"); enc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));
        dec = Cipher.getInstance("AES/CFB8/NoPadding"); dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret, "AES"), new IvParameterSpec(secret));
        int fmlHash = com.google.common.hash.Hashing.murmur3_32().hashString("FML").asInt();
        bo.reset(); d.writeByte(0x01); d.writeInt(fmlHash); wstr(d, "default"); d.writeByte(0); d.writeByte(2); d.writeByte(0); d.writeByte(0); d.writeByte(0);
        send(bo.toByteArray());                              // FML marker: fake Packet1Login (entity id = hash, dimension = 2)
        send(new byte[] { (byte) 0xCD, 0 });               // client status: initial spawn
        System.out.println("encrypted, sent 0xCD");
        Map<String, String> versions = new HashMap<>();
        Matcher vm = Pattern.compile("The mod (.+) accepts its own version \\((.*)\\)").matcher(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[4])), StandardCharsets.UTF_8));
        while (vm.find()) versions.put(vm.group(1), vm.group(2));
        System.out.println("known mod versions: " + versions.size());
        long t0 = System.currentTimeMillis();
        final long[] count = new long[1];
        final double[][] tele = new double[1][];            // server teleport: x, wireA, wireB, z
        final int[] first = { -1 };
        Thread rt = new Thread(() -> {
            try {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ByteArrayOutputStream all = new ByteArrayOutputStream();
                int teleScan = 0;
                byte[] pat = { (byte) 0xFA, 0, 3, 0, 'F', 0, 'M', 0, 'L' };
                int scanned = 0, answered = 0;
                while (true) {
                    int b = rd();
                    if (first[0] < 0) { first[0] = b; System.out.println("first packet from server: 0x" + Integer.toHexString(b) + " after " + (System.currentTimeMillis() - t0) + " ms"); }
                    count[0]++;
                    if (tele[0] == null && a.length > 5) {                    // server->client Packet13: x, stance, y, z, yaw, pitch, ground
                        all.write(b);
                        byte[] cur = all.toByteArray();
                        for (; teleScan + 42 <= cur.length; teleScan++) {
                            if (cur[teleScan] != 0x0D) continue;
                            DataInputStream pi = new DataInputStream(new ByteArrayInputStream(cur, teleScan + 1, 41));
                            double x = pi.readDouble(), p1 = pi.readDouble(), p2 = pi.readDouble(), z = pi.readDouble();
                            if (Math.abs(p1 - p2) > 1.0 && Math.abs(p1 - p2) < 2.5 && Math.abs(x) < 3e7 && Math.abs(z) < 3e7 && p1 > 0 && p1 < 300
                                    && matchesLoggedLogin(a[5], x, z)) {   // the server logged where it put us: ignore look-alikes in chunk data
                                tele[0] = new double[] { x, p1, p2, z };
                                System.out.println("server teleport: x=" + x + " a=" + p1 + " b=" + p2 + " z=" + z);
                                break;
                            }
                        }
                    }
                    if (answered < 1) {
                        buf.write(b);
                        byte[] cur = buf.toByteArray();
                        for (int i = scanned; i + pat.length + 2 <= cur.length; i++) {
                            boolean m = true; for (int k = 0; k < pat.length && m; k++) m = cur[i + k] == pat[k];
                            if (!m) { scanned = i + 1; continue; }
                            int len = (cur[i + pat.length] & 0xff) << 8 | cur[i + pat.length + 1] & 0xff;
                            if (i + pat.length + 2 + len > cur.length) break;              // wait for the rest
                            byte[] data = Arrays.copyOfRange(cur, i + pat.length + 2, i + pat.length + 2 + len);
                            scanned = i + pat.length + 2 + len;
                            System.out.println("FML packet type " + data[0] + " (" + len + " bytes)");
                            if (data[0] == 0) {                                          // MOD_LIST_REQUEST
                                DataInputStream di = new DataInputStream(new ByteArrayInputStream(data, 1, data.length - 1));
                                int n = di.readInt(); List<String> want = new ArrayList<>();
                                for (int k = 0; k < n; k++) want.add(di.readUTF());
                                ByteArrayOutputStream rb = new ByteArrayOutputStream(); DataOutputStream rd2 = new DataOutputStream(rb);
                                rd2.writeByte(1);
                                List<String> have = new ArrayList<>();
                                for (String w : want) if (versions.containsKey(w)) have.add(w);
                                rd2.writeInt(have.size());
                                for (String w : have) { rd2.writeUTF(w); rd2.writeUTF(versions.get(w)); }
                                rd2.writeInt(0);
                                byte[] payload = rb.toByteArray();
                                ByteArrayOutputStream pb = new ByteArrayOutputStream(); DataOutputStream pd = new DataOutputStream(pb);
                                pd.writeByte(0xFA); wstr(pd, "FML"); pd.writeShort(payload.length); pd.write(payload);
                                send(pb.toByteArray());
                                answered++;
                                System.out.println("answered mod list request: server wants " + n + " mods, we report " + have.size());
                            }
                        }
                    }
                }
            } catch (IOException e) { System.out.println("read ended: " + e + " after " + count[0] + " bytes"); }
        });
        rt.setDaemon(true); rt.start();
        long end = t0 + secs * 1000L;
        double[] pos = null;           // x, feet y, z
        int sent = 0;
        while (System.currentTimeMillis() < end && rt.isAlive()) {
            try {
                if (a.length <= 5) { Thread.sleep(250); send(new byte[] { 0x0A, 1 }); continue; }
                if (pos == null) {
                    Thread.sleep(50);
                    if (tele[0] == null) continue;
                    // a vanilla client answers the teleport with x, minY, posY(stance), z - the wire order has the two Y values swapped
                    double ya = tele[0][1], yb = tele[0][2];
                    double stance = Math.max(ya, yb), feet = Math.min(ya, yb);
                    pos = new double[] { tele[0][0], feet, tele[0][3] };
                    System.out.println("echoing spawn: feet=" + feet + " stance=" + stance);
                    for (int i = 0; i < 3; i++) {
                        bo.reset(); d.writeByte(0x0D); d.writeDouble(pos[0]); d.writeDouble(feet); d.writeDouble(stance); d.writeDouble(pos[2]);
                        d.writeFloat(0f); d.writeFloat(0f); d.writeByte(0); send(bo.toByteArray());
                    }
                    continue;
                }
                Thread.sleep(50);
                if (pos[1] < 140) pos[1] += 1; else pos[0] += 1;       // climb to y=140, then fly +x at 20 blocks/s
                bo.reset(); d.writeByte(0x0D); d.writeDouble(pos[0]); d.writeDouble(pos[1]); d.writeDouble(pos[1] + 1.62); d.writeDouble(pos[2]);
                d.writeFloat(-90f); d.writeFloat(0f); d.writeByte(0);
                send(bo.toByteArray());
            } catch (IOException e) { System.out.println("write failed: " + e); break; }
        }
        if (pos != null) System.out.println("final position x=" + (long) pos[0]);
        System.out.println("done: received " + count[0] + " bytes, reader " + (rt.isAlive() ? "alive" : "ended"));
    }
}
