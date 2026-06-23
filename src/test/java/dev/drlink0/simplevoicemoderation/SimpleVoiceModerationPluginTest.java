package dev.drlink0.simplevoicemoderation;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleVoiceModerationPluginTest {

    private SimpleVoiceModerationPlugin pluginInstance;
    private VoiceMuteManager muteManager;

    @Mock private MicrophonePacketEvent micEvent;
    @Mock private VoicechatConnection connection;
    @Mock private de.maxhenkel.voicechat.api.ServerPlayer voicechatPlayer;
    @Mock private Command command;
    @Mock private CommandSender sender;
    @Mock private Player targetPlayer;
    @Mock private Server server;

    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pluginInstance = mock(SimpleVoiceModerationPlugin.class, CALLS_REAL_METHODS);
        muteManager = new VoiceMuteManager();
        pluginInstance.setVoiceMuteManager(muteManager);
    }

    // --- onMicrophonePacket tests ---

    @Test
    void onMicrophonePacket_mutedPlayer_cancelsEvent() {
        when(micEvent.getSenderConnection()).thenReturn(connection);
        when(connection.getPlayer()).thenReturn(voicechatPlayer);
        when(voicechatPlayer.getUuid()).thenReturn(playerId);
        muteManager.mute(playerId);

        pluginInstance.onMicrophonePacket(micEvent);

        verify(micEvent).cancel();
    }

    @Test
    void onMicrophonePacket_unmutedPlayer_doesNotCancel() {
        when(micEvent.getSenderConnection()).thenReturn(connection);
        when(connection.getPlayer()).thenReturn(voicechatPlayer);
        when(voicechatPlayer.getUuid()).thenReturn(playerId);

        pluginInstance.onMicrophonePacket(micEvent);

        verify(micEvent, never()).cancel();
    }

    @Test
    void onMicrophonePacket_nullSenderConnection_doesNothing() {
        when(micEvent.getSenderConnection()).thenReturn(null);

        pluginInstance.onMicrophonePacket(micEvent);

        verify(micEvent, never()).cancel();
    }

    // --- onCommand tests ---

    @Test
    void onCommand_noArgs_showsInfo() {
        when(command.getName()).thenReturn("svm");

        boolean result = pluginInstance.onCommand(sender, command, "svm", new String[]{});

        assertTrue(result);
        verify(sender, times(2)).sendMessage(any(Component.class));
    }

    @Test
    void onCommand_vcmute_mutesTarget() {
        when(command.getName()).thenReturn("svm");
        when(sender.hasPermission("simplevoicemoderation.vcmute")).thenReturn(true);
        when(pluginInstance.getServer()).thenReturn(server);
        when(server.getPlayer("TestPlayer")).thenReturn(targetPlayer);
        when(targetPlayer.getUniqueId()).thenReturn(playerId);
        when(targetPlayer.getName()).thenReturn("TestPlayer");

        boolean result = pluginInstance.onCommand(sender, command, "svm",
                new String[]{"vcmute", "TestPlayer"});

        assertTrue(result);
        assertTrue(muteManager.isMuted(playerId));
        verify(sender).sendMessage(any(Component.class));
        verify(targetPlayer).sendMessage(any(Component.class));
    }

    @Test
    void onCommand_vcunmute_unmutesTarget() {
        when(command.getName()).thenReturn("svm");
        when(sender.hasPermission("simplevoicemoderation.vcmute")).thenReturn(true);
        when(pluginInstance.getServer()).thenReturn(server);
        when(server.getPlayer("TestPlayer")).thenReturn(targetPlayer);
        when(targetPlayer.getUniqueId()).thenReturn(playerId);
        when(targetPlayer.getName()).thenReturn("TestPlayer");

        muteManager.mute(playerId);

        boolean result = pluginInstance.onCommand(sender, command, "svm",
                new String[]{"vcunmute", "TestPlayer"});

        assertTrue(result);
        assertFalse(muteManager.isMuted(playerId));
    }

    @Test
    void onCommand_vcmute_withoutPermission_denied() {
        when(command.getName()).thenReturn("svm");
        when(sender.hasPermission("simplevoicemoderation.vcmute")).thenReturn(false);

        boolean result = pluginInstance.onCommand(sender, command, "svm",
                new String[]{"vcmute", "TestPlayer"});

        assertTrue(result);
        assertFalse(muteManager.isMuted(playerId));
        verify(sender).sendMessage(any(Component.class));
    }

    @Test
    void onCommand_vcmute_noPlayerArg_showsUsage() {
        when(command.getName()).thenReturn("svm");
        when(sender.hasPermission("simplevoicemoderation.vcmute")).thenReturn(true);

        boolean result = pluginInstance.onCommand(sender, command, "svm",
                new String[]{"vcmute"});

        assertTrue(result);
        verify(sender).sendMessage(any(Component.class));
    }

    @Test
    void onCommand_unknownSubcommand_showsError() {
        when(command.getName()).thenReturn("svm");

        boolean result = pluginInstance.onCommand(sender, command, "svm",
                new String[]{"unknown"});

        assertTrue(result);
        verify(sender).sendMessage(any(Component.class));
    }

    @Test
    void onCommand_nonSvmCommand_returnsFalse() {
        when(command.getName()).thenReturn("other");

        boolean result = pluginInstance.onCommand(sender, command, "other", new String[]{});

        assertFalse(result);
    }
}
