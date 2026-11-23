package com.copiloto;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Sigue al rival (al que miras o golpeas) y avisa por voz su vida cuando baja de la mitad. */
public final class Rival {
    private static final int[] STAGES = {50, 30, 15};

    public static LivingEntity target;
    private static long seen = 0;
    private static int stage = 100;

    private Rival() {}

    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide && entity instanceof LivingEntity le) set(le);
            return InteractionResult.PASS;
        });
    }

    private static void set(LivingEntity le) {
        if (le instanceof ArmorStand) return;
        if (le != target) {
            target = le;
            stage = 100;
        }
        seen = System.currentTimeMillis();
    }

    public static void reset() {
        target = null;
        stage = 100;
    }

    private static boolean announcable(LivingEntity e) {
        return e instanceof Enemy || e instanceof Player;
    }

    public static void tick(Minecraft mc) {
        Entity look = mc.crosshairPickEntity;
        if (look instanceof LivingEntity le && le != mc.player && le.isAlive()) set(le);
        if (target == null) return;

        long now = System.currentTimeMillis();
        CopilotoConfig c = CopilotoConfig.INSTANCE;

        if (target.isDeadOrDying() || target.isRemoved()) {
            if (c.rivalAlerts && announcable(target) && now - seen < 10000) Voice.urgent("rival_dead");
            reset();
            return;
        }
        if (now - seen > 6000) {
            reset();
            return;
        }
        if (!c.rivalAlerts || !announcable(target)) return;

        float max = target.getMaxHealth();
        if (max <= 0) return;
        float frac = target.getHealth() / max * 100f;

        int newStage = 100;
        for (int t : STAGES) if (frac < t) newStage = t;
        if (newStage < stage) {
            stage = newStage;
            int r = Math.max(5, Math.min(45, Math.round(frac / 5f) * 5));
            Voice.urgent("rival_" + r);
        } else if (frac >= 50f && stage < 100) {
            stage = 100;
        }
    }
}
