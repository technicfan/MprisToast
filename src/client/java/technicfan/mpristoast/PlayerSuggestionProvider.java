package technicfan.mpristoast;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.concurrent.CompletableFuture;

public class PlayerSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("None");

        for (String name : MediaTracker.getActivePlayers()) {
            builder.suggest(name.replace("org.mpris.MediaPlayer2.", ""));
        }

        return builder.buildFuture();
    }
}
