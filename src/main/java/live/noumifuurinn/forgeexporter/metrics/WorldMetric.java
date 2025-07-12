package live.noumifuurinn.forgeexporter.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import live.noumifuurinn.forgeexporter.ForgeExporter;
import lombok.SneakyThrows;
import net.minecraft.server.level.ServerLevel;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public abstract class WorldMetric extends Metric {
    private final ConcurrentMap<SoftReference<ServerLevel>, Meter> worldMeters = new ConcurrentHashMap<>();

    public WorldMetric(MeterRegistry registry) {
        super(registry);

        ForgeExporter.EXECUTOR_SERVICE.scheduleWithFixedDelay(this::syncWorldsTask, 10, 10, TimeUnit.MINUTES);
    }

    @Override
    public final void register() {
        syncWorlds();
    }

    private void syncWorlds() {
        if (!isEnabled()) {
            return;
        }

        for (ServerLevel world : ForgeExporter.getServer().getAllLevels()) {
            worldMeters.computeIfAbsent(new SoftReference<>(world), ref -> this.register(world));
        }
    }


    @SneakyThrows
    private void syncWorldsTask() {
        // 检查是否有世界被卸载
        worldMeters.forEach((world, meter) -> {
            if (world.get() != null) {
                return;
            }

            registry.remove(meter);
        });

        // 加载新创建的新世界
        syncWorlds();
    }

    protected abstract Meter register(ServerLevel world);
}
