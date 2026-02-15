import requests
from bs4 import BeautifulSoup
import csv
import json

def scrape_traits():
    url = "https://barrelwisdom.com/sophie2/traits/sc"

    # 发送请求
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }
    response = requests.get(url, headers=headers)
    response.encoding = 'utf-8'

    soup = BeautifulSoup(response.text, 'html.parser')

    # 查找所有特性数据
    traits = []

    # 尝试找到特性表格或列表
    # 根据网页结构调整选择器
    trait_elements = soup.find_all('tr', class_='trait-row')  # 假设的类名

    if not trait_elements:
        # 如果没有找到，尝试其他方式
        trait_elements = soup.find_all('div', class_='trait')

    print(f"找到 {len(trait_elements)} 个特性元素")

    # 保存HTML以便调试
    with open('page_source.html', 'w', encoding='utf-8') as f:
        f.write(response.text)

    return traits

def save_to_csv(traits, filename='traits.csv'):
    with open(filename, 'w', encoding='utf-8-sig', newline='') as f:
        writer = csv.writer(f)
        # 写入表头
        writer.writerow(['序号', '名称', '效果', '可继承物'])

        for trait in traits:
            writer.writerow([
                trait.get('id', ''),
                trait.get('name', ''),
                trait.get('effect', ''),
                trait.get('categories', '')
            ])

if __name__ == '__main__':
    print("开始抓取特性数据...")
    traits = scrape_traits()
    print(f"抓取到 {len(traits)} 个特性")

    if traits:
        save_to_csv(traits)
        print("数据已保存到 traits.csv")
    else:
        print("未能抓取到数据，请检查 page_source.html 查看网页结构")
