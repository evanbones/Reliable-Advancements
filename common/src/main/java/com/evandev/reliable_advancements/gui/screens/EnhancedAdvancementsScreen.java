package com.evandev.reliable_advancements.gui.screens;

import com.evandev.reliable_advancements.client.ClientRewardTracker;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.AdvancementContextMenu;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementTab;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementTabType;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementWidget;
import com.evandev.reliable_advancements.network.ClaimRewardPayload;
import com.evandev.reliable_advancements.network.EditAdvancementPayload;
import com.evandev.reliable_advancements.network.LinkAdvancementPayload;
import com.evandev.reliable_advancements.network.RequestAdvancementJsonPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.reference.Resources;
import com.evandev.reliable_advancements.util.PersistentData;
import com.evandev.reliable_advancements.util.RenderUtil;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EnhancedAdvancementsScreen extends Screen implements ClientAdvancements.Listener {
    public static final Set<EnhancedAdvancementWidget> selectedWidgets = new LinkedHashSet<>();
    private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
    private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
    private static final Component TITLE = Component.translatable("gui.advancements");
    private static final int WIDTH = 252, HEIGHT = 140, CORNER_SIZE = 30;
    private static final int SIDE = 30, TOP = 40, BOTTOM = 30, PADDING = 9;
    private static final float MIN_ZOOM = 0.25F, MAX_ZOOM = 2.0F, ZOOM_STEP = 0.15F;
    public static String clipboardJson = null;
    public static ResourceLocation clipboardId = null;
    public static boolean clientHasFullTree = false;
    private static ClientAdvancements lastAdvancementsManager = null;
    private static int tabPage, maxPages;
    private static ResourceLocation savedSelectedTab = null;
    private final ClientAdvancements clientAdvancements;
    private final Map<AdvancementHolder, EnhancedAdvancementTab> tabs = Maps.newLinkedHashMap();
    public EnhancedAdvancementWidget linkingWidget = null;
    public EnhancedAdvancementTab selectedTab;
    public int internalWidth;
    protected int internalHeight;
    private boolean isInitializing = false;
    private boolean isDirty = false;
    private float zoom = 1.0F;
    private boolean isScrolling;
    private EnhancedAdvancementWidget advConnectedToMouse = null;
    private AdvancementContextMenu contextMenu = null;
    private double dragOffsetX = 0.0;
    private double dragOffsetY = 0.0;
    private String linkingError = null;
    private long linkingErrorTime = 0;

    public EnhancedAdvancementsScreen(ClientAdvancements clientAdvancements) {
        super(GameNarrator.NO_TITLE);
        this.clientAdvancements = clientAdvancements;

        if (lastAdvancementsManager != clientAdvancements) {
            lastAdvancementsManager = clientAdvancements;
            clientHasFullTree = false;
        }
    }

    public static boolean canEdit() {
        return ModConfig.get().enableEditMode && Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDirty) {
            this.isDirty = false;
            this.minecraft.setScreen(new EnhancedAdvancementsScreen(this.clientAdvancements));
        }
    }

    public void centerOnAdvancement(ResourceLocation id) {
        AdvancementNode node = this.clientAdvancements.getTree().get(id);
        if (node == null) return;
        AdvancementNode root = node.root();
        EnhancedAdvancementTab targetTab = this.tabs.get(root.holder());

        if (targetTab != null) {
            this.selectedTab = targetTab;
            this.clientAdvancements.setSelectedTab(root.holder(), true);
            EnhancedAdvancementWidget widget = targetTab.getWidgets().get(node.holder());

            if (widget != null) {
                int boxWidth = getTabInternalWidth() - 2 * SIDE - 2 * PADDING;
                int boxHeight = getTabInternalHeight() - TOP - SIDE - 3 * PADDING;

                float currentZoom = getZoom();
                int scaledWidth = (int) (boxWidth / currentZoom);
                int scaledHeight = (int) (boxHeight / currentZoom);

                targetTab.scrollX = (scaledWidth / 2) - (widget.getX() + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2);
                targetTab.scrollY = (scaledHeight / 2) - (widget.getY() + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2);
                targetTab.setCentered(true);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public Map<AdvancementHolder, EnhancedAdvancementTab> getTabs() {
        return this.tabs;
    }

    public void editTabProperties() {
        if (this.selectedTab != null) {
            ResourceLocation id = this.selectedTab.getRootNode().holder().id();
            Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(id, "TabProperties"));
        }
        this.contextMenu = null;
    }

    public void sortTabs() {
        List<Map.Entry<AdvancementHolder, EnhancedAdvancementTab>> list = new ArrayList<>(this.tabs.entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<AdvancementHolder, EnhancedAdvancementTab> e) -> e.getValue().customIndex)
                .thenComparing(e -> ModConfig.get().orderTabsAlphabetically ? e.getValue().getTitle().getString() : ""));
        this.tabs.clear();
        int newIndex = 0;
        int tabW = getTabInternalWidth();
        int tabH = getTabInternalHeight();
        for (Map.Entry<AdvancementHolder, EnhancedAdvancementTab> e : list) {
            EnhancedAdvancementTab tab = e.getValue();
            tab.updateIndex(newIndex, tabW - 2 * 30, tabH - 40 - 30);
            this.tabs.put(e.getKey(), tab);
            newIndex++;
        }
    }

    public int getTabInternalWidth() {
        return selectedTab != null && selectedTab.customWidth > 0 ? selectedTab.customWidth : Math.min(this.internalWidth, 500);
    }

    public int getTabInternalHeight() {
        return selectedTab != null && selectedTab.customHeight > 0 ? selectedTab.customHeight : Math.min(this.internalHeight, 350);
    }

    public void closeContextMenu() {
        this.contextMenu = null;
    }

    public void removeWidgetFromClient(EnhancedAdvancementWidget widget) {
        if (selectedTab != null) {
            selectedTab.getWidgets().remove(widget.getAdvancement().holder());
            if (widget.getParent() != null) {
                widget.getParent().getChildren().remove(widget);
            }
        }
    }

    public float getZoom() {
        return this.zoom;
    }

    public void startLinking(EnhancedAdvancementWidget widget) {
        this.linkingWidget = widget;
        this.contextMenu = null;
        this.linkingError = null;
    }

    public void copyAdvancement(EnhancedAdvancementWidget widget) {
        ResourceLocation id = widget.getAdvancement().holder().id();
        Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(id, "Copy"));
    }

    public void deleteAdvancement(EnhancedAdvancementWidget widget) {
        this.minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        String dummyJson = "{\"criteria\":{\"impossible\":{\"trigger\":\"minecraft:impossible\"}}}";
                        EditAdvancementPayload payload = new EditAdvancementPayload(widget.getAdvancement().holder().id(), dummyJson, false);

                        if (Services.PLATFORM.canSendAdvancementEdit()) {
                            Services.PLATFORM.sendAdvancementEdit(payload);
                        }
                        PersistentData.removePosition(widget.getAdvancement().holder().id());
                        if (this.selectedTab != null && widget.getAdvancement().holder().id().equals(this.selectedTab.getRootNode().holder().id())) {
                            PersistentData.removeTabProperties(this.selectedTab.getRootNode().holder().id());
                        }
                        removeWidgetFromClient(widget);
                    }
                    this.minecraft.setScreen(this);
                },
                Component.literal("Delete Advancement?"),
                Component.literal("Are you sure you want to delete this advancement from the game? This cannot be undone.")
        ));
        this.contextMenu = null;
    }

    public void resetAdvancement(EnhancedAdvancementWidget widget) {
        this.minecraft.setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        EditAdvancementPayload payload = new EditAdvancementPayload(widget.getAdvancement().holder().id(), "{}", true);
                        if (Services.PLATFORM.canSendAdvancementEdit()) {
                            Services.PLATFORM.sendAdvancementEdit(payload);
                        }
                        PersistentData.removePosition(widget.getAdvancement().holder().id());
                        if (this.selectedTab != null && widget.getAdvancement().holder().id().equals(this.selectedTab.getRootNode().holder().id())) {
                            PersistentData.removeTabProperties(this.selectedTab.getRootNode().holder().id());
                        }

                        Services.PLATFORM.sendRequestFullTree();
                    }
                    this.minecraft.setScreen(this);
                },
                Component.literal("Reset Advancement?"),
                Component.literal("Are you sure you want to reset this advancement to its vanilla state? Any edits will be lost.")
        ));
        this.contextMenu = null;
    }

    public void pasteAdvancement(int mouseX, int mouseY) {
        if (clipboardJson == null || clipboardId == null) return;

        String namespace = clipboardId.getNamespace();
        String path = clipboardId.getPath();

        ResourceLocation newId;
        int counter = 1;

        while (true) {
            String suffix = counter == 1 ? "_copy" : "_copy" + counter;
            ResourceLocation testId = ResourceLocation.fromNamespaceAndPath(namespace, path + suffix);

            boolean exists = this.tabs.keySet().stream().anyMatch(h -> h.id().equals(testId)) ||
                    (this.selectedTab != null && this.selectedTab.getWidgets().keySet().stream().anyMatch(h -> h.id().equals(testId)));

            if (!exists) {
                newId = testId;
                break;
            }
            counter++;
        }

        JsonObject root = JsonParser.parseString(clipboardJson).getAsJsonObject();

        int tabW = getTabInternalWidth();
        int tabH = getTabInternalHeight();
        int left = SIDE + (width - tabW) / 2;
        int top = TOP + (height - tabH) / 2;
        double unzoomedX = (mouseX - left - PADDING) / this.zoom;
        double unzoomedY = (mouseY - top - 2 * PADDING) / this.zoom;

        int newPosX = (int) Math.round(unzoomedX - (this.selectedTab != null ? this.selectedTab.scrollX : 0)) - EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
        int newPosY = (int) Math.round(unzoomedY - (this.selectedTab != null ? this.selectedTab.scrollY : 0)) - EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;

        PersistentData.setPosition(newId, newPosX, newPosY);

        if (this.selectedTab != null) {
            root.addProperty("parent", this.selectedTab.getRootNode().holder().id().toString());
        } else {
            root.remove("parent");
        }

        EditAdvancementPayload payload = new EditAdvancementPayload(newId, root.toString(), false);
        if (Services.PLATFORM.canSendAdvancementEdit()) {
            Services.PLATFORM.sendAdvancementEdit(payload);
        }
    }

    public void createNewAdvancement(int mouseX, int mouseY) {
        ResourceLocation newId;
        int counter = 1;

        while (true) {
            String suffix = counter == 1 ? "" : "_" + counter;
            ResourceLocation testId = ResourceLocation.fromNamespaceAndPath("minecraft", "new_advancement" + suffix);

            boolean exists = this.selectedTab != null && this.selectedTab.getWidgets().keySet().stream().anyMatch(h -> h.id().equals(testId));

            if (!exists) {
                newId = testId;
                break;
            }
            counter++;
        }

        int tabW = getTabInternalWidth();
        int tabH = getTabInternalHeight();
        int left = SIDE + (width - tabW) / 2;
        int top = TOP + (height - tabH) / 2;
        double unzoomedX = (mouseX - left - PADDING) / this.zoom;
        double unzoomedY = (mouseY - top - 2 * PADDING) / this.zoom;

        int newPosX = (int) Math.round(unzoomedX - (this.selectedTab != null ? this.selectedTab.scrollX : 0)) - EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
        int newPosY = (int) Math.round(unzoomedY - (this.selectedTab != null ? this.selectedTab.scrollY : 0)) - EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;

        JsonObject root = new JsonObject();
        JsonObject display = new JsonObject();
        JsonObject icon = new JsonObject();
        icon.addProperty("id", "minecraft:stone");
        display.add("icon", icon);

        JsonObject title = new JsonObject();
        title.addProperty("text", "New Advancement");
        display.add("title", title);

        JsonObject description = new JsonObject();
        description.addProperty("text", "Description");
        display.add("description", description);
        root.add("display", display);

        JsonObject criteria = new JsonObject();
        JsonObject crit = new JsonObject();
        crit.addProperty("trigger", "minecraft:impossible");
        criteria.add("impossible", crit);
        root.add("criteria", criteria);

        if (this.selectedTab != null) {
            root.addProperty("parent", this.selectedTab.getRootNode().holder().id().toString());
        }

        AdvancementEditorScreen editor = new AdvancementEditorScreen(
                this, newId, true, newPosX, newPosY, "Properties", root.toString()
        );
        Minecraft.getInstance().setScreen(editor);
        this.contextMenu = null;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.isInitializing = true;

        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
        }
        PersistentData.load();
        this.internalHeight = this.height * ModConfig.get().uiScaling / 100;
        this.internalWidth = this.width * ModConfig.get().uiScaling / 100;
        this.tabs.clear();
        this.selectedTab = null;

        if (EnhancedAdvancementsScreen.canEdit() && !clientHasFullTree) {
            Services.PLATFORM.sendRequestFullTree();
            clientHasFullTree = true;
        }

        this.clientAdvancements.setListener(this);

        if (savedSelectedTab != null) {
            for (EnhancedAdvancementTab tab : this.tabs.values()) {
                if (tab.getRootNode().holder().id().equals(savedSelectedTab)) {
                    this.selectedTab = tab;
                    break;
                }
            }
        }

        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            this.selectedTab = this.tabs.values().iterator().next();
        }

        if (this.selectedTab != null) {
            this.clientAdvancements.setSelectedTab(this.selectedTab.getRootNode().holder(), true);
            this.selectedTab.loadScroll();
        }

        int tabW = getTabInternalWidth();
        int tabH = getTabInternalHeight();
        int left = SIDE + (width - tabW) / 2;
        int top = TOP + (height - tabH) / 2;
        int right = tabW - SIDE + (width - tabW) / 2;
        int bottom = tabH - SIDE + (height - tabH) / 2;
        int width = right - left;
        int height = bottom - top;
        int maxTabs = EnhancedAdvancementTabType.getMaxTabs(width, height);

        if (this.tabs.size() > maxTabs) {
            maxPages = (this.tabs.size() - 1) / maxTabs;
            tabPage = Math.min(tabPage, maxPages);
            addRenderableWidget(Button.builder(Component.literal("<"), b -> tabPage = Math.max(tabPage - 1, 0)).pos(left, bottom + 4).size(20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> tabPage = Math.min(tabPage + 1, maxPages)).pos(right - 20, bottom + 4).size(20, 20).build());
        } else {
            maxPages = 0;
            tabPage = 0;
        }

        if (ModConfig.get().showEditModeButton && this.minecraft.player != null && this.minecraft.player.hasPermissions(2)) {
            int editBtnWidth = 80;
            addRenderableWidget(Button.builder(Component.literal("Edit Mode: " + (ModConfig.get().enableEditMode ? "ON" : "OFF")), b -> {
                ModConfig.get().enableEditMode = !ModConfig.get().enableEditMode;
                if (ModConfig.get().enableEditMode) {
                    clientHasFullTree = true;
                    Services.PLATFORM.sendRequestFullTree();
                } else {
                    clientHasFullTree = false;
                    Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(
                            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "resync"), "Resync"));
                }
                b.setMessage(Component.literal("Edit: " + (ModConfig.get().enableEditMode ? "ON" : "OFF")));
            }).pos(this.width - editBtnWidth - 30, 10).size(editBtnWidth, 20).build());
        }

        this.isInitializing = false;
    }

    private boolean isDescendant(EnhancedAdvancementWidget potentialAncestor, EnhancedAdvancementWidget potentialDescendant) {
        if (potentialAncestor == potentialDescendant) return true;
        for (EnhancedAdvancementWidget child : potentialAncestor.getChildren()) {
            if (isDescendant(child, potentialDescendant)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (EnhancedAdvancementsScreen.canEdit() && button == 0 && this.contextMenu == null) {
            EnhancedAdvancementWidget hovered = getHoveredWidget(mouseX, mouseY);
            if (hovered != null) {
                if (Screen.hasShiftDown() || Screen.hasControlDown()) {
                    if (selectedWidgets.contains(hovered)) {
                        selectedWidgets.remove(hovered);
                    } else {
                        selectedWidgets.add(hovered);
                    }
                } else {
                    if (!selectedWidgets.contains(hovered)) {
                        selectedWidgets.clear();
                        selectedWidgets.add(hovered);
                    }
                }
            } else {
                int left = SIDE + (width - getTabInternalWidth()) / 2;
                int top = TOP + (height - getTabInternalHeight()) / 2;
                if (mouseX >= left && mouseX <= left + getTabInternalWidth() && mouseY >= top && mouseY <= top + getTabInternalHeight()) {
                    selectedWidgets.clear();
                }
            }
        }

        if (this.contextMenu != null) {
            if (this.contextMenu.mouseClicked(mouseX, mouseY, button)) {
                return true;
            } else {
                this.contextMenu = null;
            }
        }

        int tabW = getTabInternalWidth();
        int tabH = getTabInternalHeight();
        int left = SIDE + (width - tabW) / 2;
        int top = TOP + (height - tabH) / 2;
        int right = tabW - SIDE + (width - tabW) / 2;
        int bottom = tabH - SIDE + (height - tabH) / 2;
        int width = right - left;
        int height = bottom - top;

        if (this.linkingWidget != null && button == 0) {
            EnhancedAdvancementWidget target = getHoveredWidget(mouseX, mouseY);
            if (target != null && target != this.linkingWidget) {

                if (isDescendant(this.linkingWidget, target)) {
                    this.linkingError = "Cannot link: Creates a cyclic dependency!";
                    this.linkingErrorTime = Util.getMillis() + 3000;
                    this.linkingWidget = null;
                    return true;
                }

                ResourceLocation id = linkingWidget.getAdvancement().holder().id();

                ResourceLocation parentResId = target.getAdvancement().holder().id();
                LinkAdvancementPayload payload = new LinkAdvancementPayload(id, parentResId);
                Services.PLATFORM.sendLinkAdvancement(payload);

                if (linkingWidget.getParent() != null) {
                    linkingWidget.getParent().getChildren().remove(linkingWidget);
                }
                linkingWidget.setParent(target);
                target.getChildren().add(linkingWidget);

                this.linkingWidget = null;
                return true;
            } else if (target == null) {
                this.linkingWidget = null;
                return true;
            }
        }

        if (button == 0) {
            if (this.selectedTab != null && !EnhancedAdvancementsScreen.canEdit()) {
                EnhancedAdvancementWidget hovered = getHoveredWidget(mouseX, mouseY);
                if (hovered != null && ModConfig.get().requireRewardClaiming) {
                    boolean isCompleted = hovered.advancementProgress != null && hovered.advancementProgress.isDone();
                    boolean isClaimed = ClientRewardTracker.isClaimed(hovered.getAdvancement().holder().id());

                    if (isCompleted && !isClaimed) {
                        Minecraft.getInstance().player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

                        Services.PLATFORM.sendClaimReward(new ClaimRewardPayload(hovered.getAdvancement().holder().id()));
                        return true;
                    }
                }
            }

            int maxTabs = EnhancedAdvancementTabType.getMaxTabs(width, height);
            int skip = tabPage * maxTabs;

            for (EnhancedAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                if (tab.isMouseOver(left, top, width, height, mouseX, mouseY)) {
                    this.clientAdvancements.setSelectedTab(tab.getRootNode().holder(), true);
                    break;
                }
            }
        } else if (button == 1 && EnhancedAdvancementsScreen.canEdit()) {
            boolean inGui = mouseX < left + internalWidth - 2 * SIDE - PADDING && mouseX > left + PADDING && mouseY < top + internalHeight - TOP + 1 && mouseY > top + 2 * PADDING;

            if (inGui) {
                EnhancedAdvancementWidget hoveredWidget = getHoveredWidget(mouseX, mouseY);
                this.contextMenu = new AdvancementContextMenu(this, hoveredWidget, (int) mouseX, (int) mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private EnhancedAdvancementWidget getHoveredWidget(double mouseX, double mouseY) {
        if (this.selectedTab == null) return null;

        int left = SIDE + (width - getTabInternalWidth()) / 2;
        int top = TOP + (height - getTabInternalHeight()) / 2;

        double unzoomedX = (mouseX - left - PADDING) / this.zoom;
        double unzoomedY = (mouseY - top - 2 * PADDING) / this.zoom;

        for (EnhancedAdvancementWidget widget : this.selectedTab.getWidgets().values()) {
            if (widget.isMouseOver(this.selectedTab.scrollX, this.selectedTab.scrollY, unzoomedX, unzoomedY)) {
                return widget;
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.contextMenu != null) this.contextMenu = null;

        if (this.selectedTab != null) {
            if (Screen.hasControlDown()) {
                int left = SIDE + (width - internalWidth) / 2;
                int top = TOP + (height - internalHeight) / 2;

                double relMouseX = mouseX - (left + PADDING);
                double relMouseY = mouseY - (top + 2 * PADDING);

                float oldZoom = this.zoom;
                this.zoom = Mth.clamp(this.zoom + (float) scrollY * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);

                if (this.zoom != oldZoom) {
                    double shiftX = (relMouseX / this.zoom) - (relMouseX / oldZoom);
                    double shiftY = (relMouseY / this.zoom) - (relMouseY / oldZoom);
                    this.selectedTab.scroll(shiftX, shiftY, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
                }
            } else if (Screen.hasShiftDown()) {
                this.selectedTab.scroll(scrollY * 20.0 / this.zoom, 0, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
            } else {
                this.selectedTab.scroll(scrollX * 20.0 / this.zoom, scrollY * 20.0 / this.zoom, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.contextMenu != null) this.contextMenu = null;

        if (EnhancedAdvancementsScreen.canEdit()) {
            if (!selectedWidgets.isEmpty()) {
                int shift = Screen.hasShiftDown() ? 4 : 1;
                int dx = 0, dy = 0;
                if (keyCode == 265) dy = -shift; // Up
                else if (keyCode == 264) dy = shift; // Down
                else if (keyCode == 263) dx = -shift; // Left
                else if (keyCode == 262) dx = shift; // Right

                if (dx != 0 || dy != 0) {
                    for (EnhancedAdvancementWidget w : selectedWidgets) {
                        w.setX(w.getX() + dx);
                        w.setY(w.getY() + dy);
                        Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(w);
                        PersistentData.setMemoryPosition(w.getAdvancement().holder().id(), w.getX(), w.getY());
                    }
                    PersistentData.save(this.tabs);
                    return true;
                }
            }

            if (Screen.hasControlDown() && keyCode == 67) { // C
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.width / (double) this.minecraft.getWindow().getScreenWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.height / (double) this.minecraft.getWindow().getScreenHeight();
                EnhancedAdvancementWidget target = selectedWidgets.size() == 1 ? selectedWidgets.iterator().next() : null;
                if (target == null) target = getHoveredWidget(mouseX, mouseY);
                if (target != null) {
                    copyAdvancement(target);
                }
                return true;
            } else if (Screen.hasControlDown() && keyCode == 86) { // V
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.width / (double) this.minecraft.getWindow().getScreenWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.height / (double) this.minecraft.getWindow().getScreenHeight();
                pasteAdvancement((int) mouseX, (int) mouseY);
                return true;
            } else if (keyCode == 261 || keyCode == 259) { // Delete / backspace
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.width / (double) this.minecraft.getWindow().getScreenWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.height / (double) this.minecraft.getWindow().getScreenHeight();
                EnhancedAdvancementWidget target = selectedWidgets.size() == 1 ? selectedWidgets.iterator().next() : null;
                if (target == null) target = getHoveredWidget(mouseX, mouseY);
                if (target != null) {
                    deleteAdvancement(target);
                    return true;
                }
            }
        }

        if (this.minecraft.options.keyAdvancements.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.advConnectedToMouse != null) {
            if (selectedWidgets.contains(this.advConnectedToMouse)) {
                for (EnhancedAdvancementWidget w : selectedWidgets) {
                    Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(w);
                    PersistentData.setMemoryPosition(w.getAdvancement().holder().id(), w.getX(), w.getY());
                }
            } else {
                Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(this.advConnectedToMouse);
                PersistentData.setMemoryPosition(this.advConnectedToMouse.getAdvancement().holder().id(), this.advConnectedToMouse.getX(), this.advConnectedToMouse.getY());
            }
            this.advConnectedToMouse = null;
            if (EnhancedAdvancementsScreen.canEdit()) {
                PersistentData.save(this.tabs);
            }
        }
        this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        if (EnhancedAdvancementsScreen.canEdit()) {
            PersistentData.save(this.tabs);
        }
        if (this.selectedTab != null) {
            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
        }
        super.removed();
    }

    @Override
    public void onClose() {
        this.clientAdvancements.setListener(null);
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
        super.onClose();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double mouseDeltaX, double mouseDeltaY) {
        int left = SIDE + (width - getTabInternalWidth()) / 2;
        int top = TOP + (height - getTabInternalHeight()) / 2;

        if (button != 0 && button != 2) {
            this.isScrolling = false;
            return false;
        }

        if (this.contextMenu != null) this.contextMenu = null;

        if (!this.isScrolling) {
            if (this.advConnectedToMouse == null) {
                boolean inGui = mouseX < left + getTabInternalWidth() - 2 * SIDE - PADDING && mouseX > left + PADDING && mouseY < top + getTabInternalHeight() - TOP + 1 && mouseY > top + 2 * PADDING;
                if (this.selectedTab != null && inGui) {
                    double unzoomedMouseX = (mouseX - left - PADDING) / this.zoom;
                    double unzoomedMouseY = (mouseY - top - 2 * PADDING) / this.zoom;

                    for (EnhancedAdvancementWidget betterAdvancementEntryScreen : this.selectedTab.getWidgets().values()) {
                        if (betterAdvancementEntryScreen.isMouseOver(this.selectedTab.scrollX, this.selectedTab.scrollY, unzoomedMouseX, unzoomedMouseY)) {
                            if ((EnhancedAdvancementsScreen.canEdit() || betterAdvancementEntryScreen.enhancedDisplayInfo.allowDragging()) && button == 0) {
                                this.advConnectedToMouse = betterAdvancementEntryScreen;
                                this.dragOffsetX = unzoomedMouseX - (this.advConnectedToMouse.getX() + this.selectedTab.scrollX);
                                this.dragOffsetY = unzoomedMouseY - (this.advConnectedToMouse.getY() + this.selectedTab.scrollY);
                                break;
                            }
                        }
                    }
                }
            } else {
                double unzoomedMouseX = (mouseX - left - PADDING) / this.zoom;
                double unzoomedMouseY = (mouseY - top - 2 * PADDING) / this.zoom;

                int newPosX = (int) Math.round(unzoomedMouseX - this.dragOffsetX - this.selectedTab.scrollX);
                int newPosY = (int) Math.round(unzoomedMouseY - this.dragOffsetY - this.selectedTab.scrollY);

                if (Screen.hasShiftDown()) {
                    newPosX = 4 * Math.round((float) newPosX / 4);
                    newPosY = 4 * Math.round((float) newPosY / 4);
                }

                int deltaX = newPosX - this.advConnectedToMouse.getX();
                int deltaY = newPosY - this.advConnectedToMouse.getY();

                if (deltaX != 0 || deltaY != 0) {
                    if (selectedWidgets.contains(this.advConnectedToMouse)) {
                        for (EnhancedAdvancementWidget w : selectedWidgets) {
                            w.setX(w.getX() + deltaX);
                            w.setY(w.getY() + deltaY);
                        }
                    } else {
                        this.advConnectedToMouse.setX(newPosX);
                        this.advConnectedToMouse.setY(newPosY);
                    }
                }
            }
        } else {
            if (this.advConnectedToMouse != null) {
                Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(advConnectedToMouse);
            }
            this.advConnectedToMouse = null;
        }

        if (this.advConnectedToMouse == null) {
            if (!this.isScrolling) {
                this.isScrolling = true;
            } else if (this.selectedTab != null) {
                this.selectedTab.scroll(mouseDeltaX / this.zoom, mouseDeltaY / this.zoom, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
            }
        }
        return true;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int tabW = getTabInternalWidth();
        int tabH = getTabInternalHeight();
        int left = SIDE + (width - tabW) / 2;
        int top = TOP + (height - tabH) / 2;
        int right = tabW - SIDE + (width - tabW) / 2;
        int bottom = tabH - SIDE + (height - tabH) / 2;
        int width = right - left;
        int height = bottom - top;
        int maxTabs = EnhancedAdvancementTabType.getMaxTabs(width, height);
        int skip = tabPage * maxTabs;

        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        if (maxPages != 0) {
            Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
            int textWidth = this.font.width(page);
            guiGraphics.drawString(this.font, page.getVisualOrderText(), left + (tabW - textWidth) / 2 - textWidth, bottom + 8, -1);
        }

        this.renderInside(guiGraphics, mouseX, mouseY, left, top, right, bottom);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING, 0);
        guiGraphics.pose().scale(zoom, zoom, 1.0F);

        if (EnhancedAdvancementsScreen.canEdit() && this.selectedTab != null) {
            if (this.advConnectedToMouse != null) {
                java.util.Set<EnhancedAdvancementWidget> draggingWidgets = selectedWidgets.contains(this.advConnectedToMouse) ? selectedWidgets : java.util.Set.of(this.advConnectedToMouse);
                for (EnhancedAdvancementWidget w : draggingWidgets) {
                    int ax = w.getX() + this.selectedTab.scrollX;
                    int ay = w.getY() + this.selectedTab.scrollY;
                    guiGraphics.renderOutline(ax + 2, ay - 1, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 2, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 2, 0xFF00FF00);
                    guiGraphics.renderOutline(ax + 1, ay - 2, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 4, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 4, 0xFF00FF00);
                }
            }
        }
        guiGraphics.pose().popPose();

        if (this.linkingWidget != null) {
            int startX = (int) ((this.linkingWidget.getX() + this.selectedTab.scrollX + (float) EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2) * zoom) + left + PADDING;
            int startY = (int) ((this.linkingWidget.getY() + this.selectedTab.scrollY + (float) EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2) * zoom) + top + 2 * PADDING;

            RenderSystem.disableDepthTest();
            RenderUtil.line(guiGraphics, startX, startY, mouseX, mouseY, 2, 0xFF00FF00);
            RenderSystem.enableDepthTest();

            guiGraphics.drawString(this.font, "Select parent to link...", mouseX + 15, mouseY + 10, 0x00FF00);
        }

        this.renderWindow(guiGraphics, left, top, right, bottom, maxTabs, skip);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (this.advConnectedToMouse == null && this.contextMenu == null) {
            this.renderToolTips(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
        }

        if (this.advConnectedToMouse != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING, 0);
            guiGraphics.pose().scale(zoom, zoom, 1.0F);

            for (EnhancedAdvancementWidget advancementEntryScreen : this.selectedTab.getWidgets().values()) {
                if (advancementEntryScreen != this.advConnectedToMouse) {
                    int x1 = advancementEntryScreen.getX() + this.selectedTab.scrollX + 3;
                    int x2 = this.advConnectedToMouse.getX() + this.selectedTab.scrollX + 3;
                    int y1 = advancementEntryScreen.getY() + this.selectedTab.scrollY;
                    int y2 = this.advConnectedToMouse.getY() + this.selectedTab.scrollY;
                    int centerX1 = x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerX2 = x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerY1 = y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerY2 = y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    double degrees = Math.toDegrees(Math.atan2(centerX1 - centerX2, centerY1 - centerY2));
                    if (degrees < 0) {
                        degrees += 360;
                    }

                    if (advancementEntryScreen.getX() == this.advConnectedToMouse.getX()) {
                        if (y1 > y2) {
                            RenderUtil.drawRect(guiGraphics, x1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2, y1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        } else {
                            RenderUtil.drawRect(guiGraphics, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2, y1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, 1, 0x00FF00);
                        }
                    }
                    if (advancementEntryScreen.getY() == this.advConnectedToMouse.getY()) {
                        if (x1 > x2) {
                            RenderUtil.drawRect(guiGraphics, x2, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2, y1, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        } else {
                            RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2, y1, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        }
                    }
                    if (degrees == 45 || degrees == 135 || degrees == 225 || degrees == 315) {
                        RenderUtil.drawRect(guiGraphics, x1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x1, y1, x1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);

                        RenderUtil.drawRect(guiGraphics, x2, y2, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x2, y2, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);

                        if (degrees == 45 || degrees == 225) {
                            RenderUtil.drawRect(guiGraphics, x1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        } else {
                            RenderUtil.drawRect(guiGraphics, x1, y1, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        }
                    }
                }
            }
            guiGraphics.pose().popPose();
        }

        if (ModConfig.get().showDebugCoordinates && this.selectedTab != null && mouseX < getTabInternalWidth() - SIDE - PADDING && mouseX > SIDE + PADDING && mouseY < getTabInternalHeight() - top + 1 && mouseY > top + PADDING * 2) {
            if (this.advConnectedToMouse != null) {
                int currentX = (int) ((this.advConnectedToMouse.getX() + this.selectedTab.scrollX + 4) * zoom) + left + PADDING;
                int currentY = (int) ((this.advConnectedToMouse.getY() + this.selectedTab.scrollY) * zoom) + top + 2 * PADDING - font.lineHeight + 1;
                guiGraphics.drawString(font, this.advConnectedToMouse.getX() + "," + this.advConnectedToMouse.getY(), currentX, currentY, 0xFFFFFF);
            } else {
                int xMouse = (int) ((mouseX - left - PADDING) / zoom);
                int yMouse = (int) ((mouseY - top - 2 * PADDING) / zoom);
                int currentX = xMouse - this.selectedTab.scrollX - 4;
                int currentY = yMouse - this.selectedTab.scrollY - 1;
                guiGraphics.drawString(font, currentX + "," + currentY, mouseX, mouseY - font.lineHeight, 0xFFFFFF);
            }
        }

        if (this.contextMenu != null) {
            this.contextMenu.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        if (this.linkingError != null && Util.getMillis() < this.linkingErrorTime) {
            int errW = this.font.width(this.linkingError);
            guiGraphics.fill(mouseX + 10, mouseY - 15, mouseX + 16 + errW, mouseY + 1, 0xDD000000);
            guiGraphics.renderOutline(mouseX + 10, mouseY - 15, errW + 6, 16, 0xFFFF5555);
            guiGraphics.drawString(this.font, this.linkingError, mouseX + 13, mouseY - 11, 0xFF5555);
        }
    }

    private void renderInside(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        EnhancedAdvancementTab betterAdvancementTab = this.selectedTab;
        int boxLeft = left + PADDING;
        int boxTop = top + 2 * PADDING;
        int boxRight = right - PADDING;
        int boxBottom = bottom - PADDING;

        int width = boxRight - boxLeft;
        int height = boxBottom - boxTop;

        if (betterAdvancementTab == null) {
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, -16777216);
            guiGraphics.drawString(this.font, NO_ADVANCEMENTS_LABEL, boxLeft + (width - this.font.width(NO_ADVANCEMENTS_LABEL)) / 2, boxTop + height / 2 - this.font.lineHeight, -1);
            guiGraphics.drawString(this.font, VERY_SAD_LABEL, boxLeft + (width - this.font.width(VERY_SAD_LABEL)) / 2, boxTop + height / 2 + this.font.lineHeight, -1);
        } else {
            betterAdvancementTab.drawContents(guiGraphics, boxLeft, boxTop, width, height, mouseX, mouseY);
        }
    }

    public void renderWindow(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int maxTabs, int skip) {
        RenderSystem.enableBlend();
        guiGraphics.blit(Resources.Gui.WINDOW, left, top, 0, 0, CORNER_SIZE, CORNER_SIZE);
        int tabW = getTabInternalWidth();
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left + CORNER_SIZE, top, tabW - CORNER_SIZE - 2 * SIDE - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, 0, WIDTH - CORNER_SIZE - CORNER_SIZE, CORNER_SIZE);
        guiGraphics.blit(Resources.Gui.WINDOW, right - CORNER_SIZE, top, WIDTH - CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE - CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, right - CORNER_SIZE, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, WIDTH - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE - CORNER_SIZE);
        guiGraphics.blit(Resources.Gui.WINDOW, left, bottom - CORNER_SIZE, 0, HEIGHT - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left + CORNER_SIZE, bottom - CORNER_SIZE, tabW - CORNER_SIZE - 2 * SIDE - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE, WIDTH - CORNER_SIZE - CORNER_SIZE, CORNER_SIZE);
        guiGraphics.blit(Resources.Gui.WINDOW, right - CORNER_SIZE, bottom - CORNER_SIZE, WIDTH - CORNER_SIZE, HEIGHT - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE);

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            for (EnhancedAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                tab.drawTab(guiGraphics, left, top, width, height, tab == this.selectedTab);
            }

            RenderSystem.defaultBlendFunc();

            for (EnhancedAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                tab.drawIcon(guiGraphics, left, top, width, height);
            }

            RenderSystem.disableBlend();
        }

        FormattedCharSequence windowTitle = TITLE.getVisualOrderText();
        if (selectedTab != null) {
            windowTitle = FormattedCharSequence.composite(
                    windowTitle,
                    Component.literal(" - ").getVisualOrderText(),
                    selectedTab.getTitle().getVisualOrderText()
            );
        }
        guiGraphics.drawString(this.font, windowTitle, left + 8, top + 6, 4210752, false);
    }

    private void renderToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom, int maxTabs, int skip) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.selectedTab != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING, 400.0D);
            RenderSystem.enableDepthTest();
            this.selectedTab.drawToolTips(guiGraphics, mouseX - left - PADDING, mouseY - top - 2 * PADDING, left, top, right - left - 2 * PADDING, bottom - top - 3 * PADDING);
            RenderSystem.disableDepthTest();
            guiGraphics.pose().popPose();
        }

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            for (EnhancedAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                if (tab.isMouseOver(left, top, width, height, mouseX, mouseY)) {
                    guiGraphics.renderTooltip(this.font, tab.getTitle(), mouseX, mouseY);
                }
            }
        }
    }

    public void createNewTab(int mouseX, int mouseY) {
        ResourceLocation newId;
        int counter = 1;
        while (true) {
            String suffix = counter == 1 ? "" : "_" + counter;
            ResourceLocation testId = ResourceLocation.fromNamespaceAndPath("minecraft", "new_tab" + suffix);
            boolean exists = this.tabs.keySet().stream().anyMatch(h -> h.id().equals(testId));
            if (!exists) {
                newId = testId;
                break;
            }
            counter++;
        }

        JsonObject root = new JsonObject();
        JsonObject display = new JsonObject();
        JsonObject icon = new JsonObject();

        icon.addProperty("id", "minecraft:stone");
        display.add("icon", icon);

        JsonObject title = new JsonObject();
        title.addProperty("text", "New Tab");
        display.add("title", title);

        JsonObject description = new JsonObject();
        description.addProperty("text", "Description");
        display.add("description", description);

        display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/stone.png");
        root.add("display", display);

        JsonObject criteria = new JsonObject();
        JsonObject crit = new JsonObject();
        crit.addProperty("trigger", "minecraft:impossible");
        criteria.add("impossible", crit);
        root.add("criteria", criteria);

        AdvancementEditorScreen editor = new AdvancementEditorScreen(
                this, newId, true, 0, 0, "Properties", root.toString()
        );
        Minecraft.getInstance().setScreen(editor);
        this.contextMenu = null;
    }

    @Override
    public void onAdvancementsCleared() {
        if (this.selectedTab != null) {
            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            this.selectedTab.storeScroll();
        }
        this.tabs.clear();
        this.selectedTab = null;

        if (!this.isInitializing) {
            this.isDirty = true;
        }
    }

    @Override
    public void onAddAdvancementRoot(@NotNull AdvancementNode advancement) {
        EnhancedAdvancementTab betterAdvancementTabGui = EnhancedAdvancementTab.create(this.minecraft, this, this.tabs.size(), advancement, internalWidth - 2 * SIDE, internalHeight - TOP - SIDE);
        if (betterAdvancementTabGui != null) {
            this.tabs.put(advancement.holder(), betterAdvancementTabGui);
            sortTabs();
            if (advancement.holder().id().equals(savedSelectedTab) || this.selectedTab == null) {
                this.selectedTab = betterAdvancementTabGui;
                this.clientAdvancements.setSelectedTab(advancement.holder(), true);
                this.selectedTab.loadScroll();
            }
        }

        if (!this.isInitializing) {
            this.isDirty = true;
        }
    }

    @Override
    public void onRemoveAdvancementRoot(@NotNull AdvancementNode advancement) {
        EnhancedAdvancementTab tab = this.tabs.remove(advancement.holder());
        if (tab != null && tab == this.selectedTab) {
            if (!this.tabs.isEmpty()) {
                this.selectedTab = this.tabs.values().iterator().next();
            } else {
                this.selectedTab = null;
            }
        }

        if (!this.isInitializing) {
            this.isDirty = true;
        }
    }

    @Override
    public void onAddAdvancementTask(@NotNull AdvancementNode advancement) {
        EnhancedAdvancementTab betterAdvancementTabGui = this.getTab(advancement);
        if (betterAdvancementTabGui != null) {

            ResourceLocation id = advancement.holder().id();
            EnhancedAdvancementWidget oldWidget = null;
            for (EnhancedAdvancementWidget w : betterAdvancementTabGui.getWidgets().values()) {
                if (w.getAdvancement().holder().id().equals(id)) {
                    oldWidget = w;
                    break;
                }
            }

            if (oldWidget != null) {
                betterAdvancementTabGui.getWidgets().remove(oldWidget.getAdvancement().holder());
                if (oldWidget.getParent() != null) {
                    oldWidget.getParent().getChildren().remove(oldWidget);
                }
            }

            betterAdvancementTabGui.addAdvancement(advancement);
        }

        if (!this.isInitializing) {
            this.isDirty = true;
        }
    }

    @Override
    public void onRemoveAdvancementTask(@NotNull AdvancementNode advancement) {
        EnhancedAdvancementTab betterAdvancementTabGui = this.getTab(advancement);
        if (betterAdvancementTabGui != null) {
            EnhancedAdvancementWidget widget = betterAdvancementTabGui.getWidget(advancement.holder());
            if (widget != null) {
                betterAdvancementTabGui.getWidgets().remove(advancement.holder());
                if (widget.getParent() != null) {
                    widget.getParent().getChildren().remove(widget);
                }
            }
        }

        if (!this.isInitializing) {
            this.isDirty = true;
        }
    }

    @Override
    public void onUpdateAdvancementProgress(@NotNull AdvancementNode advancement, @NotNull AdvancementProgress advancementProgress) {
        EnhancedAdvancementWidget betterAdvancementEntryScreen = this.getAdvancementWidget(advancement);
        if (betterAdvancementEntryScreen != null) {
            betterAdvancementEntryScreen.getAdvancementProgress(advancementProgress);
        }
    }

    @Override
    public void onSelectedTabChanged(AdvancementHolder advancement) {
        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
        }
        this.selectedTab = this.tabs.get(advancement);
        if (this.selectedTab != null) {
            this.selectedTab.loadScroll();
        }
    }

    public EnhancedAdvancementWidget getAdvancementWidget(AdvancementNode advancement) {
        EnhancedAdvancementTab betterAdvancementTab = this.getTab(advancement);
        return betterAdvancementTab == null ? null : betterAdvancementTab.getWidget(advancement.holder());
    }

    private EnhancedAdvancementTab getTab(AdvancementNode advancement) {
        AdvancementNode advancementNode = advancement.root();
        return this.tabs.get(advancementNode.holder());
    }
}