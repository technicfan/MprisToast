# MprisToast

## Important

This mod is Linux only

## Description

This mod shows the media currently playing on your system using the vanilla music toast feature.

To make it work, you also have to enable the toast in vanilla options.

As you might be able to tell, this mod is based on my other mod [MprisCustomHud](https://github.com/technicfan/MprisCustomHud) I made mainly to learn how to use MPRIS (and DBUS) with dbus-java.

This I made to learn a little bit about mixins and because I think it's cool :)

### Controls

There are keybindings for enable/disable, play/pause, next, previous, refresh and cycle through active players that all have correcsponding commands.

### Configuration

By default a player is selected from the active ones and the mod is enabled, so you will not see toasts with vanilla music. To cycle through the currently active ones, use the `mpristoast cycle` command (only works if no filter is set).
<br>
To enable/disable the mod, use the `mpristoast enable` and `mpristoast disable` commands.
<br>
You can also choose a mpris player if you only want to see that one with the `mpristoast filter <player>` command which will also suggest currently active ones.
<br>
If you still want to see other players but prefer one of them, use `mpristoast preferred <player>` so that one will always be shown if it's active.
<br>
With `mpristoast player`, you get the currently active player, with `mpristoast filter` and `mpristoast preferred` the values for that and with `mpristoast refresh`, you can refresh the variables.

### Flatpak notice

- when you're running Minecraft in a Flatpak sandbox, you have to add `org.mpris.MediaPlayer2.*` to the list of well known session bus names your launcher can talk to e.g. with [Flatseal](https://github.com/tchx84/flatseal)

### Problems/Todo

- it's 1.21.10 only currently and a will expand it 1.21.11 and probably newer versions
    - there will be no older versions, as the music toast didn't exist pre 1.21.10

### Libraries used

- [dbus-java](https://github.com/hypfvieh/dbus-java)
    - Improved version of java DBus library provided by freedesktop.org [https://dbus.freedesktop.org/doc/dbus-java](https://dbus.freedesktop.org/doc/dbus-java)

### License

This Mod is licensed under the MIT License
