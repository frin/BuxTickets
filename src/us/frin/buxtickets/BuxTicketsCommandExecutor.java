package us.frin.buxtickets;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.frin.buxtickets.commands.BuxTicketsCommands;

public class BuxTicketsCommandExecutor implements CommandExecutor {
	BuxTickets plugin;
	
	public BuxTicketsCommandExecutor(BuxTickets plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("ticket")) {
			if (args.length == 0) {
				//////////////////////////////////
				// Ticket Usage Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"The Buxville Ticket System");
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket: this help page");
					if (player.hasPermission("buxtickets.use")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"open|new|create <text>"+ChatColor.WHITE+": open new ticket");
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"close #"+ChatColor.WHITE+": close ticket");
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"comment # <text>"+ChatColor.WHITE+": add comment to existing ticket");
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"view #"+ChatColor.WHITE+": view ticket details");
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"list open|closed|newest|unassigned|online|offline|mine|group:<name> [page] [filter]"+ChatColor.WHITE+": list your tickets, optional parameters, page and filter by name/content");
					}
					if (this.hasSomePerm(player)) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"assign # <name>"+ChatColor.WHITE+": assign ticket to other player");
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"unassign #"+ChatColor.WHITE+": unassign ticket");
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"move # <group>"+ChatColor.WHITE+": move ticket to other group");
					}
					if (player.hasPermission("buxtickets.admin")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"warp # [#]"+ChatColor.WHITE+": warp to the location of ticket creation (second number is optional action number on ticket)");
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.WHITE+"/ticket "+ChatColor.YELLOW+"reopen #"+ChatColor.WHITE+": reopen a closed ticket");
					}
				}
				
				return true;
			}
			else if (args[0].equalsIgnoreCase("warp")) {
				//////////////////////////////////
				// Ticket Warp Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.admin")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performWarp(player, args);
				}
			}
			else if (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("new") || args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("o") || args[0].equalsIgnoreCase("n")) {
				//////////////////////////////////
				// Ticket New Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.use")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					String group = null;
					for (String g : plugin.groups) {
						if (g.equalsIgnoreCase(args[1])) {
							group = g;
							break;
						}
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performNew(player, group, args);
				}
			}
			else if (args[0].equalsIgnoreCase("comment") || args[0].equalsIgnoreCase("co")) {
				//////////////////////////////////
				// Ticket Comment Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.use")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performComment(player, args);
				}
			}
			else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("l")) {
				//////////////////////////////////
				// Ticket List Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.use")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}

					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performList(player, args);
				}
			}
			else if (args[0].equalsIgnoreCase("close") || args[0].equalsIgnoreCase("cl")) {
				//////////////////////////////////
				// Ticket Close Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.use")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performClose(player, args);
				}
			}
			else if (args[0].equalsIgnoreCase("reopen") || args[0].equalsIgnoreCase("r")) {
				//////////////////////////////////
				// Ticket Reopen Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.admin")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performReopen(player, args);
				}
			}
			else if (args[0].equalsIgnoreCase("unassign") || args[0].equalsIgnoreCase("u")) {
				//////////////////////////////////
				// Ticket Unassign Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.admin") && !this.hasSomePerm(player)) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performUnassign(player, args);
				}
			}
			else if (args[0].equalsIgnoreCase("move") || args[0].equalsIgnoreCase("m")) {
				//////////////////////////////////
				// Ticket Move Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.admin") && !this.hasSomePerm(player)) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 3) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performMove(player, args);
				}
			}
			else if (args[0].equalsIgnoreCase("assign") || args[0].equalsIgnoreCase("a")) {
				//////////////////////////////////
				// Ticket Assign Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.admin") && !this.hasSomePerm(player)) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performAssign(player, args);
				}
			}
			else if (args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("v")) {
				//////////////////////////////////
				// Ticket View Command
				//////////////////////////////////
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be run by a player.");
				}
				else {
					Player player = (Player) sender;
					if (!player.hasPermission("buxtickets.use")) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"No permission to use this command.");
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] "+ChatColor.RED+"Not enough parameters. See "+ChatColor.RED+"/ticket");
						return true;
					}
					
					BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
					cmds.performView(player, args);
				}
			}
			return true;
		}
		return false;
	}
	
	public boolean hasSomePerm(Player player) {
		for (String group : this.plugin.groups) {
			if (player.hasPermission("buxtickets.see."+group)) {
				return true;
			}
		}
		return false;
	}
}
