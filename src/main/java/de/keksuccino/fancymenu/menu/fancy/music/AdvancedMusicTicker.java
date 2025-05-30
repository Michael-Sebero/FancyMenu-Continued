package de.keksuccino.fancymenu.menu.fancy.music;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class AdvancedMusicTicker extends MusicTicker {

	private ISound customCurrentMusic;
	private float lastMusicVolume = -1f;
	private boolean customMusicRepeats = true;
	private ResourceLocation lastPlayedSound;

	public AdvancedMusicTicker(Minecraft client) {
		super(client);
	}
	
	@Override
	public void playMusic(MusicType type) {
		if ((type != null) && (type == MusicType.MENU) && !FancyMenu.config.getOrDefault("playmenumusic", true)) {
			return;
		}
		if ((Minecraft.getMinecraft().world != null) && FancyMenu.config.getOrDefault("stopworldmusicwhencustomizable", false) && (Minecraft.getMinecraft().currentScreen != null) && MenuCustomization.isMenuCustomizable(Minecraft.getMinecraft().currentScreen)) {
			Minecraft.getMinecraft().getSoundHandler().pauseSounds();
			return;
		}
		
		super.playMusic(type);
	}
	
	@Override
	public void update() {
		// Handle volume changes for custom music
		GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;
		float currentMusicVolume = gameSettings.getSoundLevel(SoundCategory.MUSIC);
		
		// If music volume changed and we have custom music playing, we need to handle it
		if (this.customCurrentMusic != null && Math.abs(currentMusicVolume - lastMusicVolume) > 0.01f) {
			SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
			
			// Check if our custom sound is still playing
			if (soundHandler.isSoundPlaying(this.customCurrentMusic)) {
				// Volume changed, need to restart with new volume
				ResourceLocation soundLocation = this.customCurrentMusic.getSoundLocation();
				boolean repeat = this.customMusicRepeats;
				
				soundHandler.stopSound(this.customCurrentMusic);
				this.customCurrentMusic = null;
				
				// Restart with new volume
				playCustomMusicInternal(soundLocation, repeat);
			}
		}
		
		lastMusicVolume = currentMusicVolume;
		super.update();
	}
	
	/**
	 * Play custom music file through Minecraft's sound system
	 * This ensures it uses the same audio channel and respects volume settings
	 */
	public void playCustomMusic(ResourceLocation soundLocation, boolean repeat) {
		stopCustomMusic(); // Stop any currently playing custom music
		playCustomMusicInternal(soundLocation, repeat);
	}
	
	/**
	 * Play custom music with default repeat=true
	 */
	public void playCustomMusic(ResourceLocation soundLocation) {
		playCustomMusic(soundLocation, true);
	}
	
	/**
	 * Play custom music using SoundEvent
	 */
	public void playCustomMusic(SoundEvent soundEvent, boolean repeat) {
		if (soundEvent != null) {
			playCustomMusic(soundEvent.getSoundName(), repeat);
		}
	}
	
	public void playCustomMusic(SoundEvent soundEvent) {
		playCustomMusic(soundEvent, true);
	}
	
	private void playCustomMusicInternal(ResourceLocation soundLocation, boolean repeat) {
		if (soundLocation != null) {
			SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
			GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;
			
			// Get current music volume
			float musicVolume = gameSettings.getSoundLevel(SoundCategory.MUSIC);
			
			// Create a positioned sound record that properly integrates with Minecraft's audio
			PositionedSoundRecord sound = new PositionedSoundRecord(
				soundLocation,
				SoundCategory.MUSIC, // Critical: Use MUSIC category for proper channel routing
				musicVolume, // Use current music volume setting
				1.0F, // Pitch
				repeat, // Whether to repeat
				0, // Repeat delay
				ISound.AttenuationType.NONE, // No distance attenuation for music
				0.0F, 0.0F, 0.0F // Position (irrelevant for music)
			);
			
			// Play through Minecraft's sound handler - this ensures proper channel routing
			soundHandler.playSound(sound);
			this.customCurrentMusic = sound;
			this.customMusicRepeats = repeat;
			this.lastPlayedSound = soundLocation;
			this.lastMusicVolume = musicVolume;
		}
	}
	
	/**
	 * Stop only custom music, leave vanilla music alone
	 */
	public void stopCustomMusic() {
		if (this.customCurrentMusic != null) {
			Minecraft.getMinecraft().getSoundHandler().stopSound(this.customCurrentMusic);
			this.customCurrentMusic = null;
			this.customMusicRepeats = true;
			this.lastPlayedSound = null;
		}
	}
	
	/**
	 * Stop all music (custom and vanilla)
	 */
	public void stop() {
		// Stop custom music
		stopCustomMusic();
		
		// Stop vanilla music
		if (this.getCurrentMusic() != null) {
			Minecraft.getMinecraft().getSoundHandler().stopSound(this.getCurrentMusic());
			this.setCurrentMusic(null);
			this.setTimeUntilNext(0);
		}
	}
	
	/**
	 * Check if custom music is currently playing
	 */
	public boolean isPlayingCustomMusic() {
		return this.customCurrentMusic != null && 
			   Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(this.customCurrentMusic);
	}
	
	/**
	 * Get the current custom music volume (respects Minecraft's music slider)
	 */
	public float getCurrentMusicVolume() {
		return Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC);
	}
	
	protected ISound getCurrentMusic() {
		try {
			Field f = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c"); //currentMusic
			return (ISound) f.get(this);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected void setCurrentMusic(ISound sound) {
		try {
			Field f = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c"); //currentMusic
			f.set(this, sound);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void setTimeUntilNext(int time) {
		try {
			Field f = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147676_d"); //timeUntilNextMusic
			f.set(this, time);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
