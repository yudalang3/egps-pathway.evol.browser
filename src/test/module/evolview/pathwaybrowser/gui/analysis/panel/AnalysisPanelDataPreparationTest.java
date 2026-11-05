package module.evolview.pathwaybrowser.gui.analysis.panel;

import java.util.List;
import java.util.Map;

public class AnalysisPanelDataPreparationTest {

    public static void main(String[] args) throws Exception {
        verifiesSpeciesInfoTablePreparation();
        verifiesNameRowPreparation();
        verifiesInteractivePptxAndSvgRendering();
    }

    private static void verifiesInteractivePptxAndSvgRendering() throws Exception {
        var countFile = new java.io.File("src/module/evolview/pathwaybrowser/io/examples/components.info.tsv");
        var countData = module.evolview.pathwaybrowser.io.PathwayComponentCountData.load(countFile);
        if (countData.isEmpty() || countData.getSpeciesNames().size() != 9) {
            throw new AssertionError("Expected 9 species in components.info.tsv, got " + countData.getSpeciesNames());
        }
        long t0 = System.currentTimeMillis();
        var wntSlide = PptxSlideRenderer.prepareSlide(
                "src/module/evolview/pathwaybrowser/io/examples/wnt_pathway2.pptx",
                countData,
                () -> false
        );
        long t1 = System.currentTimeMillis();
        if (wntSlide == null || !wntSlide.hasDynamicElements()) {
            throw new AssertionError("Expected dynamic gene sprites on wnt_pathway2.pptx");
        }
        var humanImg = PptxSlideRenderer.renderForSpecies(wntSlide, countData, "Homo sapiens");
        var yeastImg = PptxSlideRenderer.renderForSpecies(wntSlide, countData, "Saccharomyces cerevisiae");
        long t2 = System.currentTimeMillis();
        javax.imageio.ImageIO.write(humanImg, "png", new java.io.File("temp/wnt_human_overlay.png"));
        javax.imageio.ImageIO.write(yeastImg, "png", new java.io.File("temp/wnt_yeast_overlay.png"));

        var countsSlide = PptxSlideRenderer.prepareSlide(
                "src/module/evolview/pathwaybrowser/io/examples/pathway_counts.pptx",
                countData,
                () -> false
        );
        if (countsSlide == null || countsSlide.categorySlots().size() != 9) {
            throw new AssertionError("Expected 9 category slots on pathway_counts.pptx");
        }
        var countsHumanImg = PptxSlideRenderer.renderForSpecies(countsSlide, countData, "Homo sapiens");
        javax.imageio.ImageIO.write(countsHumanImg, "png", new java.io.File("temp/pathway_counts_human_overlay.png"));
        System.out.println("Prepared wnt_pathway2.pptx in " + (t1 - t0) + " ms (" + wntSlide.geneSprites().size()
                + " gene sprites); species overlay in " + (t2 - t1) + " ms");
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
                        List.of("color", "alpha", "duplicate-blue"),
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
            throw new AssertionError("Duplicate names must keep first row data");
        }
        if (prepared.name2RowData().containsKey("")) {
            throw new AssertionError("Blank names must not be indexed");
        }
    }
}
