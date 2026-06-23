package dev.drlink0.simplevoicemoderation;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.JoinGroupEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SimpleVoiceModerationPlugin extends JavaPlugin implements VoicechatPlugin, Listener {
    private static final String BYPASS_PERMISSION = "simplevoicemoderation.group.password.bypass";

    private final Set<UUID> passwordBypassesInProgress = new HashSet<>();
    private VoiceMuteManager voiceMuteManager;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<BukkitVoicechatService> provider = getServer()
                .getServicesManager()
                .getRegistration(BukkitVoicechatService.class);

        if (provider == null) {
            getLogger().warning("Simple Voice Chat API service is not available.");
            return;
        }

        provider.getProvider().registerPlugin(this);

        voiceMuteManager = new VoiceMuteManager();

        // Register this class as a Bukkit listener for PlayerJoinEvent
        getServer().getPluginManager().registerEvents(this, this);

        // Conditionally hook into EssentialsX
        if (getServer().getPluginManager().getPlugin("Essentials") != null) {
            try {
                EssentialsMuteListener listener = new EssentialsMuteListener(voiceMuteManager, this);
                getServer().getPluginManager().registerEvents(listener, this);
                syncExistingEssentialsMutes();
                getLogger().info("EssentialsX integration enabled — mutes will sync to voice chat.");
            } catch (NoClassDefFoundError e) {
                getLogger().warning("EssentialsX found but API classes not available. Integration disabled.");
            }
        } else {
            getLogger().info("EssentialsX not found — voice mute sync disabled. Use /svm vcmute instead.");
        }

        getLogger().info("SimpleVoiceModeration enabled.");
    }

    @Override
    public void onDisable() {
        if (voiceMuteManager != null) {
            voiceMuteManager.clear();
        }
        getLogger().info("SimpleVoiceModeration disabled.");
    }

    @Override
    public String getPluginId() {
        return "simple_voice_moderation";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(JoinGroupEvent.class, this::onJoinGroup, 1000);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;

        UUID playerId = sender.getPlayer().getUuid();
        if (voiceMuteManager != null && voiceMuteManager.isMuted(playerId)) {
            event.cancel();
        }
    }

    VoiceMuteManager getVoiceMuteManager() {
        return voiceMuteManager;
    }

    void setVoiceMuteManager(VoiceMuteManager manager) {
        this.voiceMuteManager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (getServer().getPluginManager().getPlugin("Essentials") == null) return;
        try {
            syncPlayerMute(event.getPlayer());
        } catch (NoClassDefFoundError ignored) {
            // EssentialsX API not available
        }
    }

    private void syncPlayerMute(Player player) {
        com.earth2me.essentials.Essentials ess =
            (com.earth2me.essentials.Essentials) getServer().getPluginManager().getPlugin("Essentials");
        if (ess == null) return;

        com.earth2me.essentials.User user = ess.getUser(player);
        if (user != null && user.isMuted()) {
            voiceMuteManager.mute(player.getUniqueId());
            long timeout = user.getMuteTimeout();
            if (timeout > 0) {
                voiceMuteManager.scheduleUnmute(player.getUniqueId(), timeout, this);
            }
            getLogger().info("Synced existing mute for " + player.getName());
        }
    }

    private void syncExistingEssentialsMutes() {
        for (Player player : getServer().getOnlinePlayers()) {
            syncPlayerMute(player);
        }
    }

    private void onJoinGroup(JoinGroupEvent event) {
        Group group = event.getGroup();
        VoicechatConnection connection = event.getConnection();

        if (group == null || connection == null || !group.hasPassword()) {
            return;
        }

        UUID playerUuid = connection.getPlayer().getUuid();
        if (passwordBypassesInProgress.contains(playerUuid)) {
            return;
        }

        Object playerObject = connection.getPlayer().getPlayer();
        if (!(playerObject instanceof Player player) || !player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        if (!event.cancel()) {
            return;
        }

        passwordBypassesInProgress.add(playerUuid);
        try {
            connection.setGroup(group);
        } finally {
            passwordBypassesInProgress.remove(playerUuid);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("svm")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("SimpleVoiceModeration is running.", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Use /svm vcmute <player> or /svm vcunmute <player>", NamedTextColor.GRAY));
            return true;
        }

        if (args[0].equalsIgnoreCase("vcmute") || args[0].equalsIgnoreCase("vcunmute")) {
            if (!sender.hasPermission("simplevoicemoderation.vcmute")) {
                sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /svm " + args[0] + " <player>", NamedTextColor.RED));
                return true;
            }

            Player target = getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }

            boolean mute = args[0].equalsIgnoreCase("vcmute");
            if (mute) {
                voiceMuteManager.mute(target.getUniqueId());
                sender.sendMessage(Component.text("Voice-muted " + target.getName() + ".", NamedTextColor.YELLOW));
                target.sendMessage(Component.text("You have been voice-muted.", NamedTextColor.RED));
            } else {
                voiceMuteManager.unmute(target.getUniqueId());
                sender.sendMessage(Component.text("Voice-unmuted " + target.getName() + ".", NamedTextColor.GREEN));
                target.sendMessage(Component.text("You have been voice-unmuted.", NamedTextColor.GREEN));
            }
            return true;
        }

        sender.sendMessage(Component.text("Unknown subcommand. Use /svm vcmute or /svm vcunmute.", NamedTextColor.RED));
        return true;
    }
}
