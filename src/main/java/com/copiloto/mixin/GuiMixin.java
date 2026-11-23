package com.copiloto.mixin;

import com.copiloto.CopilotoConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** require = 0: si un nombre no existe en esta version, esa funcion se omite sin cerrar el juego. */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void copiloto$sbHead(GuiGraphics g, Objective objective, CallbackInfo ci) {
        CopilotoConfig c = CopilotoConfig.INSTANCE;
        if (!c.scoreboardVisible) {
            ci.cancel();
            return;
        }
        g.pose().pushPose();
        g.pose().translate(c.scoreboardX, c.scoreboardY, 0f);
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"), require = 0)
    private void copiloto$sbTail(GuiGraphics g, Objective objective, CallbackInfo ci) {
        if (CopilotoConfig.INSTANCE.scoreboardVisible) g.pose().popPose();
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), require = 0)
    private void copiloto$hotbarHead(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
        CopilotoConfig c = CopilotoConfig.INSTANCE;
        g.pose().pushPose();
        g.pose().translate(c.hotbarX, c.hotbarY, 0f);
    }

    @Inject(method = "renderItemHotbar", at = @At("RETURN"), require = 0)
    private void copiloto$hotbarTail(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
        g.pose().popPose();
    }

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true, require = 0)
    private void copiloto$itemName(GuiGraphics g, CallbackInfo ci) {
        if (!CopilotoConfig.INSTANCE.showItemName) ci.cancel();
    }
}
