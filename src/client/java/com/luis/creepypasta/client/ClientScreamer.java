package com.luis.creepypasta.client;

import com.luis.creepypasta.CreepypastaMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public final class ClientScreamer {
    private static final Identifier TEXTURE = CreepypastaMod.id("textures/gui/screamer.png");
    private static final Random RANDOM = Random.create();
    private static int remainingTicks = 0;
    private static int rareCooldown = 20 * 180;

    private ClientScreamer() { }

    public static void trigger(int ticks) {
        MinecraftClient client = MinecraftClient.getInstance();
        remainingTicks = Math.max(remainingTicks, ticks);
        if (client.player != null) {
            client.player.playSound(CreepypastaMod.SCREAMER_SOUND, 1.0F, 0.55F + RANDOM.nextFloat() * 0.25F);
        }
    }

    public static void tick(MinecraftClient client) {
        if (remainingTicks > 0) remainingTicks--;
        if (client.world == null || client.player == null) return;

        if (rareCooldown > 0) {
            rareCooldown--;
            return;
        }

        // Редкий рандомный скример: не спамит, но держит напряжение.
        rareCooldown = 20 * (420 + RANDOM.nextInt(900));
        if (RANDOM.nextInt(4) == 0) {
            trigger(34 + RANDOM.nextInt(18));
        }
    }

    public static void render(DrawContext context) {
        if (remainingTicks <= 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float pulse = remainingTicks % 8 < 4 ? 1.0F : 0.86F;
        int alpha = Math.min(255, 170 + remainingTicks * 3);
        int color = ((alpha & 0xFF) << 24) | ((int)(255 * pulse) << 16) | ((int)(255 * pulse) << 8) | ((int)(255 * pulse));
        context.fill(0, 0, width, height, 0xCC000000);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, 0.0F, 0.0F, width, height, 64, 64, color);
        if (RANDOM.nextInt(3) == 0) {
            context.fill(RANDOM.nextInt(Math.max(1, width)), 0, RANDOM.nextInt(Math.max(1, width)) + 2, height, 0x66AA0000);
        }
    }
}
