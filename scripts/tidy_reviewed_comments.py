"""Remove only comment blocks belonging to members removed by this audit."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def edit(relative, transform):
    path = ROOT / relative
    with path.open(encoding="utf-8", newline="") as handle:
        before = handle.read()
    line_ending = '\r\n' if '\r\n' in before else '\n'
    after = transform(before.replace('\r\n', '\n')).replace('\n', line_ending)
    if before != after:
        with path.open("w", encoding="utf-8", newline="") as handle:
            handle.write(after)


def cut(text, first, after):
    first, after = first.replace('\r\n', '\n'), after.replace('\r\n', '\n')
    start = text.index(first)
    end = text.index(after, start)
    return text[:start] + text[end:]


def mafft(text):
    text = cut(text, '//\t\t\tstrategy_Q_INS_i_JRadioButton =', '\t\t\tButtonGroup strategyButtonGroup')
    text = re.sub(r'(?m)^\s*// strategyButtonGroup\.add\(strategy_Q_INS_i_JRadioButton\);\r?\n', '', text)
    text = cut(text, '//\t\t\tgridBagConstraints.gridx = 0;\r\n//\t\t\tgridBagConstraints.gridy = 10;', '\t\t\tstrategyJXTaskPane.add(strategyJPane);')
    text = cut(text, '\t\t\t// parametersAminoAcidSequencesJLabel =', '\t\t\tJPanel parametersGapJPanel')
    text = cut(text, '//\t\t\tJLabel parametersScoreJLabel =', '\t\t\tJPanel parametersJPane =')
    anchor = text.index('\t\t\tJPanel parametersJPane =')
    prefix, tail = text[:anchor], text[anchor:]
    tail = cut(tail, '\t\t\t// gridBagConstraints.gridx = 0;', '\t\t\tgridBagConstraints.weightx = 0.0;')
    tail = cut(tail, '//\t\t\tgridBagConstraints.gridx = 0;\r\n//\t\t\tgridBagConstraints.gridy = 4;', '\t\t\tJPanel parametersJPane1 =')
    return prefix + tail


edit('src/module/multiseq/aligner/gui/MafftGUIPanel.java', mafft)
edit('src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java', lambda text: re.sub(
    r'(?m)^//[^\r\n]*pasteNodeAsSiblingJMenuItem[^\r\n]*\r?\n', '',
    cut(text, '// 这个功能暂时都去掉了', '\tpublic void displayMoreInformation4leaf')))
edit('src/module/evolview/gfamily/work/listener/TreeListener.java', lambda text: re.sub(
    r'(?m)^//\s*logger\.info\("The dis is \{\}"[^\r\n]*\r?\n', '', text))
edit('src/module/evolview/pathwaybrowser/gui/ControlPanelContainer.java', lambda text: re.sub(
    r'(?m)^//[^\r\n]*getColorSchemeTaskPanel\(\)[^\r\n]*\r?\n', '', text))
edit('src/module/evolview/phylotree/visualization/layout/RectHalfCircleLayout.java', lambda text: re.sub(
    r'\t\t// for debug\r?\n//\s*backGround\(g2d\);\r?\n', '', text))
edit('src/module/webmsaoperator/webIO/RestGenomicRegionOneStep.java', lambda text: re.sub(
    r'\t/\*public boolean getRegionStringMultiStep\(\) \{.*?\}\*/', '', text, flags=re.S))
for relative in ('MafTargetBlockFinder.java', 'MafPositionConvetor.java'):
    edit('src/module/remnant/mafoperator/mafParser/' + relative, lambda text: re.sub(
        r'(?m)^//[^\r\n]*numOfLines[^\r\n]*\r?\n', '', text))
edit('src/module/evolview/gfamily/GeneFamilyController.java', lambda text: re.sub(
    r'(?m)^//[^\r\n]*isAllNodesAreLeaf\(currentNode\)[^\r\n]*\r?\n', '', text))

# Drop named imports newly orphaned by deleting the reviewed methods/comments.
imports = {
    'src/module/evolview/gfamily/work/listener/TreeListener.java': ['org.slf4j.Logger', 'org.slf4j.LoggerFactory'],
    'src/module/evolview/genebrowser/GBControlPanelContainner.java': ['module.evolview.gfamily.work.gui.CtrlSignalingComps'],
    'src/module/remnant/datapanel/RightSuitableMethodsArea.java': ['javax.swing.border.EmptyBorder'],
}
for relative, names in imports.items():
    for name in names:
        edit(relative, lambda text, name=name: text.replace('import ' + name + ';\n', ''))
