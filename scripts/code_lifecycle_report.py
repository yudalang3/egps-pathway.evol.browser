"""Prepare a read-only Git baseline or publish the conservative lifecycle audit report."""
import argparse
import csv
import hashlib
import io
import json
import re
import subprocess
import tarfile
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOGS = ROOT / 'logs/code-lifecycle'
DOCS = ROOT / 'docs/current'
TYPE_KINDS = {'CLASS', 'INTERFACE', 'ENUM', 'ANNOTATION_TYPE', 'RECORD'}


def rows(snapshot, name='declarations.tsv'):
    with (LOGS / snapshot / name).open(encoding='utf-8-sig', newline='') as handle:
        return list(csv.DictReader(handle, delimiter='\t'))


def prepare_baseline(revision):
    destination = ROOT / 'temp/code-audit/original-source'
    archive = subprocess.run(['git', 'archive', '--format=tar', revision, 'src'], cwd=ROOT, check=True, capture_output=True).stdout
    omitted = []
    with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
        for member in tar.getmembers():
            if not member.isfile() or not member.name.endswith('.java'):
                continue
            if not (ROOT / member.name).exists():
                omitted.append(member.name)  # Preserve the user's pre-existing deletion.
                continue
            target = (destination / member.name).resolve()
            target.relative_to(destination.resolve())
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(tar.extractfile(member).read())
    LOGS.mkdir(parents=True, exist_ok=True)
    metadata = {'revision': subprocess.check_output(['git', 'rev-parse', revision], cwd=ROOT, text=True).strip(),
                'omitted_existing_deletions': omitted}
    (LOGS / 'original-source.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(destination)


def key(row):
    return row['file'], row['kind'], row['symbol'], row.get('scope', '')


def public_signature(row, hidden_types):
    owner = row['symbol'].split('#', 1)[0]
    return (not row['file'].startswith('src/test/') and '.' in owner and '$' not in owner
            and not any(owner == hidden or owner.startswith(hidden + '.') for hidden in hidden_types)
            and row['kind'] not in {'PARAMETER', 'LOCAL_VARIABLE', 'EXCEPTION_PARAMETER', 'RESOURCE_VARIABLE'}
            and ('public' in row['modifiers'] or 'protected' in row['modifiers']))


def cell(value):
    return str(value).replace('|', '\\|').replace('\n', ' ').replace('\r', ' ')


def location(row):
    # This report lives in docs/current, so repository links remain portable.
    return f"[{row['file']}:{row['line']}](../../{row['file']}#L{row['line']})"


def disposition(row):
    reason = row['reason']
    if reason == 'deprecated':
        return '兼容保留', '已有废弃契约；比例尺辅助 API 已退出鼠标链路', '迁移所有已知调用方并确定兼容窗口后再移除'
    if reason == 'confirmed-use':
        return '仍在使用', '已核实有读取或调用；同名变量按所属方法分别识别', '保留；相关回归验证'
    if reason in {'entry', 'record-storage'}:
        return '仍在使用', '动态入口或编译器生成的 record 存储，不能凭字段读取数删除', '保留；入口集合和现有回归验证'
    if reason == 'type-review':
        if row['reachable'] == 'true' or int(row['test_refs']):
            return '仍在使用', '入口图可达或测试明确引用；单文件引用不代表废弃', '保留；以模块发现和相关回归验证'
        return '证据不足', '未确认生产入口；公开类型、样例及反射用法仍可能构成契约', '先确认使用者和替代路径、补覆盖，再决定弃用'
    if reason == 'unused-parameter-review':
        return '兼容保留', '参数绑定未读，不等于调用参数无效；签名及回调契约保留', '不改变签名；私有参数须先检查调用实参副作用'
    if reason == 'unread-local':
        initializer = row['initializer'].strip()
        if int(row['writes']) == 0 and (initializer in {'true', 'false', 'null'} or re.fullmatch(r'[+-]?\d+(?:\.\d+)?[fFdDlL]?', initializer)):
            return '可清理', '未读且初始化为字面量；本轮保留在指定首批清理范围之外', '下一小批删除绑定并验证所属子系统；不改变参数或方法签名'
        return '证据不足', '绑定未读；初始化或写入可能执行 I/O、校验、GUI 构造或计算', '逐项证明可消除副作用，或只去掉绑定并保留调用'
    if row['symbol'].endswith('PrintAlignmentPanel#SCALEHEIGHT'):
        return '证据不足', '同名局部变量遮蔽该字段；所属 Swing 面板带显式 serialVersionUID，本轮保留', '单独确认面板序列化使用情况后删除字段，保留实际参与计算的局部变量'
    return '证据不足', '未读字段可能有写入或序列化约定；未进入本次零读零写白名单', '先核实写入表达式和存储兼容性，再单独清理'


def report():
    original, current = rows('original'), rows('current')
    if set(original[0]) != set(current[0]) or 'scope' not in original[0]:
        raise ValueError('Audit snapshots use different schemas; regenerate both before publishing the report')
    for source in rows('current', 'source-hashes.tsv'):
        if hashlib.sha256((ROOT / source['file']).read_bytes()).hexdigest() != source['sha256']:
            raise ValueError(f"Source changed after audit: {source['file']}")
    baseline_revision = json.loads((LOGS / 'original-source.json').read_text(encoding='utf-8'))['revision']
    current_by_key = {key(r): r for r in current}
    hidden_before = {r['symbol'].split('#', 1)[0] for r in original if r['kind'] in TYPE_KINDS and 'private' in r['modifiers']}
    hidden_after = {r['symbol'].split('#', 1)[0] for r in current if r['kind'] in TYPE_KINDS and 'private' in r['modifiers']}
    before_api = {(key(r), r['modifiers'], r['type']) for r in original if public_signature(r, hidden_before)}
    after_api = {(key(r), r['modifiers'], r['type']) for r in current if public_signature(r, hidden_after)}
    api_delta = {'removed': sorted(before_api - after_api), 'added': sorted(after_api - before_api)}
    (LOGS / 'public-api-diff.json').write_text(json.dumps(api_delta, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    before_modules = (LOGS / 'original/module-entries.txt').read_text(encoding='utf-8').splitlines()
    after_modules = (LOGS / 'current/module-entries.txt').read_text(encoding='utf-8').splitlines()
    if before_api != after_api or before_modules != after_modules or len(after_modules) != 23:
        raise AssertionError('Public signature or module entry set changed; inspect public-api-diff.json')
    removed = [r for r in original if not r['file'].startswith('src/test/') and key(r) not in current_by_key
               and r['kind'] in {'FIELD', 'METHOD', 'LOCAL_VARIABLE'} and '$' not in r['symbol'].split('#', 1)[0]]
    removed_methods = [r for r in removed if r['kind'] == 'METHOD']
    # Locals inside a removed method are part of that method, not separate cleanup wins.
    removed = [r for r in removed if r['kind'] != 'LOCAL_VARIABLE' or not any(
        m['file'] == r['file'] and int(m['start']) <= int(r['start']) < int(m['end']) for m in removed_methods)]
    removed_counts = Counter(r['kind'] for r in removed)
    feature_fields = sum(r['symbol'].endswith('#whetherScaleBarSelectedInDrag') for r in removed)
    feature_locals = sum(r['kind'] == 'LOCAL_VARIABLE' and r['file'].endswith('/TreeListener.java') for r in removed)
    candidates = [r for r in current if r['reason']]
    included = {key(r) for r in candidates}
    for row in original:
        retained = current_by_key.get(key(row))
        if row['reason'] and retained is not None and not retained['reason'] and key(retained) not in included:
            candidates.append(dict(retained, reason='confirmed-use'))
            included.add(key(retained))
    decision_counts = Counter(disposition(r)[0] for r in candidates)
    with (LOGS / 'test-results.csv').open(encoding='utf-8-sig', newline='') as handle:
        tests = list(csv.DictReader(handle))
    test_failures = [r for r in tests if r['Passed'].lower() != 'true']
    DOCS.mkdir(parents=True, exist_ok=True)
    inventory = ['# 废弃代码审计逐项清单', '',
                 '本清单记录已实施删除及全部语义候选的保留决定；候选数不是可删除数。', '',
                 '旧位置对应审计基线，保留项链接对应当前源码。读写统计来自 JDK 符号解析；注释不计作调用。', '',
                 '## 已清理的声明', '', '| 原位置 | 声明 | 处置 | 证据及验证 |', '|---|---|---|---|']
    for row in sorted(removed, key=lambda r: (r['file'], int(r['line']))):
        action = '仅删除绑定，保留 setChildAt 调用' if row['symbol'].endswith('#success') else '已清理'
        if row['file'].endswith('/TreeListener.java') and row['kind'] == 'LOCAL_VARIABLE':
            action = '随比例尺专用命中分支删除'
        inventory.append(f"| `{row['file']}:{row['line']}` | `{cell(row['symbol'])}` | {action} | 私有成员或局部绑定；原符号引用、现有测试及公共签名差异核验 |")
    inventory += ['', '## 保留的语义候选', '',
                  '| 位置 | 声明 | 用途/类别 | 生产/测试引用；读/写 | 决定 | 风险或理由 | 后续条件/验证 |',
                  '|---|---|---|---|---|---|---|']
    for row in sorted(candidates, key=lambda r: (r['file'], int(r['line']), r['symbol'])):
        decision, evidence, condition = disposition(row)
        counts = f"{row['production_refs']}/{row['test_refs']}；{row['reads']}/{row['writes']}"
        inventory.append(f"| {location(row)} | `{cell(row['symbol'])}` | {row['kind']} / {row['reason']} | {counts} | {decision} | {evidence} | {condition} |")
    (DOCS / 'code_lifecycle_inventory_zh.md').write_text('\n'.join(inventory) + '\n', encoding='utf-8')

    # Group unreachable candidate types by dependency connectivity, retaining every group.
    unreachable = {r['symbol'].split('#', 1)[0] for r in candidates if r['reason'] == 'type-review' and r['reachable'] == 'false'}
    neighbors = defaultdict(set)
    for edge in rows('current', 'type-edges.tsv'):
        a, b = edge['source'], edge['target']
        if a in unreachable and b in unreachable:
            neighbors[a].add(b); neighbors[b].add(a)
    groups, remaining = [], set(unreachable)
    while remaining:
        pending, group = [min(remaining)], set()
        while pending:
            item = pending.pop()
            if item in group:
                continue
            group.add(item); pending.extend(neighbors[item] - group)
        remaining -= group
        groups.append(sorted(group))
    (LOGS / 'unreachable-type-groups.json').write_text(json.dumps(sorted(groups, key=lambda g: (-len(g), g)), ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    group_report = ['# 缺少已知入口的历史类型组', '',
                    '按类型依赖的无向连通关系分组，用于发现互相引用的历史代码；并非精确运行时可达性证明。全部保留。', '']
    for index, group in enumerate(sorted(groups, key=lambda g: (-len(g), g)), 1):
        group_report += [f'## 组 {index}（{len(group)} 个类型）', ''] + [f'- `{name}`' for name in group] + ['']
    (DOCS / 'code_lifecycle_type_groups_zh.md').write_text('\n'.join(group_report), encoding='utf-8')

    validation = '\n'.join(f"| `{t['Test']}` | {'通过' if t['Passed'].lower() == 'true' else '失败'} |" for t in tests)
    summary = f'''# 全仓废弃代码审计与清理结果

审计范围为当前仓库全部 736 个 Java 文件，以及 `egps-mutationPre`、`egps-SeqTools`、`egps-shell`、`egps-base`、模块模板、R4eGPS 和 Py4eGPS 的相关源码；下游只读。采用保守策略，未删除公共兼容类型或整包历史实现。

Git 对比基线为 `{baseline_revision}`，同时排除用户原先已删除的旧 `graphics.struct.TreeLayoutProperties.java`。

## 本次结果

- 共移除 {removed_counts['FIELD']} 个私有字段、{removed_counts['METHOD']} 个私有方法、{removed_counts['LOCAL_VARIABLE']} 个局部绑定。其中未使用代码清理为 {removed_counts['FIELD'] - feature_fields} 个字段、{removed_counts['METHOD']} 个方法、{removed_counts['LOCAL_VARIABLE'] - feature_locals} 个局部绑定；另外 {feature_fields} 个状态字段和 {feature_locals} 个命中局部变量随比例尺交互删除，不与未使用代码重复计数。
- 比例尺鼠标移动功能已取消。比例尺区域沿用画布手势；比例尺由原有布局定位，数值、单位、缩放、开关、边界和导出逻辑保留。
- `TreeListener.getScaleBarListenerObj()` 及三个历史调整类型保留并标注废弃，只作为程序兼容接口，不再参与 GUI 事件。
- Newick 测试改用当前 `Position:` 格式和结构化位置值，失败计数非零时抛出异常。
- 完整回归发现 `TreeViewStateRegressionTest` 的旧断言也已过期；清理前 Git 源码可复现同一失败。测试已按当前控制器更新为 bootstrap 重算一次，以及不支持的坐标轴图标关闭但保留模型偏好；未因此修改业务行为。
- 独立模块入口集合保持 23 个；两个 Dockable VOICE 子面板按宿主规则排除。公共/受保护名称、参数、返回类型和修饰符快照相同。
- {len(tests)} 个自动验证入口，{len(tests) - len(test_failures)} 个通过，{len(test_failures)} 个失败。详情见下表和 `logs/code-lifecycle/test-results.csv`。

## 已明确废弃与兼容保留

| 对象 | 当前处置 | 替代路径或移除条件 |
|---|---|---|
| `ParameterAssigner` | 保留空兼容类，纠正其“未来可能废弃”的过期说明 | 距离配置用 `DistanceParameterConfigurer`，建树配置用 `AbstractDistanceBasedTreePipeline`；确认外部使用者迁移后再删 |
| `getImportBean(String)` | 保留并补充 Javadoc 废弃说明 | 使用 `getImportBean(OrganizedParameterGetter)`；保留旧字符串参数格式 |
| 比例尺旧拖动辅助 API | 保留程序兼容，GUI 不再调用 | 先迁移外部二进制使用者，再决定是否删除这些公开类型与入口 |
| `api.rpython.*` | 保留 | R 的 rJava 与 Python 的 JPype 通过字符串类名和方法名调用，不能按 Java 内部引用删除 |

## 历史类型的后续规划

| 类型或类型组 | 当前用途判断 | 本轮决定与未来条件 |
|---|---|---|
| `TreeStyle` | 空的公开占位类型 | 证据不足，保留；确认桥接与第三方插件不依赖其类名后再弃用 |
| `LeafLabelProperty`、`BootstapProperties`、`LineProperty`、`LeafLabel` | 历史标签/样式模型 | 保留；先与当前 `TreeLayoutProperties`、`TreeDrawUnit` 和注释模型核对迁移关系及存储兼容性 |
| `TreeBarplotProAssigner` | 对 `TreePropertiesAssigner.assign(...)` 的旧包装 | 保留；确认调用方可直接使用父类后，再安排接口迁移 |
| `aligner.notuse` 四个类型、`ClustalWAlignmentParameters` | 旧 ClustalW/MUSCLE GUI 与参数模型 | 整组保留；确认未来算法支持范围、外部程序配置和序列格式导入后再评审，不能连同 ClustalW 文件格式支持一起删除 |
| `GTRModelDist`、`LogDetTamuraKumar` | 无已确认入口的距离算法实现 | 保留；需要算法正确性测试和产品支持范围决定，不能用删除代替算法审查 |
| `remnant`、MAF、旧建树实现 | 同时存在在用流程与历史分支 | 保留公共类型；建树和格式解析仍有真实调用，按逐项清单评审 |

## 清单分类与分析边界

当前全部候选的处置数量：{dict(sorted(decision_counts.items()))}。完整位置、引用数、理由及移除条件见[逐项清单](code_lifecycle_inventory_zh.md)，缺少入口但互相引用的类型见[历史类型组](code_lifecycle_type_groups_zh.md)。

工具使用 JDK Compiler API 解析 736 个文件的声明和符号，分别统计生产/测试引用、读取和写入，保留写入位置及初始化表达式。record 的隐式存储字段、动态模块发现、继承回调及公共参数不因零读取而删除。类型入口图是保守的静态依赖图，不是完整运行时调用图；未做路径敏感的数据流证明，反射、序列化及未取得源码的外部插件仍需人工核查。

原工作区中 `graphics.struct.TreeLayoutProperties.java` 的删除保持原状，基线重建也明确排除该文件。当前在用的 `layout.TreeLayoutProperties` 保留。其他未纳入本批的字段、局部绑定和参数均逐项记录；带副作用的初始化需要额外核查，可安全删除的字面量绑定列为后续小批任务，不计作本轮删除成果。

## 验证结果

| 验证入口 | 结果 |
|---|---|
{validation}

`TestExcelReader` 需要外部 Excel 输入，`TestPhyloTreeEncoderDecoder` 依赖不存在的个人绝对路径且会捕获异常；两者为人工样例，不纳入自动通过计数。源码仍参与全量编译。

复现命令（项目根目录 PowerShell）：

```powershell
python scripts/code_lifecycle_report.py --prepare-baseline --baseline-revision {baseline_revision}
./scripts/audit_code_lifecycle.ps1 -Snapshot current
java -Xmx3g -cp temp/code-audit/auditor CodeLifecycleAudit temp/code-audit/original-source logs/code-lifecycle/original logs/code-lifecycle/current/external-roots.txt .
./scripts/verify_code_lifecycle.ps1 -IncludeGui
python scripts/code_lifecycle_report.py
```

过程数据在 `logs/code-lifecycle`，编译输出和截图在 `temp`。`clean_audited_members.py` 是本批受控清理记录，默认仅预览，应用前校验基线 SHA-256，不能将已过期的快照直接重放到新源码。

资源/下游全文引用与反射/序列化检索分别保存于 `logs/code-lifecycle/current/resource-and-downstream-references.txt` 和 `reflection-and-serialization.txt`。类型名字符串出现只作人工核查线索，不自动等同于运行调用。
'''
    (DOCS / 'code_lifecycle_audit_zh.md').write_text(summary, encoding='utf-8')
    print(f'Public signatures unchanged: {len(before_api)}; modules: {len(after_modules)}')
    print(f'Removed declarations: {dict(removed_counts)}; candidate decisions: {dict(decision_counts)}')
    print(f'Report: {DOCS / "code_lifecycle_audit_zh.md"}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--prepare-baseline', action='store_true')
    parser.add_argument('--baseline-revision', default='HEAD')
    arguments = parser.parse_args()
    prepare_baseline(arguments.baseline_revision) if arguments.prepare_baseline else report()
