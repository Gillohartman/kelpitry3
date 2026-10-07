package com.greenmod.mixin;

import com.greenmod.AutoHome;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ChatQueueMixin {

	/** While Auto Set Home is running, your own chat messages wait until it is done (no interleaving). */
	@Inject(method = "sendChat", at = @At("HEAD"), cancellable = true, require = 0)
	private void kelp$holdChat(String message, CallbackInfo ci) {
		if (AutoHome.defer(message, false)) ci.cancel();
	}

	@Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true, require = 0)
	private void kelp$holdCommand(String command, CallbackInfo ci) {
		if (AutoHome.defer(command, true)) ci.cancel();
	}
}
