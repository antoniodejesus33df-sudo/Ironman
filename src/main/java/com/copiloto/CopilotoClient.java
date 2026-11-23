package com.copiloto;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public class CopilotoClient implements ClientModInitializer {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] NAMES = {"helmet", "chest", "legs", "boots"};
    private static final int[] STAGES = {50, 25, 10};

    private final Deque<String> queue = new ArrayDeque<>();
    private final int[] armorStage = {100, 100, 100, 100};
    private int cooldown = 0, tick = 0, lastPots = -1;
    private boolean lowHp = false;

    @Override
    public void onInitializeClient() {
        CopilotoConfig.load();

        KeyMapping status = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.copiloto.status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.copiloto"));
        KeyMapping toggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.copiloto.highlight", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.copiloto"));

        KeyMapping menu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.copiloto.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.copiloto"));

        // Boton en el menu de pausa (util en celular)
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (screen instanceof PauseScreen) {
                Screens.getButtons(screen).add(Button.builder(Component.literal("Copiloto"),
                        b -> mc.setScreen(new CopilotoScreen(screen))).bounds(6, 6, 90, 20).build());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menu.consumeClick()) mc.setScreen(new CopilotoScreen(mc.screen));
            while (status.consumeClick()) sayStatus(mc);
            while (toggle.consumeClick()) {
                CopilotoConfig.INSTANCE.highlightHostile = !CopilotoConfig.INSTANCE.highlightHostile;
                CopilotoConfig.save();
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("Marcar monstruos: "
                            + (CopilotoConfig.INSTANCE.highlightHostile ? "ON" : "OFF")), true);
                }
            }
            tick(mc);
        });
    }

    private void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) {
            lastPots = -1;
            queue.clear();
            for (int i = 0; i < 4; i++) armorStage[i] = 100;
            lowHp = false;
            return;
        }
        tick++;
        if (cooldown > 0) {
            cooldown--;
        } else if (!queue.isEmpty()) {
            play(mc, queue.poll());
            cooldown = 50;
        }
        if (tick % 10 != 0) return;

        int pots = countHealingPots(p);
        if (lastPots >= 0 && pots < lastPots && pots <= CopilotoConfig.INSTANCE.potAlertMax) {
            queue.add("pots_" + Math.min(pots, 36));
        }
        lastPots = pots;

        if (CopilotoConfig.INSTANCE.armorAlerts) {
            for (int i = 0; i < 4; i++) {
                ItemStack s = p.getItemBySlot(SLOTS[i]);
                if (s.isEmpty() || !s.isDamageableItem()) {
                    armorStage[i] = 100;
                    continue;
                }
                int pct = pct(s);
                int target = 100;
                for (int t : STAGES) if (pct <= t) target = t;
                if (target < armorStage[i]) {
                    queue.add("armor_" + NAMES[i] + "_" + target);
                    armorStage[i] = target;
                } else if (pct > 50) {
                    armorStage[i] = 100;
                }
            }
        }

        if (CopilotoConfig.INSTANCE.lowHealthAlert) {
            if (p.getHealth() <= 6f && !lowHp) {
                queue.add("hp_low");
                lowHp = true;
            } else if (p.getHealth() > 10f) {
                lowHp = false;
            }
        }
    }

    private void sayStatus(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        queue.add("pots_" + Math.min(countHealingPots(p), 36));
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getItemBySlot(SLOTS[i]);
            if (s.isEmpty() || !s.isDamageableItem()) continue;
            int pct = pct(s);
            for (int j = STAGES.length - 1; j >= 0; j--) {
                if (pct <= STAGES[j]) {
                    queue.add("armor_" + NAMES[i] + "_" + STAGES[j]);
                    break;
                }
            }
        }
    }

    private static int pct(ItemStack s) {
        int max = s.getMaxDamage();
        return max <= 0 ? 100 : (max - s.getDamageValue()) * 100 / max;
    }

    private static int countHealingPots(LocalPlayer p) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.SPLASH_POTION) || s.is(Items.POTION)) {
                PotionContents pc = s.get(DataComponents.POTION_CONTENTS);
                if (pc != null && (pc.is(Potions.HEALING) || pc.is(Potions.STRONG_HEALING))) n += s.getCount();
            }
        }
        return n;
    }

    private static void play(Minecraft mc, String key) {
        SoundEvent ev = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("copiloto", key));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, 1.0F));
    }
}
