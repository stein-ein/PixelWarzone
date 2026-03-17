package com.github.steinein.pixelwarzone.config;

import com.github.steinein.pixelwarzone.selection.DefinedWarzone;
import com.github.steinein.pixelwarzone.selection.Point;
import com.github.steinein.pixelwarzone.selection.WarzoneSelection;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WarzoneConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarzoneConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;
    private JsonObject root;

    public WarzoneConfig(Path configDir) throws IOException {
        this.configFile = configDir.resolve("pixelwarzone.json");
        Files.createDirectories(configDir);
        if (!Files.exists(this.configFile)) {
            writeDefaults();
        }
        load();
    }

    private void writeDefaults() throws IOException {
        JsonObject defaults = new JsonObject();

        JsonObject settings = new JsonObject();
        settings.addProperty(ConfigOption.DEBUG, false);
        settings.addProperty(ConfigOption.LOSE_POKEMON, 1);
        settings.addProperty(ConfigOption.TURN_TIME, 60);
        settings.addProperty(ConfigOption.MIN_WARP_LEVEL, 1);
        settings.addProperty(ConfigOption.MAX_WARP_LEVEL, 100);
        settings.addProperty(ConfigOption.DISABLE_HA, false);
        settings.addProperty(ConfigOption.PREFIX, "&4&l[PixelWarzone] &r");
        settings.addProperty(ConfigOption.SHOW_GUI_LEADERBOARD, false);
        settings.addProperty(ConfigOption.BROADCAST_DEN, false);
        JsonObject bossbar = new JsonObject();
        bossbar.addProperty(ConfigOption.BOSSBAR_ENABLED, true);
        bossbar.addProperty(ConfigOption.BOSSBAR_INTERVAL, 3);
        bossbar.addProperty(ConfigOption.BOSSBAR_MIN_PLAYERS, 2);
        bossbar.addProperty(ConfigOption.BOSSBAR_DISPLAY_SECS, 10);
        settings.add(ConfigOption.BOSSBAR, bossbar);
        defaults.add(ConfigOption.SETTINGS, settings);

        JsonObject database = new JsonObject();
        database.addProperty(ConfigOption.DB_POOL_NAME, "PixelWarzone");
        database.addProperty(ConfigOption.DB_IP, "0.0.0.0");
        database.addProperty(ConfigOption.DB_PORT, 3306);
        database.addProperty(ConfigOption.DB_USERNAME, "admin");
        database.addProperty(ConfigOption.DB_PASSWORD, "password");
        database.addProperty(ConfigOption.DB_DATABASE, "PixelWarzone");
        defaults.add(ConfigOption.DATABASE, database);

        defaults.add(ConfigOption.WARZONE, new JsonObject());

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(defaults, writer);
        }
    }

    public void load() throws IOException {
        try (Reader reader = new InputStreamReader(Files.newInputStream(configFile), StandardCharsets.UTF_8)) {
            this.root = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private void save() {
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    private JsonObject settings() {
        return root.getAsJsonObject(ConfigOption.SETTINGS);
    }

    public boolean isDebugEnabled() {
        return settings().has(ConfigOption.DEBUG) && settings().get(ConfigOption.DEBUG).getAsBoolean();
    }

    public boolean showGUILeaderboard() {
        return settings().has(ConfigOption.SHOW_GUI_LEADERBOARD) && settings().get(ConfigOption.SHOW_GUI_LEADERBOARD).getAsBoolean();
    }

    private JsonObject bossbar() {
        JsonObject s = settings();
        return s.has(ConfigOption.BOSSBAR) ? s.getAsJsonObject(ConfigOption.BOSSBAR) : new JsonObject();
    }

    public boolean isBossbarEnabled() {
        JsonObject b = bossbar();
        return !b.has(ConfigOption.BOSSBAR_ENABLED) || b.get(ConfigOption.BOSSBAR_ENABLED).getAsBoolean();
    }

    public int getBossbarIntervalMinutes() {
        JsonObject b = bossbar();
        return b.has(ConfigOption.BOSSBAR_INTERVAL) ? Math.max(1, b.get(ConfigOption.BOSSBAR_INTERVAL).getAsInt()) : 3;
    }

    public int getBossbarMinPlayers() {
        JsonObject b = bossbar();
        return b.has(ConfigOption.BOSSBAR_MIN_PLAYERS) ? Math.max(1, b.get(ConfigOption.BOSSBAR_MIN_PLAYERS).getAsInt()) : 2;
    }

    public int getBossbarDisplaySeconds() {
        JsonObject b = bossbar();
        return b.has(ConfigOption.BOSSBAR_DISPLAY_SECS) ? Math.max(1, b.get(ConfigOption.BOSSBAR_DISPLAY_SECS).getAsInt()) : 10;
    }

    public boolean broadcastDen() {
        return settings().has(ConfigOption.BROADCAST_DEN) && settings().get(ConfigOption.BROADCAST_DEN).getAsBoolean();
    }

    public int losePokemon() {
        int val = settings().has(ConfigOption.LOSE_POKEMON) ? settings().get(ConfigOption.LOSE_POKEMON).getAsInt() : 1;
        return Math.max(0, Math.min(6, val));
    }

    public int getTurnTimeSeconds() {
        return Math.max(0, settings().has(ConfigOption.TURN_TIME) ? settings().get(ConfigOption.TURN_TIME).getAsInt() : 60);
    }

    public int getMinWarpLevel() {
        return Math.max(0, settings().has(ConfigOption.MIN_WARP_LEVEL) ? settings().get(ConfigOption.MIN_WARP_LEVEL).getAsInt() : 1);
    }

    public int getMaxWarpLevel() {
        return Math.max(0, settings().has(ConfigOption.MAX_WARP_LEVEL) ? settings().get(ConfigOption.MAX_WARP_LEVEL).getAsInt() : 100);
    }

    public boolean getDisableHA() {
        return settings().has(ConfigOption.DISABLE_HA) && settings().get(ConfigOption.DISABLE_HA).getAsBoolean();
    }

    public String getPrefix() {
        return settings().has(ConfigOption.PREFIX) ? settings().get(ConfigOption.PREFIX).getAsString() : "&4&l[PixelWarzone] &r";
    }

    public DatabaseConfig getDatabaseConfig() {
        JsonObject db = root.has(ConfigOption.DATABASE)
                ? root.getAsJsonObject(ConfigOption.DATABASE)
                : new JsonObject();
        return new DatabaseConfig(
                db.has(ConfigOption.DB_POOL_NAME) ? db.get(ConfigOption.DB_POOL_NAME).getAsString() : "PixelWarzone",
                db.has(ConfigOption.DB_IP)        ? db.get(ConfigOption.DB_IP).getAsString()        : "0.0.0.0",
                db.has(ConfigOption.DB_PORT)      ? db.get(ConfigOption.DB_PORT).getAsInt()         : 3306,
                db.has(ConfigOption.DB_USERNAME)  ? db.get(ConfigOption.DB_USERNAME).getAsString()  : "admin",
                db.has(ConfigOption.DB_PASSWORD)  ? db.get(ConfigOption.DB_PASSWORD).getAsString()  : "password",
                db.has(ConfigOption.DB_DATABASE)  ? db.get(ConfigOption.DB_DATABASE).getAsString()  : "PixelWarzone"
        );
    }

    public Set<String> getWarzoneNames() {
        JsonObject warzones = root.getAsJsonObject(ConfigOption.WARZONE);
        return warzones.keySet();
    }

    public void createWarzone(final String name, final WarzoneSelection region) {
        JsonObject warzones = root.getAsJsonObject(ConfigOption.WARZONE);

        JsonObject warzone = new JsonObject();

        JsonObject firstPos = new JsonObject();
        firstPos.addProperty("x", region.getFirstPos().getX());
        firstPos.addProperty("z", region.getFirstPos().getZ());
        warzone.add("firstPos", firstPos);

        JsonObject secondPos = new JsonObject();
        secondPos.addProperty("x", region.getSecondPos().getX());
        secondPos.addProperty("z", region.getSecondPos().getZ());
        warzone.add("secondPos", secondPos);

        warzone.addProperty("world", region.getWorld());

        warzones.add(name, warzone);
        save();
    }

    public void deleteWarzone(final String name) {
        root.getAsJsonObject(ConfigOption.WARZONE).remove(name);
        save();
    }

    public DefinedWarzone getWarzone(final String name) {
        JsonObject warzone = root.getAsJsonObject(ConfigOption.WARZONE).getAsJsonObject(name);

        Point firstPos = new Point(
                warzone.getAsJsonObject("firstPos").get("x").getAsInt(),
                warzone.getAsJsonObject("firstPos").get("z").getAsInt()
        );

        Point secondPos = new Point(
                warzone.getAsJsonObject("secondPos").get("x").getAsInt(),
                warzone.getAsJsonObject("secondPos").get("z").getAsInt()
        );

        String world = warzone.get("world").getAsString();

        return new DefinedWarzone(name, new WarzoneSelection(firstPos, secondPos, world));
    }

    public boolean hasSpawn(String warzoneName) {
        JsonObject warzones = root.getAsJsonObject(ConfigOption.WARZONE);
        if (warzones == null || !warzones.has(warzoneName)) return false;
        return warzones.getAsJsonObject(warzoneName).has(ConfigOption.SPAWN);
    }

    public double[] getSpawn(String warzoneName) {
        JsonObject sp = root.getAsJsonObject(ConfigOption.WARZONE)
                .getAsJsonObject(warzoneName).getAsJsonObject(ConfigOption.SPAWN);
        return new double[]{ sp.get("x").getAsDouble(), sp.get("y").getAsDouble(), sp.get("z").getAsDouble() };
    }

    public String getSpawnWorld(String warzoneName) {
        JsonObject sp = root.getAsJsonObject(ConfigOption.WARZONE)
                .getAsJsonObject(warzoneName).getAsJsonObject(ConfigOption.SPAWN);
        return sp.has("world") ? sp.get("world").getAsString() : "minecraft:overworld";
    }

    public void setSpawn(String warzoneName, double x, double y, double z, String world) {
        JsonObject warzones = root.getAsJsonObject(ConfigOption.WARZONE);
        if (!warzones.has(warzoneName)) return;
        JsonObject sp = new JsonObject();
        sp.addProperty("x", x);
        sp.addProperty("y", y);
        sp.addProperty("z", z);
        sp.addProperty("world", world);
        warzones.getAsJsonObject(warzoneName).add(ConfigOption.SPAWN, sp);
        save();
    }

    public List<DefinedWarzone> getWarzones() {
        List<DefinedWarzone> list = new ArrayList<>();
        for (String name : getWarzoneNames()) {
            list.add(getWarzone(name));
        }
        return list;
    }
}
