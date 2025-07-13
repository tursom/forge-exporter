package live.noumifuurinn.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import live.noumifuurinn.ForgeExporter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class PlayerOnline extends Metric {
    private final ConcurrentMap<UUID, PlayerStatus> status = new ConcurrentHashMap<>();

    public PlayerOnline(MeterRegistry registry) {
        super(registry);

        // 注册到Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public Set<Meter> register() {
        var meters = new HashSet<Meter>();
        for (ServerPlayer player : ForgeExporter.getServer().getPlayerList().getPlayers()) {
            meters.add(register(player));
        }
        return meters;
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        meters.add(register(event.getEntity()));
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        remove(event.getEntity());
    }

    private Meter register(Player player) {
        PlayerStatus playerStatus = status.computeIfAbsent(
                player.getUUID(),
                ignore -> {
                    PlayerStatus ps = new PlayerStatus();
                    ps.gauge = Gauge.builder(prefix("player.online"), ps, PlayerStatus::getState)
                            .description("Online state by player name")
                            .tag("name", player.getName().getString())
                            .tag("uid", player.getUUID().toString())
                            .register(registry);
                    return ps;
                }
        );
        playerStatus.state = 1;
        return playerStatus.gauge;
    }

    private void remove(Player player) {
        UUID uuid = player.getUUID();
        PlayerStatus playerStatus = status.get(uuid);
        if (playerStatus == null || playerStatus.state == 0) {
            return;
        }

        playerStatus.state = 0;

        Gauge gauge = playerStatus.gauge;
        if (gauge == null) {
            return;
        }
        ForgeExporter.EXECUTOR_SERVICE.schedule(() -> remove(uuid, gauge), 10, TimeUnit.MINUTES);
    }

    @SneakyThrows
    private void remove(UUID uuid, Gauge gauge) {
        if (status.remove(uuid, new PlayerStatus(0))) {
            registry.remove(gauge);
            meters.remove(gauge);
        }
    }

    @Data
    @NoArgsConstructor
    private static class PlayerStatus {
        private double state;
        @EqualsAndHashCode.Exclude
        private Gauge gauge;

        public PlayerStatus(double state) {
            this.state = state;
        }
    }
}
