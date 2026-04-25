package com.dooji.menko;

import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class MenkoHotbars {
	private static final Map<UUID, SavedHotbar> SAVED_HOTBARS = new HashMap<>();

	static void save(ServerPlayer player) {
		if (player == null || SAVED_HOTBARS.containsKey(player.getUUID())) {
			return;
		}

		ItemStack[] hotbar = new ItemStack[9];
		for (int slot = 0; slot < 9; slot++) {
			hotbar[slot] = player.getInventory().getItem(slot).copy();
		}

		SAVED_HOTBARS.put(player.getUUID(), new SavedHotbar(hotbar, player.getInventory().getSelectedSlot()));
	}

	static void restore(ServerPlayer player) {
		if (player == null) {
			return;
		}

		SavedHotbar saved = SAVED_HOTBARS.remove(player.getUUID());
		if (saved == null) {
			return;
		}

		for (int slot = 0; slot < 9; slot++) {
			player.getInventory().setItem(slot, saved.hotbar[slot].copy());
		}

		player.getInventory().setSelectedSlot(saved.selectedSlot);
		sync(player);
	}

	static void clear() {
		SAVED_HOTBARS.clear();
	}

	static void showPending(ServerPlayer player, boolean owner) {
		player.getInventory().setItem(0, new ItemStack(MenkoItems.LEAVE));
		player.getInventory().setItem(1, owner ? new ItemStack(MenkoItems.START) : ItemStack.EMPTY);
		for (int slot = 2; slot < 9; slot++) {
			player.getInventory().setItem(slot, ItemStack.EMPTY);
		}

		player.getInventory().setSelectedSlot(0);
		sync(player);
	}

	static void showGame(ServerPlayer player, int cards) {
		player.getInventory().setItem(0, new ItemStack(MenkoItems.LEAVE));
		clearRestOfHotbar(player);
		if (cards > 0) {
			for (int i = 0; i < cards; i++) {
				addCard(player, MenkoItems.randomMenko(player.getRandom()));
			}
		}

		player.getInventory().setSelectedSlot(cards > 0 ? firstCardSlot(player) : 0);
		sync(player);
	}

	static void giveCards(ServerPlayer player, int count) {
		for (int i = 0; i < count; i++) {
			addCard(player, MenkoItems.randomMenko(player.getRandom()));
		}

		if (count > 0 && player.getInventory().getItem(player.getInventory().getSelectedSlot()).isEmpty()) {
			player.getInventory().setSelectedSlot(firstCardSlot(player));
		}

		sync(player);
	}

	private static void clearRestOfHotbar(ServerPlayer player) {
		for (int slot = 1; slot < 9; slot++) {
			player.getInventory().setItem(slot, ItemStack.EMPTY);
		}
	}

	private static void addCard(ServerPlayer player, Item item) {
		for (int slot = 1; slot < 9; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				stack.grow(1);
				return;
			}
		}

		for (int slot = 1; slot < 9; slot++) {
			if (player.getInventory().getItem(slot).isEmpty()) {
				player.getInventory().setItem(slot, new ItemStack(item));
				return;
			}
		}
	}

	private static int firstCardSlot(ServerPlayer player) {
		for (int slot = 1; slot < 9; slot++) {
			if (!player.getInventory().getItem(slot).isEmpty()) {
				return slot;
			}
		}

		return 0;
	}

	private static void sync(ServerPlayer player) {
		player.stopUsingItem();
		player.getInventory().setChanged();
		player.containerMenu.sendAllDataToRemote();
		player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
	}

	private static class SavedHotbar {
		private final ItemStack[] hotbar;
		private final int selectedSlot;

		private SavedHotbar(ItemStack[] hotbar, int selectedSlot) {
			this.hotbar = hotbar;
			this.selectedSlot = selectedSlot;
		}
	}
}
