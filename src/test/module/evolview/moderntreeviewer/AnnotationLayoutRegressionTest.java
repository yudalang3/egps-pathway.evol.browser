package module.evolview.moderntreeviewer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.phylotree.visualization.layout.*;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.annotation.*;
public class AnnotationLayoutRegressionTest {
 static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
 static GraphicsNode node(String name,GraphicsNode... children) {
  var n=new GraphicsNode(name);n.setRealBranchLength(1);for(var child:children)n.addChild(child);return n;
 }
 static BufferedImage paint(BaseLayout layout) {
  var image=new BufferedImage(1000,800,BufferedImage.TYPE_INT_RGB);
  var g=image.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,1000,800);layout.paintGraphics(g);g.dispose();return image;
 }
 static long pixels(BufferedImage image,int color) {
  long count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getRGB(x,y)==color)count++;return count;
 }
 static void run(TreeLayout type, boolean annotateRoot) throws Exception {
  var a=node("Alpha");var b=node("Beta");var clade=node("Clade",a,b);
  var root=node("Root",clade,node("Gamma"),node("Delta"));
  var target=annotateRoot?root:clade;
  var props=new TreeLayoutProperties(root);props.setMyLayout(type);props.setShowTitle(false);props.setShowAxisBar(false);props.setShowScaleBar(false);
  LeafLabelStateInitializer.applyInitialLeafLabelState(props,true);
  var panel=new PhylogeneticTreePanel(props);var viewport=new JViewport();viewport.setSize(1000,800);viewport.setView(panel);panel.initializeLeftPanel();
  var layout=panel.getCurrLayout();var annotations=panel.getMainAnnotationsProperties();
  for(int kind=0;kind<4;kind++) {
   annotations.clearAllAnnotation();
   switch(kind) {
    case 0 -> {var style=new SidewardNodeAnnotation();style.setLineColor(Color.MAGENTA);style.setLineStroke(new BasicStroke(3));style.setTextColor(Color.BLUE);style.setTextFont(new Font("Dialog",0,14));style.setTextString("Clade annotation");style.setDrawTextHorizontal(true);var anno=new DrawPropOutterSidewardAnno(style,target);anno.setJust4annotationNot4lineageTypeTrue();annotations.getOutterSidewardAnnos().add(anno);}
    case 1 -> {var anno=new DrawPropInternalNodeInsituAnno(target);anno.setShapParameter(12,CommonShape.RECTANGE,Color.MAGENTA);anno.setFontParameter(new Font("Dialog",0,14),"Clade",Color.BLUE);annotations.getInternalNode2LeafInsituAnnos().add(anno);}
    case 2 -> annotations.getInternalNode2LeafAnnos().add(new DrawPropInternalNode2LeafAnno(Color.MAGENTA,target));
    case 3 -> annotations.getLeafNameAnnos().add(new DrawPropLeafNameAnno(Color.MAGENTA,target));
   }
   layout.calculateForPainting(1000,800);var image=paint(layout);
   check(pixels(image,Color.MAGENTA.getRGB())>0,type+" kind="+kind+" visible");
   if(kind==0 && layout instanceof CircularPhylo) {
    var annotation=annotations.getOutterSidewardAnnos().getFirst();
    if(annotation.getShape() instanceof java.awt.geom.Arc2D arc) {
     var style=annotation.getSidewardNodeAnnotation();
     double minimumRadius=arc.getWidth()/2+style.getLineStroke().getLineWidth()/2+style.getTextGap()-1;
     check(pixels(image,Color.BLUE.getRGB())>0,type+" text visible");
     for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
      if(image.getRGB(x,y)==Color.BLUE.getRGB())
       check(Math.hypot(x-arc.getCenterX(),y-arc.getCenterY())>=minimumRadius,type+" text outside arc");
    }
   }
   if(type==TreeLayout.RECT_HALF_CIRCLE && kind==2) {
    int branchX=((int)a.getXSelf()+(int)a.getXParent())/2;
    int branchY=((int)a.getYSelf()+(int)a.getYParent())/2;
    check(image.getRGB(branchX,branchY)==Color.BLACK.getRGB(),"Opaque region preserves branch color");
   }
   clade.setCollapse(true); props.initializeRoot(); layout.calculateForPainting(1000,800);
   paint(layout); clade.setCollapse(false); props.initializeRoot(); layout.calculateForPainting(1000,800);
   check(pixels(paint(layout),Color.MAGENTA.getRGB())>0,type+" expanded kind="+kind);

   Files.createDirectories(Path.of("temp/annotation-layouts"));ImageIO.write(image,"png",Path.of("temp/annotation-layouts",type+"-"+kind+(annotateRoot?"-root":"")+".png").toFile());
  }
  var hidden=new DrawPropInternalNodeInsituAnno(a);clade.setCollapse(true);check(!hidden.shouldConfigurateAndPaint(),"Hidden descendant");clade.setCollapse(false);check(hidden.shouldConfigurateAndPaint(),"Expanded descendant");
 }
 static void birds() throws Exception {
  var input=new module.evolview.moderntreeviewer.io.MTVImportInforBean();
  input.setInput_nwk_string(Files.readString(Path.of("src/module/evolview/moderntreeviewer/examples/04-bird-orders.nwk")));
  for(var type: new TreeLayout[]{TreeLayout.OUTER_CIR_PHYLO,TreeLayout.OUTER_CIR_CLADO_ALIGNED,TreeLayout.OUTER_CIR_CLADO_EQUAL,TreeLayout.OUTER_CIR_PHYLO_ALIGNED,TreeLayout.INNER_CIR_CLADO}) {
   var root=new module.evolview.moderntreeviewer.io.TreeParser4MTV().parseTree(input).orElseThrow();
   var props=new TreeLayoutProperties(root);props.setMyLayout(type);props.setShowTitle(false);props.setShowAxisBar(false);props.setShowScaleBar(false);
   LeafLabelStateInitializer.applyInitialLeafLabelState(props,true);
   var panel=new PhylogeneticTreePanel(props);var vp=new JViewport();vp.setSize(1000,800);vp.setView(panel);panel.initializeLeftPanel();
   var layout=panel.getCurrLayout();
   for(var target:PhylogeneticTreePanel.subtreeNodes(root)) {
    if(target.getChildCount()<2)continue;
    for(boolean horizontal:new boolean[]{true,false}) {
    panel.getMainAnnotationsProperties().clearAllAnnotation();
    var style=new SidewardNodeAnnotation();style.setLineStroke(new BasicStroke(6));style.setLineColor(Color.RED);style.setTextColor(Color.BLUE);style.setTextString("Bird clade");style.setTextFont(new Font("Dialog",0,14));style.setDrawTextHorizontal(horizontal);
    var anno=new DrawPropOutterSidewardAnno(style,target);anno.setJust4annotationNot4lineageTypeTrue();panel.getMainAnnotationsProperties().getOutterSidewardAnnos().add(anno);
    layout.calculateForPainting(1000,800);var image=paint(layout);
    var arc=(java.awt.geom.Arc2D)anno.getShape();
    check(pixels(image,Color.BLUE.getRGB())>0,"Bird text visible "+type);
    for(int y=0;y<800;y++)for(int x=0;x<1000;x++)if(image.getRGB(x,y)==Color.BLUE.getRGB())check(Math.hypot(x-arc.getCenterX(),y-arc.getCenterY())>=arc.getWidth()/2+style.getTextGap()+2,"Bird text outside "+type);
    if(arc.getCenterX()>anno.getTextLocation().x)ImageIO.write(image,"png",Path.of("temp/annotation-layouts",type+"-birds-left.png").toFile());
    }
   }
  }
 }
 public static void main(String[] args) throws Exception {
  birds();
  for(var type:TreeLayout.values()) {run(type,false);run(type,true);}
  org.slf4j.LoggerFactory.getLogger(AnnotationLayoutRegressionTest.class).info("Annotation matrix passed: {} layouts x 4 types x root/internal targets",TreeLayout.values().length);
 }
}
