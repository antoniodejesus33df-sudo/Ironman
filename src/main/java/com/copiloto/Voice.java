package com.copiloto;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/** Cola de voces del asistente. */
public final class Voice {
    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static int cooldown = 0;

    private Voice() {}

    public static void say(String key) {
        if (!QUEUE.contains(key)) QUEUE.addLast(key);
    }

    /** Pasa al frente de la cola (combate). */
    public static void urgent(String key) {
        QUEUE.remove(key);
        QUEUE.addFirst(key);
        if (cooldown > 40) cooldown = 40;
    }

    public static void clear() {
        QUEUE.clear();
        cooldown = 0;
    }

    public static void tick(Minecraft mc) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        String key = QUEUE.poll();
        if (key != null) {
            play(mc, key);
            cooldown = key.equals("hello") ? 130 : (key.startsWith("hp_") ? 80 : 62);
        }
    }

    private static void play(Minecraft mc, String key) {
        SoundEvent ev = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("copiloto", key));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, 1.0F, 1.0F));
    }
}
