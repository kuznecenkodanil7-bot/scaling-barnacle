package com.luis.creepypasta.client.render;

import com.luis.creepypasta.CreepypastaMod;
import com.luis.creepypasta.HaunterEntity;
import com.luis.creepypasta.client.CreepypastaClient;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.Identifier;

public class HaunterRenderer extends BipedEntityRenderer<HaunterEntity, BipedEntityRenderState, BipedEntityModel<BipedEntityRenderState>> {
    private static final Identifier TEXTURE = CreepypastaMod.id("textures/entity/haunter.png");

    public HaunterRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(CreepypastaClient.HAUNTER_LAYER)), 0.0F);
    }

    @Override
    public BipedEntityRenderState createRenderState() {
        return new BipedEntityRenderState();
    }

    @Override
    public Identifier getTexture(BipedEntityRenderState state) {
        return TEXTURE;
    }
}
