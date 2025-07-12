package live.noumifuurinn.forgeexporter;


import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@Mod.EventBusSubscriber(modid = ForgeExporter.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        try {
            Class.forName(Prometheus.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }

    public static class Prometheus {
        static {
            BUILDER.comment("prometheus配置").push("prometheus");
        }

        public static ForgeConfigSpec.ConfigValue<Boolean> ENABLED = BUILDER
                .comment("是否启用Prometheus")
                .define("enabled", true);

        public static ForgeConfigSpec.ConfigValue<String> HOST = BUILDER
                .comment("地址")
                .define("host", "0.0.0.0");
        public static ForgeConfigSpec.IntValue PORT = BUILDER
                .comment("端口")
                .defineInRange("port", 9225, 1, 65535);
        public static ForgeConfigSpec.ConfigValue<String> UNIX_SOCKET_PATH = BUILDER
                .comment("Unix Socket路径，建议使用 metrics.sock")
                .define("unixSocketPath", "");

        static {
            BUILDER.pop();
        }
    }
}
