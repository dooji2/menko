package com.dooji.menko;

import com.dooji.menko.entity.MenkoCardEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public final class MenkoDispenserBehaviors {
	private MenkoDispenserBehaviors() {
	}

	public static void init() {
		DefaultDispenseItemBehavior functionalBehavior = menkoBehavior(false, 0);
		DefaultDispenseItemBehavior temporaryNonFunctionalBehavior = menkoBehavior(true, 100);
		DefaultDispenseItemBehavior persistentNonFunctionalBehavior = menkoBehavior(true, 0);

		for (Item item : MenkoItems.MENKO_CARDS) {
			DispenserBlock.registerBehavior(item, functionalBehavior);
		}

		for (Item item : MenkoItems.NON_FUNCTIONAL_MENKO_CARDS) {
			DispenserBlock.registerBehavior(item, temporaryNonFunctionalBehavior);
		}

		for (Item item : MenkoItems.PERSISTENT_NON_FUNCTIONAL_MENKO_CARDS) {
			DispenserBlock.registerBehavior(item, persistentNonFunctionalBehavior);
		}
	}

	private static DefaultDispenseItemBehavior menkoBehavior(boolean nonFunctional, int despawnTicks) {
		return new DefaultDispenseItemBehavior() {
			@Override
			protected ItemStack execute(BlockSource source, ItemStack stack) {
				Item item = stack.getItem();
				Direction direction = source.state().getValue(DispenserBlock.FACING);
				Vec3 spawnPos = source.center().add(direction.getUnitVec3().scale(0.72));
				MenkoCardEntity card = new MenkoCardEntity(MenkoEntityTypes.MENKO_CARD, source.level());
				card.setCardItem(item);
				card.setGameId(null);
				card.setInGame(false);
				card.setTurnCard(false);
				card.setNonFunctional(nonFunctional);
				if (despawnTicks > 0) {
					card.setDespawnTicks(despawnTicks);
				}

				card.throwFromDispenser(spawnPos, direction, 0.55f);
				source.level().addFreshEntity(card);
				stack.shrink(1);
				return stack;
			}
		};
	}
}
