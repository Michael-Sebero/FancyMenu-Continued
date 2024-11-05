package de.keksuccino.fancymenu.menu.fancy.helper;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderRegistry;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public class PlaceholderEditBox extends AdvancedTextField {

	private AdvancedImageButton variableButton;
	public FMContextMenu variableMenu;

	public boolean renderContextMenu = true;
	
	private static final ResourceLocation VARIABLES_BUTTON_RESOURCE = new ResourceLocation("keksuccino", "add_btn.png");

	public PlaceholderEditBox(FontRenderer font, int x, int y, int width, int height, boolean handleTextField, CharacterFilter filter) {
		super(font, x, y, width, height, handleTextField, filter);

		this.setMaxStringLength(10000);

		variableMenu = new FMContextMenu();
		variableMenu.setAutoclose(true);

		variableButton = new AdvancedImageButton(0, 0, height, height, VARIABLES_BUTTON_RESOURCE, true, (press) -> {
			UIBase.openScaledContextMenuAtMouse(variableMenu);
		});
		variableButton.ignoreBlockedInput = true;
		variableButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.desc"), "%n%"));
		UIBase.colorizeButton(variableButton);
		variableMenu.setParentButton(variableButton);

		//Build lists of all placeholders ordered by categories
		Map<String, List<Placeholder>> categories = new LinkedHashMap<>();
		for (Placeholder p : PlaceholderRegistry.getPlaceholdersList()) {
			String category = p.getCategory();
			if (category == null) {
				category = Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.other");
			}
			List<Placeholder> l = categories.get(category);
			if (l == null) {
				l = new ArrayList<>();
				categories.put(category, l);
			}
			l.add(p);
		}
		//Move the Other category to the end
		List<Placeholder> otherCategory = categories.get(Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.other"));
		if (otherCategory != null) {
			categories.remove(Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.other"));
			categories.put(Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.other"), otherCategory);
		}

		//Add categories and its placeholders to the context menu
		for (Map.Entry<String, List<Placeholder>> m : categories.entrySet()) {
			FMContextMenu categoryMenu = new FMContextMenu();
			categoryMenu.setAutoclose(true);
			variableMenu.addChild(categoryMenu);

			AdvancedButton customCategoryButton = new AdvancedButton(0, 0, 0, 0, m.getKey(), true, (press) -> {
				categoryMenu.setParentButton((AdvancedButton) press);
				categoryMenu.openMenuAt(0, press.y);
			});
			variableMenu.addContent(customCategoryButton);

			for (Placeholder p : m.getValue()) {
				AdvancedButton customPlaceholder = new AdvancedButton(0, 0, 0, 0, p.getDisplayName(), true, (press) -> {
					this.writeText(p.getDefaultPlaceholderString().toString());
				});
				List<String> desc = p.getDescription();
				if (desc != null) {
					customPlaceholder.setDescription(desc.toArray(new String[0]));
				}
				categoryMenu.addContent(customPlaceholder);
			}
		}

	}

	@Override
	public void drawTextBox() {
		if (this.variableButton != null) {
			
			this.variableButton.width = this.height;
			this.variableButton.height = this.height;
			
			super.drawTextBox();
			
			this.variableButton.x = this.x + this.width + 5;
			this.variableButton.y = this.y;
			this.variableButton.drawButton(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());

			if (this.renderContextMenu) {
				this.renderContextMenu();
			}
			
		}
	}

	public void renderContextMenu() {
		float scale = UIBase.getUIScale();
		MouseInput.setRenderScale(scale);
		GlStateManager.pushMatrix();
		GlStateManager.scale(scale, scale, scale);
		if (this.variableMenu != null) {
			this.variableMenu.render( MouseInput.getMouseX(), MouseInput.getMouseY());
		}
		GlStateManager.popMatrix();
		MouseInput.resetRenderScale();
	}

}
