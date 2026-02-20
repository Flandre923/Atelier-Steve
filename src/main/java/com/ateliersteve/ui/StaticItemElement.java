package com.ateliersteve.ui;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.world.item.ItemStack;

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
            return;
        }
        style(style -> style.backgroundTexture(new ItemStackTexture(stack)));
    }
}
