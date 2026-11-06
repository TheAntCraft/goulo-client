package dev.nova.client.mixin;

import dev.nova.client.NovaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-Name oben links im Hauptmenü. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void novaclient$branding(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(textRenderer, NovaClient.NAME, 4, 4, 0xFF55AAFF);
        context.drawTextWithShadow(textRenderer, "v" + NovaClient.getVersion(), 4, 14, 0xFFAAAAAA);
    }
}
