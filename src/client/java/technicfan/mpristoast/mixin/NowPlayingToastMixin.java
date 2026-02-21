package technicfan.mpristoast.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.toast.NowPlayingToast;
import net.minecraft.text.Text;
import technicfan.mpristoast.MediaTracker;

@Mixin(NowPlayingToast.class)
public class NowPlayingToastMixin {
    @Inject(method = "getMusicText", at = @At("HEAD"), cancellable = true)
    private static void getMusicText(CallbackInfoReturnable<Text> cir) {
        if (MediaTracker.show()) cir.setReturnValue(Text.of(MediaTracker.track()));
    }
}
