package technicfan.mpristoast.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import technicfan.mpristoast.MediaTracker;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {
    @Redirect(
        method = {
            "rendersNowPlayingToast"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Options;getFinalSoundSourceVolume(Lnet/minecraft/sounds/SoundSource;)F"
        )
    )
    private float bypassMusicVolume(Options options, SoundSource c) {
        if (MediaTracker.show()) {
            return 1;
        } else {
            return options.getFinalSoundSourceVolume(c);
        }
    }
}
