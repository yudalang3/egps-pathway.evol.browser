package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import egps2.UnifiedAccessPoint;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Standalone, package-local-classloader entry point. No eGPS desktop launcher is used. */
public final class YyTreeBridge {
    private JFrame current;
    private final List<JFrame> windows = new ArrayList<>();

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
            preview.scroll.setPreferredSize(new Dimension(
                Math.min(Math.min(panel.logicalWidth,1200),Math.max(1,screen.width-insets.left-insets.right-60)),
                Math.min(Math.min(panel.logicalHeight,850),Math.max(1,screen.height-insets.top-insets.bottom-100))));
            current.pack();
            current.setVisible(true);
            panel.paintImmediately(0, 0, panel.getWidth(), panel.getHeight());
            result.set(Integer.toHexString(System.identityHashCode(current)));
        });
        return result.get();
    }

    public void closeAll() throws Exception { onEdt(() -> { for (JFrame frame : List.copyOf(windows)) frame.dispose(); current=null; }); }

    public String testInteraction(String json) throws Exception {
        return testInteraction(json,null);
    }

    public String testInteraction(String json,String imagePath) throws Exception {
        String id=showPlot(json,true);
        AtomicReference<String> result=new AtomicReference<>();
        AtomicReference<BufferedImage> snapshot=new AtomicReference<>();
        onEdt(()->{
            YyTreePreview content=(YyTreePreview)current.getContentPane();
            JScrollPane scroll=content.scroll;
            JLabel status=content.coordinates;
            YyTreePanel panel=(YyTreePanel)scroll.getViewport().getView();
            double before=panel.root.getXSelf();
            MouseWheelEvent wheel=new MouseWheelEvent(panel,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,300,200,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,-2);
            for(MouseWheelListener listener:panel.getMouseWheelListeners())listener.mouseWheelMoved(wheel);
            MouseEvent move=new MouseEvent(panel,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,300,200,0,false);
            for(MouseMotionListener listener:panel.getMouseMotionListeners())listener.mouseMoved(move);
            if(panel.zoom<=1 || before!=panel.root.getXSelf())throw new IllegalStateException("Zoom recalculated layout or failed.");
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
            if(imagePath!=null){BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();try{content.printAll(g);}finally{g.dispose();}snapshot.set(image);}
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
        final JSONObject scene;
        final List<JSONObject> layers = new ArrayList<>();
        final Map<Integer, GraphicsNode> nodes = new LinkedHashMap<>();
        final Map<String, GraphicsNode> named = new HashMap<>();
        final List<GraphicsNode> leaves = new ArrayList<>();
        final Map<GraphicsNode, Point2D.Double> tips = new IdentityHashMap<>();
        final Map<GraphicsNode, Double> positions = new IdentityHashMap<>();
        final Map<GraphicsNode, Color> labelColors = new IdentityHashMap<>();
        final Map<GraphicsNode, Color> fills = new IdentityHashMap<>();
        final Map<GraphicsNode, Double> sizes = new IdentityHashMap<>();
        final Map<String,Integer> legendWidths = new HashMap<>();
        final Map<String,Integer> legendCaps = new HashMap<>();
        final Container metricsHost = new Container() {
            @Override public FontMetrics getFontMetrics(Font font) {
                BufferedImage image=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics=image.createGraphics();
                try { return graphics.getFontMetrics(font); } finally { graphics.dispose(); }
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
            for (JSONObject layer : layers) {
                String type = layer.getString("type");
                if (type.equals("layout")) code = layer.getIntValue("code");
                if (type.equals("leaf_labels")) {
                    labels = layer.getBooleanValue("show"); align = layer.getString("align");
                    if(layer.get("font_family")!=null)labelFont=new Font(layer.getString("font_family"),Font.PLAIN,labelFont.getSize());
                    if(layer.get("font_size")!=null)labelFont=labelFont.deriveFont(layer.getFloatValue("font_size"));
                }
                if (type.equals("track")) trackWidth += layer.getJSONObject("columns").size() * number(layer,"width",16) + 4;
                if (type.equals("heatmap")) trackWidth += layer.getJSONArray("titles").size() * number(layer,"width",16) + 4;
            }
            prepareLegends();
            properties.setGlobalFont(labelFont);
            FontMetrics metrics = metricsHost.getFontMetrics(labelFont);
            labelWidth = labels ? leaves.stream().mapToInt(n -> metrics.stringWidth(n.getName())).max().orElse(0)+12 : 0;
            JSONArray m = scene.getJSONObject("canvas").getJSONArray("margins");
            int reserve = (int)Math.ceil(labelWidth + trackWidth + 12);
            boolean curved = code >= 3 && code <= 6;
            int top = m.getIntValue(0)+(curved?reserve:0), left=m.getIntValue(1)+(curved?reserve:0)+leftLegendReserve;
            int bottom=m.getIntValue(2)+(curved?reserve:35), right=m.getIntValue(3)+reserve+legendReserve;
            if (logicalWidth-left-right < 30 || logicalHeight-top-bottom < 30)
                throw new IllegalArgumentException("Canvas too small for labels and tracks; increase width/height.");
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
            walk(root,n -> { if(n.getChildCount()>0) positions.put(n,meanChildren(n)); });
            for(JSONObject layer:layers) applyStyle(layer);
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
            double angle=Math.atan2(node.getYSelf()-centerY,node.getXSelf()-centerX);
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
            return new Point2D.Double(treeBounds.getMaxX()+extra,n.getYSelf());
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
                double step=2*Math.PI/Math.max(1,leaves.size());
                double start=a+step/2,end=b-step/2;
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
            double top=a==0?treeBounds.getMinY():(leaves.get(a-1).getYSelf()+first.getYSelf())/2;
            double bottom=b==leaves.size()-1?treeBounds.getMaxY():(leaves.get(b+1).getYSelf()+last.getYSelf())/2;
            return new Rectangle2D.Double(treeBounds.getMaxX()+inner,top,width,bottom-top);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();
            try {
                g.scale(zoom,zoom);drawScene(g);
            } finally {g.dispose();}
        }
        void drawScene(Graphics2D g) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(labelFont);
                for(JSONObject layer:layers) if(layer.getString("type").equals("inner2leaf_highlight")) highlight(g,layer);
                drawTree(g);
                if(labels) drawLabels(g);
                for(JSONObject layer:layers) if(layer.getString("type").equals("inner_node_annotation")) nodeAnnotation(g,layer);
                drawTracks(g);
                for(JSONObject layer:layers) if(layer.containsKey("mode")) drawFree(g,layer);
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
                    double a=-Math.toRadians(first.getAngleIfNeeded()),b=-Math.toRadians(last.getAngleIfNeeded());
                    Path2D path=new Path2D.Double();for(int i=0;i<=100;i++){double t=a+(b-a)*i/100;double x=centerX+radius*Math.cos(t),y=centerY+radius*Math.sin(t);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}g.draw(path);
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
                Point2D p=anchor(leaf,6);double x=p.getX(),y=p.getY();int width=fm.stringWidth(leaf.getName());
                if(code==1||code==2||code==7) x+=switch(align){case "right"->labelWidth-width-10;case "center"->(labelWidth-width-10)/2;default->0;};
                else if(x<centerX) x-=width;
                Color background=labelColors.get(leaf);
                if(background!=null){g.setColor(background);g.fill(new Rectangle2D.Double(x,y-fm.getAscent()/2.0,width,fm.getHeight()));}
                g.setColor(Color.BLACK);g.drawString(leaf.getName(),(float)x,(float)(y+fm.getAscent()/2.0));
            }
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
                    else {Path2D p=new Path2D.Double();p.moveTo(node.getXSelf(),node.getYSelf());p.lineTo(end,d.getFirst().getYSelf());p.lineTo(end,d.getLast().getYSelf());p.closePath();g.fill(p);}
                }
            }
        }
        void nodeAnnotation(Graphics2D g,JSONObject layer) {
            for(JSONObject row:rows(layer)) nodeAnnotationRow(g,row);
        }
        void nodeAnnotationRow(Graphics2D g,JSONObject layer) {
            GraphicsNode node=mrca(layer.getString("name1"),layer.getString("name2"));double size=number(layer,"size",20),x=node.getXSelf(),y=node.getYSelf();
            Shape shape=switch(layer.getString("shape")){case "ellipse"->new Ellipse2D.Double(x-size/2,y-size/2,size,size);case "triangle"->{Path2D p=new Path2D.Double();p.moveTo(x,y-size/2);p.lineTo(x-size/2,y+size/2);p.lineTo(x+size/2,y+size/2);p.closePath();yield p;}case "rect"->new Rectangle2D.Double(x-size/2,y-size/2,size,size);default->throw new IllegalArgumentException("Unknown annotation shape.");};
            Color c=color(layer.getString("fill"));if(c!=null){g.setColor(c);g.fill(shape);}
            g.setColor(Color.BLACK);g.drawString(layer.getString("text"),(float)(x+size/2+5),(float)y);
        }
        void drawTracks(Graphics2D g) {
            double offset=labelWidth+12;
            Map<String,Integer> legendRows=new HashMap<>();
            for(JSONObject layer:layers) {
                String type=layer.getString("type");if(!type.equals("track")&&!type.equals("heatmap"))continue;
                JSONArray names=layer.getJSONArray("names");Map<String,Integer> indices=new HashMap<>();for(int i=0;i<names.size();i++)indices.put(names.getString(i),i);
                double width=number(layer,"width",16);
                if(type.equals("track")) {
                    JSONObject columns=layer.getJSONObject("columns");
                    for(String title:columns.keySet()) {
                        JSONObject column=columns.getJSONObject(title);JSONArray levels=column.getJSONArray("levels"),colors=column.getJSONArray("colors"),values=column.getJSONArray("values");
                        for(GraphicsNode leaf:leaves) {Integer row=indices.get(leaf.getName());if(row==null)continue;String value=values.getString(row);if(value==null)continue;int index=levels.indexOf(value);if(index>=0)cell(g,leaf,offset,width,color(colors.getString(index)));}
                        for(int j=0;j<levels.size();j++) legend(g,layer.getString("legend"),title+": "+levels.getString(j),color(colors.getString(j)),legendRows);
                        trackTitle(g,title,offset+width/2);
                        offset+=width;
                    }
                } else {
                    JSONArray titles=layer.getJSONArray("titles"),cells=layer.getJSONArray("cells");
                    for(int j=0;j<titles.size();j++) {for(GraphicsNode leaf:leaves){Integer row=indices.get(leaf.getName());if(row!=null)cell(g,leaf,offset,width,color(cells.getJSONArray(row).getString(j)));}trackTitle(g,titles.getString(j),offset+width/2);offset+=width;}
                    legend(g,layer.getString("legend"),String.join(", ",titles.toJavaList(String.class)),null,legendRows);
                    JSONArray palette=layer.getJSONArray("palette"),limits=layer.getJSONArray("limits");
                    for(int j=0;j<5;j++)legend(g,layer.getString("legend"),String.format(Locale.ROOT,"%.3g",limits.getDoubleValue(0)+(limits.getDoubleValue(1)-limits.getDoubleValue(0))*j/4),color(palette.getString(j*255/4)),legendRows);
                }
                offset+=4;
            }
        }
        void trackTitle(Graphics2D g,String text,double offset) {
            if(code>=3&&code<=6)return;
            Graphics2D title=(Graphics2D)g.create();
            title.setFont(labelFont.deriveFont(10f));title.setColor(Color.BLACK);
            title.translate(treeBounds.getMaxX()+offset,treeBounds.getMaxY()+10);
            title.rotate(Math.PI/3);title.drawString(text,0,0);title.dispose();
        }
        void cell(Graphics2D g,GraphicsNode leaf,double offset,double width,Color color) {if(color==null)return;g.setColor(color);g.fill(band(leaf,leaf,(code>=3&&code<=6?outer:0)+offset,width));}
        void prepareLegends() {
            Map<String,List<String>> entries=new HashMap<>();
            for(JSONObject layer:layers) {
                String position=layer.getString("legend");if(position==null||position.equals("none"))continue;
                List<String> texts=entries.computeIfAbsent(position,k->new ArrayList<>());
                if(layer.getString("type").equals("track")) {
                    JSONObject columns=layer.getJSONObject("columns");
                    for(String title:columns.keySet()) for(Object value:columns.getJSONObject(title).getJSONArray("levels"))texts.add(title+": "+value);
                } else if(layer.getString("type").equals("heatmap")) {
                    texts.add(String.join(", ",layer.getJSONArray("titles").toJavaList(String.class)));
                    for(int i=0;i<5;i++)texts.add("-0.00000");
                }
            }
            FontMetrics metrics=metricsHost.getFontMetrics(labelFont.deriveFont(10f));
            for(var entry:entries.entrySet()) {
                String position=entry.getKey(),opposite=position.startsWith("top")?position.replace("top","bottom"):position.replace("bottom","top");
                int height=entries.containsKey(opposite)?logicalHeight/2:logicalHeight;
                int cap=Math.max(1,(height-30)/15);
                int width=entry.getValue().stream().mapToInt(metrics::stringWidth).max().orElse(0)+28;
                legendWidths.put(position,width);legendCaps.put(position,cap);
                int reserve=width*((entry.getValue().size()+cap-1)/cap)+8;
                if(position.endsWith("right"))legendReserve=Math.max(legendReserve,reserve);else leftLegendReserve=Math.max(leftLegendReserve,reserve);
            }
        }
        void legend(Graphics2D g,String position,String text,Color color,Map<String,Integer> rows) {
            if("none".equals(position))return;int row=rows.getOrDefault(position,0);rows.put(position,row+1);
            Font previous=g.getFont();g.setFont(labelFont.deriveFont(10f));int w=g.getFontMetrics().stringWidth(text)+24;
            int cap=legendCaps.getOrDefault(position,Math.max(1,(logicalHeight-30)/15));
            int column=row/cap;row%=cap;
            int columnWidth=legendWidths.getOrDefault(position,w);
            int x=position.endsWith("right")?logicalWidth-(column+1)*columnWidth:8+column*columnWidth;
            int y=position.startsWith("bottom")?logicalHeight-12-row*15:15+row*15;
            g.setColor(new Color(255,255,255,230));g.fillRect(x-2,y-12,w+4,15);
            if(color!=null){g.setColor(color);g.fillRect(x,y-9,10,10);}g.setColor(Color.BLACK);g.drawString(text,x+15,y);g.setFont(previous);
        }
        double coordinate(JSONObject layer,String key,boolean x) {double value=number(layer,key,0);return switch(layer.getString("mode")){case "relative"->value*(x?logicalWidth:logicalHeight);case "percent"->value*(x?logicalWidth:logicalHeight)/100;default->value;};}
        Point2D point(JSONObject layer,JSONArray p) {JSONObject value=new JSONObject();value.put("mode",layer.getString("mode"));value.put("x",p.getDoubleValue(0));value.put("y",p.getDoubleValue(1));return new Point2D.Double(coordinate(value,"x",true),coordinate(value,"y",false));}
        Path2D curve(JSONObject layer,String key) {
            JSONArray points=layer.getJSONArray(key);Point2D a=point(layer,points.getJSONArray(0)),b=point(layer,points.getJSONArray(1)),c=point(layer,points.getJSONArray(2)),d=point(layer,points.getJSONArray(3));
            Path2D p=new Path2D.Double();p.moveTo(a.getX(),a.getY());p.curveTo(b.getX(),b.getY(),c.getX(),c.getY(),d.getX(),d.getY());return p;
        }
        void drawFree(Graphics2D g,JSONObject layer) {
            String type=layer.getString("type");double x=coordinate(layer,"x",true),y=coordinate(layer,"y",false),w=coordinate(layer,"width",true),h=coordinate(layer,"height",false);
            Color fill=color(layer.getString("fill_color")),line=color(layer.getString("line_color"));float thickness=(float)number(layer,"line_thickness",1);
            String lineType=layer.getString("line_type");g.setStroke(new BasicStroke(Math.max(.01f,thickness),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10,"dashed".equals(lineType)?new float[]{6,4}:"dotted".equals(lineType)?new float[]{1,3}:null,0));
            if(type.equals("text")){if(fill==null)return;Font previous=g.getFont();g.setFont(labelFont.deriveFont((float)number(layer,"size",12)));String text=layer.getString("text");double width=g.getFontMetrics().stringWidth(text),gap=number(layer,"gap",10);x+=switch(layer.getString("locator")){case "right"->-width-gap;case "center"->-width/2;default->gap;};g.setColor(fill);g.drawString(text,(float)x,(float)y);g.setFont(previous);return;}
            if(type.equals("free_heatmap")){JSONArray cells=layer.getJSONArray("cells");int nr=cells.size(),nc=cells.getJSONArray(0).size();for(int i=0;i<nr;i++)for(int j=0;j<nc;j++){Color c=color(cells.getJSONArray(i).getString(j));if(c!=null){g.setColor(c);g.fill(new Rectangle2D.Double(x+j*w/nc,y+i*h/nr,w/nc,h/nr));}}return;}
            Shape shape;
            switch(type) {
                case "ellipse" -> shape=new Ellipse2D.Double(x-w/2,y-h/2,w,h);
                case "rectangle" -> shape=new Rectangle2D.Double(x,y,w,h);
                case "line","arrow" -> {double x1=coordinate(layer,"x1",true),y1=coordinate(layer,"y1",false),x2=coordinate(layer,"x2",true),y2=coordinate(layer,"y2",false);Path2D p=new Path2D.Double();p.moveTo(x1,y1);p.lineTo(x2,y2);if(type.equals("arrow")){double a=Math.atan2(y2-y1,x2-x1);for(double sign:new double[]{-1,1}){p.moveTo(x2,y2);p.lineTo(x2-9*Math.cos(a+sign*.4),y2-9*Math.sin(a+sign*.4));}}shape=p;}
                case "bezier" -> shape=curve(layer,"points");
                case "bezier_ring" -> {Path2D p=curve(layer,"points");JSONArray ps=layer.getJSONArray("points2");Point2D d=point(layer,ps.getJSONArray(3)),c=point(layer,ps.getJSONArray(2)),b=point(layer,ps.getJSONArray(1)),a=point(layer,ps.getJSONArray(0));p.lineTo(d.getX(),d.getY());p.curveTo(c.getX(),c.getY(),b.getX(),b.getY(),a.getX(),a.getY());p.closePath();shape=p;}
                case "polygon" -> {Path2D p=new Path2D.Double();JSONArray ps=layer.getJSONArray("points");for(int i=0;i<ps.size();i++){Point2D pt=point(layer,ps.getJSONArray(i));if(i==0)p.moveTo(pt.getX(),pt.getY());else p.lineTo(pt.getX(),pt.getY());}p.closePath();shape=p;}
                default -> throw new IllegalArgumentException("Unknown free element: "+type);
            }
            if(fill!=null){g.setColor(fill);g.fill(shape);}if(line!=null&&thickness>0){g.setColor(line);g.draw(shape);}
        }
        @Override public ScaleBarProperty getScaleBarProperty(){return scale;}
        @Override public AnnotationsProperties getMainAnnotationsProperties(){return annotations;}
        @Override public AnnotationsProperties4LinageType getLinageTypeAnnotationsProperties(){return lineages;}
        @Override public Container getHostComponent(){return metricsHost;}
    }
}
