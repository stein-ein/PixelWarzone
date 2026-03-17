package com.github.steinein.pixelwarzone;

import com.github.steinein.pixelwarzone.commands.WarzoneCommand;
import com.github.steinein.pixelwarzone.config.WarzoneConfig;
import com.github.steinein.pixelwarzone.db_handler.DBHandler;
import com.github.steinein.pixelwarzone.listeners.*;
import com.github.steinein.pixelwarzone.selection.DefinedWarzone;
import com.github.steinein.pixelwarzone.selection.SelectionsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod("pixelwarzone")
public class PixelWarzone {

    public static final String MOD_ID = "pixelwarzone";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static PixelWarzone instance;

    private WarzoneConfig pluginConfig;
    private SelectionsManager selectionsManager;
    private List<DefinedWarzone> warzoneList;
    private DBHandler database;

    public final Map<UUID, WarzonePlayer> playerDataMap = new HashMap<>();
    public final Map<UUID, UUID> requestMap = new HashMap<>();

    // Tick counters for periodic tasks
    private int bossbarTickCounter = 0;
    private int kickTickCounter = 0;
    private int guiFixTickCounter = 0;
    private int autoAcceptTickCounter = 0;
    private int teamSelectTickCounter = 0;
    private int bossbarInterval   = 20 * 60 * 3; // updated from config on start/reload
    private int bossbarHideTimer  = 0;            // ticks remaining before hiding the bossbar
    private static final int KICK_INTERVAL = 20 * 20;        // 20 seconds
    private static final int GUI_FIX_INTERVAL = 20 * 3;      // 3 seconds
    private static final int AUTO_ACCEPT_INTERVAL = 10;      // every 10 ticks (0.5s)
    private static final int TEAM_SELECT_INTERVAL = 5;       // every 5 ticks (0.25s)

    public static PixelWarzone getInstance() {
        return instance;
    }

    private final ServerBossEvent warzoneBossBar = new ServerBossEvent(
            Component.literal("§4§lWarzone §r§7- §ePlayers inside: §f0"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private boolean pixelListenersRegistered = false;

    public PixelWarzone(IEventBus modEventBus) {
        instance = this;
        // NeoForge events (ServerStartingEvent, ServerTickEvent, CommandEvent, etc.)
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new PlayerQuit(this));
        NeoForge.EVENT_BUS.register(new PlayerCommand(this));
        NeoForge.EVENT_BUS.register(new PlayerConnectionEvents(this));
        // Pixelmon event listeners are registered in onServerStarting() to ensure
        // Pixelmon is fully loaded before we touch its EVENT_BUS.
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("PixelWarzone starting...");

        // Register Pixelmon event listeners here (not in constructor) so that
        // Pixelmon's EVENT_BUS is guaranteed to be initialized first.
        if (!pixelListenersRegistered) {
            pixelListenersRegistered = true;
            var pixelBus = com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS;
            LOGGER.info("Registering Pixelmon listeners on bus: {}",
                    pixelBus == null ? "NULL" : pixelBus.getClass().getName());
            if (pixelBus != null) {
                var loseTrainer = new LoseTrainerBattleInWarzone(this);
                var pokeball    = new PokeballImpact(this);
                var startBattle = new StartBattle(this);
                var raidDen     = new RaidDenEvents(this);
                var loseNpc     = new LoseNPCBattle(this);
                pixelBus.addListener(loseTrainer::loseBattle);
                pixelBus.addListener(pokeball::onPokeballImpact);
                pixelBus.addListener(startBattle::startBattle);
                pixelBus.addListener(raidDen::onStartRaid);
                pixelBus.addListener(loseNpc::loseNPCBattle);
                LOGGER.info("Pixelmon listeners registered successfully.");
            } else {
                LOGGER.error("Pixelmon EVENT_BUS is null — Pixelmon events will not fire!");
            }
        }

        Path configDir = FMLPaths.CONFIGDIR.get().resolve("pixelwarzone");
        try {
            Files.createDirectories(configDir);
            this.pluginConfig = new WarzoneConfig(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to load config: {}", e.getMessage());
            return;
        }

        this.database = new DBHandler(pluginConfig.getDatabaseConfig());
        this.database.createTables();

        // Load stats for any already-online players (handles hot-reload / restart with players on)
        for (ServerPlayer sp : event.getServer().getPlayerList().getPlayers()) {
            playerDataMap.computeIfAbsent(sp.getUUID(), k -> new WarzonePlayer(this, sp));
            database.loadPlayer(sp.getUUID());
        }

        this.selectionsManager = new SelectionsManager();
        this.warzoneList = pluginConfig.getWarzones();
        this.bossbarInterval = 20 * 60 * pluginConfig.getBossbarIntervalMinutes();
        LOGGER.info("Loaded {} warzones.", warzoneList.size());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Save all online players on shutdown
        playerDataMap.values().forEach(wp -> {
            if (wp.getPlayer() != null) {
                database.savePlayer(wp);
            }
        });
        database.close();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        new WarzoneCommand(this).register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        bossbarTickCounter++;
        kickTickCounter++;
        guiFixTickCounter++;

        if (bossbarTickCounter >= bossbarInterval) {
            bossbarTickCounter = 0;
            warzoneBossbar(event.getServer());
        }

        if (bossbarHideTimer > 0) {
            bossbarHideTimer--;
            if (bossbarHideTimer == 0) {
                warzoneBossBar.removeAllPlayers();
            }
        }

        if (kickTickCounter >= KICK_INTERVAL) {
            kickTickCounter = 0;
            kickUnwelcomePlayers();
        }

        if (guiFixTickCounter >= GUI_FIX_INTERVAL) {
            guiFixTickCounter = 0;
            clearStuckGuiOpened(event.getServer());
        }

        autoAcceptTickCounter++;
        if (autoAcceptTickCounter >= AUTO_ACCEPT_INTERVAL) {
            autoAcceptTickCounter = 0;
            autoAcceptWarzoneQueries();
        }

        teamSelectTickCounter++;
        if (teamSelectTickCounter >= TEAM_SELECT_INTERVAL) {
            teamSelectTickCounter = 0;
            autoSelectTeams(event.getServer());
        }
    }

    private void warzoneBossbar(net.minecraft.server.MinecraftServer server) {
        if (!pluginConfig.isBossbarEnabled()) {
            warzoneBossBar.removeAllPlayers();
            return;
        }

        List<ServerPlayer> inWarzone = server.getPlayerList().getPlayers().stream()
                .filter(p -> getWarzoneList().stream().anyMatch(w -> w.hasPlayer(p)))
                .toList();

        boolean meetsThreshold = inWarzone.size() >= pluginConfig.getBossbarMinPlayers();

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            boolean shouldSee = meetsThreshold;
            boolean seeing = warzoneBossBar.getPlayers().contains(p);
            if (shouldSee && !seeing) warzoneBossBar.addPlayer(p);
            else if (!shouldSee && seeing) warzoneBossBar.removePlayer(p);
        }

        if (meetsThreshold) {
            warzoneBossBar.setName(Component.literal(
                    "§4§lWarzone §r§7- §ePlayers inside: §f" + inWarzone.size()));
            warzoneBossBar.setProgress(1.0f);
            bossbarHideTimer = pluginConfig.getBossbarDisplaySeconds() * 20;
        }
    }

    private void autoAcceptWarzoneQueries() {
        // If either player in a pending battle challenge is in the warzone, auto-accept on behalf
        // of the opponent so they cannot decline.
        var queries = new java.util.ArrayList<>(
                com.pixelmonmod.pixelmon.battles.query.BattleQueryManager.getQueries().values());
        if (!queries.isEmpty()) {
            debug("autoAccept: %d pending queries", queries.size());
        }
        for (var query : queries) {
            net.minecraft.server.level.ServerPlayer opponent = query.getOpponent();
            net.minecraft.server.level.ServerPlayer challenger = query.getChallenger();
            debug("autoAccept: query opponent=%s challenger=%s",
                    opponent == null ? "null" : opponent.getName().getString(),
                    challenger == null ? "null" : challenger.getName().getString());
            if (opponent == null) continue;
            boolean opponentInWarzone = getWarzoneList().stream().anyMatch(w -> w.hasPlayer(opponent));
            debug("autoAccept: opponent %s inWarzone=%s pos=(%d,%d,%d)",
                    opponent.getName().getString(), opponentInWarzone,
                    opponent.blockPosition().getX(), opponent.blockPosition().getY(), opponent.blockPosition().getZ());
            if (!opponentInWarzone) continue;
            debug("Auto-accepting battle query for %s and %s (in warzone)",
                    opponent.getName().getString(),
                    challenger == null ? "null" : challenger.getName().getString());
            // Both players start with state=NONE; startBattle() only fires when BOTH are ACCEPT.
            if (challenger != null) query.acceptQuery(challenger, com.pixelmonmod.pixelmon.battles.query.BattleQueryResponseState.ACCEPT);
            query.acceptQuery(opponent, com.pixelmonmod.pixelmon.battles.query.BattleQueryResponseState.ACCEPT);
        }
    }

    private void autoSelectTeams(net.minecraft.server.MinecraftServer server) {
        try {
            // Access private teamSelectMap in TeamSelectionRegistry
            var tsrClass = com.pixelmonmod.pixelmon.battles.api.rules.teamselection.TeamSelectionRegistry.class;
            var tsClass  = com.pixelmonmod.pixelmon.battles.api.rules.teamselection.TeamSelection.class;
            var psClass  = com.pixelmonmod.pixelmon.battles.api.rules.teamselection.ParticipantSelection.class;

            java.lang.reflect.Field mapField = tsrClass.getDeclaredField("teamSelectMap");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<java.util.UUID,
                    com.pixelmonmod.pixelmon.battles.api.rules.teamselection.TeamSelection> map =
                    (java.util.Map<java.util.UUID,
                            com.pixelmonmod.pixelmon.battles.api.rules.teamselection.TeamSelection>)
                            mapField.get(null);
            if (map.isEmpty()) return;
            debug("autoSelectTeams: %d pending selections", map.size());

            java.lang.reflect.Field participantsField = tsClass.getDeclaredField("participants");
            participantsField.setAccessible(true);
            java.lang.reflect.Field storageField   = psClass.getDeclaredField("storage");
            storageField.setAccessible(true);
            java.lang.reflect.Field confirmedField = psClass.getDeclaredField("confirmed");
            confirmedField.setAccessible(true);
            java.lang.reflect.Field teamField      = psClass.getDeclaredField("team");
            teamField.setAccessible(true);
            // addTeamMember(int) uses storage.get(slot) + canBattle() — correct unlike setTeam
            java.lang.reflect.Method addTeamMember = psClass.getDeclaredMethod("addTeamMember", int.class);
            addTeamMember.setAccessible(true);
            java.lang.reflect.Method isReady       = tsClass.getDeclaredMethod("isReady");
            isReady.setAccessible(true);
            java.lang.reflect.Method startBattle   = tsClass.getDeclaredMethod("startBattle",
                    net.minecraft.core.RegistryAccess.class);
            startBattle.setAccessible(true);

            for (var ts : new java.util.ArrayList<>(map.values())) {
                var participants =
                        (com.pixelmonmod.pixelmon.battles.api.rules.teamselection.ParticipantSelection[])
                                participantsField.get(ts);
                for (var ps : participants) {
                    if ((boolean) confirmedField.get(ps)) continue; // already confirmed
                    var storage = (com.pixelmonmod.pixelmon.api.storage.PartyStorage) storageField.get(ps);
                    if (!(storage instanceof com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage pps)) continue;
                    net.minecraft.server.level.ServerPlayer sp = pps.getPlayer();
                    if (sp == null) continue;
                    boolean inWarzone = getWarzoneList().stream().anyMatch(w -> w.hasPlayer(sp));
                    if (!inWarzone) continue;
                    debug("autoSelectTeams: selecting team for %s", sp.getName().getString());
                    // Clear team and re-add each battlable Pokemon via addTeamMember
                    ((java.util.List<?>) teamField.get(ps)).clear();
                    com.pixelmonmod.pixelmon.api.pokemon.Pokemon[] all = storage.getAll();
                    for (int i = 0; i < all.length; i++) {
                        if (all[i] != null && !all[i].isEgg()) {
                            addTeamMember.invoke(ps, i);
                        }
                    }
                    int teamSize = ((java.util.List<?>) teamField.get(ps)).size();
                    debug("autoSelectTeams: %s team size=%d, confirming", sp.getName().getString(), teamSize);
                    confirmedField.set(ps, true);
                }
                if ((boolean) isReady.invoke(ts)) {
                    debug("autoSelectTeams: all confirmed, starting battle");
                    startBattle.invoke(ts, server.registryAccess());
                }
            }
        } catch (Exception e) {
            debug("autoSelectTeams reflection failed: %s: %s", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void clearStuckGuiOpened(net.minecraft.server.MinecraftServer server) {
        // Pixelmon sets guiOpened=true when a battle GUI opens on the client.
        // It only resets via a client packet that can fail (declined challenge, crashed screen, etc.).
        // Any player not currently in a battle should never have guiOpened=true.
        for (net.minecraft.server.level.ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (com.pixelmonmod.pixelmon.battles.BattleRegistry.inBattle(sp)) continue;
            var party = com.pixelmonmod.pixelmon.api.storage.StorageProxy.getPartyNow(sp);
            if (party != null && party.guiOpened) {
                party.guiOpened = false;
                debug("Cleared stuck guiOpened for %s", sp.getName().getString());
            }
        }
    }

    private void kickUnwelcomePlayers() {
        // Iterate a copy to avoid ConcurrentModificationException
        for (WarzonePlayer wp : new java.util.ArrayList<>(playerDataMap.values())) {
            ServerPlayer sp = wp.getPlayer();
            if (sp == null || !sp.isAlive()) continue;
            // Direct bounds check so ops are also subject to entry requirements
            var warzone = getWarzoneList().stream().filter(w -> w.hasPlayer(sp)).findFirst().orElse(null);
            if (warzone == null) continue;
            if (wp.inBattle()) continue;
            if (!com.github.steinein.pixelwarzone.utils.Utils.checkWarzonePlayer(this, sp, wp)) {
                log("%s was kicked out of warzone '%s'", sp.getName().getString(), warzone.getName());
                teleportToWarzoneSpawn(sp, warzone.getName());
            }
        }
    }

    public void teleportToWarzoneSpawn(ServerPlayer player, String warzoneName) {
        var config = getPluginConfig();
        if (config == null || !config.hasSpawn(warzoneName)) {
            // Fall back to world spawn if no spawn is configured
            var overworld = player.getServer().overworld();
            var spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            return;
        }
        double[] pos = config.getSpawn(warzoneName);
        net.minecraft.resources.ResourceLocation dim =
                net.minecraft.resources.ResourceLocation.parse(config.getSpawnWorld(warzoneName));
        net.minecraft.server.level.ServerLevel level = player.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, dim));
        if (level == null) level = player.getServer().overworld();
        player.teleportTo(level, pos[0], pos[1], pos[2], player.getYRot(), player.getXRot());
    }

    public void reload() {
        try {
            pluginConfig.load();
            warzoneList = pluginConfig.getWarzones();
            bossbarInterval = 20 * 60 * pluginConfig.getBossbarIntervalMinutes();
            LOGGER.info("Config reloaded. {} warzones loaded.", warzoneList.size());
        } catch (IOException e) {
            LOGGER.error("Failed to reload config: {}", e.getMessage());
        }
        // Refresh in-memory stats for all currently online players from DB
        for (WarzonePlayer wp : new java.util.ArrayList<>(playerDataMap.values())) {
            if (wp.getPlayer() != null) {
                database.loadPlayer(wp.getPlayer().getUUID());
            }
        }
    }

    public void debug(String message, Object... args) {
        if (pluginConfig != null && pluginConfig.isDebugEnabled()) {
            LOGGER.info("[DEBUG] " + String.format(message, args));
        }
    }

    public void log(String message, Object... args) {
        LOGGER.info(String.format(message, args));
    }

    public Logger getLogger() { return LOGGER; }
    public WarzoneConfig getPluginConfig() { return pluginConfig; }
    public SelectionsManager getSelectionsManager() { return selectionsManager; }
    public List<DefinedWarzone> getWarzoneList() { return warzoneList; }
    public DBHandler getDatabase() { return database; }

    public void updateWarzoneList() {
        warzoneList = pluginConfig.getWarzones();
    }
}
