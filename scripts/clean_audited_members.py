"""Apply only the reviewed, zero-reference declarations from a semantic snapshot.

Run once against before-cleanup; never infer deletions from a type name or a package name.
The UTF-16 conversion matches javac source positions and preserves original line endings.
"""
import argparse
import csv
import hashlib
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FIELDS = {
    "module.webmsaoperator.webIO.RestGenomicRegionOneStep": "processIndex",
    "module.evoltreio.parser.FormatAwareNewickParser": "FORBIDDEN_CHARS_IN_UNQUOTED_NAME",
    "module.evolview.genebrowser.GBControlPanelContainner": "globalFont",
    "module.evolview.gfamily.work.listener.TreeListener": "logger",
    "module.evolview.gfamily.work.gui.CtrlGenomeBrowserPanel": "moveUnitLength",
    "module.evolview.gfamily.work.gui.render.CustomizedRender": "TYPE_SELF",
    "module.evolview.gfamily.work.gui.browser.TrackKeyDomains": "logger",
    "module.evolview.gfamily.work.gui.browser.MouseListenerGeneStructure": "trackHeight",
    "module.evolview.gfamily.work.gui.browser.CalculatorAlleleFreq": "maxtick",
    "module.evolview.gfamily.work.gui.tree.TreePopupMenu": "pasteNodeAsSiblingJMenuItem",
    "module.remnant.datapanel.VerticalLayout": "comps",
    "module.evolview.pathwaybrowser.GuiCompCreator": "log",
    "module.evolview.pathwaybrowser.io.Voice4pathwayFamilyBrowser": "log",
    "module.evolview.pathwaybrowser.gui.EvoSelectionPressurePanel": "firstSlide",
    "module.pill.core.DrawingPanelSkeletonMaker": "logger",
    "module.multiseq.aligner.gui.MafftGUIPanel": " ".join((
        "mainSplitPane alignUnrelatedSegmentsJXTaskPane",
        "alignUnrelatedSegmentsAlignGappyRegionsJRadioButton alignUnrelatedSegmentsLeaveGappyRegionsJRadioButton",
        "parametersAminoAcidSequencesJLabel parametersAminoAcidSequencesJComboBox",
        "parametersNucleotideSequencesJLabel parametersNucleotideSequencesJComboBox",
        "parametersScoreEffectJRadioButton parametersScoreTreatedJRadioButton",
        "guideTreeJXTaskPane guideTreeDefaultJRadioButton guideTreeUpgmaJRadioButton guideTreeOutputJCheckBox",
    )),
}
METHODS = {
    "module.evoltre.mutation.MutationOperator": "isMutationStr",
    "module.evolview.genebrowser.GBControlPanelContainner": "getComponentsPanel",
    "module.evolview.gfamily.GeneFamilyController": "isAllNodesAreLeaf",
    "module.evolview.gfamily.work.CGBNodeUtil": "inferMutationsForBinaryForkRemoveOneNode",
    "module.evolview.gfamily.work.gui.tree.TreePopupMenu": "getExportItem getRefreshMutationFreq",
    "module.evolview.pathwaybrowser.gui.ControlPanelContainer": "getColorSchemeTaskPanel",
    "module.evolview.phylotree.visualization.layout.RectHalfCircleLayout": "backGround",
    "module.evolview.phylotree.visualization.primary.swing.FastestLeftRectangularLayoutWithSizeCalculator":
        "getButtomMostLeafNode getTopMostLeafNode",
    "module.multiseq.alignment.view.io.PDF2AlignmentUtil": "exportDataWithContinuousAll",
    "module.remnant.datapanel.RightSuitableMethodsArea": "addButton",
    "module.remnant.treeoperator.io.TreeDecoder": "getIntValue",
    "module.remnant.treeoperator.reconAlgo.Upgma": "printDistanceMatrix",
}
LOCALS = {
    "module.evoltreio.parser.FormatAwareNewickParser": "start",
    "module.multiseq.aligner.gui.MafftGUIPanel": "string1 string2",
    "module.remnant.mafoperator.mafParser.MafTargetBlockFinder": "numOfLines",
    "module.remnant.mafoperator.mafParser.MafPositionConvetor": "numOfLines",
    "module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator": "success",
}


def symbol_keys(mapping):
    return {owner + "#" + name for owner, names in mapping.items() for name in names.split()}


def read_source(path):
    with path.open(encoding="utf-8", newline="") as handle:
        return handle.read()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="Otherwise print the reviewed selection only")
    args = parser.parse_args()
    selections = {"private-field": symbol_keys(FIELDS), "private-method": symbol_keys(METHODS), "unread-local": symbol_keys(LOCALS)}
    snapshot = ROOT / "logs/code-lifecycle/before-cleanup/candidates.tsv"
    with snapshot.open(encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    with snapshot.with_name("source-hashes.tsv").open(encoding="utf-8", newline="") as handle:
        hashes = {row["file"]: row["sha256"] for row in csv.DictReader(handle, delimiter="\t")}
    edits = defaultdict(list)
    matched = set()
    for row in rows:
        reason = row["reason"]
        if reason not in selections:
            continue
        key = re.sub(r"#(?:<[^>]+>)?([^#(]+)\(.*", r"#\1", row["symbol"])
        if key not in selections[reason]:
            continue
        if int(row["reads"]) or int(row["writes"]):
            raise ValueError(f"Declaration is still used: {key}")
        if key in matched:
            raise ValueError(f"Ambiguous declaration: {key}")
        matched.add(key)
        path = ROOT / row["file"]
        if hashlib.sha256(path.read_bytes()).hexdigest() != hashes[row["file"]]:
            raise ValueError(f"Source changed since snapshot: {path}")
        original = read_source(path)
        # javac positions count UTF-16 code units, not Python Unicode code points.
        utf16 = original.encode("utf-16-le")
        start = len(utf16[:int(row["start"]) * 2].decode("utf-16-le"))
        end = len(utf16[:int(row["end"]) * 2].decode("utf-16-le"))
        declaration = original[start:end]
        if reason == "private-method" and not re.search(r"\bprivate\b", declaration):
            raise ValueError(f"Source no longer matches snapshot: {key}")
        replacement = ""
        if key.endswith("#success"):
            replacement = declaration.split("=", 1)[1].strip()
            if replacement != "newroot.setChildAt(i, clone(child));":
                raise ValueError("Unexpected side-effecting initializer")
        else:
            # Remove an attached Javadoc only when it belongs to this declaration.
            doc = re.search(r"/\*\*(?:(?!\*/).)*\*/[ \t\r\n]*$", original[:start], re.S)
            if doc:
                start = doc.start()
            line_start = original.rfind("\n", 0, start) + 1
            if not original[line_start:start].strip():
                start = line_start
            line_end = original.find("\n", end)
            if line_end != -1 and not original[end:line_end].strip():
                end = line_end + 1
        edits[path].append((start, end, replacement, row))
    missing = set.union(*selections.values()) - matched
    if missing:
        raise ValueError(f"Reviewed declarations missing from snapshot: {sorted(missing)}")
    ledger = []
    for path, changes in sorted(edits.items()):
        original = read_source(path)
        before = original
        previous = len(original) + 1
        for start, end, replacement, row in sorted(changes, reverse=True):
            if end > previous:
                raise ValueError(f"Overlapping changes in {path}")
            original = original[:start] + replacement + original[end:]
            previous = start
            ledger.append({"file": row["file"], "line": int(row["line"]), "kind": row["kind"],
                           "symbol": row["symbol"], "action": "preserve-initializer" if replacement else "remove",
                           "evidence": "javac resolved reads=0, writes=0; reviewed allowlist"})
        # Only remove named imports that THIS edit made unused. Keep wildcards,
        # static imports and pre-existing unused imports outside this change.
        for imported in list(re.finditer(r"(?m)^import ([\w.]+);[^\S\r\n]*\r?\n", original)):
            name = imported.group(1).rsplit(".", 1)[-1]
            pattern = r"\b" + re.escape(name) + r"\b"
            if len(re.findall(pattern, original)) == 1 and len(re.findall(pattern, before)) > 1:
                original = original.replace(imported.group(0), "", 1)
        if args.apply:
            with path.open("w", encoding="utf-8", newline="") as handle:
                handle.write(original)
    if args.apply:
        log = ROOT / "logs/code-lifecycle/cleanup-ledger.json"
        log.write_text(json.dumps(ledger, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Reviewed changes: {len(ledger)} declarations in {len(edits)} files; apply={args.apply}")


if __name__ == "__main__":
    main()
