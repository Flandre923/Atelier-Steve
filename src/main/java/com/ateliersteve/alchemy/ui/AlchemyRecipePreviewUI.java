package com.ateliersteve.alchemy.ui;

import com.ateliersteve.AtelierSteve;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeDefinition;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeIngredient;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;

public final class AlchemyRecipePreviewUI {
    private static final TagKey<Item> NEUTRALIZER_TAG = TagKey.create(
            Registries.ITEM,
            AtelierSteve.id("category_neutralizer")
    );
    private static final Map<ResourceLocation, ResourceLocation> TAG_CATEGORY_ICONS = Map.of(
            AtelierSteve.id("category_gunpowder"), AtelierSteve.id("textures/gui/ingredients/category_gunpowder.png"),
            AtelierSteve.id("category_water"), AtelierSteve.id("textures/gui/ingredients/category_water.png")
    );

    private AlchemyRecipePreviewUI() {
    }

    public static ModularUI createUI(Player player, BlockPos cauldronPos) {
        List<AlchemyRecipeDefinition> recipes = AlchemyRecipeRegistry.getAll();
        AlchemyRecipeDefinition selectedRecipe = recipes.isEmpty() ? null : recipes.get(0);

        var xml = XmlUtils.loadXml(AtelierSteve.id("alchemy_recipe_preview.xml"));
        if (xml == null) {
            return ModularUI.of(UI.of(new UIElement()), player);
        }

        var ui = UI.of(xml);
        var itemList = (ScrollerView) ui.select("#item_list").findFirst().orElseThrow();
        var titleLabel = (Label) ui.select("#title_label").findFirst().orElseThrow();
        var levelText = (Label) ui.select("#level_text").findFirst().orElseThrow();
        var bigIconSlot = (ItemSlot) ui.select("#big_icon_slot").findFirst().orElseThrow();
        var qtyLabel = (Label) ui.select("#qty_label").findFirst().orElseThrow();
        var subTag = ui.select("#sub_tag").findFirst().orElseThrow();
        var subTagLabel = (Label) ui.select("#sub_tag_label").findFirst().orElseThrow();
        var materialsHeader = (Label) ui.select("#materials_header").findFirst().orElseThrow();
        var materialsList = ui.select("#materials_list").findFirst().orElseThrow();

        subTagLabel.textStyle(textStyle -> textStyle.adaptiveWidth(true));

        populateItemList(itemList, recipes, selectedRecipe, player, cauldronPos);
        applyRecipeDetails(
                selectedRecipe,
                titleLabel,
                levelText,
                bigIconSlot,
                qtyLabel,
                subTag,
                subTagLabel,
                materialsHeader,
                materialsList
        );

        return ModularUI.of(ui, player);
    }

    private static void populateItemList(
            ScrollerView scroller,
            List<AlchemyRecipeDefinition> recipes,
            AlchemyRecipeDefinition selectedRecipe,
            Player player,
            BlockPos cauldronPos
    ) {
        List<ItemStack> stacks = recipes.stream()
                .map(AlchemyRecipePreviewUI::resolveResultStack)
                .toList();
        var listHandler = createDisplayHandler(stacks);

        if (recipes.isEmpty()) {
            scroller.addScrollViewChild(new Label()
                    .setText(Component.literal("No recipes available"))
                    .addClass("item_list_empty"));
            return;
        }

        for (int i = 0; i < recipes.size(); i++) {
            AlchemyRecipeDefinition recipe = recipes.get(i);
            ItemStack stack = stacks.get(i);
            Component name = stack.isEmpty()
                    ? Component.literal(recipe.result().toString())
                    : stack.getHoverName();
            boolean active = recipe.equals(selectedRecipe);
            scroller.addScrollViewChild(buildListItem(
                    listHandler,
                    i,
                    name,
                    active,
                    () -> {
                        if (player instanceof ServerPlayer serverPlayer) {
                            AlchemyMaterialSelectionUI.requestOpen(serverPlayer, recipe);
                            serverPlayer.closeContainer();
                            serverPlayer.getServer().execute(() -> BlockUIMenuType.openUI(serverPlayer, cauldronPos));
                        }
                    }
            ));
        }
    }

    private static void applyRecipeDetails(
            AlchemyRecipeDefinition recipe,
            Label titleLabel,
            Label levelText,
            ItemSlot bigIconSlot,
            Label qtyLabel,
            UIElement subTag,
            Label subTagLabel,
            Label materialsHeader,
            UIElement materialsList
    ) {
        int effectCount = recipe == null ? 0 : recipe.effects().size();
        int totalIngredients = recipe == null
                ? 0
                : recipe.ingredients().stream().mapToInt(AlchemyRecipeIngredient::count).sum();
        Component title = Component.literal("No Recipe");
        if (recipe != null) {
            ItemStack stack = resolveResultStack(recipe);
            title = stack.isEmpty() ? Component.literal(recipe.result().toString()) : stack.getHoverName();
        }

        titleLabel.setText(title);
        levelText.setText(Component.literal("LV: " + effectCount));

        var iconHandler = createDisplayHandler(List.of(resolveResultStack(recipe)));
        bigIconSlot.bind(iconHandler, 0);

        qtyLabel.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.craft_amount")
                .append(Component.literal(": " + totalIngredients)));

        Component subTagText = Component.empty();
        if (recipe != null) {
            ItemStack resultStack = resolveResultStack(recipe);
            if (!resultStack.isEmpty() && resultStack.is(NEUTRALIZER_TAG)) {
                subTagText = Component.translatable("ui.atelier_steve.alchemy_recipe.neutralizer_tag");
            }
        }
        subTagLabel.setText(subTagText);
        if (subTagText.getString().isEmpty()) {
            subTag.addClass("hidden");
        } else {
            subTag.removeClass("hidden");
        }

        materialsHeader.setText(Component.translatable("ui.atelier_steve.alchemy_recipe.materials"));
        materialsList.clearAllChildren();

        if (recipe == null || recipe.ingredients().isEmpty()) {
            materialsList.addChild(new Label()
                    .setText(Component.literal("No materials"))
                    .addClass("materials_empty"));
            return;
        }

        List<ItemStack> materialStacks = recipe.ingredients().stream()
                .map(AlchemyRecipePreviewUI::resolveIngredientStack)
                .toList();
        var handler = createDisplayHandler(materialStacks);

        for (int i = 0; i < recipe.ingredients().size(); i++) {
            AlchemyRecipeIngredient ingredient = recipe.ingredients().get(i);
            ItemStack stack = materialStacks.get(i);
            Component name = buildIngredientName(ingredient, stack);
            materialsList.addChild(buildMaterialRow(handler, ingredient, i, name, ingredient.count()));
        }
    }

    private static UIElement buildMaterialRow(ItemStackHandler handler, AlchemyRecipeIngredient ingredient, int slot, Component name, int count) {
        var row = new UIElement()
                .addClass("material_row")
                .addClass("row_space_between");

        var info = new UIElement()
                .addClass("material_info")
                .addClass("row_align_center");

        info.addChildren(
                buildMaterialIcon(handler, ingredient, slot),
                new Label().setText(name).addClass("material_name")
        );

        row.addChildren(info, new Label()
                .setText(Component.literal("x " + count))
                .addClass("material_qty"));

        return row;
    }

    private static UIElement buildMaterialIcon(ItemStackHandler handler, AlchemyRecipeIngredient ingredient, int slot) {
        ResourceLocation icon = resolveCategoryIcon(ingredient);
        if (icon != null) {
            return new UIElement()
                    .addClass("material_icon")
                    .addClass("material_icon_tag")
                    .lss("background", "sprite(" + icon + ")");
        }
        return new ItemSlot()
                .bind(handler, slot)
                .addClass("material_icon");
    }

    private static UIElement buildListItem(
            ItemStackHandler handler,
            int slot,
            Component name,
            boolean active,
            Runnable onServerClick
    ) {
        var row = new UIElement()
                .addClass("list_item")
                .addClass("row_space_between");

        if (active) {
            row.addClass("active");
        }

        var info = new UIElement()
                .addClass("item_info")
                .addClass("row_align_center")
                .addClass("flex_grow");

        var iconSlot = new ItemSlot()
                .bind(handler, slot)
                .addClass("item_icon_small");
        var nameLabel = new Label()
                .setText(name)
                .addClass("item_name")
                .addClass("flex_grow");
        info.addChildren(iconSlot, nameLabel);

        row.addChildren(info);

        if (onServerClick != null) {
            row.addServerEventListener(UIEvents.CLICK, e -> onServerClick.run());
        }

        return row;
    }

    private static ItemStackHandler createDisplayHandler(List<ItemStack> stacks) {
        var handler = new ItemStackHandler(stacks.size()) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
        };

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            handler.setStackInSlot(i, stack == null ? ItemStack.EMPTY : stack.copy());
        }

        return handler;
    }

    private static ItemStack resolveResultStack(AlchemyRecipeDefinition recipe) {
        if (recipe == null || recipe.result() == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(recipe.result());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static ItemStack resolveIngredientStack(AlchemyRecipeIngredient ingredient) {
        if (ingredient == null) {
            return ItemStack.EMPTY;
        }
        if (ingredient.type() == AlchemyRecipeIngredient.Type.SPECIFIC) {
            return ingredient.itemId()
                    .map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id), ingredient.count()))
                    .orElse(ItemStack.EMPTY);
        }
        if (ingredient.type() == AlchemyRecipeIngredient.Type.TAG && ingredient.tag().isPresent()) {
            TagKey<Item> tagKey = ingredient.tag().get();
            var tag = BuiltInRegistries.ITEM.getTag(tagKey);
            if (tag.isPresent()) {
                for (Holder<Item> holder : tag.get()) {
                    return new ItemStack(holder.value(), ingredient.count());
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static Component buildIngredientName(AlchemyRecipeIngredient ingredient, ItemStack stack) {
        if (ingredient.type() == AlchemyRecipeIngredient.Type.TAG && ingredient.tag().isPresent()) {
            ResourceLocation tagId = ingredient.tag().get().location();
            return Component.literal("#" + tagId);
        }
        if (!stack.isEmpty()) {
            return stack.getHoverName();
        }
        if (ingredient.type() == AlchemyRecipeIngredient.Type.SPECIFIC && ingredient.itemId().isPresent()) {
            return Component.literal(ingredient.itemId().get().toString());
        }
        return Component.literal("Unknown");
    }

    private static ResourceLocation resolveCategoryIcon(AlchemyRecipeIngredient ingredient) {
        if (ingredient == null || ingredient.type() != AlchemyRecipeIngredient.Type.TAG) {
            return null;
        }
        if (ingredient.tag().isEmpty()) {
            return null;
        }
        ResourceLocation tagId = ingredient.tag().get().location();
        return TAG_CATEGORY_ICONS.get(tagId);
    }
}
