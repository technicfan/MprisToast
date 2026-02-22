package technicfan.mpristoast.mixin.v1_21_10;

import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import technicfan.mpristoast.MediaTracker;

@Mixin(NowPlayingToast.class)
public class NowPlayingToastMixin {
    @Redirect(
        method = "tickMusicNotes",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/components/toasts/NowPlayingToast;currentSong:Ljava/lang/String;",
            opcode = Opcodes.PUTSTATIC
        )
    )
    private static void currentSong(String key) {
        if (MediaTracker.show()) {
            NowPlayingToastAccessor.currentSong(MediaTracker.track());
        } else {
            NowPlayingToastAccessor.currentSong(key);
        }
    }
}
