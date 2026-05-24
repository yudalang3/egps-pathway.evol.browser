package module.evolview.pathwaybrowser.gui.analysis.panel;

import java.util.List;
import java.util.Map;

public class AnalysisPanelDataPreparationTest {

    public static void main(String[] args) {
        verifiesSpeciesInfoTablePreparation();
        verifiesNameRowPreparation();
    }

    private static void verifiesSpeciesInfoTablePreparation() {
        SpeciesInfoPanel.ParsedTsv parsed = new SpeciesInfoPanel.ParsedTsv(
                List.of("Name", "Score", "Note"),
                List.of(
                        List.of("alpha", "1.5", "first"),
                        List.of("beta", "2.0"),
                        List.of("alpha", "9.0", "duplicate")
                )
        );

        SpeciesInfoPanel.PreparedTableData prepared = SpeciesInfoPanel.prepareTableData(parsed);

        if (prepared.nameColumnIndex() != 0) {
            throw new AssertionError("Expected Name column at index 0");
        }
        if (prepared.data().length != 3 || prepared.data()[0].length != 3) {
            throw new AssertionError("Unexpected prepared table dimensions");
        }
        if (!"".equals(prepared.data()[1][2])) {
            throw new AssertionError("Missing cells must be normalized to empty strings");
        }
        Map<String, Integer> index = prepared.name2ModelRowIndex();
        if (!Integer.valueOf(0).equals(index.get("alpha"))) {
            throw new AssertionError("Duplicate names must keep first model row index");
        }
        if (!Integer.valueOf(1).equals(index.get("beta"))) {
            throw new AssertionError("Expected beta model row index 1");
        }
    }

    private static void verifiesNameRowPreparation() {
        AbstractTsvBasedAnalysisPanel.ParsedTsv parsed = new AbstractTsvBasedAnalysisPanel.ParsedTsv(
                List.of("Trait", "Name", "Value"),
                List.of(
                        List.of("color", "alpha", "red"),
                        List.of("size", "beta", "large"),
                        List.of("ignored", "", "blank name")
                )
        );

        AbstractTsvBasedAnalysisPanel.PreparedNameRows prepared = AbstractTsvBasedAnalysisPanel.prepareNameRows(parsed);

        if (prepared.nameColumnIndex() != 1) {
            throw new AssertionError("Expected Name column at index 1");
        }
        if (!List.of("Trait", "Name", "Value").equals(prepared.headers())) {
            throw new AssertionError("Headers changed during preparation");
        }
        if (!List.of("color", "alpha", "red").equals(prepared.name2RowData().get("alpha"))) {
            throw new AssertionError("Expected alpha row data");
        }
        if (prepared.name2RowData().containsKey("")) {
            throw new AssertionError("Blank names must not be indexed");
        }
    }
}
