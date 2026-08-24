package com.github.willrees23.command;

import com.github.willrees23.zipline.settings.ZiplineOption;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.Collection;
import java.util.List;

/**
 * The completions for {@code /ziplines edit}.
 *
 * <p>Both providers are named in an annotation and built reflectively by Lamp, so each one has to be
 * public and constructible without arguments. That rules out anything needing the manager, which is
 * why zipline ids are registered on the {@code Lamp} instance instead.
 */
public final class ZiplineSuggestions {

    private ZiplineSuggestions() {
    }

    /**
     * Completes the option being changed with every key {@code /ziplines edit} accepts.
     */
    public static final class Options implements SuggestionProvider<BukkitCommandActor> {

        @Override
        public Collection<String> getSuggestions(ExecutionContext<BukkitCommandActor> context) {
            return ZiplineOption.keys();
        }
    }

    /**
     * Completes the new value with the suggestions belonging to whichever option was named earlier
     * in the command, which Lamp has already read by the time it asks for these.
     */
    public static final class Values implements SuggestionProvider<BukkitCommandActor> {

        @Override
        public Collection<String> getSuggestions(ExecutionContext<BukkitCommandActor> context) {
            String key = context.getResolvedArgumentOrNull("option");
            ZiplineOption option = key == null ? null : ZiplineOption.fromKey(key);
            return option == null ? List.of() : option.getSuggestions();
        }
    }
}
