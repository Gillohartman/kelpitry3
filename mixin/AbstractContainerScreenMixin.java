package com.greenmod.mixin;

import com.greenmod.ContainerOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow protected int leftPos;
	@Shadow protected int topPos;
	@Shadow protected int imageWidth;
	@Shadow protected int imageHeight;
	@Shadow protected Slot hoveredSlot;
	@Shadow @Final protected AbstractContainerMenu menu;

	@Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
	private void kelp$overlay(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ContainerOverlay.render(g, mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight, menu, hoveredSlot, (Screen) (Object) this);
	}
}
