package com.evandev.reliable_advancements.gui.widgets;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class JsonEditorWidget extends AbstractWidget {
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 6;
    private static final int SCROLLBAR_WIDTH = 6;

    private static final int COLOR_KEY = 0xFF79B8FF;
    private static final int COLOR_STRING = 0xFF85E89D;
    private static final int COLOR_NUMBER = 0xFFFFAB70;
    private static final int COLOR_BOOLEAN = 0xFFF97583;
    private static final int COLOR_PUNCTUATION = 0xFFD4D4D4;
    private static final int COLOR_DEFAULT = 0xFFECEEF4;
    private static final int COLOR_ERROR = 0xFFFF6B6B;

    private final Font font;
    private final List<String> lines = new ArrayList<>();
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();

    private Consumer<String> responder;
    private String syntaxError = null;
    private long focusedTime = Util.getMillis();

    private int cursorLine = 0;
    private int cursorCol = 0;
    private int selectAnchorLine = 0;
    private int selectAnchorCol = 0;

    private double scrollY = 0;
    private boolean isDraggingScrollbar = false;
    private double dragStartY = 0;
    private double dragStartScroll = 0;

    public JsonEditorWidget(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
        this.lines.add("{}");
        validateSyntax();
    }

    public void setResponder(Consumer<String> responder) {
        this.responder = responder;
    }

    public String getValue() {
        return String.join("\n", lines);
    }

    public void setValue(String text) {
        if (text == null) text = "{}";
        String current = getValue();
        if (text.equals(current)) return;

        pushUndoState();
        setRawText(text);
        cursorLine = Math.min(cursorLine, lines.size() - 1);
        cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
        clearSelection();
        validateSyntax();
        if (responder != null) responder.accept(getValue());
    }

    private void setRawText(String text) {
        lines.clear();
        String[] split = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        lines.addAll(Arrays.asList(split));
        if (lines.isEmpty()) lines.add("");
    }

    public @Nullable String getSyntaxError() {
        return syntaxError;
    }

    private void validateSyntax() {
        String text = getValue().trim();
        if (text.isEmpty()) {
            syntaxError = null;
            return;
        }
        try {
            JsonParser.parseString(text);
            syntaxError = null;
        } catch (JsonSyntaxException e) {
            syntaxError = e.getMessage();
        } catch (Exception e) {
            syntaxError = "Invalid JSON syntax";
        }
    }

    private void pushUndoState() {
        undoStack.push(getValue());
        if (undoStack.size() > 50) undoStack.removeFirst();
        redoStack.clear();
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(getValue());
            setRawText(undoStack.pop());
            cursorLine = Math.min(cursorLine, lines.size() - 1);
            cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
            clearSelection();
            validateSyntax();
            if (responder != null) responder.accept(getValue());
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(getValue());
            setRawText(redoStack.pop());
            cursorLine = Math.min(cursorLine, lines.size() - 1);
            cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
            clearSelection();
            validateSyntax();
            if (responder != null) responder.accept(getValue());
        }
    }

    private int getGutterWidth() {
        int digits = Math.max(2, String.valueOf(lines.size()).length());
        return digits * 6 + 12;
    }

    private int getContentHeight() {
        return lines.size() * LINE_HEIGHT + PADDING_Y * 2;
    }

    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - (this.height - PADDING_Y * 2));
    }

    private void clampScroll() {
        scrollY = Mth.clamp(scrollY, 0, getMaxScroll());
    }

    private void scrollToCursor() {
        int cursorPixelY = cursorLine * LINE_HEIGHT;
        int viewHeight = this.height - PADDING_Y * 2;
        if (cursorPixelY < scrollY) {
            scrollY = cursorPixelY;
        } else if (cursorPixelY + LINE_HEIGHT > scrollY + viewHeight) {
            scrollY = cursorPixelY + LINE_HEIGHT - viewHeight;
        }
        clampScroll();
    }

    private void clearSelection() {
        selectAnchorLine = cursorLine;
        selectAnchorCol = cursorCol;
    }

    private boolean hasSelection() {
        return cursorLine != selectAnchorLine || cursorCol != selectAnchorCol;
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int startL, startC, endL, endC;
        if (cursorLine < selectAnchorLine || (cursorLine == selectAnchorLine && cursorCol < selectAnchorCol)) {
            startL = cursorLine;
            startC = cursorCol;
            endL = selectAnchorLine;
            endC = selectAnchorCol;
        } else {
            startL = selectAnchorLine;
            startC = selectAnchorCol;
            endL = cursorLine;
            endC = cursorCol;
        }

        String firstLine = lines.get(startL);
        String lastLine = lines.get(endL);
        String merged = firstLine.substring(0, startC) + lastLine.substring(endC);

        lines.subList(startL, endL + 1).clear();
        lines.add(startL, merged);

        cursorLine = startL;
        cursorCol = startC;
        clearSelection();
    }

    private void insertText(String str) {
        pushUndoState();
        deleteSelection();
        String currentLine = lines.get(cursorLine);
        String[] inserts = str.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);

        if (inserts.length == 1) {
            String updated = currentLine.substring(0, cursorCol) + inserts[0] + currentLine.substring(cursorCol);
            lines.set(cursorLine, updated);
            cursorCol += inserts[0].length();
        } else {
            String prefix = currentLine.substring(0, cursorCol);
            String suffix = currentLine.substring(cursorCol);

            lines.set(cursorLine, prefix + inserts[0]);
            for (int i = 1; i < inserts.length - 1; i++) {
                lines.add(cursorLine + i, inserts[i]);
            }
            lines.add(cursorLine + inserts.length - 1, inserts[inserts.length - 1] + suffix);

            cursorLine += inserts.length - 1;
            cursorCol = inserts[inserts.length - 1].length();
        }

        clearSelection();
        validateSyntax();
        scrollToCursor();
        if (responder != null) responder.accept(getValue());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isFocused() && this.visible && this.active && this.isMouseOver(mouseX, mouseY)) {
            this.scrollY -= scrollY * LINE_HEIGHT * 2.5;
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (!this.visible || !this.active) return false;
        boolean inside = mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height;
        if (!inside) {
            setFocused(false);
            return false;
        }

        setFocused(true);

        int scrollbarX = getX() + width - SCROLLBAR_WIDTH - 2;
        if (getMaxScroll() > 0 && mouseX >= scrollbarX && mouseX <= getX() + width) {
            isDraggingScrollbar = true;
            dragStartY = mouseY;
            dragStartScroll = scrollY;
            return true;
        }

        if (button == 0) {
            int gutterW = getGutterWidth();
            double textX = mouseX - getX() - gutterW - PADDING_X;
            double textY = mouseY - getY() - PADDING_Y + scrollY;

            cursorLine = Mth.clamp((int) (textY / LINE_HEIGHT), 0, lines.size() - 1);
            String lineStr = lines.get(cursorLine);
            cursorCol = getCharOffsetAtPixel(lineStr, (int) textX);

            if (!EnhancedAdvancementsScreen.hasShiftDown()) {
                clearSelection();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (isDraggingScrollbar && button == 0 && getMaxScroll() > 0) {
            int trackH = this.height - PADDING_Y * 2;
            int thumbH = Math.max(16, trackH * trackH / Math.max(1, getContentHeight()));
            int availableTrack = trackH - thumbH;
            if (availableTrack > 0) {
                double delta = mouseY - dragStartY;
                scrollY = dragStartScroll + delta * (getMaxScroll() / (double) availableTrack);
                clampScroll();
            }
            return true;
        }

        if (button == 0 && isFocused()) {
            int gutterW = getGutterWidth();
            double textX = mouseX - getX() - gutterW - PADDING_X;
            double textY = mouseY - getY() - PADDING_Y + scrollY;

            cursorLine = Mth.clamp((int) (textY / LINE_HEIGHT), 0, lines.size() - 1);
            String lineStr = lines.get(cursorLine);
            cursorCol = getCharOffsetAtPixel(lineStr, (int) textX);
            scrollToCursor();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(event);
    }

    private int getCharOffsetAtPixel(String line, int pixelX) {
        if (pixelX <= 0) return 0;
        int accumulated = 0;
        for (int i = 0; i < line.length(); i++) {
            int charW = font.width(String.valueOf(line.charAt(i)));
            if (accumulated + charW / 2 >= pixelX) {
                return i;
            }
            accumulated += charW;
        }
        return line.length();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (!isFocused()) return false;

        boolean shift = event.hasShiftDown();
        boolean ctrl = event.hasControlDown();

        if (ctrl && keyCode == 65) {
            selectAnchorLine = 0;
            selectAnchorCol = 0;
            cursorLine = lines.size() - 1;
            cursorCol = lines.get(cursorLine).length();
            return true;
        } else if (ctrl && keyCode == 67) {
            if (hasSelection()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(getSelectedText());
            }
            return true;
        } else if (ctrl && keyCode == 88) {
            if (hasSelection()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(getSelectedText());
                deleteSelection();
                validateSyntax();
                scrollToCursor();
                if (responder != null) responder.accept(getValue());
            }
            return true;
        } else if (ctrl && keyCode == 86) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (!clip.isEmpty()) {
                insertText(clip);
            }
            return true;
        } else if (ctrl && keyCode == 90) {
            undo();
            return true;
        } else if (ctrl && keyCode == 89) {
            redo();
            return true;
        } else if (keyCode == 259) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursorCol > 0) {
                pushUndoState();
                String line = lines.get(cursorLine);
                lines.set(cursorLine, line.substring(0, cursorCol - 1) + line.substring(cursorCol));
                cursorCol--;
            } else if (cursorLine > 0) {
                pushUndoState();
                String prevLine = lines.get(cursorLine - 1);
                String currentLine = lines.get(cursorLine);
                lines.set(cursorLine - 1, prevLine + currentLine);
                lines.remove(cursorLine);
                cursorLine--;
                cursorCol = prevLine.length();
            }
            clearSelection();
            validateSyntax();
            scrollToCursor();
            if (responder != null) responder.accept(getValue());
            return true;
        } else if (keyCode == 261) {
            if (hasSelection()) {
                deleteSelection();
            } else {
                String line = lines.get(cursorLine);
                if (cursorCol < line.length()) {
                    pushUndoState();
                    lines.set(cursorLine, line.substring(0, cursorCol) + line.substring(cursorCol + 1));
                } else if (cursorLine < lines.size() - 1) {
                    pushUndoState();
                    String nextLine = lines.get(cursorLine + 1);
                    lines.set(cursorLine, line + nextLine);
                    lines.remove(cursorLine + 1);
                }
            }
            clearSelection();
            validateSyntax();
            scrollToCursor();
            if (responder != null) responder.accept(getValue());
            return true;
        } else if (keyCode == 257 || keyCode == 335) {
            pushUndoState();
            deleteSelection();
            String currentLine = lines.get(cursorLine);
            String indent = getLeadingWhitespace(currentLine);
            if (cursorCol > 0 && (currentLine.charAt(cursorCol - 1) == '{' || currentLine.charAt(cursorCol - 1) == '[')) {
                indent += "  ";
            }
            String before = currentLine.substring(0, cursorCol);
            String after = currentLine.substring(cursorCol);
            lines.set(cursorLine, before);
            lines.add(cursorLine + 1, indent + after);
            cursorLine++;
            cursorCol = indent.length();
            clearSelection();
            validateSyntax();
            scrollToCursor();
            if (responder != null) responder.accept(getValue());
            return true;
        } else if (keyCode == 258) {
            insertText("  ");
            return true;
        } else if (keyCode == 263) {
            if (cursorCol > 0) {
                cursorCol--;
            } else if (cursorLine > 0) {
                cursorLine--;
                cursorCol = lines.get(cursorLine).length();
            }
            if (!shift) clearSelection();
            scrollToCursor();
            return true;
        } else if (keyCode == 262) {
            if (cursorCol < lines.get(cursorLine).length()) {
                cursorCol++;
            } else if (cursorLine < lines.size() - 1) {
                cursorLine++;
                cursorCol = 0;
            }
            if (!shift) clearSelection();
            scrollToCursor();
            return true;
        } else if (keyCode == 265) {
            if (cursorLine > 0) {
                cursorLine--;
                cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
            } else {
                cursorCol = 0;
            }
            if (!shift) clearSelection();
            scrollToCursor();
            return true;
        } else if (keyCode == 264) {
            if (cursorLine < lines.size() - 1) {
                cursorLine++;
                cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
            } else {
                cursorCol = lines.get(cursorLine).length();
            }
            if (!shift) clearSelection();
            scrollToCursor();
            return true;
        } else if (keyCode == 268) {
            cursorCol = 0;
            if (!shift) clearSelection();
            scrollToCursor();
            return true;
        } else if (keyCode == 269) {
            cursorCol = lines.get(cursorLine).length();
            if (!shift) clearSelection();
            scrollToCursor();
            return true;
        }

        return super.keyPressed(event);
    }

    private String getSelectedText() {
        if (!hasSelection()) return "";
        int startL, startC, endL, endC;
        if (cursorLine < selectAnchorLine || (cursorLine == selectAnchorLine && cursorCol < selectAnchorCol)) {
            startL = cursorLine;
            startC = cursorCol;
            endL = selectAnchorLine;
            endC = selectAnchorCol;
        } else {
            startL = selectAnchorLine;
            startC = selectAnchorCol;
            endL = cursorLine;
            endC = cursorCol;
        }

        if (startL == endL) {
            return lines.get(startL).substring(startC, endC);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(lines.get(startL).substring(startC)).append("\n");
        for (int i = startL + 1; i < endL; i++) {
            sb.append(lines.get(i)).append("\n");
        }
        sb.append(lines.get(endL), 0, endC);
        return sb.toString();
    }

    private String getLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(0, i);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!isFocused() || !this.active || !this.visible) return false;
        int codePoint = event.codepoint();
        if (codePoint >= 32 && codePoint != 127) {
            insertText(Character.toString(codePoint));
            return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            focusedTime = Util.getMillis();
        }
    }

    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // Background and Border
        gfx.fill(x, y, x + w, y + h, 0xF411131A);
        int borderCol = (syntaxError != null) ? EditorTheme.BORDER_ERROR
                : (isFocused() ? EditorTheme.BORDER_FOCUSED : EditorTheme.BORDER_INNER);
        gfx.outline(x, y, w, h, borderCol);

        // Gutter
        int gutterW = getGutterWidth();
        gfx.fill(x + 1, y + 1, x + gutterW, y + h - 1, 0xFF0E1016);
        gfx.fill(x + gutterW, y + 1, x + gutterW + 1, y + h - 1, EditorTheme.BORDER_INNER);

        // Scissored text area
        int textX = x + gutterW + PADDING_X;
        int textW = w - gutterW - PADDING_X - SCROLLBAR_WIDTH - 4;
        int textH = h - PADDING_Y * 2;

        gfx.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);

        int startLine = Math.max(0, (int) (scrollY / LINE_HEIGHT));
        int endLine = Math.min(lines.size() - 1, (int) ((scrollY + textH) / LINE_HEIGHT) + 1);

        for (int i = startLine; i <= endLine; i++) {
            int lineY = (int) (y + PADDING_Y + i * LINE_HEIGHT - scrollY);

            // Draw line number
            String lineNumStr = String.valueOf(i + 1);
            int numW = font.width(lineNumStr);
            gfx.text(font, lineNumStr, x + gutterW - numW - 4, lineY + 1, EditorTheme.TEXT_MUTED, false);

            // Selection background
            if (hasSelection()) {
                renderLineSelection(gfx, i, textX, lineY);
            }

            // Syntax highlighted line text
            String line = lines.get(i);
            renderHighlightedLine(gfx, line, textX, lineY);

            // Cursor blink
            boolean blink = isFocused() && (Util.getMillis() - focusedTime) / 300 % 2 == 0;
            if (blink && i == cursorLine) {
                int cursorX = textX + font.width(line.substring(0, Math.min(cursorCol, line.length())));
                gfx.fill(cursorX, lineY - 1, cursorX + 1, lineY + LINE_HEIGHT - 1, EditorTheme.ACCENT_GOLD);
            }
        }

        gfx.disableScissor();

        // Scrollbar
        if (getMaxScroll() > 0) {
            int trackX = x + w - SCROLLBAR_WIDTH - 2;
            int trackY = y + 2;
            int trackH = h - 4;

            gfx.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackH, EditorTheme.SCROLL_TRACK);

            int thumbH = Math.max(16, trackH * trackH / Math.max(1, getContentHeight()));
            int availableTrack = trackH - thumbH;
            int thumbY = trackY + (int) (availableTrack * (scrollY / (double) getMaxScroll()));

            boolean hovered = mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH && mouseY >= thumbY && mouseY <= thumbY + thumbH;
            int thumbCol = (isDraggingScrollbar || hovered) ? EditorTheme.SCROLL_THUMB_HOVER : EditorTheme.SCROLL_THUMB;

            gfx.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH, thumbCol);
        }
    }

    private void renderLineSelection(GuiGraphicsExtractor gfx, int lineIdx, int textX, int lineY) {
        int startL, startC, endL, endC;
        if (cursorLine < selectAnchorLine || (cursorLine == selectAnchorLine && cursorCol < selectAnchorCol)) {
            startL = cursorLine;
            startC = cursorCol;
            endL = selectAnchorLine;
            endC = selectAnchorCol;
        } else {
            startL = selectAnchorLine;
            startC = selectAnchorCol;
            endL = cursorLine;
            endC = cursorCol;
        }

        if (lineIdx < startL || lineIdx > endL) return;

        String line = lines.get(lineIdx);
        int selStart = (lineIdx == startL) ? startC : 0;
        int selEnd = (lineIdx == endL) ? endC : line.length();
        selStart = Math.min(selStart, line.length());
        selEnd = Math.min(selEnd, line.length());

        int x1 = textX + font.width(line.substring(0, selStart));
        int x2 = textX + font.width(line.substring(0, selEnd));
        if (selStart == selEnd && lineIdx < endL) x2 += 4;

        gfx.fill(x1, lineY, Math.max(x1 + 2, x2), lineY + LINE_HEIGHT, 0x444C6EF5);
    }

    private void renderHighlightedLine(GuiGraphicsExtractor gfx, String line, int textX, int lineY) {
        if (line.isEmpty()) return;

        int curX = textX;
        int idx = 0;

        while (idx < line.length()) {
            char c = line.charAt(idx);

            if (Character.isWhitespace(c)) {
                int start = idx;
                while (idx < line.length() && Character.isWhitespace(line.charAt(idx))) idx++;
                curX += font.width(line.substring(start, idx));
                continue;
            }

            // String or Key
            if (c == '"') {
                int start = idx;
                idx++;
                while (idx < line.length() && line.charAt(idx) != '"') {
                    if (line.charAt(idx) == '\\' && idx + 1 < line.length()) idx++;
                    idx++;
                }
                if (idx < line.length()) idx++; // closing quote
                String strToken = line.substring(start, idx);

                // Lookahead: is this a property key (followed by colon)?
                boolean isKey = false;
                int look = idx;
                while (look < line.length() && Character.isWhitespace(line.charAt(look))) look++;
                if (look < line.length() && line.charAt(look) == ':') {
                    isKey = true;
                }

                int color = isKey ? COLOR_KEY : COLOR_STRING;
                gfx.text(font, strToken, curX, lineY + 1, color, false);
                curX += font.width(strToken);
                continue;
            }

            // Number
            if (Character.isDigit(c) || (c == '-' && idx + 1 < line.length() && Character.isDigit(line.charAt(idx + 1)))) {
                int start = idx;
                do {
                    idx++;
                } while (idx < line.length() && (Character.isDigit(line.charAt(idx)) || line.charAt(idx) == '.' || line.charAt(idx) == 'e' || line.charAt(idx) == 'E' || line.charAt(idx) == '+' || line.charAt(idx) == '-'));
                String numToken = line.substring(start, idx);
                gfx.text(font, numToken, curX, lineY + 1, COLOR_NUMBER, false);
                curX += font.width(numToken);
                continue;
            }

            // Boolean / null
            if (line.startsWith("true", idx)) {
                gfx.text(font, "true", curX, lineY + 1, COLOR_BOOLEAN, false);
                curX += font.width("true");
                idx += 4;
                continue;
            }
            if (line.startsWith("false", idx)) {
                gfx.text(font, "false", curX, lineY + 1, COLOR_BOOLEAN, false);
                curX += font.width("false");
                idx += 5;
                continue;
            }
            if (line.startsWith("null", idx)) {
                gfx.text(font, "null", curX, lineY + 1, COLOR_BOOLEAN, false);
                curX += font.width("null");
                idx += 4;
                continue;
            }

            // Punctuation
            if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
                gfx.text(font, String.valueOf(c), curX, lineY + 1, COLOR_PUNCTUATION, false);
                curX += font.width(String.valueOf(c));
                idx++;
                continue;
            }

            // Default fallback
            gfx.text(font, String.valueOf(c), curX, lineY + 1, COLOR_DEFAULT, false);
                curX += font.width(String.valueOf(c));
            idx++;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Raw JSON Editor"));
    }
}
