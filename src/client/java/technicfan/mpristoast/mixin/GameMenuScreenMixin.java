package technicfan.mpristoast.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screen.GameMenuScreen;
import technicfan.mpristoast.MediaTracker;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {
    @Inject(method = "shouldShowNowPlayingToast", at = @At("HEAD"), cancellable = true)
    public void showToast(CallbackInfoReturnable<Boolean> cir) {
        if (MediaTracker.enabled()) {
            cir.setReturnValue(((GameMenuScreenAccessor) this).showMenu() && MediaTracker.playing());
        }
    }
}
