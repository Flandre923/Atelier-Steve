package com.ateliersteve.block;

import com.ateliersteve.alchemy.ui.AlchemyCombineUI;
import com.ateliersteve.alchemy.ui.AlchemyMaterialSelectionUI;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeRegistry;
import com.ateliersteve.alchemy.recipe.AlchemyRecipeDefinition;
import com.ateliersteve.registry.ModBlockEntities;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AlchemyCauldronBlockEntity extends BlockEntity {
    public AlchemyCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMY_CAULDRON.get(), pos, state);
    }

    public ModularUI createUI(Player player) {
        AlchemyCombineUI.PendingCombine pendingCombine = AlchemyCombineUI.consumePendingCombine(player);
        if (pendingCombine != null) {
            return AlchemyCombineUI.createUI(player, getBlockPos(), pendingCombine);
        }
        AlchemyRecipeDefinition pending = AlchemyMaterialSelectionUI.consumePendingRecipe(player);
        if (pending == null) {
            var recipes = AlchemyRecipeRegistry.getAll();
            pending = recipes.isEmpty() ? null : recipes.get(0);
        }
        return AlchemyMaterialSelectionUI.createUI(player, getBlockPos(), pending);
    }
}
