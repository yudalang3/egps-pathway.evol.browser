package module.evolview.phylotree.visualization.layout;

import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import module.evolview.moderntreeviewer.para.TreeSettings;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Numeric branch labels must not change the pen used by subsequent branch drawing. */
public class BranchLengthDisplayRegressionTest {
    private static final Path OUTPUT = Path.of("temp/branch-length-display");
    private static final int WIDTH = 1000, HEIGHT = 740;

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record NodeState(GraphicsNode parent, int children, double length, Color color,
                             Stroke stroke, int radius, boolean name, double x, double y) {
        static NodeState capture(GraphicsNode node) {
            var unit = node.getDrawUnit();
            return new NodeState((GraphicsNode) node.getParent(), node.getChildCount(),
                    node.getRealBranchLength(), unit.getLineColor(), unit.getStroke(),
                    unit.getCircleRadius(), unit.isDrawName(), node.getXSelf(), node.getYSelf());
        }
    }

    private static BufferedImage render(BaseLayout layout) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            layout.paintGraphics(graphics);
            layout.drawFixedTitle(graphics, WIDTH, HEIGHT);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Map<String, Object> otherSettings(PhylogeneticTreePanel tree) {
        Map<String, Object> settings = new LinkedHashMap<>(TreeSettings.snapshot(tree));
        settings.remove("Labels/Show branch lengths");
        return settings;
    }

    private static void testLayout(TreeLayout type, int curvature) throws Exception {
        MTVImportInforBean input = new MTVImportInforBean();
        input.setInput_nwk_string("(Orangutan:12.5,(Gorilla:8.5,(Human:6.5,(Chimpanzee:1.5,Bonobo:1.5):5):2):4);");
        GraphicsNode root = new TreeParser4MTV().parseTree(input).orElseThrow();
        List<GraphicsNode> nodes = PhylogeneticTreePanel.subtreeNodes(root);
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).getDrawUnit().setLineColor(i % 2 == 0 ? Color.RED : Color.GREEN);
        }
        TreeLayoutProperties properties = new TreeLayoutProperties(root);
        properties.setMyLayout(type);
        properties.setGlobalFont(new Font("Dialog", Font.PLAIN, 14));
        properties.setTitleString("Great apes: teaching time tree");
        properties.setShowTitle(true);
        properties.setShowAxisBar(true);
        properties.setShowScaleBar(true);
        properties.setBranchLengthUnit("MYA (illustrative)");
        properties.getRectangularLayoutPropertiy().setCurvature(curvature);
        LeafLabelStateInitializer.applyInitialLeafLabelState(properties, true);
        PhylogeneticTreePanel tree = new PhylogeneticTreePanel(properties);
        JViewport viewport = new JViewport();
        viewport.setSize(WIDTH, HEIGHT);
        viewport.setView(tree);
        tree.initializeLeftPanel();
        tree.setTreePanelScale(120, 120);
        BaseLayout layout = tree.getCurrLayout();
        layout.calculateForPainting(WIDTH, HEIGHT);
        List<NodeState> beforeNodes = nodes.stream().map(NodeState::capture).toList();
        Map<String, Object> beforeSettings = otherSettings(tree);
        Dimension beforeScale = tree.getTreePanelScale();
        BufferedImage hidden = render(layout);
        properties.setShowNodeBranchLength(true);
        layout.calculateForPainting(WIDTH, HEIGHT);
        BufferedImage shown = render(layout);
        String name = type.name() + "-curvature-" + curvature;
        ImageIO.write(hidden, "png", OUTPUT.resolve(name + "-hidden.png").toFile());
        ImageIO.write(shown, "png", OUTPUT.resolve(name + "-shown.png").toFile());
        for (Color color : new Color[] {Color.RED, Color.GREEN}) {
            int total = 0, retained = 0;
            for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
                if (hidden.getRGB(x, y) == color.getRGB()) {
                    total++;
                    if (shown.getRGB(x, y) == color.getRGB()) retained++;
                }
            }
            require(total > 100, "Fixture contains colored branches: " + name);
            // Text may cross a few colored pixels, but must not recolor whole edges or arcs.
            require(retained >= total * .9, "Branch color retained with lengths: " + name
                    + " " + color + " retained " + retained + "/" + total);
        }
        require(!Arrays.equals(hidden.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH),
                shown.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH)), "Length text appears: " + name);
        require(beforeNodes.equals(nodes.stream().map(NodeState::capture).toList()), "Node state unchanged: " + name);
        require(beforeSettings.equals(otherSettings(tree)), "Other settings unchanged: " + name);
        require(beforeScale.equals(tree.getTreePanelScale()), "Zoom unchanged: " + name);

        Graphics2D pen = shown.createGraphics();
        try {
            pen.setFont(new Font("Monospaced", Font.BOLD, 19));
            var font = pen.getFont();
            var transform = pen.getTransform();
            GraphicsNode node = nodes.get(1);
            layout.commonDrawEachUnit(pen, node, false);
            require(pen.getColor().equals(node.getDrawUnit().getLineColor()), "Branch pen color isolated: " + name);
            require(pen.getFont().equals(font), "Branch pen font isolated: " + name);
            require(pen.getTransform().equals(transform), "Branch pen transform isolated: " + name);
        } finally {
            pen.dispose();
        }
        properties.setShowNodeBranchLength(false);
        layout.calculateForPainting(WIDTH, HEIGHT);
        BufferedImage restored = render(layout);
        require(Arrays.equals(hidden.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH),
                restored.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH)), "Toggle round trip restores all pixels: " + name);
        require(beforeNodes.equals(nodes.stream().map(NodeState::capture).toList()), "Round trip preserves tree: " + name);
        require(beforeSettings.equals(otherSettings(tree)), "Round trip preserves settings: " + name);
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        SwingUtilities.invokeAndWait(() -> {
            try {
                testLayout(TreeLayout.RECT_PHYLO_LEFT, 0);
                testLayout(TreeLayout.RECT_PHYLO_LEFT, 50);
                testLayout(TreeLayout.OUTER_CIR_PHYLO, 0);
                testLayout(TreeLayout.SPRIAL_ALPHA_PHYLO, 0);
                testLayout(TreeLayout.SPRIAL_BETA_PHYLO, 0);
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        });
        System.out.println("BranchLengthDisplayRegressionTest passed");
    }
}
