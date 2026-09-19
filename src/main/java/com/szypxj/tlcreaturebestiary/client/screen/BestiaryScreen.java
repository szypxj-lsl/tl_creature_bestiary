package com.szypxj.tlcreaturebestiary.client.screen;

import com.szypxj.tlcreaturebestiary.client.BestiarySearchPolicy;
import com.szypxj.tlcreaturebestiary.client.ClientBestiaryState;
import com.szypxj.tlcreaturebestiary.client.DangerStarRenderer;
import com.szypxj.tlcreaturebestiary.danger.DangerRating;
import com.szypxj.tlcreaturebestiary.data.BestiaryEntryIndex;
import com.szypxj.tlcreaturebestiary.data.BestiaryEntryProgress;
import com.szypxj.tlcreaturebestiary.data.BestiaryInvestigationRules;
import com.szypxj.tlcreaturebestiary.data.BestiaryInvestigationService;
import com.szypxj.tlcreaturebestiary.info.DropInfo;
import com.szypxj.tlcreaturebestiary.info.EcologicalNiche;
import com.szypxj.tlcreaturebestiary.network.BestiaryNetwork;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.CreatureInfoApi;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingFoodInfo;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcEditBox;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.spyglass.SpyglassRadarBaseline;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class BestiaryScreen extends Screen {
    private static final int TARGET_WIDTH = 640;
    private static final int TARGET_HEIGHT = 360;
    private static final int MARGIN = 10;
    private static final int HEADER_HEIGHT = 28;
    private static final int LIST_WIDTH = 230;
    private static final int ROW_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int DROP_ROW_HEIGHT = 30;
    private static final int BIOME_ROW_HEIGHT = 24;
    private static final int LOCKED_TEXT_COLOR = 0xFF6E8AA1;

    private final Screen parent;
    private final List<BestiaryEntryRow> rows = new ArrayList<>();
    private List<ResourceLocation> allTypeIds = List.of();
    private TdmcEditBox searchBox;
    private boolean editMode;
    private TdmcButton editModeButton;
    private TdmcEditBox speciesEditBox;
    private ThemedMultiLineEditBox descriptionEditBox;
    private TdmcButton saveNotesButton;
    private ResourceLocation editorEntityTypeId;
    private ResourceLocation selectedId;
    private DetailTab activeTab = DetailTab.BASIC;
    private int scroll;
    private int detailScroll;
    private int basicScrollPixels;
    private int basicContentHeight;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private boolean draggingListScrollbar;
    private boolean draggingDetailScrollbar;
    private int scrollbarDragOffset;

    public BestiaryScreen(Screen parent) {
        super(Component.translatable("gui.tl_creature_bestiary.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(TARGET_WIDTH, Math.max(320, width - MARGIN * 2));
        panelHeight = Math.min(TARGET_HEIGHT, Math.max(220, height - MARGIN * 2));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        allTypeIds = BestiaryEntryIndex.allLivingTypeIds(ClientBestiaryState.unlocked());

        searchBox = new TdmcEditBox(
                font,
                panelX + 10,
                panelY + HEADER_HEIGHT + 8,
                listContentWidth(),
                20,
                Component.translatable("gui.tl_creature_bestiary.search")
        );
        searchBox.setResponder(value -> {
            scroll = 0;
            rebuildRows(value);
            onSelectionChanged();
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(TdmcButton.create(
                panelX + panelWidth - 82,
                panelY + 4,
                72,
                20,
                Component.translatable("gui.tl_creature_bestiary.back"),
                button -> onClose()
        ));
        addDetailTabs();
        rebuildRows("");
        addInvestigationEditor();
        requestSelectedDetailIfNeeded();
    }

    private void addDetailTabs() {
        int gap = 4;
        int width = detailWidth();
        int tabWidth = Math.max(54, (width - 20 - gap * 2) / 3);
        int x = detailX() + 10;
        int y = detailY() + 42;
        addRenderableWidget(TdmcButton.create(
                x,
                y,
                tabWidth,
                20,
                Component.translatable("gui.tl_creature_bestiary.tab.basic"),
                button -> setActiveTab(DetailTab.BASIC)
        ));
        addRenderableWidget(TdmcButton.create(
                x + tabWidth + gap,
                y,
                tabWidth,
                20,
                Component.translatable("gui.tl_creature_bestiary.tab.drops"),
                button -> setActiveTab(DetailTab.DROPS)
        ));
        addRenderableWidget(TdmcButton.create(
                x + (tabWidth + gap) * 2,
                y,
                tabWidth,
                20,
                Component.translatable("gui.tl_creature_bestiary.tab.biomes"),
                button -> setActiveTab(DetailTab.BIOMES)
        ));
    }

    private void setActiveTab(DetailTab tab) {
        if (editMode) {
            activeTab = DetailTab.BASIC;
            detailScroll = 0;
            return;
        }
        activeTab = tab == null ? DetailTab.BASIC : tab;
        detailScroll = 0;
        basicScrollPixels = 0;
        basicContentHeight = 0;
    }

    private void addInvestigationEditor() {
        editModeButton = TdmcButton.create(
                panelX + panelWidth - 160,
                panelY + 4,
                72,
                20,
                Component.translatable("gui.tl_creature_bestiary.investigation.edit"),
                button -> toggleEditMode()
        );
        addRenderableWidget(editModeButton);

        int editorX = detailX() + 10;
        int editorWidth = Math.max(80, detailWidth() - 20);
        int editorTop = detailContentY() + 40;
        speciesEditBox = new TdmcEditBox(
                font,
                editorX,
                editorTop,
                editorWidth,
                20,
                Component.translatable("gui.tl_creature_bestiary.investigation.species")
        );
        speciesEditBox.setMaxLength(BestiaryInvestigationService.MAX_SPECIES_LENGTH);
        addRenderableWidget(speciesEditBox);

        int descriptionY = editorTop + 42;
        int descriptionHeight = Math.max(58, detailY() + detailHeight() - descriptionY - 38);
        descriptionEditBox = new ThemedMultiLineEditBox(
                font,
                editorX,
                descriptionY,
                editorWidth,
                descriptionHeight,
                Component.translatable("gui.tl_creature_bestiary.investigation.description"),
                Component.translatable("gui.tl_creature_bestiary.investigation.description_placeholder")
        );
        descriptionEditBox.setCharacterLimit(BestiaryInvestigationService.MAX_DESCRIPTION_LENGTH);
        addRenderableWidget(descriptionEditBox);

        saveNotesButton = TdmcButton.create(
                detailX() + detailWidth() - 82,
                detailY() + detailHeight() - 28,
                72,
                20,
                Component.translatable("gui.tl_creature_bestiary.investigation.save"),
                button -> saveInvestigationNotes()
        );
        addRenderableWidget(saveNotesButton);
        updateEditorVisibility(false);
    }

    private void toggleEditMode() {
        if (!canEditSelected()) {
            editMode = false;
            updateEditorVisibility(false);
            return;
        }
        editMode = !editMode;
        activeTab = DetailTab.BASIC;
        detailScroll = 0;
        basicScrollPixels = 0;
        basicContentHeight = 0;
        updateEditorVisibility(editMode);
    }

    private void saveInvestigationNotes() {
        if (!editMode || !canEditSelected() || selectedId == null) {
            return;
        }
        BestiaryNetwork.saveNotes(selectedId, speciesEditBox.getValue(), descriptionEditBox.getValue());
    }

    private boolean canEditSelected() {
        if (selectedId == null || !ClientBestiaryState.isUnlocked(selectedId)) {
            return false;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(selectedId);
        return BestiaryInvestigationRules.isHighRisk(type)
                && ClientBestiaryState.progress(selectedId).observed();
    }

    private void updateEditorVisibility(boolean reloadValues) {
        boolean editable = canEditSelected();
        if (!editable) {
            editMode = false;
            editorEntityTypeId = null;
        }
        boolean showInputs = editable && editMode;
        if (editModeButton != null) {
            editModeButton.visible = editable;
            editModeButton.active = editable;
            editModeButton.setMessage(Component.translatable(editMode
                    ? "gui.tl_creature_bestiary.investigation.edit_exit"
                    : "gui.tl_creature_bestiary.investigation.edit"));
        }
        if (speciesEditBox != null) {
            speciesEditBox.visible = showInputs;
            speciesEditBox.active = showInputs;
        }
        if (descriptionEditBox != null) {
            descriptionEditBox.visible = showInputs;
            descriptionEditBox.active = showInputs;
        }
        if (saveNotesButton != null) {
            saveNotesButton.visible = showInputs;
            saveNotesButton.active = showInputs;
        }
        if (showInputs && (reloadValues || !selectedId.equals(editorEntityTypeId))) {
            BestiaryEntryProgress progress = ClientBestiaryState.progress(selectedId);
            speciesEditBox.setValue(progress.species());
            descriptionEditBox.setValue(progress.description());
            editorEntityTypeId = selectedId;
        }
    }

    private void onSelectionChanged() {
        detailScroll = 0;
        basicScrollPixels = 0;
        basicContentHeight = 0;
        editMode = false;
        editorEntityTypeId = null;
        updateEditorVisibility(false);
        requestSelectedDetailIfNeeded();
    }

    private void renderInvestigationEditorLabels(GuiGraphics graphics, BestiaryEntryProgress investigation) {
        int editorX = detailX() + 10;
        graphics.drawString(
                font,
                Component.translatable("gui.tl_creature_bestiary.investigation.species"),
                editorX,
                speciesEditBox.getY() - 11,
                investigation.validSpecies() ? TdmcUiTheme.TEXT_ACCENT : TdmcUiTheme.TEXT_MUTED,
                false
        );
        int descriptionLength = BestiaryEntryProgress.countNonWhitespace(descriptionEditBox.getValue());
        boolean descriptionComplete = descriptionLength >= BestiaryEntryProgress.MIN_DESCRIPTION_NON_WHITESPACE;
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.tl_creature_bestiary.investigation.description_requirement",
                        descriptionLength,
                        BestiaryEntryProgress.MIN_DESCRIPTION_NON_WHITESPACE
                ),
                editorX,
                descriptionEditBox.getY() - 11,
                descriptionComplete ? TdmcUiTheme.TEXT_ACCENT : TdmcUiTheme.TEXT_MUTED,
                false
        );
    }

    private void rebuildRows(String query) {
        rows.clear();
        for (ResourceLocation id : allTypeIds) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type == null) {
                continue;
            }
            if (hideUndiscoveredHighRiskFromSearch(query, id, type)) {
                continue;
            }
            Component label = displayLabelForRow(id, type);
            String searchable = label.getString() + " " + type.getDescriptionId();
            boolean unlocked = ClientBestiaryState.isUnlocked(id);
            if (BestiarySearchPolicy.matches(query, unlocked, searchable, id)) {
                rows.add(new BestiaryEntryRow(id, unlocked, label));
            }
        }
        rows.sort(Comparator
                .comparing(BestiaryEntryRow::unlocked).reversed()
                .thenComparing(row -> row.label().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(row -> row.entityTypeId().toString(), String.CASE_INSENSITIVE_ORDER));
        if (selectedId == null && !rows.isEmpty()) {
            selectedId = rows.get(0).entityTypeId();
        } else if (selectedId != null && rows.stream().noneMatch(row -> row.entityTypeId().equals(selectedId)) && !rows.isEmpty()) {
            selectedId = rows.get(0).entityTypeId();
        }
        clampListScroll();
    }


    private Component displayLabelForRow(ResourceLocation id, EntityType<?> type) {
        if (type != null
                && BestiaryInvestigationRules.isHighRisk(type)
                && !ClientBestiaryState.progress(id).observed()) {
            return Component.translatable("gui.tl_creature_bestiary.investigation.unknown");
        }
        return type == null
                ? Component.translatable("gui.tl_creature_bestiary.investigation.unknown")
                : type.getDescription();
    }

    private boolean hideUndiscoveredHighRiskFromSearch(String query, ResourceLocation id, EntityType<?> type) {
        return query != null
                && !query.trim().isEmpty()
                && type != null
                && BestiaryInvestigationRules.isHighRisk(type)
                && !ClientBestiaryState.progress(id).observed();
    }

    private void requestSelectedDetailIfNeeded() {
        if (selectedId == null || !ClientBestiaryState.isUnlocked(selectedId)) {
            return;
        }
        if (ClientBestiaryState.detail(selectedId) == null) {
            BestiaryNetwork.requestDetail(selectedId);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        TdmcUiTheme.fillPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        TdmcUiTheme.fillHeader(graphics, panelX + 2, panelY + 2, panelWidth - 4, HEADER_HEIGHT - 2);
        graphics.drawString(font, title, panelX + 10, panelY + 10, TdmcUiTheme.TEXT_PRIMARY, true);
        int total = allTypeIds.size();
        long unlockedVisible = allTypeIds.stream().filter(ClientBestiaryState::isUnlocked).count();
        graphics.drawString(
                font,
                Component.translatable("gui.tl_creature_bestiary.progress", unlockedVisible, total),
                panelX + 118,
                panelY + 10,
                TdmcUiTheme.TEXT_ACCENT,
                true
        );
        renderList(graphics, mouseX, mouseY);
        renderDetails(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSearchPlaceholder(graphics);
    }

    private void renderSearchPlaceholder(GuiGraphics graphics) {
        if (searchBox == null || searchBox.isFocused() || !searchBox.getValue().isEmpty()) {
            return;
        }
        graphics.drawString(
                font,
                Component.translatable("gui.tl_creature_bestiary.search"),
                searchBox.getX() + 6,
                searchBox.getY() + (searchBox.getHeight() - 8) / 2,
                TdmcUiTheme.TEXT_MUTED,
                false
        );
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        TdmcUiTheme.fillSection(graphics, listX(), listY(), listWidth(), listHeight());
        int visible = visibleRows();
        int end = Math.min(rows.size(), scroll + visible);
        for (int i = scroll; i < end; i++) {
            BestiaryEntryRow row = rows.get(i);
            int y = listY() + (i - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= listX() && mouseX < listX() + listContentWidth() && mouseY >= y && mouseY < y + ROW_HEIGHT;
            boolean selected = row.entityTypeId().equals(selectedId);
            int background = selected ? TdmcUiTheme.ROW_SELECTED : hovered ? TdmcUiTheme.ROW_HOVERED : TdmcUiTheme.ROW_BACKGROUND;
            graphics.fill(listX() + 1, y + 1, listX() + listContentWidth() - 1, y + ROW_HEIGHT - 1, background);
            graphics.drawString(
                    font,
                    row.label(),
                    listX() + 6,
                    y + 6,
                    row.unlocked() ? TdmcUiTheme.TEXT_PRIMARY : LOCKED_TEXT_COLOR,
                    true
            );
        }
        renderScrollbar(graphics, listScrollbarX(), listScrollbarY(), listScrollbarHeight(), visible, rows.size(), scroll);
    }

    private void renderDetails(GuiGraphics graphics) {
        TdmcUiTheme.fillSection(graphics, detailX(), detailY(), detailWidth(), detailHeight());
        if (selectedId == null) {
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(selectedId);
        if (type == null) {
            return;
        }
        boolean unlocked = ClientBestiaryState.isUnlocked(selectedId);
        boolean highRisk = BestiaryInvestigationRules.isHighRisk(type);
        BestiaryEntryProgress investigation = highRisk ? ClientBestiaryState.progress(selectedId) : null;
        Component displayName = highRisk && investigation != null && !investigation.observed()
                ? Component.translatable("gui.tl_creature_bestiary.unknown")
                : type.getDescription();
        graphics.drawString(font, displayName, detailX() + 10, detailY() + 10, unlocked ? TdmcUiTheme.TEXT_PRIMARY : LOCKED_TEXT_COLOR, true);
        BaseStats displayStats = CreatureInfoApi.getBaseStats(type);
        if (!highRisk) {
            DangerStarRenderer.draw(graphics, font, detailX() + 10, detailY() + 26, DangerRating.fromBaseStats(displayStats));
        } else if (investigation != null && investigation.combatRecorded()) {
            DangerStarRenderer.draw(graphics, font, detailX() + 10, detailY() + 26, BestiaryInvestigationRules.baseDangerStars(type));
        } else {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.investigation.danger_unknown"),
                    detailX() + 10,
                    detailY() + 26,
                    TdmcUiTheme.TEXT_MUTED,
                    false
            );
        }
        if (highRisk && investigation != null) {
            renderInvestigationHeader(graphics, investigation);
        }

        if (!unlocked) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.locked"),
                    detailX() + 10,
                    detailDataY(investigation) + 10,
                    LOCKED_TEXT_COLOR,
                    true
            );
            return;
        }
        if (editMode && investigation != null && canEditSelected()) {
            renderInvestigationEditorLabels(graphics, investigation);
            return;
        }
        ClientBestiaryState.Detail detail = ClientBestiaryState.detail(selectedId);
        if (detail == null) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.loading"),
                    detailX() + 10,
                    detailDataY(investigation) + 10,
                    TdmcUiTheme.TEXT_MUTED,
                    true
            );
            return;
        }
        switch (activeTab) {
            case BASIC -> renderBasicTab(graphics, detail, investigation);
            case DROPS -> renderDropsTab(graphics, detail, investigation);
            case BIOMES -> renderBiomesTab(graphics, detail, investigation);
        }
    }

    private void renderInvestigationHeader(GuiGraphics graphics, BestiaryEntryProgress investigation) {
        Component status = Component.translatable(investigation.rewardClaimed()
                ? "gui.tl_creature_bestiary.investigation.status.reward_claimed"
                : investigation.completed()
                ? "gui.tl_creature_bestiary.investigation.status.reward_pending"
                : "gui.tl_creature_bestiary.investigation.status.in_progress");
        Component summary = Component.translatable(
                "gui.tl_creature_bestiary.investigation.summary",
                investigation.completionPercent(),
                status
        );
        graphics.drawString(
                font,
                summary,
                detailX() + 10,
                detailContentY() + 1,
                TdmcUiTheme.TEXT_ACCENT,
                true
        );

        boolean[] completed = {
                investigation.observed(),
                investigation.combatRecorded(),
                investigation.tamingRecorded(),
                investigation.validSpecies(),
                investigation.validDescription()
        };
        String[] keys = {
                "gui.tl_creature_bestiary.investigation.stage.observed",
                "gui.tl_creature_bestiary.investigation.stage.combat",
                "gui.tl_creature_bestiary.investigation.stage.taming",
                "gui.tl_creature_bestiary.investigation.stage.species",
                "gui.tl_creature_bestiary.investigation.stage.description"
        };
        int gap = 3;
        int x = detailX() + 10;
        int y = detailContentY() + 13;
        int available = detailWidth() - 20;
        int stageWidth = Math.max(1, (available - gap * 4) / 5);
        for (int i = 0; i < keys.length; i++) {
            int stageX = x + i * (stageWidth + gap);
            graphics.fill(
                    stageX,
                    y,
                    Math.min(detailX() + detailWidth() - 10, stageX + stageWidth),
                    y + 10,
                    completed[i] ? TdmcUiTheme.ROW_SELECTED : TdmcUiTheme.ROW_BACKGROUND
            );
            Component label = Component.translatable(keys[i]);
            int labelX = stageX + Math.max(1, (stageWidth - font.width(label)) / 2);
            graphics.drawString(
                    font,
                    label,
                    labelX,
                    y + 1,
                    completed[i] ? TdmcUiTheme.TEXT_PRIMARY : TdmcUiTheme.TEXT_MUTED,
                    false
            );
        }
    }

    private void renderBasicTab(
            GuiGraphics graphics,
            ClientBestiaryState.Detail detail,
            BestiaryEntryProgress investigation
    ) {
        int viewportY = detailDataY(investigation);
        int viewportHeight = detailDataHeight(investigation);
        clampBasicScroll();
        int contentTop = viewportY - basicScrollPixels;

        graphics.enableScissor(
                detailX() + 1,
                viewportY,
                detailX() + detailWidth() - 1,
                viewportY + viewportHeight
        );
        basicContentHeight = renderBasicTabContent(graphics, detail, investigation, contentTop);
        graphics.disableScissor();
        clampBasicScroll();

        if (basicContentHeight > viewportHeight) {
            renderBasicScrollbar(graphics, viewportY, viewportHeight);
        }
    }

    private int renderBasicTabContent(
            GuiGraphics graphics,
            ClientBestiaryState.Detail detail,
            BestiaryEntryProgress investigation,
            int startY
    ) {
        int cursorY = renderUnifiedBasicMetadata(graphics, detail, investigation, startY + 4);

        if (investigation != null && !investigation.combatRecorded()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.investigation.locked.combat"),
                    detailX() + 10,
                    cursorY + 2,
                    TdmcUiTheme.TEXT_MUTED,
                    true
            );
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.investigation.locked.taming"),
                    detailX() + 10,
                    cursorY + 18,
                    TdmcUiTheme.TEXT_MUTED,
                    false
            );
            cursorY += 34;
            return Math.max(1, cursorY - startY + 8);
        }

        int radarY = cursorY + 2;
        int power = SpyglassRadarBaseline.powerPercentile(detail.baseStats().attackDamage());
        int life = SpyglassRadarBaseline.lifePercentile(detail.baseStats().maxHealth());
        int speed = SpyglassRadarBaseline.speedPercentile(detail.baseStats().movementSpeed());
        BestiaryRadarRenderer.render(
                graphics,
                font,
                detailX() + 12,
                radarY,
                Math.max(100, detailWidth() - 32),
                power,
                life,
                speed
        );
        int textY = radarY + 110;

        if (investigation != null && !investigation.tamingRecorded()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.investigation.locked.taming"),
                    detailX() + 10,
                    textY,
                    TdmcUiTheme.TEXT_MUTED,
                    true
            );
            textY += 14;
            return Math.max(1, textY - startY + 8);
        }

        graphics.drawString(
                font,
                Component.translatable(detail.tamingInfo().tameable()
                        ? "gui.tl_creature_bestiary.tameable_yes"
                        : "gui.tl_creature_bestiary.tameable_no"),
                detailX() + 10,
                textY,
                TdmcUiTheme.TEXT_PRIMARY,
                true
        );
        textY += 14;
        if (detail.rideable()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.rideable"),
                    detailX() + 10,
                    textY,
                    TdmcUiTheme.TEXT_PRIMARY,
                    true
            );
            textY += 14;
        }
        if (!detail.tamingInfo().tameable()) {
            return Math.max(1, textY - startY + 8);
        }

        graphics.drawString(
                font,
                Component.translatable("gui.tl_creature_bestiary.taming_foods"),
                detailX() + 10,
                textY,
                TdmcUiTheme.TEXT_ACCENT,
                true
        );
        textY += 12;
        int foodStartX = detailX() + 16;
        int foodRight = detailScrollbarX() - 6;
        int foodCursorX = foodStartX;
        int foodRowY = textY;
        int foodGap = 12;
        for (TamingFoodInfo food : detail.tamingInfo().foods()) {
            Item item = ForgeRegistries.ITEMS.getValue(food.itemId());
            Component itemName = item == null
                    ? Component.translatable("gui.tl_creature_bestiary.unknown_item")
                    : item.getDescription();
            Component foodLine = food.configured()
                    ? Component.translatable("gui.tl_creature_bestiary.food_amount", itemName, food.amount())
                    : Component.translatable("gui.tl_creature_bestiary.food_unconfigured", itemName);
            int foodWidth = font.width(foodLine);
            if (foodCursorX > foodStartX && foodCursorX + foodWidth > foodRight) {
                foodCursorX = foodStartX;
                foodRowY += 11;
            }
            graphics.drawString(
                    font,
                    foodLine,
                    foodCursorX,
                    foodRowY,
                    food.configured() ? TdmcUiTheme.TEXT_PRIMARY : TdmcUiTheme.TEXT_MUTED,
                    true
            );
            foodCursorX += foodWidth + foodGap;
        }
        textY = foodRowY + 12;
        return Math.max(1, textY - startY + 8);
    }

    private int renderUnifiedBasicMetadata(
            GuiGraphics graphics,
            ClientBestiaryState.Detail detail,
            BestiaryEntryProgress investigation,
            int startY
    ) {
        int cursorY = renderProfileDiet(graphics, detail, startY);

        if (investigation != null) {
            cursorY = renderInvestigationNotes(graphics, investigation, cursorY);
        } else {
            cursorY = renderAutomaticProfileNotes(graphics, detail, cursorY);
        }

        if (investigation == null || investigation.combatRecorded()) {
            int stars = CreatureInfoApi.dangerStars(detail.baseStats());
            EcologicalNiche niche = EcologicalNiche.fromDangerStars(stars);
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.tl_creature_bestiary.investigation.ecological_niche",
                            Component.translatable(niche.translationKey())
                    ),
                    detailX() + 10,
                    cursorY + 2,
                    TdmcUiTheme.TEXT_ACCENT,
                    true
            );
            cursorY += 16;
        }
        return cursorY;
    }

    private int renderProfileDiet(
            GuiGraphics graphics,
            ClientBestiaryState.Detail detail,
            int startY
    ) {
        Component diet = Component.translatable(detail.profile().diet().value().translationKey());
        graphics.drawString(
                font,
                Component.translatable("gui.tl_creature_bestiary.profile.diet_value", diet),
                detailX() + 10,
                startY,
                detail.profile().diet().value() == com.szypxj.tlcreaturebestiary.api.profile.BestiaryDiet.UNKNOWN
                        ? TdmcUiTheme.TEXT_MUTED
                        : TdmcUiTheme.TEXT_PRIMARY,
                false
        );
        return startY + 12;
    }

    private int renderAutomaticProfileNotes(
            GuiGraphics graphics,
            ClientBestiaryState.Detail detail,
            int startY
    ) {
        int x = detailX() + 10;
        int maxWidth = Math.max(40, detailWidth() - 28);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.tl_creature_bestiary.investigation.species_value",
                        detail.profile().species().value()
                ),
                x,
                startY,
                TdmcUiTheme.TEXT_PRIMARY,
                false
        );

        List<net.minecraft.util.FormattedCharSequence> descriptionLines = font.split(
                Component.translatable(
                        "gui.tl_creature_bestiary.investigation.description_value",
                        detail.profile().description().value()
                ),
                maxWidth
        );
        int y = startY + 12;
        for (net.minecraft.util.FormattedCharSequence line : descriptionLines) {
            graphics.drawString(
                    font,
                    line,
                    x,
                    y,
                    TdmcUiTheme.TEXT_PRIMARY,
                    false
            );
            y += 10;
        }
        return y + 2;
    }

    private void renderBasicScrollbar(GuiGraphics graphics, int y, int height) {
        int x = detailScrollbarX();
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + height, TdmcUiTheme.SLIDER_TRACK);
        TdmcUiTheme.outline(graphics, x, y, SCROLLBAR_WIDTH, height, TdmcUiTheme.BORDER);
        int thumbHeight = basicScrollbarThumbHeight(height);
        int thumbY = currentBasicThumbY();
        graphics.fill(
                x + 1,
                thumbY + 1,
                x + SCROLLBAR_WIDTH - 1,
                thumbY + thumbHeight - 1,
                TdmcUiTheme.SLIDER_THUMB
        );
        TdmcUiTheme.outline(graphics, x, thumbY, SCROLLBAR_WIDTH, thumbHeight, TdmcUiTheme.BORDER_HOVERED);
    }

    private int renderInvestigationNotes(
            GuiGraphics graphics,
            BestiaryEntryProgress investigation,
            int startY
    ) {
        int x = detailX() + 10;
        int maxWidth = Math.max(40, detailWidth() - 20);
        Component notRecorded = Component.translatable("gui.tl_creature_bestiary.investigation.not_recorded");
        Component speciesValue = investigation.validSpecies()
                ? Component.literal(investigation.species())
                : notRecorded;
        graphics.drawString(
                font,
                Component.translatable("gui.tl_creature_bestiary.investigation.species_value", speciesValue),
                x,
                startY,
                investigation.validSpecies() ? TdmcUiTheme.TEXT_PRIMARY : TdmcUiTheme.TEXT_MUTED,
                false
        );

        boolean hasDescription = !investigation.description().isBlank();
        Component descriptionValue = hasDescription
                ? Component.literal(investigation.description())
                : notRecorded;
        List<net.minecraft.util.FormattedCharSequence> descriptionLines = font.split(
                Component.translatable("gui.tl_creature_bestiary.investigation.description_value", descriptionValue),
                maxWidth
        );
        int y = startY + 12;
        int lines = descriptionLines.size();
        for (int i = 0; i < lines; i++) {
            graphics.drawString(
                    font,
                    descriptionLines.get(i),
                    x,
                    y,
                    investigation.validDescription() ? TdmcUiTheme.TEXT_PRIMARY : TdmcUiTheme.TEXT_MUTED,
                    false
            );
            y += 10;
        }
        if (hasDescription && !investigation.validDescription()) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.tl_creature_bestiary.investigation.description_incomplete",
                            investigation.descriptionNonWhitespaceLength(),
                            BestiaryEntryProgress.MIN_DESCRIPTION_NON_WHITESPACE
                    ),
                    x,
                    y,
                    TdmcUiTheme.TEXT_MUTED,
                    false
            );
            y += 10;
        }
        return y + 2;
    }

    private void renderDropsTab(
            GuiGraphics graphics,
            ClientBestiaryState.Detail detail,
            BestiaryEntryProgress investigation
    ) {
        int contentY = detailDataY(investigation);
        int contentHeight = detailDataHeight(investigation);
        if (investigation != null && !investigation.combatRecorded()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.investigation.locked.drops_combat"),
                    detailX() + 10,
                    contentY + 10,
                    TdmcUiTheme.TEXT_MUTED,
                    true
            );
            return;
        }
        List<DropInfo> drops = detail.drops();
        if (drops.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.tl_creature_bestiary.no_drops"), detailX() + 10, contentY + 10, TdmcUiTheme.TEXT_MUTED, true);
            return;
        }
        int visible = Math.max(1, contentHeight / DROP_ROW_HEIGHT);
        detailScroll = clamp(detailScroll, 0, Math.max(0, drops.size() - visible));
        int end = Math.min(drops.size(), detailScroll + visible);
        for (int i = detailScroll; i < end; i++) {
            DropInfo drop = drops.get(i);
            int y = contentY + (i - detailScroll) * DROP_ROW_HEIGHT;
            Item item = ForgeRegistries.ITEMS.getValue(drop.itemId());
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            graphics.renderItem(stack, detailX() + 10, y + 2);
            graphics.drawString(font, item.getDescription(), detailX() + 32, y + 3, TdmcUiTheme.TEXT_PRIMARY, true);
            Component amount = dropAmount(drop);
            Component chance = dropChance(drop);
            int right = detailScrollbarX() - 6;
            graphics.drawString(font, chance, right - font.width(chance), y + 3, TdmcUiTheme.TEXT_ACCENT, true);
            graphics.drawString(font, amount, detailX() + 32, y + 15, TdmcUiTheme.TEXT_MUTED, false);
            Component flags = dropFlags(drop);
            if (!flags.getString().isEmpty()) {
                graphics.drawString(font, flags, right - font.width(flags), y + 15, TdmcUiTheme.TEXT_MUTED, false);
            }
        }
        renderScrollbar(graphics, detailScrollbarX(), contentY, contentHeight, visible, drops.size(), detailScroll);
    }

    private Component dropAmount(DropInfo drop) {
        if (drop.maxCount() < 0) {
            return Component.translatable("gui.tl_creature_bestiary.drop_amount_open", drop.minCount());
        }
        if (drop.minCount() == drop.maxCount()) {
            return Component.translatable("gui.tl_creature_bestiary.drop_amount_single", drop.minCount());
        }
        return Component.translatable("gui.tl_creature_bestiary.drop_amount_range", drop.minCount(), drop.maxCount());
    }

    private Component dropChance(DropInfo drop) {
        if (!drop.exactChance() || drop.specialCondition()) {
            return Component.translatable("gui.tl_creature_bestiary.drop_chance_special");
        }
        return Component.translatable(
                "gui.tl_creature_bestiary.drop_chance",
                String.format(Locale.ROOT, "%.1f", drop.chancePercent())
        );
    }

    private Component dropFlags(DropInfo drop) {
        if (drop.killedByPlayer() && drop.lootingAffected()) {
            return Component.translatable("gui.tl_creature_bestiary.drop_flags_player_looting");
        }
        if (drop.killedByPlayer()) {
            return Component.translatable("gui.tl_creature_bestiary.drop_flag_player");
        }
        if (drop.lootingAffected()) {
            return Component.translatable("gui.tl_creature_bestiary.drop_flag_looting");
        }
        return Component.empty();
    }

    private void renderBiomesTab(
            GuiGraphics graphics,
            ClientBestiaryState.Detail detail,
            BestiaryEntryProgress investigation
    ) {
        int contentY = detailDataY(investigation);
        int contentHeight = detailDataHeight(investigation);
        if (investigation != null && !investigation.observed()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_creature_bestiary.investigation.locked.observation"),
                    detailX() + 10,
                    contentY + 10,
                    TdmcUiTheme.TEXT_MUTED,
                    true
            );
            return;
        }
        List<ResourceLocation> biomes = detail.biomeIds();
        if (biomes.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.tl_creature_bestiary.no_biomes"), detailX() + 10, contentY + 10, TdmcUiTheme.TEXT_MUTED, true);
            return;
        }
        int visible = Math.max(1, contentHeight / BIOME_ROW_HEIGHT);
        detailScroll = clamp(detailScroll, 0, Math.max(0, biomes.size() - visible));
        int end = Math.min(biomes.size(), detailScroll + visible);
        for (int i = detailScroll; i < end; i++) {
            ResourceLocation id = biomes.get(i);
            int y = contentY + (i - detailScroll) * BIOME_ROW_HEIGHT;
            Component name = Component.translatable(Util.makeDescriptionId("biome", id));
            graphics.drawString(font, name, detailX() + 10, y + 3, TdmcUiTheme.TEXT_PRIMARY, true);
            graphics.drawString(font, id.toString(), detailX() + 10, y + 14, TdmcUiTheme.TEXT_MUTED, false);
        }
        renderScrollbar(graphics, detailScrollbarX(), contentY, contentHeight, visible, biomes.size(), detailScroll);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height, int visibleRows, int totalRows, int currentScroll) {
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + height, TdmcUiTheme.SLIDER_TRACK);
        TdmcUiTheme.outline(graphics, x, y, SCROLLBAR_WIDTH, height, TdmcUiTheme.BORDER);
        if (totalRows <= 0) {
            return;
        }
        int thumbHeight = scrollbarThumbHeight(visibleRows, totalRows, height);
        int thumbY = scrollbarThumbY(y, height, thumbHeight, visibleRows, totalRows, currentScroll);
        graphics.fill(x + 1, thumbY + 1, x + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight - 1, TdmcUiTheme.SLIDER_THUMB);
        TdmcUiTheme.outline(graphics, x, thumbY, SCROLLBAR_WIDTH, thumbHeight, TdmcUiTheme.BORDER_HOVERED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (isOverListScrollbar(mouseX, mouseY)) {
            draggingListScrollbar = true;
            scrollbarDragOffset = (int) mouseY - currentListThumbY();
            return true;
        }
        if (isOverDetailScrollbar(mouseX, mouseY)) {
            draggingDetailScrollbar = true;
            scrollbarDragOffset = (int) mouseY - currentDetailThumbY();
            return true;
        }
        if (mouseX >= listX() && mouseX < listX() + listContentWidth() && mouseY >= listY() && mouseY < listY() + listHeight()) {
            int visibleIndex = (int) ((mouseY - listY()) / ROW_HEIGHT);
            int index = scroll + visibleIndex;
            if (index < 0 || index >= rows.size()) {
                return false;
            }
            BestiaryEntryRow row = rows.get(index);
            selectedId = row.entityTypeId();
            detailScroll = 0;
            basicScrollPixels = 0;
            basicContentHeight = 0;
            onSelectionChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingListScrollbar) {
            updateListScrollFromMouse(mouseY);
            return true;
        }
        if (draggingDetailScrollbar) {
            updateDetailScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingListScrollbar = false;
        draggingDetailScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listX() && mouseX < listX() + listWidth() && mouseY >= listY() && mouseY < listY() + listHeight()) {
            scroll += delta < 0.0D ? 1 : -1;
            clampListScroll();
            return true;
        }
        BestiaryEntryProgress investigation = selectedInvestigation();
        int dataY = detailDataY(investigation);
        int dataHeight = detailDataHeight(investigation);
        if (mouseX >= detailX() && mouseX < detailX() + detailWidth() && mouseY >= dataY && mouseY < dataY + dataHeight) {
            if (activeTab == DetailTab.BASIC) {
                basicScrollPixels += delta < 0.0D ? 18 : -18;
                clampBasicScroll();
            } else {
                detailScroll += delta < 0.0D ? 1 : -1;
                clampDetailScroll();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void updateListScrollFromMouse(double mouseY) {
        int visible = visibleRows();
        int total = rows.size();
        int thumbHeight = scrollbarThumbHeight(visible, total, listScrollbarHeight());
        scroll = scrollFromMouse(mouseY, listScrollbarY(), listScrollbarHeight(), thumbHeight, visible, total);
        clampListScroll();
    }

    private void updateDetailScrollFromMouse(double mouseY) {
        if (activeTab == DetailTab.BASIC) {
            updateBasicScrollFromMouse(mouseY);
            return;
        }
        int total = detailItemCount();
        int visible = detailVisibleRows();
        BestiaryEntryProgress investigation = selectedInvestigation();
        int dataY = detailDataY(investigation);
        int dataHeight = detailDataHeight(investigation);
        int thumbHeight = scrollbarThumbHeight(visible, total, dataHeight);
        detailScroll = scrollFromMouse(mouseY, dataY, dataHeight, thumbHeight, visible, total);
        clampDetailScroll();
    }

    private void updateBasicScrollFromMouse(double mouseY) {
        BestiaryEntryProgress investigation = selectedInvestigation();
        int dataY = detailDataY(investigation);
        int dataHeight = detailDataHeight(investigation);
        int thumbHeight = basicScrollbarThumbHeight(dataHeight);
        int maxTravel = Math.max(0, dataHeight - thumbHeight);
        int thumbTop = (int) Math.round(mouseY) - scrollbarDragOffset;
        int clampedTop = clamp(thumbTop, dataY, dataY + maxTravel);
        int maxScroll = Math.max(0, basicContentHeight - dataHeight);
        if (maxTravel <= 0 || maxScroll <= 0) {
            basicScrollPixels = 0;
            return;
        }
        basicScrollPixels = (int) Math.round(((clampedTop - dataY) / (double) maxTravel) * maxScroll);
        clampBasicScroll();
    }

    private int scrollFromMouse(double mouseY, int y, int height, int thumbHeight, int visible, int total) {
        int maxTravel = Math.max(0, height - thumbHeight);
        int thumbTop = (int) Math.round(mouseY) - scrollbarDragOffset;
        int clampedTop = clamp(thumbTop, y, y + maxTravel);
        int maxScroll = Math.max(0, total - visible);
        if (maxTravel <= 0 || maxScroll <= 0) {
            return 0;
        }
        return (int) Math.round(((clampedTop - y) / (double) maxTravel) * maxScroll);
    }

    private int currentListThumbY() {
        int height = scrollbarThumbHeight(visibleRows(), rows.size(), listScrollbarHeight());
        return scrollbarThumbY(listScrollbarY(), listScrollbarHeight(), height, visibleRows(), rows.size(), scroll);
    }

    private int currentDetailThumbY() {
        if (activeTab == DetailTab.BASIC) {
            return currentBasicThumbY();
        }
        int total = detailItemCount();
        int visible = detailVisibleRows();
        BestiaryEntryProgress investigation = selectedInvestigation();
        int dataY = detailDataY(investigation);
        int dataHeight = detailDataHeight(investigation);
        int height = scrollbarThumbHeight(visible, total, dataHeight);
        return scrollbarThumbY(dataY, dataHeight, height, visible, total, detailScroll);
    }

    private int currentBasicThumbY() {
        BestiaryEntryProgress investigation = selectedInvestigation();
        int dataY = detailDataY(investigation);
        int dataHeight = detailDataHeight(investigation);
        int thumbHeight = basicScrollbarThumbHeight(dataHeight);
        int maxScroll = Math.max(0, basicContentHeight - dataHeight);
        int travel = Math.max(0, dataHeight - thumbHeight);
        return dataY + (maxScroll == 0
                ? 0
                : (int) Math.round((basicScrollPixels / (double) maxScroll) * travel));
    }

    private boolean isOverListScrollbar(double mouseX, double mouseY) {
        return mouseX >= listScrollbarX() && mouseX < listScrollbarX() + SCROLLBAR_WIDTH && mouseY >= listScrollbarY() && mouseY < listScrollbarY() + listScrollbarHeight();
    }

    private boolean isOverDetailScrollbar(double mouseX, double mouseY) {
        BestiaryEntryProgress investigation = selectedInvestigation();
        int dataY = detailDataY(investigation);
        int dataHeight = detailDataHeight(investigation);
        boolean scrollable = activeTab == DetailTab.BASIC
                ? basicContentHeight > dataHeight
                : detailItemCount() > detailVisibleRows();
        return scrollable
                && mouseX >= detailScrollbarX()
                && mouseX < detailScrollbarX() + SCROLLBAR_WIDTH
                && mouseY >= dataY
                && mouseY < dataY + dataHeight;
    }

    private int scrollbarThumbHeight(int visibleRows, int totalRows, int height) {
        return Math.min(height, Math.max(18, (int) Math.round((visibleRows / (double) Math.max(1, totalRows)) * height)));
    }

    private int scrollbarThumbY(int y, int height, int thumbHeight, int visibleRows, int totalRows, int currentScroll) {
        int maxScroll = Math.max(0, totalRows - visibleRows);
        int travel = Math.max(0, height - thumbHeight);
        return y + (maxScroll == 0 ? 0 : (int) Math.round((currentScroll / (double) maxScroll) * travel));
    }

    private void clampListScroll() {
        scroll = clamp(scroll, 0, Math.max(0, rows.size() - visibleRows()));
    }

    private void clampDetailScroll() {
        if (activeTab == DetailTab.BASIC) {
            clampBasicScroll();
            return;
        }
        detailScroll = clamp(detailScroll, 0, Math.max(0, detailItemCount() - detailVisibleRows()));
    }

    private void clampBasicScroll() {
        int viewportHeight = detailDataHeight(selectedInvestigation());
        basicScrollPixels = clamp(basicScrollPixels, 0, Math.max(0, basicContentHeight - viewportHeight));
    }

    private int basicScrollbarThumbHeight(int viewportHeight) {
        if (basicContentHeight <= 0) {
            return viewportHeight;
        }
        return Math.min(
                viewportHeight,
                Math.max(18, (int) Math.round((viewportHeight / (double) basicContentHeight) * viewportHeight))
        );
    }

    private int detailItemCount() {
        ClientBestiaryState.Detail detail = selectedId == null ? null : ClientBestiaryState.detail(selectedId);
        if (detail == null) {
            return 0;
        }
        if (activeTab == DetailTab.DROPS) {
            BestiaryEntryProgress investigation = selectedInvestigation();
            if (investigation != null && !investigation.combatRecorded()) {
                return 0;
            }
        }
        return switch (activeTab) {
            case BASIC -> 0;
            case DROPS -> detail.drops().size();
            case BIOMES -> detail.biomeIds().size();
        };
    }

    private int detailVisibleRows() {
        int dataHeight = detailDataHeight(selectedInvestigation());
        return switch (activeTab) {
            case BASIC -> 0;
            case DROPS -> Math.max(1, dataHeight / DROP_ROW_HEIGHT);
            case BIOMES -> Math.max(1, dataHeight / BIOME_ROW_HEIGHT);
        };
    }

    private BestiaryEntryProgress selectedInvestigation() {
        if (selectedId == null) {
            return null;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(selectedId);
        if (!BestiaryInvestigationRules.isHighRisk(type)) {
            return null;
        }
        return ClientBestiaryState.progress(selectedId);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int listX() {
        return panelX + 10;
    }

    private int listY() {
        return panelY + HEADER_HEIGHT + 34;
    }

    private int listHeight() {
        return panelHeight - HEADER_HEIGHT - 44;
    }

    private int listWidth() {
        return Math.min(LIST_WIDTH - 20, panelWidth / 2 - 20);
    }

    private int listContentWidth() {
        return listWidth() - SCROLLBAR_WIDTH - 4;
    }

    private int visibleRows() {
        return Math.max(1, listHeight() / ROW_HEIGHT);
    }

    private int listScrollbarX() {
        return listX() + listContentWidth() + 2;
    }

    private int listScrollbarY() {
        return listY() + 2;
    }

    private int listScrollbarHeight() {
        return listHeight() - 4;
    }

    private int detailX() {
        return panelX + Math.min(LIST_WIDTH, panelWidth / 2) + 10;
    }

    private int detailY() {
        return panelY + HEADER_HEIGHT + 8;
    }

    private int detailWidth() {
        return panelX + panelWidth - 10 - detailX();
    }

    private int detailHeight() {
        return panelHeight - HEADER_HEIGHT - 18;
    }

    private int detailContentY() {
        return detailY() + 68;
    }

    private int detailContentHeight() {
        return Math.max(1, detailY() + detailHeight() - detailContentY() - 8);
    }

    private int detailDataY(BestiaryEntryProgress investigation) {
        return detailContentY() + (investigation == null ? 0 : 28);
    }

    private int detailDataHeight(BestiaryEntryProgress investigation) {
        return Math.max(1, detailY() + detailHeight() - detailDataY(investigation) - 8);
    }

    private int detailScrollbarX() {
        return detailX() + detailWidth() - 12;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class ThemedMultiLineEditBox extends MultiLineEditBox {
        private ThemedMultiLineEditBox(
                Font font,
                int x,
                int y,
                int width,
                int height,
                Component message,
                Component placeholder
        ) {
            super(font, x, y, width, height, message, placeholder);
        }

        @Override
        protected void renderBackground(GuiGraphics graphics) {
            int background = isFocused() ? TdmcUiTheme.INPUT_FOCUSED : TdmcUiTheme.INPUT_BACKGROUND;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
        }

        @Override
        protected void renderBorder(GuiGraphics graphics, int x, int y, int width, int height) {
            int border = isFocused() ? TdmcUiTheme.BORDER_HOVERED : TdmcUiTheme.BORDER;
            TdmcUiTheme.outline(graphics, x, y, width, height, border);
        }
    }

    private enum DetailTab {
        BASIC,
        DROPS,
        BIOMES
    }
}
