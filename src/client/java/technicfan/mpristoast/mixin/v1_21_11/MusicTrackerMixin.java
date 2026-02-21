package technicfan.mpristoast.mixin.v1_21_11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.sound.MusicTracker;
import technicfan.mpristoast.MediaTracker;

@Mixin(MusicTracker.class)
public class MusicTrackerMixin {
    @Inject(method = "getCurrentMusicTranslationKey", at = @At("HEAD"), cancellable = true)
    private static void getKey(CallbackInfoReturnable<String> cir) {
        if (MediaTracker.show()) cir.setReturnValue(MediaTracker.track());
    }
}
