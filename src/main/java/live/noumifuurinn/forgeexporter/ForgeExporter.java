package live.noumifuurinn.forgeexporter;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Mod(ForgeExporter.MODID)
public class ForgeExporter {
    public static final String MODID = "forgeexporter";
    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ForgeExporter-Executor");
        thread.setDaemon(true);
        return thread;
    });

    private static final Logger LOGGER = LogManager.getLogger();
    private static MinecraftServer mcServer;
    private final static Map<Object, Runnable> serverTickReg = new java.util.concurrent.ConcurrentHashMap<>();
    private static final CompositeMeterRegistry registry = new CompositeMeterRegistry();

    private MetricsServer server;

    public ForgeExporter(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) throws Exception {
        mcServer = event.getServer();

        startMetricsServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // 服务器已完全停止时触发，用于清理资源
        LOGGER.info("Server stopping, shutting down metrics server...");

        if (server != null) {
            try {
                server.stop();
                LOGGER.info("Metrics server stopped successfully");
            } catch (Exception e) {
                LOGGER.warn("Failed to stop metrics server gracefully: " + e.getMessage(), e);
            }
        }

        // 清理服务器引用
        mcServer = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (event.type != TickEvent.Type.SERVER) {
            return;
        }
        if (event.side != LogicalSide.SERVER) {
            return;
        }
        for (Runnable r : serverTickReg.values()) {
            try {
                r.run();
            } catch (Throwable t) {

            }
        }
    }

    public static void registerServerTickEvent(Object parent, Runnable r) {
        serverTickReg.put(parent, r);
    }

    public static void unregisterServerTickEvent(Object parent) {
        serverTickReg.remove(parent);
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public static MinecraftServer getServer() {
        return mcServer;
    }

    private void startMetricsServer() throws Exception {
        server = new MetricsServer(registry, this);
        server.start();
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            LOGGER.warn("Failed to stop metrics server gracefully: " + e.getMessage());
            LOGGER.warn("Failed to stop metrics server gracefully", e);
        }
    }
}
