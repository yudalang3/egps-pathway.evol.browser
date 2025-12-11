#!/bin/bash
# 编译脚本 - 包含资源文件复制
# 用法: ./compile.sh

set -e

echo "========================================="
echo "eGPS Pathway Browser 编译脚本"
echo "========================================="
echo ""

# 1. 清理旧的编译输出
echo "🧹 清理旧的编译输出..."
rm -rf out/production/egps-pathway.evol.browser
mkdir -p out/production/egps-pathway.evol.browser

# 2. 编译 Java 源代码
echo "☕ 编译 Java 源代码..."
javac -encoding UTF-8 \
      -d ./out/production/egps-pathway.evol.browser \
      -cp "dependency-egps/*" \
      $(find src -name "*.java")

if [ $? -eq 0 ]; then
    echo "✅ Java 编译成功"
else
    echo "❌ Java 编译失败"
    exit 1
fi

# 3. 复制资源文件
echo ""
echo "📦 复制资源文件（图标、配置等）..."

resource_count=0
find src/module -type f \( \
    -name "*.svg" -o \
    -name "*.png" -o \
    -name "*.jpg" -o \
    -name "*.gif" -o \
    -name "*.ico" -o \
    -name "*.txt" -o \
    -name "*.properties" -o \
    -name "*.xml" -o \
    -name "*.json" -o \
    -name "*.html" -o \
    -name "*.fas" -o \
    -name "*.fasta" -o \
    -name "*.fa" \
\) | while read file; do
    # 构建目标路径
    target=${file/src\//out\/production\/egps-pathway.evol.browser\/}
    # 创建目标目录
    mkdir -p "$(dirname "$target")"
    # 复制文件
    cp "$file" "$target"
    resource_count=$((resource_count + 1))
done

echo "✅ 资源文件复制完成"

# 4. 统计编译结果
echo ""
echo "========================================="
echo "📊 编译统计"
echo "========================================="
echo "Java 类文件: $(find out/production/egps-pathway.evol.browser -name "*.class" | wc -l) 个"
echo "资源文件: $(find out/production/egps-pathway.evol.browser -type f \( -name "*.svg" -o -name "*.png" -o -name "*.jpg" -o -name "*.gif" -o -name "*.ico" -o -name "*.txt" -o -name "*.properties" -o -name "*.html" -o -name "*.fas" \) | wc -l) 个"
echo "  - 图标: $(find out/production/egps-pathway.evol.browser -type f \( -name "*.svg" -o -name "*.png" -o -name "*.jpg" -o -name "*.gif" -o -name "*.ico" \) | wc -l) 个"
echo "  - HTML手册: $(find out/production/egps-pathway.evol.browser -name "*.html" | wc -l) 个"
echo "  - 数据文件: $(find out/production/egps-pathway.evol.browser -name "*.fas" | wc -l) 个"
echo ""
echo "✅ 编译完成！"
echo ""
echo "运行命令："
echo "  开发模式: java -cp \"out/production/egps-pathway.evol.browser:dependency-egps/*\" -Xmx12g @eGPS.args egps2.Launcher4Dev"
echo "  生产模式: java -cp \"out/production/egps-pathway.evol.browser:dependency-egps/*\" -Xmx12g @eGPS.args egps2.Launcher"
echo ""
