package me.bottleofglass.webecon.managers;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import me.bottleofglass.webecon.WebEcon;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;

public class EconomyManager implements Economy, Listener {
    private final WebEcon plugin;
    private final double STARTING_BALANCE;
    private final String API_URL;
    private final Map<UUID, Double> balanceStorage;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    static {
        NUMBER_FORMAT.setRoundingMode(RoundingMode.FLOOR);
        NUMBER_FORMAT.setGroupingUsed(true);
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    public EconomyManager(WebEcon plugin) {
        this.plugin = plugin;
        balanceStorage = new HashMap<>();
        STARTING_BALANCE = plugin.getConfig().getDouble("starting-balance");
        API_URL = plugin.getConfig().getString("api_url");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for(Player player : Bukkit.getOnlinePlayers())
            loadPlayer(player);
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent evt) {
        loadPlayer(evt.getPlayer());
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent evt) { balanceStorage.remove(evt.getPlayer().getUniqueId());}
    private void loadPlayer(Player player)  {
        double balance = getBalanceWeb(player);
        if(balance == Double.MAX_VALUE) {
            balance = STARTING_BALANCE;
            if(!setBalanceWeb(player, balance).isSuccess())
                plugin.getLogger().severe("Couldn't save " + player.getName() + "'s initial balance to web! CHECK THIS IMMEDIATELY");
        }
        balanceStorage.put(player.getUniqueId(), balance);
    }
    public void setBalance(OfflinePlayer p, double value) {
        if(isNegative(value))
            return;
        WebResponse response = setBalanceWeb(p, value);
        if(response.isSuccess() && p.isOnline())
            balanceStorage.replace(p.getUniqueId(), response.getBalance());
    }
    private double getBalanceWeb(OfflinePlayer p) {
        HttpResponse<String> response = Unirest.get(API_URL + "/balance/" + p.getUniqueId()).asString();
        if(response.getBody().split(",")[0].equals("Success"))
            return Double.parseDouble(response.getBody().split(",")[1]);
        return Double.MAX_VALUE;
    }
    private WebResponse setBalanceWeb(OfflinePlayer p, double balance) {
        HttpResponse<String> response = Unirest.get(API_URL + "/setBalance/" + p.getUniqueId() + "/" + balance).asString();
        return new WebResponse(Double.parseDouble(response.getBody().split(",")[1].split(":")[1].trim()),response.getBody().split(",")[0].equals("Success"));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "Web-Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double v) {
        return NUMBER_FORMAT.format(v);
    }

    @Override
    public String currencyNamePlural() {
        return "USD";
    }

    @Override
    public String currencyNameSingular() {
        return "$";
    }

    @Override
    public boolean hasAccount(String s) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(s);
        return hasAccount(offlinePlayer);
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer) {
        if(offlinePlayer.isOnline())
            return balanceStorage.containsKey(offlinePlayer.getUniqueId());
        else
            return getBalanceWeb(offlinePlayer) != Double.MAX_VALUE;
    }

    @Override
    public boolean hasAccount(String s, String s1) {
        return hasAccount(s);
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
        return hasAccount(offlinePlayer);
    }

    @Override
    public double getBalance(String s) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(s);
        return getBalance(offlinePlayer);

    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer) {
        if(offlinePlayer.isOnline())
            return balanceStorage.get(offlinePlayer.getUniqueId());
        else {
            double balance = getBalanceWeb(offlinePlayer);
            if(balance == Double.MAX_VALUE)
                return 0;
            else
                return balance;
        }
    }

    @Override
    public double getBalance(String s, String s1) {
        return getBalance(s);
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer, String s) {
        return getBalance(offlinePlayer);
    }

    @Override
    public boolean has(String s, double v) {
        has(Bukkit.getOfflinePlayer(s), v);
        return false;
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, double v) {
        if(offlinePlayer.isOnline())
            return balanceStorage.get(offlinePlayer.getUniqueId()) >= v;
        else {
            double balance = getBalanceWeb(offlinePlayer);
            if(balance == Double.MAX_VALUE)
                return false;
            else
                return balance >= v;
        }
    }

    @Override
    public boolean has(String s, String s1, double v) {
        return has(Bukkit.getOfflinePlayer(s), v);
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, String s, double v) {
        return has(offlinePlayer, v);
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, double v) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(s), v);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double v) {
        double balance;
        if(offlinePlayer.isOnline())
            balance = balanceStorage.get(offlinePlayer.getUniqueId());
        else
            balance = getBalance(offlinePlayer);
        if(balance < v)
            return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient balance in players account!");
        WebResponse response = setBalanceWeb(offlinePlayer, balance - v);
        if(!response.isSuccess()) {
            plugin.getLogger().severe("Unable to update " + offlinePlayer.getName() + "'s balance on the web!");
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Unable to update balance on the web!");
        }
        if(offlinePlayer.isOnline())
            balanceStorage.replace(offlinePlayer.getUniqueId(), balanceStorage.get(offlinePlayer.getUniqueId()) - v);
        return new EconomyResponse(v,response.getBalance(), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, String s1, double v) {
        return withdrawPlayer(s,v);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double v) {
        return withdrawPlayer(offlinePlayer,v);
    }

    @Override
    public EconomyResponse depositPlayer(String s, double v) {
        return depositPlayer(Bukkit.getOfflinePlayer(s),v);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double v) {
        double balance;
        if(offlinePlayer.isOnline())
            balance = balanceStorage.get(offlinePlayer.getUniqueId());
        else
            balance = getBalance(offlinePlayer);
        WebResponse response = setBalanceWeb(offlinePlayer, balance + v);
        if(!response.isSuccess()) {
            plugin.getLogger().severe("Unable to update " + offlinePlayer.getName() + "'s balance on the web!");
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Unable to update balance on the web!");
        }
        if(offlinePlayer.isOnline())
            balanceStorage.replace(offlinePlayer.getUniqueId(), balanceStorage.get(offlinePlayer.getUniqueId()) + v);
        return new EconomyResponse(v,response.getBalance(), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String s, String s1, double v) {
        return depositPlayer(Bukkit.getOfflinePlayer(s), v);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double v) {
        return depositPlayer(offlinePlayer,v);
    }

    @Override
    public EconomyResponse createBank(String s, String s1) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse deleteBank(String s) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse bankBalance(String s) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse bankHas(String s, double v) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse bankWithdraw(String s, double v) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse bankDeposit(String s, double v) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse isBankOwner(String s, String s1) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse isBankMember(String s, String s1) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0 , 0,EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Web Economy does NOT support Banks");
    }

    @Override
    public List<String> getBanks() {
        return null;
    }

    @Override
    public boolean createPlayerAccount(String s) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(s));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
        return setBalanceWeb(offlinePlayer, STARTING_BALANCE).isSuccess();
    }

    @Override
    public boolean createPlayerAccount(String s, String s1) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(s));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
        return createPlayerAccount(offlinePlayer);
    }
    public class WebResponse {
        private double balance;
        private boolean success;
        public WebResponse(double balance, boolean success) {
            this.balance = balance;
            this.success = success;
        }

        public double getBalance() {
            return balance;
        }

        public boolean isSuccess() {
            return success;
        }
    }
    private boolean isNegative(double amount) {
        return Math.signum(amount) == -1;
    }
}
