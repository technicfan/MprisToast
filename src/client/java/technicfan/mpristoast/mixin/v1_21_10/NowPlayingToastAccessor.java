package technicfan.mpristoast.mixin.v1_21_10;

import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NowPlayingToast.class)
public interface NowPlayingToastAccessor {
    @Accessor("currentSong")
    static void currentSong(String key) {
    }
}
