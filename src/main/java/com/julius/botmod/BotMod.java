package com.julius.botmod;

import com.julius.botmod.bot.BotManager;
import com.julius.botmod.command.BotCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BotMod.MOD_ID)
public class BotMod {

    public static final String MOD_ID = "botmod";
    public static final Logger LOGGER = LogManager.getLogger();

    public BotMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BotCommand.register(event.getDispatcher());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        // Clean up all bots to release forced chunk tickets
        BotManager.removeAll();
    }
}
