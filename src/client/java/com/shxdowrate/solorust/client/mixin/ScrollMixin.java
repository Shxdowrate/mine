package com.shxdowrate.solorust.client.mixin;

import com.shxdowrate.solorust.client.ArtifactClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MouseHandler;

/** Пока в руке артефакт, колёсико листает ячейки способностей, а не хотбар. */
@Mixin(MouseHandler.class)
public abstract class ScrollMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void solorust$abilityScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (ArtifactClient.onScroll(handle, xoffset, yoffset)) {
			ci.cancel();
		}
	}
}
