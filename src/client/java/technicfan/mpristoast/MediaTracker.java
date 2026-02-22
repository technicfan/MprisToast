package technicfan.mpristoast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.sound.SoundCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.DBus.NameOwnerChanged;

public class MediaTracker {
    private static MprisToastConfig CONFIG;

    private static final String busPrefix = "org.mpris.MediaPlayer2.";

    protected static DBus dbus;
    protected static DBusConnection conn;
    private static MinecraftClient client;
    private static AutoCloseable nameHandler;
    private static HashMap<String, PlayerInfo> players = new HashMap<>();
    private static String currentBusName = busPrefix;
    private static String currentTrack;
    private static boolean active;

    protected static void init(MinecraftClient minecraft, MprisToastConfig config) {
        client = minecraft;
        CONFIG = config;

        try {
            conn = DBusConnectionBuilder.forSessionBus().build();
            dbus = conn.getRemoteObject("org.freedesktop.DBus", "/", DBus.class);
            if (CONFIG.getOnlyPreferred() ||
                    getActivePlayers().contains(currentBusName + CONFIG.getPreferred())) {
                currentBusName += CONFIG.getPreferred();
            }
            for (String name : getActivePlayers()) {
                if (currentBusName.equals(busPrefix)) {
                    currentBusName = name;
                }
                players.put(name, new PlayerInfo(name, true));
            }
            if (!players.containsKey(currentBusName))
                new PlayerInfo(currentBusName);
            // listen for name owner changes to reset the values in case the player
            // terminates
            nameHandler = conn.addSigHandler(NameOwnerChanged.class, new NameOwnerChangedHandler());

        } catch (Exception e) {
            MprisToastClient.LOGGER.error(e.toString(), e.fillInStackTrace());
        }
    }

    protected static void update(PlayerInfo info) {
        if (info.getBusName().equals(currentBusName)) {
            active = info.getPlaying();
            String track = active ? info.getArtist().equals("") ? info.getTrack()
                    : String.format("%s - %s", info.getArtist(), info.getTrack()) : null;
            if (CONFIG.getEnabled() && active && !track.equals(currentTrack)) {
                ToastManager manager = client.getToastManager();
                if (manager != null) {
                    if (active) {
                        manager.onMusicTrackStart();
                    } else {
                        manager.onMusicTrackStop();
                    }
                }
            }
            currentTrack = track;
        }
    }

    public static String track() {
        return currentTrack;
    }

    public static boolean show() {
        return CONFIG.getEnabled() &&
                ((active || CONFIG.getReplace()) || (!CONFIG.getReplace() &&
                        client.options.getSoundVolume(SoundCategory.MUSIC) <= 0));
    }

    protected static void updatePreferred() {
        if ((players.containsKey(busPrefix + CONFIG.getPreferred()) || CONFIG.getOnlyPreferred())
                && !currentBusName.equals(busPrefix + CONFIG.getPreferred())) {
            currentBusName = busPrefix + CONFIG.getPreferred();
            if (players.containsKey(currentBusName)) {
                update(players.get(currentBusName));
            } else {
                new PlayerInfo(currentBusName);
            }
        } else if (!players.containsKey(currentBusName)) {
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
        List<String> players = getActivePlayers();
        players.replaceAll(p -> p.replaceAll(busPrefix, ""));
        if (players.contains(CONFIG.getPreferred())) {
            return Stream.concat(Stream.of(""), players.stream());
        } else {
            return Stream.concat(Stream.of("", CONFIG.getPreferred()), players.stream());
        }
    }

    protected static void close() {
        try {
            MprisToastClient.LOGGER.info("Closing DBus connection and signal listeners");
            if (nameHandler != null)
                nameHandler.close();
            for (String name : players.keySet()) {
                players.get(name).close();
            }
            if (conn != null)
                conn.close();
        } catch (Exception e) {
            MprisToastClient.LOGGER.error(e.toString(), e.fillInStackTrace());
        }
    }

    protected static void refresh() {
        if (players.containsKey(currentBusName)) {
            players.get(currentBusName).refresh();
        }
    }

    protected static void cyclePlayers() {
        if (players.size() > 0 && !CONFIG.getOnlyPreferred()) {
            List<String> keys = new ArrayList<>(players.keySet());
            int index = keys.indexOf(currentBusName);
            currentBusName = keys.get(index + 1 == keys.size() ? 0 : index + 1);
            update(players.get(currentBusName));
        }
    }

    protected static void playPause() {
        if (players.size() > 0) {
            Player player = players.get(currentBusName).getPlayer();
            if (player != null)
                player.PlayPause();
        }
    }

    protected static void play() {
        if (players.size() > 0) {
            Player player = players.get(currentBusName).getPlayer();
            if (player != null)
                player.Play();
        }
    }

    protected static void pause() {
        if (players.size() > 0) {
            Player player = players.get(currentBusName).getPlayer();
            if (player != null)
                player.Pause();
        }
    }

    protected static void next() {
        if (players.size() > 0) {
            Player player = players.get(currentBusName).getPlayer();
            if (player != null)
                player.Next();
        }
    }

    protected static void previous() {
        if (players.size() > 0) {
            Player player = players.get(currentBusName).getPlayer();
            if (player != null)
                player.Previous();
        }
    }

    private static class NameOwnerChangedHandler implements DBusSigHandler<DBus.NameOwnerChanged> {
        @Override
        public void handle(DBus.NameOwnerChanged signal) {
            if (signal.newOwner.isEmpty() && !signal.oldOwner.isEmpty()
                    && players.containsKey(signal.name)) {
                players.get(signal.name).close();
                players.remove(signal.name);
                if (signal.name.equals(currentBusName)) {
                    if (!CONFIG.getOnlyPreferred()) {
                        if (players.containsKey(busPrefix + CONFIG.getPreferred())) {
                            currentBusName = busPrefix + CONFIG.getPreferred();
                            update(players.get(currentBusName));
                        } else {
                            cyclePlayers();
                        }
                    } else {
                        currentBusName = busPrefix;
                    }
                }
            } else if (!signal.newOwner.isEmpty() && signal.oldOwner.isEmpty()
                    && signal.name.startsWith(busPrefix)) {
                if (signal.name.equals(busPrefix + CONFIG.getPreferred())
                        || !(players.containsKey(currentBusName) || CONFIG.getOnlyPreferred())) {
                    currentBusName = signal.name;
                }
                players.put(signal.name, new PlayerInfo(signal.name, false));
            }
        }
    }
}
