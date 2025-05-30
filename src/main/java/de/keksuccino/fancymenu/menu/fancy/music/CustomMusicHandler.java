package de.keksuccino.fancymenu.menu.fancy.music;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class CustomMusicHandler {
    
    private static final Map<String, SoundEvent> registeredSounds = new HashMap<>();
    private static AdvancedMusicTicker musicTicker;
    
    /**
     * Initialize the custom music handler
     */
    public static void init() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getMusicTicker() instanceof AdvancedMusicTicker) {
            musicTicker = (AdvancedMusicTicker) mc.getMusicTicker();
        }
    }
    
    /**
     * Register a custom music file for use with the sound system
     * IMPORTANT: The sound file must be in OGG format and placed in assets/[modid]/sounds/
     * WAV and FLAC files must be converted to OGG for proper Minecraft integration
     * 
     * @param name Internal name for the sound
     * @param modId Your mod's ID
     * @return ResourceLocation for the registered sound
     */
    public static ResourceLocation registerCustomMusic(String name, String modId) {
        ResourceLocation soundLocation = new ResourceLocation(modId, name);
        
        // Create and register the sound event
        SoundEvent soundEvent = new SoundEvent(soundLocation);
        soundEvent.setRegistryName(soundLocation);
        
        // Register the sound event with Forge
        ForgeRegistries.SOUND_EVENTS.register(soundEvent);
        registeredSounds.put(name, soundEvent);
        
        return soundLocation;
    }
    
    /**
     * Play custom music by registered name
     * @param name The registered name of the sound
     * @param repeat Whether the music should loop
     */
    public static void playCustomMusic(String name, boolean repeat) {
        if (musicTicker == null) {
            init();
        }
        
        if (musicTicker != null && registeredSounds.containsKey(name)) {
            SoundEvent soundEvent = registeredSounds.get(name);
            musicTicker.playCustomMusic(soundEvent, repeat);
        }
    }
    
    /**
     * Play custom music by registered name (with default repeat=true)
     */
    public static void playCustomMusic(String name) {
        playCustomMusic(name, true);
    }
    
    /**
     * Play custom music by ResourceLocation
     * @param soundLocation The ResourceLocation of the sound
     * @param repeat Whether the music should loop
     */
    public static void playCustomMusic(ResourceLocation soundLocation, boolean repeat) {
        if (musicTicker == null) {
            init();
        }
        
        if (musicTicker != null) {
            musicTicker.playCustomMusic(soundLocation, repeat);
        }
    }
    
    /**
     * Play custom music by ResourceLocation (with default repeat=true)
     */
    public static void playCustomMusic(ResourceLocation soundLocation) {
        playCustomMusic(soundLocation, true);
    }
    
    /**
     * Stop all custom music
     */
    public static void stopCustomMusic() {
        if (musicTicker != null) {
            musicTicker.stopCustomMusic();
        }
    }
    
    /**
     * Stop all music (custom and vanilla)
     */
    public static void stopAllMusic() {
        if (musicTicker != null) {
            musicTicker.stop();
        }
    }
    
    /**
     * Check if custom music is currently playing
     */
    public static boolean isPlayingCustomMusic() {
        if (musicTicker != null) {
            return musicTicker.isPlayingCustomMusic();
        }
        return false;
    }
    
    /**
     * Get current music volume (from Minecraft's music slider)
     */
    public static float getCurrentMusicVolume() {
        if (musicTicker != null) {
            return musicTicker.getCurrentMusicVolume();
        }
        return 0.0f;
    }
    
    /**
     * Get the current music ticker
     */
    public static AdvancedMusicTicker getMusicTicker() {
        return musicTicker;
    }
    
    /**
     * Get all registered sound names
     */
    public static String[] getRegisteredSoundNames() {
        return registeredSounds.keySet().toArray(new String[0]);
    }
}
