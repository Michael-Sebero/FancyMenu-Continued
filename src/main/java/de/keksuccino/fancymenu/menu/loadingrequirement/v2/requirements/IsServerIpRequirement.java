package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.List;

public class IsServerIpRequirement extends LoadingRequirement {

    public IsServerIpRequirement() {
        super("fancymenu_visibility_requirement_is_server_ip");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet( String value) {

        if (value != null) {
            if (Minecraft.getMinecraft().world != null) {
                if (Minecraft.getMinecraft().getCurrentServerData() != null) {
                    if (value.contains(":")) {
                        return Minecraft.getMinecraft().getCurrentServerData().serverIP.equals(value);
                    } else {
                        String curIp = Minecraft.getMinecraft().getCurrentServerData().serverIP;
                        if (curIp.contains(":")) {
                            curIp = curIp.split("[:]", 2)[0];
                        }
                        return curIp.equals(value);
                    }
                }
            }
        }

        return false;

    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.is_server_ip");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.visibilityrequirement.is_server_ip.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.visibilityrequirement.is_server_ip.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "mc.mycoolserver.com";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
