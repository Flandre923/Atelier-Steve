import json
import csv
import re

# 读取HTML文件
with open("page_source.html", "r", encoding="utf-8") as f:
    html_content = f.read()

# 方法1: 尝试提取完整的JSON数组
print("方法1: 查找JSON数组...")
json_array_pattern = r'"traits":\s*(\[[^\]]+\])'
match = re.search(json_array_pattern, html_content, re.DOTALL)

traits_data = []

if match:
    try:
        traits_data = json.loads(match.group(1))
        print(f"从JSON数组中提取到 {len(traits_data)} 个特性")
    except:
        print("JSON数组解析失败")

# 方法2: 逐个提取JSON对象（更可靠）
if len(traits_data) < 300:
    print("\n方法2: 逐个提取JSON对象...")

    # 找到所有完整的特性对象
    # 改进的正则表达式，能匹配包含combo的复杂对象
    pos = 0
    while True:
        # 查找下一个特性对象的开始
        start = html_content.find('{"slug":"', pos)
        if start == -1:
            break

        # 找到对应的结束括号
        brace_count = 0
        i = start
        in_string = False
        escape = False

        while i < len(html_content):
            char = html_content[i]

            if escape:
                escape = False
                i += 1
                continue

            if char == "\\":
                escape = True
                i += 1
                continue

            if char == '"':
                in_string = not in_string
            elif not in_string:
                if char == "{":
                    brace_count += 1
                elif char == "}":
                    brace_count -= 1
                    if brace_count == 0:
                        # 找到完整对象
                        json_str = html_content[start : i + 1]
                        try:
                            trait = json.loads(json_str)
                            # 检查是否是特性对象（必须有name字段）
                            if "name" in trait and "slug" in trait:
                                traits_data.append(trait)
                        except:
                            pass
                        break
            i += 1

        pos = i + 1
        if pos >= len(html_content):
            break

    print(f"逐个提取到 {len(traits_data)} 个特性对象")

# 去重（根据slug）
seen_slugs = set()
unique_traits = []
for trait in traits_data:
    slug = trait.get("slug", "")
    if slug and slug not in seen_slugs:
        seen_slugs.add(slug)
        unique_traits.append(trait)

print(f"\n去重后共 {len(unique_traits)} 个唯一特性")

# 按grade排序
unique_traits.sort(key=lambda x: (x.get("grade", 0), x.get("slug", "")))

# 转换为CSV格式
csv_data = []
for i, trait in enumerate(unique_traits, 1):
    name = trait.get("name", "")
    grade = trait.get("grade", 0)
    effect = trait.get("desc", "").replace("  ", " ").strip()

    # 解析可继承物类型
    categories = []
    if trait.get("trans_syn", False):
        categories.append("调")
    if trait.get("trans_atk", False):
        categories.append("攻")
    if trait.get("trans_heal", False):
        categories.append("恢")
    if trait.get("trans_dbf", False):
        categories.append("削")
    if trait.get("trans_buff", False):
        categories.append("强")
    if trait.get("trans_wpn", False):
        categories.append("武")
    if trait.get("trans_arm", False):
        categories.append("防")
    if trait.get("trans_acc", False):
        categories.append("饰")
    if trait.get("trans_tal", False):
        categories.append("符")
    if trait.get("trans_exp", False):
        categories.append("探")

    categories_str = "、".join(categories)

    combo = ""
    combo1 = trait.get("combo1")
    combo2 = trait.get("combo2")
    if isinstance(combo1, dict) and isinstance(combo2, dict):
        combo1_name = combo1.get("name", "").strip()
        combo2_name = combo2.get("name", "").strip()
        if combo1_name and combo2_name:
            combo = f"{combo1_name} × {combo2_name}"

    csv_data.append(
        {
            "序号": i,
            "名称": name,
            "Grade": grade,
            "效果": effect,
            "可继承物": categories_str,
            "组合": combo,
        }
    )

# 保存为CSV
with open("traits.csv", "w", encoding="utf-8-sig", newline="") as f:
    writer = csv.DictWriter(
        f, fieldnames=["序号", "名称", "Grade", "效果", "可继承物", "组合"]
    )
    writer.writeheader()
    writer.writerows(csv_data)

print(f"\n数据已保存到 traits.csv，共 {len(csv_data)} 条记录")

# 打印前10条和后10条作为示例
print("\n前10条数据示例：")
for row in csv_data[:10]:
    print(f"{row['序号']}\t{row['名称']}\tGrade:{row['Grade']}\t{row['可继承物']}")

print("\n后10条数据示例：")
for row in csv_data[-10:]:
    print(f"{row['序号']}\t{row['名称']}\tGrade:{row['Grade']}\t{row['可继承物']}")
