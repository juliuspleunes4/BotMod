package com.julius.botmod.command;

import com.julius.botmod.BotMod;
import com.julius.botmod.bot.BotManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class BotCommand {

    private static final String GITHUB = "https://github.com/juliuspleunes4/BotMod";
    private static final String AUTHOR = "Julius Pleunes";
    private static final String DIVIDER = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

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

                // /bot killall
                .then(Commands.literal("killall")
                        .executes(ctx -> killAllBots(ctx.getSource()))
                )

                // /bot help
                .then(Commands.literal("help")
                        .executes(ctx -> showHelp(ctx.getSource()))
                )

                .executes(ctx -> showHelp(ctx.getSource()))
        );
    }

    private static int spawnBot(CommandSourceStack source, String name) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(formatMessage("/bot spawn can only be executed by a player."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        Vec3 pos = player.position();

        boolean success = BotManager.spawnBot(name, level, pos, player.getYRot(), player);

        if (success) {
            source.sendSuccess(() -> formatMessage(
                    "Bot '" + name + "' spawned at " +
                    (int) pos.x + ", " + (int) pos.y + ", " + (int) pos.z + "."
            ), true);
        } else {
            source.sendFailure(formatMessage("A bot with the name '" + name + "' already exists."));
        }
        return success ? 1 : 0;
    }

    private static int removeBot(CommandSourceStack source, String name) {
        boolean success = BotManager.removeBot(name);

        if (success) {
            source.sendSuccess(() -> formatMessage("Bot '" + name + "' removed."), true);
        } else {
            source.sendFailure(formatMessage("No active bot found with the name '" + name + "'."));
        }
        return success ? 1 : 0;
    }

    private static int listBots(CommandSourceStack source) {
        var names = BotManager.getBotNames();

        if (names.isEmpty()) {
            source.sendSuccess(() -> formatMessage("No active bots."), false);
        } else {
            source.sendSuccess(() -> formatMessage(
                    "Active bots (" + names.size() + "): " + String.join(", ", names)
            ), false);
        }
        return 1;
    }

    private static int killAllBots(CommandSourceStack source) {
        int count = BotManager.killAll(source.getServer());

        if (count == 0) {
            source.sendSuccess(() -> formatMessage("No bots to remove."), false);
        } else {
            source.sendSuccess(() -> formatMessage(
                    "Removed " + count + " bot" + (count == 1 ? "" : "s") + "."
            ), true);
        }
        return count;
    }

    /** Formats a command response as "[BOTMOD] » message" — blue+bold tag, gray separator, white message. */
    private static MutableComponent formatMessage(String message) {
        return Component.literal("[BOTMOD]")
                .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD)
                .append(Component.literal(" » ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(message).withStyle(ChatFormatting.WHITE));
    }

    private static int showHelp(CommandSourceStack source) {
        send(source, divider());
        send(source, title());
        send(source, subtitle());
        send(source, divider());
        send(source, infoRow("Author ", AUTHOR));
        send(source, infoRow("Version", BotMod.MOD_VERSION));
        send(source, githubRow());
        send(source, divider());
        send(source, sectionHeader("Commands"));
        send(source, commandRow("/bot spawn <name>", "Spawns a bot with your skin at your position"));
        send(source, commandRow("/bot remove <name>", "Removes the named bot"));
        send(source, commandRow("/bot list", "Lists all active bots"));
        send(source, commandRow("/bot killall", "Removes every bot on the server"));
        send(source, commandRow("/bot help", "Show this menu"));
        send(source, divider());

        return 1;
    }

    private static void send(CommandSourceStack source, Component text) {
        source.sendSuccess(() -> text, false);
    }

    private static Component divider() {
        return Component.literal(DIVIDER).withStyle(ChatFormatting.BLUE);
    }

    private static Component title() {
        return Component.literal("          ✦  BotMod  ✦")
                .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD);
    }

    private static Component subtitle() {
        return Component.literal("    Never have to manually AFK farm again")
                .withStyle(ChatFormatting.GRAY);
    }

    private static Component infoRow(String label, String value) {
        return Component.literal(" " + label + "  ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static Component githubRow() {
        Component link = Component.literal(GITHUB)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, GITHUB)));
        return Component.literal(" GitHub  ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(link);
    }

    private static Component sectionHeader(String label) {
        return Component.literal(" " + label).withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD);
    }

    private static Component commandRow(String cmd, String description) {
        return Component.literal("  " + String.format("%-19s", cmd) + "  ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(description).withStyle(ChatFormatting.DARK_GRAY));
    }
}
