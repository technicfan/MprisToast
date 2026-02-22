package technicfan.mpristoast;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;

public class PlayerInfo {
    private String name = "", track = "", trackId = "", album = "", artist = "";
    private boolean existing, playing;

    private Player player;
    private String busName;
    private AutoCloseable propertiesHandler;

    public Player getPlayer() {
        return player;
    }

    public String getBusName() {
        return busName;
    }

    public boolean getPlaying() {
        return playing;
    }

    public String getName() {
        return name;
    }

    public String getTrack() {
        return track;
    }

    public String getTrackId() {
        return trackId;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    PlayerInfo(String name) {
        busName = name;
        resetValues();
    }

    PlayerInfo(String name, boolean existing) {
        busName = name;
        synchronized (MediaTracker.conn) {
            try {
                player = MediaTracker.conn.getRemoteObject(name, "/org/mpris/MediaPlayer2", Player.class);
                propertiesHandler = MediaTracker.conn.addSigHandler(PropertiesChanged.class, new PropChangedHandler());
            } catch (DBusException e) {
                MprisToastClient.LOGGER.warn(e.toString(), e.fillInStackTrace());
            }
        }
        this.existing = existing;
        updateData(getAllValues(), null, true);
    }

    protected void close() {
        try {
            propertiesHandler.close();
        } catch (Exception e) {
            MprisToastClient.LOGGER.warn(e.toString(), e.fillInStackTrace());
        }
        resetValues();
    }

    private void resetValues() {
        name = "";
        track = "";
        trackId = "";
        playing = false;
        album = "";
        artist = "";

        MediaTracker.update(this);
    }

    private Map<String, Variant<?>> getAllValues() {
        try {
            if (Arrays.asList(MediaTracker.dbus.ListNames()).contains(busName)) {
                synchronized (MediaTracker.conn) {
                    Properties properties = MediaTracker.conn
                            .getRemoteObject(busName, "/org/mpris/MediaPlayer2", Properties.class);
                    Map<String, Variant<?>> data = properties.GetAll("org.mpris.MediaPlayer2.Player");
                    data.putAll(properties.GetAll("org.mpris.MediaPlayer2"));
                    return data;
                }
            }
        } catch (DBusException | DBusExecutionException e) {
            MprisToastClient.LOGGER.warn(e.toString(), e.fillInStackTrace());
        }
        return new HashMap<>();
    }

    protected void refresh() {
        updateData(getAllValues(), null, false);
    }

    private void updateData(Map<String, Variant<?>> data, List<String> removed, boolean init) {
        if (!(existing || init)) {
            data = getAllValues();
            removed = null;
            existing = true;
        }
        if (removed != null) {
            for (String property : removed) {
                switch (property) {
                    case "Identity":
                        name = "";
                    case "Metadata": {
                        track = "";
                        trackId = "";
                        album = "";
                        artist = "";
                    }
                }
            }
        }
        if (data.containsKey("Identity")) {
            name = (String) data.get("Identity").getValue();
        }
        if (data.containsKey("PlaybackStatus")) {
            playing = !data.get("PlaybackStatus").getValue().toString().equals("Stopped");
        }
        if (data.containsKey("Metadata")) {
            Map<?, ?> newMetadata = (Map<?, ?>) data
                    .get("Metadata")
                    .getValue();
            Map<String, Object> metadata = new HashMap<>();
            for (Map.Entry<?, ?> entry : newMetadata.entrySet()) {
                metadata.put((String) entry.getKey(), ((Variant<?>) entry.getValue()).getValue());
            }
            updateMetadata(metadata);
        }

        MediaTracker.update(this);
    }

    private void updateMetadata(Map<String, ?> metadata) {
        Object trackObj, trackIdObj, albumObj, artistsObj;
        trackIdObj = metadata.get("mpris:trackid");
        artistsObj = metadata.get("xesam:artist");
        trackObj = metadata.get("xesam:title");
        albumObj = metadata.get("xesam:album");
        if (artistsObj != null && artistsObj instanceof List) {
            List<?> tempList = (List<?>) artistsObj;
            List<String> list = new ArrayList<>();
            for (Object name : tempList) {
                list.add((String) name);
            }
            artist = list.isEmpty() ? "" : list.get(0);
        } else {
            artist = "";
        }
        if (trackObj != null && trackObj instanceof String) {
            track = (String) trackObj;
        } else {
            track = "";
        }
        if (trackIdObj != null && trackIdObj instanceof String) {
            trackId = (String) trackIdObj;
        } else {
            trackId = "";
        }
        if (albumObj != null && albumObj instanceof String) {
            album = (String) albumObj;
        } else {
            album = "";
        }
    }

    private class PropChangedHandler extends AbstractPropertiesChangedHandler {
        @Override
        public void handle(PropertiesChanged signal) {
            try {
                // check if signal came from the currently selected player
                if (MediaTracker.dbus.GetNameOwner(busName).equals(signal.getSource())) {
                    Map<String, Variant<?>> changed = signal.getPropertiesChanged();
                    updateData(changed, signal.getPropertiesRemoved(), false);
                }
            } catch (DBusExecutionException e) {
            }
        }
    }
}
