package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import module.evolview.model.tree.GraphicsNode;
import javax.swing.SwingUtilities;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Independent CLI regression entry point; no eGPS frame or R callback is needed. */
public final class YyTreeGeometryTest {
    private static final String SCENE = """
        {"schema_version":1,
         "tree":{"labels":["A","B","C"],"root":5,
           "edges":[[5,4,1],[4,1,0.12345678912345],[4,2,2],[5,3,3]]},
         "canvas":{"width":1000,"height":800,"margins":[20,40,80,40]},
         "layers":[{"type":"layout","code":1},
           {"type":"leaf_render","size":6,"fill":"#0000FFFF"},
           {"type":"leaf_render","df":[{"name":"A","fill":"#FF0000FF"}]}]}
        """;

    public static void main(String[] args) throws Exception {
        String originalTheme=javax.swing.UIManager.getLookAndFeel().getClass().getName();
        new YyTreeBridge().probeRuntime();
        for (int code=1; code<=7; code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            AtomicReference<YyTreeBridge.YyTreePanel> reference=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->reference.set(new YyTreeBridge.YyTreePanel(scene)));
            YyTreeBridge.YyTreePanel panel=reference.get();
            panel.prepare();
            require(panel.leaves.size()==3,"Leaf count changed");
            require(panel.leaves.getFirst().getName().equals("A"),"Input order changed");
            GraphicsNode a=panel.named.get("A"),b=panel.named.get("B"),c=panel.named.get("C");
            require(panel.mrca("A","B")==a.getParent(),"Wrong MRCA");
            require(panel.mrca("A","C")==panel.root,"Root MRCA failed");
            require(a.getRealBranchLength()==0.12345678912345,"Branch precision lost");
            require(panel.sizes.get(a)==6,"Partial style reset earlier radius");
            require(panel.fills.get(a).getRed()==255,"Partial style did not override color");
            require(panel.fills.get(b).getBlue()==255,"Local style leaked to another leaf");
            JSONObject annotations=JSON.parseObject("""
                {"type":"inner_node_annotation","shape":"rect","df":[
                  {"name1":"A","name2":"B","text":"AB","shape":"ellipse"},
                  {"name1":"A","name2":"C","text":"AC","shape":"triangle"}]}
                """);
            var annotationRows=panel.rows(annotations);
            require(annotationRows.get(0)!=annotationRows.get(1),"Annotation rows share a mutable object");
            require(annotationRows.get(0).getString("text").equals("AB"),"First annotation was overwritten");
            require(annotationRows.get(1).getString("shape").equals("triangle"),"Second annotation shape lost");
            require(!annotations.containsKey("text") && annotations.getString("shape").equals("rect"),"Row merge changed layer defaults");
            JSONObject styles=JSON.parseObject("""
                {"type":"leaf_render","df":[{"name":"A","size":12},{"name":"B","fill":"#FFD700FF"}]}
                """);
            panel.applyStyle(styles);
            require(panel.sizes.get(a)==12 && panel.sizes.get(b)==6,"One style row leaked size into another row");
            require(!styles.containsKey("size"),"Style merge changed the layer");
            if(code==2)require(panel.tips.get(a).x==panel.tips.get(b).x,"Rectangular tips do not align");
            if(code==4)for(GraphicsNode node:panel.leaves) {
                Point2D point=panel.tips.get(node);
                require(Math.abs(point.distance(panel.centerX,panel.centerY)-panel.outer)<1e-8,"Circular tips do not align");
            }
            if(code==3 || code==4) for(GraphicsNode node:panel.nodes.values()) {
                double angle=-Math.toRadians(node.getAngleIfNeeded());
                Point2D expected=new Point2D.Double(panel.centerX+node.getRadicusIfNeeded()*Math.cos(angle),panel.centerY+node.getRadicusIfNeeded()*Math.sin(angle));
                require(expected.distance(node.getXSelf(),node.getYSelf())<1e-7,"Circular center disagrees with MTV coordinates");
                if(node!=panel.root) {
                    GraphicsNode parent=(GraphicsNode)node.getParent();
                    double radius=Point2D.distance(panel.centerX,panel.centerY,parent.getXSelf(),parent.getYSelf());
                    expected=new Point2D.Double(panel.centerX+radius*Math.cos(angle),panel.centerY+radius*Math.sin(angle));
                    require(expected.distance(node.getXParent(),node.getYParent())<1e-7,"Circular branch disconnected from parent arc");
                }
            }
            if(code==6)for(GraphicsNode node:panel.leaves) {
                require(panel.tips.get(node).distance(panel.arch(panel.outer,node.getDoubleVariable()))<1e-8,"Arch tips do not align");
            }
            JSONObject relative=JSON.parseObject("{\"mode\":\"relative\",\"x\":0.4,\"y\":0.25}");
            JSONObject percent=JSON.parseObject("{\"mode\":\"percent\",\"x\":40,\"y\":25}");
            JSONObject pixel=JSON.parseObject("{\"mode\":\"pixel\",\"x\":400,\"y\":200}");
            require(panel.coordinate(relative,"x",true)==panel.coordinate(percent,"x",true),"Percent mismatch");
            require(panel.coordinate(relative,"y",false)==panel.coordinate(pixel,"y",false),"Pixel mismatch");
            require(panel.coordinate(relative,"x",true)==panel.coordinate(pixel,"x",true),"Relative mismatch");
        }
        testCircularLabels();
        testGapAndLayoutParameters();
        for(int code=5;code<=6;code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"left\"}"));
            scene.getJSONObject("tree").getJSONArray("labels").set(1,"Long leaf name");
            int[] left=labelPixels(prepare(scene));
            scene.getJSONArray("layers").getJSONObject(3).put("align","right");
            int[] right=labelPixels(prepare(scene));
            require(!Arrays.equals(left,right),"Curved layout "+code+" ignored label alignment");
            double[] textX=new double[3];int index=0;
            for(String align:new String[]{"left","center","right"}) {
                scene.getJSONArray("layers").getJSONObject(3).put("align",align);
                var panel=prepare(scene);var metrics=panel.metricsHost.getFontMetrics(panel.labelFont);
                for(GraphicsNode leaf:panel.leaves) {
                    var bounds=panel.labelBounds(leaf,metrics);var area=panel.labelAreaBounds(leaf,metrics);
                    double start=area.getX();
                    double fraction=align.equals("left")?0:align.equals("center")?.5:1;
                    require(Math.abs(bounds.getX()-start-fraction*(panel.maxLabelWidth-bounds.getWidth()))<1e-7,"Incorrect text alignment geometry");
                }
                textX[index++]=panel.labelBounds(panel.named.get("A"),metrics).getX();
            }
            require(textX[0]<textX[1] && textX[1]<textX[2],"Text alignment anchors are not ordered");
        }
        for(int code:new int[]{5,6}) for(int[] size:new int[][]{{400,1400},{1400,400},{800,800}}) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONObject("canvas").put("width",size[0]);
            scene.getJSONObject("canvas").put("height",size[1]);
            YyTreeBridge.YyTreePanel panel=prepare(scene);
            for(GraphicsNode node:panel.nodes.values()) {
                require(node.getXSelf()>=40 && node.getXSelf()<=size[0]-40,"Arch node outside horizontal margins");
                require(node.getYSelf()>=20 && node.getYSelf()<=size[1]-80,"Arch node outside vertical margins");
                if(node!=panel.root) {
                    GraphicsNode parent=(GraphicsNode)node.getParent();
                    Point2D expected=panel.arch(parent.getRadicusIfNeeded(),node.getDoubleVariable());
                    require(expected.distance(node.getXParent(),node.getYParent())<1e-7,"Arch branch disconnected from parent contour");
                }
            }
            for(GraphicsNode leaf:panel.leaves) {
                Point2D tip=panel.tips.get(leaf);
                require(tip.getX()>=40 && tip.getX()<=size[0]-40,"Aligned arch tip outside margins");
            }
        }
        for(int code=1;code<=7;code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            for(Object edge:scene.getJSONObject("tree").getJSONArray("edges")) ((com.alibaba.fastjson.JSONArray)edge).set(2,0);
            var panel=prepare(scene);
            for(GraphicsNode node:panel.nodes.values()) {
                require(Double.isFinite(node.getXSelf()) && Double.isFinite(node.getYSelf()),"Zero-length tree produced nonfinite coordinates");
                require(node.getRealBranchLength()==0,"Display calculation changed zero branch length");
            }
        }
        for(int code=3;code<=6;code++) for(int[] size:new int[][]{{400,1400},{1400,400},{800,800}}) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONObject("canvas").put("width",size[0]);scene.getJSONObject("canvas").put("height",size[1]);
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"center\"}"));
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"heatmap\",\"titles\":[\"one\",\"two\"],\"width\":16,\"legend\":\"none\"}"));
            var panel=prepare(scene);
            for(GraphicsNode leaf:panel.leaves) {
                var bounds=panel.labelBounds(leaf,panel.metricsHost.getFontMetrics(panel.labelFont));
                require(bounds.getMinX()>=0 && bounds.getMaxX()<=size[0] && bounds.getMinY()>=0 && bounds.getMaxY()<=size[1],"Curved label outside canvas");
                bounds=panel.band(leaf,leaf,panel.outer+panel.labelWidth+12,32).getBounds2D();
                require(bounds.getMinX()>=0 && bounds.getMaxX()<=size[0] && bounds.getMinY()>=0 && bounds.getMaxY()<=size[1],"Curved track outside canvas");
                Point2D midpoint=panel.anchor(leaf,panel.labelWidth+28);
                require(panel.band(leaf,leaf,panel.outer+panel.labelWidth+12,32).intersects(midpoint.getX()-.01,midpoint.getY()-.01,.02,.02),"Curved track is not centered on leaf "+leaf.getName()+" in layout "+code);
            }
        }
        for(int code=1;code<=7;code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONArray("layers").add(JSON.parseObject("""
                {"type":"track","names":["C","A"],"width":20,"legend":"none",
                 "columns":{"group":{"levels":["x","z"],"colors":["#FF0000FF","#0000FFFF"],"values":["z","x"]}}}
                """,Feature.OrderedField));
            JSONObject heat=JSON.parseObject("""
                {"type":"heatmap","names":["B","A","C"],"width":20,"legend":"none",
                 "titles":["value"],"cells":[["#00FF00FF"],[null],["#FFD700FF"]],"limits":[0,1],"palette":[]}
                """);
            for(int i=0;i<256;i++) heat.getJSONArray("palette").add("#000000FF");
            scene.getJSONArray("layers").add(heat);
            var panel=prepare(scene);
            var image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_ARGB);
            var graphics=image.createGraphics();graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,panel.logicalWidth,panel.logicalHeight);
            panel.drawTracks(graphics);graphics.dispose();
            int[] category={0xFF0000,0xFFFFFF,0x0000FF},numeric={0xFFFFFF,0x00FF00,0xFFD700};
            int index=0;
            for(GraphicsNode leaf:panel.leaves) {
                Point2D first=trackSample(panel,leaf,panel.labelWidth+22),second=trackSample(panel,leaf,panel.labelWidth+46);
                require((image.getRGB((int)first.getX(),(int)first.getY())&0xFFFFFF)==category[index],"Category row/name matching or missing leaf failed in layout "+code);
                require((image.getRGB((int)second.getX(),(int)second.getY())&0xFFFFFF)==numeric[index],"Heatmap row/name matching, missing value or track order failed in layout "+code);
                index++;
            }
        }
        JSONObject fontScene=JSON.parseObject(SCENE,Feature.OrderedField);
        int defaultSize=prepare(fontScene).labelFont.getSize();
        fontScene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"left\",\"font_size\":36}"));
        fontScene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"right\"}"));
        require(prepare(fontScene).labelFont.getSize()==defaultSize,"Last label settings retained an earlier font override");
        JSONObject legendScene=JSON.parseObject(SCENE,Feature.OrderedField);
        legendScene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"heatmap\",\"titles\":[\"x\"],\"width\":16,\"legend\":\"top_right\",\"limits\":[-1e200,1e200]}"));
        var legendPanel=prepare(legendScene);
        int requiredWidth=legendPanel.metricsHost.getFontMetrics(legendPanel.labelFont.deriveFont(10f)).stringWidth("-1.00e+200")+28;
        require(legendPanel.legendWidths.get("top_right")>=requiredWidth,"Scientific heatmap legend labels exceed their reserved width");
        require(!egps2.UnifiedAccessPoint.isGULaunched(),"The eGPS desktop was launched");
        require(originalTheme.equals(javax.swing.UIManager.getLookAndFeel().getClass().getName()),"Probe or layout initialized the preview theme");
        require(java.awt.Frame.getFrames().length==0,"Probe or layout opened a window");
        System.out.println("Seven-layout geometry, MRCA, style, precision and coordinate checks passed.");
    }
    private static YyTreeBridge.YyTreePanel prepare(JSONObject scene) throws Exception {
        AtomicReference<YyTreeBridge.YyTreePanel> reference=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->reference.set(new YyTreeBridge.YyTreePanel(scene)));
        reference.get().prepare();return reference.get();
    }
    private static void testGapAndLayoutParameters() throws Exception {
        for(int code=1;code<=7;code++) for(double gap:new double[]{0,6,28.5}) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONObject("tree").getJSONArray("labels").set(1,"Longest leaf WWW");
            JSONObject settings=JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"left\",\"font_size\":24}");
            settings.put("gap_length",gap);scene.getJSONArray("layers").add(settings);
            var panel=prepare(scene);var fm=panel.metricsHost.getFontMetrics(panel.labelFont);
            for(GraphicsNode leaf:panel.leaves) {
                var tip=panel.tips.get(leaf);var bounds=panel.labelBounds(leaf,fm);
                require(bounds.getMinX()>=0 && bounds.getMaxX()<=panel.logicalWidth
                    && bounds.getMinY()>=0 && bounds.getMaxY()<=panel.logicalHeight,"Gap label is clipped in layout "+code);
                if(code==3||code==4) {
                    var geometry=panel.circularLabelGeometry(leaf,fm);
                    double near=Math.min(Math.abs(geometry.x()),Math.abs(geometry.x()+fm.stringWidth(leaf.getName())));
                    require(Math.abs(near-6-gap)<1e-7,"Circular gap omitted marker radius or gap");
                } else {
                    var area=panel.labelAreaBounds(leaf,fm);
                    require(area.getWidth()==fm.stringWidth("Longest leaf WWW"),"Label regions do not use longest measured width");
                    double nx=1,ny=0;
                    if(code==5||code==6) {
                        var contour=panel.arch(panel.outer,leaf.getDoubleVariable());
                        var farther=panel.arch(panel.outer+1,leaf.getDoubleVariable());
                        nx=farther.x-contour.x;ny=farther.y-contour.y;
                    }
                    double nearest=Double.POSITIVE_INFINITY;
                    for(double x:new double[]{area.getMinX(),area.getMaxX()})
                        for(double y:new double[]{area.getMinY(),area.getMaxY()})
                            nearest=Math.min(nearest,(x-tip.x)*nx+(y-tip.y)*ny);
                    require(Math.abs(nearest-6-gap)<1e-7,"Label region does not follow its own tip in layout "+code);
                    require(bounds.getX()==area.getX(),"Left alignment did not start at the region edge");
                    if(code==1||code==2||code==7)
                        require(Math.abs(bounds.getX()-tip.x-6-gap)<1e-7,"Right-facing label has extra blank space");
                }
            }
            var a=panel.named.get("A");double left=panel.labelBounds(a,fm).getX();
            for(String align:new String[]{"center","right"}) {
                panel.align=align;var bounds=panel.labelBounds(a,fm);
                double expected=code==3||code==4?0:(fm.stringWidth("Longest leaf WWW")-fm.stringWidth("A"))*(align.equals("center")?.5:1);
                require(Math.abs(bounds.getX()-left-expected)<1e-7,"Alignment no longer refers to longest label");
            }
        }
        for(int code:new int[]{3,4}) for(int extent:new int[]{1,90,270,360}) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            JSONObject layout=scene.getJSONArray("layers").getJSONObject(0);
            layout.put("code",code);layout.put("start_angle",330);layout.put("angle_extent",extent);
            var panel=prepare(scene);double first=panel.leaves.getFirst().getAngleIfNeeded();
            require(Math.abs(first-330)<1e-7,"Circular start angle ignored");
            double last=panel.leaves.getLast().getAngleIfNeeded();
            require(Math.abs(last-first-(extent==360?240:extent))<1e-7,"Circular extent ignored");
            for(GraphicsNode leaf:panel.leaves) {
                var point=panel.anchor(leaf,panel.labelWidth+20);
                require(panel.band(leaf,leaf,panel.outer+panel.labelWidth+12,16)
                    .intersects(point.x-.01,point.y-.01,.02,.02),"Open-circle track misses its tip angle");
            }
            if(extent<360) {
                var firstLeaf=panel.leaves.getFirst();double angle=-Math.toRadians(firstLeaf.getAngleIfNeeded()-1);
                double radius=panel.outer+panel.labelWidth+20;
                require(!panel.band(firstLeaf,panel.leaves.getLast(),panel.outer+panel.labelWidth+12,16)
                    .contains(panel.centerX+radius*Math.cos(angle),panel.centerY+radius*Math.sin(angle)),"Highlight extends into circular opening");
            }
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"layout\",\"code\":3}"));
            var reset=prepare(scene);
            require(reset.properties.getCircularLayoutPropertiy().getGlobalStartDegree()==285
                && reset.properties.getCircularLayoutPropertiy().getGlobalExtendDegree()==360,"Last layout retained earlier angle settings");
        }
        JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
        JSONObject layout=scene.getJSONArray("layers").getJSONObject(0);layout.put("code",7);
        var wide=prepare(scene);double width=wide.tips.get(wide.leaves.getFirst()).x-wide.root.getXSelf();
        layout.put("tree_width_ratio",.6);
        var narrow=prepare(scene);
        require(Math.abs(narrow.tips.get(narrow.leaves.getFirst()).x-narrow.root.getXSelf()-.6*width)<1e-7,"Tree width did not scale all nodes consistently");
        for(GraphicsNode node:narrow.nodes.values())if(node!=narrow.root)
            require(((GraphicsNode)node.getParent()).getXSelf()<=node.getXSelf(),"Tree width made a branch point backwards");
        layout.put("left_margin_ratio",.4);
        narrow=prepare(scene);
        require(narrow.tips.get(narrow.leaves.getFirst()).x-narrow.root.getXSelf()<width,"Slanted width/margin parameters did not narrow tree");
        scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"layout\",\"code\":7}"));
        var reset=prepare(scene);
        require(Math.abs(reset.tips.get(reset.leaves.getFirst()).x-reset.root.getXSelf()-width)<1e-7,"Last slanted layout retained earlier width settings");
        scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"left\",\"gap_length\":1e100}"));
        try { prepare(scene);throw new AssertionError("Impossible gap accepted"); }
        catch(IllegalArgumentException expected) {
            require(expected.getMessage().contains("gap_length") && expected.getMessage().contains("Example:"),"Canvas failure has no correction example");
        }
        JSONObject single=JSON.parseObject(SCENE,Feature.OrderedField);
        single.put("tree",JSON.parseObject("{\"labels\":[\"A\"],\"root\":2,\"edges\":[[2,1,1]]}"));
        single.getJSONArray("layers").getJSONObject(0).put("code",3);
        single.getJSONArray("layers").getJSONObject(0).put("angle_extent",90);
        var panel=prepare(single);var leaf=panel.leaves.getFirst();var point=panel.anchor(leaf,panel.labelWidth+20);
        require(panel.band(leaf,leaf,panel.outer+panel.labelWidth+12,16).contains(point),"Single-leaf open-circle track disappeared");
    }
    private static void testCircularLabels() throws Exception {
        for(int code:new int[]{3,4}) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"right\",\"gap_length\":4}"));
            scene.getJSONObject("tree").getJSONArray("labels").set(1,"Long leaf name WWW");
            var panel=prepare(scene);
            panel.labelColors.put(panel.leaves.get(1),java.awt.Color.YELLOW);
            var metrics=panel.metricsHost.getFontMetrics(panel.labelFont);
            var leaf=panel.leaves.get(1);
            Point2D.Double tip=panel.tips.get(leaf);
            double radius=tip.distance(panel.centerX,panel.centerY);
            for(double angle:new double[]{0,45,89,90,91,135,180,225,269,270,271,315,360}) {
                leaf.setAngleIfNeeded(angle);
                tip.setLocation(panel.centerX+radius*Math.cos(Math.toRadians(angle)),panel.centerY-radius*Math.sin(Math.toRadians(angle)));
                var placement=module.evolview.phylotree.visualization.layout.TipLabelGeometry.at(angle,6,leaf.getName(),metrics);
                var expected=placement.boundsAt(tip.x,tip.y).getBounds2D();
                var actual=panel.labelBounds(leaf,metrics);
                require(Math.abs(actual.getX()-expected.getX())<1e-7 && Math.abs(actual.getY()-expected.getY())<1e-7
                    && Math.abs(actual.getWidth()-expected.getWidth())<1e-7 && Math.abs(actual.getHeight()-expected.getHeight())<1e-7,
                    "Circular label does not follow the MTV tip geometry at "+angle+" in layout "+code);
            }
            panel=prepare(scene);
            panel.labelColors.put(panel.leaves.get(1),java.awt.Color.YELLOW);
            for(double scale:new double[]{.65,1,1.25,2}) {
                int width=(int)Math.ceil(panel.logicalWidth*scale),height=(int)Math.ceil(panel.logicalHeight*scale);
                BufferedImage actual=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                BufferedImage expected=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                var g=actual.createGraphics();g.scale(scale,scale);g.setFont(panel.labelFont);
                var transform=g.getTransform();panel.drawLabels(g);
                require(transform.equals(g.getTransform()),"Circular labels changed the caller transform");g.dispose();
                g=expected.createGraphics();g.scale(scale,scale);g.setFont(panel.labelFont);
                for(GraphicsNode node:panel.leaves) {
                    var p=panel.tips.get(node);
                    var placement=module.evolview.phylotree.visualization.layout.TipLabelGeometry.at(node.getAngleIfNeeded(),6,node.getName(),g.getFontMetrics());
                    var label=(java.awt.Graphics2D)g.create();label.translate(p.x,p.y);label.rotate(placement.rotation());
                    if(panel.labelColors.containsKey(node)){label.setColor(panel.labelColors.get(node));label.fill(placement.box());}
                    label.setColor(java.awt.Color.BLACK);label.drawString(node.getName(),(float)placement.x(),(float)placement.baseline());label.dispose();
                }
                g.dispose();
                require(Arrays.equals(actual.getRGB(0,0,width,height,null,0,width),expected.getRGB(0,0,width,height,null,0,width)),
                    "Circular label painting differs from MTV at scale "+scale);
            }
            int[] right=labelPixels(panel);
            scene.getJSONArray("layers").getJSONObject(3).put("align","left");
            var left=prepare(scene);left.labelColors.put(left.leaves.get(1),java.awt.Color.YELLOW);
            require(Arrays.equals(right,labelPixels(left)),"Circular labels still use horizontal column alignment");
            var image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,image.getWidth(),image.getHeight());
            panel.drawScene(g);
            require(java.awt.RenderingHints.VALUE_ANTIALIAS_ON.equals(g.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING)),"Shape antialiasing is off");
            require(java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON.equals(g.getRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING)),"Text antialiasing is off");
            g.dispose();
            require(Arrays.stream(image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()))
                .anyMatch(pixel->{int r=(pixel>>16)&255,b=pixel&255;return r>0 && r<255 && r==b && r==((pixel>>8)&255);}),
                "Circular rendering has no antialiased edge pixels");
        }
    }
    private static int[] labelPixels(YyTreeBridge.YyTreePanel panel) {
        BufferedImage image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_ARGB);
        var graphics=image.createGraphics();graphics.setFont(panel.labelFont);panel.drawLabels(graphics);graphics.dispose();
        return image.getRGB(0,0,panel.logicalWidth,panel.logicalHeight,null,0,panel.logicalWidth);
    }
    private static Point2D trackSample(YyTreeBridge.YyTreePanel panel,GraphicsNode leaf,double offset) {
        if(panel.code==5 || panel.code==6) {
            double p=Math.max(1,Math.min(panel.archTotal-1,leaf.getDoubleVariable()));
            return panel.arch(panel.outer+offset,p);
        }
        Point2D.Double point=panel.anchor(leaf,offset);
        if(panel.code==1 || panel.code==2 || panel.code==7)
            point.y=Math.max(panel.treeBounds.getMinY()+1,Math.min(panel.treeBounds.getMaxY()-1,point.y));
        return point;
    }
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
