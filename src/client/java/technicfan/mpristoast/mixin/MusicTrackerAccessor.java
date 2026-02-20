package technicfan.mpristoast.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.sound.MusicTracker;

@Mixin(MusicTracker.class)
public interface MusicTrackerAccessor {
    @Accessor("shownToast")
    void shownToast(boolean shown);
}
