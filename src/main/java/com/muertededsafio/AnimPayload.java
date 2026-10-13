package com.muertededsafio;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

/** type: 0 = animación de muerte, 1 = animación roja (/momento hardcore anim) */
public record AnimPayload(int type, String name, Text cause) implements CustomPayload {
    public static final int DEATH = 0;
    public static final int RED = 1;

    public static final CustomPayload.Id<AnimPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MuerteDedsafio.MOD_ID, "anim"));

    public static final PacketCodec<RegistryByteBuf, AnimPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, AnimPayload::type,
            PacketCodecs.STRING, AnimPayload::name,
            TextCodecs.PACKET_CODEC, AnimPayload::cause,
            AnimPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
