import csv
import json
import re


def parse_categories(cat_str):
    """Parse category string like '调、攻、恢' into list"""
    if not cat_str or cat_str.strip() == "":
        return []
    return [c.strip() for c in cat_str.split("、")]


def sanitize_id(name):
    """Convert Chinese name to English ID"""
    # This is a simple mapping - you may want to customize this
    id_map = {
        "低价": "low_price",
        "高价": "high_price",
        "防御强化": "defense_boost",
        "敏捷强化": "agility_boost",
        "破坏力增加": "power_increase",
        "恢复力增加": "recovery_increase",
        "攻击强化": "attack_boost",
        "消耗MP-10%": "mp_cost_reduction_10",
        "品质提升": "quality_boost",
        "HP强化": "hp_boost",
        "MP强化": "mp_boost",
        "坚固": "sturdy",
        "茁壮成长": "vigorous_growth",
        "技能威力+10%": "skill_power_10",
        "使用次数-1": "use_count_minus_1",
        # Add more mappings as needed...
    }

    # Try direct mapping first
    if name in id_map:
        return id_map[name]

    # Otherwise, create a generic ID
    # Remove special characters and convert to lowercase
    id_str = re.sub(r"[^\w\s]", "", name)
    id_str = re.sub(r"\s+", "_", id_str)
    return f"trait_{id_str.lower()}"


def parse_combo(combo_str):
    """Parse combo string like '品质提升 × 品质提升+' into list of trait names"""
    if not combo_str or combo_str.strip() == "":
        return []
    parts = [p.strip() for p in re.split(r"\s*[×xX]\s*", combo_str) if p.strip()]
    return parts


def convert_csv_to_json(csv_path, json_path):
    """Convert traits CSV to JSON format"""
    traits = []
    pending_combos = []

    with open(csv_path, "r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Skip empty rows
            if not row.get("序号") or row["序号"].strip() == "":
                continue

            trait = {
                "id": sanitize_id(row["名称"]),
                "name": row["名称"],
                "grade": int(row["Grade"]),
                "description": row["效果"],
                "categories": parse_categories(row["可继承物"]),
            }
            traits.append(trait)

            combo_names = parse_combo(row.get("组合", ""))
            if combo_names:
                pending_combos.append((trait["name"], combo_names))

    # Resolve combo names to IDs
    name_to_id = {trait["name"]: trait["id"] for trait in traits}
    trait_by_name = {trait["name"]: trait for trait in traits}

    for target_name, ingredients in pending_combos:
        if len(ingredients) < 2:
            continue
        if target_name not in trait_by_name:
            continue

        ingredient_ids = [name_to_id.get(name) for name in ingredients]
        if any(ingredient_id is None for ingredient_id in ingredient_ids):
            continue

        target_trait = trait_by_name[target_name]
        target_trait.setdefault("combinations", []).append(
            {"ingredients": ingredient_ids}
        )

    output = {"traits": traits}

    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"Converted {len(traits)} traits to JSON")


if __name__ == "__main__":
    convert_csv_to_json(
        "tools/traits.csv", "src/main/resources/data/atelier_steve/traits/traits.json"
    )
