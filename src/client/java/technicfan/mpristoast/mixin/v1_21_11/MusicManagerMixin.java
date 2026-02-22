package technicfan.mpristoast.mixin.v1_21_11;

import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import technicfan.mpristoast.MediaTracker;

@Mixin(MusicManager.class)
public class MusicManagerMixin {
    @Inject(method = "getCurrentMusicTranslationKey", at = @At("HEAD"), cancellable = true)
    private static void getCurrentMusicTranslationKey(CallbackInfoReturnable<String> cir) {
        if (MediaTracker.show()) cir.setReturnValue(MediaTracker.track());
    }
}
