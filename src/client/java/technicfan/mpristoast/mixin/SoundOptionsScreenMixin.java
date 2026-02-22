package technicfan.mpristoast.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import technicfan.mpristoast.MprisToastClient;

@Mixin(SoundOptionsScreen.class)
public class SoundOptionsScreenMixin {
    @Inject(method = "addOptions", at = @At("TAIL"))
    private void addOptions(CallbackInfo ci) {
        ((GameOptionsScreenAccessor) this).body()
                .addAll(new SimpleOption[] { MprisToastClient.getEnabledToggle(),
                        MprisToastClient.getReplaceToggle() });
        ((GameOptionsScreenAccessor) this).body()
                .addAll(new SimpleOption[] { MprisToastClient.getPreferredToggle(),
                        MprisToastClient.getOnlyPreferredToggle() });
    }
}
