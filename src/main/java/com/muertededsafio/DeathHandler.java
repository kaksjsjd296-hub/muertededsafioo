package com.muertededsafio;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;

public final class DeathHandler {
    private DeathHandler() {}

    /**
     * Se llama justo antes de que un ser vivo muera. Si es un jugador cancelamos la muerte real
     * (así nunca aparece la pantalla de reaparecer) y hacemos todo a mano.
     */
    public static boolean onAllowDeath(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player)) return true;

        // Un espectador (ya "muerto") no vuelve a morir
        if (player.isSpectator()) {
            player.setHealth(player.getMaxHealth());
            return false;
        }

        // Dejar que el tótem de la inmortalidad funcione normal
        boolean hasTotem = player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
                || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        if (hasTotem && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;

        MinecraftServer server = player.getServer();
        if (server == null) return true;
        ServerWorld world = (ServerWorld) player.getWorld();

        Text deathMessage = player.getDamageTracker().getDeathMessage();
        String name = player.getGameProfile().getName();

        // --- Soltar inventario y experiencia como en una muerte normal ---
        if (!world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
            player.getInventory().dropAll();
            int xp = Math.min(player.experienceLevel * 7, 100);
            if (xp > 0) ExperienceOrbEntity.spawn(world, player.getPos(), xp);
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
            player.totalExperience = 0;
        }

        // --- Memorial: bedrock > valla > cabeza con la skin del jugador ---
        BlockPos base = findBase(world, player.getBlockPos());
        world.setBlockState(base, Blocks.BEDROCK.getDefaultState());
        world.setBlockState(base.up(), Blocks.OAK_FENCE.getDefaultState());
        BlockPos headPos = base.up(2);
        int rotation = MathHelper.floor((player.getYaw() * 16.0F / 360.0F) + 0.5F) & 15;
        world.setBlockState(headPos, Blocks.PLAYER_HEAD.getDefaultState().with(SkullBlock.ROTATION, rotation));
        BlockEntity be = world.getBlockEntity(headPos);
        if (be instanceof SkullBlockEntity skull) {
            skull.setOwner(new ProfileComponent(player.getGameProfile()));
        }

        // --- Dejarlo vivo y pasarlo a espectador ---
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0F);
        player.extinguish();
        player.clearStatusEffects();
        player.fallDistance = 0.0F;
        player.setAir(player.getMaxAir());
        player.changeGameMode(GameMode.SPECTATOR);
        if (player.getY() < base.getY()) {
            player.requestTeleport(player.getX(), base.getY() + 3.0, player.getZ());
        }

        // --- Mensaje en el chat + animación para TODOS los jugadores ---
        server.getPlayerManager().broadcast(deathMessage, false);
        AnimPayload payload = new AnimPayload(AnimPayload.DEATH, name, deathMessage);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
        return false;
    }

    /** Baja desde donde murió hasta encontrar suelo (máx. 64 bloques) y respeta los límites del mundo. */
    private static BlockPos findBase(ServerWorld world, BlockPos start) {
        int bottom = world.getBottomY();
        int top = world.getTopY();
        BlockPos.Mutable pos = start.mutableCopy();
        if (pos.getY() < bottom + 1) pos.setY(bottom + 1);
        int steps = 0;
        while (steps < 64 && pos.getY() > bottom + 1 && world.getBlockState(pos.down()).isAir()) {
            pos.move(Direction.DOWN);
            steps++;
        }
        if (pos.getY() > top - 4) pos.setY(top - 4);
        return pos.toImmutable();
    }
}
