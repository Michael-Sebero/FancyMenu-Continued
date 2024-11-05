package de.keksuccino.fancymenu.menu.animation;

import java.io.File;
import java.util.*;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AnimationData.Type;
import de.keksuccino.fancymenu.menu.animation.exceptions.AnimationNotFoundException;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

public class AnimationHandler {

    private static Map<String, AnimationData> animations = new HashMap<>();
    private static List<String> custom = new ArrayList<>();
    protected static boolean ready = false;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new AnimationHandlerEvents());
    }

    public static void registerAnimation(IAnimationRenderer animation, String name, Type type) {
        if (!animations.containsKey(name)) {
            animations.put(name, new AnimationData(animation, name, type));
            if (type == Type.EXTERNAL) {
                custom.add(name);
            }
        } else {
            FancyMenu.LOGGER.error("[FANCYMENU] AnimationHandler: ERROR: Duplicate animation name: " + name);
        }
    }

    public static void unregisterAnimation(IAnimationRenderer animation) {
        AnimationData d = null;
        for (AnimationData a : animations.values()) {
            if (a.animation == animation) {
                d = a;
                break;
            }
        }
        if (d != null) {
            unregisterAnimation(d.name);
        }
    }

    public static void unregisterAnimation(String name) {
        if (animationExists(name)) {
            animations.remove(name);
            custom.remove(name);
        }
    }

    public static void loadCustomAnimations() {
        File f = FancyMenu.getAnimationPath();
        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        ready = false;
        clearCustomAnimations();

        for (File a : f.listFiles()) {
            String name = null;
            String mainAudio = null;
            String introAudio = null;
            int fps = 0;
            boolean loop = true;
            List<String> frameNamesMain = new ArrayList<>();
            List<String> frameNamesIntro = new ArrayList<>();
            String resourceNamespace = null;

            if (a.isDirectory()) {
                File p = new File(a.getAbsolutePath().replace("\\", "/") + "/animation.properties");
                if (!p.exists()) {
                    continue;
                }

                PropertiesSet props = PropertiesSerializer.getProperties(p.getPath());
                if (props == null) {
                    continue;
                }

                /** ANIMATION META **/
                List<PropertiesSection> metas = props.getPropertiesOfType("animation-meta");
                if (metas.isEmpty()) {
                    continue;
                }
                PropertiesSection m = metas.get(0);

                name = m.getEntryValue("name");
                if (name == null) {
                    continue;
                }

                String fpsString = m.getEntryValue("fps");
                if ((fpsString != null) && MathUtils.isInteger(fpsString)) {
                    fps = Integer.parseInt(fpsString);
                }

                String loopString = m.getEntryValue("loop");
                if ("false".equalsIgnoreCase(loopString)) {
                    loop = false;
                }

                resourceNamespace = m.getEntryValue("namespace");
                if (resourceNamespace == null) {
                    continue;
                }

                /** MAIN FRAME NAMES **/
                List<PropertiesSection> mainFrameSecs = props.getPropertiesOfType("frames-main");
                if (mainFrameSecs.isEmpty()) {
                    continue;
                }
                PropertiesSection mainFrames = mainFrameSecs.get(0);
                Map<String, String> mainFramesMap = mainFrames.getEntries();
                List<String> mainFrameKeys = new ArrayList<>();
                for (Map.Entry<String, String> me : mainFramesMap.entrySet()) {
                    if (me.getKey().startsWith("frame_")) {
                        String frameNumber = me.getKey().split("[_]", 2)[1];
                        if (MathUtils.isInteger(frameNumber)) {
                            mainFrameKeys.add(me.getKey());
                        }
                    }
                }
                mainFrameKeys.sort(Comparator.comparingInt(o -> Integer.parseInt(o.split("[_]", 2)[1])));
                for (String s : mainFrameKeys) {
                    frameNamesMain.add("frames_main/" + mainFramesMap.get(s));
                }

                /** INTRO FRAME NAMES **/
                List<PropertiesSection> introFrameSecs = props.getPropertiesOfType("frames-intro");
                if (!introFrameSecs.isEmpty()) {
                    PropertiesSection introFrames = introFrameSecs.get(0);
                    Map<String, String> introFramesMap = introFrames.getEntries();
                    List<String> introFrameKeys = new ArrayList<>();
                    for (Map.Entry<String, String> me : introFramesMap.entrySet()) {
                        if (me.getKey().startsWith("frame_")) {
                            String frameNumber = me.getKey().split("[_]", 2)[1];
                            if (MathUtils.isInteger(frameNumber)) {
                                introFrameKeys.add(me.getKey());
                            }
                        }
                    }
                    introFrameKeys.sort(Comparator.comparingInt(o -> Integer.parseInt(o.split("[_]", 2)[1])));
                    for (String s : introFrameKeys) {
                        frameNamesIntro.add("frames_intro/" + introFramesMap.get(s));
                    }
                }

                String[] formats = {".wav", ".flac", ".opus", ".mp3"};

                for (String format : formats) {
                    File audio1 = new File(a.getAbsolutePath().replace("\\", "/") + "/audio/mainaudio" + format);
                    if (audio1.exists()) {
                        mainAudio = audio1.getPath();
                        break;
                    }
                }

                for (String format : formats) {
                    File audio2 = new File(a.getAbsolutePath().replace("\\", "/") + "/audio/introaudio" + format);
                    if (audio2.exists()) {
                        introAudio = audio2.getPath();
                        break;
                    }
                }

                if (name != null) {
                    File gifIntro = new File(a.getPath() + "/intro.gif");
                    File gifAni = new File(a.getPath() + "/animation.gif");
                    IAnimationRenderer in = null;
                    IAnimationRenderer an = null;

                    if (!frameNamesIntro.isEmpty() && !frameNamesMain.isEmpty()) {
                        in = new ResourcePackAnimationRenderer(resourceNamespace, frameNamesIntro, fps, loop, 0, 0, 100, 100);
                        an = new ResourcePackAnimationRenderer(resourceNamespace, frameNamesMain, fps, loop, 0, 0, 100, 100);
                    } else if (!frameNamesMain.isEmpty()) {
                        an = new ResourcePackAnimationRenderer(resourceNamespace, frameNamesMain, fps, loop, 0, 0, 100, 100);
                    }

                    try {
                        if (in != null && an != null) {
                            AdvancedAnimation ani = new AdvancedAnimation(in, an, introAudio, mainAudio, false);
                            ani.propertiesPath = a.getPath();
                            registerAnimation(ani, name, Type.EXTERNAL);
                            ani.prepareAnimation();
                            FancyMenu.LOGGER.info("[FANCYMENU] AnimationHandler: Animation registered: " + name);
                        } else if (an != null) {
                            AdvancedAnimation ani = new AdvancedAnimation(null, an, introAudio, mainAudio, false);
                            ani.propertiesPath = a.getPath();
                            registerAnimation(ani, name, Type.EXTERNAL);
                            ani.prepareAnimation();
                            FancyMenu.LOGGER.info("[FANCYMENU] AnimationHandler: Animation registered: " + name);
                        } else {
                            FancyMenu.LOGGER.error("[FANCYMENU] AnimationHandler: ERROR: This is not a valid animation: " + name);
                        }
                    } catch (AnimationNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static List<String> getCustomAnimationNames() {
        List<String> l = new ArrayList<>();
        l.addAll(custom);
        return l;
    }

    public static IAnimationRenderer getAnimation(String name) {
        AnimationData data = animations.getOrDefault(name, null);
        if (data != null) {
            return data.animation;
        }
        return null;
    }

    private static String getIntroPath(String path) {
        File f = new File(path + "/intro");
        if (f.exists() && f.isDirectory()) {
            return f.getPath();
        }
        return null;
    }

    private static String getAnimationPath(String path) {
        File f = new File(path + "/animation");
        if (f.exists() && f.isDirectory()) {
            return f.getPath();
        }
        return null;
    }

    private static void clearCustomAnimations() {
        for (String s : custom) {
            animations.remove(s);
        }
    }

    public static boolean animationExists(String name) {
        return animations.containsKey(name);
    }

    public static List<IAnimationRenderer> getAnimations() {
        List<IAnimationRenderer> renderers = new ArrayList<>();
        for (Map.Entry<String, AnimationData> m : animations.entrySet()) {
            renderers.add(m.getValue().animation);
        }
        return renderers;
    }

    public static void resetAnimations() {
        for (AnimationData d : animations.values()) {
            d.animation.resetAnimation();
        }
    }

    public static void resetAnimationSounds() {
        for (AnimationData d : animations.values()) {
            if (d.animation instanceof AdvancedAnimation) {
                ((AdvancedAnimation) d.animation).resetAudio();
            }
        }
    }

    public static void stopAnimationSounds() {
        for (AnimationData d : animations.values()) {
            if (d.animation instanceof AdvancedAnimation) {
                ((AdvancedAnimation) d.animation).stopAudio();
            }
        }
    }

    public static boolean isReady() {
        return ready;
    }

    public static void setReady(boolean ready) {
        AnimationHandler.ready = ready;
    }

    public static void setupAnimationSizes() {
        for (IAnimationRenderer a : getAnimations()) {
            if (a instanceof ResourcePackAnimationRenderer) {
                ((ResourcePackAnimationRenderer) a).setupAnimationSize();
            } else if (a instanceof AdvancedAnimation) {
                IAnimationRenderer main = ((AdvancedAnimation) a).getMainAnimationRenderer();
                if (main instanceof ResourcePackAnimationRenderer) {
                    ((ResourcePackAnimationRenderer) main).setupAnimationSize();
                }
                IAnimationRenderer intro = ((AdvancedAnimation) a).getIntroAnimationRenderer();
                if (intro instanceof ResourcePackAnimationRenderer) {
                    ((ResourcePackAnimationRenderer) intro).setupAnimationSize();
                }
            }
        }
    }

    public static void preloadAnimations() {
        FancyMenu.LOGGER.info("[FANCYMENU] Updating animation sizes..");
        setupAnimationSizes();

        boolean errors = false;

        if (FancyMenu.config.getOrDefault("preloadanimations", true)) {
            if (!ready) {
                FancyMenu.LOGGER.info("[FANCYMENU] LOADING ANIMATION TEXTURES! THIS CAUSES THE LOADING SCREEN TO FREEZE FOR A WHILE!");
                try {
                    List<ResourcePackAnimationRenderer> l = new ArrayList<>();
                    for (IAnimationRenderer r : getAnimations()) {
                        if (r instanceof AdvancedAnimation) {
                            IAnimationRenderer main = ((AdvancedAnimation) r).getMainAnimationRenderer();
                            IAnimationRenderer intro = ((AdvancedAnimation) r).getIntroAnimationRenderer();
                            if (main instanceof ResourcePackAnimationRenderer) {
                                l.add((ResourcePackAnimationRenderer) main);
                            }
                            if (intro instanceof ResourcePackAnimationRenderer) {
                                l.add((ResourcePackAnimationRenderer) intro);
                            }
                        } else if (r instanceof ResourcePackAnimationRenderer) {
                            l.add((ResourcePackAnimationRenderer) r);
                        }
                    }
                    for (ResourcePackAnimationRenderer r : l) {
                        for (ResourceLocation rl : r.getAnimationFrames()) {
                            TextureManager t = Minecraft.getMinecraft().getTextureManager();
                            ITextureObject to = t.getTexture(rl);
                            if (to == null) {
                                to = new SimpleTexture(rl);
                                t.loadTexture(rl, to);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    errors = true;
                }
                if (!errors) {
                    FancyMenu.LOGGER.info("[FANCYMENU] FINISHED LOADING ANIMATION TEXTURES!");
                } else {
                    FancyMenu.LOGGER.warn("[FANCYMENU] FINISHED LOADING ANIMATION TEXTURES WITH ERRORS! PLEASE CHECK YOUR ANIMATIONS!");
                }
                ready = true;
            }
        } else {
            ready = true;
        }
    }
}
