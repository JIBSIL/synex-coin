package com.synexcoin;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.p2p.solanaj.core.AssociatedTokenAccount;

import static com.synexcoin.SynexCoinPlugin.NUMBER_FORMAT;

public class ServerBalanceCommand implements CommandExecutor {

    SynexCoinPlugin plugin;

    public ServerBalanceCommand(SynexCoinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.hasPermission(sender, "synexcoin.serverbalance")) {
            if (sender instanceof Player pSender) {
                if (plugin.shouldRateLimit(pSender)) {
                    sender.sendMessage(plugin.chatPrefix+ChatColor.GRAY + "Command failed, rate limited.");
                    return true;
                } else {
                    AssociatedTokenAccount ata = AssociatedTokenAccount.getATA(plugin.rpcClient, plugin.associatedTokenAddress);
                    sender.sendMessage(plugin.chatPrefix+ChatColor.AQUA +"Server Balance is " + ChatColor.YELLOW + NUMBER_FORMAT.format(ata.getBalance(plugin.rpcClient, plugin.tokenMint)) + " " + ChatColor.AQUA + plugin.currencySymbol);
                    boolean updated = this.plugin.db.setLastRequestTimestamp(pSender.getUniqueId(), plugin.getNow().toString());
                    if (!updated) {
                        plugin.getServer().getConsoleSender().sendMessage(plugin.chatPrefix+"Failed to update last timestamp for " + pSender.getUniqueId());
                    }
                }
            } else {
                AssociatedTokenAccount ata = AssociatedTokenAccount.getATA(plugin.rpcClient, plugin.associatedTokenAddress);
                sender.sendMessage(plugin.chatPrefix+ChatColor.AQUA +"Server Balance is " + ChatColor.YELLOW + NUMBER_FORMAT.format(ata.getBalance(plugin.rpcClient, plugin.tokenMint)) + " " + ChatColor.AQUA + plugin.currencySymbol);
            }

        } else {
            sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "You are not permitted to use that command.");
        }
        return true;
    }
}
