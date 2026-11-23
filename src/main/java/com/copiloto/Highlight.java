package com.copiloto;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/** Solo monstruos hostiles (nunca jugadores) y solo mientras tengas linea de vision. */
public final class Highlight {
    private Highlight() {}

    public static boolean should(Entity e) {
        if (!CopilotoConfig.INSTANCE.highlightHostile) return false;
        if (!(e instanceof Enemy) || !(e instanceof LivingEntity living) || !living.isAlive()) return false;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || e.distanceToSqr(p) > 32 * 32) return false;
        return p.hasLineOfSight(e);
    }
}
