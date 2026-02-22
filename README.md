# MprisToast

## Important

This mod is Linux only

## Description

This (fabric) mod shows the media currently playing on your system using the vanilla music toast feature on 1.21.10 and 1.21.11 (currently).

To make it work, you also have to enable the toast in vanilla options.

As you might be able to tell, this mod is based on my other mod [MprisCustomHud](https://github.com/technicfan/MprisCustomHud) I made mainly to learn how to use MPRIS (and DBUS) with dbus-java.

This I made to learn a little bit about mixins and because I think it's cool :)

### Controls

There are keybindings for play/pause, next, previous, refresh and cycle through active players.

### Configuration

By default a player is selected from the active ones and the mod is enabled, so you will not see toasts with vanilla music while your other music is playing. To cycle through the currently active ones, use the (only works if "Only preferred source" is disabled).
<br>
All options are located in the vanilla sound settings under the option to enable music toasts in the first place. They all have tooltips that explain what they do.

### Flatpak notice

- when you're running Minecraft in a Flatpak sandbox, you have to add `org.mpris.MediaPlayer2.*` to the list of well known session bus names your launcher can talk to e.g. with [Flatseal](https://github.com/tchx84/flatseal)

### Note

- there will be no older versions, as the music toast didn't exist pre 1.21.10

### Libraries used

- [dbus-java](https://github.com/hypfvieh/dbus-java)
    - Improved version of java DBus library provided by freedesktop.org [https://dbus.freedesktop.org/doc/dbus-java](https://dbus.freedesktop.org/doc/dbus-java)

### License

This Mod is licensed under the MIT License
