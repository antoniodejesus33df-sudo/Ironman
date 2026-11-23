package com.copiloto;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.lwjgl.glfw.GLFW;

public class CopilotoClient implements ClientModInitializer {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] NAMES = {"helmet", "chest", "legs", "boots"};
    private static final int[] STAGES = {50, 25, 10};

    /** Pociones de curacion en el inventario (la usa el HUD). */
    public static int pots = 0;

    private final int[] armorStage = {100, 100, 100, 100};
    private int tick = 0, lastPots = -1;
    private boolean lowHp = false, greeted = false;

    @Override
    public void onInitializeClient() {
        CopilotoConfig.load();

        KeyMapping status = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.copiloto.status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.copiloto"));
        KeyMapping toggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.copiloto.highlight", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.copiloto"));
        KeyMapping hud = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.copiloto.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.copiloto"));

        KeyMapping menu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.copiloto.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.copiloto"));

        Rival.register();
        HudRenderCallback.EVENT.register((g, delta) -> IronHud.render(g));
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof PauseScreen) {
                Screens.getButtons(screen).add(Button.builder(Component.literal("Copiloto"),
                        b -> client.setScreen(new CopilotoScreen(screen))).bounds(6, 6, 90, 20).build());
            }
            if (screen instanceof TitleScreen || screen instanceof PauseScreen || client.level == null) {
                ScreenEvents.afterRender(screen).register((s, g, mx, my, dt) -> {
                    if (CopilotoConfig.INSTANCE.menuOverlay) IronHud.menuOverlay(g, s.width, s.height);
                });
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
            while (hud.consumeClick()) {
                CopilotoConfig.INSTANCE.ironHud = !CopilotoConfig.INSTANCE.ironHud;
                CopilotoConfig.save();
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("HUD Iron Man: "
                            + (CopilotoConfig.INSTANCE.ironHud ? "ON" : "OFF")), true);
                }
            }
            tick(mc);
        });
    }

    private void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) {
            lastPots = -1;
            pots = 0;
            Voice.clear();
            Rival.reset();
            for (int i = 0; i < 4; i++) armorStage[i] = 100;
            lowHp = false;
            greeted = false;
            return;
        }
        if (!greeted) {
            Voice.say("hello");
            greeted = true;
        }
        tick++;
        Voice.tick(mc);
        Rival.tick(mc);
        if (tick % 10 != 0) return;

        int count = countHealingPots(p);
        pots = count;
        if (lastPots >= 0 && count < lastPots && count <= CopilotoConfig.INSTANCE.potAlertMax) {
            Voice.say("pots_" + Math.min(count, 36));
        }
        lastPots = count;

        if (CopilotoConfig.INSTANCE.armorAlerts) {
            for (int i = 0; i < 4; i++) {
                ItemStack s = p.getItemBySlot(SLOTS[i]);
                if (s.isEmpty() || !s.isDamageableItem()) {
                    armorStage[i] = 100;
                    continue;
                }
                int pct = IronHud.armorPct(s);
                int target = 100;
                for (int t : STAGES) if (pct <= t) target = t;
                if (target < armorStage[i]) {
                    Voice.say("armor_" + NAMES[i] + "_" + target);
                    armorStage[i] = target;
                } else if (pct > 50) {
                    armorStage[i] = 100;
                }
            }
        }

        if (CopilotoConfig.INSTANCE.lowHealthAlert) {
            if (p.getHealth() <= 6f && !lowHp) {
                Voice.urgent("hp_low");
                lowHp = true;
            } else if (p.getHealth() > 10f) {
                lowHp = false;
            }
        }
    }

    private void sayStatus(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        Voice.say("pots_" + Math.min(countHealingPots(p), 36));
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getItemBySlot(SLOTS[i]);
            if (s.isEmpty() || !s.isDamageableItem()) continue;
            int pct = IronHud.armorPct(s);
            for (int j = STAGES.length - 1; j >= 0; j--) {
                if (pct <= STAGES[j]) {
                    Voice.say("armor_" + NAMES[i] + "_" + STAGES[j]);
                    break;
                }
            }
        }
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
}
