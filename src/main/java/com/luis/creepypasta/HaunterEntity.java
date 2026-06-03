package com.luis.creepypasta;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class HaunterEntity extends HostileEntity {
    public static final byte SCREAMER_STATUS = 67;

    private int touchCooldown = 0;
    private int blockBreakCooldown = 0;
    private int breathCooldown = 80;
    private int stareTicks = 0;
    private int activeTicks = 0;
    private boolean awakened = false;

    public HaunterEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createHaunterAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 80.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.39D)
                .add(EntityAttributes.ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.FOLLOW_RANGE, 96.0D)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void initGoals() {
        // Поведение полностью ручное: сначала стоит, потом преследует/телепортируется.
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return CreepypastaMod.BREATH_SOUND;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return CreepypastaMod.AMBIENT_SOUND;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return CreepypastaMod.SCREAMER_SOUND;
    }

    @Override
    protected float getSoundVolume() {
        return 1.35F;
    }

    @Override
    public boolean isSilent() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) return;

        ServerWorld world = (ServerWorld) getWorld();
        if (touchCooldown > 0) touchCooldown--;
        if (blockBreakCooldown > 0) blockBreakCooldown--;
        if (breathCooldown > 0) breathCooldown--;

        PlayerEntity target = findTarget(world);
        if (target == null) return;

        lookAtEntity(target, 90.0F, 90.0F);

        boolean seen = playerSeesEntity(target);
        if (!awakened) {
            getNavigation().stop();
            stareTicks += seen ? 2 : 1;
            if (seen && (stareTicks > 25 || random.nextInt(40) == 0)) {
                awaken(world, target);
            }
            breathe(world);
            return;
        }

        activeTicks++;
        breathe(world);

        double distance = squaredDistanceTo(target);
        if (seen && activeTicks % 60 == 0 && random.nextBoolean()) {
            teleportNear((ServerPlayerEntity) target, 2, 6);
            world.sendEntityStatus(this, SCREAMER_STATUS);
            playSound(CreepypastaMod.SCREAMER_SOUND, 1.8F, 0.55F + random.nextFloat() * 0.25F);
        }

        if (distance > 144.0D && activeTicks % 40 == 0) {
            teleportNear((ServerPlayerEntity) target, 5, 11);
        } else {
            EntityNavigation navigation = getNavigation();
            navigation.startMovingTo(target, 1.18D);
        }

        if (blockBreakCooldown <= 0) {
            breakBlocksTowards(target);
            blockBreakCooldown = 8 + random.nextInt(14);
        }

        if (distance < 2.25D && touchCooldown <= 0 && target instanceof ServerPlayerEntity player) {
            touchCooldown = 80;
            world.sendEntityStatus(this, SCREAMER_STATUS);
            playSound(CreepypastaMod.SCREAMER_SOUND, 2.0F, 0.55F + random.nextFloat() * 0.25F);
            NightmareManager.teleportToNightmare(player);
        }
    }

    private void awaken(ServerWorld world, PlayerEntity target) {
        awakened = true;
        activeTicks = 0;
        world.sendEntityStatus(this, SCREAMER_STATUS);
        playSound(CreepypastaMod.SCREAMER_SOUND, 2.0F, 0.55F + random.nextFloat() * 0.25F);
        if (target instanceof ServerPlayerEntity player) {
            teleportNear(player, 2, 5);
        }
        if (random.nextInt(3) == 0) {
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
                world.spawnEntity(lightning);
            }
        }
    }

    private void breathe(ServerWorld world) {
        if (breathCooldown > 0) return;
        playSound(CreepypastaMod.BREATH_SOUND, 1.15F, 0.72F + random.nextFloat() * 0.18F);
        breathCooldown = 80 + random.nextInt(110);
    }

    private PlayerEntity findTarget(ServerWorld world) {
        List<? extends PlayerEntity> players = world.getPlayers(player -> !player.isSpectator() && !player.isCreative()
                && player.squaredDistanceTo(this) < 96.0D * 96.0D);
        return players.stream().min(Comparator.comparingDouble(this::squaredDistanceTo)).orElse(null);
    }

    private boolean playerSeesEntity(PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d toEntity = getEyePos().subtract(player.getEyePos()).normalize();
        double dot = look.dotProduct(toEntity);
        return dot > 0.58D && player.canSee(this) && player.squaredDistanceTo(this) < 42.0D * 42.0D;
    }

    private void teleportNear(ServerPlayerEntity player, int min, int max) {
        ServerWorld world = (ServerWorld) getWorld();
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = min + random.nextInt(Math.max(1, max - min + 1));
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) && world.getBlockState(pos).isAir()) {
                refreshPositionAndAngles(x + 0.5D, y, z + 0.5D, player.getYaw() + 180.0F, 0.0F);
                return;
            }
        }
    }

    private void breakBlocksTowards(PlayerEntity target) {
        World world = getWorld();
        Vec3d direction = target.getPos().subtract(getPos()).normalize();
        BlockPos center = BlockPos.ofFloored(getX() + direction.x * 1.2D, getY() + 0.5D, getZ() + direction.z * 1.2D);

        for (BlockPos pos : BlockPos.iterate(center.add(-1, 0, -1), center.add(1, 2, 1))) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir() || state.getHardness(world, pos) < 0.0F || state.isOf(CreepypastaMod.DREAD_PORTAL)) continue;
            if (random.nextFloat() < 0.55F) {
                world.breakBlock(pos, false, this);
            }
        }
    }

    @Override
    public void handleStatus(byte status) {
        if (status == SCREAMER_STATUS) {
            ClientHooks.triggerScreamer(50);
        } else {
            super.handleStatus(status);
        }
    }

    @Override
    public boolean shouldRender(double distance) {
        return true;
    }
}
