package com.shxdowrate.solorust.suit;

import java.util.List;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.block.SuitStationBlock;
import com.shxdowrate.solorust.block.entity.SuitStationBlockEntity;
import com.shxdowrate.solorust.registry.ModSuits;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Станции, вызов костюма (V) и сворачивание/разворачивание (G). */
public final class SuitActions {
	/** Станция, к которой привязан игрок: с неё прилетает костюм по вызову. */
	public static final AttachmentType<GlobalPos> LINKED_STATION = AttachmentRegistry.<GlobalPos>create(SoloRust.id("linked_station"),
			builder -> builder.persistent(GlobalPos.CODEC).copyOnDeath());

	private SuitActions() {
	}

	public static void init() {
	}

	/** Индекс слота станции для типа брони: шлем, нагрудник, поножи, ботинки. */
	public static int stationSlot(ArmorType type) {
		return switch (type) {
			case HELMET -> 0;
			case CHESTPLATE -> 1;
			case LEGGINGS -> 2;
			default -> 3;
		};
	}

	// ================================================================== станция

	/** ПКМ по станции пустой рукой. */
	public static void useStation(ServerLevel level, BlockPos pos, ServerPlayer player, SuitStationBlockEntity station) {
		if (SuitAssembly.busy(player)) {
			return;
		}

		// Shift + ПКМ — снять детали со станции по одной.
		if (player.isShiftKeyDown()) {
			for (int i = 0; i < 4; i++) {
				ItemStack stack = station.get(i);

				if (!stack.isEmpty()) {
					station.set(i, ItemStack.EMPTY);

					if (!player.getInventory().add(stack)) {
						player.drop(stack, false);
					}

					SuitStationBlock.refresh(level, pos, station);
					return;
				}
			}

			return;
		}

		SuitTier worn = SuitTier.worn(player);

		if (worn != null) {
			// Снять костюм на пустую станцию.
			if (!station.isEmpty()) {
				player.sendOverlayMessage(Component.translatable("message.solorust.station.occupied").withStyle(ChatFormatting.YELLOW));
				return;
			}

			EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

			for (int i = 0; i < 4; i++) {
				station.set(i, player.getItemBySlot(slots[i]));
				player.setItemSlot(slots[i], ItemStack.EMPTY);
			}

			link(player, level, pos);
			SuitStationBlock.refresh(level, pos, station);
			Vec3 c = Vec3.atCenterOf(pos).add(0, 1, 0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 30, 0.4, 0.8, 0.4, 0.1);
			SuitAssembly.sound(level, player, SoundEvents.PISTON_CONTRACT, 0.8f, 1.2f);
			player.sendOverlayMessage(Component.translatable("message.solorust.station.stored").withStyle(ChatFormatting.AQUA));
			return;
		}

		if (!station.isFull()) {
			player.sendOverlayMessage(Component.translatable(station.isEmpty() ? "message.solorust.station.empty" : "message.solorust.station.incomplete")
					.withStyle(ChatFormatting.YELLOW));
			return;
		}

		if (player.position().distanceTo(Vec3.atBottomCenterOf(pos.above())) > 2.5) {
			player.sendOverlayMessage(Component.translatable("message.solorust.station.closer").withStyle(ChatFormatting.YELLOW));
			return;
		}

		List<ItemStack> suit = station.takeAll();
		SuitStationBlock.refresh(level, pos, station);
		link(player, level, pos);
		// Встаём ровно на платформу — манипуляторы работают по месту.
		player.teleportTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
		SuitAssembly.start(player, SuitAssembly.Style.STATION, suit, null);
	}

	private static void link(ServerPlayer player, ServerLevel level, BlockPos pos) {
		player.setAttached(LINKED_STATION, GlobalPos.of(level.dimension(), pos));
	}

	// ================================================================== вызов (V)

	public static void recall(ServerPlayer player) {
		if (SuitAssembly.busy(player)) {
			return;
		}

		if (SuitTier.worn(player) != null) {
			player.sendOverlayMessage(Component.translatable("message.solorust.suit.already_worn").withStyle(ChatFormatting.YELLOW));
			return;
		}

		GlobalPos link = player.getAttached(LINKED_STATION);

		if (link == null) {
			player.sendOverlayMessage(Component.translatable("message.solorust.station.no_link").withStyle(ChatFormatting.YELLOW));
			return;
		}

		if (!link.dimension().equals(player.level().dimension())) {
			player.sendOverlayMessage(Component.translatable("message.solorust.station.other_dimension").withStyle(ChatFormatting.RED));
			return;
		}

		ServerLevel level = player.level();

		if (!(level.getBlockEntity(link.pos()) instanceof SuitStationBlockEntity station)) {
			player.sendOverlayMessage(Component.translatable("message.solorust.station.missing").withStyle(ChatFormatting.RED));
			player.removeAttached(LINKED_STATION);
			return;
		}

		if (!station.isFull()) {
			player.sendOverlayMessage(Component.translatable("message.solorust.station.empty").withStyle(ChatFormatting.YELLOW));
			return;
		}

		List<ItemStack> suit = station.takeAll();
		SuitStationBlock.refresh(level, link.pos(), station);
		SuitAssembly.start(player, SuitAssembly.Style.RECALL, suit, Vec3.atCenterOf(link.pos()).add(0, 1, 0));
	}

	// ================================================================== свернуть / развернуть (G)

	public static void toggle(ServerPlayer player) {
		if (SuitAssembly.busy(player)) {
			return;
		}

		SuitTier worn = SuitTier.worn(player);

		if (worn != null) {
			SuitContainerItem.Kind kind = SuitContainerItem.Kind.forTier(worn);

			if (kind == null) {
				player.sendOverlayMessage(Component.translatable("message.solorust.suit.use_station").withStyle(ChatFormatting.YELLOW));
				return;
			}

			List<ItemStack> removed = SuitAssembly.strip(player, kind.style);
			int energy = SuitAssembly.energyOf(removed, worn);
			ItemStack container = SuitContainerItem.withEnergy(ModSuits.CONTAINERS.get(kind), energy);

			if (kind == SuitContainerItem.Kind.TITAN_BEACON) {
				// «Титан» улетает в небо, остаётся маяк.
				ServerLevel level = player.level();
				level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 0, 0, 1, 0, 1.0);
				level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 3, player.getZ(), 20, 0.2, 3, 0.2, 0.05);
			}

			if (!player.getInventory().add(container)) {
				player.drop(container, false);
			}

			return;
		}

		// Ищем, что развернуть: сначала нанокапсулу, потом кейс.
		Inventory inventory = player.getInventory();

		for (SuitContainerItem.Kind kind : new SuitContainerItem.Kind[] {SuitContainerItem.Kind.NANO_CAPSULE, SuitContainerItem.Kind.SUITCASE}) {
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				ItemStack stack = inventory.getItem(i);

				if (stack.getItem() instanceof SuitContainerItem item && item.kind() == kind) {
					item.deploy(player.level(), player, stack);
					return;
				}
			}
		}

		player.sendOverlayMessage(Component.translatable("message.solorust.suit.nothing_to_deploy").withStyle(ChatFormatting.GRAY));
	}
}
