package dev.drlink0.simplevoicemoderation;

import net.ess3.api.events.MuteStatusChangeEvent;
import com.earth2me.essentials.User;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EssentialsMuteListenerTest {

    private EssentialsMuteListener listener;

    @Mock private VoiceMuteManager muteManager;
    @Mock private Plugin plugin;
    @Mock private MuteStatusChangeEvent event;
    @Mock private User user;
    @Mock private Player player;

    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        listener = new EssentialsMuteListener(muteManager, plugin);

        when(event.getAffected()).thenReturn(user);
        when(user.getBase()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("TestPlayer");
    }

    @Test
    void onMuteStatusChange_muteTrue_callsMute() {
        when(event.getValue()).thenReturn(true);
        when(event.getTimestamp()).thenReturn(Optional.empty());

        listener.onMuteStatusChange(event);

        verify(muteManager).mute(playerId);
    }

    @Test
    void onMuteStatusChange_muteFalse_callsUnmute() {
        when(event.getValue()).thenReturn(false);

        listener.onMuteStatusChange(event);

        verify(muteManager).unmute(playerId);
    }

    @Test
    void onMuteStatusChange_timedMute_callsScheduleUnmute() {
        long futureTime = System.currentTimeMillis() + 60_000;
        when(event.getValue()).thenReturn(true);
        when(event.getTimestamp()).thenReturn(Optional.of(futureTime));

        listener.onMuteStatusChange(event);

        verify(muteManager).mute(playerId);
        verify(muteManager).scheduleUnmute(playerId, futureTime, plugin);
    }

    @Test
    void onMuteStatusChange_noTimestamp_doesNotCallScheduleUnmute() {
        when(event.getValue()).thenReturn(true);
        when(event.getTimestamp()).thenReturn(Optional.empty());

        listener.onMuteStatusChange(event);

        verify(muteManager).mute(playerId);
        verify(muteManager, never()).scheduleUnmute(any(), anyLong(), any());
    }
}
