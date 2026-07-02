package com.julius.botmod;

import com.julius.botmod.bot.BotManager;
import com.julius.botmod.command.BotCommand;
import com.julius.botmod.entity.BotEntity;
import com.julius.botmod.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BotMod.MOD_ID)
public class BotMod {

    public static final String MOD_ID = "botmod";
    public static final Logger LOGGER = LogManager.getLogger();

    public BotMod(IEventBus modEventBus) {
        ModEntities.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BotCommand.register(event.getDispatcher());
    }

    /**
     * Bot entities and their forced-chunk tickets already survive a restart via vanilla's own
     * persistence. What doesn't survive is BotManager's in-memory registry, so once the server
     * is up we scan every level for already-loaded BotEntity instances and re-register them.
     */
    private void onServerStarted(ServerStartedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (BotEntity bot : level.getEntities(ModEntities.BOT.get(), b -> true)) {
                BotManager.reregisterBot(bot, level);
            }
        }
    }
}
