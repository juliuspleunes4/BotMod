package com.julius.botmod.bot;

import com.julius.botmod.BotMod;
import com.julius.botmod.entity.BotEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = BotMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class BotEventHandler {

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getTarget() instanceof BotEntity) {
            event.setCanceled(true);
        }
    }
}
