package dev.drlink0.simplevoicemoderation;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceMuteManager {
    private final Set<UUID> mutedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> scheduledUnmutes = new ConcurrentHashMap<>();

    public void mute(UUID playerId) {
        mutedPlayers.add(playerId);
    }

    public void unmute(UUID playerId) {
        mutedPlayers.remove(playerId);
        BukkitTask task = scheduledUnmutes.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public boolean isMuted(UUID playerId) {
        return mutedPlayers.contains(playerId);
    }

    public void scheduleUnmute(UUID playerId, long unmuteAtMillis, Plugin plugin) {
        // Cancel any existing scheduled unmute
        BukkitTask existing = scheduledUnmutes.remove(playerId);
        if (existing != null) {
            existing.cancel();
        }

        long delayMillis = unmuteAtMillis - System.currentTimeMillis();
        if (delayMillis <= 0) {
            unmute(playerId);
            return;
        }

        long delayTicks = delayMillis / 50; // 1 tick = 50ms
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                unmute(playerId);
                scheduledUnmutes.remove(playerId);
            }
        }.runTaskLater(plugin, delayTicks);

        scheduledUnmutes.put(playerId, task);
    }

    public void clear() {
        mutedPlayers.clear();
        scheduledUnmutes.values().forEach(BukkitTask::cancel);
        scheduledUnmutes.clear();
    }
}
