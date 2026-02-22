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
import com.mojang.serialization.Codec;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MprisToastClient implements ClientModInitializer {
    public static final String MOD_ID = "mpristoast";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private final static Category MOD_CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, MOD_ID));
    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve(MprisToastClient.MOD_ID + ".json").toFile();

    private static MprisToastConfig CONFIG = new MprisToastConfig();
    private static SimpleOption<Boolean> enabledToggle;
    private static SimpleOption<Boolean> replaceToggle;
    private static SimpleOption<String> preferredToggle;
    private static SimpleOption<Boolean> onlyPreferredToggle;

    @Override
    public void onInitializeClient() {
        loadConfig();
        registerKeybindings();
        MediaTracker.init(MinecraftClient.getInstance(), CONFIG);
        createToggles();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            MediaTracker.close();
        });
    }

    private static void registerKeybindings() {
        KeyBinding playPauseBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.playpause",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));
        KeyBinding nextBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.next",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));
        KeyBinding prevBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.prev",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));
        KeyBinding refreshBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.refresh",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));
        KeyBinding cycleBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.cycle",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (playPauseBinding.wasPressed()) {
                MediaTracker.playPause();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (nextBinding.wasPressed()) {
                MediaTracker.next();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (prevBinding.wasPressed()) {
                MediaTracker.previous();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (refreshBinding.wasPressed()) {
                MediaTracker.refresh();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cycleBinding.wasPressed()) {
                MediaTracker.cyclePlayers();
            }
        });
    }

    private static void createToggles() {
        enabledToggle = SimpleOption.ofBoolean("mpristoast.option.enable", SimpleOption.emptyTooltip(),
                CONFIG.getEnabled(), (value) -> {
                    setEnabled(value);
                });
        replaceToggle = SimpleOption.ofBoolean("mpristoast.option.replace",
                SimpleOption.constantTooltip(Text.translatable("mpristoast.option.replace.tooltip")),
                CONFIG.getReplace(), (value) -> {
                    setReplace(value);
                });
        preferredToggle = new SimpleOption<String>("mpristoast.option.preferred",
                SimpleOption.constantTooltip(Text.translatable("mpristoast.option.preferred.tooltip")),
                (optionText, value) -> {
                    if ("".equals(value)) {
                        return Text.translatable("mpristoast.option.preferred.default");
                    } else {
                        return Text.literal(value);
                    }
                }, new SimpleOption.LazyCyclingCallbacks<String>(() -> MediaTracker.getPlayerStream().toList(),
                        (value) -> Optional.of(value), Codec.STRING),
                CONFIG.getPreferred(), (value) -> {
                    setPreferred(value);
                });
        onlyPreferredToggle = SimpleOption.ofBoolean("mpristoast.option.only_preferred",
                SimpleOption.constantTooltip(Text.translatable("mpristoast.option.only_preferred.tooltip")),
                CONFIG.getOnlyPreferred(), (value) -> {
                    setOnlyPreferred(value);
                });
    }

    public static SimpleOption<Boolean> getEnabledToggle() {
        return enabledToggle;
    }

    public static SimpleOption<Boolean> getReplaceToggle() {
        return replaceToggle;
    }

    public static SimpleOption<String> getPreferredToggle() {
        return preferredToggle;
    }

    public static SimpleOption<Boolean> getOnlyPreferredToggle() {
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
                LOGGER.error(e.toString(), e.fillInStackTrace());
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
