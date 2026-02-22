package technicfan.mpristoast.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.sound.SoundCategory;
import technicfan.mpristoast.MediaTracker;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {
    @Redirect(
        method = {
            "shouldShowNowPlayingToast"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/option/GameOptions;getSoundVolume(Lnet/minecraft/sound/SoundCategory;)F"
        )
    )
    private float bypassMusicVolume(GameOptions options, SoundCategory c) {
        if (MediaTracker.show()) {
            return 1;
        } else {
            return options.getSoundVolume(c);
        }
    }
}
