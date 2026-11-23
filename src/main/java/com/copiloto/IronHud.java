package com.copiloto;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

/** HUD estilo Iron Man: compas, mira, objetivo, vitales y panel de armadura a la derecha. */
public final class IronHud {
    static final int CYAN = 0x00E5FF, GOLD = 0xFFC83D, RED = 0xE62429, AMBER = 0xFF9100, DARK = 0x07131F, DIM = 0x4A6A80;

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] LABELS = {"CASCO", "PETO", "PANTALON", "BOTAS"};
    private static final String[] DIRS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
    private static final int[] BASE = {0xB71C1C, 0xB71C1C, 0xA31515, 0xD9A21B};
    private static final int[] TRIM = {GOLD, GOLD, GOLD, 0xB71C1C};

    private IronHud() {}

    // ---------- utilidades ----------
    static int a(int alpha, int rgb) { return (alpha << 24) | (rgb & 0xFFFFFF); }
    static int solid(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }
    static int status(int pct) { return pct > 50 ? CYAN : (pct > 25 ? AMBER : RED); }

    static void rect(GuiGraphics g, int x, int y, int w, int h, int c) { g.fill(x, y, x + w, y + h, c); }

    static void border(GuiGraphics g, int x, int y, int w, int h, int c) {
        rect(g, x, y, w, 1, c);
        rect(g, x, y + h - 1, w, 1, c);
        rect(g, x, y, 1, h, c);
        rect(g, x + w - 1, y, 1, h, c);
    }

    static void brackets(GuiGraphics g, int x, int y, int w, int h, int len, int th, int c) {
        rect(g, x, y, len, th, c);
        rect(g, x, y, th, len, c);
        rect(g, x + w - len, y, len, th, c);
        rect(g, x + w - th, y, th, len, c);
        rect(g, x, y + h - th, len, th, c);
        rect(g, x, y + h - len, th, len, c);
        rect(g, x + w - len, y + h - th, len, th, c);
        rect(g, x + w - th, y + h - len, th, len, c);
    }

    static void panel(GuiGraphics g, int x, int y, int w, int h, int accent) {
        rect(g, x, y, w, h, a(0xA6, DARK));
        border(g, x, y, w, h, a(0x90, accent));
        brackets(g, x - 1, y - 1, w + 2, h + 2, 6, 2, solid(GOLD));
    }

    static void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int color) {
        rect(g, x, y, w, h, a(0x70, 0x000000));
        int filled = (int) Math.round(w * Math.max(0.0, Math.min(1.0, frac)));
        for (int sx = 0; sx < filled; sx += 5) {
            int ex = Math.min(sx + 4, filled);
            rect(g, x + sx, y, ex - sx, h, solid(color));
        }
    }

    static int armorPct(ItemStack s) {
        if (s.isEmpty()) return -1;
        if (!s.isDamageableItem()) return 100;
        int max = s.getMaxDamage();
        return max <= 0 ? 100 : (max - s.getDamageValue()) * 100 / max;
    }

    // ---------- HUD principal ----------
    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        CopilotoConfig c = CopilotoConfig.INSTANCE;
        if (p == null || mc.options.hideGui || !c.ironHud) return;

        int w = g.guiWidth(), h = g.guiHeight();
        long t = System.currentTimeMillis();

        brackets(g, 4, 4, w - 8, h - 8, 18, 1, a(0x90, CYAN));
        crosshair(g, mc, w, h);
        if (c.compass) compass(g, mc, p, w);
        if (c.rivalHud) rival(g, mc, p, w);
        if (c.vitalsPanel) vitals(g, mc, p, h);
        if (c.armorPanel) armorPanel(g, mc, p, w, h, t);
    }

    private static void crosshair(GuiGraphics g, Minecraft mc, int w, int h) {
        Entity look = mc.crosshairPickEntity;
        boolean lock = look instanceof LivingEntity && !(look instanceof ArmorStand);
        int cx = w / 2, cy = h / 2;
        int r = lock ? 8 : 11;
        brackets(g, cx - r, cy - r, r * 2, r * 2, 4, 1, lock ? solid(RED) : a(0xB0, CYAN));
    }

    private static void compass(GuiGraphics g, Minecraft mc, LocalPlayer p, int w) {
        float hd = ((p.getYRot() + 180f) % 360f + 360f) % 360f;
        int cx = w / 2, y = 6, half = 90;
        rect(g, cx - half, y, half * 2, 17, a(0x78, DARK));
        rect(g, cx - half, y, half * 2, 1, a(0x90, CYAN));
        rect(g, cx - half, y + 16, half * 2, 1, a(0x90, CYAN));
        for (int d = (int) Math.floor(hd) - 46; d <= (int) Math.ceil(hd) + 46; d++) {
            int dd = ((d % 360) + 360) % 360;
            if (dd % 5 != 0) continue;
            int x = cx + Math.round((d - hd) * 2f);
            if (x < cx - half + 2 || x > cx + half - 2) continue;
            boolean card = dd % 45 == 0;
            int th = card ? 5 : (dd % 15 == 0 ? 3 : 2);
            rect(g, x, y + 1, 1, th, a(0xC0, card ? GOLD : CYAN));
            if (card) {
                String s = DIRS[dd / 45];
                g.drawString(mc.font, s, x - mc.font.width(s) / 2, y + 7, solid(dd == 0 ? RED : CYAN), false);
            }
        }
        rect(g, cx - 1, y + 17, 3, 3, solid(GOLD));
        g.drawCenteredString(mc.font, String.format("%03d", Math.round(hd) % 360) + "\u00B0", cx, y + 21, solid(GOLD));
    }

    private static void rival(GuiGraphics g, Minecraft mc, LocalPlayer p, int w) {
        LivingEntity e = Rival.target;
        if (e == null) return;
        float max = Math.max(1f, e.getMaxHealth());
        float hp = Math.max(0f, e.getHealth());
        int pct = Math.round(hp / max * 100f);
        int col = status(pct);
        boolean blink = (System.currentTimeMillis() / 250) % 2 == 0;

        int pw = 160, ph = 34;
        int x = w / 2 - pw / 2, y = 44;
        panel(g, x, y, pw, ph, col);

        String name = e.getName().getString().toUpperCase();
        if (name.length() > 16) name = name.substring(0, 16);
        g.drawString(mc.font, "> " + name, x + 6, y + 4, solid(RED), false);
        String dist = Math.round(p.distanceTo(e)) + " m";
        g.drawString(mc.font, dist, x + pw - 6 - mc.font.width(dist), y + 4, solid(DIM + 0x203040), false);
        bar(g, x + 6, y + 15, pw - 12, 6, pct / 100.0, col);
        String txt = pct + "%   " + Math.round(hp) + "/" + Math.round(max);
        int tc = (pct <= 25 && blink) ? RED : col;
        g.drawString(mc.font, txt, x + 6, y + 24, solid(tc), false);
    }

    private static void vitals(GuiGraphics g, Minecraft mc, LocalPlayer p, int h) {
        int pw = 92, ph = 60;
        int x = 6, y = Math.max(40, h / 2 - ph / 2);
        float max = Math.max(1f, p.getMaxHealth());
        int hpPct = Math.round(p.getHealth() / max * 100f);
        int food = p.getFoodData().getFoodLevel();
        int pots = CopilotoClient.pots;

        panel(g, x, y, pw, ph, CYAN);
        g.drawString(mc.font, "VITALES", x + 6, y + 4, solid(CYAN), false);
        rect(g, x + 4, y + 14, pw - 8, 1, a(0x60, CYAN));

        g.drawString(mc.font, "INTEG", x + 6, y + 18, solid(DIM + 0x203040), false);
        String hv = Math.round(p.getHealth()) + "/" + Math.round(max);
        g.drawString(mc.font, hv, x + pw - 6 - mc.font.width(hv), y + 18, solid(status(hpPct)), false);
        bar(g, x + 6, y + 27, pw - 12, 4, hpPct / 100.0, status(hpPct));

        g.drawString(mc.font, "ENERGIA", x + 6, y + 34, solid(DIM + 0x203040), false);
        bar(g, x + 6, y + 43, pw - 12, 4, food / 20.0, CYAN);

        int pc = pots == 0 ? RED : (pots <= 3 ? AMBER : CYAN);
        g.drawString(mc.font, "POCIONES", x + 6, y + 49, solid(DIM + 0x203040), false);
        String pv = String.valueOf(pots);
        g.drawString(mc.font, pv, x + pw - 6 - mc.font.width(pv), y + 49, solid(pc), false);
    }

    private static void armorPanel(GuiGraphics g, Minecraft mc, LocalPlayer p, int w, int h, long t) {
        int pw = 140, ph = 146;
        int x = w - pw - 6, y = Math.max(40, h / 2 - ph / 2);
        int[] pct = new int[4];
        int sum = 0, n = 0, worst = 100;
        for (int i = 0; i < 4; i++) {
            pct[i] = armorPct(p.getItemBySlot(SLOTS[i]));
            if (pct[i] >= 0) {
                sum += pct[i];
                n++;
                worst = Math.min(worst, pct[i]);
            }
        }
        int overall = n == 0 ? -1 : sum / n;
        panel(g, x, y, pw, ph, n == 0 ? DIM : status(worst));
        g.drawString(mc.font, "J.A.R.V.I.S. // TRAJE", x + 6, y + 4, solid(CYAN), false);
        rect(g, x + 4, y + 14, pw - 8, 1, a(0x60, CYAN));

        figure(g, x + 8, y + 20, 3, pct, t);

        boolean blink = (t / 300) % 2 == 0;
        int colX = x + 62, colW = x + pw - 6 - colX;
        for (int i = 0; i < 4; i++) {
            int by = y + 22 + i * 24;
            int col = pct[i] < 0 ? DIM : status(pct[i]);
            if (pct[i] >= 0 && pct[i] <= 25 && !blink) col = DIM;
            g.drawString(mc.font, LABELS[i], colX, by, solid(pct[i] < 0 ? DIM : CYAN), false);
            String ps = pct[i] < 0 ? "--" : pct[i] + "%";
            g.drawString(mc.font, ps, x + pw - 6 - mc.font.width(ps), by, solid(col), false);
            bar(g, colX, by + 10, colW, 4, pct[i] < 0 ? 0 : pct[i] / 100.0, col);
        }

        rect(g, x + 4, y + 118, pw - 8, 1, a(0x60, CYAN));
        if (overall < 0) {
            g.drawString(mc.font, "SIN ARMADURA", x + 6, y + 122, solid(DIM), false);
            bar(g, x + 6, y + 134, pw - 12, 5, 0, DIM);
        } else {
            g.drawString(mc.font, "INTEGRIDAD", x + 6, y + 122, solid(CYAN), false);
            String os = overall + "%";
            g.drawString(mc.font, os, x + pw - 6 - mc.font.width(os), y + 122, solid(status(overall)), false);
            bar(g, x + 6, y + 134, pw - 12, 5, overall / 100.0, status(overall));
        }
    }

    // Figura 16x32 "celdas" estilo humanoide: 0 casco, 1 peto+brazos, 2 pantalones, 3 botas
    private static int pieceAt(int col, int row) {
        if (col < 0 || col > 15 || row < 0 || row > 31) return -1;
        boolean mid = col >= 4 && col <= 11;
        if (row < 8) return mid ? 0 : -1;
        if (row < 20) return 1;
        if (row < 28) return mid ? 2 : -1;
        return mid ? 3 : -1;
    }

    private static void figure(GuiGraphics g, int x0, int y0, int s, int[] pct, long t) {
        boolean pulse = (t / 350) % 2 == 0;
        boolean blink = (t / 300) % 2 == 0;
        int scan = (int) ((t / 70) % 40);
        for (int row = 0; row < 32; row++) {
            for (int col = 0; col < 16; col++) {
                int piece = pieceAt(col, row);
                if (piece < 0) continue;
                int st = pct[piece];
                boolean trim = pieceAt(col - 1, row) != piece || pieceAt(col + 1, row) != piece
                        || pieceAt(col, row - 1) != piece || pieceAt(col, row + 1) != piece;
                int color;
                if (st < 0) {
                    color = trim ? a(0x99, CYAN) : a(0x38, 0x6EC3FF);
                } else {
                    color = trim ? solid(TRIM[piece]) : a(0xF0, BASE[piece]);
                    if (st <= 25 && !blink) color = a(0xF0, trim ? 0x6A4A10 : 0x3A0808);
                }
                // junturas del torso
                if (st >= 0 && piece == 1 && (col == 4 || col == 11) && row > 8 && row < 19) color = a(0xF0, 0x7A1212);
                // placa facial dorada con ojos
                if (piece == 0 && row >= 2 && row <= 5 && col >= 5 && col <= 10) {
                    boolean eye = row == 3 && (col <= 6 || col >= 9);
                    color = st < 0 ? a(0x55, CYAN) : (eye ? solid(0xE0FFFF) : solid(GOLD));
                }
                // reactor arc en el pecho
                if (piece == 1 && col >= 7 && col <= 8 && row >= 11 && row <= 12) {
                    color = solid(pulse ? 0xFFFFFF : 0x7FF4FF);
                } else if (piece == 1 && col >= 6 && col <= 9 && row >= 10 && row <= 13) {
                    color = a(0xC0, CYAN);
                }
                // repulsores en las palmas
                if (piece == 1 && row == 19 && (col == 1 || col == 2 || col == 13 || col == 14)) {
                    color = solid(pulse ? 0x7FF4FF : CYAN);
                }
                g.fill(x0 + col * s, y0 + row * s, x0 + col * s + s, y0 + row * s + s, color);
                if (row == scan) g.fill(x0 + col * s, y0 + row * s, x0 + col * s + s, y0 + row * s + s, a(0x55, 0xFFFFFF));
            }
        }
    }

    // ---------- menus (titulo / pausa / fuera de mundo) ----------
    public static void menuOverlay(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        long t = System.currentTimeMillis();
        g.fillGradient(0, 0, w, 46, a(0xB0, 0x00060F), 0x00000000);
        g.fillGradient(0, h - 46, w, h, 0x00000000, a(0xB0, 0x00060F));
        for (int x = 0; x < w; x += 40) rect(g, x, 0, 1, h, a(0x12, CYAN));
        for (int y = 0; y < h; y += 40) rect(g, 0, y, w, 1, a(0x12, CYAN));
        brackets(g, 8, 8, w - 16, h - 16, 26, 2, a(0xC0, CYAN));
        brackets(g, 12, 12, w - 24, h - 24, 10, 1, solid(GOLD));
        int sy = (int) ((t / 14) % Math.max(1, h));
        rect(g, 0, sy, w, 2, a(0x30, CYAN));
        g.drawString(mc.font, "J.A.R.V.I.S. // SISTEMA EN LINEA", 20, 18, solid(CYAN), false);
        g.drawString(mc.font, "COPILOTO  ::  TRAJE LISTO", 20, 29, solid(GOLD), false);
        String r = "MARK VII";
        g.drawString(mc.font, r, w - 20 - mc.font.width(r), h - 24, solid(RED), false);
    }
}
