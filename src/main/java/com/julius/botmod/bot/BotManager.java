package com.julius.botmod.bot;

import com.julius.botmod.BotMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BotManager {

    public record BotEntry(String name, ArmorStand marker, ServerLevel level, int chunkX, int chunkZ) {}

    private static final Map<String, BotEntry> activeBots = new HashMap<>();

    /**
     * Spawns a visible marker at the given position and force-loads the chunk.
     *
     * @return true if the bot was successfully spawned, false if the name is already in use
     */
    public static boolean spawnBot(String name, ServerLevel level, Vec3 pos, float yRot, ServerPlayer owner) {
        if (activeBots.containsKey(name)) {
            return false;
        }

        int cx = BlockPos.containing(pos).getX() >> 4;
        int cz = BlockPos.containing(pos).getZ() >> 4;

        // Force the chunk to stay loaded even without real players nearby
        level.setChunkForced(cx, cz, true);

        ArmorStand marker = new ArmorStand(level, pos.x, pos.y, pos.z);
        marker.setCustomName(Component.literal("[Bot] " + name));
        marker.setCustomNameVisible(true);
        marker.setInvulnerable(true);
        marker.setNoGravity(true);
        marker.setShowArms(true);
        marker.setYRot(yRot);

        // Put the owner's player head on the armor stand
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, new ResolvableProfile(owner.getGameProfile()));
        marker.setItemSlot(EquipmentSlot.HEAD, head);

        level.addFreshEntity(marker);

        activeBots.put(name, new BotEntry(name, marker, level, cx, cz));
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

        if (!entry.marker().isRemoved()) {
            entry.marker().discard();
        }

        BotMod.LOGGER.info("Bot '{}' removed", name);
        return true;
    }

    public static boolean isMarker(Entity entity) {
        return activeBots.values().stream().anyMatch(e -> e.marker() == entity);
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
