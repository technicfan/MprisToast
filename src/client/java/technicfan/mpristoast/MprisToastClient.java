package technicfan.mpristoast;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MprisToastClient implements ClientModInitializer {
    public static final String MOD_ID = "mpristoast";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final static Category MOD_CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, MOD_ID));

    @Override
    public void onInitializeClient() {
        MediaTracker.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal(MOD_ID)
                        .then(ClientCommandManager.literal("enable")
                                .executes(context -> {
                                    return toggle(context, true);
                                }))
                        .then(ClientCommandManager.literal("disable")
                                .executes(context -> {
                                    return toggle(context, false);
                                }))
                        .then(ClientCommandManager.literal("filter")
                                .then(ClientCommandManager.argument("filter", StringArgumentType.string())
                                        .suggests(new PlayerSuggestionProvider())
                                        .executes(MprisToastClient::updateFilter))
                                .executes(MprisToastClient::queryFilter))
                        .then(ClientCommandManager.literal("preferred")
                                .then(ClientCommandManager.argument("preferred", StringArgumentType.string())
                                        .suggests(new PlayerSuggestionProvider())
                                        .executes(MprisToastClient::updatePreferred))
                                .executes(MprisToastClient::queryPreferred))
                        .then(ClientCommandManager.literal("player")
                                .executes(MprisToastClient::queryPlayer))
                        .then(ClientCommandManager.literal("cycle")
                                .executes(MprisToastClient::cyclePlayers))
                        .then(ClientCommandManager.literal("refresh")
                                .executes(MprisToastClient::refreshPlayer))
                        .then(ClientCommandManager.literal("playpause")
                                .executes(MprisToastClient::playPausePlayer))
                        .then(ClientCommandManager.literal("play")
                                .executes(MprisToastClient::playPlayer))
                        .then(ClientCommandManager.literal("pause")
                                .executes(MprisToastClient::pausePlayer))
                        .then(ClientCommandManager.literal("next")
                                .executes(MprisToastClient::nextPlayer))
                        .then(ClientCommandManager.literal("previous")
                                .executes(MprisToastClient::previousPlayer))));

        KeyBinding playPauseBinding, nextBinding, prevBinding, refreshBinding, cycleBinding;
        playPauseBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.playpause",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));
        nextBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.next",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));
        prevBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.prev",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));
        refreshBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "mpristoast.key.refresh",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                MOD_CATEGORY));
        cycleBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
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

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            MediaTracker.close();
        });
    }

    private static int toggle(CommandContext<FabricClientCommandSource> commandContext, boolean b) {
        MediaTracker.setEnabled(b);
        if (b) {
            commandContext.getSource().sendFeedback(Text.translatable("mpristoast.command.enabled"));
        } else {
            commandContext.getSource().sendFeedback(Text.translatable("mpristoast.command.disabled"));
        }
        return 1;
    }

    private static int refreshPlayer(CommandContext<FabricClientCommandSource> commandContext) {
        CompletableFuture.runAsync(() -> {
            MediaTracker.refresh();
        });
        return 1;
    }

    private static int queryPlayer(CommandContext<FabricClientCommandSource> commandContext) {
        commandContext.getSource()
                .sendFeedback(Text.translatable("mpristoast.command.current_player", MediaTracker.getPlayer()));
        return 1;
    }

    private static int cyclePlayers(CommandContext<FabricClientCommandSource> commandContext) {
        CompletableFuture.runAsync(() -> {
            MediaTracker.cyclePlayers();
        });
        return 1;
    }

    private static int updateFilter(CommandContext<FabricClientCommandSource> commandContext) {
        CompletableFuture.runAsync(() -> {
            MediaTracker.setFilter(StringArgumentType.getString(commandContext, "filter"));
            commandContext.getSource()
                    .sendFeedback(Text.translatable("mpristoast.command.new_filter", MediaTracker.getFilter()));
        });
        return 1;
    }

    private static int queryFilter(CommandContext<FabricClientCommandSource> commandContext) {
        commandContext.getSource()
                .sendFeedback(Text.translatable("mpristoast.command.current_filter", MediaTracker.getFilter()));
        return 1;
    }

    private static int updatePreferred(CommandContext<FabricClientCommandSource> commandContext) {
        CompletableFuture.runAsync(() -> {
            MediaTracker.setPreferred(StringArgumentType.getString(commandContext, "preferred"));
            commandContext.getSource()
                    .sendFeedback(
                            Text.translatable("mpristoast.command.new_preferred", MediaTracker.getPreferred()));
        });
        return 1;
    }

    private static int queryPreferred(CommandContext<FabricClientCommandSource> commandContext) {
        commandContext.getSource()
                .sendFeedback(
                        Text.translatable("mpristoast.command.current_preferred", MediaTracker.getPreferred()));
        return 1;
    }

    private static int playPausePlayer(CommandContext<FabricClientCommandSource> commandContext) {
        MediaTracker.playPause();
        return 1;
    }

    private static int playPlayer(CommandContext<FabricClientCommandSource> commandContext) {
        MediaTracker.play();
        return 1;
    }

    private static int pausePlayer(CommandContext<FabricClientCommandSource> commandContext) {
        MediaTracker.pause();
        return 1;
    }

    private static int nextPlayer(CommandContext<FabricClientCommandSource> commandContext) {
        MediaTracker.next();
        return 1;
    }

    private static int previousPlayer(CommandContext<FabricClientCommandSource> commandContext) {
        MediaTracker.previous();
        return 1;
    }
}
