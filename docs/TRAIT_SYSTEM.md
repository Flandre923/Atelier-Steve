# 炼金特性系统 (Alchemy Trait System)

## 概述

炼金特性系统是一个灵活、可扩展的系统，用于为炼金物品添加各种效果。系统包含334个预定义特性，并支持通过数据文件和代码扩展。

## 系统架构

### 核心组件

1. **TraitDefinition** - 特性定义，包含特性的基本信息（ID、名称、等级、描述、可继承物品类别）
2. **TraitEffect** - 特性效果接口，定义特性的实际效果
3. **AlchemyTrait** - 特性接口，用于应用和移除效果
4. **TraitRegistry** - 特性注册表，管理所有特性
5. **TraitManager** - 特性管理器，处理特性的应用和移除
6. **ItemCategory** - 物品分类标签系统

### 物品分类

系统使用Minecraft的Tag系统来分类物品：

- **调** (Synthesis) - 调合物品
- **攻** (Attack) - 攻击物品
- **恢** (Recovery) - 恢复物品
- **削** (Debuff) - 削弱物品
- **强** (Buff) - 强化物品
- **武** (Weapon) - 武器
- **防** (Armor) - 防具
- **饰** (Accessory) - 饰品
- **符** (Rune) - 符文
- **探** (Exploration) - 探索物品

## 数据文件

### 特性数据 (traits.json)

位置: `src/main/resources/data/atelier_steve/traits/traits.json`

格式:
```json
{
  "traits": [
    {
      "id": "low_price",
      "name": "低价",
      "grade": 1,
      "description": "店头的收购价格降低10%。",
      "categories": ["调", "攻", "恢", "削", "强", "武", "防", "饰", "符", "探"]
    }
  ]
}
```

### 物品标签 (Item Tags)

位置: `src/main/resources/data/atelier_steve/tags/item/`

文件:
- `alchemy_synthesis.json` - 调合物品
- `alchemy_attack.json` - 攻击物品
- `alchemy_recovery.json` - 恢复物品
- 等等...

## 如何添加新特性

### 方法1: 通过数据文件（推荐）

1. 编辑 `tools/traits.csv` 添加新特性
2. 运行 `python tools/convert_traits.py` 生成JSON
3. 运行 `python tools/generate_lang.py` 生成语言文件

### 方法2: 通过代码

```java
// 在TraitRegistry中注册
TraitRegistry.register(new TraitDefinition.Builder()
    .id(AtelierSteve.id("my_trait"))
    .nameKey("trait.atelier_steve.my_trait")
    .grade(10)
    .descriptionKey("trait.atelier_steve.my_trait.desc")
    .inheritableCategories(Set.of(ItemCategory.WEAPON, ItemCategory.ARMOR))
    .effects(List.of(
        // 添加效果
    ))
    .build());
```

## 如何添加新效果类型

### 1. 创建效果类

```java
public class MyCustomEffect implements TraitEffect {
    public static final TraitEffectType<MyCustomEffect> TYPE = new Type();

    private final double value;

    public MyCustomEffect(double value) {
        this.value = value;
    }

    @Override
    public TraitEffectType<?> getType() {
        return TYPE;
    }

    @Override
    public void apply(LivingEntity entity, ItemStack stack) {
        // 实现效果应用逻辑
    }

    @Override
    public void remove(LivingEntity entity, ItemStack stack) {
        // 实现效果移除逻辑
    }

    @Override
    public String getDescription() {
        return "My effect: " + value;
    }

    private static class Type implements TraitEffectType<MyCustomEffect> {
        @Override
        public String getId() {
            return "my_custom_effect";
        }

        @Override
        public MyCustomEffect create(Object... params) {
            return new MyCustomEffect((Double) params[0]);
        }
    }
}
```

### 2. 使用效果

```java
TraitDefinition trait = new TraitDefinition.Builder()
    .id(AtelierSteve.id("my_trait"))
    // ... 其他属性
    .effects(List.of(
        new MyCustomEffect(10.0)
    ))
    .build();
```

## 内置效果类型

### AttributeModifierEffect

修改实体属性（HP、攻击力、防御力等）

```java
new AttributeModifierEffect(
    Attributes.MAX_HEALTH,  // 属性
    20.0,                   // 数值
    AttributeModifier.Operation.ADD_VALUE,  // 操作类型
    "my_modifier_id"        // 修改器ID
)
```

### PotionEffect

应用药水效果

```java
new PotionEffect(
    MobEffects.REGENERATION,  // 效果类型
    0,                        // 等级（0 = I级）
    200,                      // 持续时间（tick）
    false,                    // 是否环境效果
    true                      // 是否显示粒子
)
```

## 特性应用流程

1. 物品被装备/使用时，调用 `TraitManager.applyTraits(entity, stack)`
2. 系统读取物品的特性列表
3. 对每个特性，创建对应的 `AlchemyTrait` 实例
4. 调用 `trait.apply(entity, stack)` 应用效果
5. 物品被卸下时，调用 `TraitManager.removeTraits(entity, stack)` 移除效果

## 特性继承

特性可以根据物品的分类标签进行继承。使用 `TraitManager.canInheritTrait(stack, trait)` 检查物品是否可以继承特定特性。

## 工具脚本

### convert_traits.py

将CSV格式的特性数据转换为JSON格式。

用法: `python tools/convert_traits.py`

### generate_lang.py

从traits.json生成语言文件（中文和英文）。

用法: `python tools/generate_lang.py`

## 扩展性设计

系统设计时考虑了以下扩展点：

1. **效果系统** - 通过实现 `TraitEffect` 接口添加新效果类型
2. **数据驱动** - 特性定义存储在JSON文件中，易于修改
3. **分类系统** - 使用Minecraft Tag系统，可以灵活定义物品分类
4. **自定义实现** - 可以为特定特性创建自定义 `AlchemyTrait` 实现

## 未来计划

- [ ] 实现更多内置效果类型
- [ ] 添加特性合成系统
- [ ] 实现特性品质系统
- [ ] 添加特性冲突检测
- [ ] 实现特性升级机制
- [ ] 添加特性可视化UI

## 示例

### 创建一个简单的攻击力提升特性

```java
// 1. 定义特性
TraitDefinition attackBoost = new TraitDefinition.Builder()
    .id(AtelierSteve.id("attack_boost_10"))
    .nameKey("trait.atelier_steve.attack_boost_10")
    .grade(5)
    .descriptionKey("trait.atelier_steve.attack_boost_10.desc")
    .inheritableCategories(Set.of(ItemCategory.WEAPON))
    .effects(List.of(
        new AttributeModifierEffect(
            Attributes.ATTACK_DAMAGE,
            10.0,
            AttributeModifier.Operation.ADD_VALUE,
            "attack_boost_10_modifier"
        )
    ))
    .build();

// 2. 注册特性
TraitRegistry.register(attackBoost);

// 3. 应用到物品
// (通过炼金系统添加到物品的特性列表)
```

## 注意事项

1. 特性ID必须唯一
2. 效果修改器ID也必须唯一，避免冲突
3. 特性等级(Grade)范围: 1-70
4. 添加新特性后需要重新生成语言文件
5. 修改Tag文件后需要重启游戏才能生效
