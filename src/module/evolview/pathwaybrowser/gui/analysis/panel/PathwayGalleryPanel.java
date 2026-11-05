package module.evolview.pathwaybrowser.gui.analysis.panel;

import com.google.common.collect.Lists;
import com.jidesoft.swing.JideTabbedPane;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Gallery panel for displaying multiple pathway diagrams (PPTX, SVG, PNG, JPG).
 * Each file is shown in a separate tab. Clicking a species node on the phylogenetic tree
 * dynamically overlays component copy numbers and highlights on matched shapes.
 */
public class PathwayGalleryPanel extends AbstractAnalysisPanel {
    private static final Logger log = LoggerFactory.getLogger(PathwayGalleryPanel.class);

    private final List<String> files;
    private final String componentCountFilePath;
    private final List<SubSingleSlidePanel> slidesPanel = Lists.newArrayList();
    private JideTabbedPane tabbedPane;
    private volatile PathwayComponentCountData cachedCountData;

    public PathwayGalleryPanel(PathwayBrowserController controller, List<String> files) {
        this(controller, files, null);
    }

    public PathwayGalleryPanel(PathwayBrowserController controller, List<String> files, String componentCountFilePath) {
        super(controller);
        this.files = files;
        this.componentCountFilePath = componentCountFilePath;
        initializeUI();
    }

    private PathwayComponentCountData getOrLoadCountData() {
        PathwayComponentCountData local = cachedCountData;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cachedCountData == null) {
                if (componentCountFilePath != null && !componentCountFilePath.isBlank()
                        && !componentCountFilePath.equalsIgnoreCase("false")
                        && !componentCountFilePath.equalsIgnoreCase("f")
                        && !componentCountFilePath.equalsIgnoreCase("null")) {
                    try {
                        cachedCountData = PathwayComponentCountData.load(new File(componentCountFilePath));
                    } catch (Exception e) {
                        log.warn("Unable to load component count data for gallery overlay: {}", componentCountFilePath, e);
                        cachedCountData = PathwayComponentCountData.empty();
                    }
                } else {
                    cachedCountData = PathwayComponentCountData.empty();
                }
            }
            return cachedCountData;
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        tabbedPane = new JideTabbedPane();
        tabbedPane.setTabShape(JideTabbedPane.SHAPE_OFFICE2003);
        tabbedPane.setTabPlacement(JideTabbedPane.LEFT);
        tabbedPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_NONE);

        tabbedPane.setShowTabArea(true);
        tabbedPane.setShowTabContent(true);

        tabbedPane.setTabEditingAllowed(true);
        tabbedPane.setShowCloseButtonOnTab(true);
        Font tabFont = controller != null ? controller.getGlobalFont() : null;
        if (tabFont != null) {
            tabbedPane.setFont(tabFont);
            tabbedPane.setSelectedTabFont(tabFont.deriveFont(Font.BOLD));
        }
        tabbedPane.setBoldActiveTab(true);

        add(tabbedPane, BorderLayout.CENTER);

        loadAllSlides();

        tabbedPane.addChangeListener(e -> loadActiveTabAsync());
    }

    @Override
    public String getTitle() {
        return "Pathway Gallery";
    }

    @Override
    public void reInitializeGUI() {
        loadActiveTabAsync();
    }

    private void loadActiveTabAsync() {
        if (slidesPanel.isEmpty()) {
            return;
        }
        int idx = tabbedPane != null ? tabbedPane.getSelectedIndex() : 0;
        if (idx < 0 || idx >= slidesPanel.size()) {
            idx = 0;
        }
        slidesPanel.get(idx).loadSlideAsync();
    }

    @Override
    public void treeNodeClicked(String nodeName) {
        if (nodeName == null || nodeName.isBlank()) {
            return;
        }
        for (SubSingleSlidePanel slidePanel : slidesPanel) {
            slidePanel.setSelectedNodeName(nodeName);
        }
        loadActiveTabAsync();
    }

    @Override
    public void cancelPendingLoad() {
        super.cancelPendingLoad();
        for (SubSingleSlidePanel slidePanel : slidesPanel) {
            slidePanel.cancelLoad();
        }
    }

    private void loadAllSlides() {
        for (int i = 0; i < files.size(); i++) {
            String filePath = files.get(i);
            File file = new File(filePath);
            String tabTitle = file.getName();

            SubSingleSlidePanel slidePanel = new SubSingleSlidePanel(filePath, this::getOrLoadCountData);
            JScrollPane scrollPane = new JScrollPane(slidePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            slidesPanel.add(slidePanel);

            tabbedPane.addTab(tabTitle, scrollPane);
            log.debug("Added tab {} for file: {}", i + 1, tabTitle);
        }

        if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }
    }
}
