package technicfan.mpristoast.mixin.v1_21_11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.texture.SpriteContents;
import technicfan.mpristoast.MediaTracker;

// targets = "net.minecraft.client.renderer.texture.SpriteContents$AnimationState"
@Mixin(targets = "net.minecraft.class_7764$class_12298")
public class AnimationStateMixin {
    private boolean musicNotes;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void checkId(SpriteContents contents, @Coerce Object x, Int2ObjectMap<GpuTextureView> y, GpuBufferSlice[] z, CallbackInfo ci) {
        musicNotes = contents.name().getPath().equals("icon/music_notes");
    }

    // method = "tick"
    @Inject(method = "method_76307", at = @At("HEAD"), cancellable = true)
    private void pauseNotes(CallbackInfo ci) {
        if (musicNotes && MediaTracker.show() && !MediaTracker.playing()) {
            ci.cancel();
        }
    }
}
