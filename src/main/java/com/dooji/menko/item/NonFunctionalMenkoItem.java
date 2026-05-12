package com.dooji.menko.item;

import com.dooji.menko.MenkoEntityTypes;
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

public class NonFunctionalMenkoItem extends Item {
	private final int despawnTicks;

	public NonFunctionalMenkoItem(Properties properties) {
		this(properties, 100);
	}

	public NonFunctionalMenkoItem(Properties properties, int despawnTicks) {
		super(properties);
		this.despawnTicks = despawnTicks;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 20;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
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

		MenkoCardEntity card = new MenkoCardEntity(MenkoEntityTypes.MENKO_CARD, serverLevel);
		card.setOwnerId(player.getUUID());
		card.setCardItem(stack.getItem());
		card.setGameId(null);
		card.setInGame(false);
		card.setTurnCard(false);
		card.setNonFunctional(true);
		if (this.despawnTicks > 0) {
			card.setDespawnTicks(this.despawnTicks);
		}
		card.throwFrom(player, MenkoCardItem.getChargeForUseTicks(this.getUseDuration(stack, player) - remainingUseTicks));
		serverLevel.addFreshEntity(card);
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}

		return true;
	}
}
