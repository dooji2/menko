package com.dooji.menko.mixin;

import com.dooji.menko.MenkoGameManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
abstract class SlotMixin {
	@Shadow @Final public Container container;

	@Shadow
	public abstract int getContainerSlot();

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void lockHotbarPlacement(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
		if (this.isLockedHotbarSlot()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void lockHotbarPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (this.isLockedHotbarSlot()) {
			cir.setReturnValue(false);
		}
	}

	@Unique
	private boolean isLockedHotbarSlot() {
		if (!(this.container instanceof Inventory inventory)) {
			return false;
		}

		return Inventory.isHotbarSlot(this.getContainerSlot()) && MenkoGameManager.isInSession(inventory.player.getUUID());
	}
}
