package module.evolview.pathwaybrowser;

import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import module.evolview.pathwaybrowser.gui.analysis.panel.PathwayGalleryPanel;
import module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer;
import module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.*;
import module.evolview.pathwaybrowser.gui.analysis.panel.SubSingleSlidePanel;
import module.evolview.pathwaybrowser.io.PathwayBrowserExamples;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData.*;
import module.evolview.pathwaybrowser.io.Voice4pathwayFamilyBrowser;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xslf.usermodel.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Comprehensive regression test suite covering all verified review items PB-01 through PB-16 and V-01 through V-03.
 * Complying with User Rule 4: all temporary test artifacts are preserved in temp/review_test_artifacts/ without deletion.
 */
public final class PathwayReviewRegressionTest {

    private static final Path ARTIFACTS_DIR = Path.of("temp", "review_test_artifacts");

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " - expected: <" + expected + ">, but was: <" + actual + ">");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Running PathwayReviewRegressionTest...");
        Files.createDirectories(ARTIFACTS_DIR);

        testPb01ExcelFormulaCount();
        testPb02MissingValuesAndUnmappedGenes();
        testPb03CacheInvalidationOnStructureChange();
        testPb04PainterZOrderPreservation();
        testPb05ExactGeneMatchingAndWildcards();
        testPb06SvgPathTransformAndStyles();
        testPb07GalleryTabClosingDesync();
        testPb08DuplicateGeneDetection();
        testPb09BannerCopiesDeduplication();
        testPb10SpeciesNormalizationAndAlias();
        testPb11GroupShapeRecursion();
        testPb12RotatedShapeBoundingBox();
        testPb13PptxWithoutSlide1Xml();
        testPb14CorruptedPptxValidationAndRecovery();
        testPb15CountValidationEdgeCases();
        testPb16CategoryCaseInsensitivity();
        testV01SaveUtilAndExportTarget();
        testV02BoundedLruCache();
        testV03ModuleClosedGuard();

        System.out.println("ALL PB-01 through PB-16 and V-01 through V-03 regression tests passed successfully!");
        System.out.println("Test artifacts saved to: " + ARTIFACTS_DIR.toAbsolutePath() + " (preserved per User Rule 4)");
        System.exit(0);
    }

    private static void setShapeName(XSLFShape shape, String name) {
        if (shape instanceof XSLFGroupShape gs) {
            ((org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape) gs.getXmlObject())
                    .getNvGrpSpPr().getCNvPr().setName(name);
        } else if (shape instanceof XSLFConnectorShape cs) {
            ((org.openxmlformats.schemas.presentationml.x2006.main.CTConnector) cs.getXmlObject())
                    .getNvCxnSpPr().getCNvPr().setName(name);
        } else {
            ((org.openxmlformats.schemas.presentationml.x2006.main.CTShape) shape.getXmlObject())
                    .getNvSpPr().getCNvPr().setName(name);
        }
    }

    private static void testPb01ExcelFormulaCount() throws IOException {
        Path tempXlsx = ARTIFACTS_DIR.resolve("test_pb01_formula.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Counts");
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("name");
            r0.createCell(1).setCellValue("category");
            r0.createCell(2).setCellValue("Human");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("APC");
            r1.createCell(1).setCellValue("cat1");
            r1.createCell(2).setCellFormula("1+2"); // Formula = 3

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("WNT");
            r2.createCell(1).setCellValue("cat1");
            r2.createCell(2).setCellFormula("SUM(1, 4)"); // Formula = 5

            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("AXIN");
            r3.createCell(1).setCellValue("cat1");
            r3.createCell(2).setCellFormula("C2+1"); // Cross-cell formula = 4

            try (FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {
                wb.write(fos);
            }
        }

        PathwayComponentCountData data = PathwayComponentCountData.load(tempXlsx.toFile());
        Map<String, Integer> counts = data.getGeneCountsForSpecies("Human");
        assertEquals(3, counts.get("APC"), "PB-01: Formula 1+2 must evaluate to 3");
        assertEquals(5, counts.get("WNT"), "PB-01: Formula SUM(1,4) must evaluate to 5");
        assertEquals(4, counts.get("AXIN"), "PB-01: Cross-cell formula C2+1 must evaluate to 4");
        assertEquals(12, data.getCategoryCopySumForSpecies("Human").get("cat1"), "PB-01: Category sum must include all formula results");

        // Error formula (=1/0) must be rejected with clear message
        Path errXlsx = ARTIFACTS_DIR.resolve("test_pb01_err_formula.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Counts");
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("name");
            r0.createCell(1).setCellValue("Human");
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("APC");
            r1.createCell(1).setCellFormula("1/0");
            try (FileOutputStream fos = new FileOutputStream(errXlsx.toFile())) {
                wb.write(fos);
            }
        }
        assertThrowsIllegalArgument(() -> {
            try {
                PathwayComponentCountData.load(errXlsx.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "PB-01: Error formula 1/0 must throw IllegalArgumentException");
    }

    private static void testPb02MissingValuesAndUnmappedGenes() throws Exception {
        List<String> headers = List.of("name", "category", "Human");
        List<List<String>> rows = List.of(
                List.of("APC", "cat1", "0"),      // Explicitly absent
                List.of("AXIN", "cat2", "3"),     // Present
                List.of("CTNNB1", "cat1", "NA"),  // Missing / NA (cat1 has {0, NA} -> MISSING!)
                List.of("WNT", "cat2", ""),       // Missing / Empty
                List.of("DVL", "cat2", "-")       // Missing / Single dash
        );

        PathwayComponentCountData data = PathwayComponentCountData.fromRawTable(headers, rows);
        assertEquals(CountState.ABSENT, data.resolveGeneForSpecies("Human", "APC").state(), "PB-02: Count 0 must be ABSENT");
        assertEquals(CountState.PRESENT, data.resolveGeneForSpecies("Human", "AXIN").state(), "PB-02: Count 3 must be PRESENT");
        assertEquals(CountState.MISSING, data.resolveGeneForSpecies("Human", "CTNNB1").state(), "PB-02: NA must be MISSING");
        assertEquals(CountState.MISSING, data.resolveGeneForSpecies("Human", "WNT").state(), "PB-02: Empty cell must be MISSING");
        assertEquals(CountState.MISSING, data.resolveGeneForSpecies("Human", "DVL").state(), "PB-02: Dash '-' must be MISSING");
        assertTrue(!data.resolveGeneForSpecies("Human", "UNMAPPED").mappedInTable(), "PB-02: Unmapped gene must report !mappedInTable");

        // Partial missing category {APC=0, CTNNB1=NA} must report MISSING rather than confirmed ABSENT
        CategoryMatch cat1Match = data.resolveCategoryForSpecies("Human", "cat1");
        assertEquals(CountState.MISSING, cat1Match.state(), "PB-02: Category with {0, NA} must be MISSING rather than ABSENT");

        // Verify pixel rendering for ABSENT vs MISSING vs UNMAPPED + unmapped category cat9
        Path pptx = ARTIFACTS_DIR.resolve("test_pb02_states.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(400, 220));
            XSLFSlide slide = ppt.createSlide();

            XSLFAutoShape apc = slide.createAutoShape();
            apc.setShapeType(ShapeType.RECT);
            apc.setAnchor(new Rectangle2D.Double(20, 30, 60, 40));
            apc.setFillColor(new Color(220, 30, 30));
            setShapeName(apc, "APC_comp");

            XSLFAutoShape ctnnb1 = slide.createAutoShape();
            ctnnb1.setShapeType(ShapeType.RECT);
            ctnnb1.setAnchor(new Rectangle2D.Double(110, 30, 60, 40));
            ctnnb1.setFillColor(new Color(220, 30, 30));
            setShapeName(ctnnb1, "CTNNB1_comp");

            XSLFAutoShape unmapped = slide.createAutoShape();
            unmapped.setShapeType(ShapeType.RECT);
            unmapped.setAnchor(new Rectangle2D.Double(200, 30, 60, 40));
            unmapped.setFillColor(new Color(30, 60, 220));
            setShapeName(unmapped, "UNMAPPED_comp");

            XSLFAutoShape unmappedCat = slide.createAutoShape();
            unmappedCat.setShapeType(ShapeType.RECT);
            unmappedCat.setAnchor(new Rectangle2D.Double(290, 30, 80, 40));
            unmappedCat.setFillColor(new Color(40, 180, 80));
            setShapeName(unmappedCat, "cat9");

            try (FileOutputStream fos = new FileOutputStream(pptx.toFile())) {
                ppt.write(fos);
            }
        }

        PreparedSlide prepared = PptxSlideRenderer.prepareSlide(pptx.toString(), data, () -> false);
        BufferedImage full = prepared.fullSlideImage();
        BufferedImage rendered = PptxSlideRenderer.renderForSpecies(prepared, data, "Human");

        // APC=0 center (50, 50) must be ghosted (much lighter green/blue channels over white background than CTNNB1=NA)
        Color apcColor = new Color(rendered.getRGB(50, 50), true);
        Color ctnnb1Color = new Color(rendered.getRGB(140, 50), true);
        assertTrue(apcColor.getGreen() > 180, "PB-02: Explicitly absent APC=0 must be ghosted toward white");
        assertTrue(ctnnb1Color.getRed() > 200 && ctnnb1Color.getGreen() < 60, "PB-02: Missing CTNNB1=NA must NOT be ghosted");

        // Unmapped gene (230, 50) and unmapped category cat9 (330, 50) + above cat9 badge zone (355, 25) must stay untouched
        assertEquals(full.getRGB(230, 50), rendered.getRGB(230, 50), "PB-02: Unmapped gene shape must remain untouched");
        assertEquals(full.getRGB(330, 50), rendered.getRGB(330, 50), "PB-02: Unmapped category cat9 interior must not be dimmed");
        assertEquals(full.getRGB(355, 25), rendered.getRGB(355, 25), "PB-02: Unmapped category cat9 must not draw Count:? badge");
    }

    private static void testPb03CacheInvalidationOnStructureChange() throws Exception {
        Path tempPptx = ARTIFACTS_DIR.resolve("test_pb03_cache.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            XSLFSlide slide = ppt.createSlide();
            XSLFAutoShape s = slide.createAutoShape();
            s.setShapeType(ShapeType.RECT);
            s.setAnchor(new Rectangle2D.Double(10, 10, 50, 30));
            setShapeName(s, "APC_comp");
            try (FileOutputStream fos = new FileOutputStream(tempPptx.toFile())) {
                ppt.write(fos);
            }
        }

        PathwayComponentCountData tableA = PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("GENE_A", "cat1", "1"), List.of("GENE_B", "cat1", "2"))
        );
        PathwayComponentCountData tableB = PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("GENE_C", "cat1", "1"), List.of("GENE_D", "cat1", "2"))
        );
        PathwayComponentCountData tableLower = PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("apc", "cat1", "1"))
        );
        PathwayComponentCountData tableUpper = PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("APC", "cat1", "1"))
        );

        assertTrue(!tableA.getStructureSignature().equals(tableB.getStructureSignature()),
                "PB-03: Structure signature must differ for different gene names even with same gene count");
        assertTrue(!tableLower.getStructureSignature().equals(tableUpper.getStructureSignature()),
                "PB-03: Structure signature must distinguish case changes ('apc' vs 'APC')");

        PreparedSlide slideA = PptxSlideRenderer.prepareSlide(tempPptx.toString(), tableA, () -> false);
        PreparedSlide slideUpper = PptxSlideRenderer.prepareSlide(tempPptx.toString(), tableUpper, () -> false);
        assertTrue(slideA.geneSprites().isEmpty(), "PB-03: Slide prepared with GENE_A/B must have 0 APC sprites");
        assertTrue(slideUpper.geneSprites().containsKey("APC"), "PB-03: Switching count table on same PPTX must invalidate stale cache and bind APC");
    }

    private static void testPb04PainterZOrderPreservation() throws Exception {
        Path tempPptx = ARTIFACTS_DIR.resolve("test_pb04_zorder_multi.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(260, 240));
            XSLFSlide slide = ppt.createSlide();

            // z=0: Bottom instance of WNT (Red)
            XSLFAutoShape wnt1 = slide.createAutoShape();
            wnt1.setShapeType(ShapeType.RECT);
            wnt1.setAnchor(new Rectangle2D.Double(30, 30, 80, 80));
            wnt1.setFillColor(Color.RED);
            wnt1.setLineColor(Color.RED);
            setShapeName(wnt1, "WNT_comp1");

            // z=1: Static Blue cytoplasm box overlapping WNT_comp1 and under WNT_comp2
            XSLFAutoShape staticBox = slide.createAutoShape();
            staticBox.setShapeType(ShapeType.RECT);
            staticBox.setAnchor(new Rectangle2D.Double(50, 50, 140, 140));
            staticBox.setFillColor(Color.BLUE);
            staticBox.setLineColor(Color.BLUE);
            setShapeName(staticBox, "StaticCytoplasm");

            // z=2: Top instance of WNT (Green) ON TOP of StaticCytoplasm
            XSLFAutoShape wnt2 = slide.createAutoShape();
            wnt2.setShapeType(ShapeType.RECT);
            wnt2.setAnchor(new Rectangle2D.Double(100, 100, 60, 60));
            wnt2.setFillColor(new Color(0, 220, 0));
            wnt2.setLineColor(new Color(0, 220, 0));
            setShapeName(wnt2, "WNT_comp2");

            try (FileOutputStream fos = new FileOutputStream(tempPptx.toFile())) {
                ppt.write(fos);
            }
        }

        // Present (WNT = 2)
        PathwayComponentCountData presentData = PathwayComponentCountData.fromRawTable(
                List.of("name", "Human"),
                List.of(List.of("WNT", "2"))
        );
        PreparedSlide prepPresent = PptxSlideRenderer.prepareSlide(tempPptx.toString(), presentData, () -> false);
        BufferedImage imgPresent = PptxSlideRenderer.renderForSpecies(prepPresent, presentData, "Human");

        // (70, 70): WNT_comp1 is under StaticCytoplasm -> must be BLUE
        Color cUnder = new Color(imgPresent.getRGB(70, 70), true);
        assertTrue(cUnder.getBlue() > 200 && cUnder.getRed() < 30,
                "PB-04: Bottom instance WNT_comp1 must stay underneath middle static blue shape");

        // (130, 130): WNT_comp2 is above StaticCytoplasm -> must be GREEN (not pulled down to z=0!)
        Color cAbove = new Color(imgPresent.getRGB(130, 130), true);
        assertTrue(cAbove.getGreen() > 180 && cAbove.getBlue() < 40,
                "PB-04: Top instance WNT_comp2 must remain ON TOP of middle static blue shape");

        // Absent (WNT = 0)
        PathwayComponentCountData absentData = PathwayComponentCountData.fromRawTable(
                List.of("name", "Human"),
                List.of(List.of("WNT", "0"))
        );
        PreparedSlide prepAbsent = PptxSlideRenderer.prepareSlide(tempPptx.toString(), absentData, () -> false);
        BufferedImage imgAbsent = PptxSlideRenderer.renderForSpecies(prepAbsent, absentData, "Human");

        Color cUnder0 = new Color(imgAbsent.getRGB(70, 70), true);
        assertTrue(cUnder0.getBlue() > 200 && cUnder0.getRed() < 20,
                "PB-04: Ghosted bottom instance must stay underneath middle static blue shape");
        Color cAbove0 = new Color(imgAbsent.getRGB(130, 130), true);
        assertTrue(cAbove0.getBlue() > 150 && cAbove0.getGreen() > 20,
                "PB-04: Ghosted top instance WNT_comp2 must blend at 0.16f alpha OVER static blue shape");
    }

    private static void testPb05ExactGeneMatchingAndWildcards() throws Exception {
        PathwayComponentCountData data = PathwayComponentCountData.fromRawTable(
                List.of("name", "Human"),
                List.of(
                        List.of("WNT10A", "7"),
                        List.of("WNT11", "3"),
                        List.of("APC", "2"),
                        List.of("APCDD1", "5")
                )
        );

        OptionalInt countWnt1 = PathwayComponentCountData.resolveGeneCount(data.getGeneCountsForSpecies("Human"), "WNT1");
        assertTrue(countWnt1.isEmpty(), "PB-05: WNT1 must NOT match WNT10A");

        OptionalInt countApc = PathwayComponentCountData.resolveGeneCount(data.getGeneCountsForSpecies("Human"), "APC");
        assertEquals(2, countApc.getAsInt(), "PB-05: APC must match exact count 2 (not APCDD1)");

        OptionalInt countWntWild = PathwayComponentCountData.resolveGeneCount(data.getGeneCountsForSpecies("Human"), "WNT*");
        assertEquals(10, countWntWild.getAsInt(), "PB-05: Wildcard WNT* must aggregate all WNT family members");

        // Mixed-case shape names ('apc_comp' + 'APC_name') must canonicalize to 1 GeneSprite with 1 badge cluster
        Path pptx = ARTIFACTS_DIR.resolve("test_pb05_mixed_case.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(240, 160));
            XSLFSlide slide = ppt.createSlide();
            XSLFAutoShape comp = slide.createAutoShape();
            comp.setShapeType(ShapeType.RECT);
            comp.setAnchor(new Rectangle2D.Double(40, 40, 80, 36));
            comp.setFillColor(Color.ORANGE);
            setShapeName(comp, "apc_comp");

            XSLFTextBox label = slide.createTextBox();
            label.setAnchor(new Rectangle2D.Double(45, 45, 70, 26));
            label.setText("APC");
            setShapeName(label, "APC_name");

            try (FileOutputStream fos = new FileOutputStream(pptx.toFile())) {
                ppt.write(fos);
            }
        }
        PreparedSlide prep = PptxSlideRenderer.prepareSlide(pptx.toString(), data, () -> false);
        assertEquals(1, prep.geneSprites().size(), "PB-05: apc_comp and APC_name must merge into 1 canonical GeneSprite");
        assertEquals(1, prep.geneSprites().get("APC").instanceClusters().size(), "PB-05: Overlapping apc_comp and APC_name must produce 1 badge cluster");
    }

    private static void testPb06SvgPathTransformAndStyles() throws Exception {
        String svgContent = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="100 200 300 200" width="300" height="200">
                    <defs>
                        <marker id="arrow" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="8" markerHeight="8" orient="auto">
                            <path d="M 0 0 L 10 5 L 0 10 z" fill="#cc00cc"/>
                        </marker>
                    </defs>
                    <rect x="100" y="200" width="300" height="200" fill="white"/>
                    <rect id="cat1" x="300" y="220" width="70" height="45" fill="#ff8800"/>
                    <g transform="translate(120, 230)" fill="rgb(20, 150, 60)">
                        <path id="APC_comp" d="M 10 10 L 55 10 A 15 15 0 0 1 55 40 L 10 40 Z" fill="red" stroke="blue"/>
                        <polygon points="90,10 130,10 110,40"/>
                        <line x1="140" y1="25" x2="170" y2="25" stroke="#cc00cc" stroke-width="3" marker-end="url(#arrow)"/>
                    </g>
                </svg>
                """;

        Path tempSvg = ARTIFACTS_DIR.resolve("test_pb06_transform.svg");
        Files.writeString(tempSvg, svgContent, StandardCharsets.UTF_8);

        PathwayComponentCountData countData = PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("APC", "cat1", "2"))
        );

        PreparedSlide prepared = PptxSlideRenderer.prepareSlide(tempSvg.toString(), countData, () -> false);
        BufferedImage rendered = PptxSlideRenderer.renderForSpecies(prepared, countData, "Human");

        // viewBox offset (-100, -200) + translate(120, 230) => local (0,0) maps to canvas (20, 30)
        // Inside APC_comp path: local (35, 25) -> canvas (55, 55) must be RED
        Color pixelPath = new Color(rendered.getRGB(55, 55), true);
        assertTrue(pixelPath.getRed() > 200 && pixelPath.getBlue() < 50,
                "PB-06: SVG <path> with elliptical arc A and non-zero viewBox must render red fill");

        // Inside polygon inheriting fill="rgb(20, 150, 60)" from <g>: local (110, 20) -> canvas (130, 50)
        Color pixelPoly = new Color(rendered.getRGB(130, 50), true);
        assertTrue(pixelPoly.getGreen() > 120 && pixelPoly.getRed() < 50,
                "PB-06: SVG <polygon> must inherit rgb() fill and transform from <g>");

        // Category box <rect id="cat1"> at (300-100=200, 220-200=20, 70x45) -> center (235, 42) must stay orange (#ff8800), NOT erased!
        Color pixelCat = new Color(rendered.getRGB(235, 42), true);
        assertTrue(pixelCat.getRed() > 220 && pixelCat.getGreen() > 100 && pixelCat.getBlue() < 40,
                "PB-06: SVG category <rect id='cat1'> must remain visible after species selection");
    }

    private static void testPb07GalleryTabClosingDesync() throws Exception {
        List<String> files = List.of(
                "src/module/evolview/pathwaybrowser/io/examples/WP428.pptx",
                "src/module/evolview/pathwaybrowser/io/examples/wnt_pathway2.pptx",
                "src/module/evolview/pathwaybrowser/io/examples/demo_selection.pptx",
                "src/module/evolview/pathwaybrowser/io/examples/pathway_counts.pptx"
        );
        PathwayGalleryPanel[] holder = {null};
        SwingUtilities.invokeAndWait(() -> {
            PathwayGalleryPanel gallery = new PathwayGalleryPanel(null, files);
            holder[0] = gallery;
            assertEquals(4, gallery.getLiveSlidePanels().size(), "PB-07: Initial live slides count must be 4");

            // 1. Close first tab (index 0: WP428.pptx) -> remaining: wnt_pathway2, demo_selection, pathway_counts
            gallery.getTabbedPane().removeTabAt(0);
            assertEquals(3, gallery.getLiveSlidePanels().size(), "PB-07: After closing first tab, count must be 3");
            assertTrue(gallery.getActiveSlidePanel().getFilePath().endsWith("wnt_pathway2.pptx"),
                    "PB-07: Active tab after closing first must be wnt_pathway2.pptx");

            // 2. Close middle tab (index 1: demo_selection.pptx) -> remaining: wnt_pathway2, pathway_counts
            gallery.getTabbedPane().removeTabAt(1);
            assertEquals(2, gallery.getLiveSlidePanels().size(), "PB-07: After closing middle tab, count must be 2");

            // 3. Select and close last tab (index 1: pathway_counts.pptx) -> remaining: wnt_pathway2
            gallery.getTabbedPane().setSelectedIndex(1);
            gallery.getTabbedPane().removeTabAt(1);
            assertEquals(1, gallery.getLiveSlidePanels().size(), "PB-07: After closing last tab, count must be 1");
            assertTrue(gallery.getActiveSlidePanel().getFilePath().endsWith("wnt_pathway2.pptx"),
                    "PB-07: Remaining active tab must be wnt_pathway2.pptx");

            // 4. Close all tabs and click tree node -> no exception, no phantom panel load
            gallery.getTabbedPane().removeTabAt(0);
            assertEquals(0, gallery.getLiveSlidePanels().size(), "PB-07: All tabs closed");
            assertTrue(gallery.getActiveSlidePanel() == null, "PB-07: Active slide must be null when all tabs closed");
            gallery.treeNodeClicked("Human");
        });
    }

    private static void testPb08DuplicateGeneDetection() {
        // Same-category duplicate row
        assertThrowsIllegalArgument(() -> PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("APC", "cat1", "1"), List.of("APC", "cat1", "2"))
        ), "PB-08: Duplicate gene rows in same category must be rejected");

        // Cross-category duplicate row
        assertThrowsIllegalArgument(() -> PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("APC", "cat1", "1"), List.of("apc", "cat2", "2"))
        ), "PB-08: Cross-category duplicate gene rows must be rejected");

        // Species-as-rows duplicate gene column
        assertThrowsIllegalArgument(() -> PathwayComponentCountData.fromRawTable(
                List.of("Name", "WNT1", "wnt1"),
                List.of(List.of("Human", "1", "2"))
        ), "PB-08: Case-insensitive duplicate gene columns must be rejected");
    }

    private static void testPb09BannerCopiesDeduplication() {
        PathwayComponentCountData countData = PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(
                        List.of("APC", "cat1", "3"),
                        List.of("AXIN", "cat1", "2"),
                        List.of("WNT", "cat2", "4")
                )
        );

        BufferedImage dummy = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        GeneSprite apcSprite = new GeneSprite("APC", 10, 10, dummy, List.of(new Rectangle2D.Double(10, 10, 20, 20)));
        GeneSprite wntMultiSprite = new GeneSprite("WNT", 10, 50, dummy, List.of(
                new Rectangle2D.Double(10, 50, 20, 20),
                new Rectangle2D.Double(150, 50, 20, 20)
        ));
        CategorySlot cat1Slot = new CategorySlot("cat1", new Rectangle2D.Double(100, 100, 80, 20), null, new Font("Arial", Font.PLAIN, 12), Color.BLACK);

        // 1. Components only
        PreparedSlide compOnly = new PreparedSlide(new Dimension(300, 200), dummy,
                List.of(new DynamicLayer("APC", apcSprite)), Map.of("APC", apcSprite), List.of());
        BannerStats s1 = PptxSlideRenderer.computeBannerStats(compOnly, countData, "Human");
        assertEquals(3, s1.totalCopies(), "PB-09 (1): Components-only copies must be 3");
        assertEquals(1, s1.totalCount(), "PB-09 (1): Components-only total must be 1");

        // 2. Categories only
        PreparedSlide catOnly = new PreparedSlide(new Dimension(300, 200), dummy,
                List.of(), Map.of(), List.of(cat1Slot));
        BannerStats s2 = PptxSlideRenderer.computeBannerStats(catOnly, countData, "Human");
        assertEquals(5, s2.totalCopies(), "PB-09 (2): Categories-only copies must be 5");
        assertEquals(2, s2.totalCount(), "PB-09 (2): Categories-only total must be 2");

        // 3. Mixed component + category (APC appears in both -> deduplicated!)
        PreparedSlide mixed = new PreparedSlide(new Dimension(300, 200), dummy,
                List.of(new DynamicLayer("APC", apcSprite)), Map.of("APC", apcSprite), List.of(cat1Slot));
        BannerStats s3 = PptxSlideRenderer.computeBannerStats(mixed, countData, "Human");
        assertEquals(5, s3.totalCopies(), "PB-09 (3): Mixed copies must deduplicate APC (3+2=5, not 8)");

        // 4. Multiple graphical instances of same gene (WNT drawn twice)
        PreparedSlide multiInst = new PreparedSlide(new Dimension(300, 200), dummy,
                List.of(new DynamicLayer("WNT", wntMultiSprite), new DynamicLayer("WNT", wntMultiSprite)),
                Map.of("WNT", wntMultiSprite), List.of());
        BannerStats s4 = PptxSlideRenderer.computeBannerStats(multiInst, countData, "Human");
        assertEquals(4, s4.totalCopies(), "PB-09 (4): Multi-instance WNT copies must be counted once (4, not 8)");
        assertEquals(1, s4.totalCount(), "PB-09 (4): Multi-instance WNT total gene count must be 1");
    }

    private static void testPb10SpeciesNormalizationAndAlias() {
        Map<String, Integer> speciesMap = Map.of(
                "Homo_sapiens", 42,
                "Mus_musculus", 18,
                "Xenopus_tropicalis", 7,
                "Xenopus_laevis", 9
        );

        assertEquals(42, PathwayComponentCountData.lookupBySpeciesName(speciesMap, "Homo sapiens"),
                "PB-10: 'Homo sapiens' must match 'Homo_sapiens'");
        assertEquals(42, PathwayComponentCountData.lookupBySpeciesName(speciesMap, "Human"),
                "PB-10: 'Human' alias must match 'Homo_sapiens'");
        assertTrue(PathwayComponentCountData.lookupBySpeciesName(speciesMap, "Unknown_Organism") == null,
                "PB-10: Unknown species returns null");

        // Ambiguous alias ('Frog' matches both Xenopus_tropicalis and Xenopus_laevis)
        SpeciesMatchResult<Integer> frogRes = PathwayComponentCountData.resolveSpeciesMatch(speciesMap, "Frog");
        assertEquals(SpeciesMatchStatus.AMBIGUOUS, frogRes.status(),
                "PB-10: 'Frog' against two Xenopus species must return AMBIGUOUS");
        assertEquals(2, frogRes.candidates().size(), "PB-10: Ambiguous resolution must list both Xenopus candidates");
    }

    private static void testPb11GroupShapeRecursion() throws Exception {
        Path tempPptx = ARTIFACTS_DIR.resolve("test_pb11_all_groups.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(520, 320));
            XSLFSlide slide = ppt.createSlide();

            // (a) Default unnamed group ("Group 1") containing APC_comp (red) + APC_name
            XSLFGroupShape unnamedGrp = slide.createGroup();
            unnamedGrp.setAnchor(new Rectangle2D.Double(20, 30, 80, 50));
            unnamedGrp.setInteriorAnchor(new Rectangle2D.Double(20, 30, 80, 50));
            setShapeName(unnamedGrp, "Group 1");
            XSLFAutoShape apcRect = unnamedGrp.createAutoShape();
            apcRect.setShapeType(ShapeType.RECT);
            apcRect.setAnchor(new Rectangle2D.Double(20, 30, 80, 50));
            apcRect.setFillColor(Color.RED);
            setShapeName(apcRect, "APC_comp");

            // (b) Nested group: OuterGroup -> InnerGroup -> WNT_comp (blue)
            XSLFGroupShape outerGrp = slide.createGroup();
            outerGrp.setAnchor(new Rectangle2D.Double(130, 30, 80, 50));
            outerGrp.setInteriorAnchor(new Rectangle2D.Double(130, 30, 80, 50));
            XSLFGroupShape innerGrp = outerGrp.createGroup();
            innerGrp.setAnchor(new Rectangle2D.Double(130, 30, 80, 50));
            innerGrp.setInteriorAnchor(new Rectangle2D.Double(130, 30, 80, 50));
            XSLFAutoShape wntRect = innerGrp.createAutoShape();
            wntRect.setShapeType(ShapeType.RECT);
            wntRect.setAnchor(new Rectangle2D.Double(130, 30, 80, 50));
            wntRect.setFillColor(Color.BLUE);
            setShapeName(wntRect, "WNT_comp");

            // (c) Group itself named CTNNB1_comp containing unnamed child shape (green)
            XSLFGroupShape namedGrp = slide.createGroup();
            namedGrp.setAnchor(new Rectangle2D.Double(240, 30, 80, 50));
            namedGrp.setInteriorAnchor(new Rectangle2D.Double(240, 30, 80, 50));
            setShapeName(namedGrp, "CTNNB1_comp");
            XSLFAutoShape unnamedChild = namedGrp.createAutoShape();
            unnamedChild.setShapeType(ShapeType.RECT);
            unnamedChild.setAnchor(new Rectangle2D.Double(240, 30, 80, 50));
            unnamedChild.setFillColor(new Color(0, 180, 0));

            // (d) Single group containing TWO genes (AXIN_comp, DVL_comp) + static orange connector bar
            XSLFGroupShape multiGrp = slide.createGroup();
            multiGrp.setAnchor(new Rectangle2D.Double(20, 130, 300, 60));
            multiGrp.setInteriorAnchor(new Rectangle2D.Double(20, 130, 300, 60));
            XSLFAutoShape axinShape = multiGrp.createAutoShape();
            axinShape.setShapeType(ShapeType.RECT);
            axinShape.setAnchor(new Rectangle2D.Double(20, 130, 70, 50));
            axinShape.setFillColor(new Color(180, 0, 180));
            setShapeName(axinShape, "AXIN_comp");

            XSLFAutoShape staticBar = multiGrp.createAutoShape();
            staticBar.setShapeType(ShapeType.RECT);
            staticBar.setAnchor(new Rectangle2D.Double(110, 145, 70, 20));
            staticBar.setFillColor(new Color(255, 140, 0));
            setShapeName(staticBar, "StaticConnector");

            XSLFAutoShape dvlShape = multiGrp.createAutoShape();
            dvlShape.setShapeType(ShapeType.RECT);
            dvlShape.setAnchor(new Rectangle2D.Double(200, 130, 70, 50));
            dvlShape.setFillColor(new Color(0, 160, 180));
            setShapeName(dvlShape, "DVL_comp");

            try (FileOutputStream fos = new FileOutputStream(tempPptx.toFile())) {
                ppt.write(fos);
            }
        }

        PathwayComponentCountData data = PathwayComponentCountData.fromRawTable(
                List.of("name", "Human"),
                List.of(
                        List.of("APC", "2"),     // (a) Present in unnamed group
                        List.of("WNT", "1"),     // (b) Present in nested group
                        List.of("CTNNB1", "0"),  // (c) Absent in named group -> ghosted
                        List.of("AXIN", "0"),    // (d) Absent in multi-gene group -> ghosted
                        List.of("DVL", "3")      // (d) Present in multi-gene group -> opaque
                )
        );

        PreparedSlide prep = PptxSlideRenderer.prepareSlide(tempPptx.toString(), data, () -> false);
        assertEquals(5, prep.geneSprites().size(), "PB-11: All 5 grouped genes must be discovered");

        BufferedImage img = PptxSlideRenderer.renderForSpecies(prep, data, "Human");

        // (a) APC inside unnamed group at (60, 55) must be opaque RED (NOT vanished!)
        Color cApc = new Color(img.getRGB(60, 55), true);
        assertTrue(cApc.getRed() > 200 && cApc.getGreen() < 40,
                "PB-11 (a): APC_comp inside unnamed group must render opaque red when present");

        // (b) WNT inside nested group at (170, 55) must be opaque BLUE
        Color cWnt = new Color(img.getRGB(170, 55), true);
        assertTrue(cWnt.getBlue() > 200 && cWnt.getRed() < 40,
                "PB-11 (b): WNT_comp inside nested group must render opaque blue when present");

        // (c) CTNNB1=0 inside named group at (280, 55) must be ghosted
        Color cCtnnb1 = new Color(img.getRGB(280, 55), true);
        assertTrue(cCtnnb1.getRed() > 190 && cCtnnb1.getBlue() > 190,
                "PB-11 (c): Unnamed child inside CTNNB1_comp group must ghost when CTNNB1=0");

        // (d) Multi-gene group: AXIN=0 at (55, 155) ghosted, StaticConnector at (145, 155) opaque orange, DVL=3 at (235, 155) opaque teal
        Color cAxin = new Color(img.getRGB(55, 155), true);
        Color cStaticBar = new Color(img.getRGB(145, 155), true);
        Color cDvl = new Color(img.getRGB(235, 155), true);
        assertTrue(cAxin.getGreen() > 190, "PB-11 (d): AXIN=0 in shared group must ghost");
        assertTrue(cStaticBar.getRed() > 220 && cStaticBar.getGreen() > 110 && cStaticBar.getBlue() < 40,
                "PB-11 (d): Static shape inside shared group must stay opaque orange");
        assertTrue(cDvl.getGreen() > 130 && cDvl.getBlue() > 150 && cDvl.getRed() < 40,
                "PB-11 (d): Second gene DVL=3 in shared group must stay opaque");
    }

    private static void testPb12RotatedShapeBoundingBox() throws Exception {
        Path tempPptx = ARTIFACTS_DIR.resolve("test_pb12_rotate_angles.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(500, 300));
            XSLFSlide slide = ppt.createSlide();

            // 0 deg
            XSLFAutoShape s0 = slide.createAutoShape();
            s0.setShapeType(ShapeType.RECT);
            s0.setAnchor(new Rectangle2D.Double(20, 60, 120, 12));
            s0.setRotation(0.0);
            s0.setFillColor(Color.MAGENTA);
            setShapeName(s0, "APC_comp");

            // 45 deg
            XSLFAutoShape s45 = slide.createAutoShape();
            s45.setShapeType(ShapeType.RECT);
            s45.setAnchor(new Rectangle2D.Double(170, 100, 120, 12));
            s45.setRotation(45.0);
            s45.setFillColor(Color.MAGENTA);
            setShapeName(s45, "WNT_comp");

            // 90 deg
            XSLFAutoShape s90 = slide.createAutoShape();
            s90.setShapeType(ShapeType.RECT);
            s90.setAnchor(new Rectangle2D.Double(330, 100, 120, 12));
            s90.setRotation(90.0);
            s90.setFillColor(Color.MAGENTA);
            setShapeName(s90, "AXIN_comp");

            try (FileOutputStream fos = new FileOutputStream(tempPptx.toFile())) {
                ppt.write(fos);
            }
        }

        PathwayComponentCountData data = PathwayComponentCountData.fromRawTable(
                List.of("name", "Human"),
                List.of(List.of("APC", "1"), List.of("WNT", "1"), List.of("AXIN", "1"))
        );

        PreparedSlide prepared = PptxSlideRenderer.prepareSlide(tempPptx.toString(), data, () -> false);
        assertTrue(prepared.geneSprites().get("APC").spriteImage().getWidth() >= 118,
                "PB-12: 0-deg sprite width must encompass full 120px bar");
        assertTrue(prepared.geneSprites().get("WNT").spriteImage().getHeight() >= 84,
                "PB-12: 45-deg sprite height must encompass full rotated diagonal (~93px)");
        assertTrue(prepared.geneSprites().get("AXIN").spriteImage().getHeight() >= 118,
                "PB-12: 90-deg sprite height must encompass full vertical rotated 120px bar");

        // Verify extremities of 90-deg rotated AXIN bar (center 390, 106 -> spans y=46..166 at x=390; badge pill sits at y=41..56) are NOT clipped
        BufferedImage axinSpriteImg = prepared.geneSprites().get("AXIN").spriteImage();
        Color spriteTopTip = new Color(axinSpriteImg.getRGB(axinSpriteImg.getWidth() / 2, 5), true);
        Color spriteBotTip = new Color(axinSpriteImg.getRGB(axinSpriteImg.getWidth() / 2, axinSpriteImg.getHeight() - 6), true);
        assertTrue(spriteTopTip.getRed() > 200 && spriteTopTip.getBlue() > 200 && spriteTopTip.getGreen() < 40,
                "PB-12: Top extremity of 90-deg rotated sprite image must not be clipped");
        assertTrue(spriteBotTip.getRed() > 200 && spriteBotTip.getBlue() > 200 && spriteBotTip.getGreen() < 40,
                "PB-12: Bottom extremity of 90-deg rotated sprite image must not be clipped");

        BufferedImage rendered = PptxSlideRenderer.renderForSpecies(prepared, data, "Human");
        Color topTip = new Color(rendered.getRGB(390, 62), true); // y=62 is 38px above unrotated y=100 and just below top badge
        Color botTip = new Color(rendered.getRGB(390, 160), true); // y=160 is 48px below unrotated y=112
        assertTrue(topTip.getRed() > 200 && topTip.getBlue() > 200 && topTip.getGreen() < 40,
                "PB-12: Upper arm of 90-deg rotated bar on rendered slide must not be clipped");
        assertTrue(botTip.getRed() > 200 && botTip.getBlue() > 200 && botTip.getGreen() < 40,
                "PB-12: Bottom tip of 90-deg rotated bar on rendered slide must not be clipped");
    }

    private static void testPb13PptxWithoutSlide1Xml() throws Exception {
        Path tempPptx = ARTIFACTS_DIR.resolve("test_pb13_slide2.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.createSlide(); // Slide 1
            XSLFSlide slide2 = ppt.createSlide(); // Slide 2
            slide2.createAutoShape().setAnchor(new Rectangle2D.Double(10, 10, 20, 20));
            ppt.removeSlide(0); // Delete Slide 1, leaving slide2.xml

            try (FileOutputStream fos = new FileOutputStream(tempPptx.toFile())) {
                ppt.write(fos);
            }
        }

        PptxSlideRenderer.validateGalleryFile(tempPptx.toString());
        PreparedSlide prepared = PptxSlideRenderer.prepareSlide(tempPptx.toString(), PathwayComponentCountData.empty(), () -> false);
        assertTrue(prepared != null, "PB-13: Valid PPTX without slide1.xml must be accepted and prepared");
    }

    private static void testPb14CorruptedPptxValidationAndRecovery() throws Exception {
        // 1. Non-zip garbage .pptx
        Path nonZipPptx = ARTIFACTS_DIR.resolve("test_pb14_nonzip.pptx");
        Files.writeString(nonZipPptx, "This is not a valid zip or pptx file at all");
        assertThrowsIllegalArgument(() -> {
            try {
                PptxSlideRenderer.validateGalleryFile(nonZipPptx.toString());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }, "PB-14: Non-zip .pptx must fail validation");

        // 2. ZIP missing [Content_Types].xml
        Path missingCtPptx = ARTIFACTS_DIR.resolve("test_pb14_missing_ct.pptx");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(missingCtPptx))) {
            zos.putNextEntry(new ZipEntry("dummy.txt"));
            zos.write("hello".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        assertThrowsIllegalArgument(() -> {
            try {
                PptxSlideRenderer.validateGalleryFile(missingCtPptx.toString());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }, "PB-14: ZIP missing [Content_Types].xml must fail validation");

        // 3. Valid ZIP .pptx shell with corrupted XML inside ppt/slides/slide1.xml
        Path validTemplate = ARTIFACTS_DIR.resolve("test_pb14_template.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.createSlide().createAutoShape().setAnchor(new Rectangle2D.Double(10, 10, 40, 20));
            try (FileOutputStream fos = new FileOutputStream(validTemplate.toFile())) {
                ppt.write(fos);
            }
        }
        Path brokenSlideXmlPptx = ARTIFACTS_DIR.resolve("test_pb14_broken_slide1.pptx");
        try (ZipFile zf = new ZipFile(validTemplate.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(brokenSlideXmlPptx))) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                zos.putNextEntry(new ZipEntry(entry.getName()));
                if ("ppt/slides/slide1.xml".equals(entry.getName())) {
                    zos.write("<p:sld><broken unclosed xml".getBytes(StandardCharsets.UTF_8));
                } else {
                    zos.write(zf.getInputStream(entry).readAllBytes());
                }
                zos.closeEntry();
            }
        }
        assertThrowsIllegalArgument(() -> {
            try {
                PptxSlideRenderer.validateGalleryFile(brokenSlideXmlPptx.toString());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }, "PB-14: PPTX with corrupted slide1.xml must fail pre-validation");

        // 4. Truncated PNG (valid 8-byte PNG header, truncated body)
        Path truncatedPng = ARTIFACTS_DIR.resolve("test_pb14_truncated.png");
        Files.write(truncatedPng, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00});
        assertThrowsIllegalArgument(() -> {
            try {
                PptxSlideRenderer.validateGalleryFile(truncatedPng.toString());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }, "PB-14: Truncated PNG must fail pre-validation");

        // 5. Corrupted SVG
        Path tempCorruptedSvg = ARTIFACTS_DIR.resolve("test_pb14_corrupt.svg");
        Files.writeString(tempCorruptedSvg, "<svg><broken XML not closed");
        assertThrowsIllegalArgument(() -> {
            try {
                PptxSlideRenderer.validateGalleryFile(tempCorruptedSvg.toString());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }, "PB-14: Corrupted SVG XML must fail validation");

        // 6. End-to-end: failed import with corrupted slide1.xml PPTX must NOT wipe existing loaded tree/tabs
        PathwayBrowserExamples.install(PathwayBrowserExamples.directory());
        SwingUtilities.invokeAndWait(() -> {
            sharedMainFace = new PathwayFamilyMainFace(new IndependentModuleLoader());
            sharedMainFace.initializeTheModule();
        });
        String validExample = PathwayBrowserExamples.text(0, PathwayBrowserExamples.directory());
        sharedMainFace.getImportHandler().execute(new VoiceParameterParser().getOrganizedParameterGetter(validExample));
        SwingUtilities.invokeAndWait(() -> {
        }); // flush invokeLater
        var treeBefore = sharedMainFace.getSelectedPhylogeneticTreePanel();
        int tabsBefore = sharedMainFace.getTabbedAnalysisPanel().getTabCount();
        assertTrue(treeBefore != null && tabsBefore == 4, "PB-14: Initial example must load tree and 4 tabs");

        // End-to-end PB-10 tree selection check: 'Homo_sapiens' normalizes to 'Homo sapiens', while 'Homo' does NOT substring-match
        SwingUtilities.invokeAndWait(() -> {
            sharedMainFace.getController().selectNodeInTree("Homo_sapiens", null);
            var selected = sharedMainFace.getController().getActiveTreeLayoutProperties().getSelectedNodes();
            assertEquals(1, selected.size(), "PB-10: Normalized species 'Homo_sapiens' must select 'Homo sapiens' in tree");
            assertEquals("Homo sapiens", selected.getFirst().getName(), "PB-10: Selected node must be 'Homo sapiens'");

            sharedMainFace.getController().selectNodeInTree("Homo", null);
            assertEquals("Homo sapiens",
                    sharedMainFace.getController().getActiveTreeLayoutProperties().getSelectedNodes().getFirst().getName(),
                    "PB-10: Partial prefix 'Homo' must NOT override selection via substring match");
        });

        String badImportText = "$input.newick.text=((Human:1,Mouse:1)Root:1);\n$pathway.gallery.figure.paths=\n"
                + brokenSlideXmlPptx.toAbsolutePath();
        assertThrowsIllegalArgument(() -> sharedMainFace.getImportHandler().execute(
                new VoiceParameterParser().getOrganizedParameterGetter(badImportText)
        ), "PB-14: Import with broken slide1.xml PPTX must fail pre-validation");

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(treeBefore, sharedMainFace.getSelectedPhylogeneticTreePanel(),
                    "PB-14: Existing tree panel must remain intact after failed gallery import");
            assertEquals(tabsBefore, sharedMainFace.getTabbedAnalysisPanel().getTabCount(),
                    "PB-14: Existing analysis tabs must remain intact after failed gallery import");
        });
    }

    private static PathwayFamilyMainFace sharedMainFace;

    private static void testPb15CountValidationEdgeCases() {
        int row = 2;
        String col = "Human";

        assertEquals(CountState.ABSENT, PathwayComponentCountData.parseCountEntry("0", row, col).state(), "0 is ABSENT");
        assertEquals(3, PathwayComponentCountData.parseCountEntry("3", row, col).count(), "3 is PRESENT(3)");
        assertEquals(CountState.MISSING, PathwayComponentCountData.parseCountEntry("NA", row, col).state(), "NA is MISSING");
        assertEquals(CountState.MISSING, PathwayComponentCountData.parseCountEntry("-", row, col).state(), "'-' is MISSING");
        assertEquals(CountState.MISSING, PathwayComponentCountData.parseCountEntry("", row, col).state(), "empty is MISSING");

        assertThrowsIllegalArgument(() -> PathwayComponentCountData.parseCountEntry("-1", row, col), "PB-15: Negative count must be rejected");
        assertThrowsIllegalArgument(() -> PathwayComponentCountData.parseCountEntry("1.5", row, col), "PB-15: Fractional count must be rejected");
        assertThrowsIllegalArgument(() -> PathwayComponentCountData.parseCountEntry("NaN", row, col), "PB-15: NaN must be rejected");
        assertThrowsIllegalArgument(() -> PathwayComponentCountData.parseCountEntry("Infinity", row, col), "PB-15: Infinity must be rejected");
        assertThrowsIllegalArgument(() -> PathwayComponentCountData.parseCountEntry("2147483648", row, col), "PB-15: Overflow count must be rejected");
    }

    private static void testPb16CategoryCaseInsensitivity() {
        PathwayComponentCountData data1 = PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("APC", "cat1", "2"), List.of("AXIN", "CAT1", "3"))
        );
        PathwayComponentCountData data2 = PathwayComponentCountData.fromRawTable(
                List.of("name", "category", "Human"),
                List.of(List.of("AXIN", "CAT1", "3"), List.of("APC", "cat1", "2"))
        );

        assertEquals(5, data1.resolveCategoryForSpecies("Human", "CAT1").count(), "PB-16: cat1 then CAT1 must sum to 5");
        assertEquals(5, data2.resolveCategoryForSpecies("Human", "cat1").count(), "PB-16: CAT1 then cat1 must sum to 5 regardless of row order");
    }

    private static void testV01SaveUtilAndExportTarget() throws Exception {
        if (sharedMainFace == null) {
            PathwayBrowserExamples.install(PathwayBrowserExamples.directory());
            SwingUtilities.invokeAndWait(() -> {
                sharedMainFace = new PathwayFamilyMainFace(new IndependentModuleLoader());
                sharedMainFace.initializeTheModule();
            });
            String validExample = PathwayBrowserExamples.text(0, PathwayBrowserExamples.directory());
            sharedMainFace.getImportHandler().execute(new VoiceParameterParser().getOrganizedParameterGetter(validExample));
        }
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = sharedMainFace.getTabbedAnalysisPanel();
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (tabs.getComponentAt(i) instanceof PathwayGalleryPanel) {
                    tabs.setSelectedIndex(i);
                    break;
                }
            }
        });
        // Wait briefly for SubSingleSlidePanel background SwingWorker to complete
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            boolean[] loaded = {false};
            SwingUtilities.invokeAndWait(() -> {
                JPanel target = sharedMainFace.resolveExportTargetPanel();
                loaded[0] = (target instanceof SubSingleSlidePanel ssp) && ssp.isLoaded();
            });
            if (loaded[0]) {
                break;
            }
            Thread.sleep(50);
        }

        SwingUtilities.invokeAndWait(() -> {
            JPanel target = sharedMainFace.resolveExportTargetPanel();
            assertTrue(target instanceof SubSingleSlidePanel,
                    "V-01: Export target when PathwayGalleryPanel is active must resolve to full-canvas SubSingleSlidePanel");
            assertTrue(target.getWidth() >= 400 && target.getHeight() >= 300,
                    "V-01: Export target SubSingleSlidePanel must have full slide dimensions (" + target.getWidth() + "x" + target.getHeight() + ")");
        });
    }

    private static void testV02BoundedLruCache() throws Exception {
        PptxSlideRenderer.clearCache();
        assertEquals(0, PptxSlideRenderer.getCacheSize(), "V-02: Cache must start empty after clearCache()");

        Path pptx = ARTIFACTS_DIR.resolve("test_v02_lru.pptx");
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.createSlide().createAutoShape().setAnchor(new Rectangle2D.Double(10, 10, 30, 20));
            try (FileOutputStream fos = new FileOutputStream(pptx.toFile())) {
                ppt.write(fos);
            }
        }

        for (int i = 0; i < 15; i++) {
            PathwayComponentCountData cd = PathwayComponentCountData.fromRawTable(
                    List.of("name", "Human"),
                    List.of(List.of("GENE_" + i, "1"))
            );
            PptxSlideRenderer.prepareSlide(pptx.toString(), cd, () -> false);
        }

        assertEquals(12, PptxSlideRenderer.getCacheSize(),
                "V-02: Bounded LRU cache must cap retained entries at MAX_CACHE_ENTRIES (12) after loading 15 distinct signatures");
    }

    private static void testV03ModuleClosedGuard() {
        Voice4pathwayFamilyBrowser voice = new Voice4pathwayFamilyBrowser(null);
        voice.cancelPendingImport();
        voice.setModuleClosed(true);
        assertTrue(voice.isModuleClosed(), "V-03: moduleClosed flag must be set");
        voice.doUserImportAction();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void assertThrowsIllegalArgument(ThrowingRunnable runnable, String message) {
        boolean caught = false;
        try {
            runnable.run();
        } catch (IllegalArgumentException e) {
            caught = true;
        } catch (Exception e) {
            throw new AssertionError(message + " - threw unexpected checked exception: " + e, e);
        }
        assertTrue(caught, message);
    }
}
