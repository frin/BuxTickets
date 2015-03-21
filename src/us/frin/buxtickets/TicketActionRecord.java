package us.frin.buxtickets;

public class TicketActionRecord {
	// Class for holding data retrieved from MySQL table
	public int ticketactionid;
	public int ticketid;
	public String uuid;
	public String type;
	public String content;
	public String world;
	public double x;
	public double y;
	public double z;
	public String new_uuid;
	public int seen;
	public String created_at;
	
	public TicketActionRecord(int ticketid, int ticketactionid, String uuid, String type, String content, String world, double x, double y, double z, String new_uuid, int seen, String created_at) {
		this.ticketid = ticketid;
		this.ticketactionid = ticketactionid;
		this.uuid = uuid;
		this.type = type;
		this.content = content;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.new_uuid = new_uuid;
		this.seen = seen;
		this.created_at = created_at;
	}

}
