package me.bottleofglass.webecon;

import me.bottleofglass.webecon.commands.BalanceCommand;
import me.bottleofglass.webecon.commands.EconomyCommand;
import me.bottleofglass.webecon.commands.PayCommand;
import me.bottleofglass.webecon.managers.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class WebEcon extends JavaPlugin {
    private EconomyManager economyManager;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        economyManager = new EconomyManager(this);
        Bukkit.getServicesManager().register(Economy.class, economyManager, this, ServicePriority.Normal);
        registerCommands();
    }
    private void registerCommands() {
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("economy").setExecutor(new EconomyCommand(this));
        getCommand("balance").setExecutor(new BalanceCommand(this));
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    public String getMessage(String s) { return ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + s)); }

}
