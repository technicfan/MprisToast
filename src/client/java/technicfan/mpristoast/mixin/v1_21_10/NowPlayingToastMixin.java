package technicfan.mpristoast.mixin.v1_21_10;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.toast.NowPlayingToast;
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
        if (MediaTracker.show()) {
            NowPlayingToastAccessor.musicKey(MediaTracker.track());
        } else {
            NowPlayingToastAccessor.musicKey(key);
        }
    }
}
