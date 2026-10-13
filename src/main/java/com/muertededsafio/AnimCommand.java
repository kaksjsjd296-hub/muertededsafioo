package com.muertededsafio;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class AnimCommand {
    private AnimCommand() {}

    /** /momento hardcore anim  (requiere ser operador, nivel 2) */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("momento")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("hardcore")
                        .then(CommandManager.literal("anim")
                                .executes(ctx -> {
                                    MinecraftServer server = ctx.getSource().getServer();
                                    AnimPayload payload = new AnimPayload(AnimPayload.RED, "", Text.empty());
                                    for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                                        ServerPlayNetworking.send(p, payload);
                                    }
                                    return 1;
                                }))));
    }
}
