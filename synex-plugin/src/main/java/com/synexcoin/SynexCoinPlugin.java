package com.synexcoin;

import com.synexcoin.util.Base58;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.Mint;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcClient;

import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class SynexCoinPlugin extends JavaPlugin implements Listener {

    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    static {
        NUMBER_FORMAT.setRoundingMode(RoundingMode.FLOOR);
        NUMBER_FORMAT.setGroupingUsed(true);
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    FileConfiguration config = getConfig();
    protected String rpcURL;
    protected RpcClient rpcClient;
    protected Account signer;
    protected PublicKey publicKey;
    protected PublicKey tokenMintAddress;
    protected PublicKey associatedTokenAddress;
    protected int requestLimitPerSecond;
    protected double minimumExport;
    protected double startingBalance = 0.0;
    protected Mint tokenMint;
    protected SQL db;
    boolean enabled;
    protected VaultIntegration vaultIntegration;
    protected String currencySymbol;

    protected String chatPrefix = ChatColor.GRAY + "["+ChatColor.RESET+"Synex"+ChatColor.GRAY + "]: " + ChatColor.RESET;

    public void loadSignerAccount() {
        if (config.contains("signer")) {
            this.signer = new Account(Base58.decode(Objects.requireNonNull(config.getString("signer"))));
        }
    }

    public void loadPublicKey() {
        String publicKey = config.getString("publicKey");
        try {
            assert publicKey != null;
            this.publicKey = new PublicKey(publicKey);
        } catch(IllegalArgumentException e) {
            getServer().getConsoleSender().sendMessage(this.chatPrefix + ChatColor.RED + " config field 'publicKey' invalid.");
        }

    }

    public void loadTokenMint() {
        String tokenMint = config.getString("tokenMint");
        try {
            assert tokenMint != null;
            this.tokenMintAddress = new PublicKey(tokenMint);
            this.tokenMint = Mint.getMint(this.rpcClient, this.tokenMintAddress);
            this.associatedTokenAddress = Mint.getAssociatedTokenAddress(this.tokenMintAddress, this.publicKey);
        } catch(IllegalArgumentException e) {
            getServer().getConsoleSender().sendMessage(this.chatPrefix + ChatColor.RED + "Config field 'tokenMint' invalid.");
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(this.chatPrefix + ChatColor.RED + "Failed to find associated token account, make sure you have some of the token in your account.");
            e.printStackTrace();
        }

    }

    public void loadSQL() {
        if (this.chatPrefix == null) {
            this.chatPrefix = ChatColor.GRAY + "["+ChatColor.RESET+"Synex"+ChatColor.GRAY + "]: " + ChatColor.RESET;
        }
        this.getServer().getConsoleSender().sendMessage(this.chatPrefix + ChatColor.GRAY + "Loading SQL");
        if (this.db != null) {
            this.db.disconnect();
        }
        this.db = new SQL(
                this
        );
        try {
            if (Objects.requireNonNull(config.getString("dbType")).equalsIgnoreCase("sqlite")) {
                this.db.connectSQLite(config.getString("sqliteLocation"));
            } else {
                this.db.connectSQL(config.getString("dbType"), config.getString("dbHost"),
                        config.getString("dbPort"),
                        config.getString("dbName"),
                        config.getString("dbUsername"),
                        config.getString("dbPassword"),
                        config.getBoolean("dbUseSSL"));
            }
            this.getServer().getConsoleSender().sendMessage(this.chatPrefix + ChatColor.GREEN + "Database connected!");
        } catch (SQLException e) {
            e.printStackTrace();
            this.getServer().getConsoleSender().sendMessage(this.chatPrefix + ChatColor.RED + "Database connection failed.");
        }
    }

    public void setupSQL() {
        this.loadSQL();
        if (this.db.isConnected())
            this.db.setupBalanceTable();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.tryHookVault("onLoad");
    }

    @Override
    public void onEnable () {

        this.reloadConfig();

        if (this.enabled) {

            Objects.requireNonNull(this.getCommand("admin")).setExecutor(new AdminCommand(this));
            Objects.requireNonNull(this.getCommand("balance")).setExecutor(new BalanceCommand(this));
            Objects.requireNonNull(this.getCommand("serverbalance")).setExecutor(new ServerBalanceCommand(this));
            Objects.requireNonNull(this.getCommand("db")).setExecutor(new DBCommand(this));
            Objects.requireNonNull(this.getCommand("export")).setExecutor(new ExportCommand(this));
            Objects.requireNonNull(this.getCommand("lbh")).setExecutor(new LbhCommand(this));
            Objects.requireNonNull(this.getCommand("send")).setExecutor(new SendCommand(this));

            getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.GREEN + "Synex Coin Enabled");
            this.tryHookVault("onEnable");
            getServer().getPluginManager().registerEvents(this, this);
        } else {
            getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.GREEN + "Synex Coin Not Enabled");
        }


    }

    @Override
    public void onDisable () {
        if (this.db != null) {
            if (this.db.isConnected()) {
                this.db.disconnect();
            }
        }
        getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.RED + "Synex Coin Disabled");

    }

    // This method checks for incoming players and sends them a message
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (this.enabled) {
//            player.sendMessage(plugin.chatPrefix+"This server has Synex Coin support!");
            if(this.db.isConnected()) {
                this.db.addPlayerToBalanceTable(player);
            }
        }
    }

    public enum TELLRAWCOLOR {
        red,
        dark_red,
        yellow,
        gold,
        green,
        dark_green,
        blue,
        dark_blue,
        aqua,
        dark_aqua,
        light_purple,
        dark_purple,
        gray,
        dark_gray,
        white,
        black
    }

    public void sendURLToPlayer(Player player, String message, String url, TELLRAWCOLOR color) {
        this.getServer().dispatchCommand(
                this.getServer().getConsoleSender(),
                "tellraw " + player.getName() +
                        " {\"text\":\"" + message + "\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" +
                        url + "\"},\"color\":\""+color.name()+"\",\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\""+url+"\"]}}");
    }

    public void sendCopyableTextToPlayer(Player player, String message, String toCopy, TELLRAWCOLOR color) {
        this.getServer().dispatchCommand(
                this.getServer().getConsoleSender(),
                "tellraw " + player.getName() +
                        " {\"text\":\"" + message + "\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" +
                        toCopy + "\"},\"color\":\""+color.name()+"\",\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Copy to clipboard\"]}}");
    }

    public Timestamp getNow() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = formatter.format((new Date(System.currentTimeMillis())));
        return Timestamp.valueOf(now);
    }

    public boolean shouldRateLimit(Player player) {

        Timestamp lastRequest = Timestamp.valueOf(this.db.getLastRequestTimestamp(player.getUniqueId()));
        Timestamp now = this.getNow();
        long milliseconds = now.getTime() - lastRequest.getTime();
        int seconds = (int) milliseconds / 1000;
        return (seconds < this.requestLimitPerSecond);
    }

    public void tryHookVault(String where) {
        if (this.config.getBoolean("vaultEnabled")) {
            if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
                if (this.vaultIntegration == null) {
                    this.vaultIntegration = new VaultIntegration(this);
                }
            } else {
                this.getServer().getLogger().warning("[WARNING][Synex] Vault not found during " + where + ".");
            }
        }
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        if (this.config.getBoolean("vaultEnabled")) {
            if (this.vaultIntegration != null) {
                if (this.vaultIntegration.getPermissions() != null) {
                    if (this.vaultIntegration.getPermissions().isEnabled()) {
                        return vaultIntegration.getPermissions().has(sender, permission);
                    }
                }
            }
        }
        return sender.hasPermission(permission);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        saveDefaultConfig();
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        this.enabled = config.getBoolean("enabled");

        if (this.enabled) {

//            this.tryHookVault("reload");


            if (config.contains("rpcURL")) {
                String rpcURL = config.getString("rpcURL");
                boolean loadClient = true;
                if (this.rpcURL == null) {
                    this.rpcURL = rpcURL;
                } else if (this.rpcURL.equalsIgnoreCase(rpcURL) && this.rpcClient == null) {
                    loadClient = false;
                }
                if (loadClient && !Objects.requireNonNull(rpcURL).equals("")) {
                    this.rpcClient = new RpcClient(rpcURL);
                } else {
                    this.rpcClient = new RpcClient(Cluster.MAINNET);
                    getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.RED + "config field 'rpcURL' is blank. Defaulting to \"https://api.mainnet-beta.solana.com\".");
                }
            }


            if (config.contains("signer")) {
                boolean loadSigner = true;
                if (this.signer != null) {
                    if (!Base58.encode(this.signer.getSecretKey()).equalsIgnoreCase(config.getString("signer"))) {
                        getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.YELLOW + "'signer' config field changed!");
                    } else {
                        loadSigner = false;
                    }
                }
                if (loadSigner) {
                    this.loadSignerAccount();
                }
            } else {
                getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.RED + "config field 'signer' missing! Won't be able to send transactions.");
            }

            if (config.contains("publicKey")) {
                boolean loadPublicKey = true;
                if (this.publicKey != null) {
                    if (!this.publicKey.toBase58().equalsIgnoreCase(config.getString("publicKey"))) {
                        getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.YELLOW + "'publicKey' config field changed!");
                    } else {
                        loadPublicKey = false;
                    }
                }
                if (loadPublicKey) {
                    this.loadPublicKey();
                }
            } else {
                getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.RED + "config field 'publicKey' missing. Won't be able to send transactions.");
            }

            if (config.contains("tokenMint")) {
                boolean loadTokenMint = true;
                if (this.tokenMint != null) {
                    if (!this.tokenMint.address.toBase58().equalsIgnoreCase(config.getString("tokenMint"))) {
                        getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.YELLOW + "'tokenMint' config field changed!");
                    } else {
                        loadTokenMint = false;
                    }
                }
                if (loadTokenMint) {
                    this.loadTokenMint();
                }
            } else {
                getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.RED + "config field 'tokenMint' missing. Won't be able to send transactions.");
            }

            if (config.contains("minimumExport")) {
                this.minimumExport = config.getDouble("minimumExport");
            }

            if (config.contains("requestLimitPerSecond")) {
                this.requestLimitPerSecond = config.getInt("requestLimitPerSecond");
            }

            if(config.contains("startingBalance")) {
                this.startingBalance = config.getDouble("startingBalance");
            }

            if (config.contains("currencySymbol")) {
                boolean loadCurrencySymbol = true;
                if (this.currencySymbol != null) {
                    if (!this.currencySymbol.equals("")) {
                        if (!this.currencySymbol.equalsIgnoreCase(config.getString("currencySymbol"))) {
                            getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.YELLOW + "'currencySymbol' config field changed!");
                        } else {
                            loadCurrencySymbol = false;
                        }
                    }
                }
                if (loadCurrencySymbol) {
                    this.currencySymbol = config.getString("currencySymbol");
                }

            }
            this.setupSQL();
        } else {
            if (this.db != null) {
                if (this.db.isConnected()) {
                    this.db.disconnect();
                }
                getServer().getConsoleSender().sendMessage(this.chatPrefix+ChatColor.RED + "Synex Coin Disabled via Config Reload");
            }
        }


    }



}
