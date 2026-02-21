package technicfan.mpristoast;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.ToastManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.interfaces.DBus.NameOwnerChanged;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MediaTracker {
    private static File CONFIG_FILE;
    private static MprisToastConfig CONFIG = new MprisToastConfig();

    private static final String busPrefix = "org.mpris.MediaPlayer2.";

    protected static DBus dbus;
    protected static DBusConnection conn;
    private static MinecraftClient client;
    private static AutoCloseable nameHandler;
    private static HashMap<String, PlayerInfo> players = new HashMap<>();
    private static String currentBusName;
    private static String currentTrack;
    private static boolean playing;

    public static void init() {
        CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(MprisToastClient.MOD_ID + ".json").toFile();
        client = MinecraftClient.getInstance();
        loadConfig();
        currentBusName = busPrefix + CONFIG.getFilter();

        try {
            conn = DBusConnectionBuilder.forSessionBus().build();
            dbus = conn.getRemoteObject("org.freedesktop.DBus", "/", DBus.class);
            if (CONFIG.getFilter().isEmpty() && getActivePlayers().contains(currentBusName + CONFIG.getPreferred())) {
                currentBusName += CONFIG.getPreferred();
            }
            for (String name : getActivePlayers()) {
                if (currentBusName.equals(busPrefix)) {
                    currentBusName = name;
                }
                if (!addPlayer(name, true))
                    currentBusName = busPrefix;
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
            playing = info.getPlaying();
            String track = playing ? String.format("%s - %s", info.getArtist(), info.getTrack()) : null;
            if (show() && !track.equals(currentTrack)) {
                ToastManager manager = client.getToastManager();
                if (manager != null) {
                    if (playing) {
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
        return CONFIG.getEnabled() && playing;
    }

    protected static void setEnabled(boolean enabled) {
        CONFIG.setEnabled(enabled);
        saveToFile();
    }

    protected static void setFilter(String filter) {
        if (filter.equals("None")) {
            filter = "";
        }
        if (!CONFIG.getFilter().equals(filter)) {
            CONFIG.setFilter(filter);
            CONFIG.setPreferred("");
            boolean changed = !currentBusName.equals(busPrefix + filter);
            currentBusName = busPrefix + filter;
            if (!getActivePlayers().contains(currentBusName)) {
                new PlayerInfo(currentBusName);
            } else if (changed) {
                update(players.get(currentBusName));
            }
            saveToFile();
        }
    }

    protected static void setPreferred(String preferred) {
        if (preferred.equals("None")) {
            preferred = "";
        }
        if (!CONFIG.getPreferred().equals(preferred)) {
            CONFIG.setPreferred(preferred);
            CONFIG.setFilter("");
            if (players.containsKey(busPrefix + preferred)
                    && !currentBusName.equals(busPrefix + preferred)) {
                currentBusName = busPrefix + preferred;
                update(players.get(currentBusName));
            } else if (!players.containsKey(currentBusName)) {
                cyclePlayers();
            }
            saveToFile();
        }
    }

    protected static String getPlayer() {
        return currentBusName.equals(busPrefix) ? "None" : players.get(currentBusName).getName();
    }

    protected static String getFilter() {
        return CONFIG.getFilter().isEmpty() ? "None" : CONFIG.getFilter();
    }

    protected static String getPreferred() {
        return CONFIG.getPreferred().isEmpty() ? "None" : CONFIG.getPreferred();
    }

    protected static List<String> getActivePlayers() {
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

    private static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    CONFIG = new Gson().fromJson(reader, MprisToastConfig.class);
                    MprisToastClient.LOGGER.info("MprisTost config loaded");
                }
            } catch (IOException e) {
                MprisToastClient.LOGGER.error(e.toString(), e.fillInStackTrace());
            }
        }
    }

    private static void saveToFile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(gson.toJson(CONFIG));
        } catch (IOException e) {
            MprisToastClient.LOGGER.error(e.toString(), e.fillInStackTrace());
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
            players.get(currentBusName).updateData(getAllValues(currentBusName), null, true);
        }
    }

    protected static void cyclePlayers() {
        if (players.size() > 0 && CONFIG.getFilter().isEmpty()) {
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

    private static boolean addPlayer(String name, boolean existing) {
        Player player;
        AutoCloseable handler;
        synchronized (conn) {
            try {
                player = MediaTracker.conn.getRemoteObject(name, "/org/mpris/MediaPlayer2", Player.class);
                handler = MediaTracker.conn.addSigHandler(PropertiesChanged.class, new PropChangedHandler(name));
            } catch (DBusException e) {
                MprisToastClient.LOGGER.warn(e.toString(), e.fillInStackTrace());
                return false;
            }
        }
        players.put(name, new PlayerInfo(name, player, handler, existing));
        return true;
    }

    protected static Map<String, Variant<?>> getAllValues(String busName) {
        try {
            if (Arrays.asList(dbus.ListNames()).contains(busName)) {
                synchronized (conn) {
                    Properties properties = conn
                            .getRemoteObject(busName, "/org/mpris/MediaPlayer2", Properties.class);
                    Map<String, Variant<?>> data = properties.GetAll("org.mpris.MediaPlayer2.Player");
                    data.putAll(properties.GetAll("org.mpris.MediaPlayer2"));
                    return data;
                }
            }
        } catch (DBusException e) {
            MprisToastClient.LOGGER.warn(e.toString(), e.fillInStackTrace());
        }
        return new HashMap<>();
    }

    private static class NameOwnerChangedHandler implements DBusSigHandler<DBus.NameOwnerChanged> {
        @Override
        public void handle(DBus.NameOwnerChanged signal) {
            if (signal.newOwner.isEmpty() && !signal.oldOwner.isEmpty() && players.containsKey(signal.name)) {
                players.get(signal.name).close();
                players.remove(signal.name);
                if (signal.name.equals(currentBusName)) {
                    if (CONFIG.getFilter().isEmpty()) {
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
                String tempName = currentBusName;
                if (signal.name.equals(busPrefix + CONFIG.getPreferred())
                        || signal.name.equals(busPrefix + CONFIG.getFilter())) {
                    currentBusName = signal.name;
                }
                if (!addPlayer(signal.name, false))
                    currentBusName = tempName;
                if (!players.containsKey(currentBusName)) {
                    cyclePlayers();
                }
            }
        }
    }

    private static class PropChangedHandler extends AbstractPropertiesChangedHandler {
        private String busName;

        private PropChangedHandler(String busName) {
            this.busName = busName;
        }

        @Override
        public void handle(PropertiesChanged signal) {
            // check if signal came from the currently selected player
            if (MediaTracker.dbus.GetNameOwner(busName).equals(signal.getSource())) {
                Map<String, Variant<?>> changed = signal.getPropertiesChanged();
                players.get(busName).updateData(changed, signal.getPropertiesRemoved(), false);
            }
        }
    }
}
