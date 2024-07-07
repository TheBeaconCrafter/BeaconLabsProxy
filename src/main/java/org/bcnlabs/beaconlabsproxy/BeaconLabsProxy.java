package org.bcnlabs.beaconlabsproxy;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public final class BeaconLabsProxy extends Plugin {

    public String prefix = "[BeaconLabs]";
    private File file;
    private Configuration configuration;

    @Override
    public void onEnable() {
        getLogger().info("BeaconLabs Proxy system was enabled.");
        getProxy().getPluginManager().registerCommand(this, new PingCommand(this));

        file = new File(ProxyServer.getInstance().getPluginsFolder() + "/BeaconLabs/config.yml");

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

            configuration.set("Print_Out.1", "This configuration file works!");
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String printout = configuration.getString("Print_Out.1");
        getLogger().info(printout);
    }

    @Override
    public void onDisable() {
        getLogger().info("BeaconLabs Proxy system was disabled.");
    }

    public String getPrefix() {
        return prefix;
    }
}
