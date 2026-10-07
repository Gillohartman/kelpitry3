package com.greenmod.mixin;

import com.greenmod.Modules;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

	private static final String SCALE = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V";

	/** Hides the totem pop animation if the option says so. */
	@Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true, require = 0)
	private void kelp$hidePop(CallbackInfo ci) {
		if (Modules.TOTEM_POP.isActive() && Modules.TP_HIDE.value) ci.cancel();
	}

	/** Shrinks the totem pop animation by scaling the item that is drawn. */
	@ModifyArg(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = SCALE), index = 0, require = 0)
	private float kelp$popX(float v) { return Modules.TOTEM_POP.isActive() ? v * Modules.TP_SCALE.f() : v; }

	@ModifyArg(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = SCALE), index = 1, require = 0)
	private float kelp$popY(float v) { return Modules.TOTEM_POP.isActive() ? v * Modules.TP_SCALE.f() : v; }

	@ModifyArg(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = SCALE), index = 2, require = 0)
	private float kelp$popZ(float v) { return Modules.TOTEM_POP.isActive() ? v * Modules.TP_SCALE.f() : v; }
}
