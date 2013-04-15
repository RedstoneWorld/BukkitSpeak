package net.but2002.minecraft.BukkitSpeak.TeamspeakCommands;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.but2002.minecraft.BukkitSpeak.BukkitSpeak;
import net.but2002.minecraft.BukkitSpeak.AsyncQueryUtils.QuerySender;
import net.but2002.minecraft.BukkitSpeak.Commands.BukkitSpeakCommand;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;

public class TeamspeakCommandSender implements CommandSender {
	
	private final boolean operator;
	private final PermissibleBase permissions;
	private final Map<String, String> client;
	private final String name;
	private final List<String> outBuffer;
	
	private BufferSender outSender;
	
	public TeamspeakCommandSender(Map<String, String> clientInfo, boolean op, Map<String, Boolean> perms) {
		client = clientInfo;
		name = client.get("client_nickname");
		outBuffer = Collections.synchronizedList(new LinkedList<String>());
		operator = op;
		
		permissions = new PermissibleBase(null);
		for (Map.Entry<String, Boolean> e : perms.entrySet()) {
			permissions.addAttachment(BukkitSpeak.getInstance(), e.getKey(), e.getValue());
		}
	}
	
	@Override
	public boolean isPermissionSet(String name) {
		return permissions.isPermissionSet(name);
	}
	
	@Override
	public boolean isPermissionSet(Permission perm) {
		return permissions.isPermissionSet(perm);
	}
	
	@Override
	public boolean hasPermission(String name) {
		return permissions.hasPermission(name);
	}
	
	@Override
	public boolean hasPermission(Permission perm) {
		return permissions.hasPermission(perm);
	}
	
	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return permissions.addAttachment(plugin, name, value);
	}
	
	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return permissions.addAttachment(plugin);
	}
	
	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return permissions.addAttachment(plugin, name, value, ticks);
	}
	
	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return permissions.addAttachment(plugin, ticks);
	}
	
	@Override
	public void removeAttachment(PermissionAttachment attachment) {
		permissions.removeAttachment(attachment);
	}
	
	@Override
	public void recalculatePermissions() {
		permissions.recalculatePermissions();
	}
	
	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return permissions.getEffectivePermissions();
	}
	
	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}
	
	@Override
	public boolean isOp() {
		return operator;
	}
	
	@Override
	public void setOp(boolean op) {
		throw new UnsupportedOperationException("A TeamspeakCommandSender's OP status cannot be changed.");
	}
	
	@Override
	public String getName() {
		return BukkitSpeak.getStringManager().getTeamspeakNamePrefix() + name;
	}
	
	@Override
	public void sendMessage(String message) {
		if (message == null) return;
		if (!BukkitSpeak.getQuery().isConnected()) return;
		
		outBuffer.add(format(message));
		startBuffer();
	}
	
	@Override
	public void sendMessage(String[] messages) {
		if (messages == null || messages.length == 0) return;
		
		for (int i = 0; i < messages.length; i++) {
			outBuffer.add(format(messages[i]));
		}
		
		startBuffer();
	}
	
	private String format(String s) {
		//TODO: Format message?
		return BukkitSpeakCommand.convertToTeamspeak(s, false, true);
	}
	
	private void startBuffer() {
		if (outSender == null || outSender.isDone()) {
			outSender = new BufferSender(outBuffer, client);
			Bukkit.getScheduler().runTaskLater(BukkitSpeak.getInstance(), outSender, 
					BukkitSpeak.getStringManager().getTeamspeakCommandSenderBuffer());
		}
	}
}

class BufferSender implements Runnable {
	
	private final List<String> buffer;
	private final int clid;
	
	private boolean done;
	
	public BufferSender(List<String> outBuffer, Map<String, String> client) {
		buffer = outBuffer;
		clid = Integer.valueOf(client.get("clid"));
	}
	
	public void run() {
		StringBuilder sb = new StringBuilder();
		
		for (String message = buffer.remove(0); buffer.size() > 0; message = "\n" + buffer.remove(0)) {
			if (sb.length() + message.length() < 1024) {
				sb.append(message);
			} else {
				sendToTeamspeak(sb.toString());
				sb = new StringBuilder(message.substring(2));
			}
		}
		sendToTeamspeak(sb.toString());
		
		setDone();
	}
	
	public boolean isDone() {
		return done;
	}
	
	private void sendToTeamspeak(String message) {
		QuerySender qs = new QuerySender(clid, JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, message);
		Bukkit.getScheduler().runTaskAsynchronously(BukkitSpeak.getInstance(), qs);
	}
	
	private void setDone() {
		done = true;
	}
}