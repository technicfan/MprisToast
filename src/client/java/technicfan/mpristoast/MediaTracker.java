package technicfan.mpristoast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.sounds.SoundSource;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.DBus.NameOwnerChanged;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;

public class MediaTracker {
    private static Config CONFIG;

    protected static final String busPrefix = "org.mpris.MediaPlayer2.";

    private static DBus dbus;
    protected static DBusConnection conn;
    private static Minecraft client;
    private static AutoCloseable nameChangedHandler, propertiesChangedHandler;
    private static List<String> busNames = new ArrayList<String>();
    private static Track currentTrack;

    protected static void init(Minecraft minecraft, Config config) {
        client = minecraft;
        CONFIG = config;

        try {
            conn = DBusConnectionBuilder.forSessionBus().build();
            dbus = conn.getRemoteObject("org.freedesktop.DBus", "/", DBus.class);
            busNames = getActivePlayers();
            if (busNames.contains(CONFIG.getBusName())) {
                currentTrack = new Track(CONFIG.getBusName(), true);
            }
            for (String name : busNames) {
                if (currentTrack == null) {
                    currentTrack = new Track(name, true);
                }
            }
            // listen for name owner changes to reset the values in case the player
            // terminates
            nameChangedHandler = conn.addSigHandler(NameOwnerChanged.class, new NameOwnerChangedHandler());
            propertiesChangedHandler = conn.addSigHandler(PropertiesChanged.class, new PropChangedHandler());
        } catch (Exception e) {
            MprisToastClient.LOGGER.error(e.toString(), e.fillInStackTrace());
        }
    }

    private static void showToast() {
        if (CONFIG.getEnabled() && currentTrack != null && currentTrack.changed()) {
            ToastManager manager = client.getToastManager();
            if (manager != null) {
                if (currentTrack.active()) {
                    manager.showNowPlayingToast();
                } else {
                    manager.hideNowPlayingToast();
                }
            }
            currentTrack = currentTrack.update();
        }
    }

    public static String track() {
        return currentTrack != null && currentTrack.active() ? currentTrack.name() : null;
    }

    public static boolean show() {
        return CONFIG.getEnabled() &&
                (((currentTrack != null && currentTrack.active()) || CONFIG.getReplace()) || (!CONFIG.getReplace() &&
                        client.options.getFinalSoundSourceVolume(SoundSource.MUSIC) <= 0));
    }

    protected static void setConfig(Config config) {
        CONFIG = config;
    }

    protected static Config getConfig() {
        return CONFIG;
    }

    protected static void updatePreferred() {
        if (busNames.contains(CONFIG.getBusName())
                && (currentTrack == null || !currentTrack.busName().equals(CONFIG.getBusName()))) {
            currentTrack = new Track(CONFIG.getBusName(), true);
            showToast();
        } else if (CONFIG.getOnlyPreferred()) {
            currentTrack = null;
        } else if (currentTrack == null) {
            cyclePlayers();
        }
    }

    private static List<String> getActivePlayers() {
        List<String> players = new ArrayList<>();
        if (dbus != null) {
            for (String name : dbus.ListNames()) {
                if (name.startsWith(busPrefix)) {
                    players.add(name);
                }
            }
        }
        return players;
    }

    protected static Stream<String> getPlayerStream() {
        List<String> players = new ArrayList<>(busNames);
        players.replaceAll(p -> p.replaceAll(busPrefix, ""));
        if (CONFIG.getPreferred().isEmpty() || players.contains(CONFIG.getPreferred())) {
            return Stream.concat(Stream.of(""), players.stream());
        } else {
            return Stream.concat(Stream.of("", CONFIG.getPreferred()), players.stream());
        }
    }

    protected static void close() {
        try {
            MprisToastClient.LOGGER.info("Closing DBus connection and signal listeners");
            if (nameChangedHandler != null)
                nameChangedHandler.close();
            if (propertiesChangedHandler != null)
                propertiesChangedHandler.close();
            if (conn != null)
                conn.close();
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
        if (busNames.size() > 0 && !CONFIG.getOnlyPreferred()) {
            int index = busNames.indexOf(currentTrack == null ? "" : currentTrack.busName());
            int newIndex = index + 1 == busNames.size() ? 0 : index + 1;
            if (index != newIndex) {
                currentTrack = new Track(busNames.get(newIndex), true);
                showToast();
            }
        } else if (busNames.size() == 0) {
            currentTrack = null;
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

    private static class NameOwnerChangedHandler implements DBusSigHandler<DBus.NameOwnerChanged> {
        @Override
        public void handle(DBus.NameOwnerChanged signal) {
            if (busNames.contains(signal.name) && signal.newOwner.isEmpty() && !signal.oldOwner.isEmpty()) {
                busNames.remove(signal.name);
                if (currentTrack != null && signal.name.equals(currentTrack.busName())) {
                    if (!CONFIG.getOnlyPreferred()) {
                        if (busNames.contains(CONFIG.getBusName())) {
                            currentTrack = new Track(CONFIG.getBusName(), true);
                            showToast();
                        } else {
                            cyclePlayers();
                        }
                    } else {
                        currentTrack = null;
                    }
                }
            } else if (signal.name.startsWith(busPrefix) && !signal.newOwner.isEmpty() && signal.oldOwner.isEmpty()) {
                busNames.add(signal.name);
                if (signal.name.equals(CONFIG.getBusName())
                        || (currentTrack == null && !CONFIG.getOnlyPreferred())) {
                    currentTrack = new Track(signal.name, false);
                    showToast();
                }
            }
        }
    }

    private static class PropChangedHandler extends AbstractPropertiesChangedHandler {
        @Override
        public void handle(PropertiesChanged signal) {
            try {
                // check if signal came from the currently selected player
                if (currentTrack != null && dbus.GetNameOwner(currentTrack.busName()).equals(signal.getSource())) {
                    currentTrack = currentTrack.update(signal.getPropertiesChanged(), signal.getPropertiesRemoved(),
                            false);
                    showToast();
                }
            } catch (DBusExecutionException e) {
            }
        }
    }
}
