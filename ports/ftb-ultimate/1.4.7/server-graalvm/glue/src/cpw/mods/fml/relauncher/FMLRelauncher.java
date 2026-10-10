package cpw.mods.fml.relauncher;

/** Dedicated-server-only replacement: there is no relaunch, the process already is the "relaunched" one. */
public class FMLRelauncher {
    private static FMLRelauncher INSTANCE = new FMLRelauncher();

    public static String logFileNamePattern = "ForgeModLoader-server-%g.log";
    private static String side = "SERVER";
    private RelaunchClassLoader classLoader;

    static FMLRelauncher instance() {
        return INSTANCE;
    }

    public static String side() {
        return side;
    }

    public static void handleClientRelaunch(ArgsWrapper args) {
        throw new UnsupportedOperationException("client");
    }

    public static void handleServerRelaunch(ArgsWrapper args) {
        // never entered: ServerBoot calls MinecraftServer.fmlReentry directly
    }
}
