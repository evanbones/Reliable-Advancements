package com.evandev.reliable_advancements.gui.screens;

import com.evandev.reliable_advancements.advancements.IAdvancementSyncListener;
import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.advancements.IMultiParentNode;
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
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class EnhancedAdvancementsScreen extends Screen implements ClientAdvancements.Listener, IAdvancementSyncListener {
    public static final Set<EnhancedAdvancementWidget> selectedWidgets = new LinkedHashSet<>();
    private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
    private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
    private static final Component TITLE = Component.translatable("gui.advancements");
    private static final int WIDTH = 252, HEIGHT = 140, CORNER_SIZE = 30;
    private static final int SIDE = 30, TOP = 40, BOTTOM = 30, PADDING = 9;
    private static final float MIN_ZOOM = 0.25F, MAX_ZOOM = 2.0F, ZOOM_STEP = 0.15F;
    public static String clipboardJson = null;
    public static Identifier clipboardId = null;
    public static boolean clientHasFullTree = false;
    private static ClientAdvancements lastAdvancementsManager = null;
    private static int tabPage, maxPages;
    private static Identifier savedSelectedTab = null;
    private static float savedZoom = 1.0F;
    private final ClientAdvancements clientAdvancements;
    private final Screen parent;
    private final Map<AdvancementHolder, EnhancedAdvancementTab> tabs = Maps.newLinkedHashMap();
    public EnhancedAdvancementWidget linkingWidget = null;
    public EnhancedAdvancementTab selectedTab;
    public int internalWidth;
    protected int internalHeight;
    private boolean isLoading = false;
    private long loadingTimeout = 0;
    private Button prevPageBtn;
    private Button nextPageBtn;
    private Button editModeBtn;
    private float zoom = savedZoom;
    private boolean isScrolling;
    private EnhancedAdvancementWidget advConnectedToMouse = null;
    private AdvancementContextMenu contextMenu = null;
    private double dragOffsetX = 0.0;
    private double dragOffsetY = 0.0;
    private String linkingError = null;
    private long linkingErrorTime = 0;

    public EnhancedAdvancementsScreen(ClientAdvancements clientAdvancements) {
        this(clientAdvancements, null);
    }

    public EnhancedAdvancementsScreen(ClientAdvancements clientAdvancements, Screen parent) {
        super(GameNarrator.NO_TITLE);
        this.clientAdvancements = clientAdvancements;
        this.parent = parent;

        if (lastAdvancementsManager != clientAdvancements) {
            lastAdvancementsManager = clientAdvancements;
            clientHasFullTree = false;
        }
    }

    public static void setSavedSelectedTab(Identifier id) {
        savedSelectedTab = id;
    }

    public static boolean canEdit() {
        return ModConfig.get().enableEditMode && Minecraft.getInstance().player != null && Minecraft.getInstance().player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean hasShiftDown() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean hasControlDown() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static int tabSortOrdinal(AdvancementHolder root) {
        List<String> order = ModConfig.get().tabSortOrder;
        if (order == null || order.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        int index = order.indexOf(root.id().toString());
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            this.loadingTimeout = Util.getMillis() + 6000;
            this.advConnectedToMouse = null;
            this.linkingWidget = null;
            this.contextMenu = null;
            selectedWidgets.clear();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isLoading && Util.getMillis() > this.loadingTimeout) {
            this.isLoading = false;
        }
        if (editModeBtn == null && ModConfig.get().showEditModeButton && this.minecraft.player != null && this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            rebuildEditModeButton();
        }
    }

    @Override
    public void onAdvancementSyncComplete() {
        this.isLoading = false;
        this.finalizeResync();
    }

    public void finalizeResync() {
        Identifier draggingId = this.advConnectedToMouse != null
                ? this.advConnectedToMouse.getAdvancement().holder().id() : null;
        Set<Identifier> selectedIds = new LinkedHashSet<>();
        for (EnhancedAdvancementWidget w : selectedWidgets) {
            selectedIds.add(w.getAdvancement().holder().id());
        }
        for (EnhancedAdvancementTab t : this.tabs.values()) {
            PersistentData.snapshotTabPositions(t);
        }

        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
            if (savedSelectedTab == null) {
                savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            }
        }

        sortTabs();
        this.selectedTab = findTabById(savedSelectedTab);
        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            this.selectedTab = this.tabs.values().iterator().next();
            if (savedSelectedTab == null) {
                savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            }
        }
        if (this.selectedTab != null) {
            this.clientAdvancements.setSelectedTab(this.selectedTab.getRootNode().holder(), true);
            this.selectedTab.loadScroll();
        }
        rebuildNavigationWidgets();
        rebuildEditModeButton();
        this.isLoading = false;

        this.advConnectedToMouse = draggingId != null ? findWidgetById(draggingId) : null;
        selectedWidgets.clear();
        for (Identifier id : selectedIds) {
            EnhancedAdvancementWidget w = findWidgetById(id);
            if (w != null) selectedWidgets.add(w);
        }
    }

    private @Nullable EnhancedAdvancementWidget findWidgetById(Identifier id) {
        if (id == null) return null;
        for (EnhancedAdvancementTab t : this.tabs.values()) {
            EnhancedAdvancementWidget w = t.getWidget(id);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    public TabBounds getTabBounds() {
        int tabW = getTabInternalWidth();
        int tabH = getTabInternalHeight();
        int left = SIDE + (this.width - tabW) / 2;
        int top = TOP + (this.height - tabH) / 2;
        int right = tabW - SIDE + (this.width - tabW) / 2;
        int bottom = tabH - SIDE + (this.height - tabH) / 2;
        int w = right - left;
        int h = bottom - top;
        int maxTabs = EnhancedAdvancementTabType.getMaxTabs(w, h);
        return new TabBounds(left, top, right, bottom, w, h, maxTabs);
    }

    public @Nullable EnhancedAdvancementTab findTabById(@Nullable Identifier id) {
        if (id == null) return null;
        for (EnhancedAdvancementTab tab : this.tabs.values()) {
            if (tab.getRootNode().holder().id().equals(id)) {
                return tab;
            }
        }
        return null;
    }

    public JsonObject createDefaultAdvancementJson(String titleText, String descText, @Nullable String background, @Nullable Identifier parent) {
        JsonObject root = new JsonObject();
        JsonObject display = new JsonObject();
        JsonObject icon = new JsonObject();
        icon.addProperty("id", "minecraft:stone");
        display.add("icon", icon);

        JsonObject title = new JsonObject();
        title.addProperty("text", titleText);
        display.add("title", title);

        JsonObject description = new JsonObject();
        description.addProperty("text", descText);
        display.add("description", description);

        if (background != null) {
            display.addProperty("background", background);
        }
        root.add("display", display);

        JsonObject criteria = new JsonObject();
        JsonObject crit = new JsonObject();
        crit.addProperty("trigger", "minecraft:impossible");
        criteria.add("impossible", crit);
        root.add("criteria", criteria);

        if (parent != null) {
            root.addProperty("parent", parent.toString());
        }
        return root;
    }

    public AdvancementEditorScreen createEditorScreen(Identifier id, int posX, int posY, JsonObject rootJson) {
        return new AdvancementEditorScreen(this, id, true, posX, posY, "Properties", rootJson.toString());
    }

    public void updateTabPage(int maxTabs) {
        if (maxTabs > 0 && this.selectedTab != null) {
            int tabIndex = new ArrayList<>(this.tabs.values()).indexOf(this.selectedTab);
            if (tabIndex >= 0) {
                tabPage = Math.min(tabIndex / maxTabs, maxPages);
            }
        }
    }

    public void rebuildNavigationWidgets() {
        if (prevPageBtn != null) removeWidget(prevPageBtn);
        if (nextPageBtn != null) removeWidget(nextPageBtn);

        TabBounds bounds = getTabBounds();

        if (this.tabs.size() > bounds.maxTabs && bounds.maxTabs > 0) {
            maxPages = (this.tabs.size() - 1) / bounds.maxTabs;
            updateTabPage(bounds.maxTabs);
            prevPageBtn = Button.builder(Component.literal("<"), b -> tabPage = Math.max(tabPage - 1, 0)).pos(bounds.left, bounds.bottom + 4).size(20, 20).build();
            nextPageBtn = Button.builder(Component.literal(">"), b -> tabPage = Math.min(tabPage + 1, maxPages)).pos(bounds.right - 20, bounds.bottom + 4).size(20, 20).build();
            addRenderableWidget(prevPageBtn);
            addRenderableWidget(nextPageBtn);
        } else {
            maxPages = 0;
            tabPage = 0;
        }
    }

    public void rebuildEditModeButton() {
        if (editModeBtn != null) {
            removeWidget(editModeBtn);
            editModeBtn = null;
        }

        if (ModConfig.get().showEditModeButton && this.minecraft.player != null && this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            int editBtnWidth = 80;
            editModeBtn = Button.builder(Component.literal("Edit: " + (ModConfig.get().enableEditMode ? "ON" : "OFF")), b -> {
                ModConfig.get().enableEditMode = !ModConfig.get().enableEditMode;
                if (this.selectedTab != null) {
                    savedSelectedTab = this.selectedTab.getRootNode().holder().id();
                    this.selectedTab.storeScroll();
                    PersistentData.snapshotTabPositions(this.selectedTab);
                }
                this.setLoading(true);
                if (ModConfig.get().enableEditMode) {
                    clientHasFullTree = true;
                    Services.PLATFORM.sendRequestFullTree();
                } else {
                    clientHasFullTree = false;
                    Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(
                            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "resync"), "Resync"));
                }
                b.setMessage(Component.literal("Edit: " + (ModConfig.get().enableEditMode ? "ON" : "OFF")));
            }).pos(this.width - editBtnWidth - 30, 10).size(editBtnWidth, 20).build();
            addRenderableWidget(editModeBtn);
        }
    }

    public void centerOnAdvancement(Identifier id) {
        AdvancementNode node = this.clientAdvancements.getTree().get(id);
        if (node == null) return;
        AdvancementNode root = node.root();
        EnhancedAdvancementTab targetTab = this.tabs.get(root.holder());

        if (targetTab != null) {
            this.selectedTab = targetTab;
            savedSelectedTab = root.holder().id();
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
            Identifier id = this.selectedTab.getRootNode().holder().id();
            Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(id, "TabProperties"));
        }
        this.contextMenu = null;
    }

    public void sortTabs() {
        List<Map.Entry<AdvancementHolder, EnhancedAdvancementTab>> list = new ArrayList<>(this.tabs.entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<AdvancementHolder, EnhancedAdvancementTab> e) -> tabSortOrdinal(e.getKey()))
                .thenComparingInt(e -> e.getValue().customIndex)
                .thenComparing(e -> ModConfig.get().orderTabsAlphabetically ? e.getValue().getTitle().getString() : "")
                .thenComparing(e -> e.getKey().id().toString()));
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
            for (EnhancedAdvancementWidget p : widget.getParents()) {
                p.getChildren().remove(widget);
            }
        }
    }

    public void unlinkAllParents(EnhancedAdvancementWidget widget) {
        if (widget == null) return;
        if (this.selectedTab != null) {
            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            this.selectedTab.storeScroll();
            PersistentData.snapshotTabPositions(this.selectedTab);
        }
        Identifier childId = widget.getAdvancement().holder().id();
        PersistentData.save(this.tabs);

        for (EnhancedAdvancementWidget parentWidget : new ArrayList<>(widget.getParents())) {
            LinkAdvancementPayload payload = new LinkAdvancementPayload(childId, parentWidget.getAdvancement().holder().id(), true);
            Services.PLATFORM.sendLinkAdvancement(payload);
            parentWidget.getChildren().remove(widget);
        }
        widget.getParents().clear();
        IMultiParentAdvancement.setParents(widget.getAdvancement().advancement(), List.of());
        this.setLoading(true);
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
        Identifier id = widget.getAdvancement().holder().id();
        Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(id, "Copy"));
    }

    public void deleteAdvancement(EnhancedAdvancementWidget widget) {
        this.minecraft.setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        if (this.selectedTab != null) {
                            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
                            this.selectedTab.storeScroll();
                            PersistentData.snapshotTabPositions(this.selectedTab);
                        }

                        Identifier deletedId = widget.getAdvancement().holder().id();

                        for (EnhancedAdvancementWidget child : new ArrayList<>(widget.getChildren())) {
                            LinkAdvancementPayload unlinkPayload = new LinkAdvancementPayload(child.getAdvancement().holder().id(), deletedId, true);
                            Services.PLATFORM.sendLinkAdvancement(unlinkPayload);
                            child.getParents().remove(widget);
                        }
                        widget.getChildren().clear();

                        String dummyJson = "{\"criteria\":{\"impossible\":{\"trigger\":\"minecraft:impossible\"}}}";
                        EditAdvancementPayload payload = new EditAdvancementPayload(deletedId, dummyJson, false);
                        if (Services.PLATFORM.canSendAdvancementEdit()) {
                            Services.PLATFORM.sendAdvancementEdit(payload);
                        }
                        PersistentData.removePosition(deletedId);
                        if (this.selectedTab != null && deletedId.equals(this.selectedTab.getRootNode().holder().id())) {
                            PersistentData.removeTabProperties(deletedId);
                        }
                        PersistentData.save(this.tabs);
                        removeWidgetFromClient(widget);
                        this.setLoading(true);
                    }
                    this.minecraft.setScreen(this);
                },
                Component.translatable("gui.reliable_advancements.dialog.delete.title"),
                Component.translatable("gui.reliable_advancements.dialog.delete.message")
        ));
        this.contextMenu = null;
    }

    public void resetAdvancement(EnhancedAdvancementWidget widget) {
        this.minecraft.setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        if (this.selectedTab != null) {
                            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
                            this.selectedTab.storeScroll();
                        }
                        EditAdvancementPayload payload = new EditAdvancementPayload(widget.getAdvancement().holder().id(), "{}", true);
                        if (Services.PLATFORM.canSendAdvancementEdit()) {
                            Services.PLATFORM.sendAdvancementEdit(payload);
                        }
                        PersistentData.removePosition(widget.getAdvancement().holder().id());
                        if (this.selectedTab != null && widget.getAdvancement().holder().id().equals(this.selectedTab.getRootNode().holder().id())) {
                            PersistentData.removeTabProperties(this.selectedTab.getRootNode().holder().id());
                        }

                        this.setLoading(true);
                    }
                    this.minecraft.setScreen(this);
                },
                Component.translatable("gui.reliable_advancements.dialog.reset_advancement.title"),
                Component.translatable("gui.reliable_advancements.dialog.reset_advancement.message")
        ));
        this.contextMenu = null;
    }

    public void pasteAdvancement(int mouseX, int mouseY) {
        if (clipboardJson == null || clipboardId == null) return;

        String namespace = clipboardId.getNamespace();
        String path = clipboardId.getPath();

        Identifier newId;
        int counter = 1;

        while (true) {
            String suffix = counter == 1 ? "_copy" : "_copy" + counter;
            Identifier testId = Identifier.fromNamespaceAndPath(namespace, path + suffix);

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

        if (this.selectedTab != null) {
            PersistentData.snapshotTabPositions(this.selectedTab);
        }
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
        this.setLoading(true);
    }

    public void createNewAdvancement(int mouseX, int mouseY) {
        Identifier newId;
        int counter = 1;

        while (true) {
            String suffix = counter == 1 ? "" : "_" + counter;
            Identifier testId = Identifier.fromNamespaceAndPath("minecraft", "new_advancement" + suffix);

            boolean exists = this.selectedTab != null && this.selectedTab.getWidgets().keySet().stream().anyMatch(h -> h.id().equals(testId));

            if (!exists) {
                newId = testId;
                break;
            }
            counter++;
        }

        TabBounds bounds = getTabBounds();
        double unzoomedX = (mouseX - bounds.left - PADDING) / this.zoom;
        double unzoomedY = (mouseY - bounds.top - 2 * PADDING) / this.zoom;

        int newPosX = (int) Math.round(unzoomedX - (this.selectedTab != null ? this.selectedTab.scrollX : 0)) - EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
        int newPosY = (int) Math.round(unzoomedY - (this.selectedTab != null ? this.selectedTab.scrollY : 0)) - EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;

        Identifier parentId = this.selectedTab != null ? this.selectedTab.getRootNode().holder().id() : null;
        JsonObject root = createDefaultAdvancementJson("New Advancement", "Description", null, parentId);

        AdvancementEditorScreen editor = createEditorScreen(newId, newPosX, newPosY, root);
        Minecraft.getInstance().setScreen(editor);
        this.contextMenu = null;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.editModeBtn = null;
        this.advConnectedToMouse = null;
        this.linkingWidget = null;
        this.contextMenu = null;
        selectedWidgets.clear();

        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
        }
        PersistentData.load();
        this.internalHeight = this.height * ModConfig.get().uiScaling / 100;
        this.internalWidth = this.width * ModConfig.get().uiScaling / 100;
        this.clientAdvancements.setListener(this);

        if (EnhancedAdvancementsScreen.canEdit() && !clientHasFullTree) {
            this.setLoading(true);
            Services.PLATFORM.sendRequestFullTree();
            clientHasFullTree = true;
            return;
        }

        sortTabs();

        this.selectedTab = findTabById(savedSelectedTab);
        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            this.selectedTab = this.tabs.values().iterator().next();
        }

        if (this.selectedTab != null) {
            if (savedSelectedTab == null || this.selectedTab.getRootNode().holder().id().equals(savedSelectedTab)) {
                savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            }
            this.clientAdvancements.setSelectedTab(this.selectedTab.getRootNode().holder(), true);
            this.selectedTab.loadScroll();
        }

        this.rebuildNavigationWidgets();
        this.rebuildEditModeButton();
    }

    private boolean isDescendant(EnhancedAdvancementWidget potentialAncestor, EnhancedAdvancementWidget potentialDescendant) {
        Set<EnhancedAdvancementWidget> visited = new HashSet<>();
        return isDescendant(potentialAncestor, potentialDescendant, visited);
    }

    private boolean isDescendant(EnhancedAdvancementWidget current, EnhancedAdvancementWidget target, Set<EnhancedAdvancementWidget> visited) {
        if (current == target) return true;
        if (!visited.add(current)) return false;
        for (EnhancedAdvancementWidget child : current.getChildren()) {
            if (isDescendant(child, target, visited)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (this.isLoading) return true;

        if (EnhancedAdvancementsScreen.canEdit() && button == 0 && this.contextMenu == null) {
            EnhancedAdvancementWidget hovered = getHoveredWidget(mouseX, mouseY);
            if (hovered != null) {
                if (hasShiftDown() || hasControlDown()) {
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
                TabBounds bounds = getTabBounds();
                if (mouseX >= bounds.left && mouseX <= bounds.right && mouseY >= bounds.top && mouseY <= bounds.bottom) {
                    selectedWidgets.clear();
                }
            }
        }

        if (this.contextMenu != null) {
            if (this.contextMenu.mouseClicked(event, doubleClick)) {
                return true;
            } else {
                this.contextMenu = null;
            }
        }

        TabBounds bounds = getTabBounds();
        int left = bounds.left;
        int top = bounds.top;
        int width = bounds.width;
        int height = bounds.height;

        if (this.linkingWidget != null && button == 0) {
            EnhancedAdvancementWidget target = getHoveredWidget(mouseX, mouseY);
            if (target != null && target != this.linkingWidget) {
                if (this.selectedTab != null) {
                    savedSelectedTab = this.selectedTab.getRootNode().holder().id();
                    this.selectedTab.storeScroll();
                    PersistentData.snapshotTabPositions(this.selectedTab);
                }

                Identifier id = linkingWidget.getAdvancement().holder().id();
                Identifier parentResId = target.getAdvancement().holder().id();

                PersistentData.save(this.tabs);

                if (linkingWidget.getParents().contains(target)) {
                    LinkAdvancementPayload payload = new LinkAdvancementPayload(id, parentResId, true);
                    Services.PLATFORM.sendLinkAdvancement(payload);

                    linkingWidget.removeParent(target);
                    target.getChildren().remove(linkingWidget);
                } else {
                    if (isDescendant(this.linkingWidget, target)) {
                        this.linkingError = Component.translatable("gui.reliable_advancements.linking.error.cycle").getString();
                        this.linkingErrorTime = Util.getMillis() + 3000;
                        this.linkingWidget = null;
                        return true;
                    }

                    LinkAdvancementPayload payload = new LinkAdvancementPayload(id, parentResId, false);
                    Services.PLATFORM.sendLinkAdvancement(payload);

                    linkingWidget.addParent(target);
                    if (!target.getChildren().contains(linkingWidget)) {
                        target.getChildren().add(linkingWidget);
                    }
                }

            }
            this.linkingWidget = null;
            return true;
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
        return super.mouseClicked(event, doubleClick);
    }

    private EnhancedAdvancementWidget getHoveredWidget(double mouseX, double mouseY) {
        if (this.selectedTab == null) return null;

        TabBounds bounds = getTabBounds();

        double unzoomedX = (mouseX - bounds.left - PADDING) / this.zoom;
        double unzoomedY = (mouseY - bounds.top - 2 * PADDING) / this.zoom;

        for (EnhancedAdvancementWidget widget : this.selectedTab.getWidgets().values()) {
            if (widget.isMouseOver(this.selectedTab.scrollX, this.selectedTab.scrollY, unzoomedX, unzoomedY)) {
                return widget;
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isLoading) return true;
        if (this.contextMenu != null) this.contextMenu = null;
        if (this.selectedTab != null) {
            if (hasControlDown()) {
                int left = SIDE + (width - internalWidth) / 2;
                int top = TOP + (height - internalHeight) / 2;
                double relMouseX = mouseX - (left + PADDING);
                double relMouseY = mouseY - (top + 2 * PADDING);
                float oldZoom = this.zoom;
                this.zoom = Mth.clamp(this.zoom + (float) scrollY * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
                savedZoom = this.zoom;

                if (this.zoom != oldZoom) {
                    double shiftX = (relMouseX / this.zoom) - (relMouseX / oldZoom);
                    double shiftY = (relMouseY / this.zoom) - (relMouseY / oldZoom);
                    this.selectedTab.scroll(shiftX, shiftY, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
                }
            } else if (hasShiftDown()) {
                this.selectedTab.scroll(scrollY * 20.0 / this.zoom, 0, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
            } else {
                this.selectedTab.scroll(scrollX * 20.0 / this.zoom, scrollY * 20.0 / this.zoom, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        if (this.isLoading) {
            if (keyCode == 256) {
                this.onClose();
                return true;
            }
            return true;
        }
        if (this.contextMenu != null) this.contextMenu = null;

        if (keyCode == 256 && this.linkingWidget != null) {
            this.linkingWidget = null;
            return true;
        }

        if (EnhancedAdvancementsScreen.canEdit()) {
            if (!selectedWidgets.isEmpty()) {
                int shift = hasShiftDown() ? 4 : 1;
                int dx = 0, dy = 0;
                if (keyCode == 265) dy = -shift;
                else if (keyCode == 264) dy = shift;
                else if (keyCode == 263) dx = -shift;
                else if (keyCode == 262) dx = shift;

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

            if (hasControlDown() && keyCode == 67) {
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.width / (double) this.minecraft.getWindow().getScreenWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.height / (double) this.minecraft.getWindow().getScreenHeight();
                EnhancedAdvancementWidget target = selectedWidgets.size() == 1 ? selectedWidgets.iterator().next() : null;
                if (target == null) target = getHoveredWidget(mouseX, mouseY);
                if (target != null) {
                    copyAdvancement(target);
                }
                return true;
            } else if (hasControlDown() && keyCode == 86) {
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.width / (double) this.minecraft.getWindow().getScreenWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.height / (double) this.minecraft.getWindow().getScreenHeight();
                pasteAdvancement((int) mouseX, (int) mouseY);
                return true;
            } else if (keyCode == 261 || keyCode == 259) {
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

        if (this.minecraft.options.keyAdvancements.matches(event) || this.minecraft.options.keyInventory.matches(event)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (this.isLoading) return true;
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
            if (this.selectedTab != null) {
                this.selectedTab.recalculateBounds();
            }
            if (EnhancedAdvancementsScreen.canEdit()) {
                PersistentData.save(this.tabs);
            }
        }
        this.isScrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        if (EnhancedAdvancementsScreen.canEdit()) {
            PersistentData.save(this.tabs);
        }
        if (this.selectedTab != null) {
            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            this.selectedTab.storeScroll();
        }
        this.advConnectedToMouse = null;
        this.linkingWidget = null;
        this.contextMenu = null;
        selectedWidgets.clear();
        super.removed();
    }

    @Override
    public void onClose() {
        this.clientAdvancements.setListener(null);
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseDeltaX, double mouseDeltaY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (this.isLoading) return true;
        TabBounds bounds = getTabBounds();

        if (button != 0 && button != 2) {
            this.isScrolling = false;
            return false;
        }

        if (this.contextMenu != null) this.contextMenu = null;

        if (!this.isScrolling) {
            if (this.advConnectedToMouse == null) {
                boolean inGui = mouseX < bounds.right - PADDING && mouseX > bounds.left + PADDING && mouseY < bounds.bottom + 1 && mouseY > bounds.top + 2 * PADDING;
                if (this.selectedTab != null && inGui) {
                    double unzoomedMouseX = (mouseX - bounds.left - PADDING) / this.zoom;
                    double unzoomedMouseY = (mouseY - bounds.top - 2 * PADDING) / this.zoom;

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
                double unzoomedMouseX = (mouseX - bounds.left - PADDING) / this.zoom;
                double unzoomedMouseY = (mouseY - bounds.top - 2 * PADDING) / this.zoom;

                int newPosX = (int) Math.round(unzoomedMouseX - this.dragOffsetX - this.selectedTab.scrollX);
                int newPosY = (int) Math.round(unzoomedMouseY - this.dragOffsetY - this.selectedTab.scrollY);

                if (hasShiftDown()) {
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
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        TabBounds bounds = getTabBounds();
        int tabW = getTabInternalWidth();
        int left = bounds.left;
        int top = bounds.top;
        int right = bounds.right;
        int bottom = bounds.bottom;
        int maxTabs = bounds.maxTabs;
        int skip = tabPage * maxTabs;

        if (maxPages != 0) {
            Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
            int textWidth = this.font.width(page);
            guiGraphics.text(this.font, page.getVisualOrderText(), left + (tabW - textWidth) / 2 - textWidth, bottom + 8, -1);
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

        this.renderInside(guiGraphics, mouseX, mouseY, left, top, right, bottom);

        if (!this.isLoading) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING);
            guiGraphics.pose().scale(zoom, zoom);

            if (EnhancedAdvancementsScreen.canEdit() && this.selectedTab != null) {
                if (this.advConnectedToMouse != null) {
                    Set<EnhancedAdvancementWidget> draggingWidgets = selectedWidgets.contains(this.advConnectedToMouse) ? selectedWidgets : Set.of(this.advConnectedToMouse);
                    for (EnhancedAdvancementWidget w : draggingWidgets) {
                        int ax = w.getX() + this.selectedTab.scrollX;
                        int ay = w.getY() + this.selectedTab.scrollY;
                        guiGraphics.outline(ax + 2, ay - 1, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 2, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 2, 0xFF00FF00);
                        guiGraphics.outline(ax + 1, ay - 2, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 4, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 4, 0xFF00FF00);
                    }
                }
            }
            guiGraphics.pose().popMatrix();

            if (this.linkingWidget != null && this.selectedTab != null) {
                int startX = (int) ((this.linkingWidget.getX() + this.selectedTab.scrollX + (float) EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2) * zoom) + left + PADDING;
                int startY = (int) ((this.linkingWidget.getY() + this.selectedTab.scrollY + (float) EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2) * zoom) + top + 2 * PADDING;

                EnhancedAdvancementWidget hoveredTarget = getHoveredWidget(mouseX, mouseY);
                int lineColor = 0xFF00FF00;
                Component promptText = Component.translatable("gui.reliable_advancements.linking.prompt");

                if (hoveredTarget != null && hoveredTarget != this.linkingWidget) {
                    if (linkingWidget.getParents().contains(hoveredTarget)) {
                        lineColor = 0xFFFF5555;
                        promptText = Component.translatable("gui.reliable_advancements.linking.unlink", Advancement.name(hoveredTarget.getAdvancement().holder()));
                    } else if (isDescendant(this.linkingWidget, hoveredTarget)) {
                        lineColor = 0xFFAA0000;
                        promptText = Component.translatable("gui.reliable_advancements.linking.error.cycle");
                    } else {
                        lineColor = 0xFF55FF55;
                        promptText = Component.translatable("gui.reliable_advancements.linking.link", Advancement.name(hoveredTarget.getAdvancement().holder()));
                    }
                }

                
                RenderUtil.line(guiGraphics, startX, startY, mouseX, mouseY, 1, lineColor);
                

                guiGraphics.text(this.font, promptText, mouseX + 15, mouseY + 10, lineColor);
            }
        }

        this.renderWindow(guiGraphics, left, top, right, bottom, maxTabs, skip);

        if (this.isLoading) {
            guiGraphics.pose().pushMatrix();
            

            int boxLeft = left + PADDING;
            int boxTop = top + 2 * PADDING;
            int boxRight = right - PADDING;
            int boxBottom = bottom - PADDING;
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xEE0B0F19);

            int cardW = 190, cardH = 46;
            int cardX = boxLeft + (boxRight - boxLeft - cardW) / 2;
            int cardY = boxTop + (boxBottom - boxTop - cardH) / 2;
            guiGraphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xF0161B22);
            guiGraphics.outline(cardX, cardY, cardW, cardH, 0xFF30363D);

            String dots = ".".repeat((int) ((Util.getMillis() / 350) % 4));
            String msg = "Loading Advancements" + dots;
            guiGraphics.text(this.font, msg, cardX + (cardW - this.font.width(msg)) / 2, cardY + (cardH - this.font.lineHeight) / 2, 0xFFE6EDF3, false);

            
            guiGraphics.pose().popMatrix();
        } else {
            if (this.advConnectedToMouse == null && this.contextMenu == null) {
                this.renderToolTips(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
            }

            if (this.advConnectedToMouse != null && this.selectedTab != null) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING);
                guiGraphics.pose().scale(zoom, zoom);

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
                                RenderUtil.drawRect(guiGraphics, x1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2, y1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0xFF00FF00);
                            } else {
                                RenderUtil.drawRect(guiGraphics, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2, y1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, 1, 0xFF00FF00);
                            }
                        }
                        if (advancementEntryScreen.getY() == this.advConnectedToMouse.getY()) {
                            if (x1 > x2) {
                                RenderUtil.drawRect(guiGraphics, x2, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1, y1, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x2, y1, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x2, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                            } else {
                                RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1, y2, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x2, y1, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1, y1, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                            }
                        }
                        if (degrees == 45 || degrees == 135 || degrees == 225 || degrees == 315) {
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, 1, 0xFF00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);

                            RenderUtil.drawRect(guiGraphics, x2, y2, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0xFF00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                            RenderUtil.drawRect(guiGraphics, x2, y2, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);

                            if (degrees == 45 || degrees == 225) {
                                RenderUtil.drawRect(guiGraphics, x1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0xFF00FF00);
                            } else {
                                RenderUtil.drawRect(guiGraphics, x1, y1, x2, y2, 1, 0xFF00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0xFF00FF00);
                            }
                        }
                    }
                }
                guiGraphics.pose().popMatrix();
            }

            if (ModConfig.get().showDebugCoordinates && this.selectedTab != null && mouseX < getTabInternalWidth() - SIDE - PADDING && mouseX > SIDE + PADDING && mouseY < getTabInternalHeight() - top + 1 && mouseY > top + PADDING * 2) {
                if (this.advConnectedToMouse != null) {
                    int currentX = (int) ((this.advConnectedToMouse.getX() + this.selectedTab.scrollX + 4) * zoom) + left + PADDING;
                    int currentY = (int) ((this.advConnectedToMouse.getY() + this.selectedTab.scrollY) * zoom) + top + 2 * PADDING - font.lineHeight + 1;
                    guiGraphics.text(font, this.advConnectedToMouse.getX() + "," + this.advConnectedToMouse.getY(), currentX, currentY, 0xFFFFFFFF);
                } else {
                    int xMouse = (int) ((mouseX - left - PADDING) / zoom);
                    int yMouse = (int) ((mouseY - top - 2 * PADDING) / zoom);
                    int currentX = xMouse - this.selectedTab.scrollX - 4;
                    int currentY = yMouse - this.selectedTab.scrollY - 1;
                    guiGraphics.text(font, currentX + "," + currentY, mouseX, mouseY - font.lineHeight, 0xFFFFFFFF);
                }
            }

            if (this.contextMenu != null) {
                this.contextMenu.render(guiGraphics, mouseX, mouseY, partialTicks);
            }

            if (this.linkingError != null && Util.getMillis() < this.linkingErrorTime) {
                int errW = this.font.width(this.linkingError);
                guiGraphics.fill(mouseX + 10, mouseY - 15, mouseX + 16 + errW, mouseY + 1, 0xDD000000);
                guiGraphics.outline(mouseX + 10, mouseY - 15, errW + 6, 16, 0xFFFF5555);
                guiGraphics.text(this.font, this.linkingError, mouseX + 13, mouseY - 11, 0xFFFF5555);
            }
        }
    }

    private void renderInside(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        int boxLeft = left + PADDING;
        int boxTop = top + 2 * PADDING;
        int boxRight = right - PADDING;
        int boxBottom = bottom - PADDING;

        int width = boxRight - boxLeft;
        int height = boxBottom - boxTop;

        if (this.isLoading) {
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xFF0E131F);
            return;
        }

        EnhancedAdvancementTab betterAdvancementTab = this.selectedTab;
        if (betterAdvancementTab == null && !this.tabs.isEmpty()) {
            this.selectedTab = this.tabs.values().iterator().next();
            betterAdvancementTab = this.selectedTab;
        }

        if (betterAdvancementTab == null) {
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, -16777216);
            guiGraphics.text(this.font, NO_ADVANCEMENTS_LABEL, boxLeft + (width - this.font.width(NO_ADVANCEMENTS_LABEL)) / 2, boxTop + height / 2 - this.font.lineHeight, -1);
            guiGraphics.text(this.font, VERY_SAD_LABEL, boxLeft + (width - this.font.width(VERY_SAD_LABEL)) / 2, boxTop + height / 2 + this.font.lineHeight, -1);
        } else {
            betterAdvancementTab.drawContents(guiGraphics, boxLeft, boxTop, width, height, mouseX, mouseY);
        }
    }

    public void renderWindow(GuiGraphicsExtractor guiGraphics, int left, int top, int right, int bottom, int maxTabs, int skip) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WINDOW, left, top, 0f, 0f, CORNER_SIZE, CORNER_SIZE, 256, 256);
        int tabW = getTabInternalWidth();
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left + CORNER_SIZE, top, tabW - CORNER_SIZE - 2 * SIDE - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, 0, WIDTH - CORNER_SIZE - CORNER_SIZE, CORNER_SIZE);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WINDOW, right - CORNER_SIZE, top, (float) (WIDTH - CORNER_SIZE), 0f, CORNER_SIZE, CORNER_SIZE, 256, 256);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE - CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, right - CORNER_SIZE, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, WIDTH - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE - CORNER_SIZE);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WINDOW, left, bottom - CORNER_SIZE, 0f, (float) (HEIGHT - CORNER_SIZE), CORNER_SIZE, CORNER_SIZE, 256, 256);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left + CORNER_SIZE, bottom - CORNER_SIZE, tabW - CORNER_SIZE - 2 * SIDE - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE, WIDTH - CORNER_SIZE - CORNER_SIZE, CORNER_SIZE);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WINDOW, right - CORNER_SIZE, bottom - CORNER_SIZE, (float) (WIDTH - CORNER_SIZE), (float) (HEIGHT - CORNER_SIZE), CORNER_SIZE, CORNER_SIZE, 256, 256);

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            for (EnhancedAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                tab.drawTab(guiGraphics, left, top, width, height, tab == this.selectedTab);
            }

            for (EnhancedAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                tab.drawIcon(guiGraphics, left, top, width, height);
            }
        }

        FormattedCharSequence windowTitle = TITLE.getVisualOrderText();
        if (selectedTab != null) {
            windowTitle = FormattedCharSequence.composite(
                    windowTitle,
                    Component.literal(" - ").getVisualOrderText(),
                    selectedTab.getTitle().getVisualOrderText()
            );
        }
        guiGraphics.text(this.font, windowTitle, left + 8, top + 6, 0xFF404040, false);
    }

    private void renderToolTips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom, int maxTabs, int skip) {
        if (this.selectedTab != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING);
            
            this.selectedTab.drawToolTips(guiGraphics, mouseX - left - PADDING, mouseY - top - 2 * PADDING, left, top, right - left - 2 * PADDING, bottom - top - 3 * PADDING);
            
            guiGraphics.pose().popMatrix();
        }

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            for (EnhancedAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                if (tab.isMouseOver(left, top, width, height, mouseX, mouseY)) {
                    guiGraphics.setTooltipForNextFrame(this.font, tab.getTitle(), mouseX, mouseY);
                }
            }
        }
    }

    public void createNewTab() {
        if (this.selectedTab != null) {
            PersistentData.snapshotTabPositions(this.selectedTab);
        }

        Identifier newId;
        int counter = 1;
        while (true) {
            String suffix = counter == 1 ? "" : "_" + counter;
            Identifier testId = Identifier.fromNamespaceAndPath("minecraft", "new_tab" + suffix);
            boolean exists = this.tabs.keySet().stream().anyMatch(h -> h.id().equals(testId));
            if (!exists) {
                newId = testId;
                break;
            }
            counter++;
        }

        JsonObject root = createDefaultAdvancementJson("New Tab", "Description", "minecraft:textures/gui/advancements/backgrounds/stone.png", null);

        AdvancementEditorScreen editor = createEditorScreen(newId, 0, 0, root);
        Minecraft.getInstance().setScreen(editor);
        this.contextMenu = null;
    }

    @Override
    public void onAdvancementsCleared() {
        this.setLoading(true);
        if (this.selectedTab != null) {
            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            this.selectedTab.storeScroll();
        }
        this.tabs.clear();
        this.selectedTab = null;
    }

    @Override
    public void onAddAdvancementRoot(@NotNull AdvancementNode advancement) {
        EnhancedAdvancementTab existingTab = findTabById(advancement.holder().id());
        EnhancedAdvancementTab betterAdvancementTabGui = EnhancedAdvancementTab.create(this.minecraft, this, existingTab != null ? existingTab.getIndex() : this.tabs.size(), advancement, internalWidth - 2 * SIDE, internalHeight - TOP - SIDE);
        if (betterAdvancementTabGui != null) {
            if (existingTab != null) {
                betterAdvancementTabGui.scrollX = existingTab.scrollX;
                betterAdvancementTabGui.scrollY = existingTab.scrollY;
                betterAdvancementTabGui.setCentered(true);

                for (Map.Entry<AdvancementHolder, EnhancedAdvancementWidget> entry : existingTab.getWidgets().entrySet()) {
                    if (!entry.getKey().id().equals(advancement.holder().id())) {
                        EnhancedAdvancementWidget childWidget = entry.getValue();
                        betterAdvancementTabGui.addWidget(childWidget, entry.getKey());
                    }
                }

                for (EnhancedAdvancementWidget widget : betterAdvancementTabGui.getWidgets().values()) {
                    widget.attachToParent();
                }

                this.tabs.remove(existingTab.getRootNode().holder());
            }

            this.tabs.put(advancement.holder(), betterAdvancementTabGui);
            if (advancement.holder().id().equals(savedSelectedTab) || (existingTab != null && existingTab == this.selectedTab)) {
                this.selectedTab = betterAdvancementTabGui;
                this.selectedTab.loadScroll();
                this.clientAdvancements.setSelectedTab(this.selectedTab.getRootNode().holder(), true);
            }
        } else {
            this.onAddAdvancementTask(advancement);
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
        } else if (tab == null) {
            this.onRemoveAdvancementTask(advancement);
        }
    }

    @Override
    public void onAddAdvancementTask(@NotNull AdvancementNode advancement) {
        EnhancedAdvancementTab betterAdvancementTabGui = this.getTab(advancement);
        if (betterAdvancementTabGui != null) {
            Identifier id = advancement.holder().id();
            EnhancedAdvancementWidget oldWidget = betterAdvancementTabGui.getWidget(id);

            if (oldWidget != null) {
                betterAdvancementTabGui.removeWidget(oldWidget.getAdvancement().holder());
            }

            betterAdvancementTabGui.addAdvancement(advancement);
        }
    }

    @Override
    public void onRemoveAdvancementTask(@NotNull AdvancementNode advancement) {
        EnhancedAdvancementTab betterAdvancementTabGui = this.getTab(advancement);
        if (betterAdvancementTabGui != null) {
            betterAdvancementTabGui.removeWidget(advancement.holder());
        }
    }

    @Override
    public void onUpdateAdvancementProgress(@NotNull AdvancementNode advancement, @NotNull AdvancementProgress progress) {
        EnhancedAdvancementWidget betterAdvancementWidget = this.getAdvancementWidget(advancement);
        if (betterAdvancementWidget != null) {
            betterAdvancementWidget.getAdvancementProgress(progress);
        }
    }

    @Override
    public void onSelectedTabChanged(@Nullable AdvancementHolder advancement) {
        TabBounds bounds = getTabBounds();
        if (advancement != null && this.tabs.containsKey(advancement)) {
            if (this.selectedTab != null) {
                this.selectedTab.storeScroll();
            }
            this.selectedTab = this.tabs.get(advancement);
            savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            this.selectedTab.loadScroll();
            updateTabPage(bounds.maxTabs);
            return;
        }

        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            this.selectedTab = findTabById(savedSelectedTab);
            if (this.selectedTab == null) {
                this.selectedTab = this.tabs.values().iterator().next();
            }
            if (savedSelectedTab == null) {
                savedSelectedTab = this.selectedTab.getRootNode().holder().id();
            }
            this.selectedTab.loadScroll();
            updateTabPage(bounds.maxTabs);
        }
    }

    public EnhancedAdvancementWidget getAdvancementWidget(AdvancementNode advancement) {
        EnhancedAdvancementTab betterAdvancementTab = this.getTab(advancement);
        return betterAdvancementTab == null ? null : betterAdvancementTab.getWidget(advancement.holder());
    }

    private EnhancedAdvancementTab getTab(AdvancementNode advancement) {
        if (advancement == null) return this.selectedTab;

        AdvancementNode rootNode = advancement.root();
        EnhancedAdvancementTab tab = this.tabs.get(rootNode.holder());
        if (tab != null) {
            return tab;
        }

        for (AdvancementNode parent : IMultiParentNode.getParents(advancement)) {
            if (parent != null && parent != advancement) {
                AdvancementNode pRoot = parent.root();
                EnhancedAdvancementTab pTab = this.tabs.get(pRoot.holder());
                if (pTab != null) {
                    return pTab;
                }
            }
        }

        for (EnhancedAdvancementTab t : this.tabs.values()) {
            if (t.getWidget(advancement.holder().id()) != null) {
                return t;
            }
        }

        Identifier advId = advancement.holder().id();
        for (EnhancedAdvancementTab t : this.tabs.values()) {
            Identifier tabRootId = t.getRootNode().holder().id();
            if (advId.getNamespace().equals(tabRootId.getNamespace())) {
                String advPath = advId.getPath();
                String rootPath = tabRootId.getPath();
                int advSlash = advPath.indexOf('/');
                int rootSlash = rootPath.indexOf('/');
                if (advSlash != -1 && rootSlash != -1 && advPath.substring(0, advSlash).equals(rootPath.substring(0, rootSlash))) {
                    return t;
                }
            }
        }

        EnhancedAdvancementTab savedTab = findTabById(savedSelectedTab);
        if (savedTab != null) {
            return savedTab;
        }
        if (this.selectedTab != null) {
            return this.selectedTab;
        }
        return !this.tabs.isEmpty() ? this.tabs.values().iterator().next() : null;
    }

    public record TabBounds(int left, int top, int right, int bottom, int width, int height, int maxTabs) {
    }
}
