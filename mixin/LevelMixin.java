package com.greenmod.mixin;

import com.greenmod.Modules;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

	/** Always Day: the client world reports daytime (visual only). */
	@Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true, require = 0)
	private void kelp$day(CallbackInfoReturnable<Long> cir) {
		if (Modules.ALWAYS_DAY.isActive() && (Object) this instanceof ClientLevel) cir.setReturnValue(6000L);
	}
}
