import json

def generate_lang_files():
    """Generate language files from traits.json"""

    # Load traits data
    with open('src/main/resources/data/atelier_steve/traits/traits.json', 'r', encoding='utf-8') as f:
        data = json.load(f)

    # Generate Chinese (Simplified) language file
    zh_cn = {}

    for trait in data['traits']:
        trait_id = trait['id']
        name = trait['name']
        description = trait['description']

        zh_cn[f"trait.atelier_steve.{trait_id}"] = name
        zh_cn[f"trait.atelier_steve.{trait_id}.desc"] = description

    # Add category translations
    zh_cn.update({
        "category.atelier_steve.synthesis": "调合",
        "category.atelier_steve.attack": "攻击",
        "category.atelier_steve.recovery": "恢复",
        "category.atelier_steve.debuff": "削弱",
        "category.atelier_steve.buff": "强化",
        "category.atelier_steve.weapon": "武器",
        "category.atelier_steve.armor": "防具",
        "category.atelier_steve.accessory": "饰品",
        "category.atelier_steve.rune": "符文",
        "category.atelier_steve.exploration": "探索"
    })

    # Write Chinese language file
    with open('src/main/resources/assets/atelier_steve/lang/zh_cn.json', 'r', encoding='utf-8') as f:
        existing_zh = json.load(f)

    existing_zh.update(zh_cn)

    with open('src/main/resources/assets/atelier_steve/lang/zh_cn.json', 'w', encoding='utf-8') as f:
        json.dump(existing_zh, f, ensure_ascii=False, indent=2)

    print(f"Generated Chinese translations for {len(data['traits'])} traits")

    # Generate English language file (with placeholder translations)
    en_us = {}

    for trait in data['traits']:
        trait_id = trait['id']
        name = trait['name']
        description = trait['description']

        # For now, use Chinese names in English file too
        # These can be translated later
        en_us[f"trait.atelier_steve.{trait_id}"] = name
        en_us[f"trait.atelier_steve.{trait_id}.desc"] = description

    en_us.update({
        "category.atelier_steve.synthesis": "Synthesis",
        "category.atelier_steve.attack": "Attack",
        "category.atelier_steve.recovery": "Recovery",
        "category.atelier_steve.debuff": "Debuff",
        "category.atelier_steve.buff": "Buff",
        "category.atelier_steve.weapon": "Weapon",
        "category.atelier_steve.armor": "Armor",
        "category.atelier_steve.accessory": "Accessory",
        "category.atelier_steve.rune": "Rune",
        "category.atelier_steve.exploration": "Exploration"
    })

    # Write English language file
    with open('src/main/resources/assets/atelier_steve/lang/en_us.json', 'r', encoding='utf-8') as f:
        existing_en = json.load(f)

    existing_en.update(en_us)

    with open('src/main/resources/assets/atelier_steve/lang/en_us.json', 'w', encoding='utf-8') as f:
        json.dump(existing_en, f, ensure_ascii=False, indent=2)

    print(f"Generated English translations for {len(data['traits'])} traits")

if __name__ == '__main__':
    generate_lang_files()
