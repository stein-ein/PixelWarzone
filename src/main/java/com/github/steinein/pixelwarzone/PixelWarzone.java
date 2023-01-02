package com.github.steinein.pixelwarzone;

import com.github.steinein.pixelwarzone.commands.WarzoneCommand;
import com.github.steinein.pixelwarzone.config.WarzoneConfig;
import com.github.steinein.pixelwarzone.db_handler.DBHandler;
import com.github.steinein.pixelwarzone.listeners.*;
import com.github.steinein.pixelwarzone.selection.DefinedWarzone;
import com.github.steinein.pixelwarzone.selection.SelectionsManager;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.google.inject.Inject;
import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraftforge.common.MinecraftForge;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.sql.SqlService;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "pixelwarzone",
        name = "PixelWarzone",
        description = "Pikachu goes to gulag.",
        version = "1.12.2-1.1.0",
        authors = {
                "Steinein_"
        }
)
public class PixelWarzone {

    private static final String CONFIG = "pixelwarzone.conf";
    private static PixelWarzone instance;

    @Inject
    private Logger logger;

    @Inject
    public Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDirPath;

    @Inject
    private PluginContainer container;

    private WarzoneConfig pluginConfig;

    private SelectionsManager selectionsManager;
    private List<DefinedWarzone> warzoneList;

    public final Map<UUID, WarzonePlayer> playerDataMap = new HashMap<UUID, WarzonePlayer>();
    public final Map<UUID, UUID> requestMap = new HashMap<>();

    public static PixelWarzone getInstance() {
        return instance;
    }

    private String dbPath;
    private DataSource dataSource;

    private static DBHandler database;

    public static DBHandler getDatabase() {
        return database;
    }

    public Connection getConnection() {
        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Listener
    public void onServerInit(GameInitializationEvent event) {
        instance = this;
        logger.info("Loading plugin configuration...");
        HoconConfigurationLoader loader = this.loadConfig();
        database = new DBHandler();

        if (loader == null) {
            this.logger.error("Something went wrong during configuration load. Stopping plugin!");
            return;
        }

        try {
            this.pluginConfig = new WarzoneConfig(loader);
        } catch (IOException e) {
            this.logger.error("Something went wrong during config parsing! Stopping plugin!");
            e.printStackTrace();
            return;
        }
        logger.info("Plugin configuration successfully loaded.");

        try {
            this.dbPath = String.format("jdbc:h2:%s/players.db;mode=MySQL", PixelWarzone.getInstance().configDirPath);
            this.dataSource = Sponge.getServiceManager().provide(SqlService.class).get().getDataSource(dbPath);
            database.createTables();
        } catch (Exception e) {
            logger.error("Error loading database " + e.getMessage());
        }

        this.selectionsManager = new SelectionsManager();

        logger.info("Loading defined warzones...");
        this.warzoneList = this.pluginConfig.getWarzones();
        logger.info("Loaded " + this.warzoneList.size() + " warzones.");

        logger.info("Registering commands...");
        this.registerCommands();

        logger.info("Registering Pixelmon events...");
        this.registerEvents();

        this.warzoneBossbar();
        this.kickUnwelcomePlayers();
    }

    @Listener
    public void onReload(GameReloadEvent reloadEvent) {
        logger.info("Loading plugin configuration...");
        HoconConfigurationLoader loader = this.loadConfig();

        if (loader == null) {
            this.logger.error("Something went wrong during configuration load. Stopping plugin!");
            return;
        }

        try {
            this.pluginConfig = new WarzoneConfig(loader);
        } catch (IOException e) {
            this.logger.error("Something went wrong during config parsing!");
            e.printStackTrace();
            return;
        }
        logger.info("Plugin configuration successfully loaded.");

        logger.info("Loading defined warzones...");
        this.warzoneList = this.pluginConfig.getWarzones();
        logger.info("Loaded " + this.warzoneList.size() + " warzones.");
    }

    private void warzoneBossbar() {
        Task task = Task.builder().execute(() -> {
            int warzonePlayers = 0;
            for (Player player : Sponge.getServer().getOnlinePlayers()) {
                WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(this, player);
                if (warzonePlayer.inWarzone()) {
                    warzonePlayers++;
                }
            }
            if (warzonePlayers > 0) {
                Sponge.getCommandManager().process(
                        Sponge.getServer().getConsole(), "warzonebossbar " + warzonePlayers
                );
            }
        }).interval(3, TimeUnit.MINUTES).name("Warzone bossbar").submit(this);
    }

    private void kickUnwelcomePlayers() {
        Task task = Task.builder().execute(() -> {
            for (Player player : Sponge.getServer().getOnlinePlayers()) {
                WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(this, player);
                if (warzonePlayer.inWarzone() && !warzonePlayer.inBattle() && !Utils.checkWarzonePlayer(this, player, warzonePlayer)) {
                    logger.info(player.getName() + " was kicked out of the warzone");
                    Task.builder().execute(
                            () -> Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "warp " + player.getName() + " warzone")
                    ).submit(this);
                    player.offer(
                            Keys.POTION_EFFECTS,
                            Collections.singletonList(PotionEffect.of(PotionEffectTypes.BLINDNESS, 1, 200))
                    );
                    return;
                }
            }
        }).interval(20, TimeUnit.SECONDS).name("Kick Unwelcome Players").async().submit(this);
    }

    private HoconConfigurationLoader loadConfig() {

        Optional<Asset> configAsset = Sponge.getAssetManager().getAsset(this, CONFIG);

        if (configAsset.isPresent()) {

            try {

                configAsset.get().copyToDirectory(configDirPath, false, true);

                return HoconConfigurationLoader.builder()
                        .setPath(configDirPath.resolve(CONFIG))
                        .build();

            } catch (IOException e) {

                logger.error("Could not copy the config file to the config directory! Aborting!");
                e.printStackTrace();
                return null;

            }

        } else {

            logger.error("Could not find configuration asset 'pixelwarzone.conf'! Aborting!");
            return null;

        }

    }

    private void registerCommands() {
        Sponge.getCommandManager()
                .register(this, new WarzoneCommand(this).getSpec(), "warzone", "wzone");
    }

    private void registerEvents() {
        Pixelmon.EVENT_BUS.register(new LoseTrainerBattleInWarzone(this));
        Pixelmon.EVENT_BUS.register(new LoseNPCBattle(this));
        Pixelmon.EVENT_BUS.register(new PokeballImpact(this));
        Pixelmon.EVENT_BUS.register(new StartBattle(this));
        Pixelmon.EVENT_BUS.register(new RaidDenEvents(this));
        MinecraftForge.EVENT_BUS.register(new PlayerQuit(this));
        game.getEventManager().registerListeners(this, new PlayerCommand(this));
        game.getEventManager().registerListeners(this, new PlayerConnectionEvents());
    }

    public void debug(String message, Object... args) {
        if (this.getPluginConfig().isDebugEnabled()) {
            this.logger.info(String.format(message, args));
        }
    }

    public void log(String message, Object... args) {
        this.logger.info(String.format(message, args));
    }

    public Logger getLogger() {
        return this.logger;
    }

    public WarzoneConfig getPluginConfig() {
        return this.pluginConfig;
    }

    public SelectionsManager getSelectionsManager() {
        return this.selectionsManager;
    }

    public void updateWarzoneList() {
        this.warzoneList = this.getPluginConfig().getWarzones();
    }

    public List<DefinedWarzone> getWarzoneList() {
        return this.warzoneList;
    }

    public PluginContainer getContainer() {
        return container;
    }
}
