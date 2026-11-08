package module.evolview.pathwaybrowser;

import module.evolview.pathwaybrowser.io.*;
import module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.io.File;
import org.apache.poi.xslf.usermodel.*;

public final class P3BoundaryTest {
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createDirectories(Path.of("temp", "p3-boundaries"));
        SwingUtilities.invokeAndWait(() -> {
            var panel = new JPanel() { @Override protected void paintComponent(Graphics g) { g.setColor(Color.RED); g.fillRect(0,0,getWidth(),getHeight()); } };
            panel.setSize(120,80);
            class Saver extends PathwaySnapshotSaveUtil {
                boolean overwrite = true;
                @Override protected boolean confirmOverwrite(Component parent, File file) { require(parent == panel, "Wrong fallback parent"); return overwrite; }
                @Override protected void showSaveError(Component parent, Exception error) { throw new AssertionError(error); }
                Optional<File> run(Path path, String ext, boolean cancel) {
                    return process(panel, (org.apache.commons.lang3.tuple.Pair<egps2.utils.common.util.SaveFileFilter, java.util.function.Consumer<String>>) null, new JFileChooser() {
                        @Override public int showSaveDialog(Component parent) {
                            require(parent == panel, "Wrong chooser parent");
                            for (var filter : getChoosableFileFilters()) if (filter instanceof FileNameExtensionFilter f && f.getExtensions()[0].equals(ext)) setFileFilter(filter);
                            setSelectedFile(path.toFile()); return cancel ? CANCEL_OPTION : APPROVE_OPTION;
                        }
                    });
                }
            }
            var saver = new Saver();
            try {
                for (String suffix : new String[]{"PNG", "JPG", "PDF", "pNg", "jPg", "pDf", "SVG", "EPS", "PPTX"}) {
                    Path file = dir.resolve("diagram." + suffix); Files.writeString(file,"old");
                    require(saver.run(file,suffix.toLowerCase(java.util.Locale.ROOT),false).orElseThrow().equals(file.toFile()), "File renamed");
                    require(Files.size(file)>3, "File not overwritten");
                    if (suffix.equalsIgnoreCase("png") || suffix.equalsIgnoreCase("jpg")) {
                        var image=javax.imageio.ImageIO.read(file.toFile());
                        require(image!=null && image.getWidth()==120 && image.getHeight()==80, "Invalid bitmap");
                        require(new Color(image.getRGB(60,40)).getRed()>240, "Missing pixels");
                    }
                    if (suffix.equalsIgnoreCase("pdf")) {
                        var pdf=new com.itextpdf.text.pdf.PdfReader(Files.readAllBytes(file));
                        try { require(pdf.getNumberOfPages()==1,"Invalid PDF"); } finally { pdf.close(); }
                    }
                    byte[] before=Files.readAllBytes(file); saver.overwrite=false;
                    require(saver.run(file,suffix.toLowerCase(java.util.Locale.ROOT),false).isEmpty(),"Overwrite refused");
                    require(saver.run(file,suffix.toLowerCase(java.util.Locale.ROOT),true).isEmpty(),"Cancel refused");
                    require(java.util.Arrays.equals(before,Files.readAllBytes(file)),"Refusal changed file"); saver.overwrite=true;
                }
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        for (int alpha : new int[]{255,0}) for (boolean vertical : new boolean[]{false,true}) for (int rotation : new int[]{0,30,90}) for (boolean flip : new boolean[]{false,true}) {
            Path file=dir.resolve("line-"+alpha+"-"+vertical+"-"+rotation+"-"+flip+".pptx");
            try(var ppt=new XMLSlideShow()) {
                ppt.setPageSize(new Dimension(400,300)); var slide=ppt.createSlide();
                var outer=slide.createGroup(); outer.setAnchor(new Rectangle2D.Double(40,40,260,160)); outer.setInteriorAnchor(new Rectangle2D.Double(0,0,200,200)); outer.setFlipHorizontal(flip);
                var inner=outer.createGroup(); inner.setAnchor(new Rectangle2D.Double(0,0,200,200)); inner.setInteriorAnchor(new Rectangle2D.Double(0,0,200,200)); inner.setRotation(rotation);
                var line=inner.createConnector(); line.setAnchor(new Rectangle2D.Double(60,60,vertical?0:80,vertical?80:0)); line.setLineColor(new Color(255,0,0,alpha)); line.setLineWidth(3);
                ((org.openxmlformats.schemas.presentationml.x2006.main.CTConnector)line.getXmlObject()).getNvCxnSpPr().getCNvPr().setName("A_comp");
                try(var output=Files.newOutputStream(file)){ppt.write(output);}
            }
            var counts=PathwayComponentCountData.fromRawTable(List.of("Name","A"),List.of(List.of("Human","2")));
            var slide=PptxSlideRenderer.prepareSlide(file.toString(),counts,()->false);
            var clusters=slide.geneSprites().get("A").instanceClusters();
            if (alpha == 0 && rotation == 0) { require(clusters.isEmpty(),"Transparent flat line gained a badge region"); continue; }
            if (alpha == 0) continue;
            require(!clusters.isEmpty() && clusters.getFirst().getWidth()>0 && clusters.getFirst().getHeight()>0,"Line badge lost");
            require(slide.geneSprites().get("A").spriteImage().getWidth()>0,"Line pixels lost");
        }
        System.out.println("P3 filename, cancellation and line-boundary checks passed"); System.exit(0);
    }
}
