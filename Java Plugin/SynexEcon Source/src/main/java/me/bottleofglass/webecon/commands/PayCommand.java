package me.bottleofglass.webecon.commands;

import me.bottleofglass.webecon.WebEcon;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {
    private WebEcon plugin;

    public PayCommand(WebEcon plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!commandSender.hasPermission("webecon.pay")) {
            commandSender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage(plugin.getMessage("only_player"));
            return true;
        }
        Player player = (Player) commandSender;
        if(strings.length < 2) {
            commandSender.sendMessage(plugin.getMessage("pay_usage"));
            return true;
        }
        Player target = Bukkit.getPlayer(strings[0]);
        if(target == null) {
            commandSender.sendMessage(plugin.getMessage("invalid_player"));
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(strings[1]);
            if(!plugin.getEconomyManager().has(player,amount)) {
                player.sendMessage(plugin.getMessage("insufficient_funds").replace("%amount%", String.valueOf(amount)));
                return true;
            }
            plugin.getEconomyManager().withdrawPlayer(player, amount);
            player.sendMessage(plugin.getMessage("pay_msg").replace("%player%", target.getName()).replace("%amount%", String.valueOf(amount)));
            plugin.getEconomyManager().depositPlayer(target, amount);
            target.sendMessage(plugin.getMessage("receive_msg").replace("%player%", player.getName()).replace("%amount%", String.valueOf(amount)));

        } catch (NumberFormatException e) {
            commandSender.sendMessage(plugin.getMessage("invalid_amount"));
            return true;
        }
        return true;
    }
}
