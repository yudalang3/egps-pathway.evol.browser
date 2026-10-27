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
        testSlantDirections();
        testHiddenLabelTracks();
        testAnnotationSpacingAndLegends();
        testAnnotationTitles();
        testContinuousHeatLegend();
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
    private static JSONObject categoryTrack(String title,int count,String position) {
        JSONObject track=JSON.parseObject("""
            {"type":"track","names":["A","B","C"],"width":20,"columns":{}}
            """,Feature.OrderedField);
        track.put("legend",position);
        JSONObject column=JSON.parseObject("{\"levels\":[],\"colors\":[],\"values\":[\"category 0\",\"category 0\",\"category 0\"]}");
        for(int i=0;i<count;i++) {
            column.getJSONArray("levels").add("category "+i);
            column.getJSONArray("colors").add("#FF0000FF");
        }
        track.getJSONObject("columns").put(title,column);
        return track;
    }
    private static BufferedImage annotationImage(YyTreeBridge.YyTreePanel panel) {
        BufferedImage image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,image.getWidth(),image.getHeight());
        panel.drawTracks(g);g.dispose();return image;
    }
    private static JSONObject gradientHeat(String position,boolean constant,int alpha) {
        JSONObject heat=JSON.parseObject("""
            {"type":"heatmap","names":["A","B","C"],"titles":["signal"],"width":20,
             "legend_title":"Signal","limits":[-2,2],"cells":[[null],[null],[null]],"palette":[]}
            """);
        heat.put("legend",position);
        if(constant)heat.getJSONArray("limits").set(0,2);
        for(int i=0;i<256;i++)heat.getJSONArray("palette").add(String.format(java.util.Locale.ROOT,"#%02X00%02X%02X",i,255-i,alpha));
        return heat;
    }
    private static int colorOnWhite(String color) {
        BufferedImage image=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,1,1);
        g.setColor(YyTreeBridge.color(color));g.fillRect(0,0,1,1);g.dispose();return image.getRGB(0,0);
    }
    private static void testContinuousHeatLegend() throws Exception {
        for(String position:new String[]{"top_left","top_right","bottom_left","bottom_right"})
            for(boolean constant:new boolean[]{false,true})for(int alpha:new int[]{255,128}) {
                JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
                scene.getJSONArray("layers").add(categoryTrack("before",2,position));
                JSONObject heat=gradientHeat(position,constant,alpha);scene.getJSONArray("layers").add(heat);
                scene.getJSONArray("layers").add(categoryTrack("after",2,position));
                var panel=prepare(scene);var column=panel.legendColumns.getFirst();
                require(panel.legendColumns.size()==1,"Small mixed legend should stay together");
                require(column.rows().stream().filter(YyTreeBridge.YyTreePanel.LegendRow::separator).count()==2,"Gradient lost group separators");
                int top=column.top();
                for(var row:column.rows()) {if(row.gradient()!=null)break;top+=row.units()*15;}
                top-=9;int x=column.x();
                BufferedImage image=annotationImage(panel);
                java.util.Set<Integer> colors=new java.util.HashSet<>();
                for(int y=0;y<80;y++) {
                    int actual=image.getRGB(x+6,top+y);colors.add(actual);
                    int index=constant?127:(int)Math.round(255*(1-y/79.0));
                    require(actual==colorOnWhite(heat.getJSONArray("palette").getString(index)),"Gradient differs from heatmap palette or alpha");
                }
                require(constant?colors.size()==1:colors.size()>60,"Heatmap legend is discrete instead of continuous");
                require((image.getRGB(x+14,top+40)&0xFFFFFF)==0,"Middle tick missing");
                require((image.getRGB(x+14,top)&0xFFFFFF)==(constant?0xFFFFFF:0),"Top tick should appear only for a nonconstant scale");
                require((image.getRGB(x+14,top+79)&0xFFFFFF)==(constant?0xFFFFFF:0),"Bottom tick should appear only for a nonconstant scale");
                require(top>=0 && top+80<panel.logicalHeight,"Gradient is clipped");
            }
        JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
        scene.getJSONObject("canvas").put("height",300);
        scene.getJSONArray("layers").add(categoryTrack("first",13,"top_right"));
        scene.getJSONArray("layers").add(gradientHeat("top_right",false,255));
        var panel=prepare(scene);
        require(panel.legendColumns.size()==2,"Gradient block should move to the next column");
        var rows=panel.legendColumns.get(1).rows();
        require(rows.size()==2 && rows.getFirst().heading() && rows.getLast().gradient()!=null,"Gradient and title were split");
        scene=JSON.parseObject(SCENE,Feature.OrderedField);
        scene.getJSONArray("layers").add(gradientHeat("none",false,255));
        require(prepare(scene).legendColumns.isEmpty(),"Hidden gradient legend was created");
    }
    private static void testAnnotationTitles() throws Exception {
        for(int code:new int[]{1,2,7})for(String direction:code==7?new String[]{"right","down","left","up"}:new String[]{"right"})
            for(double angle:new double[]{-90,-45,0,45,60,90}) {
                JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
                JSONObject layout=scene.getJSONArray("layers").getJSONObject(0);layout.put("code",code);
                if(code==7)layout.put("direction",direction);
                String name="Long annotation column label";
                JSONObject track=categoryTrack(name,1,"none");track.put("text_rotation",angle);
                scene.getJSONArray("layers").add(track);
                var panel=prepare(scene);
                BufferedImage image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_ARGB);
                var g=image.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,image.getWidth(),image.getHeight());
                panel.trackTitle(g,name,panel.labelWidth+12+panel.trackOffsets.get(track)+10,angle);g.dispose();
                int count=0,minX=image.getWidth(),maxX=0,minY=image.getHeight(),maxY=0;
                double sumX=0,sumY=0,sumXY=0;
                double edge=code==7?panel.slantTreeBounds().getMaxY():panel.treeBounds.getMaxY();
                for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
                    if((image.getRGB(x,y)&0xFFFFFF)!=0xFFFFFF) {
                        count++;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                        double along=code==7?panel.along(x,y):x,across=code==7?panel.across(x,y):y;
                        require(across>=edge+3,"Rotated title overlaps annotation area: "+direction+" "+angle);
                        sumX+=along;sumY+=across;sumXY+=along*across;
                    }
                require(count>0 && minX>0 && minY>0 && maxX<image.getWidth()-1 && maxY<image.getHeight()-1,
                    "Rotated title is missing or clipped: "+direction+" "+angle);
                double covariance=sumXY/count-(sumX/count)*(sumY/count);
                if(angle==-45||angle==45)require(Math.signum(covariance)==Math.signum(angle),"Title tilts the wrong way: "+direction+" "+angle);
            }
        JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
        JSONObject track=categoryTrack("group",2,"top_right");
        track.getJSONObject("columns").put("habitat",categoryTrack("habitat",2,"top_right").getJSONObject("columns").getJSONObject("habitat"));
        track.put("legend_titles",JSON.parseObject("{\"group\":\"Species group\"}"));
        scene.getJSONArray("layers").add(track);
        JSONObject heat=JSON.parseObject("""
            {"type":"heatmap","names":["A","B","C"],"titles":["score"],"width":20,
             "legend":"top_right","legend_title":"Expression level","limits":[0,1]}
            """);
        scene.getJSONArray("layers").add(heat);
        var panel=prepare(scene);
        var rows=panel.legendColumns.getFirst().rows();
        require(rows.stream().anyMatch(r->r.heading()&&r.text().equals("Species group")),"Custom category title missing");
        require(rows.stream().anyMatch(r->r.heading()&&r.text().equals("habitat")),"Unspecified category title changed");
        require(rows.stream().anyMatch(r->r.heading()&&r.text().equals("Expression level")),"Custom heatmap title missing");
        require(rows.stream().filter(YyTreeBridge.YyTreePanel.LegendRow::separator).count()==1,"Custom titles changed legend grouping");
    }
    private static void testAnnotationSpacingAndLegends() throws Exception {
        for(int code=1;code<=7;code++)for(String direction:code==7?new String[]{"right","down","left","up"}:new String[]{"right"})
            for(double gap:new double[]{0,2.5,12}) {
                JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
                JSONObject layout=scene.getJSONArray("layers").getJSONObject(0);layout.put("code",code);
                if(code==7)layout.put("direction",direction);
                var layers=scene.getJSONArray("layers");
                layers.add(JSON.parseObject("{\"type\":\"annotation_gap\",\"width\":99}"));
                JSONObject first=categoryTrack("first",1,"none"),second=categoryTrack("second",1,"none"),third=categoryTrack("third",1,"none");
                layers.add(first);
                layers.add(JSON.parseObject("{\"type\":\"annotation_gap\",\"width\":99}"));
                layers.add(JSON.parseObject("{\"type\":\"leaf_render\",\"size\":0}"));
                JSONObject spacing=JSON.parseObject("{\"type\":\"annotation_gap\"}");spacing.put("width",gap);layers.add(spacing);
                layers.add(second);
                layers.add(JSON.parseObject("{\"type\":\"annotation_gap\",\"width\":20}"));layers.add(third);
                layers.add(JSON.parseObject("{\"type\":\"annotation_gap\",\"width\":99}"));
                var panel=prepare(scene);
                require(panel.trackOffsets.get(first)==0,"Leading gap added space");
                require(panel.trackOffsets.get(second)==20+gap,"Gap did not replace default spacing");
                require(panel.trackOffsets.get(third)==60+gap,"Second explicit gap changed");
                require(panel.trackWidth==84+gap,"Trailing gap changed reserved width");
                BufferedImage image=annotationImage(panel);
                for(GraphicsNode leaf:panel.leaves) {
                    Point2D sample=trackSample(panel,leaf,panel.labelWidth+12+20+gap+10);
                    require((image.getRGB((int)sample.getX(),(int)sample.getY())&0xFFFFFF)==0xFF0000,"Track offset was not used for painting");
                    if(gap>=12) {
                        sample=trackSample(panel,leaf,panel.labelWidth+12+20+gap/2);
                        require((image.getRGB((int)sample.getX(),(int)sample.getY())&0xFFFFFF)==0xFFFFFF,"Gap was painted instead of left blank");
                    }
                }
            }
        JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
        JSONObject first=categoryTrack("first",1,"none"),second=categoryTrack("second",1,"none");
        scene.getJSONArray("layers").add(first);scene.getJSONArray("layers").add(second);
        var panel=prepare(scene);
        require(panel.trackOffsets.get(second)==24 && panel.trackWidth==48,"Default spacing changed");
        scene=JSON.parseObject(SCENE,Feature.OrderedField);
        scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"annotation_gap\",\"width\":100}"));
        require(prepare(scene).trackWidth==0,"A gap without annotations reserves space");
        for(String position:new String[]{"top_left","top_right","bottom_left","bottom_right"}) {
            scene=JSON.parseObject(SCENE,Feature.OrderedField);
            first=categoryTrack("group",3,position);
            first.getJSONObject("columns").put("habitat",categoryTrack("habitat",2,position).getJSONObject("columns").getJSONObject("habitat"));
            scene.getJSONArray("layers").add(first);
            JSONObject heat=JSON.parseObject("""
                {"type":"heatmap","names":["A","B","C"],"titles":["control","treated","score","expression"],
                 "cells":[[null,null,null,null],[null,null,null,null],[null,null,null,null]],"width":20,"limits":[-1e200,1e200],"palette":[]}
                """);
            heat.put("legend",position);
            for(int i=0;i<256;i++)heat.getJSONArray("palette").add("#0000FFFF");
            scene.getJSONArray("layers").add(heat);
            scene.getJSONArray("layers").add(JSON.parseObject(first.toJSONString(),Feature.OrderedField));
            panel=prepare(scene);
            require(panel.legendColumns.size()==1,"Small annotation blocks should stay in one column");
            var column=panel.legendColumns.getFirst();var rows=column.rows();
            require(rows.stream().filter(YyTreeBridge.YyTreePanel.LegendRow::separator).count()==2,"Expected one separator per annotation boundary");
            require(rows.stream().filter(r->r.heading()&&r.text().equals("group")).count()==2,"Repeated legend was merged");
            require(rows.getFirst().heading()&&rows.getFirst().text().equals("group"),"Column title missing or reordered");
            require(rows.get(1).text().equals("category 0"),"Category text still contains column prefix");
            require(column.top()>=15 && column.top()+(rows.size()-1)*15<=panel.logicalHeight-12,"Legend outside canvas");
            BufferedImage image=annotationImage(panel);
            int separator=0;while(!rows.get(separator).separator())separator++;
            int y=column.top()+separator*15-5,ink=0,blank=0;
            for(int x=column.x();x<column.x()+column.width()-12;x++) {
                int rgb=image.getRGB(x,y)&0xFFFFFF;
                if(rgb==0x969696)ink++;
                if(rgb==0xFFFFFF)blank++;
            }
            require(ink>0&&blank>0,"Legend separator is not dashed");
            int required=panel.metricsHost.getFontMetrics(panel.labelFont.deriveFont(10f)).stringWidth("-1.00e+200")+28;
            require(column.width()>=required,"Scientific labels clipped");
        }
        scene=JSON.parseObject(SCENE,Feature.OrderedField);scene.getJSONObject("canvas").put("width",1800);
        scene.getJSONObject("canvas").put("height",300);
        scene.getJSONArray("layers").add(categoryTrack("first",9,"top_right"));
        scene.getJSONArray("layers").add(categoryTrack("second",9,"top_right"));
        panel=prepare(scene);
        require(panel.legendColumns.size()==2,"A complete block should move to the next column");
        require(panel.legendColumns.get(1).rows().getFirst().text().equals("second"),"Block was split unnecessarily");
        scene.getJSONArray("layers").add(categoryTrack("long classification heading",40,"bottom_left"));
        panel=prepare(scene);
        int middle=panel.logicalWidth/2;
        var bottom=panel.legendColumns.stream().filter(c->c.x()<middle).toList();
        require(bottom.size()>1,"Oversized block did not wrap");
        for(var column:bottom) {
            require(column.rows().getFirst().heading()&&column.rows().getFirst().text().equals("long classification heading"),"Continuation heading missing");
            require(!column.rows().getLast().heading()&&!column.rows().getFirst().separator(),"Orphan legend heading or separator");
        }
        scene=JSON.parseObject(SCENE,Feature.OrderedField);
        scene.getJSONArray("layers").add(categoryTrack("top",30,"top_right"));
        scene.getJSONArray("layers").add(categoryTrack("bottom",30,"bottom_right"));
        panel=prepare(scene);
        for(var column:panel.legendColumns) {
            boolean top=column.rows().getFirst().text().equals("top");
            require(top?column.top()+column.rows().size()*15<=panel.logicalHeight/2:column.top()-12>=panel.logicalHeight/2,
                "Top and bottom legends overlap");
        }
        scene=JSON.parseObject(SCENE,Feature.OrderedField);
        scene.getJSONArray("layers").add(categoryTrack("hidden",3,"none"));
        panel=prepare(scene);
        require(panel.legendColumns.isEmpty()&&panel.legendReserve==0&&panel.leftLegendReserve==0,"Hidden legend reserved space");
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
    private static void testSlantDirections() throws Exception {
        for(String direction:new String[]{"right","down","left","up"}) {
            for(int[] dimensions:new int[][]{{1000,800},{500,1400},{1400,500}})
                for(double gap:new double[]{0,6,24})for(String align:new String[]{"left","center","right"}) {
                    JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
                    JSONObject layout=scene.getJSONArray("layers").getJSONObject(0);layout.put("code",7);layout.put("direction",direction);
                    scene.getJSONObject("canvas").put("width",dimensions[0]);scene.getJSONObject("canvas").put("height",dimensions[1]);
                    scene.getJSONObject("canvas").put("margins",JSON.parseArray("[27,39,63,71]"));
                    scene.getJSONObject("tree").getJSONArray("labels").set(1,"Longest leaf WWW");
                    JSONObject labels=JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"font_size\":18}");
                    labels.put("align",align);labels.put("gap_length",gap);scene.getJSONArray("layers").add(labels);
                    var panel=prepare(scene);var fm=panel.metricsHost.getFontMetrics(panel.labelFont);
                    double previous=Double.NEGATIVE_INFINITY;
                    for(GraphicsNode leaf:panel.leaves) {
                        var tip=panel.tips.get(leaf);var area=panel.labelAreaBounds(leaf,fm);var bounds=panel.labelBounds(leaf,fm);
                        require(bounds.getMinX()>=0 && bounds.getMaxX()<=dimensions[0] && bounds.getMinY()>=0 && bounds.getMaxY()<=dimensions[1],"Slant label clipped: "+direction);
                        double clearance=projectedMinimum(area,panel.growthX,panel.growthY)-panel.along(tip.x,tip.y);
                        require(Math.abs(clearance-6-gap)<1e-7,"Direction changed label clearance: "+direction);
                        double position=panel.across(tip.x,tip.y);require(position>previous,"Rotation changed original leaf order");previous=position;
                        var geometry=panel.slantLabelGeometry(leaf,fm);
                        require(Math.abs(geometry.rotation())<=Math.PI/2,"Slant label is upside down");
                        double rx=Math.cos(geometry.rotation()),ry=Math.sin(geometry.rotation());
                        double fraction=align.equals("left")?0:align.equals("center")?.5:1;
                        double offset=projectedMinimum(bounds,rx,ry)-projectedMinimum(area,rx,ry);
                        require(Math.abs(offset-fraction*(panel.maxLabelWidth-fm.stringWidth(leaf.getName())))<1e-7,"Direction broke longest-label alignment: "+direction);
                    }
                    for(GraphicsNode node:panel.nodes.values())if(node!=panel.root) {
                        var parent=(GraphicsNode)node.getParent();
                        require(panel.along(node.getXSelf()-parent.getXSelf(),node.getYSelf()-parent.getYSelf())>=-1e-7,"A slant branch points backwards");
                    }
                    require(panel.named.get("A").getRealBranchLength()==0.12345678912345,"Rotation changed a stored length");
                }
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            JSONObject layout=scene.getJSONArray("layers").getJSONObject(0);layout.put("code",7);layout.put("direction",direction);
            var wide=prepare(scene);var tip=wide.tips.get(wide.leaves.getFirst());
            double span=wide.along(tip.x-wide.root.getXSelf(),tip.y-wide.root.getYSelf());
            layout.put("tree_width_ratio",.6);var narrow=prepare(scene);tip=narrow.tips.get(narrow.leaves.getFirst());
            require(Math.abs(narrow.along(tip.x-narrow.root.getXSelf(),tip.y-narrow.root.getYSelf())-.6*span)<1e-7,"Width ratio does not follow growth axis");
            layout.remove("tree_width_ratio");layout.put("left_margin_ratio",.4);narrow=prepare(scene);tip=narrow.tips.get(narrow.leaves.getFirst());
            require(narrow.along(tip.x-narrow.root.getXSelf(),tip.y-narrow.root.getYSelf())<span,"Margin ratio does not follow growth axis");
            layout.remove("left_margin_ratio");
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"right\"}"));
            scene.getJSONArray("layers").add(JSON.parseObject("""
                {"type":"track","names":["C","A"],"width":20,"legend":"none",
                 "columns":{"group":{"levels":["x","z"],"colors":["#FF0000FF","#0000FFFF"],"values":["z","x"]}}}
                """,Feature.OrderedField));
            var heat=JSON.parseObject("""
                {"type":"heatmap","names":["B","A","C"],"width":20,"legend":"none","titles":["value"],
                 "cells":[["#00FF00FF"],[null],["#FFD700FF"]],"limits":[0,1],"palette":[]}
                """);
            for(int i=0;i<256;i++)heat.getJSONArray("palette").add("#000000FF");scene.getJSONArray("layers").add(heat);
            var panel=prepare(scene);var image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_RGB);
            var graphics=image.createGraphics();graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,image.getWidth(),image.getHeight());graphics.setFont(panel.labelFont);
            panel.drawTracks(graphics);int index=0;int[] category={0xFF0000,0xFFFFFF,0x0000FF},numeric={0xFFFFFF,0x00FF00,0xFFD700};
            for(GraphicsNode leaf:panel.leaves) {
                var first=panel.anchor(leaf,panel.labelWidth+22);var second=panel.anchor(leaf,panel.labelWidth+46);
                require((image.getRGB((int)first.x,(int)first.y)&0xFFFFFF)==category[index],"Rotated category rows mismatched");
                require((image.getRGB((int)second.x,(int)second.y)&0xFFFFFF)==numeric[index],"Rotated heatmap rows mismatched");index++;
                var bounds=panel.band(leaf,leaf,panel.labelWidth+12,44).getBounds2D();
                require(bounds.getMinX()>=0 && bounds.getMaxX()<=image.getWidth() && bounds.getMinY()>=0 && bounds.getMaxY()<=image.getHeight(),"Rotated tracks clipped");
            }
            var highlight=JSON.parseObject("{\"type\":\"inner2leaf_highlight\",\"name1\":\"A\",\"name2\":\"C\",\"fill\":\"#00FF00FF\"}");
            panel.highlight(graphics,highlight);
            int cx=(int)((panel.root.getXSelf()+panel.tips.get(panel.leaves.getFirst()).x+panel.tips.get(panel.leaves.getLast()).x)/3);
            int cy=(int)((panel.root.getYSelf()+panel.tips.get(panel.leaves.getFirst()).y+panel.tips.get(panel.leaves.getLast()).y)/3);
            require(((image.getRGB(cx,cy)>>8)&255)>(image.getRGB(cx,cy)&255),"Rotated highlight missed its clade");graphics.dispose();
            scene.getJSONArray("layers").add(highlight);
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"inner_node_annotation\",\"name1\":\"A\",\"name2\":\"B\",\"shape\":\"triangle\",\"text\":\"AB\",\"size\":20,\"fill\":\"#FFB6C1FF\"}"));
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"rectangle\",\"mode\":\"pixel\",\"x\":10,\"y\":10,\"width\":10,\"height\":10,\"fill_color\":\"#FF00FFFF\",\"line_color\":null,\"line_thickness\":0,\"line_type\":\"solid\"}"));
            panel=prepare(scene);graphics=image.createGraphics();graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,image.getWidth(),image.getHeight());
            var transform=graphics.getTransform();panel.drawScene(graphics);require(transform.equals(graphics.getTransform()),"Rotation leaked into canvas graphics");graphics.dispose();
            require((image.getRGB(15,15)&0xFFFFFF)==0xFF00FF,"Direction moved free canvas geometry");
            new java.io.File("temp/yytree-directions").mkdirs();javax.imageio.ImageIO.write(image,"png",new java.io.File("temp/yytree-directions/java-"+direction+".png"));
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"layout\",\"code\":7}"));
            require(prepare(scene).slantRotation==0,"Last layout did not reset direction");
        }
    }
    private static void testHiddenLabelTracks() throws Exception {
        for(int code:new int[]{2,1,3,4,5,6,7})
            for(String direction:code==7?new String[]{"right","down","left","up"}:new String[]{"right"})
                for(int radius:new int[]{6,64})for(boolean hideExplicitly:new boolean[]{false,true})
                    for(int tracks=1;tracks<=3;tracks++) {
                        JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
                        JSONObject layout=scene.getJSONArray("layers").getJSONObject(0);
                        layout.put("code",code);if(code==7)layout.put("direction",direction);
                        scene.getJSONObject("canvas").put("margins",JSON.parseArray(radius==6?"[100,0,100,0]":"[100,40,100,40]"));
                        // Retain the global radius of 6; the large marker is a local override on A.
                        scene.getJSONArray("layers").getJSONObject(2).getJSONArray("df").getJSONObject(0).put("size",radius);
                        if(hideExplicitly)scene.getJSONArray("layers").add(JSON.parseObject(
                            "{\"type\":\"leaf_labels\",\"show\":false,\"align\":\"right\",\"gap_length\":1e100}"));
                        if(tracks!=2)scene.getJSONArray("layers").add(JSON.parseObject("""
                            {"type":"track","names":["C","A","B"],"width":20,"legend":"none",
                             "columns":{"g":{"levels":["x","y","z"],"colors":["#FF0000FF","#00FF00FF","#0000FFFF"],"values":["z","x","y"]}}}
                            """,Feature.OrderedField));
                        if(tracks!=1) {
                            var heat=JSON.parseObject("""
                                {"type":"heatmap","names":["B","C","A"],"width":20,"legend":"none","titles":["v"],
                                 "cells":[["#00FF00FF"],["#0000FFFF"],["#FF0000FF"]],"limits":[0,1],"palette":[]}
                                """);
                            for(int i=0;i<256;i++)heat.getJSONArray("palette").add("#000000FF");
                            scene.getJSONArray("layers").add(heat);
                        }
                        var panel=prepare(scene);
                        String context="layout="+code+", direction="+direction+", radius="+radius+", explicit="+hideExplicitly+", tracks="+tracks;
                        require(!panel.labels,"Hidden labels became visible: "+context);
                        var image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_RGB);
                        var graphics=image.createGraphics();graphics.setColor(java.awt.Color.WHITE);
                        graphics.fillRect(0,0,image.getWidth(),image.getHeight());graphics.setFont(panel.labelFont);
                        panel.drawTracks(graphics);graphics.dispose();
                        int[] colors={0xFF0000,0x00FF00,0x0000FF};
                        for(int column=0;column<(tracks==3?2:1);column++)for(int i=0;i<panel.leaves.size();i++) {
                            var leaf=panel.leaves.get(i);double offset=panel.labelWidth+12+column*24;
                            var bounds=panel.band(leaf,leaf,(code>=3&&code<=6?panel.outer:0)+offset,20).getBounds2D();
                            require(bounds.getMinX()>=0 && bounds.getMaxX()<=image.getWidth()
                                && bounds.getMinY()>=0 && bounds.getMaxY()<=image.getHeight(),"Hidden-label track clipped: "+context+", bounds="+bounds);
                            var point=trackSample(panel,leaf,offset+10);
                            require((image.getRGB((int)point.getX(),(int)point.getY())&0xFFFFFF)==colors[i],
                                "Hidden-label track lost leaf/color matching: "+context);
                            if(code==1||code==2||code==7) {
                                var local=code==7?panel.slantLocalBounds(bounds):bounds;
                                require(Math.abs(local.getWidth()-20)<1e-7,"Hidden-label track width changed: "+context);
                            }
                        }
                    }
    }
    private static double projectedMinimum(java.awt.geom.Rectangle2D bounds,double x,double y) {
        double minimum=Double.POSITIVE_INFINITY;
        for(double px:new double[]{bounds.getMinX(),bounds.getMaxX()})for(double py:new double[]{bounds.getMinY(),bounds.getMaxY()})
            minimum=Math.min(minimum,px*x+py*y);
        return minimum;
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
