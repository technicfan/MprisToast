package technicfan.mpristoast.mixin;

import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.sounds.MusicManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import technicfan.mpristoast.MediaTracker;

@Mixin(MusicManager.class)
public class MusicManagerMixin {
    @Redirect(
        method = {
            "startPlaying",
            "showNowPlayingToastIfNeeded"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;showNowPlayingToast()V"
        )
    )
    private void preventShowToast(ToastManager manager) {
        if (!MediaTracker.show()) {
            manager.showNowPlayingToast();
        }
    }

    @Redirect(
        method = {
            "startPlaying",
            "showNowPlayingToastIfNeeded"
        },
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/sounds/MusicManager;toastShown:Z",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void showToastOverride(MusicManager tracker, boolean shown) {
        if (!MediaTracker.show()) {
            ((MusicManagerAccessor) tracker).toastShown(shown);
        }
    }
}
