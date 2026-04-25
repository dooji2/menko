package com.dooji.menko.client.render;

import com.dooji.menko.entity.MenkoCardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MenkoCardRenderer extends EntityRenderer<MenkoCardEntity, MenkoCardRenderer.RenderState> {
	private static final float MODEL_OFFSET_X = 4.5f / 16.0f;
	private static final float MODEL_OFFSET_Y = 7.75f / 16.0f;
	private static final float MODEL_OFFSET_Z = 2.0f / 16.0f;
	private final ItemModelResolver itemModelResolver;

	public MenkoCardRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
	}

	@Override
	public RenderState createRenderState() {
		return new RenderState();
	}

	@Override
	public void extractRenderState(MenkoCardEntity entity, RenderState renderState, float partialTick) {
		super.extractRenderState(entity, renderState, partialTick);
		renderState.yRot = entity.getYRot(partialTick);
		renderState.xRot = entity.getXRot(partialTick);
		renderState.roll = entity.getVisualRoll(partialTick);
		ItemStack stack = new ItemStack(entity.getCardItem());
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
		this.itemModelResolver.updateForNonLiving(renderState.item, stack, ItemDisplayContext.FIXED, entity);
	}

	@Override
	public void submit(RenderState renderState, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraRenderState) {
		poseStack.pushPose();
		poseStack.translate(0.0f, MenkoCardEntity.CARD_THICKNESS * 0.5f, 0.0f);
		poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(renderState.xRot));
		poseStack.mulPose(Axis.ZP.rotationDegrees(renderState.roll));
		poseStack.translate(MODEL_OFFSET_X, MODEL_OFFSET_Y, MODEL_OFFSET_Z);
		renderState.item.submit(poseStack, renderTasks, renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
		poseStack.popPose();
	}

	public static class RenderState extends EntityRenderState {
		public float yRot;
		public float xRot;
		public float roll;
		public final ItemStackRenderState item = new ItemStackRenderState();
	}
}
