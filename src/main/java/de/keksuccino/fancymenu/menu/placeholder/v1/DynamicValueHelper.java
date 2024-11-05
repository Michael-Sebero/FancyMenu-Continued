package de.keksuccino.fancymenu.menu.placeholder.v1;

import java.io.File;
import java.util.*;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextContainer;
import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextRegistry;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.button.ButtonMimeHandler;
import de.keksuccino.fancymenu.menu.servers.ServerCache;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

@Deprecated
public class DynamicValueHelper {

	public static Map<String, RandomTextPackage> randomTextIntervals = new HashMap<>();

	@Deprecated
	public static String convertFromRaw(String in) {
		int width = 0;
		int height = 0;
		String playername = Minecraft.getMinecraft().getSession().getUsername();
		String playeruuid = Minecraft.getMinecraft().getSession().getPlayerID();
		String mcversion = ForgeVersion.mcVersion;
		if (Minecraft.getMinecraft().currentScreen != null) {
			width = Minecraft.getMinecraft().currentScreen.width;
			height = Minecraft.getMinecraft().currentScreen.height;
		}
		
		//Convert &-formatcodes to real ones
		in = StringUtils.convertFormatCodes(in, "&", "§");
		
		//Replace height and width placeholders
		in = in.replace("%guiwidth%", "" + width);
		in = in.replace("%guiheight%", "" + height);
		
		//Replace player name and uuid placeholders
		in = in.replace("%playername%", playername);
		in = in.replace("%playeruuid%", playeruuid);
		
		//Replace mc version placeholder
		in = in.replace("%mcversion%", mcversion);

		//Replace mod version placeholder
		in = replaceModVersionPlaceolder(in);

		//Replace loaded mods placeholder
		int loaded = getLoadedMods();
		in = in.replace("%loadedmods%", "" + loaded);

		//Replace total mods placeholder
		int total = getTotalMods();
		if (total < loaded) {
			total = loaded;
		}
		in = in.replace("%totalmods%", "" + total);

		in = replaceLocalsPlaceolder(in);

		in = replaceServerMOTD(in);
		in = replaceServerMotdFirstLine(in);
		in = replaceServerMotdSecondLine(in);

		in = replaceServerPing(in);

		in = replaceServerVersion(in);

		in = replaceServerPlayerCount(in);

		in = replaceServerStatus(in);

		if (in.contains("ram%")) {
			long i = Runtime.getRuntime().maxMemory();
			long j = Runtime.getRuntime().totalMemory();
			long k = Runtime.getRuntime().freeMemory();
			long l = j - k;

			in = in.replace("%percentram%", (l * 100L / i) + "%");

			in = in.replace("%usedram%", "" + bytesToMb(l));

			in = in.replace("%maxram%", "" + bytesToMb(i));
		}

		if (in.contains("%realtime")) {

			Calendar c = Calendar.getInstance();

			in = in.replace("%realtimeyear%", "" + c.get(Calendar.YEAR));

			in = in.replace("%realtimemonth%", formatToFancyDateTime(c.get(Calendar.MONTH) + 1));

			in = in.replace("%realtimeday%", formatToFancyDateTime(c.get(Calendar.DAY_OF_MONTH)));

			in = in.replace("%realtimehour%", formatToFancyDateTime(c.get(Calendar.HOUR_OF_DAY)));

			in = in.replace("%realtimeminute%", formatToFancyDateTime(c.get(Calendar.MINUTE)));

			in = in.replace("%realtimesecond%", formatToFancyDateTime(c.get(Calendar.SECOND)));

		}

		in = replaceVanillaButtonLabelPlaceolder(in);

		in = replaceRandomTextValue(in);

		//Handle all custom placeholders added via the API
		for (PlaceholderTextContainer p : PlaceholderTextRegistry.getPlaceholders()) {
			in = p.replacePlaceholders(in);
		}
		
		return in;
	}

	@Deprecated
	public static boolean containsDynamicValues(String in) {
		String s = convertFromRaw(in);
		return !s.equals(in);
	}

	private static String replaceRandomTextValue(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%randomtext:")) { // %randomtext:<filepath>:<change_interval_sec>%
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String value = blank.split(":", 2)[1];
					if (value.contains(":")) {
						String pathString = value.split(":", 2)[0];
						File path = new File(pathString);
						if (!path.exists() || !path.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getMinecraft().mcDataDir.getAbsolutePath().replace("\\", "/"))) {
							path = new File(Minecraft.getMinecraft().mcDataDir, pathString);
						}
						String intervalString = value.split(":", 2)[1];
						if (MathUtils.isLong(intervalString) && path.isFile() && path.getPath().toLowerCase().endsWith(".txt")) {
							long interval = Long.parseLong(intervalString) * 1000;
							if (interval < 0L) {
								interval = 0L;
							}
							long currentTime = System.currentTimeMillis();
							RandomTextPackage p;
							if (randomTextIntervals.containsKey(path.getPath())) {
								p = randomTextIntervals.get(path.getPath());
							} else {
								p = new RandomTextPackage();
								randomTextIntervals.put(path.getPath(), p);
							}
							if ((interval > 0) || (p.currentText == null)) {
								if ((p.lastChange + interval) <= currentTime) {
									p.lastChange = currentTime;
									List<String> txtLines = FileUtils.getFileLines(path);
									if (!txtLines.isEmpty()) {
										p.currentText = txtLines.get(MathUtils.getRandomNumberInRange(0, txtLines.size()-1));
									} else {
										p.currentText = null;
									}
								}
							}
							if (p.currentText != null) {
								in = in.replace(s, p.currentText);
							} else {
								in = in.replace(s, "");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceVanillaButtonLabelPlaceolder(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%vanillabuttonlabel:")) {
				String blank = s.substring(1, s.length()-1);
				String buttonLocator = blank.split(":", 2)[1];
				ButtonData d = ButtonMimeHandler.getButton(buttonLocator);
				if (d != null) {
					in = in.replace(s, d.getButton().displayString);
				} else {
					in = in.replace(s, "§c[unable to get button label]");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceLocalsPlaceolder(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%local:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String localizationKey = blank.split(":", 2)[1];
					String localized = Locals.localize(localizationKey);
					if (localized.equals(localizationKey)) {
						localized = I18n.format(localizationKey);
						if (localized == null) {
							localized = localizationKey;
						}
					}
					in = in.replace(s, localized);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerVersion(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%serverversion:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.gameVersion != null) {
							in = in.replace(s, sd.gameVersion);
						} else {
							in = in.replace(s, "---");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerStatus(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%serverstatus:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.pingToServer > -1L) {
							in = in.replace(s, "§aOnline");
						} else {
							in = in.replace(s, "§cOffline");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerPlayerCount(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%serverplayercount:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.populationInfo != null) {
							in = in.replace(s, "" + sd.populationInfo);
						} else {
							in = in.replace(s, "0/0");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerPing(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%serverping:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						in = in.replace(s, "" + sd.pingToServer);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerMOTD(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%servermotd:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.serverMOTD != null) {
							in = in.replace(s, sd.serverMOTD);
						} else {
							in = in.replace(s, "");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerMotdFirstLine(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%servermotd_line1:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.serverMOTD != null) {
							List<String> lines = splitMotdLines(sd.serverMOTD);
							if (!lines.isEmpty()) {
								in = in.replace(s, lines.get(0));
							} else {
								in = in.replace(s, "");
							}
						} else {
							in = in.replace(s, "");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerMotdSecondLine(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%servermotd_line2:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.serverMOTD != null) {
							List<String> lines = splitMotdLines(sd.serverMOTD);
							if (lines.size() >= 2) {
								in = in.replace(s, lines.get(1));
							} else {
								in = in.replace(s, "");
							}
						} else {
							in = in.replace(s, "");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	protected static List<String> splitMotdLines(String motd) {
		List<String> l = new ArrayList<>();
		try {
			if (motd.contains("\n")) {
				l.addAll(Arrays.asList(motd.split("\n")));
			} else {
				l.add(motd);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}

	private static String replaceModVersionPlaceolder(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%version:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String mod = blank.split(":", 2)[1];
					if (Loader.isModLoaded(mod)) {
						ModContainer c = getModContainerById(mod);
						if (c != null) {
							String version = c.getVersion();
							in = in.replace(s, version);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	protected static List<String> getReplaceablesWithValue(String in, String placeholderBase) {
		List<String> l = new ArrayList<String>();
		try {
			if (in.contains(placeholderBase)) {
				int index = -1;
				int i = 0;
				while (i < in.length()) {
					String s = "" + in.charAt(i);
					if (s.equals("%")) {
						if (index == -1) {
							index = i;
						} else {
							String sub = in.substring(index, i+1);
							if (sub.startsWith(placeholderBase) && sub.endsWith("%")) {
								l.add(sub);
							}
							index = -1;
						}
					}
					i++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}
	
	private static ModContainer getModContainerById(String modid) {
		try {
			for (ModContainer c : Loader.instance().getActiveModList()) {
				if (c.getModId().equals(modid)) {
					return c;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static int getTotalMods() {
		try {
			int i = 0;
			if (Konkrete.isOptifineLoaded) {
				i++;
			}
			return Loader.instance().getModList().size() + i;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static int getLoadedMods() {
		try {
			int i = 0;
			if (Konkrete.isOptifineLoaded) {
				i++;
			}
			return Loader.instance().getActiveModList().size() + 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static String formatToFancyDateTime(int in) {
		String s = "" + in;
		if (s.length() < 2) {
			s = "0" + s;
		}
		return s;
	}

	private static long bytesToMb(long bytes) {
		return bytes / 1024L / 1024L;
	}

	@Deprecated
	public static class RandomTextPackage {
		public String currentText = null;
		public long lastChange = 0L;
	}

}
