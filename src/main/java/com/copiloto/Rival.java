package com.copiloto;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** Sigue al rival (al que miras o golpeas) y avisa por voz cuando le queda poca vida. */
public final class Rival {
    private static final int[] STAGES = {50, 30, 15};
    private static final Set<String> KNOWN = Set.of(
            "zombie", "skeleton", "creeper", "spider", "cave_spider", "enderman", "witch", "slime",
            "drowned", "husk", "stray", "pillager", "vindicator", "evoker", "ravager", "phantom",
            "blaze", "ghast", "magma_cube", "wither_skeleton", "piglin", "piglin_brute",
            "zombified_piglin", "hoglin", "zoglin", "guardian", "elder_guardian", "shulker",
            "silverfish", "endermite", "vex", "warden", "wither", "ender_dragon", "zombie_villager",
            "bogged", "breeze", "creaking", "player");

    public static LivingEntity target;
    private static long seen = 0;
    private static int stage = 100;
    private static boolean engaged = false; // lo golpeaste (o le hiciste dano)

    private Rival() {}

    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide && entity instanceof LivingEntity le && !(le instanceof ArmorStand)) {
                set(le);
                engaged = true;
            }
            return InteractionResult.PASS;
        });
    }

    private static void set(LivingEntity le) {
        if (le != target) {
            target = le;
            stage = 100;
            engaged = false;
        }
        seen = System.currentTimeMillis();
    }

    public static void reset() {
        target = null;
        stage = 100;
        engaged = false;
    }

    private static boolean announcable(LivingEntity e) {
        return e instanceof Enemy || e instanceof Player;
    }

    private static String lowKey(LivingEntity e) {
        String k = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        return KNOWN.contains(k) ? "low_" + k : "low_enemy";
    }

    /** Entidad bajo la mira: primero la del juego, y si no, un rayo largo (hasta 40 bloques). */
    private static LivingEntity pick(Minecraft mc) {
        LocalPlayer p = mc.player;
        Entity ch = mc.crosshairPickEntity;
        if (ch instanceof LivingEntity le && le != p && le.isAlive() && !(le instanceof ArmorStand)) return le;

        double range = 40.0;
        Vec3 start = p.getEyePosition();
        Vec3 view = p.getViewVector(1.0F);
        Vec3 end = start.add(view.scale(range));
        AABB box = p.getBoundingBox().expandTowards(view.scale(range)).inflate(1.0);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p, start, end, box,
                e -> e != p && !e.isSpectator() && e.isPickable() && e.isAlive()
                        && e instanceof LivingEntity && !(e instanceof ArmorStand),
                range * range);
        if (r != null && r.getEntity() instanceof LivingEntity le && p.hasLineOfSight(le)) return le;
        return null;
    }

    public static void tick(Minecraft mc) {
        LivingEntity look = pick(mc);
        if (look != null) {
            set(look);
            if (look.hurtTime > 0) engaged = true; // tambien cuenta si le pegas con arco, etc.
        }
        if (target == null) return;

        long now = System.currentTimeMillis();
        CopilotoConfig c = CopilotoConfig.INSTANCE;

        if (target.isDeadOrDying() || target.isRemoved()) {
            if (c.rivalAlerts && engaged && announcable(target) && now - seen < 10000) Voice.urgent("rival_dead");
            reset();
            return;
        }
        if (now - seen > 6000) {
            reset();
            return;
        }
        if (!c.rivalAlerts || !engaged || !announcable(target)) return;

        float max = target.getMaxHealth();
        if (max <= 0) return;
        float frac = target.getHealth() / max * 100f;

        int newStage = 100;
        for (int t : STAGES) if (frac < t) newStage = t;
        if (newStage < stage) {
            boolean first = stage == 100;
            stage = newStage;
            if (first) {
                Voice.urgent(lowKey(target)); // "<nombre> a baja vida"
            } else {
                int r = Math.max(5, Math.min(45, Math.round(frac / 5f) * 5));
                Voice.urgent("rival_" + r);
            }
        } else if (frac >= 50f && stage < 100) {
            stage = 100;
        }
    }
}
