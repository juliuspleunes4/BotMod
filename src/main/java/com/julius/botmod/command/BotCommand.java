package com.julius.botmod.command;

import com.julius.botmod.bot.BotManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class BotCommand {

    private static final SuggestionProvider<CommandSourceStack> BOT_NAMES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(BotManager.getBotNames(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bot")
                .requires(source -> source.hasPermission(2)) // OP-niveau 2 vereist

                // /bot spawn <naam>
                .then(Commands.literal("spawn")
                        .then(Commands.argument("naam", StringArgumentType.word())
                                .executes(ctx -> spawnBot(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "naam")
                                ))
                        )
                )

                // /bot remove <naam>
                .then(Commands.literal("remove")
                        .then(Commands.argument("naam", StringArgumentType.word())
                                .suggests(BOT_NAMES)
                                .executes(ctx -> removeBot(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "naam")
                                ))
                        )
                )

                // /bot list
                .then(Commands.literal("list")
                        .executes(ctx -> listBots(ctx.getSource()))
                )
        );
    }

    private static int spawnBot(CommandSourceStack source, String naam) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("/bot spawn kan alleen door een speler worden uitgevoerd."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        Vec3 pos = player.position();

        boolean success = BotManager.spawnBot(naam, level, pos, player.getYRot(), player.getXRot());

        if (success) {
            source.sendSuccess(() -> Component.literal(
                    "Bot '" + naam + "' gespawned op " +
                    (int) pos.x + ", " + (int) pos.y + ", " + (int) pos.z + "."
            ), true);
        } else {
            source.sendFailure(Component.literal("Er bestaat al een bot met de naam '" + naam + "'."));
        }
        return success ? 1 : 0;
    }

    private static int removeBot(CommandSourceStack source, String naam) {
        boolean success = BotManager.removeBot(naam);

        if (success) {
            source.sendSuccess(() -> Component.literal("Bot '" + naam + "' verwijderd."), true);
        } else {
            source.sendFailure(Component.literal("Geen actieve bot gevonden met de naam '" + naam + "'."));
        }
        return success ? 1 : 0;
    }

    private static int listBots(CommandSourceStack source) {
        var namen = BotManager.getBotNames();

        if (namen.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Geen actieve bots."), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Actieve bots (" + namen.size() + "): " + String.join(", ", namen)
            ), false);
        }
        return 1;
    }
}
