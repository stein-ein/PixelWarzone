package com.github.steinein.pixelwarzone.config;

import com.github.steinein.pixelwarzone.selection.DefinedWarzone;
import com.github.steinein.pixelwarzone.selection.Point;
import com.github.steinein.pixelwarzone.selection.WarzoneSelection;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WarzoneConfig {

    private final HoconConfigurationLoader loader;
    private final ConfigurationNode rootNode;

    public WarzoneConfig(final HoconConfigurationLoader loader) throws IOException {
        this.loader = loader;
        this.rootNode = this.loader.load();
    }

    public boolean isDebugEnabled() {
        return this.rootNode.getNode(ConfigOption.SETTINGS, ConfigOption.DEBUG).getBoolean();
    }

    public int losePokemon() {
        // Should probably just throw errors instead of playing LISP
        // TODO: Care about it later
        return Math.max(0,
                Math.min(6,
                        this.rootNode.getNode(ConfigOption.SETTINGS, ConfigOption.LOSE_POKEMON).getInt()));
    }

    public Set<String> getWarzoneNames() {

        return this.rootNode.getNode(ConfigOption.WARZONE).getChildrenMap()
                .keySet()
                .stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

    }

    public void createWarzone(final String name, final WarzoneSelection region) {

        // Because serialization is a bitch
        ConfigurationNode warzone = this.rootNode.getNode(ConfigOption.WARZONE, name);

        warzone.getNode("firstPos").getNode("x").setValue(region.getFirstPos().getX());
        warzone.getNode("firstPos").getNode("z").setValue(region.getFirstPos().getZ());

        warzone.getNode("secondPos").getNode("x").setValue(region.getSecondPos().getX());
        warzone.getNode("secondPos").getNode("z").setValue(region.getSecondPos().getZ());

        this.saveConfig();

    }

    public void deleteWarzone(final String name) {
        this.rootNode.getNode(ConfigOption.WARZONE, name).setValue(null);
        this.saveConfig();
    }

    public DefinedWarzone getWarzone(final String name) {

        ConfigurationNode warzone = this.rootNode.getNode(ConfigOption.WARZONE, name);

        Point firstPos = new Point(
                warzone.getNode("firstPos", "x").getInt(),
                warzone.getNode("firstPos", "z").getInt()
        );

        Point secondPos = new Point(
                warzone.getNode("secondPos", "x").getInt(),
                warzone.getNode("secondPos", "z").getInt()
        );

        return new DefinedWarzone(name, new WarzoneSelection(firstPos, secondPos));

    }

    public List<DefinedWarzone> getWarzones() {
        return this.getWarzoneNames().stream().map(this::getWarzone).collect(Collectors.toList());
    }

    private void saveConfig() {
        try {
            this.loader.save(rootNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
