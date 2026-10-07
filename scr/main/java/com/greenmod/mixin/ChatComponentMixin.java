package com.greenmod.mixin;

import com.greenmod.ChatLog;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

	/** Chat tools: add timestamps and mention marks to every line that is added to the chat. */
	@ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private Component kelp$chat(Component message) {
		return ChatLog.process(message);
	}
}
