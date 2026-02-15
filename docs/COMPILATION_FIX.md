# 编译错误修复说明

## 修复的问题

### 1. Attribute类型错误
**问题**: Minecraft 1.21中，`getAttribute()` 方法需要 `Holder<Attribute>` 而不是 `Attribute`

**修复**:
- 将 `AttributeModifierEffect` 中的 `Attribute` 类型改为 `Holder<Attribute>`
- 添加 `import net.minecraft.core.Holder;`
- 使用 `attribute.value()` 访问实际的Attribute对象

### 2. AttributeModifier ID类型错误
**问题**: `AttributeModifier` 构造函数需要 `ResourceLocation` 而不是 `String`

**修复**:
- 将 `modifierId` 类型从 `String` 改为 `ResourceLocation`
- 添加 `import net.minecraft.resources.ResourceLocation;`

### 3. MobEffect类型错误
**问题**: Minecraft 1.21中，药水效果相关方法需要 `Holder<MobEffect>` 而不是 `MobEffect`

**修复**:
- 将 `PotionEffect` 中的 `MobEffect` 类型改为 `Holder<MobEffect>`
- 使用 `effect.value()` 访问实际的MobEffect对象

### 4. EventBusSubscriber警告
**问题**: `EventBusSubscriber.Bus.GAME` 已过时

**修复**:
- 移除 `bus = EventBusSubscriber.Bus.GAME` 参数
- 默认使用GAME总线

## 修改的文件

1. `src/main/java/com/ateliersteve/alchemy/trait/effects/AttributeModifierEffect.java`
2. `src/main/java/com/ateliersteve/alchemy/trait/effects/PotionEffect.java`
3. `src/main/java/com/ateliersteve/event/ResourceReloadHandler.java`

## 编译结果

✅ 编译成功，无错误
⚠️ 有一些unchecked警告（正常，可以忽略）

## 使用示例更新

由于API变化，使用这些效果时需要传入Holder类型：

```java
// 旧代码（不再工作）
new AttributeModifierEffect(
    Attributes.MAX_HEALTH,
    20.0,
    AttributeModifier.Operation.ADD_VALUE,
    "my_modifier"
);

// 新代码（正确）
new AttributeModifierEffect(
    Attributes.MAX_HEALTH,  // 这已经是Holder<Attribute>类型
    20.0,
    AttributeModifier.Operation.ADD_VALUE,
    AtelierSteve.id("my_modifier")  // 使用ResourceLocation
);
```

## 注意事项

1. 在Minecraft 1.21+中，许多注册表对象都使用Holder包装
2. 修改器ID必须使用ResourceLocation而不是字符串
3. 访问Holder中的实际对象使用 `.value()` 方法
