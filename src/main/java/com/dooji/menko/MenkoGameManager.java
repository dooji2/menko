package com.dooji.menko;

import com.dooji.menko.entity.MenkoCardEntity;
import com.dooji.menko.network.MenkoHudPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MenkoGameManager {
	private static final double RADIUS = 1.5;
	private static final int STARTING_CARDS = 10;
	private static final Map<UUID, MenkoGame> MENKO_GAMES = new HashMap<>();
	private static final Map<UUID, UUID> PLAYER_MENKO_GAMES = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MenkoGameManager::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				MenkoHotbars.restore(player);
			}

			MENKO_GAMES.clear();
			PLAYER_MENKO_GAMES.clear();
			MenkoHotbars.clear();
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> leaveSession(handler.player));
	}

	public static boolean canStartThrow(ServerLevel level, ServerPlayer player) {
		MenkoGame game = getPlayerGame(player.getUUID());
		return game == null || getThrowPlayer(level, player, true) != null;
	}

	public static MenkoCardEntity throwCard(ServerLevel level, ServerPlayer player, ItemStack stack, float charge) {
		MenkoGame game = getPlayerGame(player.getUUID());
		if (game == null) {
			MenkoCardEntity card = new MenkoCardEntity(MenkoEntityTypes.MENKO_CARD, level);
			card.setOwnerId(player.getUUID());
			card.setCardItem(stack.getItem());
			card.setGameId(null);
			card.setInGame(false);
			card.setTurnCard(false);
			card.throwFrom(player, charge);
			sendHud(level, player.getUUID(), Component.translatable("hud.menko.throwing_card"), 0);
			return card;
		}

		MenkoPlayer data = getThrowPlayer(level, player, true);
		if (data == null) {
			return null;
		}

		MenkoCardEntity card = new MenkoCardEntity(MenkoEntityTypes.MENKO_CARD, level);
		card.setOwnerId(player.getUUID());
		card.setCardItem(stack.getItem());
		data.cards--;
		card.setGameId(game.id);
		card.setInGame(true);
		card.setTurnCard(true);
		card.throwFrom(player, charge);
		sendHud(level, player.getUUID(), Component.translatable("hud.menko.throwing_card"), 0);
		return card;
	}

	private static MenkoPlayer getThrowPlayer(ServerLevel level, ServerPlayer player, boolean showHud) {
		MenkoGame game = getPlayerGame(player.getUUID());
		if (game == null) {
			return null;
		}

		if (!game.dimension.equals(level.dimension())) {
			leaveSession(player);
			return null;
		}

		if (!game.started) {
			if (showHud) {
				sendHud(level, player.getUUID(), Component.translatable("hud.menko.use_start_or_leave"), 0);
			}

			return null;
		}

		if (!game.isTurn(player.getUUID())) {
			if (showHud) {
				sendHud(level, player.getUUID(), Component.translatable("hud.menko.not_your_turn"), 0);
			}
			return null;
		}

		MenkoPlayer data = game.getPlayer(player.getUUID());
		if (data == null || data.left || data.cards <= 0) {
			if (showHud) {
				sendHud(level, player.getUUID(), Component.translatable("hud.menko.no_cards"), 0);
			}

			return null;
		}

		return data;
	}

	public static void onCardLanded(ServerLevel level, MenkoCardEntity card) {
		if (card.isRemoved() || card.getOwnerId() == null) {
			return;
		}

		if (card.isInGame()) {
			landThrownCard(level, card);
		} else {
			landJoinCard(level, card);
		}
	}

	public static void useLeaveItem(ServerPlayer player) {
		leaveSession(player);
	}

	public static void useStartItem(ServerPlayer player) {
		MenkoGame game = getPlayerGame(player.getUUID());
		if (game == null || game.started || !player.getUUID().equals(game.ownerId)) {
			return;
		}

		if (game.activePlayers() < 2) {
			sendHud(player.level(), player.getUUID(), Component.translatable("hud.menko.need_two_players"), 0);
			return;
		}

		startMenko(player.level(), game);
	}

	private static void tick(MinecraftServer server) {
		Iterator<MenkoGame> iterator = MENKO_GAMES.values().iterator();
		while (iterator.hasNext()) {
			MenkoGame game = iterator.next();
			ServerLevel level = server.getLevel(game.dimension);
			if (level == null) {
				for (MenkoPlayer player : game.players) {
					PLAYER_MENKO_GAMES.remove(player.id);
				}

				iterator.remove();
				continue;
			}

			if (!game.started) {
				if (game.activePlayers() == 0) {
					iterator.remove();
				}

				continue;
			}

			recountCards(level, game);
			if (game.activePlayers() <= 1) {
				endMenko(level, game, false);
				iterator.remove();
			}
		}
	}

	private static void landJoinCard(ServerLevel level, MenkoCardEntity card) {
		if (PLAYER_MENKO_GAMES.containsKey(card.getOwnerId())) {
			card.discard();
			return;
		}

		if (findMenkoGame(level, card.position(), true) != null) {
			card.discard();
			return;
		}

		MenkoGame game = findMenkoGame(level, card.position(), false);
		if (game == null) {
			game = new MenkoGame(level.dimension(), card.position(), card.getOwnerId());
			game.players.add(new MenkoPlayer(card.getOwnerId(), card.getUUID()));
			MENKO_GAMES.put(game.id, game);
			PLAYER_MENKO_GAMES.put(card.getOwnerId(), game.id);
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(card.getOwnerId());
			if (player != null) {
				MenkoHotbars.save(player);
			}

			refreshPendingHotbar(level, game);
			send(level, card.getOwnerId(), Component.translatable("hud.menko.created_game"));
			return;
		}

		game.players.add(new MenkoPlayer(card.getOwnerId(), card.getUUID()));
		PLAYER_MENKO_GAMES.put(card.getOwnerId(), game.id);
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(card.getOwnerId());
		if (player != null) {
			MenkoHotbars.save(player);
		}

		send(level, card.getOwnerId(), Component.translatable("hud.menko.joined_game"));
		message(level, game, Component.translatable("hud.menko.player_joined", name(level, card.getOwnerId())));
		refreshPendingHotbar(level, game);
	}

	private static void landThrownCard(ServerLevel level, MenkoCardEntity card) {
		MenkoGame game = MENKO_GAMES.get(card.getGameId());
		if (game == null || !game.started) {
			card.discard();
			return;
		}

		MenkoPlayer thrower = game.getPlayer(card.getOwnerId());
		if (thrower == null || thrower.left || !game.isTurn(card.getOwnerId())) {
			card.discard();
			return;
		}

		if (!card.isTurnCard()) {
			if (!game.fieldCards.contains(card.getUUID())) {
				game.fieldCards.add(card.getUUID());
			}

			return;
		}

		if (card.position().distanceToSqr(game.center) > RADIUS * RADIUS) {
			card.discard();
			addCards(level, thrower.id, 1);
			return;
		}

		int captured = card.getCapturedCards();
		if (captured > 0) {
			card.discard();
			addCards(level, thrower.id, captured + 1);
			message(level, game, Component.translatable("hud.menko.player_flipped", name(level, thrower.id), captured));
			sendHud(level, thrower.id, Component.translatable("hud.menko.got_cards"), 0);
		} else {
			card.setTurnCard(false);
			game.fieldCards.add(card.getUUID());
			thrower.fieldCards++;
		}

		kickOutEmptyPlayers(level, game);
		if (game.activePlayers() <= 1) {
			endMenko(level, game, false);
			MENKO_GAMES.remove(game.id);
			return;
		}

		nextPlayer(level, game);
	}

	public static int resolveImpact(ServerLevel level, MenkoCardEntity thrown, double impactSpeed, double impactTilt) {
		MenkoGame game = MENKO_GAMES.get(thrown.getGameId());
		if (game == null || !game.started) {
			return 0;
		}

		double power = impactSpeed * Mth.clamp(impactTilt / 60.0, 0.25, 1.2);
		if (power < 0.18) {
			return 0;
		}

		int flipped = 0;
		for (UUID cardId : new ArrayList<>(game.fieldCards)) {
			MenkoCardEntity target = (MenkoCardEntity) level.getEntity(cardId);
			if (target == null || target.isRemoved() || thrown.getOwnerId().equals(target.getOwnerId())) {
				continue;
			}

			Vec3 delta = target.position().subtract(thrown.position());
			double distSqr = delta.horizontalDistanceSqr();
			if (distSqr > 0.75 * 0.75 || Math.abs(delta.y) > 0.35) {
				continue;
			}

			double dist = delta.horizontalDistance();
			double edgeDistance = distanceToEdge(target, thrown.position());
			double edgeHit = 1.0 - Math.min(1.0, edgeDistance / 0.22);
			double centerHit = 1.0 - Math.min(1.0, dist / 0.18);
			double score = power * (0.45 + edgeHit * 1.25 - centerHit * 0.9);
			if (edgeHit < 0.2 || score < 0.24) {
				continue;
			}

			MenkoPlayer owner = game.getPlayer(target.getOwnerId());
			if (owner != null && owner.fieldCards > 0) {
				owner.fieldCards--;
			}

			game.fieldCards.remove(cardId);
			target.playFlipAnimation(thrown.position(), power);
			flipped++;
		}

		return flipped;
	}

	private static double distanceToEdge(MenkoCardEntity card, Vec3 pos) {
		double yaw = Math.toRadians(-card.getYRot());
		double dx = pos.x - card.getX();
		double dz = pos.z - card.getZ();
		double x = dx * Math.cos(yaw) - dz * Math.sin(yaw);
		double z = dx * Math.sin(yaw) + dz * Math.cos(yaw);
		double halfWidth = MenkoCardEntity.CARD_WIDTH * 0.5;
		double halfHeight = MenkoCardEntity.CARD_HEIGHT * 0.5;
		if (Math.abs(x) <= halfWidth && Math.abs(z) <= halfHeight) {
			return Math.min(halfWidth - Math.abs(x), halfHeight - Math.abs(z));
		}

		double outsideX = Math.max(Math.abs(x) - halfWidth, 0.0);
		double outsideZ = Math.max(Math.abs(z) - halfHeight, 0.0);
		return Math.sqrt(outsideX * outsideX + outsideZ * outsideZ);
	}

	private static void startMenko(ServerLevel level, MenkoGame game) {
		if (game.started) {
			return;
		}

		game.started = true;
		Collections.shuffle(game.players);
		game.fieldCards.clear();
		game.turn = 0;
		for (MenkoPlayer player : game.players) {
			if (player.left) {
				continue;
			}

			player.cards = STARTING_CARDS;
			player.fieldCards = 0;

			MenkoCardEntity joinCard = (MenkoCardEntity) level.getEntity(player.joinCardId);
			if (joinCard != null && !joinCard.isRemoved()) {
				joinCard.setGameId(game.id);
				joinCard.setInGame(true);
				joinCard.setTurnCard(false);
				if (!game.fieldCards.contains(joinCard.getUUID())) {
					game.fieldCards.add(joinCard.getUUID());
				}

				player.fieldCards = 1;
			}
		}

		while (game.turn < game.players.size() && (game.players.get(game.turn).left || game.players.get(game.turn).cards <= 0)) {
			game.turn++;
		}

		if (game.turn >= game.players.size()) {
			endMenko(level, game, false);
			MENKO_GAMES.remove(game.id);
			return;
		}

		refreshGameHotbar(level, game);
		message(level, game, Component.translatable("hud.menko.started"));
		announceTurn(level, game);
	}

	private static void nextPlayer(ServerLevel level, MenkoGame game) {
		for (int i = 0; i < game.players.size(); i++) {
			game.turn = (game.turn + 1) % game.players.size();
			MenkoPlayer player = game.players.get(game.turn);
			if (!player.left && player.cards > 0) {
				announceTurn(level, game);
				return;
			}
		}
	}

	private static void kickOutEmptyPlayers(ServerLevel level, MenkoGame game) {
		for (MenkoPlayer player : game.players) {
			if (!player.left && player.cards <= 0 && player.fieldCards <= 0) {
				player.left = true;
				PLAYER_MENKO_GAMES.remove(player.id);
				sendHud(level, player.id, Component.translatable("hud.menko.you_are_out"), 120);
				MenkoHotbars.restore(level.getServer().getPlayerList().getPlayer(player.id));
				message(level, game, Component.translatable("hud.menko.player_out", name(level, player.id)));
			}
		}
	}

	private static void recountCards(ServerLevel level, MenkoGame game) {
		Iterator<UUID> iterator = game.fieldCards.iterator();
		while (iterator.hasNext()) {
			MenkoCardEntity card = (MenkoCardEntity) level.getEntity(iterator.next());
			if (card == null || card.isRemoved()) {
				iterator.remove();
			}
		}

		for (MenkoPlayer player : game.players) {
			player.fieldCards = 0;
		}

		for (UUID cardId : game.fieldCards) {
			MenkoCardEntity card = (MenkoCardEntity) level.getEntity(cardId);
			if (card != null && !card.isRemoved()) {
				MenkoPlayer owner = game.getPlayer(card.getOwnerId());
				if (owner != null) {
					owner.fieldCards++;
				}
			}
		}
	}

	public static void leaveSession(ServerPlayer player) {
		MenkoGame game = getPlayerGame(player.getUUID());
		if (game == null) {
			MenkoHotbars.restore(player);
			return;
		}

		ServerLevel level = (ServerLevel) player.level();
		boolean cancelPendingGame = !game.started && player.getUUID().equals(game.ownerId);
		leave(level, game, player.getUUID());
		if (cancelPendingGame) {
			endMenko(level, game, true);
			MENKO_GAMES.remove(game.id);
			return;
		}

		if (game.started) {
			if (game.activePlayers() <= 1) {
				endMenko(level, game, false);
				MENKO_GAMES.remove(game.id);
			}
			return;
		}

		if (game.activePlayers() == 0) {
			endMenko(level, game, false);
			MENKO_GAMES.remove(game.id);
			return;
		}

		refreshPendingHotbar(level, game);
	}

	private static void leave(ServerLevel level, MenkoGame game, UUID playerId) {
		MenkoPlayer player = game.getPlayer(playerId);
		if (player == null || player.left) {
			return;
		}

		boolean wasTurn = game.started && game.isTurn(playerId);

		player.left = true;
		PLAYER_MENKO_GAMES.remove(playerId);
		sendHud(level, playerId, Component.translatable("hud.menko.you_left"), 120);
		MenkoCardEntity joinCard = (MenkoCardEntity) level.getEntity(player.joinCardId);
		if (joinCard != null) {
			joinCard.discard();
		}

		game.fieldCards.remove(player.joinCardId);
		for (UUID cardId : new ArrayList<>(game.fieldCards)) {
			MenkoCardEntity card = (MenkoCardEntity) level.getEntity(cardId);
			if (card != null && playerId.equals(card.getOwnerId())) {
				card.discard();
				game.fieldCards.remove(cardId);
			}
		}

		MenkoHotbars.restore(level.getServer().getPlayerList().getPlayer(playerId));
		message(level, game, Component.translatable("hud.menko.player_left", name(level, playerId)));

		if (game.started && wasTurn) {
			nextPlayer(level, game);
		}
	}

	private static void endMenko(ServerLevel level, MenkoGame game, boolean cancelled) {
		UUID winner = null;
		for (MenkoPlayer player : game.players) {
			PLAYER_MENKO_GAMES.remove(player.id);
			MenkoHotbars.restore(level.getServer().getPlayerList().getPlayer(player.id));
			MenkoCardEntity joinCard = (MenkoCardEntity) level.getEntity(player.joinCardId);
			if (joinCard != null) {
				joinCard.discard();
			}

			if (!player.left) {
				winner = player.id;
			}
		}

		if (cancelled) {
			sendHudToAll(level, game, Component.translatable("hud.menko.cancelled"), 120);
		} else if (winner != null) {
			for (MenkoPlayer player : game.players) {
				sendHud(level, player.id, player.id.equals(winner) ? Component.translatable("hud.menko.you_won") : Component.translatable("hud.menko.player_won", name(level, winner)), 120);
			}
		}

		for (UUID cardId : new ArrayList<>(game.fieldCards)) {
			MenkoCardEntity card = (MenkoCardEntity) level.getEntity(cardId);
			if (card != null) {
				card.discard();
			}
		}
	}

	private static MenkoGame findMenkoGame(ServerLevel level, Vec3 pos, boolean started) {
		for (MenkoGame game : MENKO_GAMES.values()) {
			if (game.started == started && game.dimension.equals(level.dimension()) && game.center.distanceToSqr(pos) <= RADIUS * RADIUS) {
				return game;
			}
		}

		return null;
	}

	private static MenkoGame getPlayerGame(UUID playerId) {
		UUID gameId = PLAYER_MENKO_GAMES.get(playerId);
		return gameId == null ? null : MENKO_GAMES.get(gameId);
	}

	private static void addCards(ServerLevel level, UUID playerId, int count) {
		if (count <= 0) {
			return;
		}

		MenkoGame game = getPlayerGame(playerId);
		if (game == null || !game.started) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
			if (player != null) {
				MenkoHotbars.giveCards(player, count);
			}

			return;
		}

		MenkoPlayer data = game.getPlayer(playerId);
		if (data == null || data.left) {
			return;
		}

		data.cards += count;
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
		if (player != null) {
			MenkoHotbars.showGame(player, data.cards);
		}
	}

	private static void refreshPendingHotbar(ServerLevel level, MenkoGame game) {
		for (MenkoPlayer player : game.players) {
			if (player.left) {
				continue;
			}

			ServerPlayer serverPlayer = level.getServer().getPlayerList().getPlayer(player.id);
			if (serverPlayer != null) {
				MenkoHotbars.showPending(serverPlayer, player.id.equals(game.ownerId));
				sendHud(level, player.id, Component.translatable("hud.menko.waiting", game.activePlayers()), 0);
			}
		}
	}

	private static void refreshGameHotbar(ServerLevel level, MenkoGame game) {
		for (MenkoPlayer player : game.players) {
			if (player.left) {
				continue;
			}

			ServerPlayer serverPlayer = level.getServer().getPlayerList().getPlayer(player.id);
			if (serverPlayer != null) {
				MenkoHotbars.showGame(serverPlayer, player.cards);
			}
		}
	}

	private static void message(ServerLevel level, MenkoGame game, Component text) {
		for (MenkoPlayer player : game.players) {
			if (!player.left) {
				send(level, player.id, text);
			}
		}
	}

	private static void send(ServerLevel level, UUID playerId, Component text) {
		sendHud(level, playerId, text, 0);
	}

	private static void announceTurn(ServerLevel level, MenkoGame game) {
		UUID currentPlayer = game.currentPlayer();
		if (currentPlayer == null) {
			return;
		}

		for (MenkoPlayer player : game.players) {
			if (!player.left) {
				sendHud(level, player.id, player.id.equals(currentPlayer) ? Component.translatable("hud.menko.your_turn") : Component.translatable("hud.menko.player_turn", name(level, currentPlayer)), 0);
			}
		}
	}

	private static void sendHudToAll(ServerLevel level, MenkoGame game, Component text, int durationTicks) {
		for (MenkoPlayer player : game.players) {
			sendHud(level, player.id, text, durationTicks);
		}
	}

	private static void sendHud(ServerLevel level, UUID playerId, Component text, int durationTicks) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
		if (player != null) {
			MenkoGame game = getPlayerGame(playerId);
			boolean canCharge = true;
			if (game != null && game.dimension.equals(level.dimension())) {
				MenkoPlayer data = game.getPlayer(playerId);
				canCharge = game.started && game.isTurn(playerId) && data != null && !data.left && data.cards > 0;
			}

			ServerPlayNetworking.send(player, new MenkoHudPayload(text, durationTicks, canCharge));
		}
	}

	private static String name(ServerLevel level, UUID playerId) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
		return player == null ? "Player" : player.getName().getString();
	}

	private static class MenkoGame {
		private final UUID id = UUID.randomUUID();
		private final ResourceKey<Level> dimension;
		private final Vec3 center;
		private final List<MenkoPlayer> players = new ArrayList<>();
		private final List<UUID> fieldCards = new ArrayList<>();
		private UUID ownerId;
		private int turn;
		private boolean started;

		private MenkoGame(ResourceKey<Level> dimension, Vec3 center, UUID ownerId) {
			this.dimension = dimension;
			this.center = center;
			this.ownerId = ownerId;
		}

		private MenkoPlayer getPlayer(UUID playerId) {
			for (MenkoPlayer player : this.players) {
				if (player.id.equals(playerId)) {
					return player;
				}
			}

			return null;
		}

		private int activePlayers() {
			int count = 0;
			for (MenkoPlayer player : this.players) {
				if (!player.left) {
					count++;
				}
			}

			return count;
		}

		private UUID currentPlayer() {
			return this.players.isEmpty() ? null : this.players.get(this.turn).id;
		}

		private boolean isTurn(UUID playerId) {
			UUID current = this.currentPlayer();
			return current != null && current.equals(playerId);
		}
	}

	private static class MenkoPlayer {
		private final UUID id;
		private final UUID joinCardId;
		private int cards;
		private int fieldCards;
		private boolean left;

		private MenkoPlayer(UUID id, UUID joinCardId) {
			this.id = id;
			this.joinCardId = joinCardId;
		}
	}
}
