package module.evolview.pathwaybrowser;

import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.List;
import java.util.Map;

public final class RemainingDataRegressionTest {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var data = PathwayComponentCountData.fromRawTable(List.of("name", "category", "Human"),
                List.of(List.of("WNT1", "cat1", "0"), List.of("WNT2", "cat1", "NA")));
        require(data.resolveGeneForSpecies("Human", "WNT*").state() == PathwayComponentCountData.CountState.MISSING, "Zero plus missing is unknown");
        data = PathwayComponentCountData.fromRawTable(List.of("name", "category", "Human"),
                List.of(List.of("WNT1", "cat1", "2"), List.of("WNT2", "cat1", "NA")));
        var match = data.resolveGeneForSpecies("Human", "WNT*");
        require(match.count() == 2 && match.isPartial(), "Positive partial family is a lower bound");
        for (var values : List.of(List.of("0", "NA"), List.of("2", "NA"), List.of("0", "0"), List.of("NA", "NA"))) {
            var sample = PathwayComponentCountData.fromRawTable(List.of("name", "category", "Human"),
                    List.of(List.of("WNT1", "cat1", values.get(0)), List.of("WNT2", "cat1", values.get(1))));
            var gene = sample.resolveGeneForSpecies("Human", "WNT*");
            var category = sample.resolveCategoryForSpecies("Human", "cat1");
            require(gene.state() == category.state() && gene.count() == category.count() && gene.knownGenes() == category.knownGenes(), "Family/category missing semantics differ: " + values);
            require((gene.state() == PathwayComponentCountData.CountState.ABSENT) == values.equals(List.of("0", "0")), "Only complete zeros are absent");
            var bounds = new java.awt.geom.Rectangle2D.Double(10, 10, 20, 20);
            var sprite = new module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.GeneSprite("WNT*", 10, 10,
                    new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_ARGB), List.of(bounds));
            var slot = new module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.CategorySlot("cat1", bounds, bounds, null, null);
            var slide = new module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.PreparedSlide(new java.awt.Dimension(200, 100), null, List.of(), Map.of("WNT*", sprite), List.of(slot));
            var banner = module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.computeBannerStats(slide, sample, "Human");
            require(banner.totalCount() == 2 && banner.totalCopies() == gene.count() && banner.partial() == gene.isPartial(), "Copies completeness or gene/category deduplication differs: " + values);
        }
        require(!PathwayComponentCountData.resolveSpeciesMatch(Map.of("Xenopus laevis", 1), "Xenopus tropicalis").isMatched(), "Different species must not substitute");
        require(!PathwayComponentCountData.resolveSpeciesMatch(Map.of("Xenopus tropicalis", 1), "Xenopus laevis").isMatched(), "Reverse species substitution");
        require(PathwayComponentCountData.resolveSpeciesMatch(Map.of("Xenopus tropicalis", 1, "Xenopus laevis", 2), "Frog").isAmbiguous(), "Generic frog must report ambiguity");
        require(PathwayComponentCountData.resolveSpeciesMatch(Map.of("Xenopus tropicalis", 1), "Xenopus").isMatched(), "Unique generic frog candidate");
        require(PathwayComponentCountData.resolveSpeciesMatch(Map.of("Homo sapiens", 1), "Homo-sapiens").isMatched(), "Hyphen normalization");
        try (var book = new XSSFWorkbook()) {
            var cell = book.createSheet().createRow(0).createCell(0);
            cell.setCellFormula("CUBEMEMBER(\"connection\",\"member\")");
            try {
                PathwayComponentCountData.evaluateCellToString(cell, book.getCreationHelper().createFormulaEvaluator());
                throw new AssertionError("Unsupported formula accepted");
            } catch (IllegalArgumentException expected) {
                require(expected.getMessage().contains("row 1"), "Formula failure identifies location");
            }
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("temp", "remaining-data"));
            var path = java.nio.file.Path.of("temp", "remaining-data", "unsupported.xlsx");
            try (var output = java.nio.file.Files.newOutputStream(path)) { book.write(output); }
            try { PathwayComponentCountData.load(path.toFile()); throw new AssertionError("Unsupported file accepted"); }
            catch (IllegalArgumentException expected) {
                require(expected.getMessage().contains("unsupported.xlsx") && expected.getMessage().contains("Sheet0") && expected.getMessage().contains("column 1") && expected.getMessage().contains("CUBEMEMBER"), "Formula file/sheet/cell diagnostic incomplete");
            }
            cell.setCellFormula("SUM(2,3)");
            require(PathwayComponentCountData.evaluateCellToString(cell, book.getCreationHelper().createFormulaEvaluator()).equals("5"), "Supported formula regressed");
        }
        org.slf4j.LoggerFactory.getLogger(RemainingDataRegressionTest.class).info("Remaining data regressions passed");
    }
}
