package com.synexcoin;

import com.synexcoin.SynexCoinPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.p2p.solanaj.rpc.RpcException;

public class LbhCommand implements CommandExecutor {

    SynexCoinPlugin plugin;

    public LbhCommand(SynexCoinPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (
                plugin.hasPermission(sender, "synexcoin.lbh")
        ) {
            try {
                sender.sendMessage(plugin.chatPrefix+ChatColor.GOLD + "Latest Blockhash " + plugin.rpcClient.getApi().getRecentBlockhash());
            } catch (RpcException e) {
                e.printStackTrace();
            }
        } else {
            sender.sendMessage(plugin.chatPrefix+ChatColor.RED + "You are not permitted to use that command.");
        }
        return true;
    }
}
