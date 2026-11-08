package module.evolview.pathwaybrowser.gui.analysis.panel;

import com.jidesoft.swing.JideTabbedPane;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.io.File;
import java.util.ArrayList;
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
    private JideTabbedPane tabbedPane;
    private volatile PathwayComponentCountData cachedCountData;
    private boolean closed;

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

        tabbedPane.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentRemoved(ContainerEvent e) {
                SubSingleSlidePanel removed = extractSlidePanel(e.getChild());
                if (removed != null) {
                    removed.cancelLoad();
                }
                if (tabbedPane.getTabCount() > 0) {
                    SwingUtilities.invokeLater(PathwayGalleryPanel.this::loadActiveTabAsync);
                }
            }
        });

        loadAllSlides();

        tabbedPane.addChangeListener(e -> loadActiveTabAsync());
    }

    public JideTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void installPrepared(module.evolview.pathwaybrowser.io.PreparedImport prepared) {
        cachedCountData = prepared.counts();
        for (SubSingleSlidePanel panel : getLiveSlidePanels()) {
            panel.installPrepared(prepared.slides().get(panel.getFilePath()), prepared.counts());
        }
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
        if (closed) return;
        SubSingleSlidePanel active = getActiveSlidePanel();
        if (active != null) {
            active.loadSlideAsync();
        }
    }

    public SubSingleSlidePanel getActiveSlidePanel() {
        if (tabbedPane == null) return null;
        Component comp = tabbedPane.getSelectedComponent();
        return extractSlidePanel(comp);
    }

    private SubSingleSlidePanel extractSlidePanel(Component comp) {
        if (comp instanceof SubSingleSlidePanel sp) {
            return sp;
        }
        if (comp instanceof JScrollPane scroll) {
            Component view = scroll.getViewport().getView();
            if (view instanceof SubSingleSlidePanel sp) {
                return sp;
            }
        }
        return null;
    }

    public List<SubSingleSlidePanel> getLiveSlidePanels() {
        if (tabbedPane == null) return List.of();
        List<SubSingleSlidePanel> list = new ArrayList<>();
        int count = tabbedPane.getTabCount();
        for (int i = 0; i < count; i++) {
            SubSingleSlidePanel sp = extractSlidePanel(tabbedPane.getComponentAt(i));
            if (sp != null) {
                list.add(sp);
            }
        }
        return list;
    }

    @Override
    public void treeNodeClicked(String nodeName) {
        if (closed || nodeName == null || nodeName.isBlank()) {
            return;
        }
        for (SubSingleSlidePanel slidePanel : getLiveSlidePanels()) {
            slidePanel.setSelectedNodeName(nodeName);
        }
        loadActiveTabAsync();
    }

    @Override
    public void cancelPendingLoad() {
        closed = true;
        super.cancelPendingLoad();
        for (SubSingleSlidePanel slidePanel : getLiveSlidePanels()) {
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

            tabbedPane.addTab(tabTitle, scrollPane);
            log.debug("Added tab {} for file: {}", i + 1, tabTitle);
        }

        if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }
    }
}
