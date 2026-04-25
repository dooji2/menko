package com.dooji.menko.item;

import com.dooji.menko.MenkoGameManager;
import com.dooji.menko.entity.MenkoCardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MenkoCardItem extends Item {
	public MenkoCardItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel serverLevel && !MenkoGameManager.canStartThrow(serverLevel, (ServerPlayer) player)) {
			return InteractionResult.FAIL;
		}

		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 20;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.NONE;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseTicks) {
		if (!(level instanceof ServerLevel serverLevel) || !(livingEntity instanceof ServerPlayer player)) {
			return false;
		}

		float charge = getChargeForUseTicks(this.getUseDuration(stack, player) - remainingUseTicks);
		MenkoCardEntity menkoCard = MenkoGameManager.throwCard(serverLevel, player, stack, charge);
		if (menkoCard == null) {
			return false;
		}

		serverLevel.addFreshEntity(menkoCard);
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}

		return true;
	}

	public static float getChargeForUseTicks(int useTicks) {
		float progress = Math.clamp(useTicks / 20.0f, 0.0f, 1.0f);
		return 0.1f + 0.9f * progress;
	}
}
