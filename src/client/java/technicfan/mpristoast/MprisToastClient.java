package technicfan.mpristoast;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MprisToastClient implements ClientModInitializer {
    public static final String MOD_ID = "mpristoast";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private final static Category MOD_CATEGORY = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, MOD_ID));
    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve(MprisToastClient.MOD_ID + ".json").toFile();

    private static MprisToastConfig CONFIG = new MprisToastConfig();
    private static OptionInstance<Boolean> enabledToggle;
    private static OptionInstance<Boolean> replaceToggle;
    private static OptionInstance<String> preferredToggle;
    private static OptionInstance<Boolean> onlyPreferredToggle;

    @Override
    public void onInitializeClient() {
        loadConfig();
        registerKeybindings();
        MediaTracker.init(Minecraft.getInstance(), CONFIG);
        createToggles();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            MediaTracker.close();
        });
    }

    private static void registerKeybindings() {
        KeyMapping playPauseBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "mpristoast.key.playpause",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                MOD_CATEGORY));
        KeyMapping nextBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "mpristoast.key.next",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                MOD_CATEGORY));
        KeyMapping prevBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "mpristoast.key.prev",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                MOD_CATEGORY));
        KeyMapping refreshBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "mpristoast.key.refresh",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                MOD_CATEGORY));
        KeyMapping cycleBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "mpristoast.key.cycle",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                MOD_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (playPauseBinding.consumeClick()) {
                MediaTracker.playPause();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (nextBinding.consumeClick()) {
                MediaTracker.next();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (prevBinding.consumeClick()) {
                MediaTracker.previous();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (refreshBinding.consumeClick()) {
                MediaTracker.refresh();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cycleBinding.consumeClick()) {
                MediaTracker.cyclePlayers();
            }
        });
    }

    private static void createToggles() {
        enabledToggle = OptionInstance.createBoolean("mpristoast.option.enable", OptionInstance.noTooltip(),
                CONFIG.getEnabled(), (value) -> {
                    setEnabled(value);
                });
        replaceToggle = OptionInstance.createBoolean("mpristoast.option.replace",
                OptionInstance.cachedConstantTooltip(Component.translatable("mpristoast.option.replace.tooltip")),
                CONFIG.getReplace(), (value) -> {
                    setReplace(value);
                });
        preferredToggle = new OptionInstance<String>("mpristoast.option.preferred",
                OptionInstance.cachedConstantTooltip(Component.translatable("mpristoast.option.preferred.tooltip")),
                (optionText, value) -> {
                    if (value.isEmpty()) {
                        return Component.translatable("mpristoast.option.preferred.default");
                    } else {
                        return Component.literal(value);
                    }
                }, new OptionInstance.LazyEnum<String>(() -> MediaTracker.getPlayerStream().toList(),
                        (value) -> Optional.of(value), Codec.STRING),
                CONFIG.getPreferred(), (value) -> {
                    setPreferred(value);
                });
        onlyPreferredToggle = OptionInstance.createBoolean("mpristoast.option.only_preferred",
                OptionInstance.cachedConstantTooltip(Component.translatable("mpristoast.option.only_preferred.tooltip")),
                CONFIG.getOnlyPreferred(), (value) -> {
                    setOnlyPreferred(value);
                });
    }

    public static OptionInstance<Boolean> getEnabledToggle() {
        return enabledToggle;
    }

    public static OptionInstance<Boolean> getReplaceToggle() {
        return replaceToggle;
    }

    public static OptionInstance<String> getPreferredToggle() {
        return preferredToggle;
    }

    public static OptionInstance<Boolean> getOnlyPreferredToggle() {
        return onlyPreferredToggle;
    }

    private static void setEnabled(boolean enabled) {
        CONFIG.setEnabled(enabled);
        MprisToastClient.saveConfig();
    }

    private static void setReplace(boolean replace) {
        CONFIG.setReplace(replace);
        MprisToastClient.saveConfig();
    }

    private static void setOnlyPreferred(boolean onlyPreferred) {
        CONFIG.setOnlyPreferred(onlyPreferred);
        MediaTracker.updatePreferred();
        MprisToastClient.saveConfig();
    }

    private static void setPreferred(String preferred) {
        if (preferred.equals("None")) {
            preferred = "";
        }
        if (!CONFIG.getPreferred().equals(preferred)) {
            CONFIG.setPreferred(preferred);
            MediaTracker.updatePreferred();
            MprisToastClient.saveConfig();
        }
    }

    private static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    CONFIG = new Gson().fromJson(reader, MprisToastConfig.class);
                    LOGGER.info("MprisTost config loaded");
                }
            } catch (IOException e) {
                LOGGER.warn(e.toString(), e.fillInStackTrace());
            }
        }
    }

    protected static void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(gson.toJson(CONFIG));
        } catch (IOException e) {
            LOGGER.error(e.toString(), e.fillInStackTrace());
        }
    }
}
