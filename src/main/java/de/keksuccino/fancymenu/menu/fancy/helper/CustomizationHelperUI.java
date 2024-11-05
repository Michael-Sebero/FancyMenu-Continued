package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.io.Files;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroScreen;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.CreateCustomGuiPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.MenuBar;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.MenuBar.ElementAlignment;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
import de.keksuccino.fancymenu.menu.variables.VariableHandler;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.screens.ConfigScreen;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CustomizationHelperUI extends UIBase {
	
	public static MenuBar bar;
	
	public static boolean showButtonInfo = false;
	public static boolean showMenuInfo = false;
	protected static List<ButtonData> buttons = new ArrayList<ButtonData>();
	protected static int tick = 0;
	protected static long lastButtonInfoRightClick = 0;
	
	protected static final ResourceLocation CLOSE_BUTTON_TEXTURE = new ResourceLocation("keksuccino", "close_btn.png");
	protected static final ResourceLocation RELOAD_BUTTON_TEXTURE = new ResourceLocation("keksuccino", "filechooser/back_icon.png");
	
	public static void init() {
		
		MinecraftForge.EVENT_BUS.register(new CustomizationHelperUI());
		
	}
	
	public static void updateUI() {
		try {
			
			boolean extended = true;
			if (bar != null) {
				extended = bar.isExtended();
			}
			
			bar = new MenuBar();
			
			/** CURRENT MENU TAB START **/
			FMContextMenu currentMenu = new FMContextMenu();
			currentMenu.setAutoclose(true);
			bar.addChild(currentMenu, "fm.ui.tab.current", ElementAlignment.LEFT);
			
			String toggleLabel = Locals.localize("helper.popup.togglecustomization.enable");
			if (MenuCustomization.isMenuCustomizable(Minecraft.getMinecraft().currentScreen)) {
				toggleLabel = Locals.localize("helper.popup.togglecustomization.disable");
			}
			CustomizationButton toggleCustomizationButton = new CustomizationButton(0, 0, 0, 0, toggleLabel, true, (press) -> {
				if (MenuCustomization.isMenuCustomizable(Minecraft.getMinecraft().currentScreen)) {
					press.displayString = Locals.localize("helper.popup.togglecustomization.enable");
					MenuCustomization.disableCustomizationForMenu(Minecraft.getMinecraft().currentScreen);
					CustomizationHelper.reloadSystemAndMenu();
				} else {
					press.displayString = Locals.localize("helper.popup.togglecustomization.disable");
					MenuCustomization.enableCustomizationForMenu(Minecraft.getMinecraft().currentScreen);
					CustomizationHelper.reloadSystemAndMenu();
				}
			});
			toggleCustomizationButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.onoff.btndesc"), "%n%"));
			currentMenu.addContent(toggleCustomizationButton);
			
			FMContextMenu layoutsMenu = new FMContextMenu();
			layoutsMenu.setAutoclose(true);
			currentMenu.addChild(layoutsMenu);
			
			CustomizationButton newLayoutButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.new"), true, (press) -> {
				LayoutEditorScreen.isActive = true;
				GuiScreen s = Minecraft.getMinecraft().currentScreen;
				Minecraft.getMinecraft().displayGuiScreen(new LayoutEditorScreen(s));
				MenuCustomization.stopSounds();
				MenuCustomization.resetSounds();
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							((AdvancedAnimation)r).resetAnimation();
						}
					}
				}
			});
			newLayoutButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.new.desc"), "%n%"));
			layoutsMenu.addContent(newLayoutButton);

			ManageLayoutsContextMenu manageLayoutsMenu = new ManageLayoutsContextMenu(false);
			manageLayoutsMenu.setAutoclose(true);
			layoutsMenu.addChild(manageLayoutsMenu);
			
			CustomizationButton manageLayoutsButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.manage"), true, (press) -> {
				manageLayoutsMenu.setParentButton((AdvancedButton) press);
				manageLayoutsMenu.openMenuAt(press);
			});
			manageLayoutsButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.manage.desc"), "%n%"));
			layoutsMenu.addContent(manageLayoutsButton);
			
			CustomizationButton layoutsButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts"), true, (press) -> {
				layoutsMenu.setParentButton((AdvancedButton) press);
				layoutsMenu.openMenuAt(0, press.y);
			});
			if (!MenuCustomization.isMenuCustomizable(Minecraft.getMinecraft().currentScreen)) {
				layoutsButton.enabled = false;
			}
			layoutsButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.desc"), "%n%"));
			currentMenu.addContent(layoutsButton);
			
			FMContextMenu advancedMenu = new FMContextMenu();
			advancedMenu.setAutoclose(true);
			currentMenu.addChild(advancedMenu);
			
			OverrideMenuContextMenu overrideMenu = new OverrideMenuContextMenu();
			overrideMenu.setAutoclose(true);
			advancedMenu.addChild(overrideMenu);
			
			String overrLabel = Locals.localize("helper.buttons.tools.overridemenu");
			if (CustomizationHelper.isScreenOverridden(Minecraft.getMinecraft().currentScreen)) {
				overrLabel = Locals.localize("helper.buttons.tools.resetoverride");
			}
			CustomizationButton overrideButton = new CustomizationButton(0, 0, 0, 0, overrLabel, true, (press) -> {
				
				if (!CustomizationHelper.isScreenOverridden(Minecraft.getMinecraft().currentScreen)) {
					
					overrideMenu.setParentButton((AdvancedButton) press);
					overrideMenu.openMenuAt(0, press.y);
					
				} else {

					for (String s : FileUtils.getFiles(FancyMenu.getCustomizationPath().getPath())) {
						PropertiesSet props = PropertiesSerializer.getProperties(s);
						if (props == null) {
							continue;
						}
						PropertiesSet props2 = new PropertiesSet(props.getPropertiesType());
						List<PropertiesSection> l = props.getProperties();
						List<PropertiesSection> l2 = new ArrayList<PropertiesSection>();
						boolean b = false;

						List<PropertiesSection> metas = props.getPropertiesOfType("customization-meta");
						if ((metas == null) || metas.isEmpty()) {
							metas = props.getPropertiesOfType("type-meta");
						}
						if (metas != null) {
							if (metas.isEmpty()) {
								continue;
							}
							String identifier = metas.get(0).getEntryValue("identifier");
							GuiScreen overridden = ((CustomGuiBase)Minecraft.getMinecraft().currentScreen).getOverriddenScreen();
							if ((identifier == null) || !identifier.equalsIgnoreCase(overridden.getClass().getName())) {
								continue;
							}

						} else {
							continue;
						}

						for (PropertiesSection sec : l) {
							String action = sec.getEntryValue("action");
							if (sec.getSectionType().equalsIgnoreCase("customization-meta") || sec.getSectionType().equalsIgnoreCase("type-meta")) {
								l2.add(sec);
								continue;
							}
							if ((action != null) && !action.equalsIgnoreCase("overridemenu")) {
								l2.add(sec);
							}
							if ((action != null) && action.equalsIgnoreCase("overridemenu")) {
								b = true;
							}
						}

						if (b) {
							File f = new File(s);
							if (f.exists() && f.isFile()) {
								f.delete();
							}

							if (l2.size() > 1) {
								for (PropertiesSection sec : l2) {
									props2.addProperties(sec);
								}

								PropertiesSerializer.writeProperties(props2, s);
							}
						}
					}

					CustomizationHelper.reloadSystemAndMenu();
					if (Minecraft.getMinecraft().currentScreen instanceof CustomGuiBase) {
						Minecraft.getMinecraft().displayGuiScreen(((CustomGuiBase) Minecraft.getMinecraft().currentScreen).getOverriddenScreen());
					}
				}
			});
			overrideButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.overridewith.btndesc"), "%n%"));
			if (!(Minecraft.getMinecraft().currentScreen instanceof CustomGuiBase)) {
				advancedMenu.addContent(overrideButton);
			} else if (((CustomGuiBase)Minecraft.getMinecraft().currentScreen).getOverriddenScreen() != null) {
				advancedMenu.addContent(overrideButton);
			}
			
			CustomizationButton advancedButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.advanced"), true, (press) -> {
				advancedMenu.setParentButton((AdvancedButton) press);
				advancedMenu.openMenuAt(0, press.y);
			});
			if (!MenuCustomization.isMenuCustomizable(Minecraft.getMinecraft().currentScreen)) {
				advancedButton.enabled = false;
			}
			advancedButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.advanced.desc"), "%n%"));
			if (FancyMenu.config.getOrDefault("advancedmode", false)) {
				currentMenu.addContent(advancedButton);
			}
			
			CustomizationButton closeCustomGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.closegui"), true, (press) -> {
				Minecraft.getMinecraft().displayGuiScreen(null);
			});
			closeCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			if ((Minecraft.getMinecraft().currentScreen instanceof CustomGuiBase) && (((CustomGuiBase)Minecraft.getMinecraft().currentScreen).getOverriddenScreen() == null)) {
				currentMenu.addContent(closeCustomGuiButton);
			}
			
			CustomizationButton currentTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current"), true, (press) -> {
				currentMenu.setParentButton((AdvancedButton) press);
				currentMenu.openMenuAt(press.x, press.y + press.height);
			});
			bar.addElement(currentTab, "fm.ui.tab.current", ElementAlignment.LEFT, false);
			/** CURRENT MENU TAB END **/

			/** UNIVERSAL LAYOUTS START **/
			FMContextMenu universalLayoutsMenu = new FMContextMenu();
			universalLayoutsMenu.setAutoclose(true);
			bar.addChild(universalLayoutsMenu, "fm.ui.tab.universal_layouts", ElementAlignment.LEFT);

			CustomizationButton newUniversalLayoutButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.universal_layouts.new"), true, (press) -> {
				LayoutEditorScreen.isActive = true;
				Minecraft.getMinecraft().displayGuiScreen(new LayoutEditorScreen(new CustomGuiBase("", "%fancymenu:universal_layout%", true, Minecraft.getMinecraft().currentScreen, null)));
				MenuCustomization.stopSounds();
				MenuCustomization.resetSounds();
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							r.resetAnimation();
						}
					}
				}
			});
			universalLayoutsMenu.addContent(newUniversalLayoutButton);

			ManageLayoutsContextMenu manageUniversalLayoutsMenu = new ManageLayoutsContextMenu(true);
			manageUniversalLayoutsMenu.setAutoclose(true);
			universalLayoutsMenu.addChild(manageUniversalLayoutsMenu);

			CustomizationButton manageUniversalLayoutsButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.universal_layouts.manage"), true, (press) -> {
				manageUniversalLayoutsMenu.setParentButton((AdvancedButton) press);
				manageUniversalLayoutsMenu.openMenuAt(press);
			});
			universalLayoutsMenu.addContent(manageUniversalLayoutsButton);

			CustomizationButton universalLayoutsTabButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.universal_layouts"), true, (press) -> {
				universalLayoutsMenu.setParentButton((AdvancedButton) press);
				universalLayoutsMenu.openMenuAt(press.x, press.y + press.height);
			});
			universalLayoutsTabButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.universal_layouts.btn.desc"), "%n%"));
			bar.addElement(universalLayoutsTabButton, "fm.ui.tab.universal_layouts", ElementAlignment.LEFT, false);
			/** UNIVERSAL LAYOUTS END **/

			/** SETUP TAB START **/
			FMContextMenu setupMenu = new FMContextMenu();
			setupMenu.setAutoclose(true);
			bar.addChild(setupMenu, "fm.ui.tab.setup_import_export", ElementAlignment.LEFT);

			CustomizationButton setupTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.setup"), true, (press) -> {
				setupMenu.setParentButton((AdvancedButton) press);
				setupMenu.openMenuAt(press.x, press.y + press.height);
			});
			bar.addElement(setupTab, "fm.ui.tab.setup_import_export", ElementAlignment.LEFT, false);

			//Export Button
			CustomizationButton exportSetupButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.setup.export"), true, (press) -> {
				SetupSharingEngine.exportSetup();
			});
			exportSetupButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.setup.export.btn.desc"), "%n%"));
			setupMenu.addContent(exportSetupButton);

			//Import Menu
			FMContextMenu importMenu = new FMContextMenu();
			importMenu.setAutoclose(true);
			setupMenu.addChild(importMenu);

			//Import Button
			CustomizationButton importSetupButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.setup.import"), true, (press) -> {
				importMenu.setParentButton((AdvancedButton) press);
				importMenu.openMenuAt(0, press.y);
			});
			importSetupButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.setup.import.btn.desc"), "%n%"));
			setupMenu.addContent(importSetupButton);

			//Import -> Choose From Saved Setups
			CustomizationButton chooseFromSavedButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved"), true, (press) -> {
				SetupSharingEngine.importSetupFromSavedSetups();
			});
			chooseFromSavedButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.setupsharing.import.choosefromsaved.btn.desc"), "%n%"));
			importMenu.addContent(chooseFromSavedButton);

			//Import -> Enter Path
			CustomizationButton enterPathButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.setupsharing.import.choosefrompath"), true, (press) -> {
				SetupSharingEngine.importSetup();
			});
			enterPathButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.setupsharing.import.choosefrompath.btn.desc"), "%n%"));
			importMenu.addContent(enterPathButton);

			//Restore Menu
			FMContextMenu restoreMenu = new FMContextMenu();
			restoreMenu.setAutoclose(true);
			setupMenu.addChild(restoreMenu);

			//Restore Button
			CustomizationButton restoreButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.setup.restore"), true, (press) -> {
				restoreMenu.setParentButton((AdvancedButton) press);
				restoreMenu.openMenuAt(0, press.y);
			});
			restoreButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.setup.restore.btn.desc"), "%n%"));
			setupMenu.addContent(restoreButton);

			//Add entries to restore menu
			try {
				File backups = SetupSharingEngine.SETUP_BACKUP_DIR;
				backups.mkdirs();
				boolean hasContent = false;
				File[] backupsSorted = SetupSharingEngine.sortByDateModified(backups.listFiles());
				for (File f : backupsSorted) {
					if (SetupSharingEngine.isValidSetup(f.getPath())) {
						String btnName = "Backup";
						try {
							LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
							DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
							dtf.withZone(ZoneId.systemDefault());
							dtf.withLocale(Locale.getDefault());
							btnName = dt.format(dtf);
						} catch (Exception e3) {
							e3.printStackTrace();
						}
						CustomizationButton backupEntryButton = new CustomizationButton(0, 0, 0, 0, btnName, true, (press) -> {
							FMYesNoPopup backupConfirmPop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
								if (call) {
									new Thread(() -> {
										SetupSharingEngine.StatusPopup restoreBlockerPopup = new SetupSharingEngine.StatusPopup(Locals.localize("fancymenu.helper.setupsharing.restore.restoring"));
										PopupHandler.displayPopup(restoreBlockerPopup);
										try {
											try {
												
												File fmFolder = FancyMenu.MOD_DIR;
												if (fmFolder.isDirectory()) {
													org.apache.commons.io.FileUtils.deleteDirectory(fmFolder);
												}
											} catch (Exception e2) {
												e2.printStackTrace();
											}

											
											File homeRaw = Minecraft.getMinecraft().mcDataDir;
											File home = new File(homeRaw.getAbsolutePath().replace("\\", "/"));
											if (home.isDirectory()) {
												//Check that we're really in the correct dir, because I'm paranoid
												File config = new File(home.getPath() + "/config");
												if (config.isDirectory()) {
													File backupInstance = new File(f.getPath() + "/setup");
													if (backupInstance.isDirectory()) {
														org.apache.commons.io.FileUtils.copyDirectory(backupInstance, home);
														FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, () -> {
															CustomizationHelper.reloadSystemAndMenu();
														}, Locals.localize("fancymenu.helper.setupsharing.restore.success"));
														PopupHandler.displayPopup(pop);
													}
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
											FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
												if (call2) {
													try {
														CustomizationHelper.openFile(SetupSharingEngine.SETUP_BACKUP_DIR);
													} catch (Exception e3) {
														e3.printStackTrace();
													}
												}
												CustomizationHelper.reloadSystemAndMenu();
											}, Locals.localize("fancymenu.helper.setupsharing.restore.error"));
											PopupHandler.displayPopup(pop);
										}
										restoreBlockerPopup.setDisplayed(false);
									}).start();
								}
							}, Locals.localize("fancymenu.helper.setupsharing.restore.confirm"));
							PopupHandler.displayPopup(backupConfirmPop);
						});
						backupEntryButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.setup.restore.entry.btn.desc"), "%n%"));
						restoreMenu.addContent(backupEntryButton);
						hasContent = true;
					}
				}
				if (!hasContent) {
					CustomizationButton emptyButton = new CustomizationButton(0, 0, 0, 0, "----------", true, (press) -> {});
					restoreMenu.addContent(emptyButton);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			/** SETUP TAB END **/
			
			/** CUSTOM GUI TAB START **/
			FMContextMenu customGuiMenu = new FMContextMenu();
			customGuiMenu.setAutoclose(true);
			bar.addChild(customGuiMenu, "fm.ui.tab.customguis", ElementAlignment.LEFT);
			
			CustomizationButton newCustomGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis.new"), true, (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						PopupHandler.displayPopup(new CreateCustomGuiPopup());
					}
				}, Locals.localize("helper.ui.customguis.new.sure")));
			});
			newCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.customguis.new.desc"), "%n%"));
			customGuiMenu.addContent(newCustomGuiButton);
			
			ManageCustomGuiContextMenu manageCustomGuiMenu = new ManageCustomGuiContextMenu();
			manageCustomGuiMenu.setAutoclose(true);
			customGuiMenu.addChild(manageCustomGuiMenu);
			
			CustomizationButton manageCustomGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis.manage"), true, (press) -> {
				manageCustomGuiMenu.setParentButton((AdvancedButton) press);
				manageCustomGuiMenu.openMenuAt(0, press.y);
			});
			manageCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.customguis.manage.desc"), "%n%"));
			customGuiMenu.addContent(manageCustomGuiButton);
			
			CustomizationButton customGuiTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis"), true, (press) -> {
				customGuiMenu.setParentButton((AdvancedButton) press);
				customGuiMenu.openMenuAt(press.x, press.y + press.height);
			});
			if (FancyMenu.config.getOrDefault("advancedmode", false)) {
				bar.addElement(customGuiTab, "fm.ui.tab.customguis", ElementAlignment.LEFT, false);
			}
			/** CUSTOM GUI TAB END **/
			
			/** TOOLS TAB START **/
			FMContextMenu toolsMenu = new FMContextMenu();
			toolsMenu.setAutoclose(true);
			bar.addChild(toolsMenu, "fm.ui.tab.tools", ElementAlignment.LEFT);
			
			CustomizationButton menuInfoButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.tools.menuinfo.off"), true, (press) -> {
				if (showMenuInfo) {
					showMenuInfo = false;
					((AdvancedButton)press).displayString = Locals.localize("helper.ui.tools.menuinfo.off");
				} else {
					showMenuInfo = true;
					((AdvancedButton)press).displayString = Locals.localize("helper.ui.tools.menuinfo.on");
				}
			});
			if (showMenuInfo) {
				menuInfoButton.displayString = Locals.localize("helper.ui.tools.menuinfo.on");
			}
			menuInfoButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.tools.menuinfo.desc"), "%n%"));
			toolsMenu.addContent(menuInfoButton);

			CustomizationButton buttonInfoButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.tools.buttoninfo.off"), true, (press) -> {
				if (showButtonInfo) {
					showButtonInfo = false;
					((AdvancedButton)press).displayString = Locals.localize("helper.ui.tools.buttoninfo.off");
				} else {
					showButtonInfo = true;
					((AdvancedButton)press).displayString = Locals.localize("helper.ui.tools.buttoninfo.on");
				}
			}) {
				@Override
				public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
					GuiScreen current = Minecraft.getMinecraft().currentScreen;
					if ((current != null) && MenuCustomization.isMenuCustomizable(current)) {
						this.enabled = true;
						this.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.tools.buttoninfo.desc"), "%n%"));
					} else {
						this.enabled = false;
						this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations"), "%n%"));
					}
					super.drawButton(mc, mouseX, mouseY, partialTicks);
				}
			};
			if (showButtonInfo) {
				buttonInfoButton.displayString = Locals.localize("helper.ui.tools.buttoninfo.on");
			}
			toolsMenu.addContent(buttonInfoButton);

			CustomizationButton clearVariablesButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.ui.tools.clear_variables"), true, (press) -> {
				FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call) -> {
					if (call) {
						VariableHandler.clearVariables();
					}
				}, StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.tools.clear_variables.confirm"), "%n%"));
				PopupHandler.displayPopup(p);
			});
			clearVariablesButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.tools.clear_variables.desc"), "%n%"));
			toolsMenu.addContent(clearVariablesButton);
			
			CustomizationButton toolsTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.tools"), true, (press) -> {
				toolsMenu.setParentButton((AdvancedButton) press);
				toolsMenu.openMenuAt(press.x, press.y + press.height);
			});
			bar.addElement(toolsTab, "fm.ui.tab.tools", ElementAlignment.LEFT, false);
			/** TOOLS TAB END **/
			
			/** MISCELLANEOUS TAB START **/
			FMContextMenu miscMenu = new FMContextMenu();
			miscMenu.setAutoclose(true);
			bar.addChild(miscMenu, "fm.ui.tab.misc", ElementAlignment.LEFT);
			
			CustomizationButton closeGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.closegui"), true, (press) -> {
				Minecraft.getMinecraft().displayGuiScreen(null);
			});
			closeGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			miscMenu.addContent(closeGuiButton);

			CustomizationButton miscTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.misc"), true, (press) -> {
				miscMenu.setParentButton((AdvancedButton) press);
				miscMenu.openMenuAt(press.x, press.y + press.height);
			});
			bar.addElement(miscTab, "fm.ui.tab.misc", ElementAlignment.LEFT, false);
			/** MISCELLANEOUS TAB END **/
			
			/** CLOSE GUI BUTTON START **/
			AdvancedImageButton closeGuiButtonTab = new AdvancedImageButton(20, 20, 20, 20, CLOSE_BUTTON_TEXTURE, true, (press) -> {
				Minecraft.getMinecraft().displayGuiScreen(null);
			}) {
				@Override
				public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
					this.width = this.height;
					super.drawButton(mc, mouseX, mouseY, partialTicks);
				}
			};
			closeGuiButtonTab.ignoreLeftMouseDownClickBlock = true;
			closeGuiButtonTab.ignoreBlockedInput = true;
			closeGuiButtonTab.enableRightclick = true;
			closeGuiButtonTab.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			bar.addElement(closeGuiButtonTab, "fm.ui.tab.closegui", ElementAlignment.RIGHT, false);
			/** CLOSE GUI BUTTON END **/
			
			/** RELOAD BUTTON START **/
			AdvancedImageButton reloadButtonTab = new AdvancedImageButton(20, 20, 20, 20, RELOAD_BUTTON_TEXTURE, true, (press) -> {
				CustomizationHelper.reloadSystemAndMenu();
			}) {
				@Override
				public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
					this.width = this.height;
					super.drawButton(mc, mouseX, mouseY, partialTicks);
				}
			};
			reloadButtonTab.ignoreLeftMouseDownClickBlock = true;
			reloadButtonTab.ignoreBlockedInput = true;
			reloadButtonTab.enableRightclick = true;
			reloadButtonTab.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.reload.desc"), "%n%"));
			bar.addElement(reloadButtonTab, "fm.ui.tab.reload", ElementAlignment.RIGHT, false);
			/** RELOAD BUTTON END **/
			
			AdvancedButton expandButton = bar.getElement("menubar.default.extendbtn");
			if (expandButton != null) {
				if (expandButton instanceof AdvancedImageButton) {
					if (!extended) {
						((AdvancedImageButton)expandButton).setImage(MenuBar.EXPAND_BTN_TEXTURE);
						expandButton.setDescription(StringUtils.splitLines(Locals.localize("helper.menubar.expand"), "%n%"));
					}
				}
			}
			
			bar.setExtended(extended);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void render(GuiScreen screen) {
		try {
			
			if (bar != null) {
				if (!PopupHandler.isPopupActive()) { 
					if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
						if (!(screen instanceof LayoutEditorScreen) && !(screen instanceof ConfigScreen) && !(screen instanceof GameIntroScreen) && AnimationHandler.isReady() && MenuCustomization.isValidScreen(screen)) {
							
							RenderUtils.setZLevelPre(400);
							
							renderMenuInfo(screen);
							
							renderUnicodeWarning(screen);
							
							renderButtonInfo(screen);

							renderButtonInfoWarning(screen);
							
							RenderUtils.setZLevelPost();
							
							bar.render(screen);
							
						}
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static void renderButtonInfo(GuiScreen screen) {
		if (showButtonInfo) {
			boolean isButtonHovered = false;
			for (ButtonData d : buttons) {
				if (d.getButton().isMouseOver()) {
					isButtonHovered = true;
					long id = d.getId();
					String idString = Locals.localize("helper.buttoninfo.idnotfound");
					if (id >= 0) {
						idString = String.valueOf(id);
					}
					if (d.getCompatibilityId() != null) {
						idString = d.getCompatibilityId();
					}
					String key = ButtonCache.getKeyForButton(d.getButton());
					if (key == null) {
						key = Locals.localize("helper.buttoninfo.keynotfound");
					}
					
					List<String> info = new ArrayList<>();
					int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(Locals.localize("helper.button.buttoninfo")) + 10;

					long now = System.currentTimeMillis();

					info.add("§f" + Locals.localize("helper.buttoninfo.id") + ": " + idString);
					info.add("§f" + Locals.localize("general.width") + ": " + d.getButton().width);
					info.add("§f" + Locals.localize("general.height") + ": " + d.getButton().height);
					info.add("§f" + Locals.localize("helper.buttoninfo.labelwidth") + ": " + Minecraft.getMinecraft().fontRenderer.getStringWidth(d.getButton().displayString));
					info.add("");
					if (lastButtonInfoRightClick + 2000 < now) {
						info.add(Locals.localize("fancymenu.helper.button_info.copy_locator"));
					} else {
						info.add(Locals.localize("fancymenu.helper.button_info.copy_locator.copied"));
					}

					if (MouseInput.isRightMouseDown()) {
						GuiScreen current = Minecraft.getMinecraft().currentScreen;
						String locator = current.getClass().getName() + ":" + idString;
						GuiScreen.setClipboardString(locator);
						lastButtonInfoRightClick = now;
					}

					for (String s : info) {
						int i = Minecraft.getMinecraft().fontRenderer.getStringWidth(s) + 10;
						if (i > width) {
							width = i;
						}
					}
					
					GlStateManager.pushMatrix();
					
					GlStateManager.scale(getUIScale(), getUIScale(), getUIScale());
					
					MouseInput.setRenderScale(getUIScale());
					
					int x = MouseInput.getMouseX();
					if ((screen.width / getUIScale()) < x + width + 10) {
						x -= width + 10;
					}
					
					int y = MouseInput.getMouseY();
					if ((screen.height / getUIScale()) < y + 80) {
						y -= 90;
					}
					
					drawRect(x, y, x + width + 10, y + 100, new Color(102, 0, 102, 200).getRGB());
					
					GlStateManager.enableBlend();
					screen.drawString(Minecraft.getMinecraft().fontRenderer, "§f§l" + Locals.localize("helper.button.buttoninfo"), x + 10, y + 10, -1);

					int i2 = 20;
					for (String s : info) {
						screen.drawString(Minecraft.getMinecraft().fontRenderer, s, x + 10, y + 10 + i2, -1);
						i2 += 10;
					}
					
					MouseInput.resetRenderScale();
					
					GlStateManager.popMatrix();
					
					GlStateManager.disableBlend();
					
					break;
				}
			}
			if (!isButtonHovered) {
				lastButtonInfoRightClick = 0;
			}
		}
	}

	protected static void renderButtonInfoWarning(GuiScreen screen) {
		if (showButtonInfo && !MenuCustomization.isMenuCustomizable(screen)) {
			List<String> info = new ArrayList<>();
			int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations.cursorwarning.line1")) + 10;

			info.add(Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations.cursorwarning.line2"));
			info.add(Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations.cursorwarning.line3"));

			for (String s : info) {
				int i = Minecraft.getMinecraft().fontRenderer.getStringWidth(s) + 10;
				if (i > width) {
					width = i;
				}
			}

			GlStateManager.pushMatrix();

			GlStateManager.scale(getUIScale(), getUIScale(), getUIScale());

			MouseInput.setRenderScale(getUIScale());

			int x = MouseInput.getMouseX();
			if ((screen.width / getUIScale()) < x + width + 10) {
				x -= width + 10;
			}

			int y = MouseInput.getMouseY();
			if ((screen.height / getUIScale()) < y + 80) {
				y -= 90;
			}

			drawRect(x, y, x + width + 10, y + 60, new Color(230, 15, 0, 240).getRGB());

			GlStateManager.enableBlend();
			screen.drawString(Minecraft.getMinecraft().fontRenderer, "§f§l" + Locals.localize("fancymenu.helper.ui.tools.buttoninfo.enablecustomizations.cursorwarning.line1"), x + 10, y + 10, -1);

			int i2 = 20;
			for (String s : info) {
				screen.drawString(Minecraft.getMinecraft().fontRenderer, s, x + 10, y + 10 + i2, -1);
				i2 += 10;
			}

			MouseInput.resetRenderScale();

			GlStateManager.popMatrix();

			GlStateManager.disableBlend();
		}
	}
	
	protected static void renderMenuInfo(GuiScreen screen) {
		if (showMenuInfo) {
			String infoTitle = "§f§l" + Locals.localize("helper.menuinfo.identifier") + ":";
			String id = "";
			if (screen instanceof CustomGuiBase) {
				id = ((CustomGuiBase)screen).getIdentifier();
			} else {
				id = screen.getClass().getName();
			}
			int w = Minecraft.getMinecraft().fontRenderer.getStringWidth(infoTitle);
			int w2 = Minecraft.getMinecraft().fontRenderer.getStringWidth(id);
			if (w2 > w) {
				w = w2;
			}
			int h = bar.getHeight() + 5;
			
			GlStateManager.enableBlend();
			
			GlStateManager.pushMatrix();
			
			GlStateManager.scale(getUIScale(), getUIScale(), getUIScale());
			
			drawRect(3, h, 3 + w + 4, h + 23, new Color(0, 0, 0, 240).getRGB());

			screen.drawString(Minecraft.getMinecraft().fontRenderer, infoTitle, 5, h + 2, 0);
			if (tick == 0) {
				screen.drawString(Minecraft.getMinecraft().fontRenderer, "§f" + id, 5, h + 13, 0);
			} else {
				screen.drawString(Minecraft.getMinecraft().fontRenderer, "§a" + Locals.localize("helper.menuinfo.idcopied"), 5, h + 13, 0);
			}

			MouseInput.setRenderScale(getUIScale());
			
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			if (!bar.isChildOpen()) {
				if ((mouseX >= 5) && (mouseX <= 5 + w2) && (mouseY >= h + 13) && (mouseY <= h + 13 + 10) && (tick == 0)) {
					drawRect(5, h + 13 + 10 - 1, 5 + w2, h + 13 + 10, -1);
					
					if (MouseInput.isLeftMouseDown()) {
						tick++;
						GuiScreen.setClipboardString(id);
					}
				}
			}
			if (tick > 0) {
				if (tick < 60) {
					tick++;
				} else {
					tick = 0;
				}
			}
			
			MouseInput.resetRenderScale();
			
			GlStateManager.popMatrix();
			
			GlStateManager.disableBlend();
		}
	}
	
	protected static void renderUnicodeWarning(GuiScreen screen) {
		if (!FancyMenu.config.getOrDefault("show_unicode_warning", true)) {
			return;
		}
		if (Minecraft.getMinecraft().gameSettings.forceUnicodeFont) {
			String title = Locals.localize("helper.ui.warning");
			int w = Minecraft.getMinecraft().fontRenderer.getStringWidth(title);
			String[] lines = StringUtils.splitLines(Locals.localize("helper.ui.warning.unicode"), "%n%");
			for (String s : lines) {
				int w2 = Minecraft.getMinecraft().fontRenderer.getStringWidth(s);
				if (w2 > w) {
					w = w2;
				}
			}
			
			int x = screen.width - w - 5;
			int y = (int) ((bar.getHeight() + 5) * UIBase.getUIScale());
			
			GlStateManager.enableBlend();
			
			int h = 13;
			if (lines.length > 0) {
				h += 10*lines.length;
			}
			drawRect(x - 4, y, x + w + 2, y + h, new Color(230, 15, 0, 240).getRGB());

			screen.drawString(Minecraft.getMinecraft().fontRenderer, title, x, y + 2, Color.WHITE.getRGB());
			
			int i = 0;
			for (String s : lines) {
				screen.drawString(Minecraft.getMinecraft().fontRenderer, s, x, y + 13 + i, Color.WHITE.getRGB());
				i += 10;
			}
			
			GlStateManager.disableBlend();
		}
	}
	
	@SubscribeEvent
	public void onButtonsCached(ButtonCachedEvent e) {
		buttons = e.getButtonDataList();
	}
	
	@SubscribeEvent
	public void onInitScreen(GuiScreenEvent.InitGuiEvent.Pre e) {
		try {
			
			if (e.getGui() != null) {
				if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
					
					updateUI();
					
				}
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static class ManageCustomGuiContextMenu extends FMContextMenu {

		private ManageCustomGuiSubContextMenu manageMenu;

		public ManageCustomGuiContextMenu() {
			
			this.manageMenu = new ManageCustomGuiSubContextMenu();
			this.addChild(this.manageMenu);

		}

		@Override
		public void openMenuAt(int x, int y) {

			this.content.clear();

			List<String> l = CustomGuiLoader.getCustomGuis();
			if (!l.isEmpty()) {

				this.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.openbyname"), true, (press) -> {
					PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.openbyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								Minecraft.getMinecraft().displayGuiScreen(CustomGuiLoader.getGui(call, Minecraft.getMinecraft().currentScreen, null));
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));

				this.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.deletebyname"), true, (press) -> {
					PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.deletebyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
									if (call2) {
										if (CustomGuiLoader.guiExists(call)) {
											List<File> delete = new ArrayList<File>();
											for (String s : FileUtils.getFiles(FancyMenu.getCustomGuiPath().getPath())) {
												File f = new File(s);
												for (String s2 : FileUtils.getFileLines(f)) {
													if (s2.replace(" ", "").toLowerCase().equals("identifier=" + call)) {
														delete.add(f);
													}
												}
											}

											for (File f : delete) {
												if (f.isFile()) {
													f.delete();
												}
											}

											CustomizationHelper.reloadSystemAndMenu();
										}
									}
								}, Locals.localize("helper.buttons.tools.customguis.sure")));
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));
				
				this.addSeparator();

				for (String s : l) {
					String label = s;
					if (Minecraft.getMinecraft().fontRenderer.getStringWidth(label) > 80) {
						label = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(label, 75) + "..";
					}

					this.addContent(new CustomizationButton(0, 0, 0, 0, label, true, (press) -> {
						this.manageMenu.setParentButton((AdvancedButton) press);
						this.manageMenu.openMenuAt(0, press.y, s);
					}));
				}

			}
			
			super.openMenuAt(x, y);

		}
		
	}
	
	private static class ManageCustomGuiSubContextMenu extends FMContextMenu {

		public void openMenuAt(int x, int y, String customGuiIdentifier) {
			this.content.clear();
			
			CustomizationButton openMenuButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.open"), (press) -> {
				if (CustomGuiLoader.guiExists(customGuiIdentifier)) {
					Minecraft.getMinecraft().displayGuiScreen(CustomGuiLoader.getGui(customGuiIdentifier, Minecraft.getMinecraft().currentScreen, null));
				}
			});
			this.addContent(openMenuButton);

			CustomizationButton deleteMenuButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.delete"), (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						if (CustomGuiLoader.guiExists(customGuiIdentifier)) {
							List<File> delete = new ArrayList<File>();
							for (String s : FileUtils.getFiles(FancyMenu.getCustomGuiPath().getPath())) {
								File f = new File(s);
								for (String s2 : FileUtils.getFileLines(f)) {
									if (s2.replace(" ", "").toLowerCase().equals("identifier=" + customGuiIdentifier)) {
										delete.add(f);
									}
								}
							}

							for (File f : delete) {
								if (f.isFile()) {
									f.delete();
								}
							}

							CustomizationHelper.reloadSystemAndMenu();
						}
					}
				}, Locals.localize("helper.buttons.tools.customguis.sure")));
			});
			this.addContent(deleteMenuButton);

			this.openMenuAt(x, y);
		}
		
	}

	private static class ManageLayoutsContextMenu extends FMContextMenu {

		private ManageLayoutsSubContextMenu manageSubPopup;
		private boolean isUniversal;

		public ManageLayoutsContextMenu(boolean isUniversal) {

			this.isUniversal = isUniversal;
			
			this.manageSubPopup = new ManageLayoutsSubContextMenu();
			this.addChild(this.manageSubPopup);
			
		}

		public void openMenuAt(GuiButton parentBtn) {
			this.content.clear();

			String identifier = Minecraft.getMinecraft().currentScreen.getClass().getName();
			if (Minecraft.getMinecraft().currentScreen instanceof CustomGuiBase) {
				identifier = ((CustomGuiBase) Minecraft.getMinecraft().currentScreen).getIdentifier();
			}
			if (this.isUniversal) {
				identifier = "%fancymenu:universal_layout%";
			}
			
			List<PropertiesSet> enabled = MenuCustomizationProperties.getPropertiesWithIdentifier(identifier);
			if (!this.isUniversal) {
				List<PropertiesSet> sets = new ArrayList<>();
				for (PropertiesSet s : enabled) {
					List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
					if (!metas.isEmpty()) {
						PropertiesSection meta = metas.get(0);
						String id = meta.getEntryValue("identifier");
						if (!id.equals("%fancymenu:universal_layout%")) {
							sets.add(s);
						}
					}
				}
				enabled = sets;
			}
			if (!enabled.isEmpty()) {
				for (PropertiesSet s : enabled) {
					List<PropertiesSection> secs = s.getPropertiesOfType("customization-meta");
					if (secs.isEmpty()) {
						secs = s.getPropertiesOfType("type-meta");
					}
					if (!secs.isEmpty()) {
						String name = "<missing name>";
						PropertiesSection meta = secs.get(0);
						File f = new File(meta.getEntryValue("path"));
						if (f.isFile()) {
							name = Files.getNameWithoutExtension(f.getName());
							
							int totalactions = s.getProperties().size() - 1;
							CustomizationButton layoutEntryBtn = new CustomizationButton(0, 0, 0, 0, "§a" + name, (press) -> {
								this.manageSubPopup.setParentButton((AdvancedButton) press);
								this.manageSubPopup.openMenuAt(0, press.y, f, false);
							});
							layoutEntryBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.layout.btndesc", Locals.localize("helper.buttons.customization.managelayouts.enabled"), "" + totalactions), "%n%"));
							this.addContent(layoutEntryBtn);
						}
					}
				}
			}
			
			List<PropertiesSet> disabled = MenuCustomizationProperties.getDisabledPropertiesWithIdentifier(identifier);
			if (!this.isUniversal) {
				List<PropertiesSet> sets = new ArrayList<>();
				for (PropertiesSet s : disabled) {
					List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
					if (!metas.isEmpty()) {
						PropertiesSection meta = metas.get(0);
						String id = meta.getEntryValue("identifier");
						if (!id.equals("%fancymenu:universal_layout%")) {
							sets.add(s);
						}
					}
				}
				disabled = sets;
			}
			if (!disabled.isEmpty()) {
				for (PropertiesSet s : disabled) {
					List<PropertiesSection> secs = s.getPropertiesOfType("customization-meta");
					if (secs.isEmpty()) {
						secs = s.getPropertiesOfType("type-meta");
					}
					if (!secs.isEmpty()) {
						String name = "<missing name>";
						PropertiesSection meta = secs.get(0);
						File f = new File(meta.getEntryValue("path"));
						if (f.isFile()) {
							name = Files.getNameWithoutExtension(f.getName());
							
							int totalactions = s.getProperties().size() - 1;
							CustomizationButton layoutEntryBtn = new CustomizationButton(0, 0, 0, 0, "§c" + name, (press) -> {
								this.manageSubPopup.setParentButton((AdvancedButton) press);
								this.manageSubPopup.openMenuAt(0, press.y, f, true);
							});
							layoutEntryBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.layout.btndesc", Locals.localize("helper.buttons.customization.managelayouts.disabled"), "" + totalactions), "%n%"));
							this.addContent(layoutEntryBtn);
						}
					}
				}
			}
			
			if (enabled.isEmpty() && disabled.isEmpty()) {
				CustomizationButton emptyBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), (press) -> {});
				this.addContent(emptyBtn);
			}

			this.openMenuAt(parentBtn.x - this.getWidth() - 2, parentBtn.y);
		}
		
		@Override
		public void render(int mouseX, int mouseY) {
			super.render(mouseX, mouseY);
			
			if (this.manageSubPopup != null) {
				this.manageSubPopup.render(mouseX, mouseY);
				if (!this.isOpen()) {
					this.manageSubPopup.closeMenu();
				}
			}
		}
		
		@Override
		public void closeMenu() {
			if (!this.manageSubPopup.isHovered()) {
				super.closeMenu();
			}
		}
		
		@Override
		public boolean isHovered() {
			if (this.manageSubPopup.isOpen() && this.manageSubPopup.isHovered()) {
				return true;
			} else {
				return super.isHovered();
			}
		}
		
	}

	private static class ManageLayoutsSubContextMenu extends FMContextMenu {

		public void openMenuAt(int x, int y, File layout, boolean disabled) {
			
			this.content.clear();
			
			String toggleLabel = Locals.localize("helper.buttons.customization.managelayouts.disable");
			if (disabled) {
				toggleLabel = Locals.localize("helper.buttons.customization.managelayouts.enable");
			}
			CustomizationButton toggleLayoutBtn = new CustomizationButton(0, 0, 0, 0, toggleLabel, (press) -> {
				if (disabled) {
					String name = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationPath().getPath(), Files.getNameWithoutExtension(layout.getName()), "txt");
					FileUtils.copyFile(layout, new File(FancyMenu.getCustomizationPath().getPath() + "/" + name));
					layout.delete();
				} else {
					String disPath = FancyMenu.getCustomizationPath().getPath() + "/.disabled";
					String name = FileUtils.generateAvailableFilename(disPath, Files.getNameWithoutExtension(layout.getName()), "txt");
					FileUtils.copyFile(layout, new File(disPath + "/" + name));
					layout.delete();
				}
				CustomizationHelper.reloadSystemAndMenu();
			});
			this.addContent(toggleLayoutBtn);

			CustomizationButton editLayoutBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.manage.edit"), (press) -> {
				GuiScreen s = Minecraft.getMinecraft().currentScreen;
				if ((this.parent != null) && (this.parent instanceof ManageLayoutsContextMenu)) {
					if (((ManageLayoutsContextMenu)this.parent).isUniversal) {
						s = new CustomGuiBase("", "%fancymenu:universal_layout%", true, Minecraft.getMinecraft().currentScreen, null);
					}
				}
				CustomizationHelper.editLayout(s, layout);
			});
			editLayoutBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.manage.edit.desc"), "%n%"));
			this.addContent(editLayoutBtn);
			
			CustomizationButton openInTextEditorBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.customization.managelayouts.openintexteditor"), (press) -> {
				CustomizationHelper.openFile(layout);
			});
			openInTextEditorBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.openintexteditor.desc"), "%n%"));
			this.addContent(openInTextEditorBtn);
			
			CustomizationButton deleteLayoutBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.customization.managelayouts.delete"), (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						if (layout.exists()) {
							layout.delete();
							CustomizationHelper.reloadSystemAndMenu();
						}
					}
				}, Locals.localize("helper.buttons.customization.managelayouts.delete.msg"), "", "", "", ""));
				CustomizationHelper.reloadSystemAndMenu();
			});
			this.addContent(deleteLayoutBtn);
			
			this.openMenuAt(x, y);
			
		}
	}
	
	private static class OverrideMenuContextMenu extends FMContextMenu {
		
		@Override
		public void openMenuAt(int x, int y) {
			
			this.content.clear();

			List<String> l = CustomGuiLoader.getCustomGuis();

			if (!l.isEmpty()) {

				this.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.pickbyname"), true, (press) -> {
					PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.pickbyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								onOverrideWithCustomGui(Minecraft.getMinecraft().currentScreen, call);
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));
				
				this.addSeparator();
				
				for (String s : l) {
					String label = s;
					if (Minecraft.getMinecraft().fontRenderer.getStringWidth(label) > 80) {
						label = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(label, 75) + "..";
					}

					this.addContent(new CustomizationButton(0, 0, 0, 0, label, true, (press) -> {
						onOverrideWithCustomGui(Minecraft.getMinecraft().currentScreen, s);
					}));

				}

			} else {
				this.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), true, (press) -> {}));
			}
			
			super.openMenuAt(x, y);
			
		}
		
	}

	private static void onOverrideWithCustomGui(GuiScreen current, String customGuiIdentifier) {
		if ((customGuiIdentifier != null) && CustomGuiLoader.guiExists(customGuiIdentifier)) {
			PropertiesSection meta = new PropertiesSection("customization-meta");
			meta.addEntry("identifier", current.getClass().getName());

			PropertiesSection or = new PropertiesSection("customization");
			or.addEntry("action", "overridemenu");
			or.addEntry("identifier", customGuiIdentifier);

			PropertiesSet props = new PropertiesSet("menu");
			props.addProperties(meta);
			props.addProperties(or);

			String screenname = current.getClass().getName();
			if (screenname.contains(".")) {
				screenname = new StringBuilder(new StringBuilder(screenname).reverse().toString().split("[.]", 2)[0]).reverse().toString();
			}
			String filename = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationPath().getPath(), "overridemenu_" + screenname, "txt");

			String finalpath = FancyMenu.getCustomizationPath().getPath() + "/" + filename;
			PropertiesSerializer.writeProperties(props, finalpath);

			CustomizationHelper.reloadSystemAndMenu();
		}
	}

}
