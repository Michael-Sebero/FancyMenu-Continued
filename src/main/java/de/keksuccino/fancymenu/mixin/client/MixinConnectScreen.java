package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.world.LastWorldHandler;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public class MixinConnectScreen {

    private static final Logger MIXIN_LOGGER = LogManager.getLogger("fancymenu/mixin/ConnectScreen");

    @Inject(at = @At("HEAD"), method = "connectToServer")
    private void onStartConnecting(ServerData server, CallbackInfo info) {

        if (server != null) {
            LastWorldHandler.setLastWorld(server.serverIP, true);
        }

    }

}
