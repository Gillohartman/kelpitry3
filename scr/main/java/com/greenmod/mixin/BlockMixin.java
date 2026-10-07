package com.greenmod.mixin;

import com.greenmod.Xray;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {

	/** Xray: only whitelisted blocks draw their faces. */
	@Inject(method = "shouldRenderFace", at = @At("RETURN"), cancellable = true, require = 0)
	private static void kelp$xray(BlockState state, BlockState neighborState, Direction face, CallbackInfoReturnable<Boolean> cir) {
		if (Xray.active()) cir.setReturnValue(Xray.isVisible(state.getBlock()));
	}
}
