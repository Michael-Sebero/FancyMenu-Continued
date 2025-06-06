//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.*;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.component.Component;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.widget.AdvancedTextField;
import de.keksuccino.fancymenu.mixin.client.IMixinGuiTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TextEditorLine extends AdvancedTextField {

    private static final Logger LOGGER = LogManager.getLogger();

    public TextEditorScreen parent;
    protected String lastTickValue = "";
    public boolean isInMouseHighlightingMode = false;
    protected final FontRenderer font2;
    protected final boolean handleSelf2;
    public int textWidth = 0;
    public int lineIndex = 0;
    protected int currentHighlightPosXStart = 0;
    protected int currentHighlightPosXEnd = 0;
    protected int currentCharacterRenderIndex = 0;

    protected static boolean leftRightArrowWasDown = false;

    public TextEditorLine(FontRenderer font, int x, int y, int width, int height, boolean handleSelf,  CharacterFilter characterFilter, TextEditorScreen parent) {
        super(font, x, y, width, height, handleSelf, characterFilter);
        this.parent = parent;
        this.font2 = font;
        this.handleSelf2 = handleSelf;
        this.setBordered(false);
    }

    @Override
    public void render(int mouseX, int mouseY, float partial) {

        //Only render line if inside the editor area (for performance reasons)
        if (this.isInEditorArea()) {
            super.render(mouseX, mouseY, partial);
        }

        this.lastTickValue = this.getValue();

    }

    protected ITextComponent getFormattedText(String text) {
        List<ITextComponent> chars = new ArrayList<>();
        for (char c : text.toCharArray()) {
            TextStyle style = TextStyle.EMPTY;
            for (TextEditorFormattingRule r : this.parent.formattingRules) {
                TextStyle rs = r.getStyle(c, this.currentCharacterRenderIndex, this.getCursorPosition(), this, this.parent.currentRenderCharacterIndexTotal, this.parent);
                if ((rs != null) && (rs != TextStyle.EMPTY)) {
                    style = rs.applyTo(style);
                }
            }
            chars.add(Component.literal(String.valueOf(c)).setStyle(style));
            this.currentCharacterRenderIndex++;
            this.parent.currentRenderCharacterIndexTotal++;
        }
        ITextComponent comp = Component.literal("");
        for (ITextComponent c : chars) {
            comp.appendSibling(c);
        }
        return comp;
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float partial) {

        this.currentCharacterRenderIndex = 0;

        this.setTextColor(this.parent.textColor.getRGB());
        this.setTextColorUneditable(this.parent.textColor.getRGB());

        if (this.isVisible()) {

            if (this.isFocused()) {
                //Render focused background
                fill(0, this.getY(), this.parent.width, this.getY() + this.height, this.parent.focusedLineColor.getRGB());
            }

            int textColorInt = this.isEditable() ? this.getAsAccessor().getTextColorFancyMenu() : this.getAsAccessor().getTextColorUneditableFancyMenu();
            int cursorPos = this.getCursorPosition() - this.getAsAccessor().getDisplayPosFancyMenu();
            int highlightPos = this.getAsAccessor().getHighlightPosFancyMenu() - this.getAsAccessor().getDisplayPosFancyMenu();
//                String text = this.font2.plainSubstrByWidth(this.getValue().substring(this.getAsAccessor().getDisplayPosFancyMenu()), this.getInnerWidth());
            String text = this.getValue();
            boolean isCursorNotAtStartOrEnd = cursorPos >= 0 && cursorPos <= text.length();
            boolean renderCursor = this.isFocused() && this.getAsAccessor().getFrameFancyMenu() / 6 % 2 == 0 && isCursorNotAtStartOrEnd;
            int textX = this.getAsAccessor().getBorderedFancyMenu() ? this.getX() + 4 : this.getX() + 1;
            int textY = this.getAsAccessor().getBorderedFancyMenu() ? this.getY() + (this.height - 8) / 2 : (this.getY() + Math.max(0, (this.getHeight() / 2)) - (this.font2.FONT_HEIGHT / 2));
            int textXRender = textX;
            if (highlightPos > text.length()) {
                highlightPos = text.length();
            }

            if (!text.isEmpty()) {
                String textBeforeCursor = isCursorNotAtStartOrEnd ? text.substring(0, cursorPos) : text;
                //Render text before cursor
                textXRender = AbstractGui.drawFormattedString(this.font2, this.getFormattedText(textBeforeCursor), textX, textY, textColorInt);
            }

            boolean isCursorAtEndOfLine = this.getCursorPosition() < this.getValue().length() || this.getValue().length() >= this.getAsAccessor().getMaxLengthFancyMenu();
            int cursorPosRender = textXRender;
            if (!isCursorNotAtStartOrEnd) {
                cursorPosRender = cursorPos > 0 ? textX + this.width : textX;
            } else if (isCursorAtEndOfLine) {
                cursorPosRender = textXRender - 1;
                --textXRender;
            }

            if (!text.isEmpty() && isCursorNotAtStartOrEnd && cursorPos < text.length()) {
                //Render text after cursor
//                this.font2.draw(matrix, this.getAsAccessor().getFormatterFancyMenu().apply(text.substring(cursorPos), this.getCursorPosition()), (float)textXRender, (float)textY, textColorInt);
                AbstractGui.drawFormattedString(this.font2, this.getFormattedText(text.substring(cursorPos)), textXRender, textY, textColorInt);
            }

            if (renderCursor) {
                if (isCursorAtEndOfLine) {
                    fill(cursorPosRender, textY - 1, cursorPosRender + 1, textY + 1 + 9, -3092272);
                } else {
                    AbstractGui.drawFormattedString(this.font2, Component.literal("_"), cursorPosRender, textY, textColorInt);
                }
            }

            if (highlightPos != cursorPos) {
                this.currentHighlightPosXStart = cursorPosRender;
                this.currentHighlightPosXEnd = textX + this.font2.getStringWidth(text.substring(0, highlightPos)) - 1;
                this.getAsAccessor().invokeRenderHighlightFancyMenu(this.currentHighlightPosXStart, textY - 1, this.currentHighlightPosXEnd, textY + 1 + 9);
            } else {
                this.currentHighlightPosXStart = 0;
                this.currentHighlightPosXEnd = 0;
            }

        }

    }

    public boolean isInEditorArea() {
        return ((this.getY() + this.getHeight() >= this.parent.getEditorAreaY()) && (this.getY() <= this.parent.getEditorAreaY() + this.parent.getEditorAreaHeight()));
    }

    public boolean isHighlightedHovered() {
        if (this.isInEditorArea() && (this.currentHighlightPosXStart != this.currentHighlightPosXEnd) && this.isHovered()) {
            int mouseX = MouseInput.getMouseX();
            return ((mouseX >= Math.min(this.currentHighlightPosXStart, this.currentHighlightPosXEnd)) && (mouseX <= Math.max(this.currentHighlightPosXStart, this.currentHighlightPosXEnd)));
        }
        return false;
    }

    public int getActualHeight() {
        int h = this.height;
        if (this.getAsAccessor().getBorderedFancyMenu()) {
            h += 2;
        }
        return h;
    }

    public IMixinGuiTextField getAsAccessor() {
        return (IMixinGuiTextField) this;
    }

    @Override
    public void setCursorPosition(int pos) {

        this.textWidth = this.font2.getStringWidth(this.getValue());

        super.setCursorPosition(pos);

        //Caching the last cursor position set by the user, to set it to the new line when changing the line
        if ((pos != this.parent.lastCursorPosSetByUser) && this.isFocused()) {
            this.parent.lastCursorPosSetByUser = this.getCursorPosition();
        }

        this.parent.correctXScroll(this);

    }

    @Override
    public void tick() {

        if (!MouseInput.isLeftMouseDown() && this.isInMouseHighlightingMode) {
            this.isInMouseHighlightingMode = false;
        }

        super.tick();

        leftRightArrowWasDown = false;

    }

    @Override
    public boolean keyPressed(int keycode, int i1, int i2) {
        //Handled by the editor
        if (Screen.isCopy(keycode) || Screen.isPaste(keycode) || Screen.isSelectAll(keycode) || Screen.isCut(keycode)) {
            return false;
        }
        //Text deletion is handled by the editor
        if (keycode == InputConstants.BACKSPACE) {
            return false;
        }
        //Don't move cursor when in mouse-highlighting mode
        if (((keycode == InputConstants.ARROW_LEFT) || (keycode == InputConstants.ARROW_RIGHT)) && this.parent.isInMouseHighlightingMode()) {
            return false;
        }
        //Jump to line above when pressing ARROW LEFT while at start of line
        if (keycode == InputConstants.ARROW_LEFT) {
            if (!leftRightArrowWasDown) {
                if (this.parent.isLineFocused() && (this.parent.getFocusedLine() == this) && (this.getCursorPosition() <= 0) && (this.parent.getLineIndex(this) > 0)) {
                    leftRightArrowWasDown = true;
                    this.parent.goUpLine();
                    this.parent.getFocusedLine().moveCursorTo(this.parent.getFocusedLine().getValue().length());
                    this.parent.correctYScroll(0);
                    return true;
                }
            } else {
                return true;
            }
        }
        //Jump to line below when pressing ARROW RIGHT while at end of line
        if (keycode == InputConstants.ARROW_RIGHT) {
            if (!leftRightArrowWasDown) {
                if (this.parent.isLineFocused() && (this.parent.getFocusedLine() == this) && (this.getCursorPosition() >= this.getValue().length()) && (this.parent.getLineIndex(this) < this.parent.getLineCount() - 1)) {
                    leftRightArrowWasDown = true;
                    this.parent.goDownLine(false);
                    this.parent.getFocusedLine().moveCursorTo(0);
                    this.parent.correctYScroll(0);
                    return true;
                }
            } else {
                return true;
            }
        }
        return super.keyPressed(keycode, i1, i2);
    }

    @Override
    public void deleteChars(int i) {
        //If the user presses BACKSPACE and the cursor pos is at 0, it will jump one line up, adds
        //the text behind the cursor at the end of the new line and deletes the old line
        if (!this.parent.justSwitchedLineByWordDeletion) {
            if ((this.getCursorPosition() == 0) && (this.parent.getFocusedLineIndex() > 0)) {
                int lastLineIndex = this.parent.getFocusedLineIndex();
                this.parent.justSwitchedLineByWordDeletion = true;
                this.parent.goUpLine();
                this.parent.getFocusedLine().moveCursorToEnd();
                this.parent.getFocusedLine().insertText(this.getValue());
                this.parent.getFocusedLine().setCursorPosition(this.parent.getFocusedLine().getCursorPosition()-this.getValue().length());
                this.parent.getFocusedLine().setHighlightPos(this.parent.getFocusedLine().getCursorPosition());
                if (lastLineIndex > 0) {
                    this.parent.removeLineAtIndex(this.parent.getFocusedLineIndex()+1);
                    this.parent.correctYScroll(-1);
                }
            } else {
                super.deleteChars(i);
            }
        }
        this.textWidth = this.font2.getStringWidth(this.getValue());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

        if (!this.parent.isMouseInsideEditorArea() || this.parent.rightClickContextMenu.isOpen()) {
            return false;
        }

        if ((mouseButton == 0) && this.isHovered() && !this.isInMouseHighlightingMode && this.isVisible()) {
            if (!this.parent.isAtLeastOneLineInHighlightMode()) {
                this.parent.startHighlightLine = this;
            }
            this.isInMouseHighlightingMode = true;
            this.parent.setFocusedLine(Math.max(0, this.parent.getLineIndex(this)));
            super.mouseClicked(mouseX, mouseY, mouseButton);
            this.shiftPressed = false;
            this.setHighlightPos(this.getCursorPosition());
        } else if ((mouseButton == 0) && !this.isHovered()) {
            //Clear highlighting when left-clicked in another line, etc.
            this.setHighlightPos(this.getCursorPosition());
        }

        if (!this.isInMouseHighlightingMode && (mouseButton == 0)) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return true;

    }

    @Override
    public void setValue(String p_94145_) {
        super.setValue(p_94145_);
        this.textWidth = this.font2.getStringWidth(this.getValue());
    }

    @Override
    public void insertText(String text) {
        super.insertText(text);
        this.textWidth = this.font2.getStringWidth(this.getValue());
    }

    @Override
    public void setMaxLength(int p_94200_) {
        super.setMaxLength(p_94200_);
        this.textWidth = this.font2.getStringWidth(this.getValue());
    }

}
