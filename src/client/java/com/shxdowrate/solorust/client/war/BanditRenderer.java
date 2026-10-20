package com.shxdowrate.solorust.client.war;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.war.Bandit;
import com.shxdowrate.solorust.war.BanditRole;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/** Бандит — человек в форме своей роли; с оружием в руках, а в бою вскидывает его к плечу. */
public class BanditRenderer extends HumanoidMobRenderer<Bandit, BanditRenderState, HumanoidModel<BanditRenderState>> {
	private static final Identifier[] TEXTURES = new Identifier[BanditRole.values().length];

	static {
		for (BanditRole role : BanditRole.values()) {
			TEXTURES[role.ordinal()] = SoloRust.id("textures/entity/bandit/" + role.id + ".png");
		}
	}

	public BanditRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
		addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), part -> new HumanoidModel<BanditRenderState>(part)),
				context.getEquipmentRenderer()));
	}

	@Override
	public BanditRenderState createRenderState() {
		return new BanditRenderState();
	}

	@Override
	public void extractRenderState(Bandit entity, BanditRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.role = entity.role().ordinal();
	}

	@Override
	public Identifier getTextureLocation(BanditRenderState state) {
		return TEXTURES[Math.floorMod(state.role, TEXTURES.length)];
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(Bandit mob, HumanoidArm arm) {
		if (mob.getMainArm() == arm && !mob.getMainHandItem().isEmpty()) {
			return mob.isAggressive() ? HumanoidModel.ArmPose.CROSSBOW_HOLD : HumanoidModel.ArmPose.ITEM;
		}

		return super.getArmPose(mob, arm);
	}
}
