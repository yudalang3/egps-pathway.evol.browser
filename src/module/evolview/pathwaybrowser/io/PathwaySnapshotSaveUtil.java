package module.evolview.pathwaybrowser.io;

import egps2.utils.common.util.SaveUtil;
import egps2.utils.common.util.SaveFileFilter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.sl.usermodel.PictureData.PictureType;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;

/** Module-local saving: PPTX explicitly embeds the rendered canvas rather than SLGraphics. */
public class PathwaySnapshotSaveUtil extends SaveUtil {
    public static void savePptx(JComponent panel, File file) throws IOException {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try { panel.paint(graphics); } finally { graphics.dispose(); }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", png);
        try (XMLSlideShow presentation = new XMLSlideShow(); OutputStream output = new FileOutputStream(file)) {
            presentation.setPageSize(panel.getSize());
            var picture = presentation.addPicture(png.toByteArray(), PictureType.PNG);
            presentation.createSlide().createPicture(picture).setAnchor(new Rectangle2D.Double(0, 0, panel.getWidth(), panel.getHeight()));
            presentation.write(output);
        }
    }

    @Override protected Optional<File> process(JComponent panel, Pair<SaveFileFilter, Consumer<String>> action, JFileChooser chooser) {
        Component parent = egps2.UnifiedAccessPoint.isGULaunched() ? egps2.UnifiedAccessPoint.getInstanceFrame() : panel;
        chooser.resetChoosableFileFilters();
        chooser.setAcceptAllFileFilterUsed(false);
        for (String extension : new String[]{"png", "jpg", "pptx", "svg", "pdf", "eps"})
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " (*." + extension + ")", extension));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        String extension = ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0];
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith("." + extension)) file = new File(file.getPath() + "." + extension);
        if (file.exists() && !confirmOverwrite(parent, file)) return Optional.empty();
        try {
            switch (extension) {
                case "pptx" -> savePptx(panel, file);
                case "png", "jpg" -> egps2.utils.common.util.EGPSPrintUtilities.saveAsBitGraphics(extension, panel, file);
                case "pdf" -> egps2.utils.common.util.EGPSPrintUtilities.saveAsPDF(panel, file);
                case "svg", "eps" -> egps2.frame.gui.VectorGraphicsEncoder.saveVectorGraphic(panel, file.getAbsolutePath(),
                        extension.equals("svg") ? egps2.frame.gui.VectorGraphicsEncoder.VectorGraphicsFormat.SVG : egps2.frame.gui.VectorGraphicsEncoder.VectorGraphicsFormat.EPS);
                default -> throw new IllegalArgumentException("Unsupported format: " + extension);
            }
            return Optional.of(file);
        } catch (Exception error) {
            showSaveError(parent, error);
            return Optional.empty();
        }
    }

    protected boolean confirmOverwrite(Component parent, File file) {
        return JOptionPane.showConfirmDialog(parent, "Overwrite " + file.getName() + "?", "Export", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    protected void showSaveError(Component parent, Exception error) {
        JOptionPane.showMessageDialog(parent, error.getMessage(), "Export failed", JOptionPane.ERROR_MESSAGE);
    }

}
