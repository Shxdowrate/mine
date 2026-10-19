package com.shxdowrate.solorust.client.mixin;

import com.shxdowrate.solorust.client.ArtifactClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;

/** Пока в руке артефакт, колёсико листает ячейки способностей, а не хотбар. */
@Mixin(MouseHandler.class)
public abstract class ScrollMixin {
	@Redirect(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private void solorust$abilityScroll(Inventory inventory, int slot) {
		if (!ArtifactClient.onScroll(inventory.getSelectedSlot(), slot)) {
			inventory.setSelectedSlot(slot);
		}
	}
}
