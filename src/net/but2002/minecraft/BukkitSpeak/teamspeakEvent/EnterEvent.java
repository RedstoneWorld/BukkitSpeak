package net.but2002.minecraft.BukkitSpeak.teamspeakEvent;

import java.util.HashMap;

import org.bukkit.entity.Player;

import net.but2002.minecraft.BukkitSpeak.BukkitSpeak;

public class EnterEvent extends TeamspeakEvent{
	
	public EnterEvent(HashMap<String, String> info) {
		super(Integer.valueOf(info.get("clid")));
		sendMessage();
	}
	
	protected void sendMessage() {
		String m = BukkitSpeak.getStringManager().getMessage("Join");
		if (m.isEmpty()) return;
		for (Player pl : getOnlinePlayers()) {
			if (!isMuted(pl) && checkPermissions(pl, "join")) {
				pl.sendMessage(replaceValues(m, true));
			}
		}
		if (BukkitSpeak.getStringManager().getLogInConsole()) BukkitSpeak.log().info(replaceValues(m, false));
	}
}
