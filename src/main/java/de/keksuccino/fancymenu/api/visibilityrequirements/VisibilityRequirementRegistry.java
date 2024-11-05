//TODO übernehmenn
package de.keksuccino.fancymenu.api.visibilityrequirements;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirementRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



import java.util.*;

/**
 * DEPRECATED! Use {@link de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirementRegistry} instead!
 */
@Deprecated
public class VisibilityRequirementRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static LinkedHashMap<String, VisibilityRequirement> requirements = new LinkedHashMap<>();

    /**
     * DEPRECATED! Use {@link de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirementRegistry} instead!
     */
    @Deprecated
    public static void registerRequirement(VisibilityRequirement requirement) {
        if (requirement != null) {
            if (requirement.getIdentifier() != null) {
                LOGGER.warn("[FANCYMENU] Registering VisibilityRequirement to deprecated registry! Use LoadingRequirementRegistry instead! (" + requirement.getIdentifier() + ")");
                if (requirements.containsKey(requirement.getIdentifier())) {
                    LOGGER.warn("[FANCYMENU] A VisibilityRequirement with the identifier '" + requirement.getIdentifier() + "' is already registered! Overriding requirement!");
                }
                requirements.put(requirement.getIdentifier(), requirement);
            } else {
                LOGGER.error("[FANCYMENU] VisibilityRequirement identifier cannot be NULL!");
            }
            LoadingRequirementRegistry.registerRequirement(convertToLoadingRequirement(requirement));
        }
    }

    /**
     * DEPRECATED! Use {@link de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirementRegistry} instead!
     */
    @Deprecated
    public static void unregisterRequirement(String requirementIdentifier) {
        requirements.remove(requirementIdentifier);
        LoadingRequirementRegistry.unregisterRequirement(requirementIdentifier);
    }

    /**
     * DEPRECATED! Use {@link de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirementRegistry} instead!
     */
    @Deprecated
    public static List<VisibilityRequirement> getRequirements() {
        List<VisibilityRequirement> l = new ArrayList<>();
        requirements.forEach((key, value) -> {
            l.add(value);
        });
        return l;
    }

    /**
     * DEPRECATED! Use {@link de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirementRegistry} instead!
     */
    @Deprecated
    public static VisibilityRequirement getRequirement(String requirementIdentifier) {
        return requirements.get(requirementIdentifier);
    }

    private static LoadingRequirement convertToLoadingRequirement(VisibilityRequirement visReq) {
        return new LoadingRequirement(visReq.getIdentifier()) {
            @Override
            public boolean hasValue() {
                return visReq.hasValue();
            }
            @Override
            public boolean isRequirementMet( String value) {
                return visReq.isRequirementMet(value);
            }
            @Override
            public  String getDisplayName() {
                return visReq.getDisplayName();
            }
            @Override
            public  List<String> getDescription() {
                return visReq.getDescription();
            }
            @Override
            public  String getCategory() {
                return null;
            }

            @Override
            public String getValueDisplayName() {
                return visReq.getValueDisplayName();
            }

            @Override
            public String getValuePreset() {
                return visReq.getValuePreset();
            }
            @Override
            public  List<TextEditorFormattingRule> getValueFormattingRules() {
                return null;
            }
        };
    }

}
