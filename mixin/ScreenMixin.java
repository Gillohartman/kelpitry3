package com.greenmod.mixin;

import com.greenmod.AutoReconnect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void kelp$reconnectText(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		AutoReconnect.drawCountdown(g, (Screen) (Object) this);
	}
}
