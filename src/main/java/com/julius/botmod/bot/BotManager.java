package com.julius.botmod.bot;

import com.julius.botmod.BotMod;
import com.julius.botmod.entity.BotEntity;
import com.julius.botmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BotManager {

    public record BotEntry(String name, BotEntity bot, ServerLevel level, int chunkX, int chunkZ) {}

    private static final Map<String, BotEntry> activeBots = new HashMap<>();

    /**
     * Spawns a BotEntity with the owner's full skin and force-loads the chunk.
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
        bot.setCustomName(Component.literal("[Bot] " + name));
        bot.setCustomNameVisible(true);

        int cx = BlockPos.containing(pos).getX() >> 4;
        int cz = BlockPos.containing(pos).getZ() >> 4;

        // Force the chunk to stay loaded even without real players nearby
        level.setChunkForced(cx, cz, true);
        level.addFreshEntity(bot);

        activeBots.put(name, new BotEntry(name, bot, level, cx, cz));
        BotMod.LOGGER.info("Bot '{}' spawned at ({}, {}, {})", name, (int) pos.x, (int) pos.y, (int) pos.z);
        return true;
    }

    /**
     * Removes an existing bot and releases the forced chunk ticket.
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

    /** Removes all active bots — called on server shutdown. */
    public static void removeAll() {
        new HashSet<>(activeBots.keySet()).forEach(BotManager::removeBot);
    }
}
