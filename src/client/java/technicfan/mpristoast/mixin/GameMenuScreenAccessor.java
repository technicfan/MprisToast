package technicfan.mpristoast.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screen.GameMenuScreen;

@Mixin(GameMenuScreen.class)
public interface GameMenuScreenAccessor {
    @Accessor("showMenu")
    boolean showMenu();
}
