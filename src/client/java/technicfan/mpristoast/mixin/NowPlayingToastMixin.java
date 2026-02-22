package technicfan.mpristoast.mixin;

import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import technicfan.mpristoast.MediaTracker;

@Mixin(NowPlayingToast.class)
public class NowPlayingToastMixin {
    @Inject(method = "getNowPlayingString", at = @At("HEAD"), cancellable = true)
    private static void getNowPlayingString(CallbackInfoReturnable<Component> cir) {
        if (MediaTracker.show()) cir.setReturnValue(Component.nullToEmpty(MediaTracker.track()));
    }
}
