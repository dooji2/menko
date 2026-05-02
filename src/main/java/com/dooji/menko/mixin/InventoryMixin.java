package com.dooji.menko.mixin;

import com.dooji.menko.MenkoGameManager;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
abstract class InventoryMixin {
	@Shadow @Final public Player player;
	@Shadow @Final private NonNullList<ItemStack> items;

	@Shadow
	public abstract ItemStack getItem(int slot);

	@Shadow
	protected abstract boolean hasRemainingSpaceForItem(ItemStack slotItemStack, ItemStack newItemStack);

	@Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
	private void skipHotbarFreeSlot(CallbackInfoReturnable<Integer> cir) {
		if (!this.isSessionActive()) {
			return;
		}

		int slot = cir.getReturnValue();
		if (!Inventory.isHotbarSlot(slot)) {
			return;
		}

		for (int i = 9; i < this.items.size(); i++) {
			if (this.items.get(i).isEmpty()) {
				cir.setReturnValue(i);
				return;
			}
		}

		cir.setReturnValue(-1);
	}

	@Inject(method = "getSlotWithRemainingSpace", at = @At("RETURN"), cancellable = true)
	private void skipHotbarStackSpace(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
		if (!this.isSessionActive()) {
			return;
		}

		int slot = cir.getReturnValue();
		if (!Inventory.isHotbarSlot(slot)) {
			return;
		}

		for (int i = 9; i < this.items.size(); i++) {
			if (this.hasRemainingSpaceForItem(this.getItem(i), itemStack)) {
				cir.setReturnValue(i);
				return;
			}
		}

		cir.setReturnValue(-1);
	}

	@Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
	private void blockSelectedHotbarRemove(boolean all, CallbackInfoReturnable<ItemStack> cir) {
		if (this.isSessionActive()) {
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}

	@Unique
	private boolean isSessionActive() {
		return MenkoGameManager.isInSession(this.player.getUUID());
	}
}
