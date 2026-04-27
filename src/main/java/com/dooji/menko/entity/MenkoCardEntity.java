package com.dooji.menko.entity;

import com.dooji.menko.MenkoItems;
import com.dooji.menko.MenkoGameManager;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;

import java.util.UUID;

public class MenkoCardEntity extends Entity {
	public static final float CARD_WIDTH = 7.0f / 16.0f;
	public static final float CARD_HEIGHT = 12.0f / 16.0f;
	public static final float CARD_THICKNESS = 0.5f / 16.0f;
	private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(MenkoCardEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(MenkoCardEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> CARD_ITEM = SynchedEntityData.defineId(MenkoCardEntity.class, EntityDataSerializers.STRING);

	private CardBody physics;
	private float roll;
	private float rollOld;
	private int stillTicks;
	private double maxImpactSpeed;
	private double maxImpactTilt;
	private boolean impactResolved;
	private int capturedCards;
	private int capturedTicks;
	private int despawnTicks;
	private UUID ownerId;
	private UUID gameId;
	private boolean inGame;
	private boolean turnCard;
	private boolean nonFunctional;

	public MenkoCardEntity(EntityType<? extends MenkoCardEntity> entityType, Level level) {
		super(entityType, level);
	}

	public void throwFrom(Player player, float charge) {
		Vec3 forward = player.getViewVector(1.0f).normalize();
		Vec3 eyePos = player.getEyePosition();
		Vec3 rayEnd = eyePos.add(forward.scale(6.0));
		HitResult blockHit = player.level().clip(new ClipContext(eyePos, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 targetPos = blockHit.getType() == HitResult.Type.MISS ? rayEnd : blockHit.getLocation();
		AABB targetBox = new AABB(eyePos, rayEnd).inflate(0.3);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eyePos, rayEnd, targetBox, entity -> entity instanceof MenkoCardEntity && entity != this, targetPos.distanceToSqr(eyePos));
		if (entityHit != null) {
			targetPos = entityHit.getLocation();
		}

		Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
		if (right.lengthSqr() < 0.000001) {
			right = Vec3.directionFromRotation(0.0f, player.getYRot() - 90.0f);
		} else {
			right = right.normalize();
		}

		HumanoidArm throwingArm = player.getMainArm();
		if (player.getUsedItemHand() != InteractionHand.MAIN_HAND && throwingArm == HumanoidArm.RIGHT) {
			throwingArm = HumanoidArm.LEFT;
		} else if (player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
			throwingArm = HumanoidArm.RIGHT;
		}

		double armOffset = 0.38;
		if (throwingArm != HumanoidArm.RIGHT) {
			armOffset = -armOffset;
		}

		Vec3 spawnPos = eyePos.add(right.scale(armOffset)).add(0.0, -0.48, 0.0).add(forward.scale(0.78));
		Vec3 throwDir = targetPos.subtract(spawnPos);
		if (throwDir.lengthSqr() < 0.000001) {
			throwDir = forward;
		} else {
			throwDir = throwDir.normalize();
		}

		Vec3 velocity = throwDir.scale(0.2 + charge);

		this.setPos(spawnPos);
		this.setYRot(player.getYRot());
		this.setXRot(player.getXRot());
		this.setOldPos();
		this.setOldRot();
		this.setDeltaMovement(velocity);
		this.roll = 0.0f;
		this.rollOld = 0.0f;
		this.stillTicks = 0;
		this.maxImpactSpeed = 0.0;
		this.maxImpactTilt = 0.0;
		this.impactResolved = false;
		this.capturedCards = 0;
		this.entityData.set(ROLL, this.roll);
		this.entityData.set(LANDED, false);
		this.updateBounds();
		this.ensurePhysics().launch(this.position(), velocity, player.getXRot(), (player.getRandom().nextFloat() - 0.5f) * (4.0f + charge * 20.0f));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(ROLL, 0.0f);
		builder.define(LANDED, false);
		builder.define(CARD_ITEM, BuiltInRegistries.ITEM.getKey(MenkoItems.MENKO_NIGHTSKY).toString());
	}

	@Override
	public void tick() {
		super.tick();
		boolean wasLanded = this.hasLanded();
		this.rollOld = this.roll;

		if (this.level().isClientSide()) {
			this.roll = this.entityData.get(ROLL);
			this.updateBounds();
			return;
		}

		if (this.despawnTicks > 0) {
			this.despawnTicks--;
			if (this.despawnTicks <= 0) {
				this.discard();
				return;
			}
		}

		if (this.capturedTicks > 0) {
			this.capturedTicks--;
			if (this.capturedTicks <= 0) {
				this.discard();
				return;
			}
		}

		if (this.hasLanded()) {
			if (!this.hasGroundSupport()) {
				this.setLanded(false);
				this.stillTicks = 0;
				this.setDeltaMovement(0.0, -0.05, 0.0);
				this.ensurePhysics().sync(this.position(), this.getDeltaMovement());
			} else {
				this.setDeltaMovement(Vec3.ZERO);
				this.ensurePhysics().sync(this.position(), Vec3.ZERO);
				this.updateBounds();
				return;
			}
		}

		Vec3 prevPos = this.position();
		boolean wasOnGround = this.onGround();
		double fallSpeed = Math.max(0.0, -this.getDeltaMovement().y);
		CardBody body = this.ensurePhysics();
		body.step(prevPos, this.getDeltaMovement(), this.onGround());
		Vec3 moveVec = body.position().subtract(prevPos);
		this.setDeltaMovement(body.velocity());
		this.move(MoverType.SELF, moveVec);
		Vec3 realMove = this.position().subtract(prevPos);
		boolean blockedDownward = moveVec.y < 0.0 && realMove.y > moveVec.y + 0.000001;
		boolean groundContact = this.onGround() || blockedDownward;
		if (!wasOnGround && groundContact) {
			this.maxImpactSpeed = Math.max(this.maxImpactSpeed, fallSpeed);
			this.maxImpactTilt = Math.max(this.maxImpactTilt, Math.abs(body.pitch()));
			if (!this.impactResolved && this.turnCard && this.inGame && this.level() instanceof ServerLevel serverLevel) {
				this.impactResolved = true;
				this.capturedCards = MenkoGameManager.resolveImpact(serverLevel, this, fallSpeed, Math.abs(body.pitch()));
			}
		}

		if (realMove.distanceToSqr(moveVec) > 0.000001) {
			Vec3 slowVel = this.getDeltaMovement().multiply(0.44, groundContact ? 0.0 : 0.28, 0.44);
			this.setDeltaMovement(slowVel);
			body.collide(this.position(), slowVel, groundContact, fallSpeed);
		} else {
			body.sync(this.position(), this.getDeltaMovement());
		}

		this.setYRot(Mth.wrapDegrees(this.getYRot() + (float) (this.getDeltaMovement().horizontalDistance() * 24.0)));
		this.setXRot(body.pitch());
		this.roll = body.roll();

		if ((groundContact || this.hasGroundSupport()) && this.getDeltaMovement().lengthSqr() < 0.0012) {
			this.stillTicks++;
		} else {
			this.stillTicks = 0;
		}

		if (this.stillTicks >= 6) {
			this.setLanded(true);
			this.setDeltaMovement(Vec3.ZERO);
			this.setXRot(0.0f);
			this.roll = 0.0f;
			this.ensurePhysics().sync(this.position(), Vec3.ZERO);
		}

		this.entityData.set(ROLL, this.roll);
		this.updateBounds();
		if (!this.nonFunctional && !wasLanded && this.hasLanded() && this.capturedTicks <= 0 && this.level() instanceof ServerLevel serverLevel) {
			MenkoGameManager.onCardLanded(serverLevel, this);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.roll = input.getFloatOr("roll", 0.0f);
		this.rollOld = this.roll;
		this.stillTicks = input.getIntOr("still_ticks", 0);
		this.maxImpactSpeed = input.getDoubleOr("strongest_ground_impact_speed", 0.0);
		this.maxImpactTilt = input.getDoubleOr("strongest_ground_impact_tilt", 0.0);
		this.impactResolved = input.getBooleanOr("impact_resolved", false);
		this.capturedCards = input.getIntOr("captured_cards", 0);
		this.capturedTicks = input.getIntOr("captured_ticks", 0);
		this.despawnTicks = input.getIntOr("despawn_ticks", 0);
		this.entityData.set(ROLL, this.roll);
		this.entityData.set(LANDED, input.getBooleanOr("landed", false));
		this.entityData.set(CARD_ITEM, input.getStringOr("card_item", BuiltInRegistries.ITEM.getKey(MenkoItems.MENKO_NIGHTSKY).toString()));
		String owner = input.getStringOr("owner", "");
		String game = input.getStringOr("game", "");
		this.ownerId = owner.isEmpty() ? null : UUID.fromString(owner);
		this.gameId = game.isEmpty() ? null : UUID.fromString(game);
		this.inGame = input.getBooleanOr("in_game", false);
		this.turnCard = input.getBooleanOr("turn_card", false);
		this.nonFunctional = input.getBooleanOr("nonfunc", false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("roll", this.roll);
		output.putInt("still_ticks", this.stillTicks);
		output.putDouble("strongest_ground_impact_speed", this.maxImpactSpeed);
		output.putDouble("strongest_ground_impact_tilt", this.maxImpactTilt);
		output.putBoolean("impact_resolved", this.impactResolved);
		output.putInt("captured_cards", this.capturedCards);
		output.putInt("captured_ticks", this.capturedTicks);
		output.putInt("despawn_ticks", this.despawnTicks);
		output.putBoolean("landed", this.hasLanded());
		output.putString("card_item", this.entityData.get(CARD_ITEM));
		output.putString("owner", this.ownerId == null ? "" : this.ownerId.toString());
		output.putString("game", this.gameId == null ? "" : this.gameId.toString());
		output.putBoolean("in_game", this.inGame);
		output.putBoolean("turn_card", this.turnCard);
		output.putBoolean("nonfunc", this.nonFunctional);
	}

	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource damageSource, float damage) {
		this.discard();
		return true;
	}

	@Override
	public boolean canBeCollidedWith(Entity entity) {
		return true;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	public float getVisualRoll(float partialTick) {
		return Mth.lerp(partialTick, this.rollOld, this.roll);
	}

	public UUID getOwnerId() {
		return this.ownerId;
	}

	public Item getCardItem() {
		Identifier id = Identifier.tryParse(this.entityData.get(CARD_ITEM));
		if (id == null) {
			return MenkoItems.MENKO_NIGHTSKY;
		}

		Item item = BuiltInRegistries.ITEM.getValue(id);
		return item == null ? MenkoItems.MENKO_NIGHTSKY : item;
	}

	public void setCardItem(Item item) {
		this.entityData.set(CARD_ITEM, BuiltInRegistries.ITEM.getKey(item).toString());
	}

	public void setDespawnTicks(int despawnTicks) {
		this.despawnTicks = despawnTicks;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}

	public UUID getGameId() {
		return this.gameId;
	}

	public void setGameId(UUID gameId) {
		this.gameId = gameId;
	}

	public boolean isInGame() {
		return this.inGame;
	}

	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}

	public boolean isTurnCard() {
		return this.turnCard;
	}

	public void setTurnCard(boolean turnCard) {
		this.turnCard = turnCard;
	}

	public void setNonFunctional(boolean nonFunctional) {
		this.nonFunctional = nonFunctional;
	}

	public int getCapturedCards() {
		return this.capturedCards;
	}

	public void playFlipAnimation(Vec3 source, double power) {
		Vec3 away = this.position().subtract(source);
		Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
		if (horizontal.lengthSqr() < 0.000001) {
			horizontal = new Vec3(0.0, 0.0, 1.0);
		} else {
			horizontal = horizontal.normalize();
		}

		Vec3 velocity = horizontal.scale(0.08 + power * 0.2).add(0.0, 0.16 + power * 0.1, 0.0);
		this.setLanded(false);
		this.stillTicks = 0;
		this.capturedTicks = 12;
		this.setDeltaMovement(velocity);
		this.setXRot(-55.0f);
		this.ensurePhysics().launch(this.position(), velocity, -55.0f, 18.0f + (float) power * 24.0f);
	}

	@Override
	protected AABB makeBoundingBox(Vec3 position) {
		double halfWidth = CARD_WIDTH * 0.5;
		double halfHeight = CARD_HEIGHT * 0.5;
		double yaw = Math.toRadians(this.getYRot());
		double xExtent = Math.abs(Math.cos(yaw)) * halfWidth + Math.abs(Math.sin(yaw)) * halfHeight;
		double zExtent = Math.abs(Math.sin(yaw)) * halfWidth + Math.abs(Math.cos(yaw)) * halfHeight;
		return new AABB(
			position.x - xExtent,
			position.y,
			position.z - zExtent,
			position.x + xExtent,
			position.y + CARD_THICKNESS,
			position.z + zExtent
		);
	}

	public boolean hasLanded() {
		return this.entityData.get(LANDED);
	}

	private void setLanded(boolean landed) {
		this.entityData.set(LANDED, landed);
	}

	private void updateBounds() {
		this.setBoundingBox(this.makeBoundingBox());
	}

	private boolean hasGroundSupport() {
		return !this.level().noCollision(this, this.getBoundingBox().move(0.0, -0.02, 0.0));
	}

	private CardBody ensurePhysics() {
		if (this.physics == null) {
			this.physics = new CardBody();
		}

		return this.physics;
	}

	private static class CardBody {
		private Vec3 position = Vec3.ZERO;
		private Vec3 velocity = Vec3.ZERO;
		private float pitch;
		private float roll;
		private float pitchVelocity;
		private float rollVelocity;

		private void launch(Vec3 position, Vec3 velocity, float pitch, float rollVelocity) {
			this.position = position;
			this.velocity = velocity;
			this.pitch = pitch;
			this.roll = 0.0f;
			this.pitchVelocity = pitch * 0.055f;
			this.rollVelocity = rollVelocity;
		}

		private void sync(Vec3 position, Vec3 velocity) {
			this.position = position;
			this.velocity = velocity;
		}

		private void step(Vec3 position, Vec3 velocity, boolean onGround) {
			this.position = position;
			this.velocity = velocity;

			if (!onGround) {
				this.velocity = this.velocity.add(0.0, -0.05, 0.0).scale(0.988);
				this.pitchVelocity *= 0.988f;
				this.rollVelocity *= 0.994f;
			} else {
				this.velocity = this.velocity.multiply(0.64, 0.0, 0.64);
				this.pitchVelocity *= 0.68f;
				this.rollVelocity *= 0.62f;
			}

			this.position = this.position.add(this.velocity);
			this.pitch = Mth.wrapDegrees(this.pitch + this.pitchVelocity);
			this.roll = Mth.wrapDegrees(this.roll + this.rollVelocity);

			if (onGround) {
				float settle = 4.0f + (float) this.velocity.horizontalDistance() * 14.0f;
				this.pitch = Mth.approachDegrees(this.pitch, 0.0f, settle);
				this.roll = Mth.approachDegrees(this.roll, 0.0f, settle);
				this.pitch = Mth.clamp(this.pitch, -12.0f, 12.0f);
				this.roll = Mth.clamp(this.roll, -12.0f, 12.0f);
			}
		}

		private void collide(Vec3 position, Vec3 velocity, boolean onGround, double impactSpeed) {
			if (onGround) {
				float pitchSin = (float) Math.sin(Math.toRadians(this.pitch));
				this.pitchVelocity = this.pitchVelocity * -0.32f + (float) (-impactSpeed * pitchSin * 28.0);
				this.rollVelocity *= 0.65f;
				this.pitch = Mth.clamp(this.pitch, -12.0f, 12.0f);
				this.roll = Mth.clamp(this.roll, -12.0f, 12.0f);
			}

			this.position = position;
			this.velocity = velocity;
		}

		private Vec3 position() {
			return this.position;
		}

		private Vec3 velocity() {
			return this.velocity;
		}

		private float pitch() {
			return this.pitch;
		}

		private float roll() {
			return this.roll;
		}
	}
}
