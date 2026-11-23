package com.copiloto.mixin;

import com.copiloto.Highlight;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void copiloto$red(CallbackInfoReturnable<Integer> cir) {
        if (Highlight.should((Entity) (Object) this)) cir.setReturnValue(com.copiloto.CopilotoConfig.INSTANCE.highlightColor);
    }
}
