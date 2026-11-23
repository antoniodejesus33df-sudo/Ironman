package com.copiloto;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Menu principal estilo Minecraft con submenus. */
public class CopilotoScreen extends Screen {
    private final Screen parent;

    public CopilotoScreen(Screen parent) {
        super(Component.literal("Copiloto"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 220, x = this.width / 2 - w / 2, y = 44, step = 26;
        addRenderableWidget(Button.builder(Component.literal("Avisos y marcado"),
                b -> minecraft.setScreen(new AlertsScreen(this))).bounds(x, y, w, 20).build());
        y += step;
        addRenderableWidget(Button.builder(Component.literal("Color del contorno"),
                b -> minecraft.setScreen(new ColorScreen(this))).bounds(x, y, w, 20).build());
        y += step;
        addRenderableWidget(Button.builder(Component.literal("HUD (hotbar, marcador)"),
                b -> minecraft.setScreen(new HudScreen(this))).bounds(x, y, w, 20).build());
        y += step;
        addRenderableWidget(Button.builder(Component.literal("Mano / item"),
                b -> minecraft.setScreen(new HandScreen(this))).bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(x, this.height - 28, w, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        CopilotoConfig.save();
        minecraft.setScreen(parent);
    }

    // ---------- base de los submenus ----------
    abstract static class Sub extends Screen {
        final Screen parent;

        Sub(String title, Screen parent) {
            super(Component.literal(title));
            this.parent = parent;
        }

        abstract void build(int x, int w);

        @Override
        protected void init() {
            int w = 220;
            build(this.width / 2 - w / 2, w);
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(this.width / 2 - w / 2, this.height - 28, w, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float delta) {
            super.render(g, mx, my, delta);
            g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        }

        @Override
        public void onClose() {
            CopilotoConfig.save();
            minecraft.setScreen(parent);
        }

        void toggle(int x, int y, int w, String label, boolean value, Consumer<Boolean> set) {
            addRenderableWidget(CycleButton.onOffBuilder(value).create(x, y, w, 20,
                    Component.literal(label), (b, v) -> set.accept(v)));
        }

        void slider(int x, int y, int w, String label, float min, float max, boolean whole,
                    Supplier<Float> get, Consumer<Float> set) {
            addRenderableWidget(new Slider(x, y, w, label, min, max, whole, get, set));
        }
    }

    static class AlertsScreen extends Sub {
        AlertsScreen(Screen p) { super("Avisos y marcado", p); }

        @Override
        void build(int x, int w) {
            CopilotoConfig c = CopilotoConfig.INSTANCE;
            int y = 36, s = 24;
            toggle(x, y, w, "Marcar monstruos visibles", c.highlightHostile, v -> c.highlightHostile = v);
            y += s;
            toggle(x, y, w, "Voz: durabilidad de armadura", c.armorAlerts, v -> c.armorAlerts = v);
            y += s;
            toggle(x, y, w, "Voz: vida baja", c.lowHealthAlert, v -> c.lowHealthAlert = v);
            y += s;
            slider(x, y, w, "Avisar pociones si quedan <=", 0f, 36f, true,
                    () -> (float) c.potAlertMax, v -> c.potAlertMax = Math.round(v));
        }
    }

    static class ColorScreen extends Sub {
        private static final List<Integer> COLORS = List.of(0xFF0000, 0x00FF00, 0x3366FF, 0xFFFF00, 0xFF8800, 0xAA00FF, 0x00FFFF, 0xFFFFFF);
        private static final String[] NAMES = {"Rojo", "Verde", "Azul", "Amarillo", "Naranja", "Morado", "Cian", "Blanco"};

        ColorScreen(Screen p) { super("Color del contorno", p); }

        @Override
        void build(int x, int w) {
            CopilotoConfig c = CopilotoConfig.INSTANCE;
            int y = 36, s = 24;
            int initial = COLORS.contains(c.highlightColor) ? c.highlightColor : COLORS.get(0);
            addRenderableWidget(CycleButton.<Integer>builder(v -> Component.literal(NAMES[COLORS.indexOf(v)]))
                    .withValues(COLORS).withInitialValue(initial)
                    .create(x, y, w, 20, Component.literal("Color predefinido"), (b, v) -> {
                        c.highlightColor = v;
                        this.rebuildWidgets();
                    }));
            y += s;
            slider(x, y, w, "Rojo", 0f, 255f, true, () -> (float) ((c.highlightColor >> 16) & 255),
                    v -> c.highlightColor = (Math.round(v) << 16) | (c.highlightColor & 0x00FFFF));
            y += s;
            slider(x, y, w, "Verde", 0f, 255f, true, () -> (float) ((c.highlightColor >> 8) & 255),
                    v -> c.highlightColor = (Math.round(v) << 8) | (c.highlightColor & 0xFF00FF));
            y += s;
            slider(x, y, w, "Azul", 0f, 255f, true, () -> (float) (c.highlightColor & 255),
                    v -> c.highlightColor = Math.round(v) | (c.highlightColor & 0xFFFF00));
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float delta) {
            super.render(g, mx, my, delta);
            int w = 220, x = this.width / 2 - w / 2, y = 36 + 24 * 4;
            g.fill(x, y, x + w, y + 16, 0xFF000000 | CopilotoConfig.INSTANCE.highlightColor);
        }
    }

    static class HudScreen extends Sub {
        HudScreen(Screen p) { super("HUD", p); }

        @Override
        void build(int x, int w) {
            CopilotoConfig c = CopilotoConfig.INSTANCE;
            int y = 36, s = 24;
            toggle(x, y, w, "Mostrar marcador (scoreboard)", c.scoreboardVisible, v -> c.scoreboardVisible = v);
            y += s;
            slider(x, y, w, "Marcador: horizontal", -200f, 200f, true, () -> c.scoreboardX, v -> c.scoreboardX = v);
            y += s;
            slider(x, y, w, "Marcador: vertical", -150f, 150f, true, () -> c.scoreboardY, v -> c.scoreboardY = v);
            y += s;
            slider(x, y, w, "Hotbar: horizontal", -150f, 150f, true, () -> c.hotbarX, v -> c.hotbarX = v);
            y += s;
            slider(x, y, w, "Hotbar: vertical", -100f, 40f, true, () -> c.hotbarY, v -> c.hotbarY = v);
            y += s;
            toggle(x, y, w, "Nombre del item en mano", c.showItemName, v -> c.showItemName = v);
        }
    }

    static class HandScreen extends Sub {
        HandScreen(Screen p) { super("Mano / item", p); }

        @Override
        void build(int x, int w) {
            CopilotoConfig c = CopilotoConfig.INSTANCE;
            int y = 36, s = 24;
            slider(x, y, w, "Izquierda / derecha", -0.3f, 0.3f, false, () -> c.itemX, v -> c.itemX = v);
            y += s;
            slider(x, y, w, "Arriba / abajo", -0.3f, 0.3f, false, () -> c.itemY, v -> c.itemY = v);
            y += s;
            slider(x, y, w, "Cerca / lejos", -0.3f, 0.3f, false, () -> c.itemZ, v -> c.itemZ = v);
            y += s;
            slider(x, y, w, "Tamano", 0.5f, 1.5f, false, () -> c.itemScale, v -> c.itemScale = v);
        }
    }

    // ---------- slider ----------
    static class Slider extends AbstractSliderButton {
        private final String label;
        private final float min, max;
        private final boolean whole;
        private final Consumer<Float> set;

        Slider(int x, int y, int w, String label, float min, float max, boolean whole,
               Supplier<Float> get, Consumer<Float> set) {
            super(x, y, w, 20, Component.empty(), (get.get() - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.whole = whole;
            this.set = set;
            updateMessage();
        }

        private float real() {
            return min + (float) this.value * (max - min);
        }

        @Override
        protected void updateMessage() {
            float r = real();
            setMessage(Component.literal(label + ": " + (whole ? String.valueOf(Math.round(r)) : String.format("%.2f", r))));
        }

        @Override
        protected void applyValue() {
            set.accept(real());
        }
    }
}
