package com.evandev.advancement_enhancement.util;

import com.evandev.advancement_enhancement.config.ModConfig;
import com.evandev.advancement_enhancement.reference.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.*;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An arrangement of criteria into rows and columns.
 */
public class CriterionGrid {
    private static final CriterionGrid empty = new CriterionGrid();
    public final int numRows;
    private final List<Component> cellContents;
    private final int[] cellWidths;
    private final int fontHeight;
    private final int numColumns;
    public List<Column> columns;
    public int width;
    public int height;

    private CriterionGrid() {
        this.cellContents = Collections.emptyList();
        this.cellWidths = new int[0];
        this.fontHeight = 0;
        this.numColumns = 0;
        this.numRows = 0;
        this.columns = Collections.emptyList();
        this.width = 0;
        this.height = 0;
    }

    public CriterionGrid(List<Component> cellContents, int[] cellWidths, int fontHeight, int numColumns) {
        this.cellContents = cellContents;
        this.cellWidths = cellWidths;
        this.fontHeight = fontHeight;
        this.numColumns = numColumns;
        this.numRows = (int) Math.ceil((double) cellContents.size() / numColumns);
    }

    /**
     * Of all the possible grids whose aspect ratio is less than the maximum, this method returns the one with the smallest number of rows.
     * If there is no such grid, this method returns a single-column grid.
     */
    public static CriterionGrid findOptimalCriterionGrid(AdvancementHolder holder, Advancement advancement, AdvancementProgress progress, int maxWidth, Font font) {
        if (progress == null || progress.isDone() || CriteriaDetail.fromName(ModConfig.get().criteriaDetail) == CriteriaDetail.OFF) {
            return CriterionGrid.empty;
        }
        AdvancementRequirements requirements = advancement.requirements();
        if (requirements.size() <= 1) {
            return CriterionGrid.empty;
        }
        int numUnobtained = 0;
        List<Component> cellContents = new ArrayList<>();
        CriteriaDetail currentDetail = CriteriaDetail.fromName(ModConfig.get().criteriaDetail);

        for (String criterion : requirements.names()) {
            CriterionProgress criterionProgress = progress.getCriterion(criterion);
            String criterionKey = Constants.MOD_ID + ".criterion." + holder.id() + "." + criterion;

            if (criterionProgress != null && criterionProgress.isDone()) {
                if (currentDetail.showObtained()) {
                    MutableComponent text = Component.literal(" + ").withStyle(ChatFormatting.GREEN);
                    MutableComponent text2 = Component.translatableWithFallback(criterionKey, criterion).withStyle(ChatFormatting.WHITE);
                    text.append(text2);
                    cellContents.add(text);
                }
            } else {
                if (currentDetail.showUnobtained()) {
                    MutableComponent text = Component.literal(" x ").withStyle(ChatFormatting.DARK_RED);
                    MutableComponent text2 = Component.translatableWithFallback(criterionKey, criterion).withStyle(ChatFormatting.WHITE);
                    text.append(text2);
                    cellContents.add(text);
                }
                numUnobtained++;
            }
        }

        if (!currentDetail.showUnobtained() && numUnobtained > 0) {
            MutableComponent text = Component.literal(" x ").withStyle(ChatFormatting.DARK_RED);
            MutableComponent text2 = Component.translatable(Constants.MOD_ID + ".remaining", numUnobtained).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC);
            text.append(text2);
            cellContents.add(text);
        }

        int[] cellWidths = new int[cellContents.size()];
        for (int i = 0; i < cellWidths.length; i++) {
            cellWidths[i] = font.width(cellContents.get(i));
        }

        int numCols = 0;
        CriterionGrid prevGrid = null;
        CriterionGrid currGrid = null;
        do {
            numCols++;
            CriterionGrid newGrid = new CriterionGrid(cellContents, cellWidths, font.lineHeight, numCols);
            if (prevGrid != null && newGrid.numRows == prevGrid.numRows) {
                // We increased the width without decreasing the height, which is pointless.
                continue;
            }
            newGrid.init();
            prevGrid = currGrid;
            currGrid = newGrid;
        } while (numCols <= cellContents.size() && currGrid.width <= maxWidth);
        return prevGrid != null ? prevGrid : currGrid;
    }

    public void init() {
        this.columns = new ArrayList<>();
        this.width = 0;
        for (int c = 0; c < this.numColumns; c++) {
            List<Component> column = new ArrayList<>();
            int columnWidth = 0;
            for (int r = 0; r < this.numRows; r++) {
                int cellIndex = c * this.numRows + r;
                if (cellIndex >= this.cellContents.size()) {
                    break;
                }
                column.add(this.cellContents.get(cellIndex));
                columnWidth = Math.max(columnWidth, this.cellWidths[cellIndex]);
            }
            this.columns.add(new Column(column, columnWidth));
            this.width += columnWidth;
        }
        this.height = this.numRows * this.fontHeight;
    }

    public record Column(List<Component> cells, int width) {
    }
}
