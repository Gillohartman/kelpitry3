package com.greenmod.mixin;

import com.greenmod.Modules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

	private static void zero(Object o, String name) {
		for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				f.setFloat(o, 0f);
				return;
			} catch (Throwable ignored) {
			}
		}
	}

	/** Flat Items: no spinning and no bobbing. */
	@Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
	private void kelp$noSpin(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
		if (Modules.FLAT_ITEMS.isActive() && Modules.FI_NOSPIN.value) {
			zero(state, "ageInTicks");
			zero(state, "bobOffset");
		}
	}

	/** Flat Items: lay the item down on the ground. */
	@ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private PoseStack kelp$layFlat(PoseStack stack) {
		if (Modules.FLAT_ITEMS.isActive() && Modules.FI_FLAT.value) {
			stack.mulPose(Axis.XP.rotationDegrees(90f));
		}
		return stack;
	}
}
