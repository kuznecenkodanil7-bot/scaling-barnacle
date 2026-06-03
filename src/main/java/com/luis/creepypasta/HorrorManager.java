package com.luis.creepypasta;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class HorrorManager {
    private static final Random RANDOM = new Random();
    private static final Map<UUID, Progress> PROGRESS = new HashMap<>();

    private static final String[] MESSAGES = {
            "help",
            "не смотри назад",
            "ты слышишь дыхание?",
            "он стоит слишком близко",
            "ошибка: player_not_alone",
            "выход не там",
            "HELP!!!",
            "ночь помнит тебя",
            "не открывай чат",
            "твой мир уже заражён"
    };

    private HorrorManager() { }

    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        Progress progress = PROGRESS.computeIfAbsent(player.getUuid(), uuid -> new Progress());
        progress.ticks++;

        if (player.getWorld().getRegistryKey().equals(CreepypastaMod.NIGHTMARE_WORLD)) {
            NightmareManager.tickNightmarePlayer(player);
            tickChat(player, progress, true);
            tickTime(player, progress, true);
            return;
        }

        int phase = progress.phase();
        if (phase >= 1) tickChat(player, progress, false);
        if (phase >= 2) tickTime(player, progress, false);
        if (phase >= 2) tickLightning(player, progress);
        if (phase >= 3) ensureHaunter(player, progress);
        if (phase >= 3) tickInfection(player, progress, phase);
        if (phase >= 5) tickChunkBreak(player, progress, phase);
    }

    private static void tickChat(ServerPlayerEntity player, Progress progress, boolean nightmare) {
        if (progress.chatCooldown > 0) {
            progress.chatCooldown--;
            return;
        }
        progress.chatCooldown = (nightmare ? 140 : 360) + RANDOM.nextInt(nightmare ? 220 : 700);
        String message = MESSAGES[RANDOM.nextInt(MESSAGES.length)];
        Formatting color = nightmare ? Formatting.DARK_RED : (RANDOM.nextBoolean() ? Formatting.GRAY : Formatting.DARK_GRAY);
        MutableText text = Text.literal(message).formatted(color);
        if (RANDOM.nextInt(4) == 0) text = text.formatted(Formatting.OBFUSCATED);
        player.sendMessage(text, true);
    }

    private static void tickTime(ServerPlayerEntity player, Progress progress, boolean nightmare) {
        if (progress.timeCooldown > 0) {
            progress.timeCooldown--;
            return;
        }
        progress.timeCooldown = (nightmare ? 220 : 900) + RANDOM.nextInt(nightmare ? 380 : 1200);
        ServerWorld world = player.getWorld();
        long time = RANDOM.nextBoolean() ? 18000L : (RANDOM.nextBoolean() ? 6000L : 13000L + RANDOM.nextInt(9000));
        world.setTimeOfDay(time);
        if (RANDOM.nextInt(nightmare ? 2 : 4) == 0) {
            world.setWeather(0, 6000 + RANDOM.nextInt(6000), true, true);
        }
    }

    private static void tickLightning(ServerPlayerEntity player, Progress progress) {
        if (progress.lightningCooldown > 0) {
            progress.lightningCooldown--;
            return;
        }
        progress.lightningCooldown = 700 + RANDOM.nextInt(1300);
        ServerWorld world = player.getWorld();
        int x = player.getBlockX() - 14 + RANDOM.nextInt(29);
        int z = player.getBlockZ() - 14 + RANDOM.nextInt(29);
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning != null) {
            lightning.refreshPositionAfterTeleport(x + 0.5D, y, z + 0.5D);
            world.spawnEntity(lightning);
        }
    }

    private static void ensureHaunter(ServerPlayerEntity player, Progress progress) {
        if (progress.haunterCooldown > 0) {
            progress.haunterCooldown--;
            return;
        }
        progress.haunterCooldown = 500 + RANDOM.nextInt(900);
        ServerWorld world = player.getWorld();
        Box box = player.getBoundingBox().expand(96.0D);
        if (!world.getEntitiesByType(CreepypastaMod.HAUNTER, box, entity -> true).isEmpty()) return;

        for (int i = 0; i < 18; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            int radius = 18 + RANDOM.nextInt(30);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!world.getBlockState(pos).isAir()) continue;
            HaunterEntity haunter = CreepypastaMod.HAUNTER.create(world);
            if (haunter != null) {
                haunter.refreshPositionAndAngles(x + 0.5D, y, z + 0.5D, player.getYaw() + 180.0F, 0.0F);
                world.spawnEntity(haunter);
            }
            return;
        }
    }

    private static void tickInfection(ServerPlayerEntity player, Progress progress, int phase) {
        if (progress.infectionCooldown > 0) {
            progress.infectionCooldown--;
            return;
        }
        progress.infectionCooldown = Math.max(5, 50 - phase * 6);
        ServerWorld world = player.getWorld();
        int radius = 5 + phase * 3;
        int tries = 4 + phase;
        for (int i = 0; i < tries; i++) {
            BlockPos pos = player.getBlockPos().add(RANDOM.nextInt(radius * 2 + 1) - radius, RANDOM.nextInt(9) - 4, RANDOM.nextInt(radius * 2 + 1) - radius);
            corruptBlock(world, pos, phase);
        }
        if (RANDOM.nextInt(70) == 0) {
            placeHelpSign(world, topNear(player, radius));
        }
    }

    private static void corruptBlock(ServerWorld world, BlockPos pos, int phase) {
        if (!world.isChunkLoaded(new ChunkPos(pos).toLong())) return;
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.getHardness(world, pos) < 0.0F || state.isOf(CreepypastaMod.DREAD_PORTAL)) return;

        Block replacement;
        if (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT) || state.isOf(Blocks.PODZOL) || state.isOf(Blocks.MYCELIUM)) {
            replacement = RANDOM.nextBoolean() ? Blocks.SCULK : Blocks.SOUL_SOIL;
        } else if (state.isOf(Blocks.STONE) || state.isOf(Blocks.COBBLESTONE) || state.isOf(Blocks.DEEPSLATE)) {
            replacement = RANDOM.nextBoolean() ? Blocks.BLACKSTONE : Blocks.CRYING_OBSIDIAN;
        } else if (state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) {
            if (phase >= 4) {
                world.breakBlock(pos, false);
                return;
            }
            replacement = Blocks.AIR;
        } else if (state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) {
            replacement = RANDOM.nextBoolean() ? Blocks.STRIPPED_DARK_OAK_LOG : Blocks.DARK_OAK_LOG;
        } else {
            if (phase >= 6 && RANDOM.nextFloat() < 0.18F) {
                world.breakBlock(pos, false);
                return;
            }
            replacement = RANDOM.nextBoolean() ? Blocks.BLACKSTONE : Blocks.SCULK;
        }
        world.setBlockState(pos, replacement.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private static BlockPos topNear(ServerPlayerEntity player, int radius) {
        ServerWorld world = player.getWorld();
        int x = player.getBlockX() + RANDOM.nextInt(radius * 2 + 1) - radius;
        int z = player.getBlockZ() + RANDOM.nextInt(radius * 2 + 1) - radius;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static void tickChunkBreak(ServerPlayerEntity player, Progress progress, int phase) {
        if (progress.chunkCooldown > 0) {
            progress.chunkCooldown--;
            return;
        }
        progress.chunkCooldown = Math.max(260, 1050 - phase * 90) + RANDOM.nextInt(650);
        ServerWorld world = player.getWorld();
        ChunkPos chunk = player.getChunkPos();
        int baseX = chunk.getStartX() + RANDOM.nextInt(16);
        int baseZ = chunk.getStartZ() + RANDOM.nextInt(16);
        int topY = Math.min(world.getTopY(Heightmap.Type.MOTION_BLOCKING, baseX, baseZ), player.getBlockY() + 8);
        int bottomY = Math.max(world.getBottomY(), topY - (8 + phase * 2));

        for (int x = baseX - 1; x <= baseX + 1; x++) {
            for (int z = baseZ - 1; z <= baseZ + 1; z++) {
                for (int y = bottomY; y <= topY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.getHardness(world, pos) >= 0.0F && !state.isOf(CreepypastaMod.DREAD_PORTAL)) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    public static void placeHelpSign(ServerWorld world, BlockPos pos) {
        BlockPos base = pos;
        if (!world.getBlockState(base.down()).isSolidBlock(world, base.down())) return;
        world.setBlockState(base, Blocks.OAK_SIGN.getDefaultState().with(Properties.ROTATION, RANDOM.nextInt(16)), Block.NOTIFY_ALL);
        if (world.getBlockEntity(base) instanceof SignBlockEntity sign) {
            sign.setText(sign.getFrontText()
                    .withMessage(0, Text.literal("HELP!!!").formatted(Formatting.DARK_RED, Formatting.BOLD))
                    .withMessage(1, Text.literal("HELP!!!").formatted(Formatting.RED))
                    .withMessage(2, Text.literal("HELP!!!").formatted(Formatting.DARK_RED))
                    .withMessage(3, Text.literal("HELP!!!").formatted(Formatting.RED))
                    .withColor(DyeColor.RED)
                    .withGlowing(true), true);
            sign.markDirty();
            world.updateListeners(base, world.getBlockState(base), world.getBlockState(base), Block.NOTIFY_ALL);
        }
    }

    private static final class Progress {
        int ticks = 0;
        int chatCooldown = 500;
        int timeCooldown = 1100;
        int lightningCooldown = 1500;
        int haunterCooldown = 900;
        int infectionCooldown = 260;
        int chunkCooldown = 2400;

        int phase() {
            if (ticks < 20 * 60) return 0;        // 1 минута — тишина.
            if (ticks < 20 * 150) return 1;       // Первые сообщения.
            if (ticks < 20 * 300) return 2;       // Время/молнии.
            if (ticks < 20 * 480) return 3;       // Сущность и заражение.
            if (ticks < 20 * 720) return 4;       // Сильнее ломает блоки.
            if (ticks < 20 * 960) return 5;       // Начинаются поломки чанков.
            return 6;                             // Полная крипипаста.
        }
    }
}
