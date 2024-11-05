package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.File;
import java.util.function.Consumer;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMFilePickerPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class ChooseFilePopup extends FMTextInputPopup {

	protected AdvancedButton chooseFileBtn;
	private String[] fileTypes;
	
	public ChooseFilePopup(Consumer<String> callback, String... fileTypes) {
		super(new Color(0, 0, 0, 0), Locals.localize("helper.creator.choosefile.enterorchoose"), null, 0, callback);
		this.fileTypes = fileTypes;
	}
	
	@Override
	protected void init(Color color, String title, CharacterFilter filter, Consumer<String> callback) {
		super.init(color, title, filter, callback);
		
		this.chooseFileBtn = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.choosefile.choose"), true, (press) -> {
			PopupHandler.displayPopup(new FMFilePickerPopup(Minecraft.getMinecraft().mcDataDir.getAbsoluteFile().getAbsolutePath().replace("\\", "/"), Minecraft.getMinecraft().mcDataDir.getAbsoluteFile().getAbsolutePath().replace("\\", "/"), this, true, (call) -> {
				if (call != null) {
					String path = call.getAbsolutePath().replace("\\", "/");
					File home = Minecraft.getMinecraft().mcDataDir;
					if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
						path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
						if (path.startsWith("\\") || path.startsWith("/")) {
							path = path.substring(1);
						}
					}
					path = path.replace("\\", "/");
					this.setText(path);
				}
			}, fileTypes));
		});
		this.addButton(chooseFileBtn);
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		if (!this.isDisplayed()) {
			return;
		}
		GlStateManager.enableBlend();
		GuiScreen.drawRect(0, 0, renderIn.width, renderIn.height, new Color(0, 0, 0, 240).getRGB());
		GlStateManager.disableBlend();
		
		renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, title, renderIn.width / 2, (renderIn.height / 2) - 40, Color.WHITE.getRGB());
		
		this.textField.x = (renderIn.width / 2) - (this.textField.getWidth() / 2);
		this.textField.y = (renderIn.height / 2) - (this.textField.height / 2);
		this.textField.drawTextBox();
		
		this.doneButton.x = (renderIn.width / 2) - (this.doneButton.width / 2);
		this.doneButton.y = ((renderIn.height / 2) + 100) - this.doneButton.height - 5;
		
		this.chooseFileBtn.x = (renderIn.width / 2) - (this.doneButton.width / 2);
		this.chooseFileBtn.y = ((renderIn.height / 2) + 50) - this.doneButton.height - 5;
		
		this.renderButtons(mouseX, mouseY);
	}

}
