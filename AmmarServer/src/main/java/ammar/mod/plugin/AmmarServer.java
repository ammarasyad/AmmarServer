package ammar.mod.plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_10_R1.CraftServer;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftFallingSand;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftThrownPotion;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.projectiles.BlockProjectileSource;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;

import local.thehutman.worldgen.WorldGen;
import net.minecraft.server.v1_10_R1.ChatComponentText;
import net.minecraft.server.v1_10_R1.ChatMessage;
import net.minecraft.server.v1_10_R1.CommandAbstract;
import net.minecraft.server.v1_10_R1.EntityLiving;
import net.minecraft.server.v1_10_R1.IChatBaseComponent;
import net.minecraft.server.v1_10_R1.PacketPlayOutChat;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

@SuppressWarnings({ "unused", "static-access" })
public final class AmmarServer extends JavaPlugin implements Listener, Runnable, PluginMessageListener {

	public static final String version = "v0.1.0-SNAPSHOT";
	public static final String BUILDDATE = getBuildDate(AmmarServer.class);

	public static final DecimalFormat HEALTH_DECIMAL_FORMAT = new DecimalFormat("#.##");
	public static final String SH_MSG_COLOR = "\u00a77";
	public static final String SERVER_HEADER = "\u00a79%s> " + SH_MSG_COLOR;
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	private static final SimpleDateFormat BUILD = new SimpleDateFormat("dd MMM yyyy");

	private int errorsDetected;

	private boolean showDamageSummary = true;

	private static final boolean isSnapshot = isSnapshot();

	private int ticker;
	private Map<Player, List<Damage>> damages = new HashMap<>();

	@Override
	public void onEnable() {
		try {
			this.getLogger().info("AmmarServer plugin is loading!");
			this.getLogger().info("Plugin author: Ammar (ammar18)");
			this.getLogger().info("Plugin version: " + this.version);
			this.getLogger().info("Plugin build date: " + this.BUILDDATE);

			if (!"v1_10_R1".equals(Utils.getVersion2())) {
				errorsDetected++;
				throw new RuntimeException("This plugin does not support your Minecraft version (" + Utils.getVersion2()
						+ "). Either update or downgrade to v1_10_R1!");
			}
			if (isSnapshot) {
				this.getLogger()
						.warning("This plugin is in the snapshot phase! Any losses are not under my responsibility!");
			}

			// if (this.version.substring(8, 15) == "SNAPSHOT") {
			// this.getLogger().warning(
			// "THIS BUILD IS SNAPSHOT! ANY DATA LOSS, IRRECOVERABLE CRASH, AND
			// YOUR CAT DIED IS NOT UNDER MY RESPONSIBILITY!");
			// }

			// for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			//
			// }
			// getCommand("checkplayer").setExecutor(new
			// AmmarCommandExec(this));
			WorldGen.onEnable(this);
			Bukkit.getPluginManager().registerEvents(this, this);
			Bukkit.getMessenger().registerIncomingPluginChannel(this, "MC|Brand", this);
		} catch (Throwable e) {
			errorsDetected++;
			Utils.broke(e);
			this.getLogger().severe("Fatal error detected when loading the plugin! This plugin will be disabled.");
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	private static boolean isSnapshot() {
		if (version.substring(7, 15) == "SNAPSHOT") {
			return true;
		}
		return false;
	}

	@Override
	public void onDisable() {
		this.getLogger().info("Plugin is disabled.");
		this.getLogger().warning("Errors detected: " + errorsDetected);
	}

	private Player getPlayerByName(String s) {
		return Bukkit.getServer().getPlayer(s);
	}

	private Player getPlayerInArray(String[] astring, int index) {
		try {
			Player player = getPlayerByName(astring[index]);

			if (player == null) {
				throw new CommandException("That player (" + astring[index] + ") cannot be found");
			}

			return player;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new CommandException("Player not defined in argument " + (index + 1));
		}
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {
		if (label.equalsIgnoreCase("checkplayer")) {
			getPlayerInArray(args, 0);
			return true;
		}

		if (label.equalsIgnoreCase("sysinfo")) {
			if (sender instanceof BlockCommandSender) {
				return true;
			}

			validateOp(sender);
			String color = "\u00a77";
			sender.sendMessage("\u00a72--- System Information ---");
			List<String> props = new ArrayList<>();
			SystemInfo sys = new SystemInfo();
			HardwareAbstractionLayer hal = sys.getHardware();
			props.add(color + "Processor: \u00a7r" + hal.getProcessor().getLogicalProcessorCount() + "x "
					+ hal.getProcessor().getName());
			props.add(
					color + "RAM: \u00a7r" + Utils.freeOf(hal.getMemory().getAvailable(), hal.getMemory().getTotal()));
			OperatingSystem os = sys.getOperatingSystem();
			props.add(color + "Operating System: \u00a7r");
			props.add(color + " Name: \u00a7r" + os.getManufacturer() + " " + os.getFamily());
			props.add(color + " Version: \u00a7r" + os.getVersion());
			props.add(color + "Drives: ");

			for (OSFileStore f : sys.getHardware().getFileStores()) {
				if (!f.getName().isEmpty()) {
					props.add(color + " " + f.getName() + "\u00a7r: "
							+ Utils.freeOf(f.getUsableSpace(), f.getTotalSpace()));
				}
			}
			sender.sendMessage("\u00a72--- System Information ---");
			sender.sendMessage(props.toArray(new String[props.size()]));
			return true;
		}

		if (label.equalsIgnoreCase("freeze")) {
			senderAsPlayer(sender, "This command is disabled in order to prevent abuse");
			getPlayerInArray(args, 0).setWalkSpeed(0);
			return true;
		}

		if (label.equalsIgnoreCase("unfreeze")) {
			senderAsPlayer(sender, "This command is disabled in order to prevent abuse");
			// 0.2 for default speed
			// 0.1 for sneaking speed
			getPlayerInArray(args, 0).setWalkSpeed(0.2f);
			return true;
		}

		if (label.equalsIgnoreCase("banduration")) {
			validateOp(sender);
			List<String> allMatches = new ArrayList<>();
			Matcher m = Pattern.compile("\\d+\\s\\w+|in(de|)finite").matcher(Utils.buildString(args, 1));
			while (m.find()) {
				allMatches.add(m.group());
			}

			if (allMatches.contains("infinite") || allMatches.contains("indefinite") || args.length == 1) {
				Bukkit.getBanList(Type.NAME).addBan(getPlayerInArray(args, 0).getName(), "", null, null);
				msg(sender, "Banned player " + getPlayerInArray(args, 0) + " forever.");
			} else if (args.length > 1) {
				Date expires = new Date(System.currentTimeMillis());
				Calendar c2 = Calendar.getInstance();
				c2.setTime(expires);
				for (String i : allMatches) {
					String[] splt = i.split(" ");
					int val = Integer.parseInt(splt[0]);
					String toSwitch = splt[1].toLowerCase();
					if (toSwitch.endsWith("s"))
						toSwitch = toSwitch.substring(0, toSwitch.length() - 1);
					switch (toSwitch) {
					case "second":
						c2.add(Calendar.SECOND, val);
						break;
					case "minute":
						c2.add(Calendar.MINUTE, val);
						break;
					case "hour":
						c2.add(Calendar.HOUR, val);
						break;
					case "day":
						c2.add(Calendar.DATE, val);
						break;
					case "week":
						c2.add(Calendar.WEEK_OF_YEAR, val);
						break;
					case "month":
						c2.add(Calendar.MONTH, val);
						break;
					case "year":
						c2.add(Calendar.YEAR, val);
						break;
					default:
						throw new CommandException("What is " + toSwitch + "? I don't know.");
					}
				}
				Bukkit.getBanList(Type.NAME).addBan(getPlayerInArray(args, 0).getName(), "", c2.getTime(), null);

				if (getServer().getPlayer(getPlayerInArray(args, 0).getName()) != null) {
					getServer().getPlayer(getPlayerInArray(args, 0).getName())
							.kickPlayer("You are banned from the server.");
				}

				msg(sender, "Banned player " + getPlayerInArray(args, 0) + " until "
						+ new SimpleDateFormat().format(c2.getTime()));
			}
			return true;
		}

		if (label.equalsIgnoreCase("changeworld")) {
			senderAsPlayer(sender);
			String worldname = Utils.buildString(args, 0);
			String worldbefore = senderAsPlayer(sender).getWorld().getName();
			List<String> exworlds = Utils.getExistingWorlds();

			if (!exworlds.contains(worldname)) {
				msg(sender, "\u00a7cThat world doesn't exist. Existing worlds are:");
				msg(sender, Utils.joinNiceString(exworlds.toArray(new String[exworlds.size()])));
				return true;
			}

			msg(sender, "Teleporting you to world \u00a7l" + worldname);
			World w = getServer().getWorld(worldname);

			if (w == null) {
				getServer().getWorlds().add(w = getServer().createWorld(new WorldCreator(worldname)));
			}

			senderAsPlayer(sender).teleport(new Location(w, w.getSpawnLocation().getX(), w.getSpawnLocation().getY(),
					w.getSpawnLocation().getZ()));
			msg(sender,
					"WARNING: This feature is very experimental. The scoreboard in " + worldbefore
							+ " are mixed with the scoreboard in " + worldname
							+ ". So don't try to teleport to lots-of-command-blocks maps!");
			return true;
		}

		/*
		 * if (label.equalsIgnoreCase("zeus")) { Player player =
		 * senderAsPlayer(sender); PlayerInventory inv = player.getInventory();
		 * ItemStack itemstack = new ItemStack(Material.BLAZE_ROD);
		 * 
		 * if (!inv.contains(itemstack)) { inv.addItem(itemstack);
		 * player.sendMessage("\u00A7eSeems you wanna be like Zeus!"); } else {
		 * player.sendMessage("\u00A7cWhoops! You already have it!"); }
		 * 
		 * return true; }
		 */
		return false;
	}

	public List<String> onTabComplete(CommandSender a, Command b, String c, String[] args) {
		try{
			switch(b.getName().toLowerCase()) {
			case"changeworld":
				return CommandAbstract.a(args, Utils.getExistingWorlds());
			}
		}catch(Throwable e) {
			Utils.broke(e);
		}
		
		return Collections.emptyList();
	}
	
	public static void sandbox() {
		throw new CommandException(
				"This command is very risky, extra extra extra experimental, and a catastrophic failure will occur in this world upon executing this command. If you want to try this command, install this plugin outside this server.");
	}

	private static Player senderAsPlayer(CommandSender commandsender) {
		return senderAsPlayer(commandsender, "Only players can execute this command");
	}

	private static Player senderAsPlayer(CommandSender commandsender, String s) {
		if (!(commandsender instanceof Player)) {
			throw new CommandException(s);
		}

		return (Player) commandsender;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer(); // player yg join pasti online lah
		p.sendMessage("Welcome to Ammar's Server! This plugin is very stubborn, so"
				+ " if you found bugs, please report to the admins!");
	}

	/*
	 * @EventHandler public void onPlayerInteractBlock(PlayerInteractEvent e) {
	 * Player p = e.getPlayer(); if (p.getItemInHand().getType() ==
	 * Material.BLAZE_ROD) {
	 * p.getWorld().strikeLightning(p.getTargetBlock((Set<Material>) null,
	 * 200).getLocation()); } }
	 */

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntityType() != EntityType.PLAYER || !showDamageSummary) {
			return;
		}

		Player p = (Player) e.getEntity();
		List<Damage> toSet = damages.containsKey(e) ? damages.get(e) : new ArrayList<Damage>();
		Damage d = new Damage(e);
		toSet.add(d);
		damages.put(p, toSet);

		try {
			((CraftPlayer) e.getEntity()).getHandle().playerConnection
					.sendPacket(new PacketPlayOutChat(damageSummary(d), (byte) 2));
		} catch (Throwable e1) {
			Utils.broke(e1);
		}
	}

	public void run() {
		try {
			long idleThreshold = 120;
			long idleThresholdMs = idleThreshold * 1000 * 60;
			for (Player i : Bukkit.getOnlinePlayers()) {
				Object nmsS1 = Utils.getHandle(i);
				long lastActive = (long) nmsS1.getClass().getMethod("I").invoke(nmsS1);

				if (lastActive > 0L && idleThreshold > 0) {
					long notActive = System.currentTimeMillis() - lastActive;
					long timeUntilKick = (idleThresholdMs - notActive) / 1000 + 1;

					if (timeUntilKick < 11 && idleThresholdMs - notActive >= 0) {
						i.sendMessage(String.format(
								ChatColor.GOLD.toString() + ChatColor.BOLD + "You will be kicked in %d second%s",
								timeUntilKick, plural((int) timeUntilKick)));
						i.playSound(i.getLocation(), "minecraft:block.note.pling", 3e7f,
								(timeUntilKick / 10f * 1.5f) + .5f);
					}

					if (notActive > idleThresholdMs) {
						i.kickPlayer(
								String.format("AFKing for %d minute%s", idleThreshold, plural((int) idleThreshold)));
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			getLogger().severe("Error caught in afk_kicker.exe, cancelling!");
			Bukkit.getScheduler().cancelTask(ticker);
		}
	}

	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfeces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfeces) {
				List<InetAddress> add = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : add) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress();

						boolean isIPv4 = sAddr.indexOf(':') < 0;

						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%');
								return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
							}
						}
					}
				}
			}
		} catch (Throwable e) {
		}
		return "";
	}

	private Object plural(int timeUntilKick) {
		return null;
	}

	public PlayerNameHistory[] getPNameHistory(UUID u) throws Exception {
		String raw = Utils
				.readUrl("https://api.mojang.com/user/profiles/" + u.toString().replaceAll("-", "") + "/names");
		if (raw.length() == 0) {
			throw new RuntimeException("The server responded nothing. Try deleting the user cache.");
		}
		return new Gson().fromJson(raw, PlayerNameHistory[].class);
	}

	public void listPNameHistory(final CommandSender sender, final String string) {
		new Thread(new Runnable() {
			public void run() {
				if (!Bukkit.getOnlineMode()) {
					msg(sender, "\u00a7cThis server must be in online mode in order for this to work!");
					return;
				}

				msg(sender, "\u00a72Fetching player name history of \u00a7l" + string + "\u00a7r...");
				GameProfile game = ((CraftServer) Bukkit.getServer()).getHandle().getServer().getUserCache()
						.getProfile(string);

				if (game == null) {
					msg(sender, "\u00a7cNon-existent player: " + string);
					return;
				}

				int c = 1;

				try {
					UUID u = game.getId();
					msg(sender, string + "'s UUID is" + u);

					for (PlayerNameHistory i : getPNameHistory(u)) {
						String d = "\u00a76" + c++ + ".\u00a7r \u00a7e" + i.name + "\u00a7r";
						d += (i.changedToAt > 0 ? "on \u00a7e" + SDF.format(i.changedToAt) : "initial name");
						sender.sendMessage(d);
					}
				} catch (Throwable e) {
					Utils.broke(e);
				}
			}
		}).start();
		;
	}

	private IChatBaseComponent damageSummary(Damage d, boolean fromMe) throws Exception {
		EntityDamageEvent e = d.a;
		IChatBaseComponent icbc = new ChatComponentText(String.format("\u00a76%s:\u00a7r",
				StringUtils.capitalize(e.getCause().toString().toLowerCase().replaceAll("_", " "))));

		if (e instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent e1 = (EntityDamageByEntityEvent) e;
			Entity damager = ((EntityDamageByEntityEvent) e).getDamager();
			String fromto = "\u00a77" + (fromMe ? "to" : "from") + "\u00a7r";
			boolean flag = true;

			if (e.getCause() == DamageCause.PROJECTILE && ((Projectile) damager).getShooter() != null) {
				Projectile p = (Projectile) damager;
				icbc.addSibling(new ChatMessage(" %s%s", ((CraftEntity) damager).getHandle().getScoreboardDisplayName(),
						damager instanceof TippedArrow
								? new ChatComponentText(" ").addSibling(testArrow((TippedArrow) damager)) : ""));

				if (p.getShooter() instanceof Entity) {
					damager = (Entity) p.getShooter();
				} else if (p.getShooter() instanceof BlockProjectileSource) {
					icbc.addSibling(
							new ChatComponentText(" \u00a77from a \u00a7r " + ((BlockProjectileSource) p.getShooter())
									.getBlock().getType().toString().toLowerCase().replaceAll("_", " ")));
					flag = false;
				} else {
					flag = false;
				}
			}

			if (e.getCause() == DamageCause.MAGIC) {
				if (damager instanceof ThrownPotion) {
					icbc.addSibling(
							new ChatMessage(" %s %s", ((CraftEntity) damager).getHandle().getScoreboardDisplayName(),
									((CraftThrownPotion) damager).getHandle().getItem().B()));
					flag = false;

					if (((Projectile) damager).getShooter() != null) {
						if (((Projectile) damager).getShooter() instanceof Entity) {
							damager = (Entity) ((Projectile) damager).getShooter();
							flag = true;
						} else {
							flag = false;
						}
					}
				}
			}

			if (damager instanceof AreaEffectCloud) {
				if (((AreaEffectCloud) damager).getSource() != null) {
					icbc.addSibling(
							new ChatMessage(" %s", ((CraftEntity) damager).getHandle().getScoreboardDisplayName()));
					damager = (Entity) ((AreaEffectCloud) damager).getSource();
				}
			}

			Entity fromto1 = fromMe ? e.getEntity() : damager;

			if (flag) {
				icbc.addSibling(new ChatMessage(" %s %s", fromto, damager != e.getEntity()
						? ((CraftEntity) fromto1).getHandle().getScoreboardDisplayName() : "you"));
			}
			if (e.getCause() == DamageCause.FALLING_BLOCK) {
				if (damager instanceof FallingBlock) {
					icbc.addSibling(new ChatMessage(" (%s)",
							((CraftFallingSand) damager).getHandle().getBlock().getBlock().getName()));
				}
			}

			if (damager instanceof LivingEntity) {
				EntityLiving el = ((CraftLivingEntity) damager).getHandle();
				net.minecraft.server.v1_10_R1.ItemStack byItem = el.getItemInMainHand();

				if (byItem != null) {
					icbc.addSibling(new ChatMessage("\u00a77using\u00a7r %s", byItem.B()));
				}
			}
		}

		icbc.addSibling(new ChatComponentText("\u00a7c-" + HEALTH_DECIMAL_FORMAT.format(e.getFinalDamage())));
		long l = System.currentTimeMillis() - d.b;

		if (l >= 1000) {
			icbc.addSibling(new ChatComponentText(
					String.format(" \u00a79%ss", HEALTH_DECIMAL_FORMAT.format((double) l / 1000D))));
		}

		return icbc;
	}

	private IChatBaseComponent damageSummary(Damage d) throws Exception {
		return damageSummary(d, false);
	}

	private static IChatBaseComponent testArrow(TippedArrow e) throws Exception {
		Object nmsS1 = Utils.getHandle(e);
		Method m = nmsS1.getClass().getDeclaredMethod("j");
		m.setAccessible(true);
		return ((net.minecraft.server.v1_10_R1.ItemStack) m.invoke(nmsS1)).B();
	}

	private static class Damage {
		private EntityDamageEvent a;
		private long b = System.currentTimeMillis();

		public Damage(EntityDamageEvent a) {
			this.a = a;
		}
	}

	public static void msg(CommandSender a, String b, String c) {
		a.sendMessage(String.format(SERVER_HEADER, c) + b);
	}

	public static void msg(CommandSender a, String s) {
		msg(a, s, "Server");
	}

	private void validateOp(CommandSender a) {
		if (!a.isOp()) {
			throw new CommandException("You do not have sufficient privileges to execute this command.");
		}
	}

	public static String getBuildDate(Class<?> c) {
		try {
			File f = new File(c.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			return BUILD.format(f.lastModified());
		} catch (Exception e) {
			return "0";
		}
	}

	public String getVersion() {
		return this.version;
	}

	@Override
	public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
		if (s.equals("MC|Brand")) {
			String brand = new String(bytes).substring(1);

			if (!brand.equals("vanilla")) {
				player.sendMessage(ChatColor.GRAY
						+ "We've detected that you're using a modded version of Minecraft, which is +" + brand
						+ "+ . You're allowed to use mods as long it doesn't involve hacking, griefing, scamming, etc!");
			}
		}
	}

	private static class PlayerNameHistory {
		String name = "Player";
		long changedToAt = 0;
	}

	// @SuppressWarnings("deprecation")
	// @EventHandler
	// public void onPlayerDeath(PlayerDeathEvent e) {
	// Player p = e.getEntity();
	// Player pKill = p.getKiller();
	//
	// if(pKill instanceof Player) {
	// p.sendMessage("You died by the hands of " + pKill.getDisplayName() + " by
	// using" + pKill.getItemInHand().toString());
	// }else if (!(pKill instanceof Player)){
	// p.sendMessage("You died by a creature type " +
	// e.getEntityType().toString());
	// }
	// }

	@EventHandler
	public void a(EntityDamageByEntityEvent a) {
		if (a.getDamager().getType() != EntityType.PLAYER && !(a.getDamager() instanceof Projectile))
			return;
		try {
			Entity dmg = a.getDamager() instanceof Projectile && ((Projectile) a.getDamager()).getShooter() != null
					&& ((Projectile) a.getDamager()).getShooter() instanceof Entity
							? (Entity) ((Projectile) a.getDamager()).getShooter() : a.getDamager();
			// System.out.println(((Projectile) a.getDamager()).getShooter());
			if (!(dmg instanceof Player))
				return;
			// msg(dmg, ((CraftEntity) a.getEntity()).getHandle().toString());
			IChatBaseComponent c = new ChatComponentText("\u00a7b<-- ").addSibling(damageSummary(new Damage(a), true))
					.addSibling(new ChatComponentText(" \u00a7b-->"));
			((CraftPlayer) dmg).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(c, (byte) 2));
		} catch (Throwable ex) {
			Utils.broke(ex);
		}
	}

	@EventHandler
	public void a(SignChangeEvent a) {
		int i = 0;
		for (String s : a.getLines())
			a.setLine(i++, ChatColor.translateAlternateColorCodes('&', s));
	}
}