package com.greenmod.mixin;

import com.greenmod.Modules;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

	/** Hides vanilla nametags whenever the Ruined Client nametag overlay is handling that entity. */
	@Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
	private void kelp$hideVanillaName(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
		if (entity instanceof Player) {
			if (Modules.PLAYER_TAGS.isActive()) state.nameTag = null;
		} else if (entity.hasCustomName() && Modules.MOB_TAGS.isActive()) {
			state.nameTag = null;
		}
	}
}
