package com.shxdowrate.solorust.client.mixin;

import com.shxdowrate.solorust.client.helmet.HelmetHud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;

/** Шлем силовой брони обводит мобов сквозь стены (как эффект свечения, но только у тебя на экране). */
@Mixin(EntityRenderer.class)
public abstract class EntityOutlineMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void solorust$helmetOutline(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
		if (state.outlineColor == 0) {
			int color = HelmetHud.outlineColor(entity);

			if (color != 0) {
				state.outlineColor = ARGB.opaque(color);
			}
		}
	}
}
