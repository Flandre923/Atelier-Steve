package com.ateliersteve.alchemy.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Set;

/**
 * Represents an alchemy trait definition with all its properties.
 * This is a data-driven approach that allows traits to be defined in JSON files.
 */
public class TraitDefinition {
    private final ResourceLocation id;
    private final String nameKey;
    private final int grade;
    private final String descriptionKey;
    private final Set<TagKey<Item>> inheritableCategories;
    private final List<TraitEffect> effects;
    private final List<TraitCombinationRecipe> combinations;

    public TraitDefinition(
            ResourceLocation id,
            String nameKey,
            int grade,
            String descriptionKey,
            Set<TagKey<Item>> inheritableCategories,
            List<TraitEffect> effects,
            List<TraitCombinationRecipe> combinations
    ) {
        this.id = id;
        this.nameKey = nameKey;
        this.grade = grade;
        this.descriptionKey = descriptionKey;
        this.inheritableCategories = inheritableCategories;
        this.effects = effects;
        this.combinations = combinations;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Component getName() {
        return Component.translatable(nameKey);
    }

    public int getGrade() {
        return grade;
    }

    public Component getDescription() {
        return Component.translatable(descriptionKey);
    }

    public Set<TagKey<Item>> getInheritableCategories() {
        return inheritableCategories;
    }

    public List<TraitEffect> getEffects() {
        return effects;
    }

    public List<TraitCombinationRecipe> getCombinations() {
        return combinations;
    }

    /**
     * Check if this trait can be inherited by an item with the given category.
     */
    public boolean canInheritTo(TagKey<Item> category) {
        return inheritableCategories.contains(category);
    }

    public static class Builder {
        private ResourceLocation id;
        private String nameKey;
        private int grade;
        private String descriptionKey;
        private Set<TagKey<Item>> inheritableCategories = Set.of();
        private List<TraitEffect> effects = List.of();
        private List<TraitCombinationRecipe> combinations = List.of();

        public Builder id(ResourceLocation id) {
            this.id = id;
            return this;
        }

        public Builder nameKey(String nameKey) {
            this.nameKey = nameKey;
            return this;
        }

        public Builder grade(int grade) {
            this.grade = grade;
            return this;
        }

        public Builder descriptionKey(String descriptionKey) {
            this.descriptionKey = descriptionKey;
            return this;
        }

        public Builder inheritableCategories(Set<TagKey<Item>> categories) {
            this.inheritableCategories = categories;
            return this;
        }

        public Builder effects(List<TraitEffect> effects) {
            this.effects = effects;
            return this;
        }

        public Builder combinations(List<TraitCombinationRecipe> combinations) {
            this.combinations = combinations;
            return this;
        }

        public TraitDefinition build() {
            return new TraitDefinition(id, nameKey, grade, descriptionKey, inheritableCategories, effects, combinations);
        }
    }
}
