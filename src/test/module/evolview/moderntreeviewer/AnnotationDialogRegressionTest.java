package module.evolview.moderntreeviewer;
import java.awt.*;
import java.lang.reflect.Field;
import javax.swing.*;
import module.evolview.gfamily.work.gui.dialog.*;
import module.evolview.gfamily.work.gui.tree.*;
import module.evolview.phylotree.visualization.layout.*;
import module.evolview.model.tree.GraphicsNode;
public class AnnotationDialogRegressionTest {
 static Object field(Object object,String name) {
  try {Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}
 }
 static <T> T find(Container container,Class<T> type,String text) {
  for(var child:container.getComponents()) {
   if(type.isInstance(child) && (text==null || child instanceof JButton b && text.equals(b.getText())))return type.cast(child);
   if(child instanceof Container nested){T result=find(nested,type,text);if(result!=null)return result;}
  }return null;
 }
 static void choose(JButton button,Color color,boolean cancel) {
  Timer timer=new Timer(50,null);long deadline=System.nanoTime()+5_000_000_000L;
  timer.addActionListener(event->{
   for(var window:Window.getWindows())if(window instanceof JDialog dialog && dialog.isShowing()) {
    var chooser=find(dialog,JColorChooser.class,null);if(chooser==null)continue;
    chooser.setColor(color);find(dialog,JButton.class,cancel?"Cancel":"OK").doClick();timer.stop();return;
   }
   if(System.nanoTime()>deadline){timer.stop();for(var w:Window.getWindows())if(w instanceof JDialog)w.dispose();}
  });timer.start();try{button.doClick();}finally{timer.stop();}
 }
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  java.util.Locale.setDefault(java.util.Locale.ENGLISH);
  var screenshot=new java.util.concurrent.atomic.AtomicReference<java.awt.image.BufferedImage>();
  var root=new GraphicsNode("Root");var a=new GraphicsNode("A");a.setRealBranchLength(1);root.addChild(a);var b=new GraphicsNode("B");b.setRealBranchLength(2);root.addChild(b);
  var props=new TreeLayoutProperties(root);var panel=new PhylogeneticTreePanel(props);var dialog=new NodeAnnotationDialogContainer[1];
  try {
   SwingUtilities.invokeAndWait(()->{var viewport=new JViewport();viewport.setSize(900,600);viewport.setView(panel);panel.initializeLeftPanel();dialog[0]=new NodeAnnotationDialogContainer(panel,root);dialog[0].setModal(false);dialog[0].setVisible(true);});
   SwingUtilities.invokeAndWait(()->{});
   SwingUtilities.invokeAndWait(()->{
    var tree=(JTree)field(dialog[0],"jTree");var model=(javax.swing.tree.DefaultMutableTreeNode)tree.getModel().getRoot();
    check(model.getChildAt(0) instanceof DialogLeafNameAnnotation,"Leaf annotation first");
    check(tree.getLastSelectedPathComponent()==model.getChildAt(0),"Leaf annotation selected by default");
    tree.setSelectionRow(1);
    var side=(DialogSidewardAnnotation)model.getChildAt(1);
    ((JSpinner)field(side,"lineGap")).setValue(24.0);
    ((JSpinner)field(side,"textGap")).setValue(13.0);
    var gapStyle=dialog[0].getCurrentAnnotationsProperties().getOutterSidewardAnnos().getFirst().getSidewardNodeAnnotation();
    Font originalPreviewFont=gapStyle.getTextFont();
    check(gapStyle.getLineGap()==24 && gapStyle.getTextGap()==13,"Gap controls update preview");
    ((JTextField)field(side,"rdTextField")).setText("Example clade");
    check(!(((JSplitPane)dialog[0].getContentPane().getComponent(0)).getRightComponent() instanceof JScrollPane),"Preview has no scroll pane");
    var canvas=(JPanel)((JSplitPane)dialog[0].getContentPane().getComponent(0)).getRightComponent();
    for (Dimension dimension : new Dimension[]{new Dimension(360,340),new Dimension(240,180)}) {
     canvas.setSize(dimension);
     var small=new java.awt.image.BufferedImage(dimension.width,dimension.height,java.awt.image.BufferedImage.TYPE_INT_RGB);
     var smallGraphics=small.createGraphics();canvas.printAll(smallGraphics);smallGraphics.dispose();
     int red=0;
     for(int y=0;y<dimension.height;y++)for(int x=0;x<dimension.width;x++)if(small.getRGB(x,y)==Color.RED.getRGB()) {
      red++;check(x>=10 && y>=10 && x<dimension.width-10 && y<dimension.height-10,"Preview mark fits with margin");
     }
     check(red>0,"Small preview shows annotation");
    }
    dialog[0].validate();
    var image=new java.awt.image.BufferedImage(900,450,java.awt.image.BufferedImage.TYPE_INT_RGB);
    var graphics=image.createGraphics();dialog[0].validate();dialog[0].printAll(graphics);graphics.dispose();screenshot.set(image);
    check(gapStyle.getTextFont().equals(originalPreviewFont),"Preview magnification preserves actual font");

    choose((JButton)field(side,"textColorButton"),Color.BLUE,false);
    var style=dialog[0].getCurrentAnnotationsProperties().getOutterSidewardAnnos().getFirst().getSidewardNodeAnnotation();
    check(style.getTextColor().equals(Color.BLUE) && style.getLineColor().equals(Color.RED),"Independent text color");
    choose((JButton)field(side,"textColorButton"),Color.GREEN,true);
    check(dialog[0].getCurrentAnnotationsProperties().getOutterSidewardAnnos().getFirst().getSidewardNodeAnnotation().getTextColor().equals(Color.BLUE),"Cancelled text color");
    for(int row : new int[]{2,3,0}) {
     tree.setSelectionRow(row);var option=model.getChildAt(row);
     String buttonName=row==2?"shapeColorButton":row==3?"colorChangeButton":"changeColorButton";
     choose((JButton)field(option,buttonName),Color.MAGENTA,false);
     var annotations=dialog[0].getCurrentAnnotationsProperties();
     if(row==2)check(!annotations.getInternalNode2LeafInsituAnnos().isEmpty(),"In-situ preview");
     if(row==3)check(annotations.getInternalNode2LeafAnnos().getFirst().getColor().equals(Color.MAGENTA),"Region color");
     if(row==0)check(annotations.getLeafNameAnnos().getFirst().getColor().equals(Color.MAGENTA),"Leaf color");
    }
    find(dialog[0],JButton.class,"Cancel").doClick();check(!panel.getMainAnnotationsProperties().hasAnnotation(),"Cancel is isolated");
   });
   java.nio.file.Files.createDirectories(java.nio.file.Path.of("temp/annotation-preview"));
   javax.imageio.ImageIO.write(screenshot.get(),"png",java.nio.file.Path.of("temp/annotation-preview/dialog.png").toFile());
   org.slf4j.LoggerFactory.getLogger(AnnotationDialogRegressionTest.class).info("Four annotation dialogs and real color chooser passed");
  }finally{SwingUtilities.invokeAndWait(()->{for(var w:Window.getWindows())w.dispose();});}
 }
}
