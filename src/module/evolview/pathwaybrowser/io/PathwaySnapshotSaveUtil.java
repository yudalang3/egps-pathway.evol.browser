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
        chooser.resetChoosableFileFilters();
        chooser.setAcceptAllFileFilterUsed(false);
        for (String extension : new String[]{"png", "jpg", "pptx", "svg", "pdf", "eps"})
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " (*." + extension + ")", extension));
        if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        String extension = ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0];
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith("." + extension)) file = new File(file.getPath() + "." + extension);
        if (file.exists() && JOptionPane.showConfirmDialog(panel, "Overwrite " + file.getName() + "?", "Export", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return Optional.empty();
        try {
            if (extension.equals("pptx")) savePptx(panel, file);
            else directlyProduceFigureAccording2filePath(file.toString(), panel);
            return Optional.of(file);
        } catch (Exception error) {
            JOptionPane.showMessageDialog(panel, error.getMessage(), "Export failed", JOptionPane.ERROR_MESSAGE);
            return Optional.empty();
        }
    }
}
