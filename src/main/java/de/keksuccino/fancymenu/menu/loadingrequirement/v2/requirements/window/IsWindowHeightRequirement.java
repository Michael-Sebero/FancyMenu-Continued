package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements.window;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IsWindowHeightRequirement extends LoadingRequirement {

    public IsWindowHeightRequirement() {
        super("fancymenu_loading_requirement_is_window_height");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet( String value) {
        if (value != null) {
            List<Integer> l = this.parseIntegers(value);
            if (!l.isEmpty()) {
                return l.contains(Minecraft.getMinecraft().displayHeight);
            }
        }
        return false;
    }

    protected List<Integer> parseIntegers(String value) {
        List<Integer> l = new ArrayList<>();
        if (value != null) {
            if (value.contains(",")) {
                for (String s : value.replace(" ", "").split("[,]")) {
                    //Filtering some human errors by checking for double, even if int is needed
                    if (MathUtils.isDouble(s)) {
                        l.add((int)Double.parseDouble(s));
                    }
                }
            } else {
                if (MathUtils.isInteger(value.replace(" ", ""))) {
                    l.add((int)Double.parseDouble(value.replace(" ", "")));
                }
            }
        }
        return l;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight.desc", "" + Minecraft.getMinecraft().displayWidth, "" + Minecraft.getMinecraft().displayHeight), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.editor.loading_requirement.category.window");
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight.valuename");
    }

    @Override
    public String getValuePreset() {
        return "1080";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
