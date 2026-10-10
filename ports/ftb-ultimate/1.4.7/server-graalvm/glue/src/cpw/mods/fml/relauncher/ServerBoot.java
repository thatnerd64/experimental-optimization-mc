package cpw.mods.fml.relauncher;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Entry point of the pre-transformed dedicated server (what FMLRelauncher.relaunchServer used to do). */
public class ServerBoot {
    public static void main(String[] args) throws Exception {
        File home = new File(".");
        FMLRelaunchLog.minecraftHome = home;
        FMLInjectionData.minecraftHome = home;
        FMLInjectionData.major = "4";
        FMLInjectionData.minor = "7";
        FMLInjectionData.rev = "35";
        FMLInjectionData.build = "556";
        FMLInjectionData.mccversion = "1.4.7";
        FMLInjectionData.mcpversion = "7.26a";

        List<URL> urls = new ArrayList<URL>();
        File server = new File(home, "server.jar");
        if (server.isFile()) urls.add(server.toURI().toURL());
        RelaunchClassLoader loader = new RelaunchClassLoader(urls.toArray(new URL[0]));
        Thread.currentThread().setContextClassLoader(loader);

        FMLRelaunchLog.info("Forge Mod Loader version %s.%s.%s.%s for Minecraft %s loading", FMLInjectionData.major,
                FMLInjectionData.minor, FMLInjectionData.rev, FMLInjectionData.build, FMLInjectionData.mccversion,
                FMLInjectionData.mcpversion);
        RelaunchLibraryManager.downloadMonitor = new DummyDownloader();
        RelaunchLibraryManager.handleLaunch(home, loader);
        net.minecraft.server.MinecraftServer.fmlReentry(new ArgsWrapper(args));
    }
}
