package live.noumifuurinn.forgeexporter.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import live.noumifuurinn.forgeexporter.ForgeExporter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
    public void register() {
        for (ServerPlayer player : ForgeExporter.getServer().getPlayerList().getPlayers()) {
            register(player);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        register(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        remove(event.getEntity());
    }

    private void register(Player player) {
        status.computeIfAbsent(
                player.getUUID(),
                ignore -> {
                    PlayerStatus playerStatus = new PlayerStatus();
                    playerStatus.gauge = Gauge.builder(prefix("player.online"), playerStatus, PlayerStatus::getState)
                            .description("Online state by player name")
                            .tag("name", player.getName().getString())
                            .tag("uid", player.getUUID().toString())
                            .register(registry);
                    return playerStatus;
                }
        ).state = 1;
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
