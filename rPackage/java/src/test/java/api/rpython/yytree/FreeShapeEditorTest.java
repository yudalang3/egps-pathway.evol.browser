package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/** Interaction and R round-trip fixtures, with real 1x/2x dialog acceptance. */
public final class FreeShapeEditorTest {
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static void near(double actual,double expected,String message){require(Math.abs(actual-expected)<1e-7,message+": "+actual+" != "+expected);}
    static YyTreePreview preview() throws Exception {
        JSONObject scene=JSON.parseObject("""
            {"schema_version":1,"tree":{"labels":["A","B"],"root":3,"edges":[[3,1,1],[3,2,2]]},
             "canvas":{"width":1000,"height":800,"margins":[20,40,80,40]},"layers":[]}
            """);
        AtomicReference<YyTreeBridge.YyTreePanel> panel=new AtomicReference<>();SwingUtilities.invokeAndWait(()->panel.set(new YyTreeBridge.YyTreePanel(scene)));panel.get().prepare();
        AtomicReference<YyTreePreview> result=new AtomicReference<>();SwingUtilities.invokeAndWait(()->{YyTreePreview p=new YyTreePreview(panel.get());p.setSize(1000,850);p.doLayout();for(Component c:p.getComponents())if(c instanceof Container container)container.doLayout();p.scroll.setSize(980,770);p.scroll.doLayout();p.fitToViewport();p.cache.start();result.set(p);});
        YyTreeBridge.awaitRendering(result.get());return result.get();
    }
    static MouseEvent event(YyTreePreview p,int id,double x,double y,int clicks){return new MouseEvent(p.canvas,id,System.currentTimeMillis(),id==MouseEvent.MOUSE_DRAGGED?InputEvent.BUTTON1_DOWN_MASK:0,(int)Math.round(x*p.canvas.zoom),(int)Math.round(y*p.canvas.zoom),clicks,false,id==MouseEvent.MOUSE_DRAGGED?MouseEvent.NOBUTTON:MouseEvent.BUTTON1);}
    static void drag(YyTreePreview p,double x1,double y1,double x2,double y2){p.navigation.mousePressed(event(p,MouseEvent.MOUSE_PRESSED,x1,y1,1));p.navigation.mouseDragged(event(p,MouseEvent.MOUSE_DRAGGED,x2,y2,1));p.navigation.mouseReleased(event(p,MouseEvent.MOUSE_RELEASED,x2,y2,1));}
    static JSONObject heatmap(String matrix,String colors,String limits){JSONObject source=new JSONObject(true);source.put("matrix",FreeShapes.parseMatrix(matrix));source.put("colors",JSON.parseArray(colors));source.put("limits",limits==null?null:JSON.parseArray(limits));JSONObject l=JSON.parseObject("{\"type\":\"free_heatmap\",\"mode\":\"percent\",\"x\":10,\"y\":60,\"width\":25,\"height\":20}");l.put("source",source);l.put("cells",FreeShapes.heatColors(source));return l;}
    static void interactions(YyTreePreview p){
        var e=p.editor;var g=p.canvas.freeShapes;e.active=true;p.snap.setSelected(false);String original=p.canvas.scene.toJSONString();int paints=p.canvas.sceneDrawCount.get();
        for(String type:new String[]{"rectangle","ellipse","line","arrow","bezier","bezier_ring"}){
            int count=e.layers.size();e.setTool(type);drag(p,500,400,350,290);require(e.layers.size()==count+1,type+" creation failed");JSONObject l=e.layers.getLast();require(l.getString("mode").equals("percent"),"New mode is not percent");require(e.tool.equals("select")&&e.selected==count,"Creation did not select object");
            var before=FreeShapes.copy(l);require(before.equals(l),"Copy changed numeric types");
            if(type.equals("rectangle")){Rectangle2D bounds=g.shape(l).getBounds2D();p.navigation.mousePressed(event(p,MouseEvent.MOUSE_PRESSED,bounds.getCenterX(),bounds.getCenterY(),1));p.navigation.mouseReleased(event(p,MouseEvent.MOUSE_RELEASED,bounds.getCenterX(),bounds.getCenterY(),1));}
            e.undo();require(e.layers.size()==count,"Creation undo failed or a no-op click polluted history");e.redo();require(e.layers.getLast().equals(before),"Creation redo failed");
            Rectangle2D bounds=g.shape(e.layers.getLast()).getBounds2D();Point2D point=type.equals("line")||type.equals("arrow")?new Point2D.Double(425,345):new Point2D.Double(bounds.getCenterX(),bounds.getCenterY());
            // Curves may not pass through their bounds centre: select by an endpoint.
            if(type.startsWith("bezier"))point=g.point(e.layers.getLast(),e.layers.getLast().getJSONArray("points").getJSONArray(0));
            drag(p,point.getX(),point.getY(),point.getX()+25,point.getY()+20);require(!e.layers.getLast().equals(before),type+" drag did not edit geometry");e.undo();require(e.layers.getLast().equals(before),type+" drag was not one undo step");
        }
        e.setTool("polygon");for(double[] point:new double[][]{{50,50},{170,60},{120,160}})p.navigation.mousePressed(event(p,MouseEvent.MOUSE_PRESSED,point[0],point[1],1));e.finishPolygon();require(e.layers.getLast().getJSONArray("points").size()==3,"Polygon creation failed");
        JSONObject text=JSON.parseObject("{\"type\":\"text\",\"mode\":\"pixel\",\"x\":300,\"y\":100,\"text\":\"中文 \\\"title\\\" \\\\ path\\nline\\tend\",\"locator\":\"right\",\"gap\":7,\"size\":18,\"fill_color\":\"#12345680\"}");e.add(text);
        Rectangle2D textBounds=g.shape(e.layers.getLast()).getBounds2D();Point2D fixed=new Point2D.Double(textBounds.getMinX(),textBounds.getMinY());drag(p,textBounds.getMaxX(),textBounds.getMaxY(),fixed.getX()+2*textBounds.getWidth(),fixed.getY()+2*textBounds.getHeight());require(FreeShapes.n(e.layers.getLast(),"size",0)>30,"Text handle did not scale font");
        JSONObject heat=heatmap("0\tNA\t10\n-2\t5\t8","[\"#2166ACFF\",\"#FFFFFFFF\",\"#B2182BFF\"]","[0,10]");g.prepareHeatmap(heat);e.add(heat);require(e.layers.size()==9,"Nine types were not created");
        for(String mode:new String[]{"relative","pixel","percent"}){
            for(JSONObject l:e.layers){Rectangle2D before=g.shape(l).getBounds2D();g.mode(l,mode);Rectangle2D after=g.shape(l).getBounds2D();near(before.getX(),after.getX(),"Mode x");near(before.getY(),after.getY(),"Mode y");near(before.getWidth(),after.getWidth(),"Mode width");near(before.getHeight(),after.getHeight(),"Mode height");}
        }
        // Every shape supports shared affine movement/resize, preserving matrix identity.
        for(JSONObject l:e.layers){JSONObject changed=FreeShapes.copy(l);Rectangle2D before=g.shape(l).getBounds2D();g.transform(changed,AffineTransform.getTranslateInstance(11,13));Rectangle2D after=g.shape(changed).getBounds2D();near(after.getX(),before.getX()+11,"Translation x");near(after.getY(),before.getY()+13,"Translation y");g.transform(changed,AffineTransform.getScaleInstance(2,2));require(g.shape(changed).getBounds2D().getWidth()>=after.getWidth(),"Resize failed");}
        e.selected=0;JSONObject first=e.layers.getFirst();e.reorder(1);require(e.layers.get(1)==first,"Forward layer failed");e.undo();e.selected=0;e.delete();require(e.layers.size()==8,"Delete failed");e.undo();require(e.layers.size()==9,"Delete undo failed");
        // Cancel must restore the exact geometry without adding an undo operation.
        e.selected=0;Rectangle2D b=g.shape(e.layers.getFirst()).getBounds2D();String before=e.layers.getFirst().toJSONString();p.navigation.mousePressed(event(p,MouseEvent.MOUSE_PRESSED,b.getCenterX(),b.getCenterY(),1));p.navigation.mouseDragged(event(p,MouseEvent.MOUSE_DRAGGED,b.getCenterX()+40,b.getCenterY()+30,1));e.cancel();require(e.layers.getFirst().toJSONString().equals(before),"Escape did not restore drag");
        p.navigation.mousePressed(event(p,MouseEvent.MOUSE_PRESSED,b.getCenterX(),b.getCenterY(),1));p.navigation.mouseDragged(event(p,MouseEvent.MOUSE_DRAGGED,b.getCenterX()+10,b.getCenterY()+10,1));
        e.eventDispatched(new MouseEvent(p.hint,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,0,0,1,false,MouseEvent.BUTTON1));require(!e.isDragging(),"Outside release left a stale shape drag");e.undo();
        // Endpoints snap to tree nodes, but whole-object translation does not.
        e.setTool("arrow");drag(p,100,200,200,200);p.snap.setSelected(true);var node=p.nodePoints.getFirst();JSONObject arrow=e.layers.getLast();Point2D endpoint=new Point2D.Double(g.value(arrow,"x2",true),g.value(arrow,"y2",false));drag(p,endpoint.getX(),endpoint.getY(),node.x()+3/p.canvas.zoom,node.y()+2/p.canvas.zoom);near(g.value(e.layers.getLast(),"x2",true),node.x(),"Arrow node snap x");near(g.value(e.layers.getLast(),"y2",false),node.y(),"Arrow node snap y");e.delete();
        BufferedImage image=new BufferedImage(1000,800,BufferedImage.TYPE_INT_ARGB);Graphics2D graphics=image.createGraphics();e.paintContents(graphics);e.paintOverlay(graphics);graphics.dispose();
        require(p.canvas.sceneDrawCount.get()==paints,"Shape editing redrew the tree");require(original.equals(p.canvas.scene.toJSONString()),"Editing changed original scene");
        String code=FreeShapes.code(e.layers);require(code.startsWith("+ free_")&&code.contains("matrix(c(")&&code.contains("\\\"title\\\""),"R serialization missed content");
        int count=e.layers.size();e.close();require(!e.active&&e.layers.size()==count,"Closing editor lost contents");
        String snapshot=p.exportSnapshot();
        JSONObject exported=JSON.parseObject(snapshot);
        require(exported.getJSONArray("layers").size()==count,"Export lost edited shapes");
        require(exported.getJSONObject("canvas").getIntValue("width")==1000,"Export changed logical size");
        JSONObject saved=FreeShapes.copy(e.layers.getFirst());
        e.layers.getFirst().put("x",99999);
        require(!exported.getJSONArray("layers").getJSONObject(0).equals(e.layers.getFirst()),"Export snapshot changed after editing");
        e.layers.set(0,saved);
        p.canvas.zoom=2;Point2D view=new Point2D.Double(300,350);MouseEvent mouse=event(p,MouseEvent.MOUSE_MOVED,view.getX(),view.getY(),1);near(e.logical(mouse).getX(),300,"Zoomed logical x");
    }
    static void arrowSheet(YyTreePreview p,Path directory) throws Exception {
        BufferedImage image=new BufferedImage(900,520,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,900,520);int row=0;
        for(String style:new String[]{"open","filled"})for(int thickness:new int[]{1,2,5,10}){
            int y=35+row++*60;g.setColor(Color.BLACK);g.drawString(style+", width "+thickness,12,y);
            for(int direction=0;direction<4;direction++){
                JSONObject l=JSON.parseObject("{\"type\":\"arrow\",\"mode\":\"pixel\",\"line_color\":\"#00CC00FF\",\"line_type\":\"dashed\"}");l.put("head_style",style);l.put("line_thickness",thickness);l.put("x1",200+direction*160);l.put("y1",y+12);l.put("x2",direction%2==0?270+direction*160:230+direction*160);l.put("y2",direction%2==0?y-10:y-24);p.canvas.freeShapes.draw(g,l);
                require(p.canvas.freeShapes.arrow(l).head().getBounds2D().getWidth()>0,"Arrow geometry missing");
                l.put("x2",l.get("x1"));l.put("y2",l.get("y1"));require(p.canvas.freeShapes.arrow(l).head().getBounds2D().isEmpty(),"Zero-length arrow not empty");
            }
        }g.dispose();ImageIO.write(image,"png",directory.resolve("arrows.png").toFile());
    }
    static void fixtures(YyTreePreview p,Path directory)throws Exception {
        List<JSONObject> fixtures=new ArrayList<>();JSONObject all=new JSONObject(true);all.put("code",FreeShapes.code(p.editor.layers));all.put("layers",p.editor.layers);fixtures.add(all);
        for(JSONObject heat:List.of(
            heatmap("NA\tNA\nNA\tNA","[\"#00000000\",\"#FFFFFFFF\"]",null),
            heatmap("5\t5\nNA\t5","[\"#2166ACFF\",\"#FFFFFFFF\",\"#B2182BFF\"]","[5,5]"),
            heatmap("-1e308\t0\t1e308\nNA\t-9e307\t9e307","[\"#01234520\",\"#89ABCDEF\",\"#FEDCBA88\"]","[-1e308,1e308]"))) {
            JSONObject entry=new JSONObject(true);entry.put("code",FreeShapes.code(List.of(heat)));entry.put("layers",List.of(heat));fixtures.add(entry);
        }
        for(int stops:new int[]{2,3,4,6,7,8,9,16,256}){
            JSONArray colors=new JSONArray();for(int i=0;i<stops;i++)colors.add(String.format(java.util.Locale.ROOT,"#%02X%02X%02X%02X",(i*71+13)%256,(i*19+47)%256,(i*53+89)%256,(i*31+11)%256));
            List<String> values=new ArrayList<>();for(int i=0;i<256;i++)values.add(Integer.toString(i));
            JSONObject heat=heatmap(String.join("\t",values),colors.toJSONString(),"[0,255]");JSONObject entry=new JSONObject(true);entry.put("code",FreeShapes.code(List.of(heat)));entry.put("layers",List.of(heat));fixtures.add(entry);
        }
        List<String> ramp=new ArrayList<>();for(int i=0;i<256;i++)ramp.add(Integer.toString(i));
        for(JSONObject heat:List.of(
            heatmap(String.join("\t",ramp),"[\"#01234520\",\"#DE781293\",\"#89ABCDEF\",\"#3210FEDC\",\"#FEDCBA88\"]","[0,255]"),
            heatmap("NA","[\"#12345678\"]",null))) {
            JSONObject entry=new JSONObject(true);entry.put("code",FreeShapes.code(List.of(heat)));entry.put("layers",List.of(heat));fixtures.add(entry);
        }
        Files.writeString(directory.resolve("roundtrip.json"),JSON.toJSONString(fixtures),StandardCharsets.UTF_8);
        JSONObject old=heatmap("0\t1","[\"#000000FF\",\"#FFFFFFFF\"]",null);old.remove("source");boolean rejected=false;try{FreeShapes.code(old);}catch(IllegalStateException expected){rejected=true;}require(rejected,"Legacy heatmap incorrectly invented numeric values");
        require(FreeShapes.parseMatrix("1\t\nNA\t3\n").getJSONArray(0).get(1)==null,"Trailing blank cell was lost");
        for(String invalid:new String[]{"1,2\n3","1,Inf","1,word"," "}){boolean failed=false;try{FreeShapes.parseMatrix(invalid);}catch(IllegalArgumentException expected){failed=true;}require(failed,"Invalid matrix accepted: "+invalid);}
    }
    static void window(YyTreePreview p,Path directory)throws Exception {
        AtomicReference<JFrame> frame=new AtomicReference<>();SwingUtilities.invokeAndWait(()->{JFrame f=new JFrame("Free shapes acceptance");f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);f.setContentPane(p);f.setSize(1050,850);f.setLocation(5,5);f.setVisible(true);frame.set(f);p.addFreeShapes.doClick();require(p.editor.dialog.isVisible()&&!p.editor.dialog.isModal(),"Editor is not modeless");p.editor.dialog.setLocation(30,60);p.editor.selected=0;p.editor.setTool("select");});
        YyTreeBridge.awaitRendering(p);
        SwingUtilities.invokeAndWait(()->{p.editor.objects.setSelectedIndex(3);setProperty(p.editor,"head_length","17");near(FreeShapes.n(p.editor.layers.get(3),"head_length",0),17,"Properties did not apply");p.editor.undo();});
        AtomicReference<Boolean> ready=new AtomicReference<>(false);
        for(int attempt=0;attempt<100&&!ready.get();attempt++){Thread.sleep(25);SwingUtilities.invokeAndWait(()->ready.set(p.editor.copyButton.isEnabled()));}
        require(ready.get(),"Generated code did not become available");
        SwingUtilities.invokeAndWait(()->{try{var clipboard=new java.awt.datatransfer.Clipboard("free-shape-test");p.editor.copyCode(clipboard);String contents=(String)clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor);require(contents.equals(FreeShapes.code(p.editor.layers)),"Clipboard code was stale");}catch(Exception error){throw new RuntimeException(error);}});
        SwingUtilities.invokeAndWait(()->{try{
            JDialog dialog=p.editor.dialog;double scale=dialog.getGraphicsConfiguration().getDefaultTransform().getScaleX();
            BufferedImage image=new BufferedImage((int)Math.ceil(dialog.getWidth()*scale),(int)Math.ceil(dialog.getHeight()*scale),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();try{g.scale(scale,scale);dialog.printAll(g);}finally{g.dispose();}
            ImageIO.write(image,"png",directory.resolve("editor-"+System.getProperty("sun.java2d.uiScale","native")+"x.png").toFile());
        }catch(Exception error){throw new RuntimeException(error);}});
        SwingUtilities.invokeAndWait(()->{p.editor.close();p.addFreeShapes.doClick();require(p.editor.layers.size()==9,"Reopen lost shapes");p.editor.dialog.dispatchEvent(new WindowEvent(p.editor.dialog,WindowEvent.WINDOW_CLOSING));require(!p.editor.active,"Window close left editor active");p.disposeInteraction();frame.get().dispose();});
    }
    static void setProperty(FreeShapeEditor editor,String key,String value){Component[] children=editor.properties.getComponents();for(int i=0;i<children.length-1;i++)if(children[i] instanceof JLabel label&&label.getText().equals(key)&&children[i+1] instanceof JTextField field){field.setText(value);field.postActionEvent();return;}throw new AssertionError("Missing property "+key);}
    static void capture(Component component,Path path){try{double scale=component.getGraphicsConfiguration().getDefaultTransform().getScaleX();BufferedImage image=new BufferedImage((int)Math.ceil(component.getWidth()*scale),(int)Math.ceil(component.getHeight()*scale),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();try{g.scale(scale,scale);component.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",path.toFile());}catch(Exception error){throw new RuntimeException(error);}}
    static void example(Path directory)throws Exception {
        YyTreeBridge bridge=new YyTreeBridge();try{
            bridge.showPlot(Files.readString(directory.resolve("run5-scene.json"),StandardCharsets.UTF_8),true);
            AtomicReference<YyTreePreview> shown=new AtomicReference<>();SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w instanceof JFrame f&&f.getContentPane() instanceof YyTreePreview p){shown.set(p);break;}require(shown.get()!=null,"run5 preview missing");capture(shown.get(),directory.resolve("run5-preview.png"));shown.get().addFreeShapes.doClick();int index=-1;for(int i=0;i<shown.get().editor.layers.size();i++)if("arrow".equals(shown.get().editor.layers.get(i).getString("type"))&&FreeShapes.n(shown.get().editor.layers.get(i),"line_thickness",1)==5)index=i;require(index>=0,"Original green arrow missing");shown.get().editor.objects.setSelectedIndex(index);setProperty(shown.get().editor,"head_style","filled");});
            Thread.sleep(250);SwingUtilities.invokeAndWait(()->{capture(shown.get().editor.dialog,directory.resolve("run5-editor.png"));capture(shown.get(),directory.resolve("run5-edited-preview.png"));try{Files.writeString(directory.resolve("run5-code.R"),FreeShapes.code(shown.get().editor.layers),StandardCharsets.UTF_8);}catch(Exception error){throw new RuntimeException(error);}});
        }finally{bridge.closeAll();}
    }
    public static void main(String[] args)throws Exception {
        Path directory=Path.of("temp","yytree-free-shapes");Files.createDirectories(directory);SwingUtilities.invokeAndWait(YyTreePreview::initializeTheme);
        if(args.length>0&&args[0].equals("--example")){example(directory);System.out.println("Original run5 script preview and live arrow editor passed.");return;}
        YyTreePreview p=preview();
        try{SwingUtilities.invokeAndWait(()->interactions(p));arrowSheet(p,directory);fixtures(p,directory);if(!GraphicsEnvironment.isHeadless())window(p,directory);}finally{SwingUtilities.invokeAndWait(()->{p.disposeInteraction();Window owner=SwingUtilities.getWindowAncestor(p);if(owner!=null)owner.dispose();});}
        System.out.println("Nine free shapes, gestures, modes, history, snap, source isolation, matrix parsing and R fixtures passed.");
    }
}
