package com.greenmod.mixin;

import com.greenmod.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

	/** Safe Walk: behave as if sneaking at an edge, so you never walk off. */
	@Inject(method = "isStayingOnGroundSurface", at = @At("RETURN"), cancellable = true, require = 0)
	private void kelp$safeWalk(CallbackInfoReturnable<Boolean> cir) {
		if (Modules.SAFE_WALK.isActive() && (Object) this == Minecraft.getInstance().player) cir.setReturnValue(true);
	}
}
