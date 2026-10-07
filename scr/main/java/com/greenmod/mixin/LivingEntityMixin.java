package com.greenmod.mixin;

import com.greenmod.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	/** Slow Swing: longer swing animation for your own hand (visual only). */
	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
	private void kelp$slowSwing(CallbackInfoReturnable<Integer> cir) {
		if (Modules.SLOW_SWING.isActive() && (Object) this == Minecraft.getInstance().player) {
			int base = cir.getReturnValue();
			cir.setReturnValue(Math.max(1, (int) Math.round(base * Modules.SWING_MULT.value)));
		}
	}
}
