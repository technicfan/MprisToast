package technicfan.mpristoast.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.toast.NowPlayingToast;
import net.minecraft.text.Text;
import technicfan.mpristoast.MediaTracker;

@Mixin(NowPlayingToast.class)
public class NowPlayingToastMixin {
    @Redirect(
        method = "tick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/toast/NowPlayingToast;musicTranslationKey:Ljava/lang/String;",
            opcode = Opcodes.PUTSTATIC
        )
    )
    private static void title(String key) {
        if (MediaTracker.enabled() && MediaTracker.playing()) {
            NowPlayingToastAccessor.musicKey(MediaTracker.track());
        } else {
            NowPlayingToastAccessor.musicKey(key);
        }
    }

    @Inject(method = "getMusicText", at = @At("HEAD"), cancellable = true)
    private static void getMusicText(CallbackInfoReturnable<Text> cir) {
        if (MediaTracker.enabled()) cir.setReturnValue(Text.of(MediaTracker.track()));
    }
}
