package me.bottleofglass.webecon.commands;

import me.bottleofglass.webecon.WebEcon;
import me.bottleofglass.webecon.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {
    private WebEcon plugin;
    private EconomyManager economyManager;

    public BalanceCommand(WebEcon plugin) {
        this.plugin = plugin;
        economyManager = plugin.getEconomyManager();
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length == 0) {
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage(plugin.getMessage("only_player"));
                return true;
            }
            Player player = (Player) commandSender;
            double amount = economyManager.getBalance(player);
            if(amount == Double.MAX_VALUE)
                player.sendMessage(ChatColor.DARK_RED + "COULDN'T GET BALANCE OF " + player.getName());
            else
                player.sendMessage(plugin.getMessage("balance_msg").replace("%amount%", String.valueOf(amount)));
            return  true;
        }
        Player target = Bukkit.getPlayer(strings[0]);
        if(target == null)  {
            commandSender.sendMessage(plugin.getMessage("invalid_player"));
            return true;
        }
        double amount = economyManager.getBalance(target);
        if(amount == Double.MAX_VALUE)
            commandSender.sendMessage(ChatColor.DARK_RED + "COULDN'T GET BALANCE OF " + strings[0]);
        else
            commandSender.sendMessage(plugin.getMessage("balance_msg").replace("%amount%", String.valueOf(amount)));
        return true;
    }
}
