package module.evolview.pathwaybrowser.gui.analysis.panel;

import com.google.common.collect.Lists;
import com.jidesoft.swing.JideTabbedPane;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Gallery panel for displaying multiple PPTX pathway diagrams.
 * Each PPTX file is shown in a separate tab, displaying only the first slide.
 */
public class PathwayGalleryPanel extends AbstractAnalysisPanel {
    private static final Logger log = LoggerFactory.getLogger(PathwayGalleryPanel.class);

    private final List<String> files;
    private final List<SubSingleSlidePanel> slidesPanel = Lists.newArrayList();
    private JideTabbedPane tabbedPane;

    public PathwayGalleryPanel(PathwayBrowserController controller, List<String> files) {
        super(controller);
        this.files = files;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create JideTabbedPane with vertical tab placement (LEFT)
        tabbedPane = new JideTabbedPane();
        tabbedPane.setTabShape(JideTabbedPane.SHAPE_OFFICE2003);
        tabbedPane.setTabPlacement(JideTabbedPane.LEFT);  // Vertical split layout

        tabbedPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_FIT);

        tabbedPane.setShowTabArea(true);
        tabbedPane.setShowTabContent(true);

        tabbedPane.setTabEditingAllowed(true);
        tabbedPane.setShowCloseButtonOnTab(true);
        tabbedPane.setBoldActiveTab(true);

        add(tabbedPane, BorderLayout.CENTER);

        loadAllSlides();
    }

    @Override
    public String getTitle() {
        return "Pathway Gallery";
    }

    @Override
    public void reInitializeGUI() {
        if (files != null && !files.isEmpty()) {
            for (SubSingleSlidePanel slidePanel: slidesPanel){
                slidePanel.loadSlideAsync();
            }
        }
    }

    @Override
    public void treeNodeClicked(String nodeName) {
        // Handle tree node click if needed
    }

    /**
     * Load all PPTX files and add them as tabs
     */
    private void loadAllSlides() {
        for (int i = 0; i < files.size(); i++) {
            String filePath = files.get(i);
            File file = new File(filePath);
            String tabTitle = file.getName();

            // Create a SubSingleSlidePanel for each PPTX file
            SubSingleSlidePanel slidePanel = new SubSingleSlidePanel(filePath);
            JScrollPane scrollPane = new JScrollPane(slidePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            slidesPanel.add(slidePanel);

            // Add as a new tab
            tabbedPane.addTab(tabTitle, scrollPane);
            tabbedPane.setFont(controller.getGlobalFont());

            log.debug("Added tab {} for file: {}", i + 1, tabTitle);
        }

        // Select the first tab
        if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }
    }
}
