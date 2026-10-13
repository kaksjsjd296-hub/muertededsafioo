package com.muertededsafio.client;

import com.muertededsafio.AnimPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class MuerteDedsafioClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AnimPayload.ID,
                (payload, context) -> AnimationRenderer.start(payload));
        HudRenderCallback.EVENT.register(AnimationRenderer::render);
    }
}
