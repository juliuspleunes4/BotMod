package com.julius.botmod.bot;

import com.julius.botmod.BotMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.*;

public class BotManager {

    public record BotEntry(String name, FakePlayer player, ServerLevel level, int chunkX, int chunkZ) {}

    private static final Map<String, BotEntry> activeBots = new HashMap<>();

    /**
     * Spawns a FakePlayer bot at the given position and force-loads the chunk.
     *
     * @return true if the bot was successfully spawned, false if the name is already in use
     */
    public static boolean spawnBot(String name, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        if (activeBots.containsKey(name)) {
            return false;
        }

        // Minecraft enforces a 16-character limit on player names
        String profileName = name.length() > 16 ? name.substring(0, 16) : name;
        GameProfile profile = new GameProfile(UUID.randomUUID(), profileName);
        FakePlayer bot = FakePlayerFactory.get(level, profile);
        bot.setPos(pos.x, pos.y, pos.z);
        bot.setYRot(yRot);
        bot.setXRot(xRot);

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

        if (!entry.player().isRemoved()) {
            entry.player().discard();
        }

        BotMod.LOGGER.info("Bot '{}' removed", name);
        return true;
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
