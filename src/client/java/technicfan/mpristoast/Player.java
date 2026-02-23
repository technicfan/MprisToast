package technicfan.mpristoast;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
public interface Player extends DBusInterface {
    void PlayPause();

    void Play();

    void Pause();

    void Next();

    void Previous();
}
