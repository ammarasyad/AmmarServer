package ammar.mod.plugin;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.v1_10_R1.CraftServer;
import org.bukkit.craftbukkit.v1_10_R1.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.v1_10_R1.command.ProxiedNativeCommandSender;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftMinecartCommand;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.server.v1_10_R1.DedicatedServer;
import net.minecraft.server.v1_10_R1.ICommandListener;
import net.minecraft.server.v1_10_R1.MinecraftServer;
import net.minecraft.server.v1_10_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_10_R1.NBTTagCompound;

@SuppressWarnings({ "unused", "resource" })
public class Utils {
	private static final Map<Class<?>, Class<?>> CORRESPONDING_TYPES = new HashMap<>();

	private static int BUFFER = 32768;

	public static void a(Player a, File b) {
		String msgHead = "Preview";
		a.sendMessage("\u00a76\u00a7l--- " + b.getName() + " ---");
		try {
			switch (FilenameUtils.getExtension(b.getName()).toUpperCase()) {
			case "NBT":
			case "DAT":
			case "SCHEMATIC":
				AmmarServer.msg(a, "\u00a7oAttempting to read this file as NBT", msgHead);
				try {
					FileInputStream nbtfis = new FileInputStream(b);
					NBTTagCompound root = NBTCompressedStreamTools.a(nbtfis);
					nbtfis.close();
					a.sendMessage(root.toString());
				} catch (ZipException e) {
					AmmarServer.msg(a, "\u00a7oNot an NBT file!", msgHead);
				}
				break;
			case "JSON":
			case "MCMETA":
				AmmarServer.msg(a, "\u00a7oAttempting to read this file as JSON", msgHead);
				JsonParser p = new JsonParser();
				FileReader f = new FileReader(b);
				JsonElement j = p.parse(f);
				f.close();
				Gson g = new GsonBuilder().setPrettyPrinting().create();
				a.sendMessage(g.toJson(j));
				break;
			default:
				AmmarServer.msg(a, "\u00a7oAttempting to read this file as text", msgHead);
				try (BufferedReader br = new BufferedReader(new FileReader(b))) {
					String cl;
					while ((cl = br.readLine()) != null)
						a.sendMessage(cl);
					br.close();
				}
				break;
			}
		} catch (Throwable e) {
			broke(e);
		}
	}

	public static void actionBar(Player a, String b) {
		try {
			Class<?> icbc = getNMSClass("IChatBaseComponent");
			Object handle = getHandle(a);
			Object connection = getField(handle.getClass(), "playerConnection").get(handle);
			Method sendPacket = getMethod(connection.getClass(), "sendPacket");
			Object ser = getNMSClass("ChatComponentText").getConstructor(String.class)
					.newInstance(b.replaceAll("&", "\u00a7"));
			Object pkt = getNMSClass("PacketPlayOutChat").getConstructor(icbc, Byte.TYPE).newInstance(ser, (byte) 2);
			sendPacket.invoke(connection, pkt);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static String actionBoxJson(String a, String b) {
		return actionBoxJson(a, b, false);
	}

	public static String actionBoxJson(String a, String b, boolean c) {
		return clickBoxJson(a, b, "suggest_command", c);
	}

	public static String clickBoxJson(String a, String b, String c) {
		return clickBoxJson(a, b, c, false);
	}

	public static String clickBoxJson(String a, String b, String c, boolean d) {
		return String.format("{\"text\":\"[%s]\",\"clickEvent\":{\"action\":\"%s\",\"value\":\"%s\"}}%s", a, c, b,
				d ? "{\"text\":\" \"}" : "");
	}

	public static void beepOnceNormalPitch(CommandSender a, JavaPlugin c) {
		if (!(a instanceof Player))
			return;
		Player asPlayer = (Player) a;
		asPlayer.playSound(asPlayer.getLocation(), "minecraft:block.note.pling", 3.0f, 1);
	}

	public static void broke(Throwable e) {
		broke(e, "");
	}

	public static void broke(Throwable e, String string) {
		// if(AmmarServer.verbose)
		Bukkit.broadcastMessage(
				"§4§l§ka§4§l>>§r    §c§lOh noes! I've caught an error " + string + "!§r    §4§l<<§ka");
		Bukkit.broadcastMessage("\u00a7c" + e.toString());

		for (StackTraceElement i : e.getStackTrace()) {
			String string1 = i.getClassName();
			String fcl = string1.substring(0, string1.lastIndexOf('.') + 1) + "\u00a7f"
					+ string1.substring(string1.lastIndexOf('.') + 1);
			String cls = "{\"text\":\"" + string1.substring(string1.lastIndexOf('.') + 1)
					+ "\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"\u00a77" + fcl + "\"}}";
			String str = "\u00a7r" + (i.isNativeMethod() ? "(\u00a7oNative Method\u00a7r)"
					: (i.getFileName() != null && i.getLineNumber() >= 0
							? "(\u00a7b" + i.getFileName() + "\u00a7r:\u00a73" + i.getLineNumber() + "\u00a7r)"
							: (i.getFileName() != null ? "(\u00a7b" + i.getFileName() + "\u00a7r)"
									: "(\u00a7oUnknown Source\u00a7r)")));

			// if(AmmarServer.verbose)
			bcmJson("[{\"text\":\"\u00a77at \"}," + cls + ",{\"text\":\".\u00a7a" + i.getMethodName() + str + "\"}]");
			Bukkit.getConsoleSender()
					.sendMessage("\u00a77     at " + fcl + "\u00a7r.\u00a7a" + i.getMethodName() + str);
		}

		// if(!AmmarServer.verbose) {
		// bcmJson("{\"text\ ":\"An error occured.\"}");
		// }
	}

	public static ItemStack applyName(ItemStack a, String b) {
		ItemMeta im = a.getItemMeta();
		im.setDisplayName(b);
		a.setItemMeta(im);
		return a;
	}

	public static String freeOf(long available, long total) {
		long used = total - available;
		return String.format("%s free of %s (%d%% used)", Utils.formatFileSize(available), Utils.formatFileSize(total),
				(int) ((double) used / (double) total * 100D));
	}

	private static void bcmJson(String string) {
		try {
			Class<?> icbc = getNMSClass("IChatBaseComponent$ChatSerializer");
			Object ser = icbc.getMethod("a", String.class).invoke(icbc.newInstance(), string);
			Object pkt = getNMSClass("PacketPlayOutChat").getConstructor(getNMSClass("IChatBaseComponent"))
					.newInstance(ser);
			Object nmsS1 = getHandle(Bukkit.getServer());
			nmsS1.getClass().getMethod("sendAll", getNMSClass("Packet")).invoke(nmsS1, pkt);
			/*
			 * for (Player a : Bukkit.getOnlinePlayers()) { Object handle =
			 * getHandle(a); Object connection = getField(handle.getClass(),
			 * "playerConnection").get(handle); Method sendPacket =
			 * getMethod(connection.getClass(), "sendPacket");
			 * sendPacket.invoke(connection, pkt); }
			 */
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static String buildString(String[] a, int b) {
		StringBuilder sb = new StringBuilder();
		for (int i = b; i < a.length; ++i) {
			if (i > b)
				sb.append(" ");
			String s = a[i];
			sb.append(s);
		}
		String res = sb.toString();
		return res.substring(0, res.length() - (res.endsWith(" ") ? 1 : 0));
	}

	public static File createUniqueCopyName(File path, String fileName) {
		File file = getFile(path, fileName);
		if (!file.exists())
			return file;
		return createUniqueCopyName(path,
				FilenameUtils.removeExtension(fileName) + " - Copy." + FilenameUtils.getExtension(fileName));
	}

	public static boolean equalsTypeArray(Class<?>[] a, Class<?>[] o) {
		if (a.length != o.length)
			return false;
		for (int i = 0; i < a.length; i++)
			if (!a[i].equals(o[i]) && !a[i].isAssignableFrom(o[i]))
				return false;
		return true;
	}

	public static String fancyTime(long l, boolean simple) {
		return DurationFormatUtils.formatDurationWords(l, true, true);
	}

	public static int floor(double a) {
		int i = (int) a;
		return a < i ? i - 1 : i;
	}

	public static String formatFileSize(long sizeBytes) {
		final BytesResult res = formatBytes(sizeBytes, 0);
		return String.format("%1$s, %2$s", res.val, res.un);
	}

	public static final long KB_IN_BYTES = 1024;
	public static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
	public static final long GB_IN_BYTES = MB_IN_BYTES * 1024;
	public static final long TB_IN_BYTES = GB_IN_BYTES * 1024;
	public static final long PB_IN_BYTES = TB_IN_BYTES * 1024;
	public static final int FLAG_SHORTER = 1 << 0;
	public static final int FLAG_CALCULATE_ROUNDED = 1 << 1;

	public static BytesResult formatBytes(long sizeBytes, int flags) {
		final boolean isNegative = (sizeBytes < 0);
		float result = isNegative ? -sizeBytes : sizeBytes;
		String suffix = "B";
		long mult = 1;
		if (result > 900) {
			suffix = "KB";
			mult = KB_IN_BYTES;
			result = result / 1024;
		}
		if (result > 900) {
			suffix = "MB";
			mult = MB_IN_BYTES;
			result = result / 1024;
		}
		if (result > 900) {
			suffix = "GB";
			mult = GB_IN_BYTES;
			result = result / 1024;
		}
		if (result > 900) {
			suffix = "TB";
			mult = TB_IN_BYTES;
			result = result / 1024;
		}
		if (result > 900) {
			suffix = "PB";
			mult = PB_IN_BYTES;
			result = result / 1024;
		}

		final int roundFactor;
		final String roundFormat;
		if (mult == 1 || result >= 100) {
			roundFactor = 1;
			roundFormat = "%.0f";
		} else if (result < 1) {
			roundFactor = 100;
			roundFormat = "%.2f";
		} else if (result < 10) {
			if ((flags & FLAG_SHORTER) != 0) {
				roundFactor = 10;
				roundFormat = "%.1f";
			} else {
				roundFactor = 100;
				roundFormat = "%.2f";
			}
		} else { // 10 <= result < 100
			if ((flags & FLAG_SHORTER) != 0) {
				roundFactor = 1;
				roundFormat = "%.0f";
			} else {
				roundFactor = 100;
				roundFormat = "%.2f";
			}
		}
		if (isNegative) {
			result = -result;
		}
		final String roundedString = String.format(roundFormat, result);
		// Note this might overflow if abs(result) >= Long.MAX_VALUE / 100, but
		// that's like 80PB so
		// it's okay (for now)...
		final long roundedBytes = (flags & FLAG_CALCULATE_ROUNDED) == 0 ? 0
				: (((long) Math.round(result * roundFactor)) * mult / roundFactor);
		return new BytesResult(roundedString, suffix, roundedBytes);
	}

	public static class BytesResult {
		public final String val;
		public final String un;
		public final long roundedBytes;

		public BytesResult(String val, String un, long rounded) {
			this.val = val;
			this.un = un;
			this.roundedBytes = rounded;
		}
	}

	public static File getCopyFile(File f) {
		// if (!f.isFile()) throw new IllegalArgumentException("The file is a
		// folder!");
		if (f.exists())
			return new File(f.getParentFile(), "Copy of " + f.getName());
		else
			return f;
	}

	public static Field getField(Class<?> clazz, String name) {
		try {
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public static File getFile(File curdir, String file) {
		return getFile(curdir.getAbsolutePath(), file);
	}

	private static File getFile(String curdir, String file) {
		String separator = "/";
		if (curdir.endsWith("/"))
			separator = "";
		return new File(curdir + separator + file);
	}

	public static Object getHandle(Object obj) {
		try {
			return getMethod("getHandle", obj.getClass()).invoke(obj);
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Method getMethod(Class<?> clazz, String name, Class<?>... args) {
		for (Method m : clazz.getMethods())
			if (m.getName().equals(name) && (args.length == 0 || ClassListEqual(args, m.getParameterTypes()))) {
				m.setAccessible(true);
				return m;
			}
		return null;
	}

	public static boolean ClassListEqual(Class<?>[] l1, Class<?>[] l2) {
		boolean equal = true;
		if (l1.length != l2.length)
			return false;
		for (int i = 0; i < l1.length; i++)
			if (l1[i] != l2[i]) {
				equal = false;
				break;
			}
		return equal;
	}

	public static Method getMethod(String name, Class<?> clazz, Class<?>... paramTypes) {
		Class<?>[] t = toPrimitiveTypeArray(paramTypes);
		for (Method m : clazz.getMethods()) {
			Class<?>[] types = toPrimitiveTypeArray(m.getParameterTypes());
			if (m.getName().equals(name) && equalsTypeArray(types, t))
				return m;
		}
		return null;
	}

	public static Class<?> getNMSClass(String className) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + getVersion() + className);
	}

	public static List<String> getExistingWorlds() {
		List<String> res = new ArrayList<>();
		for (File i : Bukkit.getWorldContainer().listFiles()) {
			if (i.isDirectory() && !i.getName().endsWith("_nether") && !i.getName().endsWith("_the_end"))
				for (String j : i.list())
					if (j.equals("level.dat")) {
						res.add(i.getName());
						break;
					}
			return res;
		}
		return res;
	}

	public static Class<?> getOBCClass(String className) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + getVersion() + className);
	}

	public static Class<?> getPrimitiveType(Class<?> clazz) {
		return CORRESPONDING_TYPES.containsKey(clazz) ? CORRESPONDING_TYPES.get(clazz) : clazz;
	}

	public static String getVersion() {
		String name = Bukkit.getServer().getClass().getPackage().getName();
		return name.substring(name.lastIndexOf('.') + 1) + ".";
	}

	public static String getVersion2() {
		String name = Bukkit.getServer().getClass().getPackage().getName();
		return name.substring(name.lastIndexOf('.') + 1);
	}

	public static String joinNiceString(Object[] a) {
		StringBuilder v0 = new StringBuilder();
		for (int i = 0; i < a.length; ++i) {
			String s = a[i].toString();
			if (i > 0) {
				if (i == a.length - 1)
					v0.append((a.length > 2 ? "," : "") + " and ");
				else
					v0.append(", ");
			}
			v0.append(s);
		}
		return v0.toString();
	}

	public static String joinNiceStringFromCollection(Collection<String> strings) {
		return joinNiceString(strings.toArray(new String[strings.size()]));
	}

	public static void jsonMsg(Player a, String b) {
		jsonMsg(a, b, true);
	}

	public static void jsonMsg(Player a, String b, boolean c) {
		try {
			Class<?> icbc = getNMSClass("IChatBaseComponent$ChatSerializer");
			Object handle = getHandle(a);
			Object connection = getField(handle.getClass(), "playerConnection").get(handle);
			Method sendPacket = getMethod(connection.getClass(), "sendPacket");
			Object ser = icbc.getMethod("a", String.class).invoke(icbc.newInstance(),
					c ? String.format("[{\"text\":\"%s\"},%s]", String.format(AmmarServer.SERVER_HEADER, "Server"), b)
							: b);
			Object pkt = getNMSClass("PacketPlayOutChat").getConstructor(getNMSClass("IChatBaseComponent"))
					.newInstance(ser);
			sendPacket.invoke(connection, pkt);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static String readUrl(String urlString) throws Exception {
		BufferedReader reader = null;
		try {
			URL url = new URL(urlString);
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuffer buffer = new StringBuffer();
			int read;
			char[] chars = new char[1024];
			while ((read = reader.read(chars)) != -1)
				buffer.append(chars, 0, read);
			return buffer.toString();
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	public static String sha1(String input) throws NoSuchAlgorithmException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] result = mDigest.digest(input.getBytes());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < result.length; i++)
			sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
		return sb.toString();
	}

	public static void tabHeaderFooter(Player player, String s, String s1) {
		try {
			Class<?> icbc = getNMSClass("IChatBaseComponent");
			Object handle = getHandle(player);
			Object connection = getField(handle.getClass(), "playerConnection").get(handle);
			Method sendPacket = getMethod(connection.getClass(), "sendPacket");
			Object hh = getNMSClass("ChatComponentText").getConstructor(String.class).newInstance(s);
			Object ff = getNMSClass("ChatComponentText").getConstructor(String.class).newInstance(s1);
			Object packet = getNMSClass("PacketPlayOutPlayerListHeaderFooter").getConstructor(icbc).newInstance(hh);
			getField(packet.getClass(), "b").set(packet, ff);
			sendPacket.invoke(connection, packet);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void textSlide(final Player player, String s, final int i, int j, BukkitScheduler bukkitscheduler,
			JavaPlugin javaplugin) {
		// String text, int textLengthInFrame, int speedTicks
		s = ChatColor.stripColor(s);
		String s1 = "";

		for (int k = -i; k < -1; k++) {
			s1 += " ";
		}

		s1 += s;

		for (int k = 0; k < i; k++) {
			s1 += " ";
		}

		final String v1 = s1;

		for (int k = 0; k + k <= v1.length(); k++) {
			final int i1 = k;
			bukkitscheduler.scheduleSyncDelayedTask(javaplugin, new Runnable() {
				@Override
				public void run() {
					actionBar(player, v1.substring(i1, i + i1));
				}
			}, j * k);
		}
	}

	public static Class<?>[] toPrimitiveTypeArray(Class<?>[] classes) {
		int a = classes != null ? classes.length : 0;
		Class<?>[] types = new Class<?>[a];
		for (int i = 0; i < a; i++)
			types[i] = getPrimitiveType(classes[i]);
		return types;
	}

	@SuppressWarnings("deprecation")
	public static ICommandListener getListener(CommandSender sender) {
		if (sender instanceof Player) {
			return ((CraftPlayer) sender).getHandle();
		}
		if (sender instanceof BlockCommandSender) {
			return ((CraftBlockCommandSender) sender).getTileEntity();
		}
		if (sender instanceof CommandMinecart) {
			return ((CraftMinecartCommand) sender).getHandle().getCommandBlock();
		}
		if (sender instanceof RemoteConsoleCommandSender) {
			return ((DedicatedServer) MinecraftServer.getServer()).remoteControlCommandListener;
		}
		if (sender instanceof ConsoleCommandSender) {
			return ((CraftServer) sender.getServer()).getServer();
		}
		if (sender instanceof ProxiedCommandSender) {
			return ((ProxiedNativeCommandSender) sender).getHandle();
		}
		throw new IllegalArgumentException("Cannot make " + sender + " a vanilla command listener");
	}

	public static void tripleBeepSamePitch(CommandSender a, JavaPlugin c) {
		if (!(a instanceof Player))
			return;
		final Player asPlayer = (Player) a;
		Runnable sound = new Runnable() {
			@Override
			public void run() {
				asPlayer.playSound(asPlayer.getLocation(), "minecraft:block.note.pling", 3.0f, 0.5f);
			}
		};
		sound.run();
		Bukkit.getScheduler().scheduleSyncDelayedTask(c, sound, 4L);
		Bukkit.getScheduler().scheduleSyncDelayedTask(c, sound, 8L);
	}

	public static void extractFile(File f, File d) throws Exception {
		ZipInputStream zip = null;
		if (!d.exists())
			d.mkdir();
		zip = new ZipInputStream(new FileInputStream(f));
		ZipEntry e = zip.getNextEntry();

		while (e != null) {
			String filePath = d.getPath() + File.separator + e.getName();
			if (!e.isDirectory()) {
				new File(filePath).getParentFile().mkdirs();
				new File(filePath).createNewFile();
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
				byte[] bytesIn = new byte[BUFFER];
				int read = 0;
				while ((read = zip.read(bytesIn)) != -1)
					bos.write(bytesIn, 0, read);
				bos.close();
			} else {
				File dir = new File(filePath);
				dir.mkdir();
			}

			zip.closeEntry();
			e = zip.getNextEntry();
		}
		zip.close();
	}

	public static boolean isInSubDirectory(File f, File f1) {
		return !(f1 == null | f.isFile()) && (f1.equals(f) || isInSubDirectory(f, f1.getParentFile()));
	}

	public static void noReflection(CommandSender a) {
		String s = "\u00a7c\u00a7lWarning: this command only works in 1.10.*!";
		if (a instanceof Player)
			actionBar((Player) a, s);
		else
			AmmarServer.msg(a, s);
	}

	// http://stackoverflow.com/questions/1399126/java-util-zip-recreating-directory-structure
	public static void zip(CommandSender a, File srcFile, File destination) throws IOException {
		if (!FilenameUtils.getExtension(destination.getName()).equals("zip"))
			throw new IllegalArgumentException(
					"Argument file is not a file with a .ZIP extension! The file name is " + destination.getName());
		if (Utils.isInSubDirectory(srcFile, destination))
			throw new IllegalArgumentException("Destination is subfolder of source");
		URI base = srcFile.toURI();
		Deque<File> queue = new LinkedList<>();
		queue.push(srcFile);
		OutputStream out = new FileOutputStream(destination);
		Closeable res = out;
		try {
			ZipOutputStream zout = new ZipOutputStream(out);
			res = zout;
			while (!queue.isEmpty()) {
				srcFile = queue.pop();
				for (File kid : srcFile.listFiles()) {
					String name = base.relativize(kid.toURI()).getPath();
					if (kid.isDirectory()) {
						queue.push(kid);
						name = name.endsWith("/") ? name : name + "/";
						a.sendMessage("Adding folder: " + name);
						zout.putNextEntry(new ZipEntry(name));
					} else {
						a.sendMessage("Adding file: " + name);
						zout.putNextEntry(new ZipEntry(name));
						copy(kid, zout);
						zout.closeEntry();
					}
				}
			}
		} finally {
			res.close();
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[BUFFER];
		while (true) {
			int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException {
		try (InputStream in = new FileInputStream(file)) {
			copy(in, out);
		}
	}

	private static void copy(InputStream in, File file) throws IOException {
		try (OutputStream out = new FileOutputStream(file)) {
			copy(in, out);
		}
	}
}
