package com.ateliersteve.alchemy.trait;

import com.ateliersteve.AtelierSteve;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Item categories for trait inheritance system.
 * Based on Atelier series alchemy categories.
 */
public class ItemCategory {
    // 调合 (Synthesis) - General synthesis items
    public static final TagKey<Item> SYNTHESIS = create("alchemy_synthesis");

    // 攻击 (Attack) - Offensive items
    public static final TagKey<Item> ATTACK = create("alchemy_attack");

    // 恢复 (Recovery) - Healing/recovery items
    public static final TagKey<Item> RECOVERY = create("alchemy_recovery");

    // 削弱 (Debuff) - Debuff/weakening items
    public static final TagKey<Item> DEBUFF = create("alchemy_debuff");

    // 强化 (Buff) - Buff/enhancement items
    public static final TagKey<Item> BUFF = create("alchemy_buff");

    // 武器 (Weapon) - Weapons
    public static final TagKey<Item> WEAPON = create("alchemy_weapon");

    // 防具 (Armor) - Armor
    public static final TagKey<Item> ARMOR = create("alchemy_armor");

    // 饰品 (Accessory) - Accessories
    public static final TagKey<Item> ACCESSORY = create("alchemy_accessory");

    // 符文 (Rune) - Runes/charms
    public static final TagKey<Item> RUNE = create("alchemy_rune");

    // 探索 (Exploration) - Exploration items
    public static final TagKey<Item> EXPLORATION = create("alchemy_exploration");

    private static TagKey<Item> create(String name) {
        return TagKey.create(Registries.ITEM, AtelierSteve.id(name));
    }

    /**
     * Parse category code to TagKey.
     * @param code Category code (调, 攻, 恢, 削, 强, 武, 防, 饰, 符, 探)
     * @return Corresponding TagKey or null if invalid
     */
    public static TagKey<Item> fromCode(String code) {
        return switch (code) {
            case "调" -> SYNTHESIS;
            case "攻" -> ATTACK;
            case "恢" -> RECOVERY;
            case "削" -> DEBUFF;
            case "强" -> BUFF;
            case "武" -> WEAPON;
            case "防" -> ARMOR;
            case "饰" -> ACCESSORY;
            case "符" -> RUNE;
            case "探" -> EXPLORATION;
            default -> null;
        };
    }
}
