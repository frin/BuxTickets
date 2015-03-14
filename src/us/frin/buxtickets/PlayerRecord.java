package us.frin.buxtickets;

public class PlayerRecord {
	// Class for holding data retrieved from MySQL table
	public String uuid;
	public String name;
	
	public PlayerRecord(String uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}
}
