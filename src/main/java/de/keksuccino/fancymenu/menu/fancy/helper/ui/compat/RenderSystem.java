package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat;

import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;

public class RenderSystem {
    // Private constructor to prevent instantiation
    private RenderSystem() {}
    
    public static void color4f(float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
    }

    public static void enableScissor(int x, int y, int width, int height) {
        RenderUtils.enableScissor(x, y, width, height);
    }

    public static void disableScissor() {
        RenderUtils.disableScissor();
    }

    // Matrix operations
    public static void pushMatrix() {
        GlStateManager.pushMatrix();
    }

    public static void popMatrix() {
        GlStateManager.popMatrix();
    }

    public static void scale(float x, float y, float z) {
        GlStateManager.scale(x, y, z);
    }

    // Blend state
    public static void enableBlend() {
        GlStateManager.enableBlend();
    }

    public static void disableBlend() {
        GlStateManager.disableBlend();
    }

    // Additional commonly used methods that might be needed
    public static void translate(float x, float y, float z) {
        GlStateManager.translate(x, y, z);
    }

    public static void rotate(float angle, float x, float y, float z) {
        GlStateManager.rotate(angle, x, y, z);
    }
}
