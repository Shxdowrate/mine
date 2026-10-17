package com.shxdowrate.solorust.client.mixin;

import com.shxdowrate.solorust.client.SoloRustClient;
import com.shxdowrate.solorust.gun.GunStats;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

/** Приближение камеры при прицеливании из оружия. */
@Mixin(AbstractClientPlayer.class)
public abstract class FovMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void solorust$aimZoom(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
		if ((Object) this instanceof LocalPlayer player && firstPerson && GunStats.isAiming(player)) {
			cir.setReturnValue(SoloRustClient.aimZoom(player));
		}
	}
}
