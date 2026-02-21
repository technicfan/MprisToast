package technicfan.mpristoast.mixin.v1_21_10;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.toast.NowPlayingToast;

@Mixin(NowPlayingToast.class)
public interface NowPlayingToastAccessor {
    @Accessor("musicTranslationKey")
    static void musicKey(String key) {
    }
}
