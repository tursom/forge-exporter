package live.noumifuurinn;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import live.noumifuurinn.utils.CommonUtils;
import net.minecraft.util.StringUtil;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Mod(ForgeExporter.MODID)
public class ForgeExporter {
    public static final String MODID = "forgeexporter";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final CompositeMeterRegistry registry = new CompositeMeterRegistry();

    private MetricsServer server;

    public ForgeExporter(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) throws Exception {
        if (!StringUtil.isNullOrEmpty(Config.prefix.get())) {
            registry.config().meterFilter(new MeterFilter() {
                @Override
                public Meter.@NotNull Id map(Meter.@NotNull Id id) {
                    return id.withName(Config.prefix.get() + id.getName());
                }
            });
        }
        if (!Config.tags.get().isEmpty()) {
            registry.config().commonTags(Config.tags.get().entrySet().stream()
                    .map((entry) -> Tag.of(entry.getKey(), entry.getValue()))
                    .toList());
        }

        CommonUtils.setServer(event.getServer());
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
        CommonUtils.setServer(null);
    }

    public Logger getLogger() {
        return LOGGER;
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
