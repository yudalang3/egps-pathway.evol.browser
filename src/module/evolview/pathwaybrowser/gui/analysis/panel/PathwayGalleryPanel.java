package module.evolview.pathwaybrowser.gui.analysis.panel;

import com.google.common.collect.Lists;
import egps2.frame.gui.EGPSMainGuiUtil;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

public class PathwayGalleryPanel extends AbstractAnalysisPanel{
    private final List<String> files;
    private List<XSLFSlide> slides;

    public PathwayGalleryPanel(PathwayBrowserController controller, List< String> files) {
        super(controller);
        this.files = files;
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public void reInitializeGUI() {
        if (files != null && !files.isEmpty()){
//            renderSlideImageAsync();
        }
    }

    @Override
    public void treeNodeClicked(String nodeName) {

    }

//    private void renderSlideImageAsync() {
//        if (isRendering || pageSize == null) {
//            return;
//        }
//
//        isRendering = true;
//
//        new SwingWorker<BufferedImage, Void>() {
//            @Override
//            protected BufferedImage doInBackground() throws Exception {
//                // Create and render image in background thread
//                BufferedImage image = new BufferedImage(
//                        pageSize.width,
//                        pageSize.height,
//                        BufferedImage.TYPE_INT_ARGB
//                );
//                Graphics2D g2d = image.createGraphics();
//                EGPSMainGuiUtil.setupHighQualityRendering(g2d);
//                firstSlide.draw(g2d);
//                g2d.dispose();
//                return image;
//            }
//
//            @Override
//            protected void done() {
//                try {
//                    cachedSlideImage = get();
//                    repaint();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    isRendering = false;
//                }
//            }
//        }.execute();
//    }
}
