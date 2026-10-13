package com.muertededsafio;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent DEDSAFIO = register("dedsafio");
    public static final SoundEvent RULETA = register("ruleta");

    private ModSounds() {}

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of(MuerteDedsafio.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {}
}
