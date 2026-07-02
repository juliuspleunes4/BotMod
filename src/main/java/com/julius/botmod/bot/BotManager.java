package com.julius.botmod.bot;

import com.julius.botmod.BotMod;
import com.julius.botmod.entity.BotEntity;
import com.julius.botmod.entity.ModEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.*;

public class BotManager {

    public record BotEntry(
            String name,
            BotEntity bot,
            FakePlayer spawnSimulator,
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {}

    private static final Map<String, BotEntry> activeBots = new HashMap<>();

    /**
     * Spawns a BotEntity with the owner's full skin and force-loads the chunk.
     * Also injects a FakePlayer into the level's player list so the mob spawning
     * engine considers this location "player-occupied".
     *
     * @return true if the bot was successfully spawned, false if the name is already in use
     */
    public static boolean spawnBot(String name, ServerLevel level, Vec3 pos, float yRot, ServerPlayer owner) {
        if (activeBots.containsKey(name)) {
            return false;
        }

        BotEntity bot = ModEntities.BOT.get().create(level);
        if (bot == null) return false;

        bot.setPos(pos.x, pos.y, pos.z);
        bot.setYRot(yRot);
        bot.setOwnerProfile(owner.getGameProfile());
        bot.setBotName(name);
        bot.setCustomName(Component.literal("[Bot]")
                .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD)
                .append(Component.literal(" " + name)));
        bot.setCustomNameVisible(true);

        int cx = BlockPos.containing(pos).getX() >> 4;
        int cz = BlockPos.containing(pos).getZ() >> 4;
        level.setChunkForced(cx, cz, true);
        level.addFreshEntity(bot);

        // FakePlayer added to level.players() so NaturalSpawner.getNearestPlayer()
        // finds a "player" here and allows mobs to spawn around the bot.
        // FakePlayerFactory uses weak refs, so we keep a strong ref in BotEntry.
        String fakeName = name.length() > 16 ? name.substring(0, 16) : name;
        FakePlayer spawnSimulator = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), fakeName));
        spawnSimulator.setPos(pos.x, pos.y, pos.z);
        level.players().add(spawnSimulator);

        activeBots.put(name, new BotEntry(name, bot, spawnSimulator, level, cx, cz));
        BotMod.LOGGER.info("Bot '{}' spawned at ({}, {}, {})", name, (int) pos.x, (int) pos.y, (int) pos.z);
        return true;
    }

    /**
     * Removes an existing bot, releases the forced chunk ticket, and removes
     * the FakePlayer from the level's player list.
     *
     * @return true if the bot was found and removed
     */
    public static boolean removeBot(String name) {
        BotEntry entry = activeBots.remove(name);
        if (entry == null) {
            return false;
        }

        entry.level().setChunkForced(entry.chunkX(), entry.chunkZ(), false);

        if (!entry.bot().isRemoved()) {
            entry.bot().discard();
        }

        entry.level().players().remove(entry.spawnSimulator());

        BotMod.LOGGER.info("Bot '{}' removed", name);
        return true;
    }

    public static boolean isBot(Entity entity) {
        return entity instanceof BotEntity;
    }

    public static Set<String> getBotNames() {
        return Collections.unmodifiableSet(activeBots.keySet());
    }

    public static boolean hasBot(String name) {
        return activeBots.containsKey(name);
    }

    /**
     * Re-registers a bot that survived a server restart. Its chunk was already force-loaded again
     * by vanilla's own forced-chunk persistence before {@code ServerStartedEvent} fires, so the
     * entity is already present in {@code level} — this just rebuilds the in-memory tracking
     * (registry entry + FakePlayer) that {@code /bot list} and {@code /bot remove} rely on.
     */
    public static void reregisterBot(BotEntity bot, ServerLevel level) {
        String name = bot.getBotName();
        if (name.isEmpty() || activeBots.containsKey(name)) {
            return;
        }

        BlockPos pos = bot.blockPosition();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        String fakeName = name.length() > 16 ? name.substring(0, 16) : name;
        FakePlayer spawnSimulator = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), fakeName));
        spawnSimulator.setPos(bot.getX(), bot.getY(), bot.getZ());
        level.players().add(spawnSimulator);

        level.setChunkForced(cx, cz, true);

        activeBots.put(name, new BotEntry(name, bot, spawnSimulator, level, cx, cz));
        BotMod.LOGGER.info("Bot '{}' restored after server restart at ({}, {}, {})", name, pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Removes every bot, including ones left over from before this registry existed
     * (e.g. bots spawned on 1.0.0, which have no stored {@code BotName} and were never
     * re-registered — {@link #reregisterBot} skips those on purpose). Sweeps every level
     * for leftover {@code BotEntity} instances after clearing the tracked registry.
     *
     * @return the number of bots removed
     */
    public static int killAll(MinecraftServer server) {
        int count = 0;

        for (String name : new HashSet<>(activeBots.keySet())) {
            if (removeBot(name)) {
                count++;
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (BotEntity bot : level.getEntities(ModEntities.BOT.get(), b -> true)) {
                if (!bot.isRemoved()) {
                    BlockPos pos = bot.blockPosition();
                    level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
                    bot.discard();
                    count++;
                    BotMod.LOGGER.info("Orphaned bot at ({}, {}, {}) in {} removed via killall", pos.getX(), pos.getY(), pos.getZ(), level.dimension().location());
                }
            }
        }

        return count;
    }
}
