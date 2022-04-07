package me.bottleofglass.webecon.commands;

import me.bottleofglass.webecon.WebEcon;
import me.bottleofglass.webecon.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EconomyCommand implements CommandExecutor {
    private WebEcon plugin;
    private EconomyManager economyManager;

    public EconomyCommand(WebEcon plugin) {
        this.plugin = plugin;
        economyManager = plugin.getEconomyManager();
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.hasPermission("webecon.eco")) {
            commandSender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }
        if(strings.length == 1 && strings[0].equalsIgnoreCase("help")) {
            commandSender.sendMessage(plugin.getMessage(""));
        }
        if(strings.length < 3) {
            commandSender.sendMessage(plugin.getMessage("eco_usage"));
            return true;
        }
        Player target = Bukkit.getPlayer(strings[1]);
        if(target == null) {
            commandSender.sendMessage(plugin.getMessage("invalid_player"));
            return true;
        }
        double amount = 0;
        try {
            amount = Double.parseDouble(strings[2]);
        } catch (NumberFormatException e) {
            commandSender.sendMessage(plugin.getMessage("invalid_amount"));
            return true;
        }
        if ("set".equalsIgnoreCase(strings[0])) {
            economyManager.setBalance(target, amount);
            commandSender.sendMessage(plugin.getMessage("set_msg").replace("%player%", target.getName()).replace("%amount%", String.valueOf(amount)));
            target.sendMessage(plugin.getMessage("set_receiver_msg").replace("%amount%", String.valueOf(amount)));
        }
        return true;
    }
}
