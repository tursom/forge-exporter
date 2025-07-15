package live.noumifuurinn.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 抽取出的与 mod 端有关的代码，方便不同 mod 端之间的移植
 */
@UtilityClass
@Slf4j
public class CommonUtils {
    private final Map<Object, Runnable> serverTickReg = new ConcurrentHashMap<>();
    private final EventHandler eventHandler = new EventHandler();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ForgeExporter-Executor");
        thread.setDaemon(true);
        return thread;
    });


    @Getter
    @Setter
    private static MinecraftServer server;

    public long[] getTickTimesNanos() {
        return server.tickTimes;
    }

    public void onPlayerJoin(Consumer<Player> consumer) {
        eventHandler.playerJoinHandlers.add(consumer);
    }

    public void onPlayerLeave(Consumer<Player> consumer) {
        eventHandler.playerLeaveHandlers.add(consumer);
    }

    public void executeAfter(long delay, TimeUnit timeUnit, Runnable task) {
        executorService.schedule(task, 10, TimeUnit.MINUTES);
    }

    public void registerServerTickEvent(Object parent, Runnable r) {
        serverTickReg.put(parent, r);
    }

    public void unregisterServerTickEvent(Object parent) {
        serverTickReg.remove(parent);
    }

    @SneakyThrows
    private void delay(long delay, TimeUnit timeUnit) {
        Thread.sleep(timeUnit.toMillis(delay));
    }

    private static class EventHandler {
        private final List<Consumer<Player>> playerJoinHandlers = new ArrayList<>();
        private final List<Consumer<Player>> playerLeaveHandlers = new ArrayList<>();

        {
            MinecraftForge.EVENT_BUS.register(this);
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

        @SubscribeEvent
        public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            for (Consumer<Player> consumer : playerJoinHandlers) {
                try {
                    consumer.accept(event.getEntity());
                } catch (Exception e) {
                    log.warn("Error in player join event handler", e);
                }
            }
        }

        @SubscribeEvent
        public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            for (Consumer<Player> consumer : playerLeaveHandlers) {
                try {
                    consumer.accept(event.getEntity());
                } catch (Exception e) {
                    log.warn("Error in player leave event handler", e);
                }
            }
        }
    }
}
