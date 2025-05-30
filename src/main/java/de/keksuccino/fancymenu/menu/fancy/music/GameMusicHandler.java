package de.keksuccino.fancymenu.menu.fancy.music;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class GameMusicHandler {
	
	public static void init() {
		try {
			// Replace Minecraft's default MusicTicker with our AdvancedMusicTicker
			Field f = ObfuscationReflectionHelper.findField(Minecraft.class, "field_147126_aw"); //musicTicker
			f.set(Minecraft.getMinecraft(), new AdvancedMusicTicker(Minecraft.getMinecraft()));
			
			// Initialize the custom music handler
			CustomMusicHandler.init();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
