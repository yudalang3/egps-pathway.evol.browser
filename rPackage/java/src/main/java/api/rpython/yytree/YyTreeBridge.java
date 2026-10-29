package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import egps2.UnifiedAccessPoint;
import egps2.frame.gui.EGPSMainGuiUtil;
import egps2.panels.dialog.SwingDialog;
import egps2.panels.dialog.modern.ModernDialogType;
import graphic.engine.guicalculator.BlankArea;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.model.tree.ScaleBarProperty;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties4LinageType;
import module.evolview.phylotree.visualization.layout.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Standalone rendering bridge reused by one yytree JVM. No eGPS desktop launcher is used. */
public final class YyTreeBridge {
    private JFrame current;
    private final List<JFrame> windows = new ArrayList<>();
    private JDialog parameterError;
    private JFrame parameterErrorOwner;

    public String probeRuntime() throws Exception {
        if (Runtime.version().feature() < 25) throw new IllegalStateException("Java 25 or newer is required.");
        TreeLayoutHost.class.getMethod("getHostComponent");
        BaseLayout.class.getMethod("calculateForPainting", int.class, int.class);
        for (Class<?> type : List.of(RectPhyloLayout.class, CircularPhylo.class,
                RectHalfCircleLayout.class, SlopeCladoAlignedLeft.class))
            type.getConstructor(TreeLayoutProperties.class, GraphicsNode.class, TreeLayoutHost.class);
        JSONObject result = new JSONObject();
        result.put("java", System.getProperty("java.version"));
        result.put("architecture", System.getProperty("os.arch"));
        result.put("mtv", BaseLayout.class.getProtectionDomain().getCodeSource().getLocation().toString());
        for(String name:List.of("com.formdev.flatlaf.FlatLightLaf","org.jdesktop.swingx.JXStatusBar")) {
            Class<?> dependency=Class.forName(name,false,YyTreeBridge.class.getClassLoader());
            dependency.getConstructor();
            result.put(name,dependency.getProtectionDomain().getCodeSource().getLocation().toString());
        }
        result.put("desktopLaunched", UnifiedAccessPoint.isGULaunched());
        return result.toJSONString();
    }

    public String showPlot(String json, boolean newWindow) throws Exception {
        onEdt(YyTreePreview::initializeTheme);
        JSONObject scene = JSON.parseObject(json,Feature.OrderedField);
        YyTreePanel panel = preparePanel(scene);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<YyTreePreview> content = new AtomicReference<>();
        AtomicReference<JFrame> window = new AtomicReference<>();
        onEdt(() -> {
            if (newWindow || current == null || !current.isDisplayable()) {
                current = new JFrame("yytree — Modern Tree View");
                current.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                current.setLocationByPlatform(true);
                windows.add(current);
                JFrame frame = current;
                frame.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosed(WindowEvent e) { if(frame.getContentPane() instanceof YyTreePreview preview)preview.disposeInteraction();windows.remove(frame); if(current==frame)current=null; }
                    @Override public void windowActivated(WindowEvent e) { current=frame; }
                });
            }
            if(current.getContentPane() instanceof YyTreePreview previous)previous.disposeInteraction();
            YyTreePreview preview=new YyTreePreview(panel);
            current.setContentPane(preview);
            GraphicsConfiguration configuration=current.getGraphicsConfiguration();
            Rectangle screen=configuration.getBounds();Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(configuration);
            current.pack();
            current.setSize(Math.min(current.getWidth(), screen.width-insets.left-insets.right),
                Math.min(current.getHeight(), screen.height-insets.top-insets.bottom));
            current.validate();
            preview.fitToViewport();
            preview.cache.start();
            content.set(preview);
            window.set(current);
            result.set(Integer.toHexString(System.identityHashCode(current)));
        });
        awaitRendering(content.get());
        onEdt(() -> window.get().setVisible(true));
        // Native window realization can adjust the viewport, particularly at 200% DPI.
        awaitRendering(content.get());
        onEdt(() -> { content.get().captureInitialScale(); panel.paintImmediately(0, 0, panel.getWidth(), panel.getHeight()); });
        return result.get();
    }

    public String showParameterError(String message) throws Exception {
        if (GraphicsEnvironment.isHeadless()) return null;
        onEdt(() -> {
            YyTreePreview.initializeTheme();
            if (parameterError != null) parameterError.dispose();
            Window owner=current;
            if(owner==null || !owner.isDisplayable()) {
                // An explicit owner prevents the dialog builder from reusing a disposed focused dialog.
                if(parameterErrorOwner==null)parameterErrorOwner=new JFrame();
                owner=parameterErrorOwner;
            }
            parameterError = SwingDialog.builder().owner(owner).type(ModernDialogType.ERROR)
                .title("yytree — Check plotting parameters").message(message).okButton().build();
            parameterError.setModalityType(Dialog.ModalityType.MODELESS);
            parameterError.setLocationRelativeTo(current);
            parameterError.setVisible(true);
        });
        return null;
    }

    public void closeAll() throws Exception { onEdt(() -> {
        if (parameterError != null) { parameterError.dispose(); parameterError=null; }
        if (parameterErrorOwner != null) { parameterErrorOwner.dispose(); parameterErrorOwner=null; }
        for (JFrame frame : List.copyOf(windows)) frame.dispose(); current=null;
    }); }

    static void awaitRendering(YyTreePreview preview) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return;
        AtomicReference<java.util.concurrent.CompletableFuture<Void>> ready = new AtomicReference<>();
        AtomicReference<Boolean> current = new AtomicReference<>(false);
        do {
            onEdt(() -> { preview.cache.request(); ready.set(preview.cache.ready); });
            ready.get().get();
            onEdt(() -> current.set(preview.cache.isCurrent()));
        } while (!current.get());
    }

    public String testInteraction(String json) throws Exception {
        return testInteraction(json,null);
    }

    public String testInteraction(String json,String imagePath) throws Exception {
        String id=showPlot(json,true);
        AtomicReference<String> result=new AtomicReference<>();
        AtomicReference<BufferedImage> snapshot=new AtomicReference<>();
        AtomicReference<YyTreePreview> preview=new AtomicReference<>();
        onEdt(()->{
            YyTreePreview content=(YyTreePreview)current.getContentPane();
            preview.set(content);
            JScrollPane scroll=content.scroll;
            JLabel status=content.coordinates;
            YyTreePanel panel=(YyTreePanel)scroll.getViewport().getView();
            double before=panel.root.getXSelf();
            double oldZoom=panel.zoom;
            Dimension extent=scroll.getViewport().getExtentSize();
            if(panel.getPreferredSize().width>extent.width || panel.getPreferredSize().height>extent.height
                || scroll.getHorizontalScrollBar().isVisible() || scroll.getVerticalScrollBar().isVisible())
                throw new IllegalStateException("Initial preview does not fit the viewport.");
            double targetZoom=Math.max((extent.width+200.0)/panel.logicalWidth,(extent.height+200.0)/panel.logicalHeight);
            int rotations=Math.max(2,(int)Math.ceil(Math.log(targetZoom/oldZoom)/Math.log(1.12)));
            MouseWheelEvent wheel=new MouseWheelEvent(panel,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,300,200,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,-rotations);
            for(MouseWheelListener listener:panel.getMouseWheelListeners())listener.mouseWheelMoved(wheel);
            MouseEvent move=new MouseEvent(panel,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,300,200,0,false);
            for(MouseMotionListener listener:panel.getMouseMotionListeners())listener.mouseMoved(move);
            if(panel.zoom<=oldZoom || before!=panel.root.getXSelf())throw new IllegalStateException("Zoom recalculated layout or failed.");
            String expected=String.format(Locale.ROOT,"%.1f",300/panel.zoom);
            if(!status.getText().contains(expected))throw new IllegalStateException("Hover coordinates disagree with inverse zoom.");
            JViewport viewport=scroll.getViewport();
            viewport.setViewPosition(new Point(60,50));
            MouseEvent press=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),InputEvent.BUTTON1_DOWN_MASK,260,250,1,false,MouseEvent.BUTTON1);
            content.navigation.mousePressed(press);
            MouseEvent drag=new MouseEvent(panel,MouseEvent.MOUSE_DRAGGED,System.currentTimeMillis(),InputEvent.BUTTON1_DOWN_MASK,225,225,0,false,MouseEvent.NOBUTTON);
            content.navigation.mouseDragged(drag);
            if(!viewport.getViewPosition().equals(new Point(95,75)))throw new IllegalStateException("Left drag failed to pan the preview.");
            MouseEvent release=new MouseEvent(content.hint,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,0,0,1,false,MouseEvent.BUTTON1);
            content.navigation.eventDispatched(release);
            if(content.navigation.isDragging() || panel.getCursor().getType()==Cursor.HAND_CURSOR)throw new IllegalStateException("Outside release left a stale drag.");
            expected=String.format(Locale.ROOT,"%.1f",260/panel.zoom);
            if(!status.getText().contains(expected) || before!=panel.root.getXSelf())throw new IllegalStateException("Pan changed layout or hover coordinates.");
            if(!(UIManager.getLookAndFeel() instanceof com.formdev.flatlaf.FlatLightLaf))throw new IllegalStateException("FlatLaf was not initialized.");
            if(content.statusBar.getHeight()<content.coordinates.getFontMetrics(content.coordinates.getFont()).getHeight())throw new IllegalStateException("Status bar clips its font.");
            JSONObject data=new JSONObject();data.put("window",id);data.put("zoom",panel.zoom);data.put("status",status.getText());data.put("theme",UIManager.getLookAndFeel().getName());data.put("statusBar",content.statusBar.getClass().getName());data.put("dragPassed",true);data.put("desktopLaunched",UnifiedAccessPoint.isGULaunched());result.set(data.toJSONString());
        });
        awaitRendering(preview.get());
        if(imagePath!=null)onEdt(()->{
            YyTreePreview content=preview.get();
            AffineTransform device=content.getGraphicsConfiguration().getDefaultTransform();
            BufferedImage image=new BufferedImage((int)Math.ceil(content.getWidth()*device.getScaleX()),
                (int)Math.ceil(content.getHeight()*device.getScaleY()),BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();
            try{g.scale(device.getScaleX(),device.getScaleY());content.printAll(g);}finally{g.dispose();}
            snapshot.set(image);
        });
        if(imagePath!=null)javax.imageio.ImageIO.write(snapshot.get(),"png",new File(imagePath));
        return result.get();
    }

    /** Deterministic offscreen acceptance helper; also exercises precisely the GUI renderer. */
    public String renderPng(String json, String path) throws Exception {
                YyTreePanel panel = preparePanel(JSON.parseObject(json,Feature.OrderedField));
                BufferedImage image = new BufferedImage(panel.logicalWidth, panel.logicalHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.WHITE);graphics.fillRect(0,0,panel.logicalWidth,panel.logicalHeight);
                panel.drawScene(graphics);
                graphics.dispose();
                javax.imageio.ImageIO.write(image, "png", new File(path));
                JSONObject metadata = new JSONObject();
                metadata.put("leaves", panel.leaves.size());
                metadata.put("layout", panel.code);
                metadata.put("desktopLaunched", UnifiedAccessPoint.isGULaunched());
                JSONObject geometry=new JSONObject(true);
                for(GraphicsNode leaf:panel.leaves) {Point2D tip=panel.tips.get(leaf);geometry.put(leaf.getName(),new double[]{leaf.getXSelf(),leaf.getYSelf(),tip.getX(),tip.getY()});}
                metadata.put("tips",geometry);
                return metadata.toJSONString();
    }

    public String exportPlot(String json, String path) throws Exception {
        String format = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!List.of("pdf", "svg", "pptx").contains(format))
            throw new IllegalArgumentException("Unsupported export format: " + format);
        YyTreePanel panel = preparePanel(JSON.parseObject(json, Feature.OrderedField));
        AtomicReference<JComponent> drawing = new AtomicReference<>();
        onEdt(() -> drawing.set(new JComponent() {
            @Override public int getWidth() { return panel.logicalWidth; }
            @Override public int getHeight() { return panel.logicalHeight; }
            @Override public Dimension getSize() { return new Dimension(getWidth(), getHeight()); }
            @Override public void print(Graphics g) { paint(g); }
            @Override public void paint(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setColor(Color.WHITE); g.fillRect(0, 0, getWidth(), getHeight());
                    panel.drawScene(g);
                } finally { g.dispose(); }
            }
        }));
        try {
            switch (format) {
                case "pdf" -> PdfExport.save(drawing.get(), new File(path));
                case "pptx" -> egps2.utils.common.util.EGPSPrintUtilities.saveAsPptx(drawing.get(), new File(path));
                case "svg" -> egps2.frame.gui.VectorGraphicsEncoder.saveVectorGraphic(drawing.get(), path,
                    egps2.frame.gui.VectorGraphicsEncoder.VectorGraphicsFormat.SVG);
            }
        } catch (LinkageError error) {
            throw new IllegalStateException("Export dependency failed: " + error, error);
        }
        return path;
    }

    private static YyTreePanel preparePanel(JSONObject scene) throws Exception {
        AtomicReference<YyTreePanel> panel=new AtomicReference<>();
        onEdt(()->panel.set(new YyTreePanel(scene)));
        // The R caller is not the EDT. Tree conversion, geometry and PNG encoding stay here.
        panel.get().prepare();
        return panel.get();
    }

    private static void onEdt(Runnable action) throws Exception {
        ClassLoader loader = YyTreeBridge.class.getClassLoader();
        Runnable wrapped = () -> {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            try { thread.setContextClassLoader(loader); action.run(); }
            finally { thread.setContextClassLoader(previous); }
        };
        if (SwingUtilities.isEventDispatchThread()) wrapped.run();
        else try { SwingUtilities.invokeAndWait(wrapped); }
        catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) throw cause;
            throw e;
        }
    }

    static double number(JSONObject value, String key, double fallback) {
        return value.get(key) == null ? fallback : value.getDoubleValue(key);
    }
    static Color color(String value) {
        if (value == null) return null;
        long rgba = Long.parseLong(value.substring(1), 16);
        return value.length() == 9 ? new Color((int)(rgba >> 24)&255, (int)(rgba >> 16)&255,
                (int)(rgba >> 8)&255, (int)rgba&255) : new Color((int)rgba);
    }

    static final class YyTreePanel extends JPanel implements TreeLayoutHost {
        YyTreePreview preview;
        final java.util.concurrent.atomic.AtomicInteger sceneDrawCount = new java.util.concurrent.atomic.AtomicInteger();
        final JSONObject scene;
        final List<JSONObject> layers = new ArrayList<>();
        final FreeShapes freeShapes = new FreeShapes(this);
        final Map<Integer, GraphicsNode> nodes = new LinkedHashMap<>();
        final Map<String, GraphicsNode> named = new HashMap<>();
        final List<GraphicsNode> leaves = new ArrayList<>();
        final Map<GraphicsNode, Point2D.Double> tips = new IdentityHashMap<>();
        final Map<GraphicsNode, Double> positions = new IdentityHashMap<>();
        final Map<GraphicsNode, Color> labelColors = new IdentityHashMap<>();
        final Map<GraphicsNode, Color> fills = new IdentityHashMap<>();
        final Map<GraphicsNode, Double> sizes = new IdentityHashMap<>();
        final Map<String,Integer> legendWidths = new HashMap<>();
        final Map<JSONObject,Double> trackOffsets = new IdentityHashMap<>();
        record LegendRow(String text, Color color, boolean heading, boolean separator, JSONObject gradient) {
            LegendRow(String text,Color color,boolean heading,boolean separator) {this(text,color,heading,separator,null);}
            int units() {return gradient==null?1:6;}
        }
        record LegendColumn(List<LegendRow> rows, int x, int top, int width) {}
        final List<LegendColumn> legendColumns = new ArrayList<>();
        final Container metricsHost = new Container() {
            @Override public FontMetrics getFontMetrics(Font font) {
                BufferedImage image=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics=image.createGraphics();
                try { EGPSMainGuiUtil.setupHighQualityRendering(graphics); return graphics.getFontMetrics(font); } finally { graphics.dispose(); }
            }
        };
        final ScaleBarProperty scale = new ScaleBarProperty();
        final AnnotationsProperties annotations = new AnnotationsProperties();
        final AnnotationsProperties4LinageType lineages = new AnnotationsProperties4LinageType();
        final int logicalWidth, logicalHeight;
        GraphicsNode root;
        TreeLayoutProperties properties;
        BaseLayout engine;
        int code = 1;
        boolean labels;
        String align = "right";
        double zoom = 1, trackWidth, labelWidth, outer, centerX, centerY;
        double gapLength = 6, maxLabelWidth, trackBaseX, slantWidthRatio = 1;
        int slantRotation;
        double growthX = 1, growthY;
        double archRadius, archSide, archTotal, archTop, archBottom;
        int legendReserve, leftLegendReserve;
        Font labelFont;
        Rectangle2D treeBounds;

        YyTreePanel(JSONObject scene) {
            if (scene.getIntValue("schema_version") != 1) throw new IllegalArgumentException("Unsupported scene version.");
            this.scene = scene;
            logicalWidth = scene.getJSONObject("canvas").getIntValue("width");
            logicalHeight = scene.getJSONObject("canvas").getIntValue("height");
            if (logicalWidth < 1 || logicalHeight < 1) throw new IllegalArgumentException("Invalid canvas size.");
            for (Object value : scene.getJSONArray("layers")) layers.add((JSONObject)value);
            setBackground(Color.WHITE);
            metricsHost.setBackground(Color.WHITE);
            setPreferredSize(new Dimension(logicalWidth, logicalHeight));
        }

        void prepare() throws Exception {
            for (JSONObject layer : layers) freeShapes.prepareHeatmap(layer);
            JSONObject tree = scene.getJSONObject("tree");
            JSONArray names = tree.getJSONArray("labels"), edges = tree.getJSONArray("edges");
            for (Object value : edges) {
                JSONArray edge = (JSONArray)value;
                int parent = edge.getIntValue(0), child = edge.getIntValue(1);
                GraphicsNode p = nodes.computeIfAbsent(parent, id -> new GraphicsNode(""));
                GraphicsNode c = nodes.computeIfAbsent(child, id -> new GraphicsNode(""));
                c.setRealBranchLength(edge.getDoubleValue(2));
                p.addChild(c);
            }
            root = nodes.get(tree.getIntValue("root"));
            if (root == null) throw new IllegalArgumentException("Missing tree root.");
            root.setRealBranchLength(0);
            for (int i = 0; i < names.size(); i++) {
                GraphicsNode leaf = nodes.get(i+1);
                if (leaf == null) throw new IllegalArgumentException("Missing leaf " + (i+1));
                leaf.setName(names.getString(i)); named.put(leaf.getName(), leaf);
            }
            walk(root, node -> { if (node.getChildCount() == 0) leaves.add(node); });
            onEdt(()->properties = new TreeLayoutProperties(root));
            properties.setShowAxisBar(false); properties.setShowScaleBar(false); properties.setShowTitle(false);
            properties.getShowLeafPropertiesInfo().setShowLeafLabel(false);
            labelFont = properties.getGlobalFont();
            if (labelFont == null) labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            Font defaultLabelFont=labelFont;
            JSONObject selectedLayout = null;
            for (JSONObject layer : layers) {
                String type = layer.getString("type");
                if (type.equals("layout")) { code = layer.getIntValue("code"); selectedLayout=layer; }
                if (type.equals("leaf_labels")) {
                    labels = layer.getBooleanValue("show"); align = layer.getString("align");
                    gapLength=number(layer,"gap_length",6);
                    if (!Double.isFinite(gapLength) || gapLength < 0)
                        throw new IllegalArgumentException("leaf_labels(): gap_length must be one finite nonnegative number. Example: leaf_labels(gap_length = 6)");
                    labelFont=defaultLabelFont;
                    if(layer.get("font_family")!=null)labelFont=new Font(layer.getString("font_family"),Font.PLAIN,labelFont.getSize());
                    if(layer.get("font_size")!=null)labelFont=labelFont.deriveFont(layer.getFloatValue("font_size"));
                }
            }
            prepareTrackOffsets();
            if(selectedLayout != null) {
                properties.getCircularLayoutPropertiy().setGlobalStartDegree((int)number(selectedLayout,"start_angle",285));
                properties.getCircularLayoutPropertiy().setGlobalExtendDegree((int)number(selectedLayout,"angle_extent",360));
                slantWidthRatio=number(selectedLayout,"tree_width_ratio",1);
                properties.getSlopeLayoutProperety().setBlankMarginRatio(number(selectedLayout,"left_margin_ratio",.2));
                String direction=selectedLayout.getString("direction");
                slantRotation=direction==null?0:switch(direction) {
                    case "right"->0;case "down"->90;case "left"->180;case "up"->270;
                    default->throw new IllegalArgumentException("layout(): direction must be right, down, left or up. Example: layout(code = 7, direction = 'up')");
                };
                properties.getSlopeLayoutProperety().setSlopeLayoutRotationDeg(slantRotation);
                growthX=switch(slantRotation){case 0->1;case 180->-1;default->0;};
                growthY=switch(slantRotation){case 90->1;case 270->-1;default->0;};
            }
            for(JSONObject layer:layers) applyStyle(layer);
            prepareLegends();
            properties.setGlobalFont(labelFont);
            FontMetrics metrics = metricsHost.getFontMetrics(labelFont);
            maxLabelWidth = leaves.stream().mapToInt(n -> metrics.stringWidth(n.getName())).max().orElse(0);
            double markerRadius=leaves.stream().mapToDouble(n->sizes.getOrDefault(n,0.0)).max().orElse(0);
            boolean curved = code >= 3 && code <= 6;
            // Hidden labels still leave marker bounds between the tips and tracks.
            labelWidth = labels ? Math.hypot(maxLabelWidth,metrics.getHeight())+markerRadius+gapLength+4
                : markerRadius*(curved?Math.sqrt(2):1);
            JSONArray m = scene.getJSONObject("canvas").getJSONArray("margins");
            double requestedReserve=Math.ceil(labelWidth+trackWidth+12);
            double[] margins={m.getIntValue(0),m.getIntValue(1)+leftLegendReserve,m.getIntValue(2),m.getIntValue(3)+legendReserve};
            double titleReserve=35,titleAlongReserve=0;
            if(!curved) {
                for(JSONObject layer:layers) {
                    String type=layer.getString("type");
                    if(!type.equals("track")&&!type.equals("heatmap"))continue;
                    double rotation=trackRotation(layer),width=number(layer,"width",16);
                    Collection<String> titles=type.equals("track")?layer.getJSONObject("columns").keySet():layer.getJSONArray("titles").toJavaList(String.class);
                    int index=0;
                    for(String title:titles) {
                        Rectangle2D bounds=trackTitleBounds(title,rotation);
                        double shift=Math.max(0,-bounds.getMinY()-5);
                        titleReserve=Math.max(titleReserve,Math.ceil(10+shift+bounds.getMaxY()));
                        if(code==7 && rotation==60) {
                            FontMetrics fm=metricsHost.getFontMetrics(labelFont.deriveFont(10f));
                            titleReserve=Math.max(titleReserve,Math.ceil(fm.stringWidth(title)*Math.sin(Math.PI/3)+fm.getHeight()+10));
                        }
                        titleAlongReserve=Math.max(titleAlongReserve,trackOffsets.get(layer)+(index+.5)*width+bounds.getMaxX()-trackWidth);
                        index++;
                    }
                }
                requestedReserve+=Math.ceil(Math.max(0,titleAlongReserve));
            }
            if(code==3||code==4)for(int i=0;i<4;i++)margins[i]+=requestedReserve;
            else if(code==5||code==6) {
                // Arch tracks expand above and beside the tree, not below its open ends.
                margins[0]+=requestedReserve;margins[1]+=requestedReserve;margins[3]+=requestedReserve;
                margins[2]+=Math.ceil(Math.max(markerRadius,labels?metrics.getHeight()/2.0:0));
            }
            else if(code==7) {
                margins[switch(slantRotation){case 90->2;case 180->1;case 270->0;default->3;}]+=requestedReserve;
                margins[switch(slantRotation){case 90->1;case 180->0;case 270->3;default->2;}]+=titleReserve;
            } else { margins[2]+=titleReserve;margins[3]+=requestedReserve; }
            if (logicalWidth-margins[1]-margins[3] < 30 || logicalHeight-margins[0]-margins[2] < 30)
                throw new IllegalArgumentException("Cannot fit labels and tracks on this canvas.\nReceived: width="+logicalWidth+
                    ", height="+logicalHeight+", gap_length="+gapLength+".\nExpected: enough space for the tree, labels and tracks; increase width/height or reduce gap_length/font_size.\nExample: yytree(tree, width = 1600, height = 1200) + leaf_labels(gap_length = 6, font_size = 12)");
            int top=(int)margins[0],left=(int)margins[1],bottom=(int)margins[2],right=(int)margins[3];
            if(code==5 || code==6) {
                // MTV derives the arch radius from height alone (0.3 * height).
                // Fit that layout into both dimensions before calculating any coordinates.
                int height=logicalHeight-top-bottom;
                int fittedHeight=Math.min(height,(int)Math.floor((logicalWidth-left-right)/.6));
                int spare=height-fittedHeight;
                top+=spare/2;bottom+=spare-spare/2;
            }
            properties.setBlankArea(new BlankArea(top,left,bottom,right));
            engine = switch(code) {
                case 1,2 -> new RectPhyloLayout(properties,root,this);
                case 3,4 -> new CircularPhylo(properties,root,this);
                case 5,6 -> new RectHalfCircleLayout(properties,root,this);
                case 7 -> new SlopeCladoAlignedLeft(properties,root,this);
                default -> throw new IllegalArgumentException("Invalid layout code.");
            };
            engine.calculateForPainting(logicalWidth,logicalHeight);
            treeBounds = new Rectangle2D.Double(left,top,logicalWidth-left-right,logicalHeight-top-bottom);
            if(code==7) {
                walk(root,n->{
                    double shift=(slantWidthRatio-1)*along(n.getXSelf()-treeBounds.getCenterX(),n.getYSelf()-treeBounds.getCenterY());
                    n.setXSelf(n.getXSelf()+growthX*shift);n.setYSelf(n.getYSelf()+growthY*shift);
                    shift=(slantWidthRatio-1)*along(n.getXParent()-treeBounds.getCenterX(),n.getYParent()-treeBounds.getCenterY());
                    n.setXParent(n.getXParent()+growthX*shift);n.setYParent(n.getYParent()+growthY*shift);
                });
            }
            centerX=treeBounds.getCenterX(); centerY=treeBounds.getCenterY();
            if(code==3 || code==4) { centerX=root.getXParent(); centerY=root.getYParent(); }
            outer=leaves.stream().mapToDouble(n -> Point2D.distance(centerX,centerY,n.getXSelf(),n.getYSelf())).max().orElse(1);
            if(code==5 || code==6) {
                archRadius=(int)(.3*treeBounds.getHeight()); archSide=treeBounds.getHeight()-archRadius;
                archTotal=2*archSide+Math.PI*archRadius; archTop=top+archRadius; archBottom=logicalHeight-bottom;
                outer=leaves.stream().mapToDouble(GraphicsNode::getRadicusIfNeeded).max().orElse(archRadius);
            }
            for(int i=0;i<leaves.size();i++) {
                GraphicsNode n=leaves.get(i);
                positions.put(n, (double)i);
                Point2D.Double tip=new Point2D.Double(n.getXSelf(),n.getYSelf());
                if(code==2) tip.x=treeBounds.getMaxX();
                if(code==4) tip=radial(n,outer);
                if(code==6) tip=arch(outer,n.getDoubleVariable());
                tips.put(n,tip);
            }
            trackBaseX=leaves.stream().mapToDouble(n->(code==7?along(tips.get(n).x,tips.get(n).y):tips.get(n).x)+sizes.getOrDefault(n,0.0)).max().orElse(treeBounds.getMaxX());
            // Keep tracks on a common contour, outside the actual label boxes.
            labelWidth=0;
            for(GraphicsNode leaf:leaves) {
                Rectangle2D bounds=labels?labelBounds(leaf,metrics):new Rectangle2D.Double(
                    tips.get(leaf).x-markerRadius,tips.get(leaf).y-markerRadius,2*markerRadius,2*markerRadius);
                if(code==3||code==4) {
                    for(double x:new double[]{bounds.getMinX(),bounds.getMaxX()})
                        for(double y:new double[]{bounds.getMinY(),bounds.getMaxY()})
                            labelWidth=Math.max(labelWidth,Point2D.distance(centerX,centerY,x,y)-outer);
                } else if(code==5||code==6) {
                    double contourDistance=bounds.getMinY()<archTop?
                        Math.hypot(Math.max(Math.abs(bounds.getMinX()-centerX),Math.abs(bounds.getMaxX()-centerX)),archTop-bounds.getMinY()):
                        Math.max(Math.abs(bounds.getMinX()-centerX),Math.abs(bounds.getMaxX()-centerX));
                    labelWidth=Math.max(labelWidth,contourDistance-outer);
                } else labelWidth=Math.max(labelWidth,(code==7?slantLocalBounds(bounds).getMaxX():bounds.getMaxX())-trackBaseX);
            }
            walk(root,n -> { if(n.getChildCount()>0) positions.put(n,meanChildren(n)); });
        }
        double meanChildren(GraphicsNode node) {
            double sum=0;
            for(int i=0;i<node.getChildCount();i++) {
                GraphicsNode child=(GraphicsNode)node.getChildAt(i);
                sum+=positions.containsKey(child)?positions.get(child):meanChildren(child);
            }
            double result=sum/node.getChildCount(); positions.put(node,result); return result;
        }
        static void walk(GraphicsNode node, java.util.function.Consumer<GraphicsNode> action) {
            ArrayDeque<GraphicsNode> stack=new ArrayDeque<>(); stack.push(node);
            while(!stack.isEmpty()) { GraphicsNode next=stack.pop(); action.accept(next);
                for(int i=next.getChildCount()-1;i>=0;i--) stack.push((GraphicsNode)next.getChildAt(i)); }
        }
        GraphicsNode mrca(String a,String b) {
            GraphicsNode first=named.get(a), second=named.get(b);
            if(first==null || second==null || first==second) throw new IllegalArgumentException("MRCA requires two distinct known leaves.");
            Set<GraphicsNode> ancestors=Collections.newSetFromMap(new IdentityHashMap<>());
            for(GraphicsNode n=first;n!=null;n=(GraphicsNode)n.getParent()) ancestors.add(n);
            for(GraphicsNode n=second;n!=null;n=(GraphicsNode)n.getParent()) if(ancestors.contains(n)) return n;
            throw new IllegalArgumentException("Disconnected tree.");
        }
        void applyStyle(JSONObject layer) {
            String type=layer.getString("type");
            if(!Set.of("leaf_render","inner_node_render","leaf_label_render").contains(type)) return;
            JSONArray rows=layer.getJSONArray("df");
            if(rows!=null) for(Object row:rows) {
                JSONObject style=new JSONObject(new LinkedHashMap<>(layer));style.putAll((JSONObject)row);
                GraphicsNode node=type.equals("inner_node_render")?mrca(style.getString("name1"),style.getString("name2")):named.get(style.getString("name"));
                if(node==null) throw new IllegalArgumentException("Unknown leaf.");
                styleNode(node,style,type);
            } else if(type.equals("inner_node_render") && layer.containsKey("name1")) {
                styleNode(mrca(layer.getString("name1"),layer.getString("name2")),layer,type);
            } else for(GraphicsNode node:nodes.values())
                if(type.equals("leaf_render")== (node.getChildCount()==0)) styleNode(node,layer,type);
        }
        void styleNode(GraphicsNode node,JSONObject style,String type) {
            if(type.equals("leaf_label_render")) labelColors.put(node,color(style.getString("color")));
            else {
                if(style.containsKey("fill")) {Color c=color(style.getString("fill"));fills.put(node,c==null?new Color(0,0,0,0):c);}
                if(style.get("size")!=null) sizes.put(node,style.getDoubleValue("size"));
            }
        }
        Point2D.Double radial(GraphicsNode node,double radius) {
            double angle=-Math.toRadians(node.getAngleIfNeeded());
            return new Point2D.Double(centerX+radius*Math.cos(angle),centerY+radius*Math.sin(angle));
        }
        Point2D.Double arch(double radius,double p) {
            if(p<archSide) return new Point2D.Double(centerX-radius,archBottom-p);
            if(p<archSide+Math.PI*archRadius) { double a=(p-archSide)/archRadius; return new Point2D.Double(centerX-radius*Math.cos(a),archTop-radius*Math.sin(a)); }
            return new Point2D.Double(centerX+radius,archBottom-(archTotal-p));
        }
        Point2D.Double anchor(GraphicsNode n,double extra) {
            if(code==3||code==4) return radial(n,outer+extra);
            if(code==5||code==6) return arch(outer+extra,n.getDoubleVariable());
            if(code==7)return slantPoint(trackBaseX+extra,across(n.getXSelf(),n.getYSelf()));
            return new Point2D.Double(trackBaseX+extra,n.getYSelf());
        }
        double along(double x,double y) { return growthX*x+growthY*y; }
        double across(double x,double y) { return -growthY*x+growthX*y; }
        AffineTransform slantFrame() { return new AffineTransform(growthX,growthY,-growthY,growthX,0,0); }
        Point2D.Double slantPoint(double along,double across) {
            return new Point2D.Double(growthX*along-growthY*across,growthY*along+growthX*across);
        }
        Rectangle2D slantTreeBounds() {
            return slantLocalBounds(treeBounds);
        }
        Rectangle2D slantLocalBounds(Shape shape) {
            // Orthogonal directions make the transpose the inverse, with no rounding.
            return new AffineTransform(growthX,-growthY,growthY,growthX,0,0).createTransformedShape(shape).getBounds2D();
        }
        Path2D contour(double radius,double start,double end) {
            Path2D p=new Path2D.Double(); int steps=Math.max(2,(int)Math.ceil(Math.abs(end-start)/3));
            for(int i=0;i<=steps;i++) { Point2D pt=arch(radius,start+(end-start)*i/steps); if(i==0)p.moveTo(pt.getX(),pt.getY());else p.lineTo(pt.getX(),pt.getY()); }
            return p;
        }
        Shape band(GraphicsNode first,GraphicsNode last,double inner,double width) {
            if(code==3||code==4) {
                double a=-Math.toRadians(first.getAngleIfNeeded());
                double b=-Math.toRadians(last.getAngleIfNeeded());
                double extent=properties.getCircularLayoutPropertiy().getGlobalExtendDegree();
                double step=Math.toRadians(extent/Math.max(1,leaves.size()-(extent<360?1:0)));
                boolean open=extent<360 && leaves.size()>1;
                double start=a+(open && first==leaves.getFirst()?0:step/2);
                double end=b-(open && last==leaves.getLast()?0:step/2);
                Path2D p=new Path2D.Double();
                int count=Math.max(2,(int)Math.ceil(Math.abs(end-start)*100));
                for(int i=0;i<=count;i++) {double angle=start+(end-start)*i/count;double x=centerX+(inner+width)*Math.cos(angle),y=centerY+(inner+width)*Math.sin(angle);if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}
                for(int i=count;i>=0;i--) {double angle=start+(end-start)*i/count;p.lineTo(centerX+inner*Math.cos(angle),centerY+inner*Math.sin(angle));}p.closePath();return p;
            }
            if(code==5||code==6) {
                double step=archTotal/Math.max(1,leaves.size()-1);
                double start=Math.max(0,first.getDoubleVariable()-step/2),end=Math.min(archTotal,last.getDoubleVariable()+step/2);
                Path2D p=contour(inner+width,start,end);p.append(contour(inner,end,start),true);p.closePath();return p;
            }
            int a=positions.get(first).intValue(),b=positions.get(last).intValue();
            if(code==7) {
                Rectangle2D area=slantTreeBounds();
                double top=a==0?area.getMinY():(across(leaves.get(a-1).getXSelf(),leaves.get(a-1).getYSelf())+across(first.getXSelf(),first.getYSelf()))/2;
                double bottom=b==leaves.size()-1?area.getMaxY():(across(leaves.get(b+1).getXSelf(),leaves.get(b+1).getYSelf())+across(last.getXSelf(),last.getYSelf()))/2;
                return slantFrame().createTransformedShape(new Rectangle2D.Double(trackBaseX+inner,top,width,bottom-top));
            }
            double top=a==0?treeBounds.getMinY():(leaves.get(a-1).getYSelf()+first.getYSelf())/2;
            double bottom=b==leaves.size()-1?treeBounds.getMaxY():(leaves.get(b+1).getYSelf()+last.getYSelf())/2;
            return new Rectangle2D.Double(trackBaseX+inner,top,width,bottom-top);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
            try {
                if (preview == null) { g.scale(zoom,zoom);drawScene(g); }
                else {
                    preview.cache.paint(g);
                    Graphics2D content=(Graphics2D)g.create();
                    try {content.scale(zoom,zoom);preview.editor.paintContents(content);}finally{content.dispose();}
                    preview.paintHover(g);
                    preview.editor.paintOverlay(g);
                }
            } finally {g.dispose();}
        }
        void drawScene(Graphics2D g) {
                drawBaseScene(g);
                for(JSONObject layer:layers) if(layer.containsKey("mode")) drawFree(g,layer);
        }
        void drawBaseScene(Graphics2D g) {
                sceneDrawCount.incrementAndGet();
                EGPSMainGuiUtil.setupHighQualityRendering(g);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(labelFont);
                for(JSONObject layer:layers) if(layer.getString("type").equals("inner2leaf_highlight")) highlight(g,layer);
                drawTree(g);
                if(labels) drawLabels(g);
                for(JSONObject layer:layers) if(layer.getString("type").equals("inner_node_annotation")) nodeAnnotation(g,layer);
                for(JSONObject layer:layers) if(layer.getString("type").equals("inner2leaf_highlight")) highlightText(g,layer);
                drawTracks(g);
        }
        void drawTree(Graphics2D g) {
            for(GraphicsNode node:nodes.values()) {
                g.setColor(fills.getOrDefault(node,Color.BLACK));g.setStroke(new BasicStroke(1));
                GraphicsNode parent=(GraphicsNode)node.getParent();
                if(parent!=null) {
                    if(code==7) g.draw(new Line2D.Double(parent.getXSelf(),parent.getYSelf(),node.getXSelf(),node.getYSelf()));
                    else {
                        g.draw(new Line2D.Double(node.getXParent(),node.getYParent(),node.getXSelf(),node.getYSelf()));
                        if(code==1||code==2) g.draw(new Line2D.Double(parent.getXSelf(),parent.getYSelf(),node.getXParent(),node.getYParent()));
                    }
                }
                if(node.getChildCount()>0 && (code==3||code==4)) {
                    GraphicsNode first=(GraphicsNode)node.getChildAt(0),last=(GraphicsNode)node.getChildAt(node.getChildCount()-1);
                    double radius=Point2D.distance(centerX,centerY,node.getXSelf(),node.getYSelf());
                    double a=first.getAngleIfNeeded(),b=last.getAngleIfNeeded();
                    Arc2D.Double arc=new Arc2D.Double();
                    arc.setArcByCenter(centerX,centerY,radius,a,b-a,Arc2D.OPEN);g.draw(arc);
                }
                if(node.getChildCount()>0 && (code==5||code==6)) {
                    GraphicsNode first=(GraphicsNode)node.getChildAt(0),last=(GraphicsNode)node.getChildAt(node.getChildCount()-1);
                    g.draw(contour(node.getRadicusIfNeeded(),first.getDoubleVariable(),last.getDoubleVariable()));
                }
                if(node.getChildCount()==0 && (code==2||code==4||code==6)) {
                    g.setStroke(new BasicStroke(1,0,0,10,new float[]{2,3},0));
                    Point2D tip=tips.get(node);g.draw(new Line2D.Double(node.getXSelf(),node.getYSelf(),tip.getX(),tip.getY()));
                }
                double size=sizes.getOrDefault(node,0.0); if(size>0) {
                    Point2D p=node.getChildCount()==0?tips.get(node):new Point2D.Double(node.getXSelf(),node.getYSelf());
                    g.fill(new Ellipse2D.Double(p.getX()-size,p.getY()-size,2*size,2*size));
                }
            }
        }
        void drawLabels(Graphics2D g) {
            FontMetrics fm=g.getFontMetrics();
            for(GraphicsNode leaf:leaves) {
                if(code==3||code==4||code==7) {
                    TipLabelGeometry placement=code==7?slantLabelGeometry(leaf,fm):circularLabelGeometry(leaf,fm);
                    Point2D tip=tips.get(leaf);
                    Graphics2D label=(Graphics2D)g.create();
                    try {
                        label.translate(tip.getX(),tip.getY());label.rotate(placement.rotation());
                        Color background=labelColors.get(leaf);
                        if(background!=null){label.setColor(background);label.fill(placement.box());}
                        label.setColor(Color.BLACK);label.drawString(leaf.getName(),(float)placement.x(),(float)placement.baseline());
                    } finally {label.dispose();}
                    continue;
                }
                Rectangle2D bounds=labelBounds(leaf,fm);double x=bounds.getX();
                Color background=labelColors.get(leaf);
                if(background!=null){g.setColor(background);g.fill(bounds);}
                g.setColor(Color.BLACK);g.drawString(leaf.getName(),(float)x,(float)(bounds.getY()+fm.getAscent()));
            }
        }
        TipLabelGeometry circularLabelGeometry(GraphicsNode leaf,FontMetrics fm) {
            double angle=((-leaf.getAngleIfNeeded()%360)+540)%360-180;
            boolean flip=angle>90||angle< -90;
            if(flip)angle+=angle>0?-180:180;
            double width=fm.stringWidth(leaf.getName()),distance=sizes.getOrDefault(leaf,0.0)+gapLength;
            double x=flip?-distance-width:distance,baseline=(fm.getAscent()-fm.getDescent())/2.0;
            return new TipLabelGeometry(x,baseline,Math.toRadians(angle),new Rectangle2D.Double(x,baseline-fm.getAscent(),width,fm.getHeight()));
        }
        TipLabelGeometry slantLabelGeometry(GraphicsNode leaf,FontMetrics fm) {
            double width=fm.stringWidth(leaf.getName()),distance=sizes.getOrDefault(leaf,0.0)+gapLength;
            double offset=switch(align){case "right"->maxLabelWidth-width;case "center"->(maxLabelWidth-width)/2;default->0;};
            double x=slantRotation==180?-distance-maxLabelWidth+offset:distance+offset;
            double rotation=slantRotation==180?0:Math.toRadians(slantRotation==270?-90:slantRotation);
            double baseline=fm.getAscent()-fm.getHeight()/2.0;
            return new TipLabelGeometry(x,baseline,rotation,new Rectangle2D.Double(x,-fm.getHeight()/2.0,width,fm.getHeight()));
        }
        Rectangle2D labelAreaBounds(GraphicsNode leaf,FontMetrics fm) {
            Point2D tip=tips.get(leaf);
            if(code==7)return slantFrame().createTransformedShape(new Rectangle2D.Double(
                along(tip.getX(),tip.getY())+sizes.getOrDefault(leaf,0.0)+gapLength,
                across(tip.getX(),tip.getY())-fm.getHeight()/2.0,maxLabelWidth,fm.getHeight())).getBounds2D();
            double nx=1,ny=0;
            if(code==5||code==6) {
                double p=leaf.getDoubleVariable();
                if(p<archSide)nx=-1;
                else if(p<archSide+Math.PI*archRadius) {
                    double angle=(p-archSide)/archRadius;nx=-Math.cos(angle);ny=-Math.sin(angle);
                }
            }
            double height=fm.getHeight();
            double distance=sizes.getOrDefault(leaf,0.0)+gapLength+(Math.abs(nx)*maxLabelWidth+Math.abs(ny)*height)/2;
            return new Rectangle2D.Double(tip.getX()+nx*distance-maxLabelWidth/2,
                tip.getY()+ny*distance-height/2,maxLabelWidth,height);
        }
        Rectangle2D labelBounds(GraphicsNode leaf,FontMetrics fm) {
            if(code==3||code==4||code==7) {
                Point2D tip=tips.get(leaf);
                return (code==7?slantLabelGeometry(leaf,fm):circularLabelGeometry(leaf,fm)).boundsAt(tip.getX(),tip.getY()).getBounds2D();
            }
            Rectangle2D area=labelAreaBounds(leaf,fm);int width=fm.stringWidth(leaf.getName());
            double offset=switch(align){case "right"->maxLabelWidth-width;case "center"->(maxLabelWidth-width)/2;default->0;};
            return new Rectangle2D.Double(area.getX()+offset,area.getY(),width,area.getHeight());
        }
        List<GraphicsNode> descendants(GraphicsNode node) {List<GraphicsNode> out=new ArrayList<>();walk(node,n->{if(n.getChildCount()==0)out.add(n);});return out;}
        List<JSONObject> rows(JSONObject layer) {JSONArray df=layer.getJSONArray("df");if(df==null)return List.of(layer);List<JSONObject> out=new ArrayList<>();for(Object row:df){JSONObject merged=new JSONObject(new LinkedHashMap<>(layer));merged.putAll((JSONObject)row);out.add(merged);}return out;}
        void highlight(Graphics2D g,JSONObject layer) {
            for(JSONObject row:rows(layer)) {
                GraphicsNode node=mrca(row.getString("name1"),row.getString("name2"));List<GraphicsNode> d=descendants(node);
                Color c=color(row.getString("fill"));if(c==null)continue;g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),Math.min(c.getAlpha(),40)));
                if(code==3||code==4||code==5||code==6) {
                    double r=(code==3||code==4)?Point2D.distance(centerX,centerY,node.getXSelf(),node.getYSelf()):node.getRadicusIfNeeded();
                    g.fill(band(d.getFirst(),d.getLast(),r,Math.max(0,outer-r)));
                } else {
                    double end=d.stream().mapToDouble(leaf->tips.get(leaf).x).max().orElse(node.getXSelf());
                    if(code!=7)g.fill(new Rectangle2D.Double(node.getXSelf(),d.getFirst().getYSelf(),end-node.getXSelf(),d.getLast().getYSelf()-d.getFirst().getYSelf()));
                    else {Path2D p=new Path2D.Double();p.moveTo(node.getXSelf(),node.getYSelf());p.lineTo(tips.get(d.getFirst()).x,tips.get(d.getFirst()).y);p.lineTo(tips.get(d.getLast()).x,tips.get(d.getLast()).y);p.closePath();g.fill(p);}
                }
            }
        }
        void nodeAnnotation(Graphics2D g,JSONObject layer) {
            for(JSONObject row:rows(layer)) nodeAnnotationRow(g,row);
        }
        void nodeAnnotationRow(Graphics2D g,JSONObject layer) {
            GraphicsNode node=mrca(layer.getString("name1"),layer.getString("name2"));
            Shape shape=nodeAnnotationShape(node,layer);
            Color c=color(layer.getString("fill"));if(c!=null){g.setColor(c);g.fill(shape);}
            drawAnnotationText(g,layer.getString("text"),shape.getBounds2D(),layer.getString("text_position")==null?"right":layer.getString("text_position"));
        }
        Shape nodeAnnotationShape(GraphicsNode node,JSONObject layer) {
            double size=number(layer,"size",20),x=node.getXSelf(),y=node.getYSelf();
            Shape shape=switch(layer.getString("shape")){case "ellipse"->new Ellipse2D.Double(x-size/2,y-size/2,size,size);case "triangle"->{Path2D p=new Path2D.Double();p.moveTo(x,y-size/2);p.lineTo(x-size/2,y+size/2);p.lineTo(x+size/2,y+size/2);p.closePath();yield p;}case "rect"->new Rectangle2D.Double(x-size/2,y-size/2,size,size);default->throw new IllegalArgumentException("Unknown annotation shape.");};
            if(code==7)shape=AffineTransform.getRotateInstance(Math.toRadians(slantRotation),x,y).createTransformedShape(shape);
            return shape;
        }
        Point2D.Double highlightTextAnchor(GraphicsNode node) {
            return new Point2D.Double(node.getXSelf(),node.getYSelf());
        }
        void highlightText(Graphics2D g,JSONObject layer) {
            for(JSONObject row:rows(layer)) {
                String text=row.getString("text");if(text==null||text.isEmpty())continue;
                Point2D anchor=highlightTextAnchor(mrca(row.getString("name1"),row.getString("name2")));
                drawAnnotationText(g,text,new Rectangle2D.Double(anchor.getX(),anchor.getY(),0,0),
                    row.getString("text_position")==null?"left":row.getString("text_position"));
            }
        }
        record AnnotationText(TextLayout layout,float x,float baseline,Rectangle2D bounds) {}
        AnnotationText placeAnnotationText(Graphics2D g,String text,Rectangle2D reference,String position) {
            if(text==null||text.isEmpty())return null;
            TextLayout layout=new TextLayout(text,g.getFont(),g.getFontRenderContext());
            Rectangle2D ink=layout.getBounds();
            double x=reference.getCenterX()-ink.getWidth()/2,y=reference.getCenterY()-ink.getHeight()/2;
            switch(position) {
                case "left"->x=reference.getMinX()-5-ink.getWidth();
                case "right"->x=reference.getMaxX()+5;
                case "top"->y=reference.getMinY()-5-ink.getHeight();
                case "bottom"->y=reference.getMaxY()+5;
                case "topleft"->{x=reference.getMinX()-5-ink.getWidth();y=reference.getMinY()-5-ink.getHeight();}
                case "topright"->{x=reference.getMaxX()+5;y=reference.getMinY()-5-ink.getHeight();}
                case "bottomleft"->{x=reference.getMinX()-5-ink.getWidth();y=reference.getMaxY()+5;}
                case "bottomright"->{x=reference.getMaxX()+5;y=reference.getMaxY()+5;}
                default->throw new IllegalArgumentException("text_position must be left, right, top, bottom, topleft, topright, bottomleft or bottomright.");
            }
            return new AnnotationText(layout,(float)(x-ink.getX()),(float)(y-ink.getY()),
                new Rectangle2D.Double(x,y,ink.getWidth(),ink.getHeight()));
        }
        void drawAnnotationText(Graphics2D g,String text,Rectangle2D reference,String position) {
            AnnotationText placed=placeAnnotationText(g,text,reference,position);
            if(placed!=null){g.setColor(Color.BLACK);placed.layout().draw(g,placed.x(),placed.baseline());}
        }
        void prepareTrackOffsets() {
            double end=0,gap=4;
            boolean previous=false;
            for(JSONObject layer:layers) {
                String type=layer.getString("type");
                if(type.equals("annotation_gap")) {
                    gap=number(layer,"width",12);
                    if(!Double.isFinite(gap)||gap<0)throw new IllegalArgumentException("leaf_annotation_gap(): width must be one finite nonnegative number. Example: leaf_annotation_gap(width = 12)");
                } else if(type.equals("track")||type.equals("heatmap")) {
                    if(previous)end+=gap;
                    trackOffsets.put(layer,end);
                    int count=type.equals("track")?layer.getJSONObject("columns").size():layer.getJSONArray("titles").size();
                    end+=count*number(layer,"width",16);
                    previous=true;gap=4;
                }
            }
            trackWidth=previous?end+4:0;
        }
        void drawTracks(Graphics2D g) {
            for(JSONObject layer:layers) {
                String type=layer.getString("type");if(!type.equals("track")&&!type.equals("heatmap"))continue;
                double offset=labelWidth+12+trackOffsets.get(layer);
                JSONArray names=layer.getJSONArray("names");Map<String,Integer> indices=new HashMap<>();for(int i=0;i<names.size();i++)indices.put(names.getString(i),i);
                double width=number(layer,"width",16);
                if(type.equals("track")) {
                    JSONObject columns=layer.getJSONObject("columns");
                    for(String title:columns.keySet()) {
                        JSONObject column=columns.getJSONObject(title);JSONArray levels=column.getJSONArray("levels"),colors=column.getJSONArray("colors"),values=column.getJSONArray("values");
                        for(GraphicsNode leaf:leaves) {Integer row=indices.get(leaf.getName());if(row==null)continue;String value=values.getString(row);if(value==null)continue;int index=levels.indexOf(value);if(index>=0)cell(g,leaf,offset,width,color(colors.getString(index)));}
                        trackTitle(g,title,offset+width/2,trackRotation(layer));
                        offset+=width;
                    }
                } else {
                    JSONArray titles=layer.getJSONArray("titles"),cells=layer.getJSONArray("cells");
                    for(int j=0;j<titles.size();j++) {for(GraphicsNode leaf:leaves){Integer row=indices.get(leaf.getName());if(row!=null)cell(g,leaf,offset,width,color(cells.getJSONArray(row).getString(j)));}trackTitle(g,titles.getString(j),offset+width/2,trackRotation(layer));offset+=width;}
                }
            }
            drawLegends(g);
        }
        double trackRotation(JSONObject layer) {
            double rotation=number(layer,"text_rotation",60);
            if(!Double.isFinite(rotation)||rotation< -90||rotation>90)
                throw new IllegalArgumentException("text_rotation must be finite and between -90 and 90 degrees.");
            return rotation;
        }
        Rectangle2D trackTitleBounds(String text,double rotation) {
            FontMetrics metrics=metricsHost.getFontMetrics(labelFont.deriveFont(10f));
            double angle=Math.toRadians((code==7?slantRotation:0)+rotation);
            angle=Math.atan2(Math.sin(angle),Math.cos(angle));
            boolean flip=angle>Math.PI/2+1e-10||angle< -Math.PI/2-1e-10;
            double local=Math.toRadians(rotation)+(flip?Math.PI:0);
            int width=metrics.stringWidth(text);
            Rectangle2D box=new Rectangle2D.Double(flip?-width:0,-metrics.getAscent(),width,metrics.getHeight());
            return AffineTransform.getRotateInstance(local).createTransformedShape(box).getBounds2D();
        }
        void trackTitle(Graphics2D g,String text,double offset,double rotation) {
            if(code>=3&&code<=6)return;
            Graphics2D title=(Graphics2D)g.create();
            title.setFont(labelFont.deriveFont(10f));title.setColor(Color.BLACK);
            double shift=Math.max(0,-trackTitleBounds(text,rotation).getMinY()-5);
            if(code==7) {
                Point2D point=slantPoint(trackBaseX+offset,slantTreeBounds().getMaxY()+10+shift);
                drawReadableText(title,text,point.getX(),point.getY(),Math.toRadians(slantRotation+rotation));
                title.dispose();return;
            }
            drawReadableText(title,text,trackBaseX+offset,treeBounds.getMaxY()+10+shift,Math.toRadians(rotation));
            title.dispose();
        }
        void drawReadableText(Graphics2D g,String text,double x,double y,double angle) {
            angle=Math.atan2(Math.sin(angle),Math.cos(angle));
            boolean flip=angle>Math.PI/2+1e-10||angle< -Math.PI/2-1e-10;
            Graphics2D label=(Graphics2D)g.create();
            try {
                label.translate(x,y);label.rotate(flip?angle+(angle>0?-Math.PI:Math.PI):angle);
                label.drawString(text,flip?-label.getFontMetrics().stringWidth(text):0,0);
            } finally {label.dispose();}
        }
        void cell(Graphics2D g,GraphicsNode leaf,double offset,double width,Color color) {if(color==null)return;g.setColor(color);g.fill(band(leaf,leaf,(code>=3&&code<=6?outer:0)+offset,width));}
        void prepareLegends() {
            Map<String,List<List<LegendRow>>> blocks=new LinkedHashMap<>();
            for(JSONObject layer:layers) {
                String position=layer.getString("legend");if(position==null||position.equals("none"))continue;
                List<LegendRow> rows=new ArrayList<>();
                if(layer.getString("type").equals("track")) {
                    JSONObject columns=layer.getJSONObject("columns");
                    for(String title:columns.keySet()) {
                        JSONObject titles=layer.getJSONObject("legend_titles");
                        rows.add(new LegendRow(titles!=null&&titles.containsKey(title)?titles.getString(title):title,null,true,false));
                        JSONObject column=columns.getJSONObject(title);
                        JSONArray levels=column.getJSONArray("levels"),colors=column.getJSONArray("colors");
                        for(int i=0;i<levels.size();i++)rows.add(new LegendRow(levels.getString(i),color(colors.getString(i)),false,false));
                    }
                } else if(layer.getString("type").equals("heatmap")) {
                    String customTitle=layer.getString("legend_title");
                    rows.add(new LegendRow(customTitle==null?String.join(", ",layer.getJSONArray("titles").toJavaList(String.class)):customTitle,null,true,false));
                    rows.add(new LegendRow("",null,false,false,layer));
                }
                if(!rows.isEmpty())blocks.computeIfAbsent(position,k->new ArrayList<>()).add(rows);
            }
            FontMetrics metrics=metricsHost.getFontMetrics(labelFont.deriveFont(10f));
            FontMetrics headings=metricsHost.getFontMetrics(labelFont.deriveFont(Font.BOLD,10f));
            for(var entry:blocks.entrySet()) {
                String position=entry.getKey(),opposite=position.startsWith("top")?position.replace("top","bottom"):position.replace("bottom","top");
                int height=blocks.containsKey(opposite)?logicalHeight/2:logicalHeight;
                int cap=Math.max(2,(height-30)/15);
                int width=entry.getValue().stream().flatMap(List::stream).mapToInt(row->{
                    if(row.gradient()==null)return (row.heading()?headings:metrics).stringWidth(row.text());
                    JSONArray limits=row.gradient().getJSONArray("limits");
                    int widest=0;for(int i=0;i<5;i++)widest=Math.max(widest,metrics.stringWidth(heatLegendLabel(limits,i)));
                    return widest;
                }).max().orElse(0)+28;
                legendWidths.put(position,width);
                List<List<LegendRow>> columns=new ArrayList<>();
                List<LegendRow> column=new ArrayList<>();
                for(List<LegendRow> block:entry.getValue()) {
                    int blockUnits=legendUnits(block),used=legendUnits(column);
                    if(block.stream().anyMatch(row->row.gradient()!=null) && blockUnits>cap)
                        throw new IllegalArgumentException("Cannot fit the continuous heatmap legend on this canvas; increase height.");
                    // Keep a complete annotation together whenever it fits in one column.
                    if(!column.isEmpty() && (blockUnits<=cap && used+1+blockUnits>cap ||
                        cap-used<Math.min(3,blockUnits+1))) {
                        columns.add(column);column=new ArrayList<>();
                    }
                    if(!column.isEmpty())column.add(new LegendRow("",null,false,true));
                    LegendRow heading=null;
                    for(int i=0;i<block.size();i++) {
                        LegendRow row=block.get(i);
                        int needed=row.units()+(row.heading()&&i+1<block.size()?block.get(i+1).units():0);
                        if(cap-legendUnits(column)<needed) {
                            columns.add(column);column=new ArrayList<>();
                            if(!row.heading() && heading!=null)column.add(heading);
                        }
                        column.add(row);
                        if(row.heading())heading=row;
                    }
                }
                if(!column.isEmpty())columns.add(column);
                for(int i=0;i<columns.size();i++) {
                    List<LegendRow> rows=columns.get(i);
                    int x=position.endsWith("right")?logicalWidth-(i+1)*width:8+i*width;
                    int top=position.startsWith("bottom")?logicalHeight-12-(legendUnits(rows)-1)*15:15;
                    legendColumns.add(new LegendColumn(rows,x,top,width));
                }
                int reserve=width*columns.size()+8;
                if(position.endsWith("right"))legendReserve=Math.max(legendReserve,reserve);else leftLegendReserve=Math.max(leftLegendReserve,reserve);
            }
        }
        int legendUnits(List<LegendRow> rows) {return rows.stream().mapToInt(LegendRow::units).sum();}
        String heatLegendLabel(JSONArray limits,int index) {
            double fraction=index/4.0;
            return String.format(Locale.ROOT,"%.3g",limits.getDoubleValue(0)*(1-fraction)+limits.getDoubleValue(1)*fraction);
        }
        void drawLegends(Graphics2D graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            try {
                for(LegendColumn column:legendColumns) {
                    int x=column.x(),y=column.top();
                    g.setColor(new Color(255,255,255,230));
                    g.fillRect(x-2,y-12,column.width(),legendUnits(column.rows())*15);
                    for(LegendRow row:column.rows()) {
                        if(row.separator()) {
                            g.setColor(new Color(150,150,150));
                            g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{4,3},0));
                            g.drawLine(x,y-5,x+column.width()-12,y-5);
                        } else if(row.gradient()!=null) {
                            drawHeatLegend(g,row.gradient(),x,y-9);
                        } else {
                            g.setFont(labelFont.deriveFont(row.heading()?Font.BOLD:Font.PLAIN,10f));
                            if(row.color()!=null){g.setColor(row.color());g.fillRect(x,y-9,10,10);}
                            g.setColor(Color.BLACK);g.drawString(row.text(),row.heading()?x:x+15,y);
                        }
                        y+=row.units()*15;
                    }
                }
            } finally {g.dispose();}
        }
        void drawHeatLegend(Graphics2D g,JSONObject layer,int x,int top) {
            JSONArray palette=layer.getJSONArray("palette"),limits=layer.getJSONArray("limits");
            boolean constant=limits.getDoubleValue(0)==limits.getDoubleValue(1);
            for(int row=0;row<80;row++) {
                int index=constant?127:(int)Math.round(255*(1-row/79.0));
                Color c=color(palette.getString(index));
                if(c!=null){g.setColor(c);g.fillRect(x,top+row,12,1);}
            }
            g.setStroke(new BasicStroke(1));g.setColor(new Color(150,150,150));
            g.drawRect(x-1,top-1,13,81);
            g.setFont(labelFont.deriveFont(10f));g.setColor(Color.BLACK);
            int shift=(g.getFontMetrics().getAscent()-g.getFontMetrics().getDescent())/2;
            for(int i=0;i<5;i++) {
                if(constant && i!=2)continue;
                int y=top+(int)Math.round(i*79/4.0);
                g.drawLine(x+12,y,x+15,y);
                g.drawString(heatLegendLabel(limits,4-i),x+19,y+shift);
            }
        }
        double coordinate(JSONObject layer,String key,boolean x) {return freeShapes.value(layer,key,x);}
        void drawFree(Graphics2D g,JSONObject layer) {
            freeShapes.draw(g,layer);
        }
        @Override public ScaleBarProperty getScaleBarProperty(){return scale;}
        @Override public AnnotationsProperties getMainAnnotationsProperties(){return annotations;}
        @Override public AnnotationsProperties4LinageType getLinageTypeAnnotationsProperties(){return lineages;}
        @Override public Container getHostComponent(){return metricsHost;}
    }
}
