package com.copiloto.mixin;

import com.copiloto.CopilotoConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
    private void copiloto$hand(AbstractClientPlayer player, float partialTicks, float pitch,
                               InteractionHand hand, float swingProgress, ItemStack stack,
                               float equipProgress, PoseStack poseStack,
                               MultiBufferSource buffer, int light, CallbackInfo ci) {
        CopilotoConfig c = CopilotoConfig.INSTANCE;
        float side = hand == InteractionHand.MAIN_HAND ? 1f : -1f;
        poseStack.translate(c.itemX * side, c.itemY, c.itemZ);
        if (c.itemScale != 1f) poseStack.scale(c.itemScale, c.itemScale, c.itemScale);
    }
}
