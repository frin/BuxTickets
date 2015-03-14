package us.frin.buxtickets;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;




import java.util.List;

import java.util.Map;

//import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import us.frin.buxtickets.BuxTicketsCommandExecutor;
import us.frin.buxtickets.BuxTicketsPlayerListener;

public class BuxTickets extends JavaPlugin {
	public Connection con;
	public Permission permission = null;
	public List<String> groups = null;
	public Map<String, Object> groupcolors = null;
	private NotifierThread notifier = null;
	public int notifierSeconds = 0;

	public void loadGroups() {
		this.groups = this.getConfig().getStringList("groups");
		this.groupcolors = this.getConfig().getConfigurationSection("group-colors").getValues(false);
	}
	
	public void initDatabase() {
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			stmt = this.con.prepareStatement("CREATE TABLE IF NOT EXISTS `tickets` ("
					+"`ticketid` int(10) unsigned NOT NULL AUTO_INCREMENT,"
					+"`uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,"
					+"`owner` varchar(100) COLLATE utf8_unicode_ci NOT NULL,"
					+"`content` text COLLATE utf8_unicode_ci NOT NULL,"
					+"`group` varchar(50) COLLATE utf8_unicode_ci DEFAULT NULL,"
					+"`world` varchar(100) COLLATE utf8_unicode_ci NOT NULL,"
					+"`x` decimal(12,4) NOT NULL,"
					+"`y` decimal(12,4) NOT NULL,"
					+"`z` decimal(12,4) NOT NULL,"
					+"`status` enum('open','closed') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'open',"
					+"`assigned_uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,"
					+"`created_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',"
					+"`updated_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',"
					+"PRIMARY KEY (`ticketid`),"
					+"KEY `uuid` (`uuid`),"
					+"KEY `status` (`status`),"
					+"KEY `assigned_uuid` (`assigned_uuid`)"
					+") ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1");
			stmt.executeUpdate();
			if (stmt != null) stmt.close();
			stmt = this.con.prepareStatement("CREATE TABLE IF NOT EXISTS `ticketactions` ("
					+"`ticketactionid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,"
					+"`ticketid` int(10) unsigned NOT NULL,"
					+"`uuid` varchar(36) COLLATE utf8_unicode_ci NOT NULL,"
					+"`type` enum('comment','status','assign','move') COLLATE utf8_unicode_ci NOT NULL,"
					+"`content` text COLLATE utf8_unicode_ci NOT NULL,"
					+"`world` varchar(100) COLLATE utf8_unicode_ci NOT NULL,"
					+"`x` decimal(12,4) NOT NULL,"
					+"`y` decimal(12,4) NOT NULL,"
					+"`z` decimal(12,4) NOT NULL,"
					+"`new_uuid` varchar(36) COLLATE utf8_unicode_ci DEFAULT NULL,"
					+"`seen` tinyint(1) unsigned NOT NULL DEFAULT '0',"
					+"`created_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',"
					+"PRIMARY KEY (`ticketactionid`)"
					+") ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1");
			stmt.executeUpdate();
			if (stmt != null) stmt.close();
		}
		catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		}
		finally {
			if (res != null) {
				try {
					res.close();
				}
				catch (SQLException e) {
					// Nothing
				}
				res = null;
			}
			
			if (stmt != null) {
				try {
					stmt.close();
				}
				catch (SQLException e) {
					// Nothing
				}
				stmt = null;
			}
		}
		return;
	}
	
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		this.loadGroups();
		// Attempt MySQL connection
		try {
			Class.forName("com.mysql.jdbc.Driver");
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			String user = getConfig().getString("user");
			String pass = getConfig().getString("pass");
			String name = getConfig().getString("name");
			String port = getConfig().getString("port");
			String address = getConfig().getString("address");
			con = DriverManager.getConnection("jdbc:mysql://" + address + ":" + port + "/" + name, user, pass);
			this.initDatabase();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		// Setup connections to other plugins
		if (!setupPermissions()) {
			getLogger().severe("Disabling BuxTickets - No Permissions Plugin Found");
			getServer().getPluginManager().disablePlugin(this);
		}
		
		// Initialize listener
		getServer().getPluginManager().registerEvents(new BuxTicketsPlayerListener(this), this);
		
		// Initialize command handler
		BuxTicketsCommandExecutor executor = new BuxTicketsCommandExecutor(this);
		this.getCommand("ticket").setExecutor(executor);
		
		startNotifier();

		getLogger().info("Plugin BuxTickets loaded successfully");
	}
	
	// Vault code
	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
    }
	
	@Override
	public void onDisable() {
		stopNotifier();
		// Clean up MySQL connection
		try {
			if (con != null && !con.isClosed()) {
				con.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void startNotifier() {
		notifierSeconds = 0;
		notifier = new NotifierThread(this);
		try {
			notifierSeconds = Integer.parseInt(this.getConfig().getString("notify-interval-seconds"));
		}
		catch (Exception e) {
			this.getLogger().warning("[BuxTickets] Error parsing option 'notify-interval-seconds'; must be an integer.");
			this.getLogger().warning("[BuxTickets] Using default value (300)");
		}
		if (notifierSeconds > 0) {
			notifier.setInterval(notifierSeconds);
			notifier.start();
		}
		else {
			this.getLogger().info("[BuxTickets] Notification Thread Disabled");
		}
	}

	private void stopNotifier() {
		if (notifier != null) {
			notifier.signalStop();
		}
	}
}
