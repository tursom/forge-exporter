package live.noumifuurinn;


import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@Mod.EventBusSubscriber(modid = ForgeExporter.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        try {
            Class.forName(Prometheus.class.getName());
            Class.forName(Meters.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static final ForgeConfigSpec.ConfigValue<String> prefix = BUILDER
            .comment("指标前缀")
            .define("prefix", "mc.");

    public static final ForgeConfigSpec.ConfigValue<com.electronwill.nightconfig.core.Config> tags = BUILDER
            .comment("公共标签")
            .define("tags",
                    () -> com.electronwill.nightconfig.core.Config.of(TomlFormat.instance()),
                    value -> true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static class Prometheus {
        static {
            BUILDER.comment("prometheus配置").push("prometheus");
        }

        public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLED = BUILDER
                .comment("是否启用Prometheus")
                .define("enabled", true);

        public static final ForgeConfigSpec.ConfigValue<String> HOST = BUILDER
                .comment("地址")
                .define("host", "0.0.0.0");
        public static final ForgeConfigSpec.IntValue PORT = BUILDER
                .comment("端口")
                .defineInRange("port", 9225, 1, 65535);
        public static final ForgeConfigSpec.ConfigValue<String> UNIX_SOCKET_PATH = BUILDER
                .comment("Unix Socket路径，建议使用 metrics.sock")
                .define("unixSocketPath", "");

        static {
            BUILDER.pop();
        }
    }

    public static class Meters {
        static {
            BUILDER.comment("指标配置").push("meters");
        }

        public static final ForgeConfigSpec.BooleanValue PROCESSOR = BUILDER
                .comment("是否启用 processor 监控")
                .define("processor", true);
        public static final ForgeConfigSpec.BooleanValue GC = BUILDER
                .comment("是否启用 gc 监控")
                .define("gc", true);
        public static final ForgeConfigSpec.BooleanValue ENTITIES = BUILDER
                .comment("是否启用 entities 监控")
                .define("entities", true);
        public static final ForgeConfigSpec.BooleanValue LOADED_CHUNKS = BUILDER
                .comment("是否启用 loaded.chunks 监控")
                .define("loadedChunks", true);
        public static final ForgeConfigSpec.BooleanValue MEMORY = BUILDER
                .comment("是否启用 memory 监控")
                .define("memory", true);
        public static final ForgeConfigSpec.BooleanValue PLAYER_ONLINE = BUILDER
                .comment("是否启用 player.online 监控")
                .define("playerOnline", true);
        public static final ForgeConfigSpec.BooleanValue PLAYERS_WORLD = BUILDER
                .comment("是否启用 players.world 监控")
                .define("playersWorld", true);
        public static final ForgeConfigSpec.BooleanValue THREADS = BUILDER
                .comment("是否启用 threads 监控")
                .define("threads", true);
        public static final ForgeConfigSpec.BooleanValue TICK_DURATION_AVERAGE = BUILDER
                .comment("是否启用 tick.duration.average 监控")
                .define("tickDurationAverage", true);
        public static final ForgeConfigSpec.BooleanValue TICK_DURATION_MAX = BUILDER
                .comment("是否启用 tick.duration.max 监控")
                .define("tickDurationMax", true);
        public static final ForgeConfigSpec.BooleanValue TICK_DURATION_MEDIAN = BUILDER
                .comment("是否启用 tick.duration.median 监控")
                .define("tickDurationMedian", true);
        public static final ForgeConfigSpec.BooleanValue TICK_DURATION_MIN = BUILDER
                .comment("是否启用 tick.duration.min 监控")
                .define("tickDurationMin", true);
        public static final ForgeConfigSpec.BooleanValue TPS = BUILDER
                .comment("是否启用 tps 监控")
                .define("tps", true);
        public static final ForgeConfigSpec.BooleanValue WORLD_SIZE = BUILDER
                .comment("是否启用 world.size 监控")
                .define("worldSize", true);

        static {
            BUILDER.pop();
        }
    }
}
