package com.challenger.battle;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Challenger.MODID)
public class Challenger {
    public static final String MODID = "challenger";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Challenger(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(BattleManager::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(BattleManager::onLivingHurt);
        LOGGER.info("Challenger loaded! PvP is managed exclusively by Challenger.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BattleCommand.register(event.getDispatcher());
    }
}
