package technicfan.mpristoast;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.ToastManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.DBus.NameOwnerChanged;

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

    protected static void update(PlayerInfo info, boolean newTrack) {
        if (info.getBusName().equals(currentBusName)) {
            playing = info.getPlaying();
            currentTrack = playing ? String.format("%s - %s", info.getArtist(), info.getTrack()) : null;
            if (newTrack && enabled()) {
                ToastManager manager = client.getToastManager();
                if (manager != null) {
                    if (playing) {
                        manager.onMusicTrackStart();
                    } else {
                        manager.onMusicTrackStop();
                    }
                }
            }
        }
    }

    public static String track() {
        return currentTrack;
    }

    public static boolean playing() {
        return playing;
    }

    public static boolean enabled() {
        return CONFIG.getEnabled()
                && (client.options == null ? true : client.options.getShowNowPlayingToast().getValue());
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
                update(players.get(currentBusName), true);
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
                update(players.get(currentBusName), true);
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
                    MprisToastClient.LOGGER.info("MPRIS CustomHud config loaded");
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
            players.get(currentBusName).refreshValues();
        }
    }

    protected static void cyclePlayers() {
        if (players.size() > 0 && CONFIG.getFilter().isEmpty()) {
            List<String> keys = new ArrayList<>(players.keySet());
            int index = keys.indexOf(currentBusName);
            currentBusName = keys.get(index + 1 == keys.size() ? 0 : index + 1);
            update(players.get(currentBusName), true);
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
            if (signal.newOwner.isEmpty() && !signal.oldOwner.isEmpty() && players.containsKey(signal.name)) {
                players.get(signal.name).close();
                players.remove(signal.name);
                if (signal.name.equals(currentBusName)) {
                    if (CONFIG.getFilter().isEmpty()) {
                        if (players.containsKey(busPrefix + CONFIG.getPreferred())) {
                            currentBusName = busPrefix + CONFIG.getPreferred();
                            update(players.get(currentBusName), true);
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
                        || signal.name.equals(busPrefix + CONFIG.getFilter())) {
                    currentBusName = signal.name;
                }
                players.put(signal.name, new PlayerInfo(signal.name, false));
                if (!players.containsKey(currentBusName)) {
                    cyclePlayers();
                }
            }
        }
    }
}
