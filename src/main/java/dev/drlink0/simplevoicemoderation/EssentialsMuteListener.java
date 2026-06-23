package dev.drlink0.simplevoicemoderation;

import net.ess3.api.events.MuteStatusChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class EssentialsMuteListener implements Listener {
    private final VoiceMuteManager muteManager;
    private final Plugin plugin;
    private final Logger logger;

    public EssentialsMuteListener(VoiceMuteManager muteManager, Plugin plugin) {
        this.muteManager = muteManager;
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMuteStatusChange(MuteStatusChangeEvent event) {
        Player player = event.getAffected().getBase();
        UUID playerId = player.getUniqueId();

        if (event.getValue()) {
            // Player is being muted
            muteManager.mute(playerId);
            logger.info("Voice-muted " + player.getName() + " (synced from Essentials)");

            // Schedule unmute if timed
            Optional<Long> timestamp = event.getTimestamp();
            timestamp.ifPresent(unmuteAt ->
                muteManager.scheduleUnmute(playerId, unmuteAt, plugin)
            );
        } else {
            // Player is being unmuted
            muteManager.unmute(playerId);
            logger.info("Voice-unmuted " + player.getName() + " (synced from Essentials)");
        }
    }
}
