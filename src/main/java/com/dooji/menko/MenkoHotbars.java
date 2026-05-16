package com.dooji.menko;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class MenkoHotbars {
	private static final Identifier SAVED_HOTBARS_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "saved_hotbars");
	private static final SavedDataType<SavedHotbarStorage> SAVED_HOTBARS_TYPE = new SavedDataType<>(
		SAVED_HOTBARS_ID,
		SavedHotbarStorage::new,
		SavedHotbarStorage.CODEC,
		DataFixTypes.HOTBAR
	);

	static void save(ServerPlayer player) {
		if (player == null) {
			return;
		}

		SavedHotbarStorage storage = storage(player);
		if (storage.contains(player.getUUID())) {
			return;
		}

		ItemStack[] hotbar = new ItemStack[9];
		for (int slot = 0; slot < 9; slot++) {
			hotbar[slot] = player.getInventory().getItem(slot).copy();
		}

		storage.put(player.getUUID(), new SavedHotbar(hotbar, player.getInventory().getSelectedSlot()));
		persist(player, storage);
	}

	static void restore(ServerPlayer player) {
		if (player == null) {
			return;
		}

		SavedHotbarStorage storage = storage(player);
		SavedHotbar saved = storage.remove(player.getUUID());
		if (saved == null) {
			return;
		}

		for (int slot = 0; slot < 9; slot++) {
			player.getInventory().setItem(slot, saved.hotbar[slot].copy());
		}

		player.getInventory().setSelectedSlot(saved.selectedSlot);
		sync(player);
		persist(player, storage);
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

	static void giveCard(ServerPlayer player, Item item) {
		addCard(player, item);
		if (player.getInventory().getItem(player.getInventory().getSelectedSlot()).isEmpty()) {
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

	private static SavedHotbarStorage storage(ServerPlayer player) {
		SavedDataStorage dataStorage = player.level().getServer().overworld().getDataStorage();
		return dataStorage.computeIfAbsent(SAVED_HOTBARS_TYPE);
	}

	private static void persist(ServerPlayer player, SavedHotbarStorage storage) {
		storage.setDirty();
		player.level().getServer().overworld().getDataStorage().saveAndJoin();
	}

	private static class SavedHotbar {
		private static final Codec<SavedHotbar> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.OPTIONAL_CODEC.listOf().fieldOf("hotbar").forGetter(SavedHotbar::hotbar),
			Codec.INT.fieldOf("selected_slot").forGetter(saved -> saved.selectedSlot)
		).apply(instance, SavedHotbar::fromCodec));

		private final ItemStack[] hotbar;
		private final int selectedSlot;

		private SavedHotbar(ItemStack[] hotbar, int selectedSlot) {
			this.hotbar = normalizeHotbar(Arrays.asList(hotbar));
			this.selectedSlot = Math.max(0, Math.min(8, selectedSlot));
		}

		private static SavedHotbar fromCodec(List<ItemStack> hotbar, int selectedSlot) {
			return new SavedHotbar(normalizeHotbar(hotbar), selectedSlot);
		}

		private List<ItemStack> hotbar() {
			List<ItemStack> items = new ArrayList<>(9);
			for (ItemStack stack : this.hotbar) {
				items.add(stack.copy());
			}

			return items;
		}

		private static ItemStack[] normalizeHotbar(List<ItemStack> hotbar) {
			ItemStack[] normalized = new ItemStack[9];
			for (int slot = 0; slot < normalized.length; slot++) {
				normalized[slot] = slot < hotbar.size() ? hotbar.get(slot).copy() : ItemStack.EMPTY;
			}

			return normalized;
		}
	}

	private static class SavedHotbarStorage extends SavedData {
		private static final Codec<SavedHotbarStorage> CODEC = SavedHotbarEntry.CODEC.listOf().xmap(SavedHotbarStorage::new, SavedHotbarStorage::entries);

		private final LinkedHashMap<UUID, SavedHotbar> savedHotbars = new LinkedHashMap<>();

		private SavedHotbarStorage() {
		}

		private SavedHotbarStorage(List<SavedHotbarEntry> entries) {
			for (SavedHotbarEntry entry : entries) {
				this.savedHotbars.put(entry.playerId(), entry.hotbar());
			}
		}

		private boolean contains(UUID playerId) {
			return this.savedHotbars.containsKey(playerId);
		}

		private void put(UUID playerId, SavedHotbar hotbar) {
			this.savedHotbars.put(playerId, hotbar);
		}

		private SavedHotbar remove(UUID playerId) {
			return this.savedHotbars.remove(playerId);
		}

		private List<SavedHotbarEntry> entries() {
			List<SavedHotbarEntry> entries = new ArrayList<>(this.savedHotbars.size());
			for (Map.Entry<UUID, SavedHotbar> entry : this.savedHotbars.entrySet()) {
				entries.add(new SavedHotbarEntry(entry.getKey(), entry.getValue()));
			}

			return entries;
		}
	}

	private record SavedHotbarEntry(UUID playerId, SavedHotbar hotbar) {
		private static final Codec<SavedHotbarEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("player_id").forGetter(SavedHotbarEntry::playerId),
			SavedHotbar.CODEC.fieldOf("hotbar").forGetter(SavedHotbarEntry::hotbar)
		).apply(instance, SavedHotbarEntry::new));
	}
}
