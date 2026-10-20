package com.shxdowrate.solorust.war;

import java.util.EnumSet;
import java.util.List;

import com.shxdowrate.solorust.registry.ModSounds;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Бандит из лагеря. Воюет группой: поднимает тревогу, держит дистанцию своей роли, стреляет очередями
 * с упреждением по движущейся цели, перезаряжается за укрытием, раненым отступает, без офицера паникует.
 */
public class Bandit extends Monster {
	private static final EntityDataAccessor<Integer> DATA_ROLE = SynchedEntityData.defineId(Bandit.class, EntityDataSerializers.INT);
	private static final DustParticleOptions LASER = new DustParticleOptions(0xFF1E1E, 0.5f);

	/** Лагерь (деревня), к которому приписан бандит. 0 — сам по себе. */
	private long campId;
	private boolean roleSet;

	/** Паника после гибели офицера: бандит не стреляет и бежит. */
	int panicTicks;
	/** Отступление раненого: один раз за жизнь, пока здоровье не восстановится. */
	boolean retreatUsed;
	int retreatTicks;

	public Bandit(EntityType<? extends Bandit> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 26.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.FOLLOW_RANGE, 56.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ROLE, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new CombatGoal(this));
		goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0f));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));

		targetSelector.addGoal(1, new HurtByTargetGoal(this, Bandit.class));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	// ================================================================== роль и лагерь

	public BanditRole role() {
		return BanditRole.byIndex(entityData.get(DATA_ROLE));
	}

	public void setRole(BanditRole role) {
		entityData.set(DATA_ROLE, role.ordinal());
		roleSet = true;
		applyRole(role);
	}

	private void applyRole(BanditRole role) {
		setAttribute(Attributes.MAX_HEALTH, role.health);
		setAttribute(Attributes.ARMOR, role.armor);
		setAttribute(Attributes.MOVEMENT_SPEED, role.speed);
		setAttribute(Attributes.KNOCKBACK_RESISTANCE, role == BanditRole.MACHINE_GUNNER ? 0.6 : 0.1);
		setHealth(getMaxHealth());

		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(role.weapon()));
		setDropChance(EquipmentSlot.MAINHAND, 0f);

		ItemStack head = switch (role) {
			case MACHINE_GUNNER -> new ItemStack(Items.IRON_HELMET);
			case OFFICER -> ItemStack.EMPTY;
			case SNIPER -> ItemStack.EMPTY;
			default -> getRandom().nextFloat() < 0.4f ? new ItemStack(Items.CHAINMAIL_HELMET) : ItemStack.EMPTY;
		};
		setItemSlot(EquipmentSlot.HEAD, head);
		setDropChance(EquipmentSlot.HEAD, 0f);

		if (role == BanditRole.MACHINE_GUNNER) {
			setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			setDropChance(EquipmentSlot.CHEST, 0f);
		}
	}

	private void setAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		var instance = getAttribute(attribute);

		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	public long campId() {
		return campId;
	}

	public void setCamp(long campId, BlockPos center, int radius) {
		this.campId = campId;
		setHomeTo(center, radius);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);

		if (!roleSet) {
			setRole(BanditRole.random(getRandom()));
		}

		setPersistenceRequired();
		return data;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Role", entityData.get(DATA_ROLE));
		output.putLong("Camp", campId);
		output.putBoolean("RetreatUsed", retreatUsed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_ROLE, input.getIntOr("Role", 0));
		roleSet = true;
		campId = input.getLongOr("Camp", 0L);
		retreatUsed = input.getBooleanOr("RetreatUsed", false);
	}

	// ================================================================== поведение вне боя

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);

		if (panicTicks > 0) {
			panicTicks--;
		}

		long time = level.getGameTime();

		// Офицер раздаёт приказы: союзники рядом быстрее и стойче.
		if (role() == BanditRole.OFFICER && getTarget() != null && time % 40 == 0) {
			for (Bandit ally : allies(level, 16)) {
				ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
				ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, true, false));
			}

			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.VINDICATOR_CELEBRATE, SoundSource.HOSTILE, 1.2f, 0.8f);
		}

		// Вне боя раны понемногу затягиваются.
		if (getTarget() == null && time % 40 == 0 && getHealth() < getMaxHealth()) {
			heal(1.0f);

			if (getHealth() >= getMaxHealth() * 0.9f) {
				retreatUsed = false;
			}
		}
	}

	List<Bandit> allies(ServerLevel level, double radius) {
		return level.getEntitiesOfClass(Bandit.class, getBoundingBox().inflate(radius), b -> b != this && b.isAlive());
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return other instanceof Bandit || super.considersEntityAsAlly(other);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		LivingEntity previous = getTarget();
		super.setTarget(target);

		// Заметил врага — поднимает тревогу на весь лагерь.
		if (target != null && previous == null && level() instanceof ServerLevel server) {
			BanditCamps.alarm(server, this, target);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);

		if (hurt && isAlive() && !retreatUsed && getHealth() < getMaxHealth() * 0.35f && role() != BanditRole.MACHINE_GUNNER) {
			retreatUsed = true;
			retreatTicks = 100;
		}

		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);

		if (level() instanceof ServerLevel server) {
			BanditCamps.onDeath(server, this, source);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PILLAGER_DEATH;
	}

	// ================================================================== бой

	/**
	 * Весь бой в одной цели: позиция по роли, укрытия, отступление, паника и стрельба.
	 * Занимает и движение, и взгляд, поэтому прогулки и «осмотреться» в бою не мешают.
	 */
	static final class CombatGoal extends Goal {
		private final Bandit bandit;
		/** Последние позиции цели — по ним оценивается её скорость. */
		private final Vec3[] history = new Vec3[8];
		private int historyIndex;
		private LivingEntity lastTarget;

		private int ammo;
		private int reloadTicks;
		private int burstLeft;
		private int shotCooldown;
		private int aimTicks;
		/** Сколько выстрелов подряд по одной цели — бандит «пристреливается». */
		private int zeroing;
		private int repathTicks;
		private int strafeTicks;
		private Vec3 moveGoal;
		private int noSightTicks;
		private int laserTicks;

		CombatGoal(Bandit bandit) {
			this.bandit = bandit;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = bandit.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			bandit.setAggressive(true);

			if (ammo <= 0 && reloadTicks <= 0) {
				ammo = bandit.role().magazine;
			}

			aimTicks = 0;
			zeroing = 0;
			repathTicks = 0;
		}

		@Override
		public void stop() {
			bandit.setAggressive(false);
			bandit.getNavigation().stop();
			moveGoal = null;
			lastTarget = null;
		}

		@Override
		public void tick() {
			LivingEntity target = bandit.getTarget();

			if (target == null || !(bandit.level() instanceof ServerLevel level)) {
				return;
			}

			BanditRole role = bandit.role();
			RandomSource random = bandit.getRandom();

			if (target != lastTarget) {
				lastTarget = target;
				java.util.Arrays.fill(history, null);
				zeroing = 0;
			}

			history[historyIndex] = target.position();
			historyIndex = (historyIndex + 1) % history.length;

			double dist = bandit.distanceTo(target);
			boolean canSee = bandit.getSensing().hasLineOfSight(target);
			noSightTicks = canSee ? 0 : noSightTicks + 1;

			if (shotCooldown > 0) {
				shotCooldown--;
			}

			if (reloadTicks > 0) {
				reloadTicks--;

				if (reloadTicks == 0) {
					ammo = role.magazine;
					level.playSound(null, bandit.getX(), bandit.getEyeY(), bandit.getZ(), ModSounds.RELOAD_END, SoundSource.HOSTILE, 0.6f, 1.0f);
				}
			}

			if (repathTicks > 0) {
				repathTicks--;
			}

			bandit.getLookControl().setLookAt(target, 40f, 40f);

			// ---------------------------------------------------- куда идти
			if (bandit.panicTicks > 0) {
				// Паника: бежим от цели, не стреляя.
				flee(target, 1.25);
				return;
			}

			if (bandit.retreatTicks > 0) {
				bandit.retreatTicks--;
				Vec3 cover = repathTicks == 0 ? BanditCover.find(level, bandit, target, 12) : null;

				if (cover != null) {
					move(cover, 1.2);
				} else if (repathTicks == 0) {
					flee(target, 1.2);
					repathTicks = 20;
				}

				// Отходя, огрызается редкими выстрелами.
				if (canSee && random.nextInt(4) == 0) {
					tryFire(level, target, role, dist, random);
				}

				return;
			}

			if (reloadTicks > 0) {
				// Перезарядка — за укрытием.
				if (repathTicks == 0) {
					Vec3 cover = BanditCover.find(level, bandit, target, 10);

					if (cover != null) {
						move(cover, 1.15);
					}

					repathTicks = 20;
				}

				return;
			}

			if (!canSee) {
				// Потерял из виду — обходим к последней точке (с небольшим смещением, чтобы заходить с фланга).
				if (repathTicks == 0) {
					Vec3 flank = target.position().add((random.nextDouble() - 0.5) * 8, 0, (random.nextDouble() - 0.5) * 8);
					move(flank, 1.0);
				}
			} else if (dist > role.maxRange) {
				if (repathTicks == 0) {
					move(target.position(), 1.0);
				}
			} else if (dist < role.minRange && role != BanditRole.SHOTGUNNER) {
				if (repathTicks == 0) {
					flee(target, 1.1);
				}
			} else if (role == BanditRole.SHOTGUNNER && dist > 4) {
				// Дробовик лезет вплотную.
				if (repathTicks == 0) {
					move(target.position(), 1.2);
				}
			} else if (role == BanditRole.MACHINE_GUNNER || role == BanditRole.SNIPER) {
				// Пулемётчик и снайпер стоят и стреляют.
				bandit.getNavigation().stop();
			} else if (--strafeTicks <= 0) {
				// Остальные перебегают боком, чтобы по ним было труднее попасть.
				strafeTicks = 30 + random.nextInt(50);
				Vec3 to = target.position().subtract(bandit.position());
				Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale((random.nextBoolean() ? 1 : -1) * (3 + random.nextInt(4)));
				move(bandit.position().add(side), 1.05);
			}

			// ---------------------------------------------------- стрельба
			if (canSee && dist <= role.maxRange + 16) {
				aimTicks++;
				tryFire(level, target, role, dist, random);
			} else {
				aimTicks = 0;
				laserTicks = 0;
			}
		}

		private void tryFire(ServerLevel level, LivingEntity target, BanditRole role, double dist, RandomSource random) {
			if (ammo <= 0) {
				if (reloadTicks <= 0) {
					reloadTicks = role.reload;
					repathTicks = 0;
					level.playSound(null, bandit.getX(), bandit.getEyeY(), bandit.getZ(), ModSounds.RELOAD, SoundSource.HOSTILE, 0.7f, 1.0f);
				}

				return;
			}

			// Снайпер сначала «светит» лазером — у цели есть время уйти.
			if (role == BanditRole.SNIPER) {
				if (shotCooldown > 0) {
					return;
				}

				laserTicks++;
				Vec3 eye = bandit.getEyePosition();
				Vec3 at = target.getBoundingBox().getCenter();

				if (laserTicks % 2 == 0) {
					Vec3 d = at.subtract(eye);
					int steps = Mth.clamp((int) (d.length() / 1.5), 2, 60);

					for (int i = 1; i < steps; i++) {
						Vec3 p = eye.add(d.scale(i / (double) steps));
						level.sendParticles(LASER, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
					}
				}

				if (laserTicks < 25) {
					return;
				}

				laserTicks = 0;
			} else if (aimTicks < 8) {
				return;
			}

			if (shotCooldown > 0) {
				return;
			}

			if (burstLeft <= 0) {
				burstLeft = role.burstMin + random.nextInt(role.burstMax - role.burstMin + 1);
			}

			if (role == BanditRole.GRENADIER) {
				BanditRockets.fire(level, bandit, target, predictedVelocity());
			} else if (!shoot(level, target, role, dist, random)) {
				// Свой на линии огня — не стреляем, смещаемся.
				strafeTicks = 0;
				shotCooldown = 10;
				return;
			}

			ammo--;
			burstLeft--;
			zeroing++;
			shotCooldown = burstLeft > 0 ? role.interval : role.pause + random.nextInt(role.pause / 2 + 1);
		}

		/** Скорость цели (блоки/тик), усреднённая по истории позиций. */
		private Vec3 predictedVelocity() {
			Vec3 newest = history[Math.floorMod(historyIndex - 1, history.length)];
			Vec3 oldest = null;
			int span = 0;

			for (int i = history.length - 1; i >= 1; i--) {
				Vec3 p = history[Math.floorMod(historyIndex - 1 - i, history.length)];

				if (p != null) {
					oldest = p;
					span = i;
					break;
				}
			}

			if (newest == null || oldest == null || span == 0) {
				return Vec3.ZERO;
			}

			return newest.subtract(oldest).scale(1.0 / span);
		}

		/** Выстрел (пули — мгновенные лучи). false — на линии огня свой. */
		private boolean shoot(ServerLevel level, LivingEntity target, BanditRole role, double dist, RandomSource random) {
			Vec3 eye = bandit.getEyePosition();
			Vec3 velocity = predictedVelocity();
			double speed = velocity.length();

			// Реакция бандита запаздывает на ~4 тика: целится туда, где цель была, плюс упреждение по её скорости.
			// Хороший стрелок (lead ≈ 1) компенсирует запаздывание, плохой — бьёт позади быстрой цели.
			Vec3 lagged = history[Math.floorMod(historyIndex - 5, history.length)];
			Vec3 base = (lagged != null ? lagged : target.position()).add(0, target.getBbHeight() * 0.6, 0);
			Vec3 aim = base.add(velocity.scale(4 * role.lead + dist / 40.0 * role.lead));

			// Свой на линии огня?
			for (Bandit ally : level.getEntitiesOfClass(Bandit.class, new AABB(eye, aim).inflate(1.0), b -> b != bandit)) {
				if (ally.getBoundingBox().inflate(0.3).clip(eye, aim).isPresent()) {
					return false;
				}
			}

			// Разброс: растёт со скоростью цели, на бегу и в панике; падает, пока бандит пристреливается.
			float spread = role.spread;
			spread *= (float) (1.0 + Math.min(3.0, speed * 4.0));

			if (bandit.getDeltaMovement().horizontalDistanceSqr() > 0.003) {
				spread *= 1.4f;
			}

			spread *= Mth.clamp(1.0f - zeroing * 0.06f, 0.6f, 1.0f);

			if (target.hasEffect(MobEffects.INVISIBILITY)) {
				spread *= 3.0f;
			}

			if (bandit.hasEffect(MobEffects.BLINDNESS)) {
				spread *= 4.0f;
			}

			Vec3 look = aim.subtract(eye).normalize();
			double range = role.maxRange + 40;
			boolean hit = false;

			for (int p = 0; p < role.pellets; p++) {
				Vec3 dir = spread(look, spread, random);
				Vec3 end = eye.add(dir.scale(range));
				BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bandit));
				Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();

				LivingEntity victim = null;
				Vec3 point = null;
				double best = eye.distanceToSqr(limit);

				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, limit).inflate(1.0),
						e -> e != bandit && e.isAlive() && !e.isSpectator() && !(e instanceof Bandit))) {
					var clip = e.getBoundingBox().inflate(0.1).clip(eye, limit);

					if (clip.isPresent()) {
						double d = eye.distanceToSqr(clip.get());

						if (d < best) {
							best = d;
							victim = e;
							point = clip.get();
						}
					}
				}

				Vec3 impact = point != null ? point : limit;

				if (p == 0 || p % 3 == 0) {
					tracer(level, eye.add(look.scale(0.8)), impact);
				}

				if (victim != null) {
					victim.invulnerableTime = 0;
					victim.hurtServer(level, level.damageSources().mobAttack(bandit), role.damage);
					hit = true;
				} else if (block.getType() == HitResult.Type.BLOCK) {
					level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 1, 0.02, 0.02, 0.02, 0.01);
				}
			}

			Vec3 muzzle = eye.add(look.scale(1.0)).add(0, -0.25, 0);
			level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0.02, 0.02, 0.02, 0.0);
			level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.03, 0.03, 0.03, 0.01);
			level.playSound(null, bandit.getX(), bandit.getEyeY(), bandit.getZ(), role.shotSound(), SoundSource.HOSTILE,
					role == BanditRole.SNIPER ? 4.0f : 2.2f, 0.9f + random.nextFloat() * 0.15f);

			if (hit && target instanceof Player) {
				zeroing++;
			}

			return true;
		}

		private static Vec3 spread(Vec3 look, float degrees, RandomSource random) {
			double rad = Math.toRadians(degrees);
			return look.yRot((float) (random.nextGaussian() * rad * 0.5)).xRot((float) (random.nextGaussian() * rad * 0.5)).normalize();
		}

		private static void tracer(ServerLevel level, Vec3 from, Vec3 to) {
			Vec3 delta = to.subtract(from);
			int steps = Mth.clamp((int) (delta.length() / 3.0), 1, 30);

			for (int i = 1; i <= steps; i++) {
				Vec3 point = from.add(delta.scale(i / (double) steps));
				level.sendParticles(ParticleTypes.CRIT, true, false, point.x, point.y, point.z, 1, 0, 0, 0, 0);
			}
		}

		private void move(Vec3 to, double speed) {
			if (moveGoal != null && moveGoal.distanceToSqr(to) < 2.0 && !bandit.getNavigation().isDone()) {
				return;
			}

			moveGoal = to;
			bandit.getNavigation().moveTo(to.x, to.y, to.z, speed);
			repathTicks = 10;
		}

		private void flee(LivingEntity from, double speed) {
			if (repathTicks > 0 && !bandit.getNavigation().isDone()) {
				return;
			}

			Vec3 away = bandit.position().subtract(from.position());
			away = new Vec3(away.x, 0, away.z);
			away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
			Vec3 to = bandit.position().add(away.scale(10)).add((bandit.getRandom().nextDouble() - 0.5) * 6, 0, (bandit.getRandom().nextDouble() - 0.5) * 6);
			moveGoal = null;
			move(to, speed);
		}
	}
}
