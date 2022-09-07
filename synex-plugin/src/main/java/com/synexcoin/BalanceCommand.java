package com.synexcoin;

import com.synexcoin.SynexCoinPlugin;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.p2p.solanaj.core.AssociatedTokenAccount;
import org.p2p.solanaj.core.Mint;

import static com.synexcoin.SynexCoinPlugin.NUMBER_FORMAT;

public class BalanceCommand implements CommandExecutor {

    SynexCoinPlugin plugin;

    public BalanceCommand(SynexCoinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (
                plugin.hasPermission(sender, "synexcoin.balance")
        ) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player sPlayer = (Player) sender;
                    sender.sendMessage(plugin.chatPrefix + ChatColor.GREEN + "Your balance is " + ChatColor.YELLOW + NUMBER_FORMAT.format(plugin.db.getBalanceOfPlayer(sPlayer.getUniqueId())) + " " + ChatColor.GREEN + plugin.currencySymbol);
                } else {
                    try {
                        AssociatedTokenAccount ata = AssociatedTokenAccount.getATA(plugin.rpcClient, plugin.associatedTokenAddress);
                        sender.sendMessage(plugin.chatPrefix + ChatColor.GREEN + "Server Balance is " + ChatColor.YELLOW + NUMBER_FORMAT.format(ata.getBalance(plugin.rpcClient, plugin.tokenMint)) + " " + ChatColor.GREEN + plugin.currencySymbol);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                sender.sendMessage(plugin.chatPrefix+"/synex:balance");
            }
        } else {
            sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "You are not permitted to use that command.");
        }
        return true;
    }
}
