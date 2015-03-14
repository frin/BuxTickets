package us.frin.buxtickets;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.frin.buxtickets.commands.BuxTicketsCommands;

public class NotifierThread extends Thread {
	private Boolean stop = false;
	private Long interval = 300000L; // 5 minutes
    private BuxTickets plugin = null;

    public NotifierThread(BuxTickets owner) {
    	plugin = owner;
    }

    // This method is called when the thread runs
    public void run() {
    	System.out.println("[BuxTickets] Notification Thread Started");

    	while (stop == false) {
    		// Go to sleep
    		BuxTicketsCommands cmds = new BuxTicketsCommands(plugin);
    		
//    		int total = 0;
    		for (Player player : this.plugin.getServer().getOnlinePlayers()) {
				int open = cmds.getOpenTicketCount(player);
				if (open > 0) {
					player.sendMessage(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "You have " + open + " ticket" + (open > 1 ? "s" : "") + " waiting, use '/ticket list' to review");
				}
//				total += open;
			}

    		ArrayList<String> skip = new ArrayList<String>();
    		
    		int totalReal = cmds.getOpenTicketCountAll();
    		if (totalReal > 0) {
        		cmds.notifyModerators(ChatColor.DARK_PURPLE + "[BuxTickets] " + ChatColor.GRAY + "There " + (totalReal > 1 ? "are" : "is") + " " + totalReal + " open ticket" + (totalReal > 1 ? "s" : "") + " waiting,  use '/ticket list' to review", skip);
    		}

    		try {
    			sleep(interval);
    		}
    		catch (InterruptedException e) {
    			System.out.println("[BuxTickets] Notification Thread Sleep Interrupted");
    		}
    	}
    	System.out.println("[BuxTickets] Notifier Thread Stopped");
    }

    public void signalStop() {
    	stop = true;
    	System.out.println("[BuxTickets] Notifier Thread set to stop");
    }

    public void setInterval(Integer sec) {
    	Long ms = sec * 1000L;        
    	if (ms < 30000) { ms = 30000L; }
    	interval = ms;
    	System.out.println("[BuxTickets] Notifier Thread interval set to " + sec + " seconds");
    }
}