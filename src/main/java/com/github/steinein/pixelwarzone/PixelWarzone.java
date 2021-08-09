package com.github.steinein.pixelwarzone;

import com.github.steinein.pixelwarzone.commands.WarzoneCommand;
import com.github.steinein.pixelwarzone.config.WarzoneConfig;
import com.github.steinein.pixelwarzone.listeners.*;
import com.github.steinein.pixelwarzone.selection.DefinedWarzone;
import com.github.steinein.pixelwarzone.selection.SelectionsManager;
import com.google.inject.Inject;
import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraftforge.common.MinecraftForge;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "pixelwarzone",
        name = "PixelWarzone",
        description = "Pikachu goes to gulag.",
        version = "1.12.2-1.0.0",
        authors = {
                "Steinein_"
        }
)
public class PixelWarzone {

    private static final String CONFIG = "pixelwarzone.conf";

    @Inject
    private Logger logger;

    @Inject
    public Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDirPath;

    private WarzoneConfig pluginConfig;

    private SelectionsManager selectionsManager;
    private List<DefinedWarzone> warzoneList;

    @Listener
    public void onServerInit(GameInitializationEvent event) {

        logger.info("Loading plugin configuration...");
        HoconConfigurationLoader loader = this.loadConfig();

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

        if (this.pluginConfig.isDebugEnabled()) {
            ((org.apache.logging.log4j.core.Logger) LogManager.getLogger(this.logger.getName())).setLevel(Level.DEBUG);
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
        MinecraftForge.EVENT_BUS.register(new PlayerQuit(this));
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
}
