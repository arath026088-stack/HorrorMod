package com.horror;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HorrorMod implements ModInitializer {
    public static final String MOD_ID = "horrormod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 4 minutes converted into server ticks (20 ticks per second * 60 seconds * 4)
    private static final int CYCLE_TIME_TICKS = 4 * 60 * 20;

    private static final Map<UUID, Integer> playerTicks = new HashMap<>();
    private static final Map<UUID, Integer> jumpScareCount = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Horror Stalker Mod Initialized successfully!");

        // 1. Spawns the warning sign the first time a player enters a fresh world
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerWorld world = player.getServerWorld();
            UUID uuid = player.getUuid();

            playerTicks.putIfAbsent(uuid, 0);
            jumpScareCount.putIfAbsent(uuid, 0);

            if (world.getTime() < 100) {
                BlockPos playerPos = player.getBlockPos();
                BlockPos signPos = playerPos.offset(player.getHorizontalFacing(), 2);
                
                world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState(), 3);
                
                if (world.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
                    SignText text = new SignText(
                        new Text[]{
                            Text.literal("Be a speedrunner,").formatted(Formatting.RED),
                            Text.literal("remember,").formatted(Formatting.RED),
                            Text.literal("he continues").formatted(Formatting.RED),
                            Text.literal("the cycle.").formatted(Formatting.RED)
                        },
                        new Text[]{Text.of(""), Text.of(""), Text.of(""), Text.of("")},
                        Formatting.BLACK,
                        true
                    );
                    sign.setText(text, true);
                    sign.markDirty();
                    world.updateListeners(signPos, world.getBlockState(signPos), world.getBlockState(signPos), 3);
                }
            }
        });

        // 2. The unstoppable 4-minute attack clock loops continuously
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!player.isAlive()) continue;

                UUID uuid = player.getUuid();
                int currentTicks = playerTicks.getOrDefault(uuid, 0) + 1;
                playerTicks.put(uuid, currentTicks);

                // Periodic snitching updates to chat every 50 seconds to stalk the player
                if (currentTicks % (50 * 20) == 0 && currentTicks < CYCLE_TIME_TICKS) {
                    player.sendMessage(Text.literal("§4[Stalker] I know where you are..."), false);
                }

                if (currentTicks >= CYCLE_TIME_TICKS) {
                    playerTicks.put(uuid, 0); // Restart the clock
                    ServerWorld world = player.getServerWorld();
                    
                    // Spawn creature position directly facing the player's line of sight
                    Vec3d lookVec = player.getRotationVecClient();
                    BlockPos spawnPos = BlockPos.ofFloored(player.getX() + lookVec.x * 1.5, player.getY() + 0.5, player.getZ() + lookVec.z * 1.5);
                    
                    EndermiteEntity stalker = EntityType.ENDERMITE.create(world, SpawnReason.TRIGGERED);
                    if (stalker != null) {
                        stalker.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYaw() + 180, 0);
                        stalker.setCustomName(Text.literal("§4§lThe Six-Legged Terror"));
                        stalker.setCustomNameVisible(true);
                        world.spawnEntity(stalker);
                    }

                    int count = jumpScareCount.getOrDefault(uuid, 0) + 1;
                    
                    if (count >= 4) {
                        // 4th time: Absolute execution kill
                        jumpScareCount.put(uuid, 0); 
                        player.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);
                        server.getPlayerManager().broadcast(Text.literal("§l§4" + player.getName().getString() + " was consumed by the cycle."), false);
                    } else {
                        // 1st, 2nd, and 3rd time: Unavoidable drop to half a heart (1.0 HP out of 20.0 HP)
                        jumpScareCount.put(uuid, count);
                        player.setHealth(1.0F);
                    }
                }
            }
        });
    }
}
