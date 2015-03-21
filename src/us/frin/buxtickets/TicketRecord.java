package us.frin.buxtickets;

import java.util.ArrayList;

import org.bukkit.entity.Player;

public class TicketRecord {
	// Class for holding data retrieved from MySQL table
	public int ticketid;
	public String content;
	public String uuid;
	public String owner;
	public String group;
	public String world;
	public double x;
	public double y;
	public double z;
	public String status;
	public String assigned_uuid;
	public ArrayList<TicketActionRecord> actions;
	public int justcount;
	public String created_at;
	
	public TicketRecord(int ticketid, String content, String uuid, String owner, String group, String world, double x, double y, double z, String status, String assigned_uuid, String created_at) {
		this.ticketid = ticketid;
		this.content = content;
		this.uuid = uuid;
		this.owner = owner;
		this.group = group;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.status = status;
		this.assigned_uuid = assigned_uuid;
		this.actions = new ArrayList<TicketActionRecord>();
		this.justcount = 0;
		this.created_at = created_at;
	}
	
	public boolean isOwnedBy(Player player) {
		if (player.getUniqueId().toString().equalsIgnoreCase(this.uuid)) {
			return true;
		}
		return false;
	}
	
	public boolean canAccess(Player player) {
		if (this.group.length() > 0 && player.hasPermission("buxtickets.see."+this.group)) {
			return true;
		}
		return false;
	}

}
