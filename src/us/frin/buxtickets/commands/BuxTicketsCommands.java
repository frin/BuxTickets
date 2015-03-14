package us.frin.buxtickets.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.Statement;

import us.frin.buxtickets.BuxTickets;
import us.frin.buxtickets.PlayerRecord;
import us.frin.buxtickets.TicketActionRecord;
import us.frin.buxtickets.TicketRecord;

public class BuxTicketsCommands {
	BuxTickets plugin;
	
	public BuxTicketsCommands(BuxTickets plugin) {
		this.plugin = plugin;
	}
	
	public String getTitle(String[] args, Integer index) {
		String title = "";
        while (index < args.length) {
        	title = title.concat(" " + args[index]);
            index++;
        }
        if (title.length() > 0) {
        	title = title.substring(1);
        }
        return title;
	}
	
	public void performWarp(Player player, String[] args) {
		try {
			int ticketid = Integer.parseInt(args[1]);
			TicketRecord record = this.getTicket(ticketid);
			if (record == null || !record.status.equalsIgnoreCase("open")) {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No open ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " found.");
				return;
			}
			
			if (player.hasPermission("buxtickets.admin")) {
				if (args.length == 3) {
					// Check action id
					int ticketactionid = Integer.parseInt(args[2]);
					boolean teleported = false;
					for (TicketActionRecord action : record.actions) {
						if (action.ticketactionid == ticketactionid) {
							for (World w: Bukkit.getServer().getWorlds()) {
								if (w.getName().equalsIgnoreCase(action.world)) {
									if (player.teleport(new Location(w, action.x, action.y, action.z))) {
										player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.GRAY+"Teleporting you to where the action on ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was created.");
										teleported = true;
									}
									break;
								}
							}
						}
					}
					if (!teleported) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Teleport to ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " failed.");
						return;
					}
				}
				else {
					// Teleport to ticket creation
					boolean teleported = false;
					for (World w: Bukkit.getServer().getWorlds()) {
						if (w.getName().equalsIgnoreCase(record.world)) {
							if (player.teleport(new Location(w, record.x, record.y, record.z))) {
								player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.GRAY+"Teleporting you to where the ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was opened.");
								teleported = true;
							}
							break;
						}
					}
					if (!teleported) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Teleport to ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " failed.");
						return;
					}
				}
			}
			else {
				this.plugin.getLogger().warning("Player " + player.getName() + " was denied warping to ticket #" + ticketid);
			}
		}
		catch (NumberFormatException e) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong ticket number. See "+ChatColor.RED+"/ticket");
		}
		finally {
		}
	}
	
	public void performNew(Player player, String group, String[] args) {
		int index = 1;
		if (group != null) index++;
		String title = this.getTitle(args, index);
		
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		if (title.length() == 0) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not!! enough parameters. See "+ChatColor.RED+"/ticket");
			return;
		}
		
		if (group == null) group = "admin";

		try {
			stmt = this.plugin.con.prepareStatement("INSERT INTO `tickets` (`uuid`, `owner`, `content`, `group`, `world`, `x`, `y`, `z`, `status`, `created_at`, `updated_at`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'open', NOW(), NOW())", Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, player.getUniqueId().toString());
			stmt.setString(2, player.getName());
			stmt.setString(3, title);
			stmt.setString(4, group);
			stmt.setString(5, player.getWorld().getName());
			stmt.setDouble(6, player.getLocation().getX());
			stmt.setDouble(7, player.getLocation().getY());
			stmt.setDouble(8, player.getLocation().getZ());
			stmt.executeUpdate();
			
			int ticketid = 0;
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()){
			    ticketid = rs.getInt(1);
			}
			
			if (stmt != null) stmt.close();
			
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Thank you, your ticket is " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " (use /ticket to manage it).");
			ArrayList<String> skip = new ArrayList<String>(1);
			skip.add(player.getUniqueId().toString());
			
			this.notifyModerators(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " opened by " + player.getName() + ": " + title, skip);
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
	}
	
	public TicketRecord getTicket(int ticketid) {
		PreparedStatement stmt = null;
		ResultSet res = null;
		TicketRecord tr = null;
		try {
			stmt = this.plugin.con.prepareStatement("SELECT * FROM `tickets` WHERE ticketid = ?");
			stmt.setInt(1, ticketid);
			res = stmt.executeQuery();
			
			if (res.first()) {
				tr = new TicketRecord(ticketid, res.getString("content"), res.getString("uuid"), res.getString("owner"), res.getString("group"), res.getString("world"), res.getDouble("x"), res.getDouble("y"), res.getDouble("z"), res.getString("status"), res.getString("assigned_uuid"));
				if (stmt != null) stmt.close();
				if (res != null) res.close();
				
				stmt = this.plugin.con.prepareStatement("SELECT * FROM `ticketactions` WHERE ticketid = ?");
				stmt.setInt(1, ticketid);
				res = stmt.executeQuery();
				
				ArrayList<TicketActionRecord> tas = new ArrayList<TicketActionRecord>(10);
				
				while (res.next()) {
					TicketActionRecord tar = new TicketActionRecord(res.getInt("ticketid"), res.getInt("ticketactionid"), res.getString("uuid"), res.getString("type"), res.getString("content"), res.getString("world"), res.getDouble("x"), res.getDouble("y"), res.getDouble("z"), res.getString("new_uuid"), res.getInt("seen"));
					tas.add(tar);
				}
				tr.actions = tas;
			}
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
		return tr;
	}
	
	public void performComment(Player player, String[] args) {
		int index = 2;
		String title = this.getTitle(args, index);
		
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		if (title.length() == 0) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
			return;
		}
		
		try {
			int ticketid = Integer.parseInt(args[1]);
			TicketRecord record = this.getTicket(ticketid);
			if (record == null || !record.status.equalsIgnoreCase("open")) {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No open ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " found.");
				return;
			}
			
			if (record.isOwnedBy(player) || player.hasPermission("buxtickets.admin") || record.canAccess(player)) {
				stmt = this.plugin.con.prepareStatement("UPDATE `tickets` SET updated_at = NOW() WHERE ticketid = ?");
				stmt.setInt(1, record.ticketid);
				stmt.executeUpdate();
				if (stmt != null) stmt.close();
				
				stmt = this.plugin.con.prepareStatement("INSERT INTO `ticketactions` (`ticketid`, `uuid`, `type`, `content`, `world`, `x`, `y`, `z`, `seen`, `created_at`) VALUES (?, ?, 'comment', ?, ?, ?, ?, ?, 0, NOW())");
				stmt.setInt(1, record.ticketid);
				stmt.setString(2, player.getUniqueId().toString());
				stmt.setString(3, title);
				stmt.setString(4, player.getWorld().getName());
				stmt.setDouble(5, player.getLocation().getX());
				stmt.setDouble(6, player.getLocation().getY());
				stmt.setDouble(7, player.getLocation().getZ());
				stmt.executeUpdate();
				
				ArrayList<String> skip = new ArrayList<String>();
				skip.add(player.getUniqueId().toString());
				
				if (record.isOwnedBy(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was updated: " + title);
				}
				else if (record.canAccess(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was updated: " + title);
				}
				if (record.assigned_uuid != null) {
					Player assigned = Bukkit.getPlayer(UUID.fromString(record.assigned_uuid));
					if (assigned != null && assigned.isOnline() && !assigned.getUniqueId().equals(player.getUniqueId())) {
						assigned.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " has been updated by " + player.getName());
						skip.add(record.assigned_uuid);
					}
				}
				
				this.notifyModerators(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " comment added by " + player.getName() + ": " + title, skip);
			}
			else {
				this.plugin.getLogger().warning("Player " + player.getName() + " was denied commenting to ticket #" + ticketid);
			}
		}
		catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		}
		catch (NumberFormatException e) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong ticket number. See "+ChatColor.RED+"/ticket");
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
	}
	
	public void performClose(Player player, String[] args) {
		int index = 2;
		String title = this.getTitle(args, index);

		PreparedStatement stmt = null;
		ResultSet res = null;
		
		try {
			int ticketid = Integer.parseInt(args[1]);
			TicketRecord record = this.getTicket(ticketid);
			if (record == null || !record.status.equalsIgnoreCase("open")) {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No open ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " found.");
				return;
			}
			
			if (record.isOwnedBy(player) || player.hasPermission("buxtickets.admin") || record.canAccess(player)) {
				stmt = this.plugin.con.prepareStatement("UPDATE `tickets` SET updated_at = NOW(), status = 'closed' WHERE ticketid = ?");
				stmt.setInt(1, record.ticketid);
				stmt.executeUpdate();
				if (stmt != null) stmt.close();
				
				stmt = this.plugin.con.prepareStatement("INSERT INTO `ticketactions` (`ticketid`, `uuid`, `type`, `content`, `world`, `x`, `y`, `z`, `seen`, `created_at`) VALUES (?, ?, 'status', ?, ?, ?, ?, ?, 0, NOW())");
				stmt.setInt(1, record.ticketid);
				stmt.setString(2, player.getUniqueId().toString());
				String titletmp = title;
				if (titletmp.length() > 0) {
					titletmp = "closed ticket: " + title;
				}
				else {
					titletmp = "closed ticket";
				}
				stmt.setString(3, titletmp);
				stmt.setString(4, player.getWorld().getName());
				stmt.setDouble(5, player.getLocation().getX());
				stmt.setDouble(6, player.getLocation().getY());
				stmt.setDouble(7, player.getLocation().getZ());
				stmt.executeUpdate();
				
				ArrayList<String> skip = new ArrayList<String>();
				skip.add(player.getUniqueId().toString());
				
				if (record.isOwnedBy(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was closed. " + title);
				}
				else if (record.canAccess(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was closed. " + title);
				}
				else if (player.hasPermission("buxtickets.admin")) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was closed. " + title);
					Player pptmp = Bukkit.getServer().getPlayer(UUID.fromString(record.uuid));
					if (pptmp != null && pptmp.isOnline()) {
						pptmp.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was closed. " + title);
					}
				}
				else {
					Player pptmp = Bukkit.getServer().getPlayer(UUID.fromString(record.uuid));
					if (pptmp != null && pptmp.isOnline()) {
						pptmp.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was closed. " + title);
					}
				}
				if (record.assigned_uuid != null) {
					Player assigned = Bukkit.getPlayer(UUID.fromString(record.assigned_uuid));
					if (assigned != null && assigned.isOnline() && !assigned.getUniqueId().equals(player.getUniqueId())) {
						assigned.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was closed by " + player.getName());
						skip.add(record.assigned_uuid);
					}
				}
				
				this.notifyModerators(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was closed. " + title, skip);
			}
			else {
				this.plugin.getLogger().warning("Player " + player.getName() + " was denied closing ticket #" + ticketid);
			}
		}
		catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		}
		catch (NumberFormatException e) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong ticket number. See "+ChatColor.RED+"/ticket");
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
	}
	
	public void performReopen(Player player, String[] args) {
		int index = 2;
		String title = this.getTitle(args, index);

		PreparedStatement stmt = null;
		ResultSet res = null;
		
		try {
			int ticketid = Integer.parseInt(args[1]);
			TicketRecord record = this.getTicket(ticketid);
			if (record == null || !record.status.equalsIgnoreCase("closed")) {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No closed ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " found.");
				return;
			}
			
			if (player.hasPermission("buxtickets.admin")) {
				stmt = this.plugin.con.prepareStatement("UPDATE `tickets` SET updated_at = NOW(), status = 'open' WHERE ticketid = ?");
				stmt.setInt(1, record.ticketid);
				stmt.executeUpdate();
				if (stmt != null) stmt.close();
				
				stmt = this.plugin.con.prepareStatement("INSERT INTO `ticketactions` (`ticketid`, `uuid`, `type`, `content`, `world`, `x`, `y`, `z`, `seen`, `created_at`) VALUES (?, ?, 'status', ?, ?, ?, ?, ?, 0, NOW())");
				stmt.setInt(1, record.ticketid);
				stmt.setString(2, player.getUniqueId().toString());
				String titletmp = title;
				if (titletmp.length() > 0) {
					titletmp = "reopened ticket: " + title;
				}
				else {
					titletmp = "reopened ticket";
				}
				stmt.setString(3, titletmp);
				stmt.setString(4, player.getWorld().getName());
				stmt.setDouble(5, player.getLocation().getX());
				stmt.setDouble(6, player.getLocation().getY());
				stmt.setDouble(7, player.getLocation().getZ());
				stmt.executeUpdate();
				
				ArrayList<String> skip = new ArrayList<String>();
				skip.add(player.getUniqueId().toString());
				
				if (record.isOwnedBy(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was reopened. " + title);
				}
				if (record.assigned_uuid != null) {
					Player assigned = Bukkit.getPlayer(UUID.fromString(record.assigned_uuid));
					if (assigned != null && assigned.isOnline() && !assigned.getUniqueId().equals(player.getUniqueId())) {
						assigned.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was reopened by " + player.getName());
						skip.add(record.assigned_uuid);
					}
				}
				
				this.notifyModerators(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was reopened. " + title, skip);
			}
			else {
				this.plugin.getLogger().warning("Player " + player.getName() + " was denied reopening ticket #" + ticketid);
			}
		}
		catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		}
		catch (NumberFormatException e) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong ticket number. See "+ChatColor.RED+"/ticket");
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
	}
	
	public void performUnassign(Player player, String[] args) {
		int index = 2;
		String title = this.getTitle(args, index);

		PreparedStatement stmt = null;
		ResultSet res = null;
		
		try {
			int ticketid = Integer.parseInt(args[1]);
			TicketRecord record = this.getTicket(ticketid);
			if (record == null || !record.status.equalsIgnoreCase("open")) {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No open ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " found.");
				return;
			}
			
			if (player.hasPermission("buxtickets.admin") || record.canAccess(player)) {
				stmt = this.plugin.con.prepareStatement("UPDATE `tickets` SET updated_at = NOW(), assigned_uuid = NULL WHERE ticketid = ?");
				stmt.setInt(1, record.ticketid);
				stmt.executeUpdate();
				if (stmt != null) stmt.close();
				
				stmt = this.plugin.con.prepareStatement("INSERT INTO `ticketactions` (`ticketid`, `uuid`, `type`, `content`, `world`, `x`, `y`, `z`, `seen`, `created_at`) VALUES (?, ?, 'unassign', ?, ?, ?, ?, ?, 0, NOW())");
				stmt.setInt(1, record.ticketid);
				stmt.setString(2, player.getUniqueId().toString());
				String titletmp = title;
				if (titletmp.length() > 0) {
					titletmp = "unassigned ticket: " + title;
				}
				else {
					titletmp = "unassigned ticket";
				}
				stmt.setString(3, titletmp);
				stmt.setString(4, player.getWorld().getName());
				stmt.setDouble(5, player.getLocation().getX());
				stmt.setDouble(6, player.getLocation().getY());
				stmt.setDouble(7, player.getLocation().getZ());
				stmt.executeUpdate();
				
				ArrayList<String> skip = new ArrayList<String>();
				skip.add(player.getUniqueId().toString());
				
				if (record.isOwnedBy(player)) {
//					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was unassigned. " + title);
				}
				else if (record.canAccess(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was unassigned.");
					Player pptmp = Bukkit.getServer().getPlayer(UUID.fromString(record.uuid));
					if (pptmp != null && pptmp.isOnline()) {
						pptmp.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was unassigned.");
					}
				}
				else if (player.hasPermission("buxtickets.admin")) {
					Player pptmp = Bukkit.getServer().getPlayer(UUID.fromString(record.uuid));
					if (pptmp != null && pptmp.isOnline()) {
						pptmp.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was unassigned.");
					}
				}
				if (record.assigned_uuid != null) {
					Player assigned = Bukkit.getPlayer(UUID.fromString(record.assigned_uuid));
					if (assigned != null && assigned.isOnline() && !assigned.getUniqueId().equals(player.getUniqueId())) {
						assigned.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was unassigned by " + player.getName());
						skip.add(record.assigned_uuid);
					}
				}
				
				this.notifyModerators(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was unassigned. " + title, skip);
			}
			else {
				this.plugin.getLogger().warning("Player " + player.getName() + " was denied unassigning ticket #" + ticketid);
			}
		}
		catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		}
		catch (NumberFormatException e) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong ticket number. See "+ChatColor.RED+"/ticket");
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
	}
	
	public void performAssign(Player player, String[] args) {
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		try {
			int ticketid = Integer.parseInt(args[1]);
			TicketRecord record = this.getTicket(ticketid);
			if (record == null || !record.status.equalsIgnoreCase("open")) {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No open ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " found.");
				return;
			}
			
			if (player.hasPermission("buxtickets.admin") || record.canAccess(player)) {
				PlayerRecord p = new PlayerRecord(player.getUniqueId().toString(), player.getName());
				if (args.length == 3) {
					// Assign to some user
					@SuppressWarnings("deprecation")
					Player tmp = Bukkit.getServer().getPlayerExact(args[2]);
					if (tmp != null) {
						// If we find this player, probably online
						p = new PlayerRecord(tmp.getUniqueId().toString(), tmp.getName());
					}
					else {
						// Otherwise find it in buxnewnames table
						p = this.findPlayerByName(args[2]);
					}
				}
				else {
					// Assign to self
				}
				
				if (p == null || p.uuid == null) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Error: cannot assign ticket as player UUID cannot be found, has player been seen before?");
					return;
				}
				
				stmt = this.plugin.con.prepareStatement("UPDATE `tickets` SET updated_at = NOW(), assigned_uuid = ? WHERE ticketid = ?");
				stmt.setString(1, p.uuid);
				stmt.setInt(2, record.ticketid);
				stmt.executeUpdate();
				if (stmt != null) stmt.close();
				
				stmt = this.plugin.con.prepareStatement("INSERT INTO `ticketactions` (`ticketid`, `uuid`, `type`, `content`, `world`, `x`, `y`, `z`, `seen`, `new_uuid`, `created_at`) VALUES (?, ?, 'assign', ?, ?, ?, ?, ?, 0, ?, NOW())");
				stmt.setInt(1, record.ticketid);
				stmt.setString(2, player.getUniqueId().toString());
				stmt.setString(3, "assigned ticket");
				stmt.setString(4, player.getWorld().getName());
				stmt.setDouble(5, player.getLocation().getX());
				stmt.setDouble(6, player.getLocation().getY());
				stmt.setDouble(7, player.getLocation().getZ());
				stmt.setString(8, p.uuid);
				stmt.executeUpdate();
				
				ArrayList<String> skip = new ArrayList<String>();
				
				if (record.isOwnedBy(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " assigned to " + p.name + ".");
					skip.add(player.getUniqueId().toString());
				}
				else if (record.canAccess(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was assigned to " + p.name + ".");
					skip.add(player.getUniqueId().toString());
					Player pptmp = Bukkit.getServer().getPlayer(UUID.fromString(record.uuid));
					if (pptmp != null && pptmp.isOnline()) {
						pptmp.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " assigned to " + p.name + ".");
					}
				}
				else if (player.hasPermission("buxtickets.admin")) {
					Player pptmp = Bukkit.getServer().getPlayer(UUID.fromString(record.uuid));
					if (pptmp != null && pptmp.isOnline()) {
						pptmp.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Your ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " assigned to " + p.name + ".");
					}
				}
				if (record.assigned_uuid != null) {
					Player assigned = Bukkit.getPlayer(UUID.fromString(p.uuid));
					if (assigned != null && assigned.isOnline() && !assigned.getUniqueId().equals(player.getUniqueId())) {
						assigned.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " has been assigned to you by " + player.getName() + ".");
						skip.add(p.uuid);
					}
				}
				
				this.notifyModerators(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " has been assigned to " + p.name + ". ", skip);
			}
			else {
				this.plugin.getLogger().warning("Player " + player.getName() + " was denied assigning ticket #" + ticketid);
			}
		}
		catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		}
		catch (NumberFormatException e) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong ticket number. See "+ChatColor.RED+"/ticket");
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
	}
	
	public void performMove(Player player, String[] args) {
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		try {
			int ticketid = Integer.parseInt(args[1]);
			TicketRecord record = this.getTicket(ticketid);
			if (record == null || !record.status.equalsIgnoreCase("open")) {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No open ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " found.");
				return;
			}
			
			if (player.hasPermission("buxtickets.admin") || record.canAccess(player)) {
				String group = args[2];
				
				stmt = this.plugin.con.prepareStatement("UPDATE `tickets` SET updated_at = NOW(), `group` = ? WHERE ticketid = ?");
				stmt.setString(1, group);
				stmt.setInt(2, record.ticketid);
				stmt.executeUpdate();
				if (stmt != null) stmt.close();
				
				stmt = this.plugin.con.prepareStatement("INSERT INTO `ticketactions` (`ticketid`, `uuid`, `type`, `content`, `world`, `x`, `y`, `z`, `seen`, `created_at`) VALUES (?, ?, 'assign', ?, ?, ?, ?, ?, 0, NOW())");
				stmt.setInt(1, record.ticketid);
				stmt.setString(2, player.getUniqueId().toString());
				stmt.setString(3, "moved to group " + group);
				stmt.setString(4, player.getWorld().getName());
				stmt.setDouble(5, player.getLocation().getX());
				stmt.setDouble(6, player.getLocation().getY());
				stmt.setDouble(7, player.getLocation().getZ());
				stmt.executeUpdate();
				
				ArrayList<String> skip = new ArrayList<String>();
				
				if (record.canAccess(player)) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " was moved to group " + group + ".");
					skip.add(player.getUniqueId().toString());
				}
//				if (record.assigned_uuid != null) {
//					Player assigned = Bukkit.getPlayer(UUID.fromString(p.uuid));
//					if (assigned != null && assigned.isOnline() && !assigned.getUniqueId().equals(player.getUniqueId())) {
//						assigned.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "Ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " has been moved to group " + group + ".");
//						skip.add(p.uuid);
//					}
//				}
				
				this.notifyModerators(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " has been moved to group " + group + ". ", skip);
			}
			else {
				this.plugin.getLogger().warning("Player " + player.getName() + " was denied moving groups for ticket #" + ticketid);
			}
		}
		catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		    System.out.println("SQLState: " + e.getSQLState());
		    System.out.println("VendorError: " + e.getErrorCode());
		}
		catch (NumberFormatException e) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong ticket number. See "+ChatColor.RED+"/ticket");
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
	}
	
	public String header(Server server, int ticketid, String uuid, String name, String assignee_uuid, String assignee_name, String title, int num, String group) {
		Player p = null;
		if (uuid != null) p = server.getPlayer(UUID.fromString(uuid));
		Player pa = null;
		String aname = "(unassigned)";
		if (assignee_uuid != null) {
			pa = server.getPlayer(UUID.fromString(assignee_uuid));
			if (pa != null) aname = pa.getName();
		}
		if (pa == null && assignee_uuid != null) {
			PlayerRecord pr = this.findPlayerByUUID(assignee_uuid);
			if (pr != null && pr.name != null) aname = pr.name;
			if (pr == null) System.out.println("cant find user??");
		}
		
		ChatColor groupColor = ChatColor.GRAY;
		if (this.plugin.groupcolors.containsKey(group)) {
			groupColor = ChatColor.getByChar((String)this.plugin.groupcolors.get(group));
		}
		
		return ChatColor.GOLD + "#" + ticketid + ChatColor.GRAY + " (" + groupColor.toString() + group + ChatColor.GRAY + ") " + (p != null && p.isOnline() ? ChatColor.GREEN : ChatColor.RED) + name + ChatColor.GRAY
				+ " -> " + (pa != null && pa.isOnline() ? ChatColor.GREEN : ChatColor.RED) + aname + ChatColor.GRAY + ": " + title + " (" + num + ")"; 
	}
	
	public void performView(Player player, String[] args) {
		try {
			int ticketid = Integer.parseInt(args[1]);
			TicketRecord record = this.getTicket(ticketid);
			if (record == null) {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + " found.");
				return;
			}
			
			if (record.isOwnedBy(player) || player.hasPermission("buxtickets.admin") || record.canAccess(player)) {
				Player p = null;
				if (record.assigned_uuid != null) p = Bukkit.getServer().getPlayer(UUID.fromString(record.assigned_uuid));
				String assignee_name = "(unassigned)";
				if (p != null && p.getName() != null) {
					assignee_name = p.getName();
				}
				else if (record.assigned_uuid != null) {
					PlayerRecord pr = this.findPlayerByUUID(record.assigned_uuid);
					if (pr != null && pr.name != null) assignee_name = pr.name;
				}
				// Otherwise find it in buxnewnames table
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + this.header(Bukkit.getServer(), record.ticketid, record.uuid, record.owner, record.assigned_uuid, assignee_name, record.content, record.actions.size(), record.group));
				
				ArrayList<TicketActionRecord> unseen = new ArrayList<TicketActionRecord>(5);
				
				for (TicketActionRecord	action : record.actions) {
					Player p2 = Bukkit.getServer().getPlayer(UUID.fromString(action.uuid));
					String pname = "";
					if (p2 != null) {
						pname = p2.getName();
					}
					else {
						PlayerRecord pr = this.findPlayerByUUID(action.uuid);
						pname = pr.name;
					}
					
					String highlightColor = ChatColor.GRAY.toString();
					if (action.seen == 0) {
						highlightColor = ChatColor.WHITE.toString();
						unseen.add(action);
					}

					String more = "";
					if (action.type.equalsIgnoreCase("comment")) {
						more = "commented: ";
					}
					
					String more2 = ".";
					if (action.type.equalsIgnoreCase("assign")) {
						Player p3 = Bukkit.getServer().getPlayer(UUID.fromString(action.new_uuid));
						String aname = "(unassigned)";
						if (p3 != null && p3.getName() != null) {
							aname = p3.getName();
						}
						else {
							PlayerRecord pr = this.findPlayerByUUID(action.new_uuid);
							if (pr != null && pr.name != null) aname = pr.name;
						}
						more2 = " to " + aname + ".";
					}
					
					if (action.type.equalsIgnoreCase("comment")) {
						more2 = "";
					}
					
					if (player.getUniqueId().toString().equalsIgnoreCase(record.uuid)) {
						// Creator
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + (player.hasPermission("buxtickets.admin") ? "[" + action.ticketactionid + "] " : "") + pname + " " + more + action.content + more2);
					}
					else {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + highlightColor + (player.hasPermission("buxtickets.admin") ? "[" + action.ticketactionid + "] " : "") + pname + " " + more + action.content + more2);
					}
				}
				if (unseen.size() > 0 && player.hasPermission("buxtickets.admin")) {
					this.markSeen(unseen);
				}
			}
			else {
				player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"You don't have permission to view ticket " + ChatColor.GOLD + "#" + ticketid + ChatColor.RED + ".");
				this.plugin.getLogger().warning("Player " + player.getName() + " was denied viewing ticket #" + ticketid);
			}
		}
		catch (NumberFormatException e) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong ticket number. See "+ChatColor.RED+"/ticket");
		}
		finally {
		}
	}
	
	public void performList(Player player, String[] args) {
		int limit = 10;
		int offset = 0;
		boolean include_offline = true;
		boolean include_online = true;
		boolean include_closed = false;
		boolean include_open = true;
		boolean sort_reverse = false;
		boolean ignore_assigned = false;
		boolean only_mine = false;
		String group = "";
		String filter = "";
		
		if (args.length >= 2) {
			for (int index = 1; index < args.length; index++) {
				if (args[index].equalsIgnoreCase("closed")) {
					include_closed = true;
					include_open = false;
					continue;
				}
				if (args[index].equalsIgnoreCase("open")) {
					include_open = true;
					continue;
				}
				if (args[index].equalsIgnoreCase("newest")) {
					sort_reverse = true;
					continue;
				}
				if (args[index].equalsIgnoreCase("mine")) {
					only_mine = true;
					continue;
				}
				if (args[index].equalsIgnoreCase("unassigned")) {
					ignore_assigned = true;
					continue;
				}
				if (args[index].equalsIgnoreCase("online")) {
					include_offline = false;
					continue;
				}
				if (args[index].equalsIgnoreCase("offline")) {
					include_online = false;
					continue;
				}
				if (args[index].length() >= 7 && args[index].substring(0, 6).equalsIgnoreCase("group:")) {
					String[] tmp = args[index].split(":");
					if (tmp.length >= 2) {
						group = tmp[1];
					}
					continue;
				}
				if (args[index].matches("^\\d+$")) {
					try {
						offset = (Integer.parseInt(args[index])-1)*limit;
					}
					catch (NumberFormatException e) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Wrong page number. See "+ChatColor.RED+"/ticket");
						return;
					}
					continue;
				}
				if (filter.length() > 0) filter = filter + " ";
				filter = filter + args[index];
			}
		}
		
		ArrayList<TicketRecord> records = this.getTickets(player, limit, offset, include_offline, include_online, include_closed, include_open, sort_reverse, ignore_assigned, filter, group, only_mine);
		if (records.size() == 0) {
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No tickets found.");
			return;
		}
		
		for (TicketRecord record : records) {
			Player p = null;
			if (record.assigned_uuid != null) p = Bukkit.getServer().getPlayer(UUID.fromString(record.assigned_uuid));
			String assignee_name = "(unassigned)";
			if (p != null && p.getName() != null) {
				assignee_name = p.getName();
			}
			else if (record.assigned_uuid != null) {
				PlayerRecord pr = this.findPlayerByUUID(record.assigned_uuid);
				if (pr != null && pr.name != null) assignee_name = pr.name;
			}
			// Otherwise find it in buxnewnames table
			player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + this.header(Bukkit.getServer(), record.ticketid, record.uuid, record.owner, record.assigned_uuid, assignee_name, record.content, record.justcount, record.group));
		}
	}
	
	public int getOpenTicketCount(Player player) {
		if (player == null) return 0;
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		int result = 0;

		try {
			stmt = this.plugin.con.prepareStatement("SELECT COUNT(*) AS n FROM `tickets` WHERE uuid = ? AND status = 'open'");
			stmt.setString(1, player.getUniqueId().toString());
			res = stmt.executeQuery();
			
			if (res.first()) {
				result = res.getInt("n");
			}
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
		return result;
	}
	
	public int getOpenTicketCountAll() {
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		int result = 0;

		try {
			stmt = this.plugin.con.prepareStatement("SELECT COUNT(*) AS n FROM `tickets` WHERE status = 'open'");
			res = stmt.executeQuery();
			
			if (res.first()) {
				result = res.getInt("n");
			}
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
		return result;
	}
	
	public ArrayList<TicketRecord> getTickets(Player player, int limit, int offset,
			boolean include_offline, boolean include_online,
			boolean include_closed, boolean include_open, boolean sort_reverse,
			boolean ignore_assigned, String filter, String group, boolean only_mine) {

		ArrayList<TicketRecord> records = new ArrayList<TicketRecord>(limit);
		PreparedStatement stmt = null;
		ResultSet res = null;

		try {
			if (player.hasPermission("buxtickets.admin")) {
				// Admin listing
				String sql_offline = "";
				String sql_closed = " AND `tickets`.status = 'open'";
				String sql_assigned = "";
				String sql_filter = "";
				String sql_order = "ASC";
				String sql_group = "";
				String sql_only_mine = "";
				
				if (include_offline && !include_online) {
					StringBuilder sb = new StringBuilder(50);
					int count = 0;
					for (Player playerOnline : Bukkit.getServer().getOnlinePlayers()) {
						if (count > 0) sb.append(", ");
						sb.append("'" + playerOnline.getUniqueId().toString() + "'");
						count++;
					}
					sql_offline = " AND `tickets`.uuid NOT IN ("+sb.toString()+")";
				}
				else if (include_online && !include_offline) {
					StringBuilder sb = new StringBuilder(50);
					int count = 0;
					for (Player playerOnline : Bukkit.getServer().getOnlinePlayers()) {
						if (count > 0) sb.append(", ");
						sb.append("'" + playerOnline.getUniqueId().toString() + "'");
						count++;
					}
					sql_offline = " AND `tickets`.uuid IN ("+sb.toString()+")";
				}	
				if (include_closed && !include_open) {
					sql_closed = " AND `tickets`.status = 'closed'";
				}
				else if (include_open && !include_closed) {
					sql_closed = " AND `tickets`.status = 'open'";
				}
				else {
					sql_closed = "";
				}
				if (ignore_assigned) {
					sql_assigned = " AND `tickets`.assigned_uuid IS NULL";
				}
				if (filter.length() > 0) {
					sql_filter = " AND (`tickets`.owner LIKE ? OR `tickets`.content LIKE ?)";
				}
				if (sort_reverse) {
					sql_order = "DESC";
				}
				if (group.length() > 0) {
					sql_group = " AND `tickets`.group = ?";
				}
				if (only_mine) {
					sql_only_mine = " AND `tickets`.assigned_uuid = ?";
				}
				
				stmt = this.plugin.con.prepareStatement("SELECT `tickets`.*, COUNT(`ticketactions`.`ticketactionid`) AS actions FROM `tickets` LEFT JOIN `ticketactions` USING(`ticketid`) WHERE 1=1" + sql_offline + sql_closed + sql_assigned + sql_filter + sql_group + sql_only_mine + " GROUP BY `ticketid` ORDER BY `tickets`.created_at "+sql_order+" LIMIT ?, ?");
				int idx = 1;
				if (filter.length() > 0) {
					stmt.setString(idx, "%"+filter+"%");
					idx++;
					stmt.setString(idx, "%"+filter+"%");
					idx++;
				}
				if (group.length() > 0) {
					stmt.setString(idx, group);
					idx++;
				}
				if (only_mine) {
					stmt.setString(idx, player.getUniqueId().toString());
					idx++;
				}
				stmt.setInt(idx, offset); idx++;
				stmt.setInt(idx, limit);
				res = stmt.executeQuery();
				
				while (res.next()) {
					TicketRecord tr = new TicketRecord(res.getInt("ticketid"), res.getString("content"), res.getString("uuid"), res.getString("owner"), res.getString("group"), res.getString("world"), res.getDouble("x"), res.getDouble("y"), res.getDouble("z"), res.getString("status"), res.getString("assigned_uuid"));
					tr.justcount = res.getInt("actions");
					records.add(tr);
				}
			}
			else {
				// Player listing
				String sql_offline = "";
				String sql_closed = " AND `tickets`.status = 'open'";
				String sql_assigned = "";
				String sql_filter = "";
				String sql_order = "ASC";
				String sql_group = "";
				String sql_only_mine = "";

				if (include_offline && !include_online) {
					StringBuilder sb = new StringBuilder(50);
					int count = 0;
					for (Player playerOnline : Bukkit.getServer().getOnlinePlayers()) {
						if (count > 0) sb.append(", ");
						sb.append("'" + playerOnline.getUniqueId().toString() + "'");
						count++;
					}
					sql_offline = " AND `tickets`.uuid NOT IN ("+sb.toString()+")";
				}
				else if (include_online && !include_offline) {
					StringBuilder sb = new StringBuilder(50);
					int count = 0;
					for (Player playerOnline : Bukkit.getServer().getOnlinePlayers()) {
						if (count > 0) sb.append(", ");
						sb.append("'" + playerOnline.getUniqueId().toString() + "'");
						count++;
					}
					sql_offline = " AND `tickets`.uuid IN ("+sb.toString()+")";
				}	
				if (include_closed) {
					sql_closed = "";
				}
				if (ignore_assigned) {
					sql_assigned = " AND `tickets`.assigned_uuid IS NULL";
				}
				if (filter.length() > 0) {
					sql_filter = " AND (`tickets`.owner LIKE ? OR `tickets`.content LIKE ?)";
				}
				if (sort_reverse) {
					sql_order = "DESC";
				}
				if (group.length() > 0) {
					sql_group = " AND `tickets`.group = ?";
				}
				if (only_mine) {
					sql_only_mine = " AND `tickets`.assigned_uuid = ?";
				}
				
				String sql_owner = "`tickets`.uuid = ?";
				boolean hasperm = false;
				if (group.length() > 0 && player.hasPermission("buxtickets.see." + group)) {
					sql_owner = "1=1";
					hasperm = true;
				}
				else {
					sql_group = "";
					group = "";
				}

				stmt = this.plugin.con.prepareStatement("SELECT `tickets`.*, COUNT(`ticketactions`.`ticketactionid`) AS actions FROM `tickets` LEFT JOIN `ticketactions` USING(`ticketid`) WHERE " + sql_owner + " " + sql_offline + sql_closed + sql_assigned + sql_filter + sql_group + sql_only_mine + " GROUP BY `ticketid` ORDER BY `tickets`.created_at "+sql_order+" LIMIT ?, ?");
				int idx = 1;
				if (!hasperm) {
					stmt.setString(idx, player.getUniqueId().toString()); idx++;
				}
				if (filter.length() > 0) {
					stmt.setString(idx, "%"+filter+"%");
					idx++;
					stmt.setString(idx, "%"+filter+"%");
					idx++;
				}
				if (group.length() > 0) {
					stmt.setString(idx, group);
					idx++;
				}
				if (only_mine) {
					stmt.setString(idx, player.getUniqueId().toString());
					idx++;
				}
				stmt.setInt(idx, offset); idx++;
				stmt.setInt(idx, limit);
				res = stmt.executeQuery();
				
				while (res.next()) {
					TicketRecord tr = new TicketRecord(res.getInt("ticketid"), res.getString("content"), res.getString("uuid"), res.getString("owner"), res.getString("group"), res.getString("world"), res.getDouble("x"), res.getDouble("y"), res.getDouble("z"), res.getString("status"), res.getString("assigned_uuid"));
					tr.justcount = res.getInt("actions");
					records.add(tr);
				}
			}
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
		
		return records;
	}

	public void markSeen(ArrayList<TicketActionRecord> unseen) {
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		StringBuilder sb = new StringBuilder(50);
		int count = 0;
		for (TicketActionRecord action : unseen) {
			if (count > 0) sb.append(", ");
			
			sb.append(action.ticketactionid);
			
			count++;
		}
		
		try {
			stmt = this.plugin.con.prepareStatement("UPDATE `ticketactions` SET seen = 1 WHERE ticketactionid IN ("+sb.toString()+")");
			stmt.executeUpdate();
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
	}
	
	// Send notification to moderators (new ticket made, timed notifications, etc.)
	public void notifyModerators(String msg, ArrayList<String> skipPlayer) {
		Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
		for (Player player : players) {
			if (player.hasPermission("buxtickets.admin")) {
				boolean skip = false;
				for (String uuid : skipPlayer) {
					if (player.getUniqueId().toString().equalsIgnoreCase(uuid)) {
						skip = true;
					}
				}
				
				if (!skip) {
					player.sendMessage(msg);
				}
			}
		}
	}
	
	public PlayerRecord findPlayerByName(String name) {
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		PlayerRecord result = null;

		try {
			stmt = this.plugin.con.prepareStatement("SELECT uuid, name FROM `buxnewname` WHERE name = ?");
			stmt.setString(1, name);
			res = stmt.executeQuery();
			
			if (res.first()) {
				result = new PlayerRecord(res.getString("uuid"), res.getString("name"));
				
			}
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
		return result;
	}

	public PlayerRecord findPlayerByUUID(String uuid) {
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		PlayerRecord result = null;

		try {
			stmt = this.plugin.con.prepareStatement("SELECT uuid, name FROM `buxnewname` WHERE uuid = ?");
			stmt.setString(1, uuid);
			res = stmt.executeQuery();
			
			if (res.first()) {
				result = new PlayerRecord(res.getString("uuid"), res.getString("name"));
				
			}
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
		return result;
	}
}
