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
  var root=new GraphicsNode("Root");var a=new GraphicsNode("A");a.setRealBranchLength(1);root.addChild(a);var b=new GraphicsNode("B");b.setRealBranchLength(2);root.addChild(b);
  var props=new TreeLayoutProperties(root);var panel=new PhylogeneticTreePanel(props);var dialog=new NodeAnnotationDialogContainer[1];
  try {
   SwingUtilities.invokeAndWait(()->{var viewport=new JViewport();viewport.setSize(900,600);viewport.setView(panel);panel.initializeLeftPanel();dialog[0]=new NodeAnnotationDialogContainer(panel,root);});
   SwingUtilities.invokeAndWait(()->{});
   SwingUtilities.invokeAndWait(()->{
    var tree=(JTree)field(dialog[0],"jTree");var model=(javax.swing.tree.DefaultMutableTreeNode)tree.getModel().getRoot();
    var side=(DialogSidewardAnnotation)model.getChildAt(0);
    choose((JButton)field(side,"textColorButton"),Color.BLUE,false);
    var style=dialog[0].getCurrentAnnotationsProperties().getOutterSidewardAnnos().getFirst().getSidewardNodeAnnotation();
    check(style.getTextColor().equals(Color.BLUE) && style.getLineColor().equals(Color.RED),"Independent text color");
    choose((JButton)field(side,"textColorButton"),Color.GREEN,true);
    check(dialog[0].getCurrentAnnotationsProperties().getOutterSidewardAnnos().getFirst().getSidewardNodeAnnotation().getTextColor().equals(Color.BLUE),"Cancelled text color");
    for(int row=1;row<4;row++) {
     tree.setSelectionRow(row);var option=model.getChildAt(row);
     String buttonName=row==1?"shapeColorButton":row==2?"colorChangeButton":"changeColorButton";
     choose((JButton)field(option,buttonName),Color.MAGENTA,false);
     var annotations=dialog[0].getCurrentAnnotationsProperties();
     if(row==1)check(!annotations.getInternalNode2LeafInsituAnnos().isEmpty(),"In-situ preview");
     if(row==2)check(annotations.getInternalNode2LeafAnnos().getFirst().getColor().equals(Color.MAGENTA),"Region color");
     if(row==3)check(annotations.getLeafNameAnnos().getFirst().getColor().equals(Color.MAGENTA),"Leaf color");
    }
    find(dialog[0],JButton.class,"Cancel").doClick();check(!panel.getMainAnnotationsProperties().hasAnnotation(),"Cancel is isolated");
   });
   org.slf4j.LoggerFactory.getLogger(AnnotationDialogRegressionTest.class).info("Four annotation dialogs and real color chooser passed");
  }finally{SwingUtilities.invokeAndWait(()->{for(var w:Window.getWindows())w.dispose();});}
 }
}
