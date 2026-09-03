package com.evandev.reliable_advancements.gui.screens;

import com.evandev.reliable_advancements.advancements.IAdvancementSyncListener;
import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.client.ClientRewardTracker;
import com.evandev.reliable_advancements.client.ClientTabStore;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.AdvancementContextMenu;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementTab;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementTabType;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementWidget;
import com.evandev.reliable_advancements.network.*;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.reference.Resources;
import com.evandev.reliable_advancements.tabs.ResolvedTab;
import com.evandev.reliable_advancements.tabs.TabDefinition;
import com.evandev.reliable_advancements.tabs.TabResolver;
import com.evandev.reliable_advancements.util.PersistentData;
import com.evandev.reliable_advancements.util.RenderUtil;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.advancements.*;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public static ResourceLocation clipboardId = null;
    public static boolean clientHasFullTree = false;
    public static float savedZoom = 1.0F;
    private static int tabPage, maxPages;
    private static @Nullable EnhancedAdvancementsScreen active = null;
    private static ResourceLocation savedSelectedTab = null;
    private static String lastPushedPresentation = "";
    private static int nextSyncToken = 0;
    private final ClientAdvancements clientAdvancements;
    private final Screen parent;
    private final Map<ResourceLocation, EnhancedAdvancementTab> tabs = Maps.newLinkedHashMap();
    private final Map<ResourceLocation, AdvancementProgress> progressCache = Maps.newHashMap();
    public EnhancedAdvancementWidget linkingWidget = null;
    public EnhancedAdvancementTab selectedTab;
    public int internalWidth;
    protected int internalHeight;
    private boolean tabsDirty = true;
    private int syncRequested = 0;
    private int syncCompleted = 0;
    private Button prevPageBtn;
    private Button nextPageBtn;
    private Button editModeBtn;
    private float zoom;
    private boolean isScrolling;
    private EnhancedAdvancementWidget advConnectedToMouse = null;
    private AdvancementContextMenu contextMenu = null;
    private double dragOffsetX = 0.0;
    private double dragOffsetY = 0.0;
    private String linkingError = null;
    private long linkingErrorTime = 0;
    private @Nullable ResourceLocation announcedTab = null;

    public EnhancedAdvancementsScreen(ClientAdvancements clientAdvancements) {
        this(clientAdvancements, null);
    }

    public EnhancedAdvancementsScreen(ClientAdvancements clientAdvancements, Screen parent) {
        super(GameNarrator.NO_TITLE);
        this.clientAdvancements = clientAdvancements;
        this.parent = parent;

        PersistentData.load();
        this.zoom = savedZoom = Mth.clamp(PersistentData.getZoom(), MIN_ZOOM, MAX_ZOOM);
    }

    public static void resetSession() {
        clientHasFullTree = false;
        savedSelectedTab = null;
        lastPushedPresentation = "";
        nextSyncToken = 0;
        active = null;
        EnhancedAdvancementTab.scrollHistory.clear();
        ClientTabStore.clear();
    }

    public static void setSavedSelectedTab(ResourceLocation id) {
        savedSelectedTab = id;
    }

    public static boolean canEdit() {
        return ModConfig.get().enableEditMode && Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2);
    }

    private static int tabSortOrdinal(ResourceLocation tabId) {
        List<String> order = ModConfig.get().tabSortOrder;
        if (order == null || order.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        int index = order.indexOf(tabId.toString());
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public static @Nullable EnhancedAdvancementsScreen active() {
        return active;
    }

    public void awaitServerSync() {
        this.advConnectedToMouse = null;
        this.linkingWidget = null;
        this.contextMenu = null;
        selectedWidgets.clear();

        int token = this.syncRequested = ++nextSyncToken;
        if (!Services.PLATFORM.sendSyncRequest(new RequestSyncPayload(token))) {
            this.syncCompleted = token;
        }
    }

    public void onServerSyncComplete(int token) {
        if (token > this.syncCompleted && token <= this.syncRequested) {
            this.syncCompleted = token;
        }
    }

    public boolean isLoading() {
        return this.syncCompleted < this.syncRequested;
    }

    @Override
    public void onAdvancementSyncComplete() {
        this.finalizeResync();
    }

    public void finalizeResync() {
        rebuildTabs();
        rebuildNavigationWidgets();
        rebuildEditModeButton();
    }

    public void markTabsDirty() {
        this.tabsDirty = true;
    }

    public void onTabsSynced() {
        this.tabsDirty = true;
    }

    public void rebuildTabs() {
        this.tabsDirty = false;

        ResourceLocation draggingId = this.advConnectedToMouse != null
                ? this.advConnectedToMouse.getAdvancement().holder().id() : null;
        Set<ResourceLocation> selectedIds = new LinkedHashSet<>();
        for (EnhancedAdvancementWidget w : selectedWidgets) {
            selectedIds.add(w.getAdvancement().holder().id());
        }

        if (this.selectedTab != null) this.selectedTab.storeScroll();

        List<ResolvedTab> resolved = ClientTabStore.resolve(this.clientAdvancements.getTree());
        if (!canEdit()) {
            resolved.removeIf(tab -> tab.roots().isEmpty());
        }
        resolved.sort(tabOrder());

        Map<ResourceLocation, EnhancedAdvancementTab> rebuilt = Maps.newLinkedHashMap();
        int tabW = getTabInternalWidth();
        int tabH = getTabInternalHeight();

        int index = 0;
        for (ResolvedTab definition : resolved) {
            EnhancedAdvancementTab tab = EnhancedAdvancementTab.create(
                    this.minecraft, this, index, definition, tabW - 2 * SIDE, tabH - TOP - SIDE);
            if (tab == null) continue;
            rebuilt.put(definition.id(), tab);
            index++;
        }

        this.tabs.clear();
        this.tabs.putAll(rebuilt);

        pushTabPresentation(resolved);
        populateWidgets(resolved);
        reselectTab();

        this.advConnectedToMouse = draggingId != null ? findWidgetById(draggingId) : null;
        selectedWidgets.clear();
        for (ResourceLocation id : selectedIds) {
            EnhancedAdvancementWidget w = findWidgetById(id);
            if (w != null) selectedWidgets.add(w);
        }
    }

    private void pushTabPresentation(List<ResolvedTab> resolved) {
        if (!Services.PLATFORM.canSendAdvancementEdit()) return;

        JsonObject json = new JsonObject();
        for (ResolvedTab tab : resolved) {
            if (tab.definition() != null) continue;

            DisplayInfo source = null;
            for (AdvancementNode root : tab.roots()) {
                if (root.holder().id().equals(tab.id()) && root.advancement().display().isPresent()) {
                    source = root.advancement().display().get();
                    break;
                }
            }
            if (source == null) {
                for (AdvancementNode root : tab.roots()) {
                    if (TabResolver.declaresTab(root)) {
                        source = root.advancement().display().get();
                        break;
                    }
                }
            }
            if (source == null) {
                for (AdvancementNode root : tab.roots()) {
                    if (root.advancement().display().isPresent()) {
                        source = root.advancement().display().get();
                        break;
                    }
                }
            }
            if (source == null) continue;

            JsonObject entry = new JsonObject();
            entry.addProperty("title", source.getTitle().getString());
            ResourceLocation icon = BuiltInRegistries.ITEM.getKey(source.getIcon().getItem());
            entry.addProperty("icon", icon.toString());
            source.getBackground().ifPresent(bg -> entry.addProperty("background", bg.toString()));
            json.add(tab.id().toString(), entry);
        }
        if (json.isEmpty()) return;

        String payload = json.toString();
        if (payload.equals(lastPushedPresentation)) return;
        lastPushedPresentation = payload;

        Services.PLATFORM.sendTabAction(new TabActionPayload(
                TabActionPayload.Action.CACHE_PRESENTATION,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "presentation"),
                payload
        ));
    }

    private void populateWidgets(List<ResolvedTab> resolved) {
        Map<ResourceLocation, ResourceLocation> owners =
                TabResolver.advancementToTab(this.clientAdvancements.getTree(), resolved);


        List<AdvancementNode> nodes = new ArrayList<>(this.clientAdvancements.getTree().nodes());
        nodes.sort(Comparator.comparing(node -> node.holder().id()));

        for (AdvancementNode node : nodes) {
            if (node.advancement().display().isEmpty()) continue;

            ResourceLocation tabId = owners.get(node.holder().id());
            EnhancedAdvancementTab tab = tabId == null ? null : this.tabs.get(tabId);
            if (tab != null) {
                tab.addAdvancement(node);
            }
        }

        for (EnhancedAdvancementTab tab : this.tabs.values()) {
            tab.linkWidgets();
            tab.loadScroll();
            for (Map.Entry<ResourceLocation, EnhancedAdvancementWidget> entry : tab.getWidgets().entrySet()) {
                AdvancementProgress progress = this.progressCache.get(entry.getKey());
                if (progress != null) {
                    entry.getValue().getAdvancementProgress(progress);
                }
            }
        }
    }

    private void reselectTab() {
        EnhancedAdvancementTab remembered = this.tabs.get(savedSelectedTab);
        this.selectedTab = remembered != null || this.tabs.isEmpty()
                ? remembered
                : this.tabs.values().iterator().next();

        if (this.selectedTab == null) return;

        this.selectedTab.loadScroll();
        announceTab(this.selectedTab);
    }

    private void announceTab(@Nullable EnhancedAdvancementTab tab) {
        AdvancementNode primaryRoot = tab == null ? null : tab.getPrimaryRoot();
        if (primaryRoot == null) return;

        ResourceLocation id = primaryRoot.holder().id();
        boolean changed = !id.equals(this.announcedTab);
        this.announcedTab = id;
        this.clientAdvancements.setSelectedTab(primaryRoot.holder(), changed);
    }

    private Comparator<ResolvedTab> tabOrder() {
        return Comparator.comparingInt((ResolvedTab tab) -> tabSortOrdinal(tab.id()))
                .thenComparingInt(ResolvedTab::index)
                .thenComparing(tab -> ModConfig.get().orderTabsAlphabetically ? tab.title().getString() : "")
                .thenComparing(tab -> tab.id().toString());
    }

    private @Nullable EnhancedAdvancementWidget findWidgetById(ResourceLocation id) {
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

    public JsonObject createDefaultAdvancementJson(String titleText, String descText, @Nullable String background, @Nullable ResourceLocation parent) {
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

    public AdvancementEditorScreen createEditorScreen(ResourceLocation id, int posX, int posY, JsonObject rootJson) {
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

        if (ModConfig.get().showEditModeButton && this.minecraft.player != null && this.minecraft.player.hasPermissions(2)) {
            int editBtnWidth = 80;
            editModeBtn = Button.builder(Component.literal("Edit: " + (ModConfig.get().enableEditMode ? "ON" : "OFF")), b -> {
                ModConfig.get().enableEditMode = !ModConfig.get().enableEditMode;
                if (this.selectedTab != null) this.selectedTab.storeScroll();
                if (ModConfig.get().enableEditMode) {
                    clientHasFullTree = true;
                    Services.PLATFORM.sendRequestFullTree();
                } else {
                    clientHasFullTree = false;
                    Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(
                            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "resync"), "Resync"));
                }
                this.awaitServerSync();
                b.setMessage(Component.literal("Edit: " + (ModConfig.get().enableEditMode ? "ON" : "OFF")));
            }).pos(this.width - editBtnWidth - 30, 10).size(editBtnWidth, 20).build();
            addRenderableWidget(editModeBtn);
        }
    }

    public void centerOnAdvancement(ResourceLocation id) {
        AdvancementNode node = this.clientAdvancements.getTree().get(id);
        if (node == null) return;

        EnhancedAdvancementTab targetTab = findTabContaining(id);
        if (targetTab != null) {
            this.selectedTab = targetTab;
            savedSelectedTab = targetTab.getId();
            announceTab(targetTab);
            EnhancedAdvancementWidget widget = targetTab.getWidget(id);

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

    public Map<ResourceLocation, EnhancedAdvancementTab> getTabs() {
        return this.tabs;
    }

    public void editTabProperties(@Nullable EnhancedAdvancementTab tab) {
        if (tab != null) {
            this.minecraft.setScreen(new TabEditorScreen(this, tab));
        }
        this.contextMenu = null;
    }

    public @Nullable EnhancedAdvancementTab findTabContaining(ResourceLocation id) {
        for (EnhancedAdvancementTab tab : this.tabs.values()) {
            if (tab.getWidget(id) != null) return tab;
        }
        return null;
    }

    public void savePositions(ResourceLocation tabId, Map<ResourceLocation, int[]> positions) {
        if (tabId == null || positions.isEmpty() || !canEdit()) return;

        JsonObject json = new JsonObject();
        for (Map.Entry<ResourceLocation, int[]> entry : positions.entrySet()) {
            JsonArray pair = new JsonArray();
            pair.add(entry.getValue()[0]);
            pair.add(entry.getValue()[1]);
            json.add(entry.getKey().toString(), pair);
        }
        Services.PLATFORM.sendTabAction(new TabActionPayload(
                TabActionPayload.Action.SET_POSITIONS, tabId, json.toString()));
    }

    private void saveSelectionPositions(Collection<EnhancedAdvancementWidget> widgets) {
        if (this.selectedTab == null) return;
        Map<ResourceLocation, int[]> positions = new LinkedHashMap<>();
        for (EnhancedAdvancementWidget w : widgets) {
            positions.put(w.getAdvancement().holder().id(), new int[]{w.getX(), w.getY()});
        }
        savePositions(this.selectedTab.getId(), positions);
    }

    public int getTabInternalWidth() {
        int custom = selectedTab != null ? selectedTab.getDefinition().windowWidth() : 0;
        return custom > 0 ? custom : Math.min(this.internalWidth, 500);
    }

    public int getTabInternalHeight() {
        int custom = selectedTab != null ? selectedTab.getDefinition().windowHeight() : 0;
        return custom > 0 ? custom : Math.min(this.internalHeight, 350);
    }

    public void closeContextMenu() {
        this.contextMenu = null;
    }

    public void unlinkAllParents(EnhancedAdvancementWidget widget) {
        if (widget == null) return;
        ResourceLocation childId = widget.getAdvancement().holder().id();

        for (EnhancedAdvancementWidget parentWidget : new ArrayList<>(widget.getParents())) {
            Services.PLATFORM.sendLinkAdvancement(
                    new LinkAdvancementPayload(childId, parentWidget.getAdvancement().holder().id(), true));
            widget.unlink(parentWidget);
        }
        IMultiParentAdvancement.setParents(widget.getAdvancement().advancement(), List.of());
        this.awaitServerSync();
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

    public void deleteAdvancements(Collection<EnhancedAdvancementWidget> widgets) {
        confirmBatch(widgets, AdvancementBatchPayload.Op.DELETE, "delete");
    }

    public void resetAdvancements(Collection<EnhancedAdvancementWidget> widgets) {
        confirmBatch(widgets, AdvancementBatchPayload.Op.RESET_TO_VANILLA, "reset_advancement");
    }

    private void confirmBatch(Collection<EnhancedAdvancementWidget> widgets, AdvancementBatchPayload.Op op, String key) {
        if (widgets == null || widgets.isEmpty()) return;

        List<ResourceLocation> ids = new ArrayList<>();
        for (EnhancedAdvancementWidget widget : widgets) {
            ids.add(widget.getAdvancement().holder().id());
        }

        boolean single = ids.size() == 1;
        String suffix = single ? "" : "_multiple";
        Component title = single
                ? Component.translatable("gui.reliable_advancements.dialog." + key + ".title")
                : Component.translatable("gui.reliable_advancements.dialog." + key + suffix + ".title", ids.size());
        Component message = single
                ? Component.translatable("gui.reliable_advancements.dialog." + key + ".message")
                : Component.translatable("gui.reliable_advancements.dialog." + key + suffix + ".message", ids.size());

        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        if (this.selectedTab != null) this.selectedTab.storeScroll();
                        Services.PLATFORM.sendAdvancementBatch(new AdvancementBatchPayload(op, ids));
                        this.awaitServerSync();
                    }
                    this.minecraft.setScreen(this);
                },
                title,
                message
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

            boolean exists = this.clientAdvancements.getTree().get(testId) != null;

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

        AdvancementNode pasteParent = this.selectedTab != null ? this.selectedTab.getPrimaryRoot() : null;
        if (pasteParent != null) {
            root.addProperty("parent", pasteParent.holder().id().toString());
        } else {
            root.remove("parent");
        }
        if (this.selectedTab != null) {
            savePositions(this.selectedTab.getId(), Map.of(newId, new int[]{newPosX, newPosY}));
        }

        EditAdvancementPayload payload = new EditAdvancementPayload(newId, root.toString(), false);
        if (Services.PLATFORM.canSendAdvancementEdit()) {
            Services.PLATFORM.sendAdvancementEdit(payload);
        }
        if (pasteParent == null && this.selectedTab != null) {
            Services.PLATFORM.sendTabAction(TabActionPayload.addRoot(this.selectedTab.getId(), newId));
        }
        this.awaitServerSync();
    }

    public void createNewAdvancement(int mouseX, int mouseY) {
        ResourceLocation newId;
        int counter = 1;

        while (true) {
            String suffix = counter == 1 ? "" : "_" + counter;
            ResourceLocation testId = ResourceLocation.fromNamespaceAndPath("minecraft", "new_advancement" + suffix);

            boolean exists = this.clientAdvancements.getTree().get(testId) != null;

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

        AdvancementNode newParent = this.selectedTab != null ? this.selectedTab.getPrimaryRoot() : null;
        JsonObject root = createDefaultAdvancementJson("New Advancement", "Description", null,
                newParent != null ? newParent.holder().id() : null);

        AdvancementEditorScreen editor = createEditorScreen(newId, newPosX, newPosY, root);
        Minecraft.getInstance().setScreen(editor);
        this.contextMenu = null;
    }

    @Override
    protected void init() {
        active = this;
        this.clearWidgets();
        this.editModeBtn = null;
        this.prevPageBtn = null;
        this.nextPageBtn = null;
        this.advConnectedToMouse = null;
        this.linkingWidget = null;
        this.contextMenu = null;
        selectedWidgets.clear();

        if (this.selectedTab != null) this.selectedTab.storeScroll();
        PersistentData.load();
        if (savedSelectedTab == null) {
            savedSelectedTab = PersistentData.getLastTab();
        }
        this.internalHeight = this.height * ModConfig.get().uiScaling / 100;
        this.internalWidth = this.width * ModConfig.get().uiScaling / 100;
        this.clientAdvancements.setListener(this);

        if (EnhancedAdvancementsScreen.canEdit()) {
            PersistentData.migrateLegacyLayoutToServer();
        }

        if (EnhancedAdvancementsScreen.canEdit() && !clientHasFullTree) {
            Services.PLATFORM.sendRequestFullTree();
            clientHasFullTree = true;
            this.awaitServerSync();
        } else if (!isLoading()) {
            rebuildTabs();
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isLoading()) return true;

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
                TabBounds bounds = getTabBounds();
                if (mouseX >= bounds.left && mouseX <= bounds.right && mouseY >= bounds.top && mouseY <= bounds.bottom) {
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

        TabBounds bounds = getTabBounds();
        int left = bounds.left;
        int top = bounds.top;
        int width = bounds.width;
        int height = bounds.height;

        if (this.linkingWidget != null && button == 0) {
            EnhancedAdvancementWidget target = getHoveredWidget(mouseX, mouseY);
            if (target != null && target != this.linkingWidget) {
                if (this.selectedTab != null) this.selectedTab.storeScroll();

                ResourceLocation id = linkingWidget.getAdvancement().holder().id();
                ResourceLocation parentResId = target.getAdvancement().holder().id();

                if (linkingWidget.getParents().contains(target)) {
                    LinkAdvancementPayload payload = new LinkAdvancementPayload(id, parentResId, true);
                    Services.PLATFORM.sendLinkAdvancement(payload);

                    linkingWidget.unlink(target);
                } else {
                    if (isDescendant(this.linkingWidget, target)) {
                        this.linkingError = Component.translatable("gui.reliable_advancements.linking.error.cycle").getString();
                        this.linkingErrorTime = Util.getMillis() + 3000;
                        this.linkingWidget = null;
                        return true;
                    }

                    LinkAdvancementPayload payload = new LinkAdvancementPayload(id, parentResId, false);
                    Services.PLATFORM.sendLinkAdvancement(payload);

                    linkingWidget.link(target);
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

            EnhancedAdvancementTab clickedTab = tabAt(left, top, width, height, mouseX, mouseY);
            if (clickedTab != null) selectTab(clickedTab);
        } else if (button == 1 && EnhancedAdvancementsScreen.canEdit()) {
            EnhancedAdvancementTab clickedTab = tabAt(left, top, width, height, mouseX, mouseY);
            if (clickedTab != null) {
                selectTab(clickedTab);
                this.contextMenu = new AdvancementContextMenu(this, clickedTab, (int) mouseX, (int) mouseY);
                return true;
            }

            boolean inGui = mouseX < left + internalWidth - 2 * SIDE - PADDING && mouseX > left + PADDING && mouseY < top + internalHeight - TOP + 1 && mouseY > top + 2 * PADDING;

            if (inGui) {
                EnhancedAdvancementWidget hoveredWidget = getHoveredWidget(mouseX, mouseY);
                if (hoveredWidget != null && !selectedWidgets.contains(hoveredWidget)) {
                    selectedWidgets.clear();
                    selectedWidgets.add(hoveredWidget);
                }
                this.contextMenu = new AdvancementContextMenu(this, hoveredWidget, (int) mouseX, (int) mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private @Nullable EnhancedAdvancementTab tabAt(int left, int top, int width, int height, double mouseX, double mouseY) {
        int maxTabs = EnhancedAdvancementTabType.getMaxTabs(width, height);
        int skip = tabPage * maxTabs;

        for (EnhancedAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
            if (tab.isMouseOver(left, top, width, height, mouseX, mouseY)) return tab;
        }
        return null;
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
        if (isLoading()) return true;
        if (this.contextMenu != null) this.contextMenu = null;
        if (this.selectedTab != null) {
            if (Screen.hasControlDown()) {
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
            } else if (Screen.hasShiftDown()) {
                this.selectedTab.scroll(scrollY * 20.0, 0, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
            } else {
                this.selectedTab.scroll(scrollX * 20.0, scrollY * 20.0, getTabInternalWidth() - 2 * SIDE - 3 * PADDING, getTabInternalHeight() - TOP - BOTTOM - 3 * PADDING);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isLoading()) {
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
                int shift = Screen.hasShiftDown() ? 4 : 1;
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
                    }
                    saveSelectionPositions(selectedWidgets);
                    return true;
                }
            }

            if (Screen.hasControlDown() && keyCode == 67) {
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.width / (double) this.minecraft.getWindow().getScreenWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.height / (double) this.minecraft.getWindow().getScreenHeight();
                EnhancedAdvancementWidget target = selectedWidgets.size() == 1 ? selectedWidgets.iterator().next() : null;
                if (target == null) target = getHoveredWidget(mouseX, mouseY);
                if (target != null) {
                    copyAdvancement(target);
                }
                return true;
            } else if (Screen.hasControlDown() && keyCode == 86) {
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.width / (double) this.minecraft.getWindow().getScreenWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.height / (double) this.minecraft.getWindow().getScreenHeight();
                pasteAdvancement((int) mouseX, (int) mouseY);
                return true;
            } else if (keyCode == 261 || keyCode == 259) {
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.width / (double) this.minecraft.getWindow().getScreenWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.height / (double) this.minecraft.getWindow().getScreenHeight();
                if (!selectedWidgets.isEmpty()) {
                    deleteAdvancements(selectedWidgets);
                    return true;
                }
                EnhancedAdvancementWidget target = getHoveredWidget(mouseX, mouseY);
                if (target != null) {
                    deleteAdvancements(List.of(target));
                    return true;
                }
            }
        }

        if (this.minecraft.options.keyAdvancements.matches(keyCode, scanCode) || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isLoading()) return true;
        if (this.advConnectedToMouse != null) {
            Collection<EnhancedAdvancementWidget> moved = selectedWidgets.contains(this.advConnectedToMouse)
                    ? new ArrayList<>(selectedWidgets)
                    : List.of(this.advConnectedToMouse);
            for (EnhancedAdvancementWidget w : moved) {
                Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(w);
            }
            saveSelectionPositions(moved);

            this.advConnectedToMouse = null;
            if (this.selectedTab != null) {
                this.selectedTab.recalculateBounds();
            }
        }
        this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
            savedSelectedTab = this.selectedTab.getId();
        }
        PersistentData.setLastTab(savedSelectedTab);
        PersistentData.setZoom(this.zoom);
        PersistentData.save();
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double mouseDeltaX, double mouseDeltaY) {
        if (isLoading()) return true;
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
        if (this.tabsDirty && !isLoading()) {
            rebuildTabs();
            rebuildNavigationWidgets();
        }
        TabBounds bounds = getTabBounds();
        int tabW = getTabInternalWidth();
        int left = bounds.left;
        int top = bounds.top;
        int right = bounds.right;
        int bottom = bounds.bottom;
        int maxTabs = bounds.maxTabs;
        int skip = tabPage * maxTabs;

        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        if (maxPages != 0) {
            Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
            int textWidth = this.font.width(page);
            guiGraphics.drawString(this.font, page.getVisualOrderText(), left + (tabW - textWidth) / 2 - textWidth, bottom + 8, -1);
        }

        this.renderInside(guiGraphics, mouseX, mouseY, left, top, right, bottom);

        if (!isLoading()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING, 0);
            guiGraphics.pose().scale(zoom, zoom, 1.0F);

            if (EnhancedAdvancementsScreen.canEdit() && this.selectedTab != null) {
                if (this.advConnectedToMouse != null) {
                    Set<EnhancedAdvancementWidget> draggingWidgets = selectedWidgets.contains(this.advConnectedToMouse) ? selectedWidgets : Set.of(this.advConnectedToMouse);
                    for (EnhancedAdvancementWidget w : draggingWidgets) {
                        int ax = w.getX() + this.selectedTab.scrollX;
                        int ay = w.getY() + this.selectedTab.scrollY;
                        guiGraphics.renderOutline(ax + 2, ay - 1, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 2, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 2, 0xFF00FF00);
                        guiGraphics.renderOutline(ax + 1, ay - 2, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 4, EnhancedAdvancementWidget.ADVANCEMENT_SIZE + 4, 0xFF00FF00);
                    }
                }
            }
            guiGraphics.pose().popPose();

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

                RenderSystem.disableDepthTest();
                RenderUtil.line(guiGraphics, startX, startY, mouseX, mouseY, 1, lineColor);
                RenderSystem.enableDepthTest();

                guiGraphics.drawString(this.font, promptText, mouseX + 15, mouseY + 10, lineColor);
            }
        }

        this.renderWindow(guiGraphics, left, top, right, bottom, maxTabs, skip);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (isLoading()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 400.0D);
            RenderSystem.disableDepthTest();

            int boxLeft = left + PADDING;
            int boxTop = top + 2 * PADDING;
            int boxRight = right - PADDING;
            int boxBottom = bottom - PADDING;
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xEE0B0F19);

            int cardW = 190, cardH = 46;
            int cardX = boxLeft + (boxRight - boxLeft - cardW) / 2;
            int cardY = boxTop + (boxBottom - boxTop - cardH) / 2;
            guiGraphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xF0161B22);
            guiGraphics.renderOutline(cardX, cardY, cardW, cardH, 0xFF30363D);

            String dots = ".".repeat((int) ((Util.getMillis() / 350) % 4));
            String msg = "Loading Advancements" + dots;
            guiGraphics.drawString(this.font, msg, cardX + (cardW - this.font.width(msg)) / 2, cardY + (cardH - this.font.lineHeight) / 2, 0xFFE6EDF3, false);

            RenderSystem.enableDepthTest();
            guiGraphics.pose().popPose();
        } else {
            if (this.advConnectedToMouse == null && this.contextMenu == null) {
                this.renderToolTips(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
            }

            if (this.advConnectedToMouse != null && this.selectedTab != null) {
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
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2, 1, 0x00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0x00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                            } else {
                                RenderUtil.drawRect(guiGraphics, x1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1, 1, 0x00FF00);
                                RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2, 1, 0x00FF00);
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
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);

                            RenderUtil.drawRect(guiGraphics, x2, y2, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + EnhancedAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
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

            int boxLeft = left + PADDING;
            int boxTop = top + 2 * PADDING;
            int boxRight = right - PADDING;
            int boxBottom = bottom - PADDING;

            if (ModConfig.get().showDebugCoordinates && this.selectedTab != null) {
                if (this.advConnectedToMouse != null) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 500.0F);
                    RenderSystem.disableDepthTest();
                    int currentX = (int) ((this.advConnectedToMouse.getX() + this.selectedTab.scrollX + 4) * zoom) + boxLeft;
                    int currentY = (int) ((this.advConnectedToMouse.getY() + this.selectedTab.scrollY) * zoom) + boxTop - font.lineHeight + 1;
                    guiGraphics.drawString(font, this.advConnectedToMouse.getX() + "," + this.advConnectedToMouse.getY(), currentX, currentY, 0xFFFFFF);
                    RenderSystem.enableDepthTest();
                    guiGraphics.pose().popPose();
                } else if (mouseX >= boxLeft && mouseX <= boxRight && mouseY >= boxTop && mouseY <= boxBottom) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 500.0F);
                    RenderSystem.disableDepthTest();
                    int xMouse = (int) ((mouseX - boxLeft) / zoom);
                    int yMouse = (int) ((mouseY - boxTop) / zoom);
                    int currentX = xMouse - this.selectedTab.scrollX - 4;
                    int currentY = yMouse - this.selectedTab.scrollY - 1;
                    guiGraphics.drawString(font, currentX + "," + currentY, mouseX, mouseY - font.lineHeight, 0xFFFFFF);
                    RenderSystem.enableDepthTest();
                    guiGraphics.pose().popPose();
                }
            }

            if (this.contextMenu != null) {
                this.contextMenu.render(guiGraphics, mouseX, mouseY, partialTicks);
            }

            if (this.linkingError != null && Util.getMillis() < this.linkingErrorTime) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 600.0F);
                RenderSystem.disableDepthTest();
                int errW = this.font.width(this.linkingError);
                guiGraphics.fill(mouseX + 10, mouseY - 15, mouseX + 16 + errW, mouseY + 1, 0xDD000000);
                guiGraphics.renderOutline(mouseX + 10, mouseY - 15, errW + 6, 16, 0xFFFF5555);
                guiGraphics.drawString(this.font, this.linkingError, mouseX + 13, mouseY - 11, 0xFF5555);
                RenderSystem.enableDepthTest();
                guiGraphics.pose().popPose();
            }
        }
    }

    private void renderInside(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        int boxLeft = left + PADDING;
        int boxTop = top + 2 * PADDING;
        int boxRight = right - PADDING;
        int boxBottom = bottom - PADDING;

        int width = boxRight - boxLeft;
        int height = boxBottom - boxTop;

        if (isLoading()) {
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xFF0E131F);
            return;
        }

        EnhancedAdvancementTab betterAdvancementTab = this.selectedTab;
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
        if (this.selectedTab != null) {
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

    public void createNewTab() {
        ResourceLocation newId;
        int counter = 1;
        while (true) {
            String suffix = counter == 1 ? "" : "_" + counter;
            ResourceLocation testId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "new_tab" + suffix);
            if (!this.tabs.containsKey(testId) && ClientTabStore.get().tab(testId) == null) {
                newId = testId;
                break;
            }
            counter++;
        }

        TabDefinition definition = new TabDefinition(newId);
        definition.title = "New Tab";
        definition.background = TabResolver.DEFAULT_BACKGROUND;

        Services.PLATFORM.sendTabAction(new TabActionPayload(
                TabActionPayload.Action.SAVE, newId, definition.toJson().toString()));

        savedSelectedTab = newId;
        this.contextMenu = null;
        this.awaitServerSync();
    }

    public void deleteTab(EnhancedAdvancementTab tab) {
        if (tab == null) return;
        ResourceLocation tabId = tab.getId();
        int advancementCount = tab.getWidgets().size();

        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        savedSelectedTab = null;
                        Services.PLATFORM.sendTabAction(TabActionPayload.of(TabActionPayload.Action.DELETE, tabId));
                        this.awaitServerSync();
                    }
                    this.minecraft.setScreen(this);
                },
                Component.translatable("gui.reliable_advancements.dialog.delete_tab.title", tab.getTitle()),
                Component.translatable("gui.reliable_advancements.dialog.delete_tab.message", advancementCount)
        ));
        this.contextMenu = null;
    }

    public void resetTabToVanilla(EnhancedAdvancementTab tab) {
        if (tab == null) return;
        ResourceLocation tabId = tab.getId();

        this.minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        savedSelectedTab = tabId;
                        Services.PLATFORM.sendTabAction(TabActionPayload.of(TabActionPayload.Action.RESET_TO_VANILLA, tabId));
                        this.awaitServerSync();
                    }
                    this.minecraft.setScreen(this);
                },
                Component.translatable("gui.reliable_advancements.dialog.reset_tab.title"),
                Component.translatable("gui.reliable_advancements.dialog.reset_tab.message")
        ));
        this.contextMenu = null;
    }

    public void selectTab(EnhancedAdvancementTab tab) {
        if (tab == null || tab == this.selectedTab) return;
        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
        }
        this.selectedTab = tab;
        savedSelectedTab = tab.getId();
        tab.loadScroll();
        announceTab(tab);
        updateTabPage(getTabBounds().maxTabs);
    }

    @Override
    public void onAdvancementsCleared() {
        if (this.selectedTab != null) this.selectedTab.storeScroll();
        this.tabs.clear();
        this.progressCache.clear();
        this.selectedTab = null;
        this.tabsDirty = true;
    }

    @Override
    public void onAddAdvancementRoot(@NotNull AdvancementNode advancement) {
        this.tabsDirty = true;
    }

    @Override
    public void onRemoveAdvancementRoot(@NotNull AdvancementNode advancement) {
        this.tabsDirty = true;
    }

    @Override
    public void onAddAdvancementTask(@NotNull AdvancementNode advancement) {
        this.tabsDirty = true;
    }

    @Override
    public void onRemoveAdvancementTask(@NotNull AdvancementNode advancement) {
        this.progressCache.remove(advancement.holder().id());
        this.tabsDirty = true;
    }

    @Override
    public void onUpdateAdvancementProgress(@NotNull AdvancementNode advancement, @NotNull AdvancementProgress progress) {
        this.progressCache.put(advancement.holder().id(), progress);
        EnhancedAdvancementWidget widget = getAdvancementWidget(advancement);
        if (widget != null) {
            widget.getAdvancementProgress(progress);
        }
    }

    @Override
    public void onSelectedTabChanged(@Nullable AdvancementHolder advancement) {
        if (advancement == null) return;

        EnhancedAdvancementTab tab = findTabContaining(advancement.id());
        if (tab != null) {
            selectTab(tab);
        }
    }

    public EnhancedAdvancementWidget getAdvancementWidget(AdvancementNode advancement) {
        return findWidgetById(advancement.holder().id());
    }

    public record TabBounds(int left, int top, int right, int bottom, int width, int height, int maxTabs) {
    }
}
