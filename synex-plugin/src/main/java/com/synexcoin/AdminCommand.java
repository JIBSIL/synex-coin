package com.synexcoin;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.p2p.solanaj.core.AssociatedTokenAccount;

public class AdminCommand implements CommandExecutor {

    SynexCoinPlugin plugin;

    public AdminCommand(SynexCoinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (
                plugin.hasPermission(sender, "synexcoin.admin")
        ) {
            if (args.length >= 1) {
                switch(args[0]) {
                    case "balance":
                        if (args.length == 1) {
                            try {
                                AssociatedTokenAccount ata = AssociatedTokenAccount.getATA(plugin.rpcClient, plugin.associatedTokenAddress);
                                sender.sendMessage(plugin.chatPrefix+"Server Balance is " + ata.getBalance(plugin.rpcClient, plugin.tokenMint) + " " + plugin.currencySymbol);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (args.length == 2) {
                            Player p = plugin.getServer().getPlayer(args[1]);
                            if (p == null) {
                                sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Player " + args[1] + " not found.");
                                break;
                            }
                            sender.sendMessage(plugin.chatPrefix+"" + p.getName() + "'s balance is " + plugin.db.getBalanceOfPlayer(p.getUniqueId()) + " " + plugin.currencySymbol);
                        } else {
                            sender.sendMessage(plugin.chatPrefix+"/synex:admin balance (player)");
                        }
                        break;
                    case "add":
                        if (args.length == 3) {
                            OfflinePlayer p = plugin.getServer().getOfflinePlayer(args[1]);

                            if (!plugin.db.playerExistsInBalanceTable(p.getUniqueId())) {
                                sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Player " + args[1] + " not found in db.");
                                break;
                            }

                            double amount = Double.parseDouble(args[2]);
                            if (amount > 0) {
                                plugin.db.addBalanceToPlayer(p.getUniqueId(), amount);
                                sender.sendMessage(plugin.chatPrefix+"Added " + amount + " to " + p.getName() + ". New balance is " + plugin.db.getBalanceOfPlayer(p.getUniqueId()) + " " + plugin.currencySymbol);
                                if (p.isOnline()) {
                                    plugin.getServer().getPlayer(p.getUniqueId()).sendMessage(plugin.chatPrefix+ChatColor.GOLD + "An admin sent you " + amount + " " + plugin.currencySymbol + "!");
                                }
                            } else {
                                sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Amount must be greater than zero " + plugin.currencySymbol + ".");
                            }
                        } else {
                            sender.sendMessage(plugin.chatPrefix+"/synex:admin add <player> <amount>");
                        }
                        break;
                    case "delete":
                        if (args.length == 2) {
                            OfflinePlayer p = plugin.getServer().getOfflinePlayer(args[1]);

                            if (!plugin.db.playerExistsInBalanceTable(p.getUniqueId())) {
                                sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Player " + args[1] + " not found in db.");
                                break;
                            }
                            plugin.db.removePlayerFromBalanceTable(p.getUniqueId());
                            sender.sendMessage(plugin.chatPrefix+ChatColor.YELLOW + "Removed " + p.getName() + " from the db.");
                        } else {
                            sender.sendMessage(plugin.chatPrefix+"/synex:admin delete <player>");
                        }
                        break;
                    case "subtract":
                        if (args.length == 3) {
                            OfflinePlayer p = plugin.getServer().getOfflinePlayer(args[1]);

                            if (!plugin.db.playerExistsInBalanceTable(p.getUniqueId())) {
                                sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Player " + args[1] + " not found in db.");
                                break;
                            }
                            double amount = Double.parseDouble(args[2]);
                            if (amount > 0) {
                                plugin.db.addBalanceToPlayer(p.getUniqueId(), -amount);
                                sender.sendMessage(plugin.chatPrefix+"Subtracted " + amount + " from " + p.getName() + ". New balance is " + plugin.db.getBalanceOfPlayer(p.getUniqueId()) + " " + plugin.currencySymbol);
                                if (p.isOnline()) {
                                    plugin.getServer().getPlayer(p.getUniqueId()).sendMessage(plugin.chatPrefix+ChatColor.GOLD + "An admin took " + amount + " "  + plugin.currencySymbol + "!");
                                }
                            } else {
                                sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Amount must be greater than zero " + plugin.currencySymbol + ".");
                            }
                        } else {
                            sender.sendMessage(plugin.chatPrefix+"/synex:admin subtract <player> <amount>");
                        }
                        break;
                    case "set":
                        if (args.length == 3) {
                            OfflinePlayer p = plugin.getServer().getOfflinePlayer(args[1]);

                            if (!plugin.db.playerExistsInBalanceTable(p.getUniqueId())) {
                                sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Player " + args[1] + " not found in db.");
                                break;
                            }

                            double amount = Double.parseDouble(args[2]);
                            plugin.db.setBalanceOfPlayer(p.getUniqueId(), amount);
                            sender.sendMessage(plugin.chatPrefix+"" + p.getName() + "'s new balance is " + plugin.db.getBalanceOfPlayer(p.getUniqueId()));
                            if (p.isOnline()) {
                                plugin.getServer().getPlayer(p.getUniqueId()).sendMessage(plugin.chatPrefix+ChatColor.GOLD + "An admin set your balance to " + amount + " " + plugin.currencySymbol +"!");
                            }
                        } else {
                            sender.sendMessage(plugin.chatPrefix+"/synex:admin set <player> <amount>");
                        }
                        break;
                    case "destroydb":
                        if (args.length == 2) {
                            if (args[1].equalsIgnoreCase("confirm")) {
                                plugin.db.deleteBalanceTable();
                                sender.sendMessage(plugin.chatPrefix+ChatColor.GREEN + "Database destroyed.");
                            }
                        } else {
                            sender.sendMessage(plugin.chatPrefix+ChatColor.YELLOW + "WARNING this will destroy all data in the database, please confirm.");
                            sender.sendMessage(plugin.chatPrefix+"/synex:admin destroydb confirm");
                        }
                        break;
                    case "help":
                        sender.sendMessage(plugin.chatPrefix+"/synex:admin [balance|add|delete|subtract|set|destroydb|reload]");
                        break;
                    case "reload":
                        plugin.reloadConfig();
                        sender.sendMessage(plugin.chatPrefix+"Config Reloaded");
                        break;
                }
            } else {
                sender.sendMessage(plugin.chatPrefix+"/synex:admin [balance|add|delete|subtract|set|destroydb|reload]");
            }
        } else {
            sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "You are not permitted to use that command.");
        }
        return true;
    }
}
