package module.evolview.pathwaybrowser.gui.analysis.panel;

import egps2.frame.gui.EGPSMainGuiUtil;
import egps2.panels.dialog.SwingDialog;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Panel for displaying a single slide from a PPTX file.
 * This panel loads and renders the first slide of a PowerPoint presentation.
 */
public class SubSingleSlidePanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(SubSingleSlidePanel.class);

    private final String pptxFilePath;
    private Dimension pageSize;
    private BufferedImage cachedSlideImage;
    private boolean isRendering = false;

    public SubSingleSlidePanel(String pptxFilePath) {
        this.pptxFilePath = pptxFilePath;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (cachedSlideImage != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            EGPSMainGuiUtil.setupHighQualityRendering(g2d);
            g2d.drawImage(cachedSlideImage, 0, 0, null);
            g2d.dispose();
        }
    }

    void loadSlideAsync() {
        if (isRendering) {
            return;
        }

        isRendering = true;

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                // Load PPTX and render in background thread
                try (var input = java.nio.file.Files.newInputStream(java.nio.file.Path.of(pptxFilePath));
                     var presentation = new XMLSlideShow(input)) {
                XSLFSlide firstSlide = presentation.getSlides().isEmpty() ? null : presentation.getSlides().getFirst();
                pageSize = presentation.getPageSize();

                if (firstSlide == null || pageSize == null) {
                    return null;
                }

                // Render slide to image in the same background thread
                BufferedImage image = new BufferedImage(
                        pageSize.width,
                        pageSize.height,
                        BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2d = image.createGraphics();
                EGPSMainGuiUtil.setupHighQualityRendering(g2d);
                firstSlide.draw(g2d);
                g2d.dispose();

                return image;
                }
            }

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image != null && pageSize != null) {
                        cachedSlideImage = image;
                        setPreferredSize(pageSize);
                        revalidate();
                        repaint();
                    }
                } catch (Exception e) {
                    log.error("Failed to load and render PPTX file: {}", e.getMessage(), e);
                    SwingDialog.showErrorMSGDialog("Input error", "Please check the pptx file: " + pptxFilePath);
                } finally {
                    isRendering = false;
                }
            }
        }.execute();
    }
}
