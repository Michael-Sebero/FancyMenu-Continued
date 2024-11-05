package de.keksuccino.fancymenu.core;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.SortingIndex(2)
public class FMCore implements IFMLLoadingPlugin {

    public FMCore() {

        if (FancyMenu.IS_DEV_ENVIRONMENT) {
            MixinBootstrap.init();
            Mixins.addConfiguration("fancymenu.general.mixin.json");
        }

    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}