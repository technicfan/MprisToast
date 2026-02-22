package technicfan.mpristoast.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import technicfan.mpristoast.MprisToastClient;

@Mixin(SoundOptionsScreen.class)
public class SoundOptionsScreenMixin {
    @Inject(method = "addOptions", at = @At("TAIL"))
    private void addOptions(CallbackInfo ci) {
        ((OptionsSubScreenAccessor) this).list()
                .addSmall(new OptionInstance[] { MprisToastClient.getEnabledToggle(),
                        MprisToastClient.getReplaceToggle() });
        ((OptionsSubScreenAccessor) this).list()
                .addSmall(new OptionInstance[] { MprisToastClient.getPreferredToggle(),
                        MprisToastClient.getOnlyPreferredToggle() });
    }
}
