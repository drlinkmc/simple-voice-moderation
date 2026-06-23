package dev.drlink0.simplevoicemoderation;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceMuteManagerTest {

    private VoiceMuteManager manager;

    @Mock
    private Plugin plugin;

    @Mock
    private BukkitScheduler scheduler;

    @Mock
    private BukkitTask bukkitTask;

    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        manager = new VoiceMuteManager();
    }

    @AfterEach
    void tearDown() {
        if (bukkitMock != null) {
            bukkitMock.close();
        }
    }

    private void setupBukkitScheduler() {
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);
    }

    @Test
    void mute_thenIsMuted_returnsTrue() {
        UUID id = UUID.randomUUID();
        manager.mute(id);
        assertTrue(manager.isMuted(id));
    }

    @Test
    void unmute_thenIsMuted_returnsFalse() {
        UUID id = UUID.randomUUID();
        manager.mute(id);
        manager.unmute(id);
        assertFalse(manager.isMuted(id));
    }

    @Test
    void isMuted_unknownUuid_returnsFalse() {
        assertFalse(manager.isMuted(UUID.randomUUID()));
    }

    @Test
    void unmute_cancelsScheduledTask() {
        setupBukkitScheduler();
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong()))
                .thenReturn(bukkitTask);

        UUID id = UUID.randomUUID();
        manager.mute(id);
        long futureTime = System.currentTimeMillis() + 60_000;
        manager.scheduleUnmute(id, futureTime, plugin);
        manager.unmute(id);

        verify(bukkitTask).cancel();
        assertFalse(manager.isMuted(id));
    }

    @Test
    void scheduleUnmute_pastTimestamp_unmutesImmediately() {
        UUID id = UUID.randomUUID();
        manager.mute(id);
        manager.scheduleUnmute(id, System.currentTimeMillis() - 1000, plugin);
        assertFalse(manager.isMuted(id));
    }

    @Test
    void scheduleUnmute_futureTimestamp_createsBukkitTask() {
        setupBukkitScheduler();
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong()))
                .thenReturn(bukkitTask);

        UUID id = UUID.randomUUID();
        manager.mute(id);
        long futureTime = System.currentTimeMillis() + 60_000;
        manager.scheduleUnmute(id, futureTime, plugin);

        verify(scheduler).runTaskLater(any(Plugin.class), any(Runnable.class), anyLong());
        assertTrue(manager.isMuted(id)); // still muted until task fires
    }

    @Test
    void scheduleUnmute_replacesExistingScheduledTask() {
        setupBukkitScheduler();

        BukkitTask firstTask = mock(BukkitTask.class);
        BukkitTask secondTask = mock(BukkitTask.class);
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong()))
                .thenReturn(firstTask, secondTask);

        UUID id = UUID.randomUUID();
        manager.mute(id);
        long futureTime = System.currentTimeMillis() + 60_000;
        manager.scheduleUnmute(id, futureTime, plugin);
        manager.scheduleUnmute(id, futureTime + 30_000, plugin);

        verify(firstTask).cancel();
    }

    @Test
    void clear_removesAllMutesAndCancelsTasks() {
        setupBukkitScheduler();
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong()))
                .thenReturn(bukkitTask);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        manager.mute(id1);
        manager.mute(id2);
        manager.scheduleUnmute(id1, System.currentTimeMillis() + 60_000, plugin);

        manager.clear();

        assertFalse(manager.isMuted(id1));
        assertFalse(manager.isMuted(id2));
        verify(bukkitTask).cancel();
    }
}
