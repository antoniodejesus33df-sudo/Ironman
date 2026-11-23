package com.copiloto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class CopilotoConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("copiloto.json");
    public static CopilotoConfig INSTANCE = new CopilotoConfig();

    // Avisos y marcado
    public boolean highlightHostile = true;
    public int highlightColor = 0xFF0000;
    public boolean armorAlerts = true;
    public boolean lowHealthAlert = true;
    public int potAlertMax = 8;

    // HUD
    public boolean scoreboardVisible = true;
    public float scoreboardX = 0f;
    public float scoreboardY = 0f;
    public float hotbarX = 0f;
    public float hotbarY = 0f;
    public boolean showItemName = true;

    // Mano / item
    public float itemX = 0f;
    public float itemY = 0f;
    public float itemZ = 0f;
    public float itemScale = 1f;

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                CopilotoConfig c = GSON.fromJson(Files.readString(FILE), CopilotoConfig.class);
                if (c != null) INSTANCE = c;
            }
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
