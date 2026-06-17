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
                .requires(source -> source.hasPermission(0))

                // /bot spawn <name>
                .then(Commands.literal("spawn")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> spawnBot(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))
                        )
                )

                // /bot remove <name>
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(BOT_NAMES)
                                .executes(ctx -> removeBot(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))
                        )
                )

                // /bot list
                .then(Commands.literal("list")
                        .executes(ctx -> listBots(ctx.getSource()))
                )
        );
    }

    private static int spawnBot(CommandSourceStack source, String name) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("/bot spawn can only be executed by a player."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        Vec3 pos = player.position();

        boolean success = BotManager.spawnBot(name, level, pos, player.getYRot());

        if (success) {
            source.sendSuccess(() -> Component.literal(
                    "Bot '" + name + "' spawned at " +
                    (int) pos.x + ", " + (int) pos.y + ", " + (int) pos.z + "."
            ), true);
        } else {
            source.sendFailure(Component.literal("A bot with the name '" + name + "' already exists."));
        }
        return success ? 1 : 0;
    }

    private static int removeBot(CommandSourceStack source, String name) {
        boolean success = BotManager.removeBot(name);

        if (success) {
            source.sendSuccess(() -> Component.literal("Bot '" + name + "' removed."), true);
        } else {
            source.sendFailure(Component.literal("No active bot found with the name '" + name + "'."));
        }
        return success ? 1 : 0;
    }

    private static int listBots(CommandSourceStack source) {
        var names = BotManager.getBotNames();

        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active bots."), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Active bots (" + names.size() + "): " + String.join(", ", names)
            ), false);
        }
        return 1;
    }
}
