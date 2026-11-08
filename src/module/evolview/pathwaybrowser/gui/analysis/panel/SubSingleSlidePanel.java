package module.evolview.pathwaybrowser.gui.analysis.panel;

import egps2.frame.gui.EGPSMainGuiUtil;
import egps2.panels.dialog.SwingDialog;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

/**
 * Panel for displaying a single pathway diagram from a PPTX, SVG, PNG, or JPG file.
 * Pre-renders the static skeleton and dynamic component sprites in a background worker,
 * and overlays copy counts when a species node is selected on the tree.
 */
public class SubSingleSlidePanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(SubSingleSlidePanel.class);

    private final String filePath;
    private final Supplier<PathwayComponentCountData> countDataSupplier;
    private Dimension pageSize;
    private PptxSlideRenderer.PreparedSlide preparedSlide;
    private PathwayComponentCountData countData;
    private BufferedImage cachedSlideImage;
    private String selectedNodeName;
    private boolean isRendering = false;
    private volatile boolean cancelled = false;
    private SwingWorker<PptxSlideRenderer.PreparedSlide, Void> worker;

    public SubSingleSlidePanel(String filePath) {
        this(filePath, PathwayComponentCountData::empty);
    }

    public SubSingleSlidePanel(String filePath, Supplier<PathwayComponentCountData> countDataSupplier) {
        this.filePath = filePath;
        this.countDataSupplier = countDataSupplier != null ? countDataSupplier : PathwayComponentCountData::empty;
        setBackground(Color.WHITE);
    }

    public String getFilePath() {
        return filePath;
    }

    public void cancelLoad() {
        cancelled = true;
        if (worker != null) worker.cancel(true);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isLoaded() {
        return preparedSlide != null;
    }

    public BufferedImage getCachedSlideImage() {
        return cachedSlideImage;
    }

    /** A fixed-size snapshot, independent of the scroll viewport and later selection. */
    public JPanel createExportSnapshot() {
        if (cancelled || cachedSlideImage == null) return null;
        BufferedImage image = new BufferedImage(cachedSlideImage.getWidth(), cachedSlideImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(cachedSlideImage, 0, 0, null);
        graphics.dispose();
        JPanel snapshot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        };
        snapshot.setBackground(Color.WHITE);
        snapshot.setSize(image.getWidth(), image.getHeight());
        snapshot.setPreferredSize(snapshot.getSize());
        return snapshot;
    }

    public void installPrepared(PptxSlideRenderer.PreparedSlide slide, PathwayComponentCountData counts) {
        if (cancelled) return;
        if (slide == null) throw new IllegalArgumentException("Missing prepared gallery: " + filePath);
        preparedSlide = slide;
        countData = counts;
        pageSize = slide.pageSize();
        cachedSlideImage = PptxSlideRenderer.renderForSpecies(slide, counts, selectedNodeName);
        setPreferredSize(pageSize);
        setSize(pageSize);
        revalidate();
        repaint();
    }

    void setSelectedNodeName(String nodeName) {
        if (cancelled) return;
        this.selectedNodeName = nodeName;
        if (preparedSlide != null) {
            cachedSlideImage = PptxSlideRenderer.renderForSpecies(preparedSlide, countData, selectedNodeName);
            repaint();
        }
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
        if (cancelled) return;
        if (preparedSlide != null) {
            cachedSlideImage = PptxSlideRenderer.renderForSpecies(preparedSlide, countData, selectedNodeName);
            repaint();
            return;
        }
        if (isRendering) {
            return;
        }

        cancelled = false;
        isRendering = true;

        worker = new SwingWorker<PptxSlideRenderer.PreparedSlide, Void>() {
            private PathwayComponentCountData loadedCountData;

            @Override
            protected PptxSlideRenderer.PreparedSlide doInBackground() throws Exception {
                if (cancelled) {
                    return null;
                }
                loadedCountData = countDataSupplier.get();
                if (cancelled) {
                    return null;
                }
                return PptxSlideRenderer.prepareSlide(filePath, loadedCountData, () -> cancelled);
            }

            @Override
            protected void done() {
                if (cancelled) {
                    isRendering = false;
                    return;
                }
                try {
                    PptxSlideRenderer.PreparedSlide slide = get();
                    if (slide != null && slide.pageSize() != null) {
                        preparedSlide = slide;
                        countData = loadedCountData;
                        pageSize = slide.pageSize();
                        cachedSlideImage = PptxSlideRenderer.renderForSpecies(preparedSlide, countData, selectedNodeName);
                        setPreferredSize(pageSize);
                        if (getWidth() <= 0 || getHeight() <= 0) {
                            setSize(pageSize);
                        }
                        revalidate();
                        repaint();
                    }
                } catch (Exception e) {
                    if (!cancelled) {
                        log.error("Failed to load and render pathway gallery file: {}", e.getMessage(), e);
                        SwingDialog.showErrorMSGDialog("Input error", "Please check the pathway gallery file: " + filePath);
                    }
                } finally {
                    isRendering = false;
                }
            }
        };
        worker.execute();
    }
}
