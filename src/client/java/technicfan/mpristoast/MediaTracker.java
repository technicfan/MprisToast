package technicfan.mpristoast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.sounds.SoundSource;

import org.endlesssource.mediainterface.SystemMediaFactory;
import org.endlesssource.mediainterface.api.MediaSession;
import org.endlesssource.mediainterface.api.MediaSessionListener;
import org.endlesssource.mediainterface.api.NowPlaying;
import org.endlesssource.mediainterface.api.PlaybackState;
import org.endlesssource.mediainterface.api.SystemMediaInterface;
import org.endlesssource.mediainterface.api.SystemMediaOptions;

public class MediaTracker {
    private static Config CONFIG;

    public static final int maxWidth = 175;
    protected static final float pixelPerMs = 1f / 96;

    private static SystemMediaInterface media;
    private static Minecraft client;
    private static MediaSessionListener listener = new MediaSessionHandler();
    private static ConcurrentMap<String, String> sessions = new ConcurrentHashMap<>();
    private static Track currentTrack;

    protected static void init(Minecraft minecraft, Config config) {
        client = minecraft;
        CONFIG = config;

        try {
            media = SystemMediaFactory.createSystemInterface(SystemMediaOptions.defaults()
                    .withPositionUpdatesEnabled(false));
            for (MediaSession session : media.getAllSessions()) {
                sessions.put(session.getSessionId(), session.getApplicationName());
            }
            if (sessions.containsKey(CONFIG.getPreferred())) {
                setCurrentTrack(new Track(CONFIG.getPreferred()));
            } else {
                for (String name : sessions.keySet()) {
                    if (currentTrack == null) {
                        setCurrentTrack(new Track(name));
                    }
                }
            }

            media.addSessionListener(listener);
        } catch (Exception e) {
            MprisToastClient.LOGGER.error("Event-driven example failed", e);
        }
    }

    private static void setCurrentTrack(Track newTrack) {
        MediaSession session;
        if (currentTrack != null) {
            session = getSessionById(currentTrack.sessionId());
            if (session != null) {
                session.removeListener(listener);
            }
        }
        if (newTrack != null) {
            session = getSessionById(newTrack.sessionId());
            if (session != null) {
                session.addListener(listener);
            }
        }
        currentTrack = newTrack;
    }

    protected static MediaSession getSessionById(String id) {
        if (media != null) {
            for (MediaSession session : media.getAllSessions()) {
                if (session.getSessionId().equals(id)) {
                    return session;
                }
            }
        }
        MprisToastClient.LOGGER.info(":skull:");
        return null;
    }

    private static void showToast() {
        if (CONFIG.getEnabled() && currentTrack != null && currentTrack.changed()) {
            ToastManager manager = client.getToastManager();
            if (manager != null) {
                if (!currentTrack.name().isEmpty()) {
                    manager.showNowPlayingToast();
                } else {
                    manager.hideNowPlayingToast();
                }
            }
            currentTrack = currentTrack.update();
        }
    }

    public static float currentScrollOffset(int width) {
        return currentTrack != null ? currentTrack.currentScrollOffset(width) : 0;
    }

    public static String track() {
        return currentTrack != null && !currentTrack.name().isEmpty() ? currentTrack.name() : null;
    }

    public static boolean show() {
        return CONFIG.getEnabled() &&
                (((currentTrack != null && !currentTrack.name().isEmpty()) || CONFIG.getReplace()) || (!CONFIG.getReplace() &&
                        client.options.getFinalSoundSourceVolume(SoundSource.MUSIC) <= 0));
    }

    public static boolean playing() {
        return currentTrack != null ? currentTrack.playing() : false;
    }

    protected static void setConfig(Config config) {
        CONFIG = config;
    }

    protected static Config getConfig() {
        return CONFIG;
    }

    protected static void updatePreferred() {
        if (sessions.containsKey(CONFIG.getPreferred())) {
            if (currentTrack == null || !currentTrack.sessionId().equals(CONFIG.getPreferred())) {
                setCurrentTrack(new Track(CONFIG.getPreferred()));
                showToast();
            }
        } else if (CONFIG.getOnlyPreferred()) {
            setCurrentTrack(null);
        } else if (currentTrack == null) {
            cyclePlayers();
        }
    }

    protected static Stream<String> getPlayerStream() {
        List<String> players = new ArrayList<>(sessions.keySet());
        if (CONFIG.getPreferred().isEmpty() || players.contains(CONFIG.getPreferred())) {
            return Stream.concat(Stream.of(""), players.stream());
        } else {
            return Stream.concat(Stream.of("", CONFIG.getPreferred()), players.stream());
        }
    }

    protected static void close() {
        try {
            MprisToastClient.LOGGER.info("Closing Media connection");
            if (media != null)
                media.close();
        } catch (Exception e) {
            MprisToastClient.LOGGER.warn(e.toString(), e.fillInStackTrace());
        }
    }

    protected static void refresh() {
        if (currentTrack != null) {
            currentTrack = currentTrack.refresh();
            showToast();
        }
    }

    protected static void cyclePlayers() {
        if (sessions.size() > 0 && !CONFIG.getOnlyPreferred()) {
            List<String> keys = new ArrayList<>(sessions.keySet());
            int index = keys.indexOf(currentTrack == null ? "" : currentTrack.sessionId());
            int newIndex = index + 1 == sessions.size() ? 0 : index + 1;
            if (index != newIndex) {
                setCurrentTrack(new Track(keys.get(newIndex)));
                showToast();
            }
        } else if (sessions.size() == 0) {
            setCurrentTrack(null);
        }
    }

    protected static void playPause() {
        if (currentTrack != null) {
            currentTrack.playPause();
        }
    }

    protected static void play() {
        if (currentTrack != null) {
            currentTrack.play();
        }
    }

    protected static void pause() {
        if (currentTrack != null) {
            currentTrack.pause();
        }
    }

    protected static void next() {
        if (currentTrack != null) {
            currentTrack.next();
        }
    }

    protected static void previous() {
        if (currentTrack != null) {
            currentTrack.previous();
        }
    }

    protected static String getDisplayName(String name) {
        if (sessions.containsKey(name)) {
            return sessions.get(name).replaceFirst("\\.exe$", "");
        } else if (name.equals(CONFIG.getPreferred()) && !CONFIG.getDisplayName().isEmpty()) {
            return CONFIG.getDisplayName();
        } else {
            return "";
        }
    }

    private static class MediaSessionHandler implements MediaSessionListener {
        @Override
        public void onSessionAdded(MediaSession session) {
            sessions.put(session.getSessionId(), session.getApplicationName());
            if (session.getSessionId().equals(CONFIG.getPreferred())
                    || (currentTrack == null && !CONFIG.getOnlyPreferred())) {
                setCurrentTrack(new Track(session.getSessionId()));
                showToast();
            }
        }

        @Override
        public void onSessionRemoved(String sessionId) {
            sessions.remove(sessionId);
            if (currentTrack != null && sessionId.equals(currentTrack.sessionId())) {
                if (!CONFIG.getOnlyPreferred()) {
                    if (sessions.containsKey(CONFIG.getPreferred())) {
                        setCurrentTrack(new Track(CONFIG.getPreferred()));
                        showToast();
                    } else {
                        cyclePlayers();
                    }
                } else {
                    setCurrentTrack(null);
                }
            }
        }

        @Override
        public void onNowPlayingChanged(MediaSession session, Optional<NowPlaying> nowPlaying) {
            if (nowPlaying.isPresent()) {
                currentTrack = currentTrack.update(nowPlaying.get());
                showToast();
            }
        }

        @Override
        public void onPlaybackStateChanged(MediaSession session, PlaybackState state) {
            currentTrack = currentTrack.update(state.equals(PlaybackState.PLAYING));
        }
    }
}
