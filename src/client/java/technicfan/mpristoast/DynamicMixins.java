package technicfan.mpristoast;

import org.spongepowered.asm.mixin.Mixins;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class DynamicMixins implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        if (FabricLoader.getInstance().getRawGameVersion().equals("1.21.10")) {
            Mixins.addConfiguration("10.mixins.json");
        } else {
            Mixins.addConfiguration("11.mixins.json");
        }
    }
}
