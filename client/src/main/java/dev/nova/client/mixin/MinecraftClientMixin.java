package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fenstertitel: "Nova Client 0.1.0 | Minecraft 1.21.4" */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void novaclient$windowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(NovaClient.NAME + " " + NovaClient.getVersion() + " | " + cir.getReturnValue());
    }
}
