package com.dooji.menko.item;

import com.dooji.menko.MenkoGameManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class MenkoStartItem extends Item {
	public MenkoStartItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof net.minecraft.server.level.ServerLevel) {
			MenkoGameManager.useStartItem((ServerPlayer) player);
		}

		return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}
}
