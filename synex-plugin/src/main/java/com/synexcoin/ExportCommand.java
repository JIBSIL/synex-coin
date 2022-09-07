package com.synexcoin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.p2p.solanaj.core.*;
import org.p2p.solanaj.rpc.RpcException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.synexcoin.SynexCoinPlugin.NUMBER_FORMAT;

public class ExportCommand implements CommandExecutor {

    SynexCoinPlugin plugin;

    public ExportCommand(SynexCoinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (
                plugin.hasPermission(sender, "synexcoin.export")
        ) {
            if (args.length >= 2) {
                PublicKey fromKey = plugin.publicKey;
                PublicKey toKey;
                try {
                    toKey = new PublicKey(args[0]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "<address> could not be parsed as a valid PublicKey.");
                    return true;
                }
                double amount = 0;
                try {
                    amount = Double.parseDouble(args[1]);
                } catch (NullPointerException | NumberFormatException nullPointerException) {
                    sender.sendMessage(plugin.chatPrefix+ChatColor.RED+"Failed to parse <amount>. Please make sure your sending a valid number.");
                    return true;
                }
                if (plugin.minimumExport > 0) {
                    if (amount < plugin.minimumExport) {
                        sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Minimum export amount is " + ChatColor.YELLOW + plugin.minimumExport + " " + ChatColor.RED + plugin.currencySymbol + ".");
                        return true;
                    }
                } else if (amount <= 0) {
                    sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "<amount> must be greater than" + ChatColor.YELLOW + " 0 " + ChatColor.RED + plugin.currencySymbol + ".");
                    return true;
                }

                if (sender instanceof Player pSender) {
                    double playerBalance = plugin.db.getBalanceOfPlayer(pSender.getUniqueId());
                    if (playerBalance >= amount) {
                        try {
                            if (plugin.shouldRateLimit(pSender)) {
                                sender.sendMessage(plugin.chatPrefix+ChatColor.GRAY + "Command failed, rate limited.");
                                return true;
                            } else {
                                AssociatedTokenAccount ata = AssociatedTokenAccount.getATA(plugin.rpcClient, plugin.associatedTokenAddress);
                                BigDecimal serverBalance = ata.getBalance(plugin.rpcClient, plugin.tokenMint);
                                if (serverBalance.subtract(new BigDecimal(amount)).doubleValue() < 0) {
                                    sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Server does not have enough supply of " + plugin.currencySymbol + ". Contact admin.");
                                    return true;
                                } else {
                                    plugin.db.addBalanceToPlayer(pSender.getUniqueId(), -amount);
                                    sender.sendMessage(plugin.chatPrefix+ChatColor.AQUA + "The server has " + ChatColor.YELLOW + NUMBER_FORMAT.format(serverBalance) + " " + ChatColor.AQUA + plugin.currencySymbol + " available.");
                                }
                                if (args.length == 3) {
                                    if (args[2].equals("confirm")) {
                                        PublicKey fromTokenPublicKey = plugin.associatedTokenAddress;
                                        try {
                                            PublicKey toTokenPublicKey = Mint.getAssociatedTokenAddress(plugin.tokenMint.address, toKey);
                                            AssociatedTokenAccount toATA = AssociatedTokenAccount.getATA(plugin.rpcClient, toTokenPublicKey);
                                            if (!toATA.isInitialized) {
                                                plugin.sendCopyableTextToPlayer(pSender, "Please create a Token account for token: " + this.plugin.tokenMintAddress.toBase58(), this.plugin.tokenMintAddress.toBase58(), SynexCoinPlugin.TELLRAWCOLOR.aqua);
                                                plugin.sendURLToPlayer(pSender, "This can be done by swapping on Jupiter Aggregator", "https://jup.ag/", SynexCoinPlugin.TELLRAWCOLOR.yellow);
                                                return true;
                                            }
                                            try {
                                                Transaction tx = new Transaction();
                                                TransactionInstruction instruction = Mint.transferSPL(plugin.tokenMint, fromTokenPublicKey, toTokenPublicKey, fromKey, amount);
                                                tx.addInstruction(instruction);
                                                try {
                                                    List<Account> signers = new ArrayList<>();
                                                    signers.add(plugin.signer);
                                                    String signature = plugin.rpcClient.getApi().sendTransaction(tx, signers);
                                                    sender.sendMessage(plugin.chatPrefix+ChatColor.GREEN + "Transaction sent");
                                                    plugin.sendURLToPlayer(pSender, "Check the Transaction Status", "https://solscan.io/tx/" + signature, SynexCoinPlugin.TELLRAWCOLOR.yellow);
                                                    new RetryExport(this.plugin, pSender, amount, tx, signature);
                                                } catch (RpcException e) {
                                                    sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Error while sending transaction, contact admin.");
                                                    e.printStackTrace();
                                                    return true;
                                                }
                                            } catch (Exception e) {
                                                sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Error while trying to build transaction, contact admin.");
                                                e.printStackTrace();
                                                return true;
                                            }
                                        } catch (Exception e) {
                                            plugin.sendCopyableTextToPlayer(pSender, "Please create an ATA for Token: " + this.plugin.tokenMintAddress.toBase58(), this.plugin.tokenMintAddress.toBase58(), SynexCoinPlugin.TELLRAWCOLOR.aqua);
                                            plugin.sendURLToPlayer(pSender, "This can be done by swapping on Jupiter Aggregator", "https://jup.ag/", SynexCoinPlugin.TELLRAWCOLOR.yellow);
                                            return true;
                                        }
                                    } else {
                                        sender.sendMessage(plugin.chatPrefix+ChatColor.AQUA + "Please confirm you want to send " + ChatColor.YELLOW + amount + " " + ChatColor.AQUA + plugin.currencySymbol + " to " + ChatColor.GOLD + toKey.toBase58() + ChatColor.AQUA +  ".");
                                        sender.sendMessage(plugin.chatPrefix+"/synex:export <address> <amount> confirm");
                                        return true;
                                    }
                                } else {
                                    sender.sendMessage(plugin.chatPrefix+ChatColor.AQUA + "Please confirm you want to send " + ChatColor.YELLOW + amount + " " + ChatColor.AQUA + plugin.currencySymbol + " to " + ChatColor.GOLD + toKey.toBase58() + ChatColor.AQUA + ".");
                                    sender.sendMessage(plugin.chatPrefix+"/synex:export <address> <amount> confirm");
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Error while trying to build transaction, contact admin.");
                            e.printStackTrace();
                            return true;
                        }
                    } else {
                        sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Insufficient balance.");
                    }
                } else {
                    sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "Export not available to non-players.");
                }

            } else {
                sender.sendMessage(plugin.chatPrefix+"/synex:export <address> <amount> confirm");
            }
        } else {
            sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "You are not permitted to use that command.");
        }
        return true;
    }
}
