package technicfan.mpristoast.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.toast.ToastManager;
import technicfan.mpristoast.MediaTracker;

@Mixin(MusicTracker.class)
public class MusicTrackerMixin {
    @Redirect(
        method = {
            "play",
            "tryShowToast"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/toast/ToastManager;onMusicTrackStart()V"
        )
    )
    private void preventShowToast(ToastManager manager) {
        if (!MediaTracker.enabled()) {
            manager.onMusicTrackStart();
        }
    }

    @Redirect(
        method = {
            "play",
            "tryShowToast"
        },
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/sound/MusicTracker;shownToast:Z",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void showToastOverride(MusicTracker tracker, boolean shown) {
        if (!MediaTracker.enabled()) {
            ((MusicTrackerAccessor) tracker).shownToast(shown);
        }
    }
}
