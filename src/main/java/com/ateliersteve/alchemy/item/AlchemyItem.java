package com.ateliersteve.alchemy.item;

import com.ateliersteve.alchemy.AlchemyItemData;
import com.ateliersteve.alchemy.element.ElementComponent;
import com.ateliersteve.registry.ModDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AlchemyItem extends Item {
    private final int level;
    private final AlchemyItemData baseData;
    private final List<AlchemyItemEffect> effects;

    public AlchemyItem(int level, AlchemyItemData baseData, List<AlchemyItemEffect> effects, Properties properties) {
        super(properties);
        this.level = level;
        this.baseData = baseData;
        this.effects = List.copyOf(effects);
    }

    public int getLevel() {
        return level;
    }

    public List<AlchemyItemEffect> getEffects() {
        return effects;
    }

    public AlchemyItemData createBaseAlchemyData() {
        return baseData;
    }

    public AlchemyItemData createAlchemyDataWithEffects() {
        List<ElementComponent> elements = new ArrayList<>(baseData.elements());
        for (AlchemyItemEffect effect : effects) {
            elements.addAll(effect.bonusElements());
        }
        return new AlchemyItemData(baseData.traits(), elements, baseData.cole(), baseData.quality());
    }

    public void applyBaseAlchemyData(ItemStack stack) {
        if (!stack.has(ModDataComponents.ALCHEMY_DATA.get())) {
            stack.set(ModDataComponents.ALCHEMY_DATA.get(), createBaseAlchemyData());
        }
    }

    public void applyAlchemyDataWithEffects(ItemStack stack) {
        if (!stack.has(ModDataComponents.ALCHEMY_DATA.get())) {
            stack.set(ModDataComponents.ALCHEMY_DATA.get(), createAlchemyDataWithEffects());
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        applyBaseAlchemyData(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        applyBaseAlchemyData(stack);
        super.onCraftedBy(stack, level, player);
    }
}
