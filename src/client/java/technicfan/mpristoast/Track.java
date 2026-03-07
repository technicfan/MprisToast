package technicfan.mpristoast;

import org.endlesssource.mediainterface.api.MediaSession;
import org.endlesssource.mediainterface.api.MediaTransportControls;
import org.endlesssource.mediainterface.api.NowPlaying;
import org.endlesssource.mediainterface.api.PlaybackState;

public class Track {
    private final String sessionId;
    private final MediaTransportControls controls;
    private final String name;
    private final long startTime;
    private final boolean playing;
    private final boolean changed;

    protected Track(String sessionId) {
        this(MediaTracker.getSessionById(sessionId), sessionId);
    }

    private Track(MediaSession session, String busName) {
        this.sessionId = busName;
        if (session != null) {
            this.controls = session.getControls();
            this.name = session.getNowPlaying().isPresent() ? getTrackName(session.getNowPlaying().get()) : "";
            this.playing = controls.getPlaybackState().equals(PlaybackState.PLAYING);
        } else {
            this.controls = null;
            this.name = "";
            this.playing = false;
        }
        this.startTime = System.currentTimeMillis();
        this.changed = true;
    }

    private Track(String busName, MediaTransportControls controls, String name, long startTime, boolean active, boolean changed) {
        this.sessionId = busName;
        this.name = name;
        this.controls = controls;
        this.startTime = startTime;
        this.playing = active;
        this.changed = changed;
    }

    protected String sessionId() {
        return sessionId;
    }

    protected String name() {
        return name;
    }

    protected boolean playing() {
        return playing;
    }

    protected boolean changed() {
        return changed;
    }

    protected void playPause() {
        if (controls != null)
            controls.togglePlayPause();
    }

    protected void play() {
        if (controls != null)
           controls.play();
    }

    protected void pause() {
        if (controls != null)
            controls.pause();
    }

    protected void next() {
        if (controls != null)
            controls.next();
    }

    protected void previous() {
        if (controls != null)
            controls.previous();
    }

    protected Track refresh() {
        String name = "";
        boolean playing = false;
        MediaSession session = MediaTracker.getSessionById(sessionId);
        if (session != null) {
            name = session.getNowPlaying().isPresent() ? getTrackName(session.getNowPlaying().get()) : "";
            playing = controls.getPlaybackState().equals(PlaybackState.PLAYING);
        } else {
            name = "";
            playing = false;
        }
        return update(name, playing);
    }

    private static String getTrackName(NowPlaying info) {
        return info.getArtist().isPresent() ? String.format("%s - %s", info.getArtist().get(), info.getTitle().get()) : info.getTitle().get();
    }

    protected Track update(NowPlaying info) {
        return update(getTrackName(info), playing);
    }

    protected Track update(boolean playing) {
        return update(name, playing);
    }

    private Track update(String name, boolean playing) {
        long startTime = this.startTime;
        boolean changed = !name.equals(this.name);
        if (changed) {
            startTime = System.currentTimeMillis();
        }
        return new Track(sessionId, controls, name, startTime, playing, !name.equals(this.name));
    }

    protected Track update() {
        return new Track(sessionId, controls, name, startTime, playing, false);
    }

    protected float currentScrollOffset(int width) {
        //               2000 + (width - maxWidth) * 96
        //             = 2000 + (width - maxWidth) * 64 + (width - maxWidth) * 32
        //             = 2000 + (width - maxWidth) * 2^6 + (width - maxWidth) * 2^5
        long roundTime = 2000 + ((width - MediaTracker.maxWidth) << 6)
                + ((width - MediaTracker.maxWidth) << 5);
        long time = (System.currentTimeMillis() - startTime) % roundTime - 1000;
        if (time <= 0) {
            return 0;
        } else if (time >= roundTime - 2000) {
            return width - MediaTracker.maxWidth;
        }
        return time * MediaTracker.pixelPerMs;
    }
}
