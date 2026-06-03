package com.luis.creepypasta.client;

import com.luis.creepypasta.ClientHooks;
import com.luis.creepypasta.CreepypastaMod;
import com.luis.creepypasta.client.render.HaunterRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;

public final class CreepypastaClient implements ClientModInitializer {
    public static final EntityModelLayer HAUNTER_LAYER = new EntityModelLayer(CreepypastaMod.id("haunter"), "main");

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(HAUNTER_LAYER,
                () -> TexturedModelData.of(BipedEntityModel.getModelData(Dilation.NONE, 0.0F), 64, 64));
        EntityRendererRegistry.register(CreepypastaMod.HAUNTER, HaunterRenderer::new);

        ClientHooks.SCREAMER = ClientScreamer::trigger;
        ClientTickEvents.END_CLIENT_TICK.register(ClientScreamer::tick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> ClientScreamer.render(drawContext));
    }
}
