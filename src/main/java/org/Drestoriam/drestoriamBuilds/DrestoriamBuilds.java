package org.Drestoriam.drestoriamBuilds;

import net.md_5.bungee.api.ChatColor;
import org.Drestoriam.drestoriamBuilds.Classes.BuilderTrait;
import org.Drestoriam.drestoriamBuilds.Commands.BP;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class DrestoriamBuilds extends JavaPlugin {

    public static String tag = ChatColor.DARK_BLUE + "[" + ChatColor.DARK_AQUA + "DrestoriamBuilds" + ChatColor.DARK_BLUE + "] ";


    @Override
    public void onEnable() {
        // Plugin startup logic

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        getCommand("bp").setExecutor(new BP(this));

        if(getServer().getPluginManager().getPlugin("Citizens") == null || !getServer().getPluginManager().getPlugin("Citizens").isEnabled()){

            getLogger().log(Level.SEVERE, "Citizens not found or not enabled.");

        } else {
            //Registering Trait w/ Citizens
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(BuilderTrait.class));

        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
