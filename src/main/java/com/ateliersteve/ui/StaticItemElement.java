package com.ateliersteve.ui;

import com.ateliersteve.alchemy.AlchemyTooltipText;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class StaticItemElement extends UIElement {
    private ItemStack stack = ItemStack.EMPTY;

    public StaticItemElement setStack(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        refreshTexture();
        return this;
    }

    private void refreshTexture() {
        if (stack.isEmpty()) {
            style(style -> style.backgroundTexture(null));
            style(style -> style.tooltips());
            return;
        }
        style(style -> style.backgroundTexture(new ItemStackTexture(stack)));
        List<Component> tooltip = AlchemyTooltipText.buildStackTooltip(stack, true);
        style(style -> style.tooltips(tooltip.toArray(Component[]::new)));
    }
}
