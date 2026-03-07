package technicfan.mpristoast.mixin.v1_21_10;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.texture.SpriteContents;
import technicfan.mpristoast.MediaTracker;

@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$Ticker")
public class SpriteContentsMixin {
    private boolean musicNotes;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void checkId(SpriteContents contents, @Coerce Object x, @Coerce Object y, CallbackInfo ci) {
        musicNotes = contents.name().getPath().equals("icon/music_notes");
    }

    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void pauseNotes(CallbackInfo ci) {
        if (musicNotes && MediaTracker.show() && !MediaTracker.playing()) {
            ci.cancel();
        }
    }
}
