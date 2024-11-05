package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.fancy.item.items.IActionExecutorItem;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ButtonCustomizationItem extends CustomizationItemBase implements IActionExecutorItem {

	public AdvancedButton button;
	private String hoverLabel;
	private String hoverSound;
	private boolean hover = false;
	private boolean onlyMultiplayer = false;
	private boolean onlySingleplayer = false;
	private boolean onlyOutgame = false;

	public String hoverLabelRaw;
	public String labelRaw;
	public String tooltip;
	public List<ButtonScriptEngine.ActionContainer> actions = new ArrayList<>();

	public ButtonCustomizationItem(PropertiesSection item) {
		super(item);

		if ((this.action != null) && this.action.equalsIgnoreCase("addbutton")) {
			this.labelRaw = item.getEntryValue("label");
			if (this.labelRaw == null) {
				this.labelRaw = "";
			}

			String buttonaction = item.getEntryValue("buttonaction");
			String actionvalue = item.getEntryValue("value");

			if (actionvalue == null) {
				actionvalue = "";
			}

			this.hoverSound = item.getEntryValue("hoversound");
			if (this.hoverSound != null) {
				this.hoverSound = this.hoverSound.replace("\\", "/");
				File f = new File(MenuCustomization.getAbsoluteGameDirectoryPath(this.hoverSound));
				if (f.exists() && f.isFile() && (f.getName().endsWith(".flac") || f.getName().endsWith(".mp3") || f.getName().endsWith(".wav") || f.getName().endsWith(".opus"))) {
					MenuCustomization.registerSound(this.hoverSound, this.hoverSound);
				} else {
					this.hoverSound = null;
				}
			}

			this.hoverLabelRaw = item.getEntryValue("hoverlabel");

			String onlyX = item.getEntryValue("onlydisplayin");
			if (onlyX != null) {
				if (onlyX.equalsIgnoreCase("outgame")) {
					this.onlyOutgame = true;
				}
				if (onlyX.equalsIgnoreCase("multiplayer")) {
					this.onlyMultiplayer = true;
				}
				if (onlyX.equalsIgnoreCase("singleplayer")) {
					this.onlySingleplayer = true;
				}
			}

			if (buttonaction != null) {
				if (buttonaction.contains("%btnaction_splitter_fm%")) {
					for (String s : StringUtils.splitLines(buttonaction, "%btnaction_splitter_fm%")) {
						if (s.length() > 0) {
							String action = s;
							String value = null;
							if (s.contains(";")) {
								action = s.split(";", 2)[0];
								value = s.split(";", 2)[1];
							}
							this.actions.add(new ButtonScriptEngine.ActionContainer(action, value));
						}
					}
				} else {
					this.actions.add(new ButtonScriptEngine.ActionContainer(buttonaction, actionvalue));
				}
			}

			this.button = new AdvancedButton(0, 0, this.getWidth(), this.getHeight(), "", true, (press) -> {
				for (ButtonScriptEngine.ActionContainer c : this.actions) {
					c.execute();
				}
			});

			String click = item.getEntryValue("clicksound");
			if (click != null) {
				click.replace("\\", "/");
				File f = new File(click);
				if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getMinecraft().mcDataDir.getAbsolutePath().replace("\\", "/"))) {
					f = new File(Minecraft.getMinecraft().mcDataDir.getAbsolutePath().replace("\\", "/") + "/" + click);
				}
				if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".flac")) {
					SoundHandler.registerSound(f.getPath(), f.getPath());
					this.button.setClickSound(f.getPath());
				}
			}

			this.tooltip = item.getEntryValue("description");
			if (this.tooltip != null) {
				this.button.setDescription(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(this.tooltip), "%n%"));
			}

			String backNormal = fixBackslashPath(item.getEntryValue("backgroundnormal"));
			String backHover = fixBackslashPath(item.getEntryValue("backgroundhovered"));
			String loopBackAnimations = item.getEntryValue("loopbackgroundanimations");
			String restartBackAnimationsOnHover = item.getEntryValue("restartbackgroundanimations");
			String backAnimationNormal = item.getEntryValue("backgroundanimationnormal");
			String backAnimationHover = item.getEntryValue("backgroundanimationhovered");

			if (this.button != null) {
				if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
					this.button.loopBackgroundAnimations = false;
				}
				if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
					this.button.restartBackgroundAnimationsOnHover = false;
				}
				if (backNormal != null) {
					File f = new File(backNormal.replace("\\", "/"));
					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getMinecraft().mcDataDir.getAbsolutePath().replace("\\", "/"))) {
						f = new File(Minecraft.getMinecraft().mcDataDir.getAbsolutePath().replace("\\", "/") + "/" + backNormal);
					}
					if (f.isFile()) {
						if (f.getPath().toLowerCase().endsWith(".gif")) {
							this.button.setBackgroundNormal(TextureHandler.getGifResource(f.getPath()));
						} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
							ExternalTextureResourceLocation back = TextureHandler.getResource(f.getPath());
							if (back != null) {
								if (!back.isReady()) {
									back.loadTexture();
								}
								this.button.setBackgroundNormal(back.getResourceLocation());
							}
						}
					}
				} else if (backAnimationNormal != null) {
					if (AnimationHandler.animationExists(backAnimationNormal)) {
						this.button.setBackgroundNormal(AnimationHandler.getAnimation(backAnimationNormal));
					}
				}
				if (backHover != null) {
					File f = new File(backHover.replace("\\", "/"));
					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getMinecraft().mcDataDir.getAbsolutePath().replace("\\", "/"))) {
						f = new File(Minecraft.getMinecraft().mcDataDir.getAbsolutePath().replace("\\", "/") + "/" + backHover);
					}
					if (f.isFile()) {
						if (f.getPath().toLowerCase().endsWith(".gif")) {
							this.button.setBackgroundHover(TextureHandler.getGifResource(f.getPath()));
						} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
							ExternalTextureResourceLocation back = TextureHandler.getResource(f.getPath());
							if (back != null) {
								if (!back.isReady()) {
									back.loadTexture();
								}
								this.button.setBackgroundHover(back.getResourceLocation());
							}
						}
					}
				} else if (backAnimationHover != null) {
					if (AnimationHandler.animationExists(backAnimationHover)) {
						this.button.setBackgroundHover(AnimationHandler.getAnimation(backAnimationHover));
					}
				}
			}

			this.updateValues();

		}
	}

	public void render(GuiScreen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		this.updateValues();

		if (this.onlyOutgame && (Minecraft.getMinecraft().world != null)) {
			return;
		}

		if (this.onlyMultiplayer && ((Minecraft.getMinecraft().world == null) || Minecraft.getMinecraft().isIntegratedServerRunning())) {
			return;
		}

		if (this.onlySingleplayer && ((Minecraft.getMinecraft().world == null) || !Minecraft.getMinecraft().isIntegratedServerRunning())) {
			return;
		}

		this.button.alpha = this.opacity;

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);

		this.button.x = x;
		this.button.y = y;

		if (this.button.isMouseOver() && this.button.enabled) {
			if (this.hoverLabel != null) {
				this.button.displayString = this.hoverLabel;
			} else {
				this.button.displayString = this.value;
			}
			if ((this.hoverSound != null) && !this.hover) {
				this.hover = true;
				SoundHandler.resetSound(this.hoverSound);
				SoundHandler.playSound(this.hoverSound);
			}
		} else {
			this.button.displayString = this.value;
			this.hover = false;
		}

		this.button.drawButton(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
	}

	protected void updateValues() {

		if (this.tooltip != null) {
			this.button.setDescription(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(this.tooltip), "%n%"));
		}
		if (this.labelRaw != null) {
			if (!isEditorActive()) {
				this.value = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(this.labelRaw);
			} else {
				this.value = StringUtils.convertFormatCodes(this.labelRaw, "&", "§");
			}
		}
		if (this.hoverLabelRaw != null) {
			if (!isEditorActive()) {
				this.hoverLabel = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(this.hoverLabelRaw);
			} else {
				this.hoverLabel = StringUtils.convertFormatCodes(this.hoverLabelRaw, "&", "§");
			}
		}

	}

	@Override
	public boolean shouldRender() {
		if (this.button == null) {
			return false;
		}
		return super.shouldRender();
	}

	public AdvancedButton getButton() {
		return this.button;
	}

	public Long getId() {
		int ori = 0;
		if (this.orientation.equalsIgnoreCase("original")) {
			ori = 1;
		} else if (this.orientation.equalsIgnoreCase("top-left")) {
			ori = 2;
		} else if (this.orientation.equalsIgnoreCase("mid-left")) {
			ori = 3;
		} else if (this.orientation.equalsIgnoreCase("bottom-left")) {
			ori = 4;
		} else if (this.orientation.equalsIgnoreCase("top-centered")) {
			ori = 5;
		} else if (this.orientation.equalsIgnoreCase("mid-centered")) {
			ori = 6;
		} else if (this.orientation.equalsIgnoreCase("bottom-centered")) {
			ori = 7;
		} else if (this.orientation.equalsIgnoreCase("top-right")) {
			ori = 8;
		} else if (this.orientation.equalsIgnoreCase("mid-right")) {
			ori = 9;
		} else if (this.orientation.equalsIgnoreCase("bottom-right")) {
			ori = 10;
		}

		String idRaw = "00" + ori + "" + Math.abs(this.posX) + "" + Math.abs(this.posY) + "" + Math.abs(this.getWidth());
		long id = 0;
		if (MathUtils.isLong(idRaw)) {
			id = Long.parseLong(idRaw);
		}

		return id;
	}

	@Override
	public List<ButtonScriptEngine.ActionContainer> getActionList() {
		return this.actions;
	}

}
