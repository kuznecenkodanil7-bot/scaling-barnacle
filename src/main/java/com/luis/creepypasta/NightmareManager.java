package com.luis.creepypasta;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class NightmareManager {
    private static final Random RANDOM = new Random();
    private static final Map<UUID, ReturnPoint> RETURN_POINTS = new HashMap<>();

    private static final BlockPos START = new BlockPos(0, 94, 0);
    private static final BlockPos PORTAL = new BlockPos(90, 94, 0);
    private static final int FOREST_RADIUS = 96;

    private NightmareManager() { }

    public static void teleportToNightmare(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey().equals(CreepypastaMod.NIGHTMARE_WORLD)) {
            scatterInsideNightmare(player);
            return;
        }

        ServerWorld nightmare = player.getServer().getWorld(CreepypastaMod.NIGHTMARE_WORLD);
        if (nightmare == null) {
            player.sendMessage(Text.literal("[creepypasta] Измерение creepypasta:nightmare не загрузилось.").formatted(Formatting.RED), false);
            return;
        }

        RETURN_POINTS.put(player.getUuid(), new ReturnPoint(player.getWorld().getRegistryKey(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
        buildNightmareSite(nightmare);
        player.teleport(nightmare, START.getX() + 0.5D, START.getY(), START.getZ() + 0.5D, Set.of(), player.getYaw(), player.getPitch(), true);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 70, 0, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 240, 0, false, false));
        player.playSound(CreepypastaMod.SCREAMER_SOUND, 1.6F, 0.6F);
    }

    public static void tickNightmarePlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        buildNightmareSite(world);
        ensureOneGuardian(world, player);
        if (isInPortal(player)) {
            returnToNormalWorld(player);
            return;
        }
        if (RANDOM.nextInt(180) == 0) growDeadTree(world, randomGroundNear(player, 48));
        if (RANDOM.nextInt(260) == 0) HorrorManager.placeHelpSign(world, randomGroundNear(player, 44));
        if (RANDOM.nextInt(220) == 0) {
            world.setTimeOfDay(RANDOM.nextBoolean() ? 18000L : 6000L);
        }
    }

    private static void scatterInsideNightmare(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        BlockPos pos = randomGroundNear(player, 24);
        player.teleport(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Set.of(), player.getYaw(), player.getPitch(), true);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 140, 0, false, false));
    }

    private static void returnToNormalWorld(ServerPlayerEntity player) {
        ReturnPoint point = RETURN_POINTS.remove(player.getUuid());
        ServerWorld destination = null;
        double x;
        double y;
        double z;
        float yaw;
        float pitch;

        if (point != null) {
            destination = player.getServer().getWorld(point.world());
            x = point.x();
            y = point.y();
            z = point.z();
            yaw = point.yaw();
            pitch = point.pitch();
        } else {
            destination = player.getServer().getOverworld();
            BlockPos spawn = destination.getSpawnPos();
            x = spawn.getX() + 0.5D;
            y = spawn.getY();
            z = spawn.getZ() + 0.5D;
            yaw = 0.0F;
            pitch = 0.0F;
        }

        if (destination == null) return;
        player.teleport(destination, x, y, z, Set.of(), yaw, pitch, true);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 140, 0, false, false));
        player.playSound(CreepypastaMod.AMBIENT_SOUND, 1.0F, 0.75F);
    }

    private static boolean isInPortal(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        BlockPos feet = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(feet.add(-1, -1, -1), feet.add(1, 2, 1))) {
            if (world.getBlockState(pos).isOf(CreepypastaMod.DREAD_PORTAL)) return true;
        }
        return false;
    }

    private static void buildNightmareSite(ServerWorld world) {
        buildStartArea(world);
        buildPortal(world);
        buildCross(world, new BlockPos(-24, 94, 16), 5);
        buildCross(world, new BlockPos(18, 94, -26), 7);
        buildCross(world, new BlockPos(48, 94, 22), 6);
        buildCross(world, new BlockPos(74, 94, -20), 8);
        buildCross(world, new BlockPos(100, 94, 18), 6);
        seedDeadForest(world);
    }

    private static void buildStartArea(ServerWorld world) {
        clearBox(world, START.add(-7, -1, -7), START.add(7, 7, 7));
        fill(world, START.add(-8, -2, -8), START.add(8, -1, 8), Blocks.BLACKSTONE.getDefaultState());
        fill(world, START.add(-4, -1, -4), START.add(4, -1, 4), Blocks.SCULK.getDefaultState());
        placeNightmareSign(world, START.add(2, 0, 3));
    }

    private static void buildPortal(ServerWorld world) {
        clearBox(world, PORTAL.add(-8, -1, -6), PORTAL.add(8, 8, 6));
        fill(world, PORTAL.add(-9, -2, -7), PORTAL.add(9, -1, 7), Blocks.BLACKSTONE.getDefaultState());

        for (int y = 0; y <= 5; y++) {
            set(world, PORTAL.add(-2, y, 0), Blocks.CRYING_OBSIDIAN.getDefaultState());
            set(world, PORTAL.add(2, y, 0), Blocks.CRYING_OBSIDIAN.getDefaultState());
        }
        for (int x = -2; x <= 2; x++) {
            set(world, PORTAL.add(x, 0, 0), Blocks.CRYING_OBSIDIAN.getDefaultState());
            set(world, PORTAL.add(x, 5, 0), Blocks.CRYING_OBSIDIAN.getDefaultState());
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= 4; y++) {
                set(world, PORTAL.add(x, y, 0), CreepypastaMod.DREAD_PORTAL.getDefaultState());
            }
        }
        buildCross(world, PORTAL.add(-6, 0, -3), 5);
        buildCross(world, PORTAL.add(6, 0, 3), 5);
        placeNightmareSign(world, PORTAL.add(0, 0, -4));
    }

    private static void ensureOneGuardian(ServerWorld world, ServerPlayerEntity player) {
        Box box = new Box(PORTAL).expand(160.0D);
        List<HaunterEntity> guardians = world.getEntitiesByType(CreepypastaMod.HAUNTER, box, entity -> true);
        for (int i = 1; i < guardians.size(); i++) guardians.get(i).discard();
        if (guardians.isEmpty()) {
            HaunterEntity haunter = CreepypastaMod.HAUNTER.create(world);
            if (haunter != null) {
                haunter.refreshPositionAndAngles(PORTAL.getX() + 6.5D, PORTAL.getY(), PORTAL.getZ() + 2.5D, 180.0F, 0.0F);
                world.spawnEntity(haunter);
            }
        } else {
            HaunterEntity guardian = guardians.getFirst();
            if (guardian.squaredDistanceTo(PORTAL.getX(), PORTAL.getY(), PORTAL.getZ()) > 80.0D * 80.0D) {
                guardian.refreshPositionAndAngles(PORTAL.getX() + 6.5D, PORTAL.getY(), PORTAL.getZ() + 2.5D, player.getYaw() + 180.0F, 0.0F);
            }
        }
    }

    private static void seedDeadForest(ServerWorld world) {
        if (world.getTime() % 60 != 0) return;
        for (int i = 0; i < 5; i++) {
            BlockPos pos = new BlockPos(RANDOM.nextInt(FOREST_RADIUS * 2 + 1) - FOREST_RADIUS, 94, RANDOM.nextInt(FOREST_RADIUS * 2 + 1) - FOREST_RADIUS);
            if (pos.isWithinDistance(START, 12.0D) || pos.isWithinDistance(PORTAL, 12.0D)) continue;
            growDeadTree(world, pos);
        }
    }

    private static BlockPos randomGroundNear(ServerPlayerEntity player, int radius) {
        ServerWorld world = player.getWorld();
        int x = player.getBlockX() + RANDOM.nextInt(radius * 2 + 1) - radius;
        int z = player.getBlockZ() + RANDOM.nextInt(radius * 2 + 1) - radius;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y < 70 || y > 140) y = 94;
        return new BlockPos(x, y, z);
    }

    private static void growDeadTree(ServerWorld world, BlockPos base) {
        if (base.isWithinDistance(PORTAL, 8.0D) || base.isWithinDistance(START, 8.0D)) return;
        clearBox(world, base.add(-2, 0, -2), base.add(2, 7, 2));
        int h = 5 + RANDOM.nextInt(5);
        for (int y = -2; y <= -1; y++) set(world, base.add(0, y, 0), Blocks.BLACKSTONE.getDefaultState());
        for (int y = 0; y < h; y++) set(world, base.add(0, y, 0), Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState());
        for (int y = 2; y < h; y += 2) {
            int len = 1 + RANDOM.nextInt(3);
            int dx = RANDOM.nextBoolean() ? 1 : -1;
            int dz = RANDOM.nextBoolean() ? 1 : -1;
            for (int i = 1; i <= len; i++) set(world, base.add(dx * i, y, 0), Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState());
            for (int i = 1; i <= len; i++) set(world, base.add(0, y + 1, dz * i), Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState());
        }
    }

    private static void buildCross(ServerWorld world, BlockPos base, int height) {
        fill(world, base.add(-2, -2, -2), base.add(2, -1, 2), Blocks.BLACKSTONE.getDefaultState());
        for (int y = 0; y < height; y++) set(world, base.add(0, y, 0), Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState());
        for (int x = -2; x <= 2; x++) set(world, base.add(x, height - 2, 0), Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState());
        if (RANDOM.nextBoolean()) placeNightmareSign(world, base.add(1, 0, 1));
    }

    private static void placeNightmareSign(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) set(world, pos.down(), Blocks.BLACKSTONE.getDefaultState());
        world.setBlockState(pos, Blocks.OAK_SIGN.getDefaultState().with(Properties.ROTATION, RANDOM.nextInt(16)), Block.NOTIFY_ALL);
        if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            sign.setText(sign.getFrontText()
                    .withMessage(0, Text.literal("HELP!!!").formatted(Formatting.DARK_RED, Formatting.BOLD))
                    .withMessage(1, Text.literal("RUN").formatted(Formatting.RED))
                    .withMessage(2, Text.literal("NO EXIT").formatted(Formatting.DARK_RED))
                    .withMessage(3, Text.literal("HELP!!!").formatted(Formatting.RED))
                    .withColor(DyeColor.RED)
                    .withGlowing(true), true);
            sign.markDirty();
            world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_ALL);
        }
    }

    private static void clearBox(ServerWorld world, BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.iterate(from, to)) {
            BlockState state = world.getBlockState(pos);
            if (!state.isOf(CreepypastaMod.DREAD_PORTAL)) world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void fill(ServerWorld world, BlockPos from, BlockPos to, BlockState state) {
        for (BlockPos pos : BlockPos.iterate(from, to)) set(world, pos, state);
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        if (pos.getY() < world.getBottomY() || pos.getY() >= world.getTopYInclusive()) return;
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }

    private record ReturnPoint(RegistryKey<World> world, double x, double y, double z, float yaw, float pitch) { }
}
