package com.muertededsafio;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class MuerteDedsafio implements ModInitializer {
    public static final String MOD_ID = "muertededsafio";

    @Override
    public void onInitialize() {
        ModSounds.init();
        PayloadTypeRegistry.playS2C().register(AnimPayload.ID, AnimPayload.CODEC);
        ServerLivingEntityEvents.ALLOW_DEATH.register(DeathHandler::onAllowDeath);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AnimCommand.register(dispatcher));
    }
}
